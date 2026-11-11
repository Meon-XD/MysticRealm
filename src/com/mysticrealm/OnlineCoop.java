package com.mysticrealm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineCoop extends LanCoop implements RealtimeClient.Listener {

    public volatile boolean loggedIn;
    public volatile boolean busy;
    public volatile String username = "";
    public volatile String roomId;
    public volatile String roomCode = "";
    public volatile String hostId = "";
    public volatile int maxPlayers = 4;
    public volatile int lobbyStage;
    public volatile String socialMessage = "";
    public volatile boolean socialBusy;
    public volatile List<PlayerRow> leaderboard = new ArrayList<>();
    public volatile List<PlayerRow> searchResults = new ArrayList<>();
    public volatile List<FriendRow> friends = new ArrayList<>();
    public volatile List<FriendRow> requests = new ArrayList<>();
    public volatile InviteRow pendingInvite;
    public volatile String selectedFriendId = "";

    private volatile String inviteJoinCode = "";
    private volatile long lastInvitePoll;
    private volatile SupabaseAuth.Session session;
    private volatile RealtimeClient ws;
    private volatile boolean closing;
    private final ConcurrentHashMap<String, Remote> members = new ConcurrentHashMap<>();

    public static class PlayerRow {
        public String id = "";
        public String username = "";
        public int level = 1;
        public int wins;
        public int totalGames;
        public int maxStage;
        public boolean verified;
        public boolean me;
        public int rank;
        public String friendStatus = "none";
    }

    public static class FriendRow {
        public String id = "";
        public String username = "";
        public int level = 1;
        public int wins;
        public boolean verified;
        public String createdAt = "";
    }

    public static class InviteRow {
        public String id = "";
        public String fromId = "";
        public String fromName = "";
        public String roomId = "";
        public String roomCode = "";
        public String createdAt = "";
    }

    public OnlineCoop(SaveData save) {
        super(save);
        lobbyStage = Gfx.clamp(save.maxStage, 0, 24);
    }

    public SupabaseAuth.Session session() {
        return session;
    }

    public boolean hasRoom() {
        return roomId != null && roomId.length() > 0;
    }

    public int memberCount() {
        return members.size();
    }

    public void signIn(final String user, final String password) {
        if (busy) return;
        busy = true;
        error = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    applySession(SupabaseAuth.signIn(user, password));
                    error = null;
                } catch (Exception e) {
                    error = shortError(e);
                } finally {
                    busy = false;
                }
            }
        }, "online-login").start();
    }

    public void signUp(final String user, final String password) {
        if (busy) return;
        busy = true;
        error = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    applySession(SupabaseAuth.signUp(user, password));
                    error = null;
                } catch (Exception e) {
                    error = shortError(e);
                } finally {
                    busy = false;
                }
            }
        }, "online-register").start();
    }

    public void logout() {
        leaveRoom();
        session = null;
        loggedIn = false;
        username = "";
        save.ownerMode = false;
        save.save();
        members.clear();
        leaderboard = new ArrayList<>();
        searchResults = new ArrayList<>();
        friends = new ArrayList<>();
        requests = new ArrayList<>();
        pendingInvite = null;
        socialMessage = "";
        error = null;
    }

    public void syncProfile() {
        if (!loggedIn || session == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    SupabaseAuth.Session active = session;
                    if (active == null) return;
                    JSONObject args = new JSONObject();
                    args.put("p_level", Math.max(1, save.level));
                    args.put("p_max_stage", Gfx.clamp(save.maxStage, 0, 24));
                    args.put("p_gold", Math.max(0, save.gold));
                    JSONObject profile = SupabaseAuth.rpc("sync_player_profile", args, active.accessToken);
                    if (!loggedIn || session != active) return;
                    boolean verified = profile.optBoolean("verified", false);
                    save.ownerMode = verified;
                    if (verified) save.applyOwnerBoosts();
                    save.save();
                } catch (Exception ignored) {
                }
            }
        }, "online-profile").start();
    }

    public void fetchLeaderboard() {
        if (!loggedIn) return;
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONArray arr = SupabaseAuth.rpcArray("get_leaderboard", new JSONObject(), session.accessToken);
                    ArrayList<PlayerRow> rows = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null) rows.add(playerRow(o));
                    }
                    leaderboard = rows;
                    socialMessage = rows.isEmpty() ? "Belum ada peringkat." : "";
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-leaderboard").start();
    }

    public void searchPlayers(final String query) {
        if (!loggedIn) return;
        final String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            socialMessage = "Ketik minimal 2 huruf.";
            return;
        }
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_query", q);
                    JSONArray arr = SupabaseAuth.rpcArray("search_players", args, session.accessToken);
                    ArrayList<PlayerRow> rows = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null) rows.add(playerRow(o));
                    }
                    searchResults = rows;
                    socialMessage = rows.isEmpty() ? "Pemain tidak ditemukan." : "";
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-search").start();
    }

    public void sendFriendRequest(final String target) {
        if (!loggedIn) return;
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_username", target);
                    JSONObject result = SupabaseAuth.rpc("send_friend_request", args, session.accessToken);
                    socialMessage = result.optString("message", "Permintaan dikirim.");
                    loadFriendsInternal();
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-friend-request").start();
    }

    public void respondFriendRequest(final String id, final boolean accept) {
        if (!loggedIn) return;
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_friendship_id", id);
                    args.put("p_accept", accept);
                    SupabaseAuth.rpc("respond_friend_request", args, session.accessToken);
                    socialMessage = accept ? "Pertemanan diterima." : "Permintaan ditolak.";
                    loadFriendsInternal();
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-friend-response").start();
    }

    public void fetchFriends() {
        if (!loggedIn) return;
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    loadFriendsInternal();
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-friends").start();
    }

    public void sendInvite(final String friendId) {
        if (!loggedIn) return;
        if (!hasRoom() || !host) {
            socialMessage = "Buat room sebagai host dulu.";
            return;
        }
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_friend_id", friendId);
                    args.put("p_room_id", roomId);
                    SupabaseAuth.rpc("send_game_invite", args, session.accessToken);
                    socialMessage = "Undangan dikirim.";
                } catch (Exception e) {
                    socialMessage = shortError(e);
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-invite").start();
    }

    public void respondInvite(final String id, final boolean accept) {
        if (!loggedIn) return;
        InviteRow current = pendingInvite;
        if (current != null && current.id.equals(id)) pendingInvite = null;
        socialBusy = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_invite_id", id);
                    args.put("p_accept", accept);
                    JSONObject result = SupabaseAuth.rpc("respond_game_invite", args, session.accessToken);
                    if (accept) {
                        inviteJoinCode = result.optString("room_code", "");
                        if (inviteJoinCode.length() == 0 && current != null) inviteJoinCode = current.roomCode;
                        socialMessage = inviteJoinCode.length() == 0 ? "Room tidak tersedia." : "Bergabung ke room teman.";
                    } else {
                        socialMessage = "Undangan ditolak.";
                    }
                } catch (Exception e) {
                    socialMessage = shortError(e);
                    if (current != null) pendingInvite = current;
                } finally {
                    socialBusy = false;
                }
            }
        }, "online-invite-response").start();
    }

    public String takeInviteJoinCode() {
        String code = inviteJoinCode;
        inviteJoinCode = "";
        return code;
    }

    public void fetchInvites() {
        if (!loggedIn) return;
        loadInvites(true);
    }

    private void loadInvites(final boolean manual) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONArray arr = SupabaseAuth.rpcArray("list_game_invites", new JSONObject(), session.accessToken);
                    ArrayList<InviteRow> rows = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        InviteRow r = new InviteRow();
                        r.id = o.optString("id", "");
                        r.fromId = o.optString("from_id", "");
                        r.fromName = o.optString("from_name", "Pemain");
                        r.roomId = o.optString("room_id", "");
                        r.roomCode = o.optString("room_code", "");
                        r.createdAt = o.optString("created_at", "");
                        if (r.id.length() > 0) rows.add(r);
                    }
                    InviteRow current = pendingInvite;
                    InviteRow match = null;
                    for (int i = 0; i < rows.size(); i++) {
                        if (current != null && rows.get(i).id.equals(current.id)) match = rows.get(i);
                    }
                    if (match != null) pendingInvite = match;
                    else if (current == null && !rows.isEmpty()) pendingInvite = rows.get(0);
                    else if (current != null) pendingInvite = null;
                    if (manual && rows.isEmpty()) socialMessage = "Tidak ada undangan.";
                } catch (Exception e) {
                    if (manual) socialMessage = shortError(e);
                }
            }
        }, "online-invites").start();
    }

    private void loadFriendsInternal() throws Exception {
        JSONArray fa = SupabaseAuth.rpcArray("list_friends", new JSONObject(), session.accessToken);
        ArrayList<FriendRow> fr = new ArrayList<>();
        for (int i = 0; i < fa.length(); i++) {
            JSONObject o = fa.optJSONObject(i);
            if (o != null) fr.add(friendRow(o));
        }
        JSONArray ra = SupabaseAuth.rpcArray("list_friend_requests", new JSONObject(), session.accessToken);
        ArrayList<FriendRow> rq = new ArrayList<>();
        for (int i = 0; i < ra.length(); i++) {
            JSONObject o = ra.optJSONObject(i);
            if (o != null) rq.add(friendRow(o));
        }
        friends = fr;
        requests = rq;
    }

    private static PlayerRow playerRow(JSONObject o) {
        PlayerRow r = new PlayerRow();
        r.id = o.optString("id", "");
        r.username = o.optString("username", "");
        r.level = Math.max(1, o.optInt("level", 1));
        r.wins = Math.max(0, o.optInt("wins", 0));
        r.totalGames = Math.max(0, o.optInt("total_games", 0));
        r.maxStage = Math.max(0, o.optInt("max_stage", 0));
        r.verified = o.optBoolean("verified", false);
        r.me = o.optBoolean("is_me", false);
        r.rank = Math.max(0, o.optInt("rank", 0));
        r.friendStatus = o.optString("friend_status", "none");
        return r;
    }

    private static FriendRow friendRow(JSONObject o) {
        FriendRow r = new FriendRow();
        r.id = o.optString("id", "");
        r.username = o.optString("username", "");
        r.level = Math.max(1, o.optInt("level", 1));
        r.wins = Math.max(0, o.optInt("wins", 0));
        r.verified = o.optBoolean("verified", false);
        r.createdAt = o.optString("created_at", "");
        return r;
    }

    public void createRoom(final int limit) {
        if (!loggedIn) {
            error = "Login dulu.";
            return;
        }
        if (busy) return;
        busy = true;
        error = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject settings = new JSONObject();
                    settings.put("max_players", Gfx.clamp(limit, 2, 4));
                    settings.put("game_mode", "classic");
                    JSONObject args = new JSONObject();
                    args.put("p_settings", settings);
                    JSONObject result = SupabaseAuth.rpc("create_room", args, session.accessToken);
                    roomId = result.optString("id", "");
                    roomCode = result.optString("room_code", "");
                    hostId = result.optString("host_id", userId());
                    maxPlayers = Gfx.clamp(limit, 2, 4);
                    host = true;
                    lobbyStage = Gfx.clamp(save.maxStage, 0, 24);
                    localStage = lobbyStage;
                    if (roomId.length() == 0) throw new Exception("Room gagal dibuat.");
                    ensureSelfMember();
                    connectRealtime();
                    error = null;
                } catch (Exception e) {
                    error = shortError(e);
                } finally {
                    busy = false;
                }
            }
        }, "online-create").start();
    }

    public void joinRoom(final String code) {
        if (!loggedIn) {
            error = "Login dulu.";
            return;
        }
        if (busy) return;
        final String clean = code == null ? "" : code.trim().toUpperCase();
        if (!clean.matches("[A-Z0-9]{6}")) {
            error = "Kode room harus 6 karakter.";
            return;
        }
        busy = true;
        error = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_code", clean);
                    JSONObject result = SupabaseAuth.rpc("join_room", args, session.accessToken);
                    roomId = result.optString("id", "");
                    roomCode = result.optString("room_code", clean);
                    hostId = result.optString("host_id", "");
                    host = "host".equals(result.optString("role", "member"));
                    JSONObject settings = result.optJSONObject("settings");
                    maxPlayers = settings == null ? 4 : Gfx.clamp(settings.optInt("max_players", 4), 2, 4);
                    lobbyStage = Gfx.clamp(save.maxStage, 0, 24);
                    localStage = lobbyStage;
                    if (roomId.length() == 0) throw new Exception("Gagal masuk room.");
                    ensureSelfMember();
                    connectRealtime();
                    error = null;
                } catch (Exception e) {
                    error = shortError(e);
                } finally {
                    busy = false;
                }
            }
        }, "online-join").start();
    }

    public void startGame(final int stage) {
        if (!host) {
            error = "Hanya host yang bisa memulai.";
            return;
        }
        if (!hasRoom()) {
            error = "Belum ada room.";
            return;
        }
        if (!connected) {
            error = "Realtime belum terhubung.";
            return;
        }
        if (busy) return;
        busy = true;
        error = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureSession();
                    JSONObject args = new JSONObject();
                    args.put("p_room_id", roomId);
                    SupabaseAuth.rpc("start_room", args, session.accessToken);
                    lobbyStage = Gfx.clamp(stage, 0, 24);
                    localStage = lobbyStage;
                    stageRequest = lobbyStage;
                    JSONObject data = new JSONObject();
                    data.put("stage", lobbyStage);
                    data.put("hostId", hostId);
                    data.put("userId", userId());
                    sendBroadcast("start", data);
                    error = null;
                } catch (Exception e) {
                    error = shortError(e);
                } finally {
                    busy = false;
                }
            }
        }, "online-start").start();
    }

    public void leaveRoom() {
        String oldRoom = roomId;
        String uid = userId();
        closing = true;
        if (ws != null && oldRoom != null && uid.length() > 0) {
            JSONObject data = new JSONObject();
            try {
                data.put("userId", uid);
            } catch (Exception ignored) {
            }
            sendBroadcast("leave", data);
        }
        connected = false;
        if (ws != null) {
            ws.close();
            ws = null;
        }
        roomId = null;
        roomCode = "";
        hostId = "";
        host = false;
        members.clear();
        lobbyStage = 0;
        closing = false;
        if (oldRoom != null && uid.length() > 0 && session != null) {
            final String rid = oldRoom;
            final String token = session.accessToken;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject args = new JSONObject();
                        args.put("p_room_id", rid);
                        SupabaseAuth.rpc("leave_room", args, token);
                    } catch (Exception ignored) {
                    }
                }
            }, "online-leave").start();
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (loggedIn && session != null && !socialBusy && now - lastInvitePoll > 3000L) {
            lastInvitePoll = now;
            loadInvites(false);
        }
        Iterator<Remote> it = members.values().iterator();
        while (it.hasNext()) {
            Remote r = it.next();
            if (r.self) continue;
            if (r.id.equals(hostId)) continue;
            if (now - r.lastSeen > 30000L) it.remove();
        }
    }

    @Override
    public List<Remote> remotes() {
        return new ArrayList<>(members.values());
    }

    @Override
    public void sendStage(int stage) {
        localStage = Gfx.clamp(stage, 0, 24);
        updateSelf();
        JSONObject data = new JSONObject();
        try {
            data.put("stage", localStage);
            data.put("userId", userId());
        } catch (Exception ignored) {
        }
        sendBroadcast("stage", data);
    }

    @Override
    public void setLocalStage(int stage) {
        localStage = Gfx.clamp(stage, 0, 24);
        updateSelf();
    }

    @Override
    public void sendState(Player player) {
        if (!connected || ws == null || roomId == null || player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastStateAt < 70L) return;
        lastStateAt = now;
        updateSelf(player);
        JSONObject data = new JSONObject();
        try {
            data.put("userId", userId());
            data.put("name", username);
            data.put("x", round(player.x));
            data.put("y", round(player.y));
            data.put("dir", round(player.dir));
            data.put("hp", round(player.hp));
            data.put("maxHp", round(player.maxHp));
            data.put("class", save.classIndex);
            data.put("level", save.level);
            data.put("moving", player.moving);
            data.put("stage", localStage);
        } catch (Exception ignored) {
        }
        sendBroadcast("state", data);
    }

    @Override
    public void close() {
        leaveRoom();
        super.close();
    }

    @Override
    public void onConnected() {
        sendJoin();
    }

    @Override
    public void onEvent(String event, JSONObject payload) {
        if ("phx_reply".equals(event)) {
            if ("ok".equals(payload.optString("status", ""))) {
                connected = true;
                ensureSelfMember();
                sendHello(false);
            } else {
                connected = false;
                JSONObject response = payload.optJSONObject("response");
                String reason = response == null ? "" : response.optString("reason", "");
                error = reason.length() == 0 ? "Gagal masuk channel room." : "Gagal masuk channel: " + reason;
            }
            return;
        }
        if (!"broadcast".equals(event)) return;
        String name = payload.optString("event", "");
        JSONObject data = payload.optJSONObject("payload");
        handleBroadcast(name, data == null ? new JSONObject() : data);
    }

    @Override
    public void onDisconnected(String message) {
        if (closing) return;
        connected = false;
        if (error == null) error = message == null ? "Koneksi online terputus." : message;
    }

    private void handleBroadcast(String event, JSONObject data) {
        String uid = data.optString("userId", "");
        if ("hello".equals(event)) {
            if (uid.length() == 0 || uid.equals(userId())) return;
            Remote r = member(uid);
            r.name = cleanName(data.optString("name", "Partner"));
            r.classIndex = Gfx.clamp(data.optInt("class", 0), 0, 2);
            r.level = Math.max(1, data.optInt("level", 1));
            r.stage = data.optInt("stage", -1);
            r.lastSeen = System.currentTimeMillis();
            if (!data.optBoolean("reply", false)) sendHello(true);
        } else if ("state".equals(event)) {
            if (uid.length() == 0 || uid.equals(userId())) return;
            Remote r = member(uid);
            r.name = cleanName(data.optString("name", r.name.length() == 0 ? "Partner" : r.name));
            r.x = (float) data.optDouble("x", r.x);
            r.y = (float) data.optDouble("y", r.y);
            r.dir = (float) data.optDouble("dir", r.dir);
            r.hp = (float) data.optDouble("hp", r.hp);
            r.maxHp = Math.max(1f, (float) data.optDouble("maxHp", r.maxHp));
            r.classIndex = Gfx.clamp(data.optInt("class", r.classIndex), 0, 2);
            r.level = Math.max(1, data.optInt("level", r.level));
            r.moving = data.optBoolean("moving", false);
            r.stage = data.optInt("stage", r.stage);
            r.lastSeen = System.currentTimeMillis();
        } else if ("stage".equals(event)) {
            int stage = Gfx.clamp(data.optInt("stage", -1), -1, 24);
            if (stage < 0) return;
            if (uid.length() > 0 && !uid.equals(userId())) member(uid).stage = stage;
            if (!host) {
                lobbyStage = stage;
                synchronized (this) {
                    stageRequest = stage;
                }
            }
        } else if ("start".equals(event)) {
            if (!host && uid.length() > 0 && !uid.equals(hostId)) return;
            int stage = Gfx.clamp(data.optInt("stage", 0), 0, 24);
            lobbyStage = stage;
            synchronized (this) {
                stageRequest = stage;
            }
        } else if ("leave".equals(event)) {
            if (uid.length() == 0 || uid.equals(userId())) return;
            members.remove(uid);
            if (uid.equals(hostId) && !host) {
                error = "Host keluar dari room.";
                connected = false;
                if (ws != null) ws.close();
            }
        }
    }

    private void connectRealtime() {
        if (ws != null) ws.close();
        ws = new RealtimeClient(SupabaseConfig.realtimeUrl(), this);
        ws.connect();
    }

    private void sendJoin() {
        if (session == null || roomId == null) return;
        try {
            JSONObject broadcast = new JSONObject();
            broadcast.put("ack", false);
            broadcast.put("self", false);
            JSONObject presence = new JSONObject();
            presence.put("key", userId());
            JSONObject config = new JSONObject();
            config.put("private", true);
            config.put("broadcast", broadcast);
            config.put("presence", presence);
            config.put("postgres_changes", new JSONArray());
            JSONObject payload = new JSONObject();
            payload.put("config", config);
            payload.put("access_token", session.accessToken);
            sendEnvelope("phx_join", payload, "1");
        } catch (Exception e) {
            error = shortError(e);
        }
    }

    private void sendHello(boolean reply) {
        if (!connected || roomId == null) return;
        JSONObject data = new JSONObject();
        try {
            data.put("userId", userId());
            data.put("name", username);
            data.put("class", save.classIndex);
            data.put("level", save.level);
            data.put("stage", localStage);
            data.put("reply", reply);
        } catch (Exception ignored) {
        }
        sendBroadcast("hello", data);
    }

    private void sendBroadcast(String event, JSONObject data) {
        if (ws == null || roomId == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", "broadcast");
            payload.put("event", event);
            payload.put("payload", data == null ? new JSONObject() : data);
            sendEnvelope("broadcast", payload, null);
        } catch (Exception ignored) {
        }
    }

    private void sendEnvelope(String event, JSONObject payload, String ref) {
        RealtimeClient client = ws;
        if (client == null || roomId == null) return;
        try {
            JSONObject envelope = new JSONObject();
            envelope.put("topic", "realtime:room:" + roomId);
            envelope.put("event", event);
            envelope.put("payload", payload);
            envelope.put("ref", ref == null ? JSONObject.NULL : ref);
            envelope.put("join_ref", "1");
            client.send(envelope.toString());
        } catch (Exception ignored) {
        }
    }

    private void ensureSession() throws Exception {
        SupabaseAuth.Session current = session;
        if (current == null) throw new Exception("Login dulu.");
        if (current.expiresAt - System.currentTimeMillis() < 60000L) {
            current = SupabaseAuth.refresh(current);
            applySession(current);
        }
    }

    private void applySession(SupabaseAuth.Session value) {
        session = value;
        String nextUser = value.username == null ? "" : value.username;
        boolean userChanged = !nextUser.equals(username == null ? "" : username);
        username = nextUser;
        loggedIn = true;
        if (userChanged) save.ownerMode = false;
        if (value.username != null && value.username.length() > 0) {
            save.playerName = value.username;
            save.save();
        }
        ensureSelfMember();
        syncProfile();
    }

    private void ensureSelfMember() {
        String uid = userId();
        if (uid.length() == 0) return;
        Remote self = member(uid);
        self.self = true;
        self.name = username;
        self.classIndex = save.classIndex;
        self.level = save.level;
        self.stage = localStage;
        self.lastSeen = System.currentTimeMillis();
    }

    private void updateSelf() {
        updateSelf(null);
    }

    private void updateSelf(Player player) {
        String uid = userId();
        if (uid.length() == 0) return;
        Remote self = member(uid);
        self.self = true;
        self.name = username;
        self.classIndex = save.classIndex;
        self.level = save.level;
        self.stage = localStage;
        self.lastSeen = System.currentTimeMillis();
        if (player != null) {
            self.x = player.x;
            self.y = player.y;
            self.dir = player.dir;
            self.hp = player.hp;
            self.maxHp = player.maxHp;
            self.moving = player.moving;
        }
    }

    private Remote member(String uid) {
        Remote r = members.get(uid);
        if (r == null) {
            r = new Remote();
            r.id = uid;
            r.lastSeen = System.currentTimeMillis();
            members.put(uid, r);
        }
        return r;
    }

    private String userId() {
        SupabaseAuth.Session current = session;
        return current == null ? "" : current.userId;
    }

    private static double round(float value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String shortError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.length() == 0) msg = e.getClass().getSimpleName();
        return msg.length() > 90 ? msg.substring(0, 90) : msg;
    }
}
