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
