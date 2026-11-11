-- Mystic Realm online schema for Supabase.
-- Run this in Supabase Dashboard -> SQL Editor.
-- Login uses username + password. The client maps username to a synthetic
-- address on the project's own Supabase domain because Supabase Auth requires
-- an email/phone identity. Disable "Confirm email" in Authentication ->
-- Providers -> Email, otherwise signup cannot return a session.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------- tables

create table if not exists public.players (
    id uuid primary key references auth.users(id) on delete cascade,
    username text not null,
    avatar_url text,
    created_at timestamptz not null default now()
);
create unique index if not exists players_username_lower_idx
    on public.players (lower(username));

create table if not exists public.game_rooms (
    id uuid primary key default gen_random_uuid(),
    room_code text not null unique,
    host_id uuid not null references public.players(id) on delete cascade,
    status text not null default 'waiting'
        check (status in ('waiting', 'playing', 'finished')),
    settings jsonb not null default '{"max_players":4,"game_mode":"classic"}'::jsonb,
    created_at timestamptz not null default now()
);
create index if not exists game_rooms_status_idx
    on public.game_rooms (status, created_at desc);

create table if not exists public.room_members (
    id uuid primary key default gen_random_uuid(),
    room_id uuid not null references public.game_rooms(id) on delete cascade,
    player_id uuid not null references public.players(id) on delete cascade,
    role text not null default 'member' check (role in ('host', 'member')),
    joined_at timestamptz not null default now(),
    unique (room_id, player_id)
);
create index if not exists room_members_room_idx
    on public.room_members (room_id);

create table if not exists public.game_sessions (
    id uuid primary key default gen_random_uuid(),
    room_id uuid references public.game_rooms(id) on delete set null,
    result jsonb not null default '{}'::jsonb,
    started_at timestamptz,
    ended_at timestamptz not null default now()
);

create table if not exists public.player_stats (
    player_id uuid primary key references public.players(id) on delete cascade,
    wins integer not null default 0,
    losses integer not null default 0,
    total_games integer not null default 0
);

-- ------------------------------------------------------- signup profile

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    uname text;
begin
    uname := coalesce(
        new.raw_user_meta_data ->> 'username',
        split_part(coalesce(new.email, ''), '@', 1)
    );
    uname := left(regexp_replace(uname, '[^A-Za-z0-9_]', '', 'g'), 16);
    if length(uname) < 3 then
        uname := 'Player' || substr(new.id::text, 1, 6);
    end if;
    if lower(uname) = 'meon_xd' then
        raise exception 'OWNER_USERNAME_RESERVED';
    end if;

    insert into public.players (id, username)
    values (new.id, uname)
    on conflict (id) do nothing;

    insert into public.player_stats (player_id)
    values (new.id)
    on conflict (player_id) do nothing;

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ------------------------------------------------------------ room code

create or replace function public.generate_room_code()
returns text
language plpgsql
as $$
declare
    alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    code text;
    i integer;
begin
    loop
        code := '';
        for i in 1..6 loop
            code := code || substr(
                alphabet,
                1 + floor(random() * length(alphabet))::integer,
                1
            );
        end loop;
        exit when not exists (
            select 1 from public.game_rooms where room_code = code
        );
    end loop;
    return code;
end;
$$;

-- -------------------------------------------------------------- create

create or replace function public.create_room(p_settings jsonb default '{}'::jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_room public.game_rooms%rowtype;
    v_max integer;
    v_settings jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    v_max := least(greatest(coalesce((p_settings ->> 'max_players')::integer, 4), 2), 4);
    v_settings := jsonb_build_object(
        'max_players', v_max,
        'game_mode', coalesce(p_settings ->> 'game_mode', 'classic')
    );

    update public.game_rooms
       set status = 'finished'
     where host_id = uid and status = 'waiting';

    insert into public.game_rooms (room_code, host_id, settings)
    values (public.generate_room_code(), uid, v_settings)
    returning * into v_room;

    insert into public.room_members (room_id, player_id, role)
    values (v_room.id, uid, 'host')
    on conflict (room_id, player_id) do update set role = 'host';

    return jsonb_build_object(
        'id', v_room.id,
        'room_code', v_room.room_code,
        'status', v_room.status,
        'settings', v_room.settings,
        'host_id', v_room.host_id,
        'role', 'host'
    );
end;
$$;

-- ---------------------------------------------------------------- join

create or replace function public.join_room(p_code text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_code text;
    v_room public.game_rooms%rowtype;
    v_count integer;
    v_max integer;
    v_role text := 'member';
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    v_code := upper(regexp_replace(coalesce(p_code, ''), '[^A-Za-z0-9]', '', 'g'));
    if length(v_code) <> 6 then
        raise exception 'ROOM_CODE_INVALID';
    end if;

    select * into v_room
      from public.game_rooms
     where room_code = v_code
     for update;

    if not found then
        raise exception 'ROOM_NOT_FOUND';
    end if;
    if v_room.status <> 'waiting' then
        raise exception 'ROOM_NOT_WAITING';
    end if;

    if exists (
        select 1 from public.room_members
         where room_id = v_room.id and player_id = uid
    ) then
        v_role := case when v_room.host_id = uid then 'host' else 'member' end;
        return jsonb_build_object(
            'id', v_room.id,
            'room_code', v_room.room_code,
            'status', v_room.status,
            'settings', v_room.settings,
            'host_id', v_room.host_id,
            'role', v_role
        );
    end if;

    select count(*) into v_count
      from public.room_members
     where room_id = v_room.id;

    v_max := least(greatest(coalesce((v_room.settings ->> 'max_players')::integer, 4), 2), 4);
    if v_count >= v_max then
        raise exception 'ROOM_FULL';
    end if;

    insert into public.room_members (room_id, player_id, role)
    values (v_room.id, uid, 'member')
    on conflict (room_id, player_id) do nothing;

    return jsonb_build_object(
        'id', v_room.id,
        'room_code', v_room.room_code,
        'status', v_room.status,
        'settings', v_room.settings,
        'host_id', v_room.host_id,
        'role', 'member'
    );
end;
$$;

-- --------------------------------------------------------------- leave

create or replace function public.leave_room(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    delete from public.room_members
     where room_id = p_room_id and player_id = uid;

    if exists (
        select 1 from public.game_rooms
         where id = p_room_id and host_id = uid
    ) then
        update public.game_rooms
           set status = 'finished'
         where id = p_room_id;
        delete from public.room_members where room_id = p_room_id;
    end if;

    return jsonb_build_object('ok', true);
end;
$$;

-- --------------------------------------------------------------- start

create or replace function public.start_room(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_room public.game_rooms%rowtype;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    select * into v_room
      from public.game_rooms
     where id = p_room_id and host_id = uid
     for update;

    if not found then
        raise exception 'NOT_HOST';
    end if;
    if v_room.status <> 'waiting' then
        raise exception 'ROOM_NOT_WAITING';
    end if;

    update public.game_rooms
       set status = 'playing'
     where id = p_room_id;

    return jsonb_build_object('id', p_room_id, 'status', 'playing');
end;
$$;

-- -------------------------------------------------------------- finish

create or replace function public.finish_room(p_room_id uuid, p_result jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;

    if not exists (
        select 1 from public.game_rooms
         where id = p_room_id and host_id = uid
    ) then
        raise exception 'NOT_HOST';
    end if;

    update public.game_rooms
       set status = 'finished'
     where id = p_room_id;

    insert into public.game_sessions (room_id, result, started_at, ended_at)
    values (
        p_room_id,
        coalesce(p_result, '{}'::jsonb),
        nullif(p_result ->> 'started_at', '')::timestamptz,
        now()
    );

    update public.player_stats ps
       set total_games = ps.total_games + 1,
           wins = ps.wins + case
               when coalesce((p_result -> 'winner_ids') ? ps.player_id::text, false)
               then 1 else 0 end,
           losses = ps.losses + case
               when coalesce((p_result -> 'winner_ids') ? ps.player_id::text, false)
               then 0 else 1 end
      from public.room_members rm
     where rm.room_id = p_room_id
       and rm.player_id = ps.player_id;

    return jsonb_build_object('ok', true);
end;
$$;

-- --------------------------------------------------------------- RLS

alter table public.players enable row level security;
alter table public.game_rooms enable row level security;
alter table public.room_members enable row level security;
alter table public.game_sessions enable row level security;
alter table public.player_stats enable row level security;

drop policy if exists players_select on public.players;
create policy players_select on public.players
    for select to authenticated using (true);

drop policy if exists players_update_own on public.players;
create policy players_update_own on public.players
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

drop policy if exists rooms_select on public.game_rooms;
create policy rooms_select on public.game_rooms
    for select to authenticated using (true);

drop policy if exists members_select on public.room_members;
create policy members_select on public.room_members
    for select to authenticated using (true);

drop policy if exists sessions_select on public.game_sessions;
create policy sessions_select on public.game_sessions
    for select to authenticated
    using (
        exists (
            select 1 from public.room_members rm
             where rm.room_id = game_sessions.room_id
               and rm.player_id = auth.uid()
        )
    );

drop policy if exists stats_select on public.player_stats;
create policy stats_select on public.player_stats
    for select to authenticated using (true);

-- Direct writes are intentionally not allowed. Use the RPCs above.

revoke all on function public.create_room(jsonb) from public;
revoke all on function public.join_room(text) from public;
revoke all on function public.leave_room(uuid) from public;
revoke all on function public.start_room(uuid) from public;
revoke all on function public.finish_room(uuid, jsonb) from public;

grant execute on function public.create_room(jsonb) to authenticated;
grant execute on function public.join_room(text) to authenticated;
grant execute on function public.leave_room(uuid) to authenticated;
grant execute on function public.start_room(uuid) to authenticated;
grant execute on function public.finish_room(uuid, jsonb) to authenticated;

-- ------------------------------------------- private realtime channels

drop policy if exists room_realtime_members on realtime.messages;
create policy room_realtime_members on realtime.messages
    for all to authenticated
    using (
        realtime.topic() like 'room:%'
        and exists (
            select 1 from public.room_members rm
             where rm.player_id = auth.uid()
               and rm.room_id::text = split_part(realtime.topic(), ':', 2)
        )
    )
    with check (
        realtime.topic() like 'room:%'
        and exists (
            select 1 from public.room_members rm
             where rm.player_id = auth.uid()
               and rm.room_id::text = split_part(realtime.topic(), ':', 2)
        )
    );

-- --------------------------------------------------- social expansion

alter table public.players add column if not exists level integer not null default 1;
alter table public.players add column if not exists max_stage integer not null default 0;
alter table public.players add column if not exists gold bigint not null default 0;
alter table public.players add column if not exists verified boolean not null default false;
alter table public.players add column if not exists last_seen timestamptz not null default now();

drop policy if exists players_update_own on public.players;

create table if not exists public.friendships (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.players(id) on delete cascade,
    addressee_id uuid not null references public.players(id) on delete cascade,
    status text not null default 'pending'
        check (status in ('pending', 'accepted', 'blocked')),
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    check (requester_id <> addressee_id)
);
create unique index if not exists friendships_pair_idx
    on public.friendships (
        least(requester_id, addressee_id),
        greatest(requester_id, addressee_id)
    );
create index if not exists friendships_addressee_idx
    on public.friendships (addressee_id, status);

create table if not exists public.game_invites (
    id uuid primary key default gen_random_uuid(),
    from_id uuid not null references public.players(id) on delete cascade,
    to_id uuid not null references public.players(id) on delete cascade,
    room_id uuid not null references public.game_rooms(id) on delete cascade,
    room_code text not null,
    status text not null default 'pending'
        check (status in ('pending', 'accepted', 'declined', 'expired')),
    created_at timestamptz not null default now(),
    responded_at timestamptz
);
create index if not exists game_invites_to_idx
    on public.game_invites (to_id, status, created_at desc);
create index if not exists game_invites_room_idx
    on public.game_invites (room_id);

create or replace function public.sync_player_profile(
    p_level integer,
    p_max_stage integer,
    p_gold bigint
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_name text;
    v_verified boolean;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select username into v_name from public.players where id = uid;
    v_verified := lower(coalesce(v_name, '')) = 'meon_xd';
    update public.players
       set level = greatest(coalesce(p_level, 1), 1),
           max_stage = least(greatest(coalesce(p_max_stage, 0), 0), 24),
           gold = greatest(coalesce(p_gold, 0), 0),
           verified = v_verified,
           last_seen = now()
     where id = uid;
    return jsonb_build_object('ok', true, 'verified', v_verified);
end;
$$;

create or replace function public.get_leaderboard(p_limit integer default 20)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_limit integer;
    v_result jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    v_limit := least(greatest(coalesce(p_limit, 20), 1), 50);
    select coalesce(jsonb_agg(row_to_json(q) order by q.rank), '[]'::jsonb)
      into v_result
      from (
        select
            row_number() over (
                order by p.level desc, coalesce(ps.wins, 0) desc,
                         p.max_stage desc, lower(p.username)
            )::integer as rank,
            p.id,
            p.username,
            p.level,
            coalesce(ps.wins, 0) as wins,
            coalesce(ps.total_games, 0) as total_games,
            p.max_stage,
            p.verified,
            (p.id = uid) as is_me
          from public.players p
          left join public.player_stats ps on ps.player_id = p.id
         order by p.level desc, coalesce(ps.wins, 0) desc, p.max_stage desc, lower(p.username)
         limit v_limit
      ) q;
    return v_result;
end;
$$;

create or replace function public.search_players(p_query text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    q text;
    v_result jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    q := trim(coalesce(p_query, ''));
    if length(q) < 2 then
        raise exception 'QUERY_TOO_SHORT';
    end if;
    select coalesce(jsonb_agg(row_to_json(x) order by x.username), '[]'::jsonb)
      into v_result
      from (
        select
            p.id,
            p.username,
            p.level,
            p.max_stage,
            p.verified,
            coalesce(ps.wins, 0) as wins,
            case
                when f.status = 'pending' and f.requester_id = uid then 'pending_out'
                when f.status = 'pending' then 'pending_in'
                else coalesce(f.status, 'none')
            end as friend_status
          from public.players p
          left join public.player_stats ps on ps.player_id = p.id
          left join lateral (
              select ff.status, ff.requester_id
                from public.friendships ff
               where (ff.requester_id = uid and ff.addressee_id = p.id)
                  or (ff.requester_id = p.id and ff.addressee_id = uid)
               order by ff.created_at desc
               limit 1
          ) f on true
         where p.id <> uid
           and p.username ilike '%' || q || '%'
         order by p.username
         limit 20
      ) x;
    return v_result;
end;
$$;

create or replace function public.send_friend_request(p_username text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    target public.players%rowtype;
    f public.friendships%rowtype;
    v_status text;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select * into target
      from public.players
     where lower(username) = lower(trim(coalesce(p_username, '')))
     limit 1;
    if not found then
        raise exception 'PLAYER_NOT_FOUND';
    end if;
    if target.id = uid then
        raise exception 'CANNOT_ADD_SELF';
    end if;

    select * into f
      from public.friendships
     where (requester_id = uid and addressee_id = target.id)
        or (requester_id = target.id and addressee_id = uid)
     order by created_at desc
     limit 1;

    if found then
        if f.status = 'accepted' then
            return jsonb_build_object('status', 'accepted', 'message', 'Sudah berteman.');
        elsif f.status = 'blocked' then
            return jsonb_build_object('status', 'blocked', 'message', 'Tidak dapat menambahkan pemain ini.');
        elsif f.requester_id = uid then
            return jsonb_build_object('status', 'pending', 'message', 'Permintaan sudah dikirim.');
        else
            update public.friendships
               set status = 'accepted', responded_at = now()
             where id = f.id;
            return jsonb_build_object('status', 'accepted', 'message', 'Permintaan teman diterima.');
        end if;
    end if;

    insert into public.friendships (requester_id, addressee_id, status)
    values (uid, target.id, 'pending');
    v_status := 'pending';
    return jsonb_build_object('status', v_status, 'message', 'Permintaan teman dikirim.');
end;
$$;

create or replace function public.respond_friend_request(
    p_friendship_id uuid,
    p_accept boolean
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    f public.friendships%rowtype;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select * into f
      from public.friendships
     where id = p_friendship_id
       and addressee_id = uid
       and status = 'pending'
     for update;
    if not found then
        raise exception 'REQUEST_NOT_FOUND';
    end if;
    if p_accept then
        update public.friendships
           set status = 'accepted', responded_at = now()
         where id = f.id;
        return jsonb_build_object('ok', true, 'status', 'accepted');
    end if;
    delete from public.friendships where id = f.id;
    return jsonb_build_object('ok', true, 'status', 'declined');
end;
$$;

create or replace function public.list_friends()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_result jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select coalesce(jsonb_agg(row_to_json(x) order by x.username), '[]'::jsonb)
      into v_result
      from (
        select
            p.id,
            p.username,
            p.level,
            p.verified,
            coalesce(ps.wins, 0) as wins,
            f.created_at
          from public.friendships f
          join public.players p
            on p.id = case when f.requester_id = uid then f.addressee_id else f.requester_id end
          left join public.player_stats ps on ps.player_id = p.id
         where f.status = 'accepted'
           and (f.requester_id = uid or f.addressee_id = uid)
         order by p.username
      ) x;
    return v_result;
end;
$$;

create or replace function public.list_friend_requests()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_result jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select coalesce(jsonb_agg(row_to_json(x) order by x.created_at desc), '[]'::jsonb)
      into v_result
      from (
        select
            f.id,
            p.id as player_id,
            p.username,
            p.level,
            p.verified,
            f.created_at
          from public.friendships f
          join public.players p on p.id = f.requester_id
         where f.addressee_id = uid
           and f.status = 'pending'
         order by f.created_at desc
      ) x;
    return v_result;
end;
$$;

create or replace function public.send_game_invite(
    p_friend_id uuid,
    p_room_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_room public.game_rooms%rowtype;
    v_id uuid;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    if not exists (
        select 1 from public.friendships
         where status = 'accepted'
           and ((requester_id = uid and addressee_id = p_friend_id)
             or (requester_id = p_friend_id and addressee_id = uid))
    ) then
        raise exception 'NOT_FRIENDS';
    end if;
    select * into v_room
      from public.game_rooms
     where id = p_room_id and host_id = uid and status = 'waiting';
    if not found then
        raise exception 'NOT_HOST';
    end if;
    update public.game_invites
       set status = 'expired', responded_at = now()
     where status = 'pending' and created_at < now() - interval '2 minutes';

    select id into v_id
      from public.game_invites
     where from_id = uid and to_id = p_friend_id
       and room_id = p_room_id and status = 'pending'
     order by created_at desc
     limit 1;
    if v_id is not null then
        return jsonb_build_object('id', v_id, 'ok', true);
    end if;

    insert into public.game_invites (from_id, to_id, room_id, room_code)
    values (uid, p_friend_id, v_room.id, v_room.room_code)
    returning id into v_id;
    return jsonb_build_object('id', v_id, 'ok', true);
end;
$$;

create or replace function public.list_game_invites()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_result jsonb;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    update public.game_invites
       set status = 'expired', responded_at = now()
     where status = 'pending' and created_at < now() - interval '2 minutes';

    select coalesce(jsonb_agg(row_to_json(x) order by x.created_at desc), '[]'::jsonb)
      into v_result
      from (
        select
            gi.id,
            gi.from_id,
            p.username as from_name,
            gi.room_id,
            gi.room_code,
            gi.created_at
          from public.game_invites gi
          join public.players p on p.id = gi.from_id
         where gi.to_id = uid
           and gi.status = 'pending'
         order by gi.created_at desc
         limit 10
      ) x;
    return v_result;
end;
$$;

create or replace function public.respond_game_invite(
    p_invite_id uuid,
    p_accept boolean
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
    v_invite public.game_invites%rowtype;
    v_code text;
begin
    if uid is null then
        raise exception 'AUTH_REQUIRED';
    end if;
    select * into v_invite
      from public.game_invites
     where id = p_invite_id and to_id = uid and status = 'pending'
     for update;
    if not found then
        raise exception 'INVITE_NOT_FOUND';
    end if;
    if p_accept then
        select room_code into v_code from public.game_rooms where id = v_invite.room_id;
        update public.game_invites
           set status = 'accepted', responded_at = now()
         where id = v_invite.id;
        return jsonb_build_object('ok', true, 'room_code', v_code);
    end if;
    update public.game_invites
       set status = 'declined', responded_at = now()
     where id = v_invite.id;
    return jsonb_build_object('ok', true);
end;
$$;

alter table public.friendships enable row level security;
alter table public.game_invites enable row level security;

drop policy if exists friendships_select on public.friendships;
create policy friendships_select on public.friendships
    for select to authenticated
    using (requester_id = auth.uid() or addressee_id = auth.uid());

drop policy if exists invites_select on public.game_invites;
create policy invites_select on public.game_invites
    for select to authenticated
    using (from_id = auth.uid() or to_id = auth.uid());

revoke all on public.friendships from anon, authenticated;
revoke all on public.game_invites from anon, authenticated;

revoke all on function public.sync_player_profile(integer, integer, bigint) from public;
revoke all on function public.get_leaderboard(integer) from public;
revoke all on function public.search_players(text) from public;
revoke all on function public.send_friend_request(text) from public;
revoke all on function public.respond_friend_request(uuid, boolean) from public;
revoke all on function public.list_friends() from public;
revoke all on function public.list_friend_requests() from public;
revoke all on function public.send_game_invite(uuid, uuid) from public;
revoke all on function public.list_game_invites() from public;
revoke all on function public.respond_game_invite(uuid, boolean) from public;

grant execute on function public.sync_player_profile(integer, integer, bigint) to authenticated;
grant execute on function public.get_leaderboard(integer) to authenticated;
grant execute on function public.search_players(text) to authenticated;
grant execute on function public.send_friend_request(text) to authenticated;
grant execute on function public.respond_friend_request(uuid, boolean) to authenticated;
grant execute on function public.list_friends() to authenticated;
grant execute on function public.list_friend_requests() to authenticated;
grant execute on function public.send_game_invite(uuid, uuid) to authenticated;
grant execute on function public.list_game_invites() to authenticated;
grant execute on function public.respond_game_invite(uuid, boolean) to authenticated;
