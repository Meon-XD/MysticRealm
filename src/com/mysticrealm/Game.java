package com.mysticrealm;

import android.content.Context;
import android.graphics.Canvas;
import android.view.KeyEvent;

import java.util.List;

public class Game {

    public static final int MENU = 0;
    public static final int CLASS_SELECT = 1;
    public static final int WORLD_MAP = 2;
    public static final int PLAY = 3;
    public static final int PAUSE = 4;
    public static final int INVENTORY = 5;
    public static final int SHOP = 6;
    public static final int SKILLS = 7;
    public static final int SETTINGS = 8;
    public static final int RESULT = 9;
    public static final int LAN_LOBBY = 10;
    public static final int LOADING = 11;
    public static final int ONLINE_LOBBY = 12;
    public static final int PROFILE = 13;
    public static final int ACHIEVEMENTS = 14;
    public static final int HELP = 15;
    public static final int FRIENDS = 16;
    public static final int LEADERBOARD = 17;
    public static final int OWNER_PANEL = 18;

    public static final int INPUT_NAME = 1;
    public static final int INPUT_IP = 2;
    public static final int INPUT_USERNAME = 3;
    public static final int INPUT_PASSWORD = 4;
    public static final int INPUT_ROOM_CODE = 5;
    public static final int INPUT_SEARCH = 6;
    public static final int INPUT_OWNER_PASSWORD = 7;
    public static final int INPUT_OWNER_SEARCH = 8;

    public final Context context;
    public final Gfx gfx = new Gfx();
    public final SaveData save;
    public final Audio audio;
    public final Screens screens;

    public Gameplay gp;
    public LanCoop lan;
    public OnlineCoop online;
    public int lanStage = 0;
    public int onlineStage = 0;
    public String lanLastIp = "";
    public String onlineUser = "";
    public String onlinePass = "";
    public String onlineCode = "";
    public int mode = MENU;
    public int mapWorld = 0;
    public int selClass = 0;
    public int backTo = MENU;
    public int itemPage = 0;
    public int shopPage = 0;
    public String selItemId = null;
    public boolean confirmNew = false;
    public String searchQuery = "";
    public String ownerQuery = "";
    public int helpPage = 0;

    public float W = 1920f;
    public float H = 1080f;
    public float uiTime = 0f;

    public String notice = null;
    public float noticeT = 0f;

    public float loadingT = 0f;
    public final float loadingDuration = 3.8f;

    public boolean inputActive = false;
    public int inputPurpose = INPUT_NAME;
    public String inputTitle = "";
    public String inputMessage = "";
    public String inputHint = "";
    public String inputValue = "";
    public int inputMax = 16;
    public boolean inputNumeric = false;
    public boolean inputPassword = false;
    public boolean inputShift = false;
    public boolean inputError = false;
    public boolean cinematicActive = false;

    private int lastMusic = -99;
    private String lanErrorShown = null;
    private String pendingOwnerName = null;

    public Game(Context context) {
        this.context = context;
        gfx.setSize(W, H);
        save = new SaveData(context);
        if (save.isOwner()) save.applyOwnerBoosts();
        audio = new Audio(context);
        audio.setMusicVolume(save.musicVolume);
        audio.setSfxVolume(save.sfxVolume);
        audio.setMusicOn(save.musicVolume > 0.001f);
        audio.setSfxOn(save.sfxVolume > 0.001f);
        screens = new Screens(this);
        online = new OnlineCoop(save);
        selClass = save.classIndex;
        mapWorld = Gfx.clamp(save.currentStage / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1);
        if (save.hasCharacter) mapWorld = Gfx.clamp(save.maxStage / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1);
        mode = LOADING;
        lastMusic = -99;
    }

    // ------------------------------------------------------------ lifecycle

    public void onResume() {
        if (!cinematicActive) audio.resumeMusic();
    }

    public void onPause() {
        save.save();
        audio.pauseMusic();
    }

    public void shutdown() {
        leaveLan();
        if (online != null) online.close();
        save.save();
        audio.shutdown();
    }

    public boolean onBack() {
        if (inputActive) {
            closeTextInput();
            audio.play("click");
            return true;
        }
        if (confirmNew) {
            confirmNew = false;
            return true;
        }
        switch (mode) {
            case LOADING:
                finishLoading();
                return true;
            case MENU:
                return false;
            case CLASS_SELECT:
                mode = MENU;
                return true;
            case WORLD_MAP:
                mode = MENU;
                save.save();
                return true;
            case PLAY:
                if (gp != null) gp.pauseRequested = false;
                mode = PAUSE;
                audio.play("click");
                return true;
            case PAUSE:
                mode = PLAY;
                if (gp != null) gp.pauseRequested = false;
                audio.play("click");
                return true;
            case INVENTORY:
            case SHOP:
            case SKILLS:
            case SETTINGS:
                mode = backTo;
                save.save();
                return true;
            case RESULT:
                gp = null;
                mode = WORLD_MAP;
                return true;
            case LAN_LOBBY:
                leaveLan();
                mode = MENU;
                return true;
            case ONLINE_LOBBY:
                if (online != null) online.leaveRoom();
                lan = null;
                mode = MENU;
                return true;
            case PROFILE:
            case ACHIEVEMENTS:
            case HELP:
            case FRIENDS:
            case LEADERBOARD:
                mode = MENU;
                return true;
            case OWNER_PANEL:
                mode = MENU;
                return true;
            default:
                mode = MENU;
                return true;
        }
    }

    // --------------------------------------------------------------- update

    public void update(float dt) {
        uiTime += dt;
        if (noticeT > 0f) {
            noticeT -= dt;
            if (noticeT <= 0f) {
                noticeT = 0f;
                notice = null;
            }
        }

        if (cinematicActive) return;

        if (mode == LOADING) {
            loadingT += dt;
            if (loadingT >= loadingDuration) finishLoading();
        }

        if (online != null) {
            online.tick();
            if (online.hasRoom()) {
                if (lan != online) lan = online;
            } else if (lan == online) {
                lan = null;
            }
            if ((mode == ONLINE_LOBBY || mode == PLAY) && !online.host && online.hasRoom()) {
                onlineStage = online.lobbyStage;
            }
            String joinCode = online.takeInviteJoinCode();
            if (joinCode.length() > 0) {
                openOnlineLobby();
                online.joinRoom(joinCode);
                notify("Bergabung ke room " + joinCode);
            }
        }

        if ((mode == LAN_LOBBY || mode == ONLINE_LOBBY) && lan != null) {
            if (lan.error != null && !lan.error.equals(lanErrorShown)) {
                lanErrorShown = lan.error;
                notify(lan.error);
            }
            int req = lan.takeStageRequest();
            if (req >= 0) {
                startStage(req, true);
                return;
            }
        }

        if (mode == PLAY && gp != null) {
            if (gp.consumePauseRequest()) {
                mode = PAUSE;
                audio.play("click");
                save.save();
                return;
            }
            gp.update(dt);
            if (gp.finished) {
                save.save();
                if (gp.lan != null) {
                    leaveLan();
                    gp.lan = null;
                }
                if (gp.won && gp.stageIndex == Catalog.STAGE_COUNT - 1) {
                    playCinematic("ending");
                    return;
                }
                mode = RESULT;
                return;
            }
        }

        int theme = -1;
        if (mode == MENU || mode == CLASS_SELECT || mode == LAN_LOBBY
                || mode == ONLINE_LOBBY || mode == PROFILE || mode == ACHIEVEMENTS
                || mode == HELP || mode == FRIENDS || mode == LEADERBOARD) {
            theme = 0;
        } else if (mode == WORLD_MAP || mode == INVENTORY || mode == SHOP
                || mode == SKILLS || mode == SETTINGS) {
            theme = 1 + mapWorld;
        } else if (mode == PLAY && gp != null) {
            theme = gp.boss != null ? 6 : 1 + gp.world.theme;
        }
        if (theme >= 0 && theme != lastMusic) {
            audio.music(theme);
            lastMusic = theme;
        }
    }

    private void finishLoading() {
        if (mode != LOADING) return;
        loadingT = loadingDuration;
        mode = MENU;
        lastMusic = -99;
        audio.play("level");
        if (!save.ownerMode && SaveData.isOwnerName(save.playerName)) {
            pendingOwnerName = save.playerName;
            promptOwnerPassword();
        }
    }

    // ----------------------------------------------------------------- draw

    public void draw(Canvas canvas) {
        gfx.resize(W, H);
        if (mode == PLAY && gp != null) {
            gp.draw(canvas, gfx);
        } else {
            screens.draw(canvas, gfx, mode);
        }
    }

    public void resize(float w, float h) {
        W = w;
        H = h;
        gfx.resize(w, h);
        if (gp != null) {
            gp.viewW = w;
            gp.viewH = h;
            gp.zoom = zoomFor(h);
        }
    }

    private float zoomFor(float h) {
        return Gfx.clamp(h / 720f, 1f, 1.6f) * 0.95f;
    }

    // --------------------------------------------------------------- input

    public void pointerDown(int id, float x, float y) {
        if (mode == LOADING) {
            if (loadingT > 0.55f) finishLoading();
            return;
        }
        if (mode == PLAY && gp != null) {
            gp.pointerDown(id, x, y);
            return;
        }
        int bid = screens.hit(x, y);
        if (inputActive) {
            if (Screens.isInputId(bid)) action(bid);
            return;
        }
        if (bid != Screens.NONE) action(bid);
    }

    public void pointerMove(int id, float x, float y) {
        if (mode == PLAY && gp != null) gp.pointerMove(id, x, y);
    }

    public void pointerUp(int id, float x, float y) {
        if (mode == PLAY && gp != null) gp.pointerUp(id, x, y);
    }

    public void pointerCancel() {
        if (mode == PLAY && gp != null) gp.pointerCancel();
    }

    public boolean keyDown(int code) {
        if (inputActive) return inputKey(code);
        if (mode == LOADING) {
            if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_BACK
                    || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_SPACE) {
                finishLoading();
            }
            return true;
        }
        if (mode == PLAY && gp != null && gp.keyDown(code)) return true;
        if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_BACK) {
            onBack();
            return true;
        }
        return false;
    }

    public void keyUp(int code) {
        if (gp != null) gp.keyUp(code);
    }

    public void mouseMoved(float x, float y) {
        if (mode == PLAY && gp != null) gp.mouseMoved(x, y);
    }

    public void mouseDown(int button, float x, float y) {
        if (mode == PLAY && gp != null) {
            gp.mouseDown(button, x, y);
        } else {
            pointerDown(-100, x, y);
        }
    }

    public void mouseUp(int button, float x, float y) {
        if (mode == PLAY && gp != null) gp.mouseUp(button, x, y);
    }

    public void notify(String msg) {
        notice = msg;
        noticeT = 2.2f;
    }

    // -------------------------------------------------------------- actions

    private void action(int id) {
        if (inputActive) {
            inputAction(id);
            return;
        }
        if (confirmNew && id != Screens.CONFIRM_YES && id != Screens.CONFIRM_NO) return;
        if (id >= Screens.OWNER_ROW_BASE && id < Screens.OWNER_ROW_BASE + 20) {
            int idx = id - Screens.OWNER_ROW_BASE;
            if (online != null && idx < online.adminResults.size()) {
                online.adminSelectedId = online.adminResults.get(idx).id;
                audio.play("click");
            }
            return;
        }
        if (id >= Screens.OWNER_SKILL_DOWN_BASE && id < Screens.OWNER_SKILL_DOWN_BASE + 3) {
            ownerSkill(id - Screens.OWNER_SKILL_DOWN_BASE, -1);
            return;
        }
        if (id >= Screens.OWNER_SKILL_UP_BASE && id < Screens.OWNER_SKILL_UP_BASE + 3) {
            ownerSkill(id - Screens.OWNER_SKILL_UP_BASE, 1);
            return;
        }
        if (id >= Screens.FRIEND_ROW_BASE && id < Screens.FRIEND_ROW_BASE + 20) {
            int idx = id - Screens.FRIEND_ROW_BASE;
            if (online != null && idx < online.friends.size()) {
                online.selectedFriendId = online.friends.get(idx).id;
                audio.play("click");
            }
            return;
        }
        if (id >= Screens.FRIEND_ACCEPT_BASE && id < Screens.FRIEND_ACCEPT_BASE + 20) {
            int idx = id - Screens.FRIEND_ACCEPT_BASE;
            if (online != null && idx < online.requests.size()) {
                online.respondFriendRequest(online.requests.get(idx).id, true);
            }
            return;
        }
        if (id >= Screens.FRIEND_DECLINE_BASE && id < Screens.FRIEND_DECLINE_BASE + 20) {
            int idx = id - Screens.FRIEND_DECLINE_BASE;
            if (online != null && idx < online.requests.size()) {
                online.respondFriendRequest(online.requests.get(idx).id, false);
            }
            return;
        }
        if (id >= Screens.ADD_FRIEND_BASE && id < Screens.ADD_FRIEND_BASE + 20) {
            int idx = id - Screens.ADD_FRIEND_BASE;
            if (online != null && idx < online.searchResults.size()) {
                online.sendFriendRequest(online.searchResults.get(idx).username);
            }
            return;
        }
        if (id >= Screens.ITEM_ROW_BASE) {
            List<String> list = itemList();
            int idx = id - Screens.ITEM_ROW_BASE;
            if (idx >= 0 && idx < list.size()) {
                selItemId = list.get(idx);
                audio.play("click");
            }
            return;
        }
        if (id >= Screens.SHOP_ROW_BASE) {
            buy(id - Screens.SHOP_ROW_BASE);
            return;
        }
        if (id >= Screens.CLASS_BASE && id < Screens.CLASS_BASE + 3) {
            selClass = id - Screens.CLASS_BASE;
            audio.play("click");
            return;
        }
        if (id >= Screens.STAGE_BASE && id < Screens.STAGE_BASE + 5) {
            startStage(mapWorld * Catalog.STAGES_PER_WORLD + (id - Screens.STAGE_BASE));
            return;
        }
        if (id >= Screens.SKILL_UP_BASE && id < Screens.SKILL_UP_BASE + 3) {
            upgradeSkill(id - Screens.SKILL_UP_BASE);
            return;
        }
        if (id >= Screens.TRACK_UP_BASE && id < Screens.TRACK_UP_BASE + 6) {
            upgradeTrack(id - Screens.TRACK_UP_BASE);
            return;
        }

        switch (id) {
            case Screens.BACK:
                onBack();
                audio.play("click");
                break;
            case Screens.CONTINUE:
                mapWorld = Gfx.clamp(save.maxStage / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1);
                mode = WORLD_MAP;
                audio.play("click");
                break;
            case Screens.NEW_GAME:
                if (save.hasCharacter) {
                    confirmNew = true;
                    audio.play("click");
                } else {
                    confirmNew = false;
                    selClass = save.classIndex;
                    mode = CLASS_SELECT;
                    audio.play("click");
                }
                break;
            case Screens.CONFIRM_YES:
                confirmNew = false;
                selClass = save.classIndex;
                mode = CLASS_SELECT;
                audio.play("click");
                break;
            case Screens.CONFIRM_NO:
                confirmNew = false;
                audio.play("click");
                break;
            case Screens.START:
                save.reset(selClass);
                save.save();
                mapWorld = 0;
                audio.play("level");
                playCinematic("opening");
                break;
            case Screens.QUIT:
                save.save();
                System.exit(0);
                break;
            case Screens.PREV_WORLD:
                if (mapWorld > 0) {
                    mapWorld--;
                    shopPage = 0;
                    audio.play("click");
                }
                break;
            case Screens.NEXT_WORLD:
                if (mapWorld < 4) {
                    mapWorld++;
                    shopPage = 0;
                    audio.play("click");
                }
                break;
            case Screens.DIFF:
                cycleDifficulty();
                break;
            case Screens.RESUME:
                mode = PLAY;
                if (gp != null) gp.pauseRequested = false;
                audio.play("click");
                break;
            case Screens.PAUSE_INV:
                backTo = PAUSE;
                itemPage = 0;
                mode = INVENTORY;
                audio.play("click");
                break;
            case Screens.PAUSE_SHOP:
                backTo = PAUSE;
                shopPage = 0;
                mode = SHOP;
                audio.play("click");
                break;
            case Screens.PAUSE_SKILLS:
                backTo = PAUSE;
                mode = SKILLS;
                audio.play("click");
                break;
            case Screens.PAUSE_SETTINGS:
                backTo = PAUSE;
                mode = SETTINGS;
                audio.play("click");
                break;
            case Screens.MAP_INV:
                backTo = WORLD_MAP;
                itemPage = 0;
                mode = INVENTORY;
                audio.play("click");
                break;
            case Screens.MAP_SHOP:
                backTo = WORLD_MAP;
                shopPage = 0;
                mode = SHOP;
                audio.play("click");
                break;
            case Screens.MAP_SKILLS:
                backTo = WORLD_MAP;
                mode = SKILLS;
                audio.play("click");
                break;
            case Screens.MENU_SETTINGS:
                backTo = MENU;
                mode = SETTINGS;
                audio.play("click");
                break;
            case Screens.MENU_MULTIPLAYER:
                openOnlineLobby();
                break;
            case Screens.MENU_LAN:
                openLanLobby();
                break;
            case Screens.MENU_NAME:
                promptPlayerName();
                break;
            case Screens.MENU_PROFILE:
                mode = PROFILE;
                audio.play("click");
                break;
            case Screens.MENU_ACHIEVEMENTS:
                mode = ACHIEVEMENTS;
                audio.play("click");
                break;
            case Screens.MENU_FRIENDS:
                mode = FRIENDS;
                if (online != null && online.loggedIn) online.fetchFriends();
                audio.play("click");
                break;
            case Screens.MENU_LEADERBOARD:
                mode = LEADERBOARD;
                if (online != null && online.loggedIn) online.fetchLeaderboard();
                audio.play("click");
                break;
            case Screens.MENU_OWNER:
                if (online == null || !online.ownerVerified) {
                    notify("Login online sebagai owner untuk membuka panel.");
                    audio.play("deny");
                } else {
                    mode = OWNER_PANEL;
                    if (online.adminResults.isEmpty()) online.adminSearch(ownerQuery);
                    audio.play("click");
                }
                break;
            case Screens.OWNER_SEARCH:
                promptOwnerSearch();
                audio.play("click");
                break;
            case Screens.OWNER_REFRESH:
                if (online != null) online.adminSearch(ownerQuery);
                audio.play("click");
                break;
            case Screens.OWNER_VIP:
                if (online != null) {
                    OnlineCoop.PlayerRow r = online.adminSelected();
                    if (r != null) online.adminSetVip(r.id, !r.vip);
                }
                audio.play("click");
                break;
            case Screens.OWNER_BAN:
                if (online != null) {
                    OnlineCoop.PlayerRow r = online.adminSelected();
                    if (r != null) online.adminSetBan(r.id, !r.banned);
                }
                audio.play("click");
                break;
            case Screens.OWNER_GOLD:
                if (online != null) {
                    OnlineCoop.PlayerRow r = online.adminSelected();
                    if (r != null) online.adminGrant(r.id, 10000, 0);
                }
                audio.play("click");
                break;
            case Screens.OWNER_DIAMOND:
                if (online != null) {
                    OnlineCoop.PlayerRow r = online.adminSelected();
                    if (r != null) online.adminGrant(r.id, 0, 100);
                }
                audio.play("click");
                break;
            case Screens.MENU_HELP:
                helpPage = 0;
                mode = HELP;
                audio.play("click");
                break;
            case Screens.INVITE_ACCEPT:
                if (online != null && online.pendingInvite != null) {
                    online.respondInvite(online.pendingInvite.id, true);
                }
                audio.play("click");
                break;
            case Screens.INVITE_DECLINE:
                if (online != null && online.pendingInvite != null) {
                    online.respondInvite(online.pendingInvite.id, false);
                }
                audio.play("click");
                break;
            case Screens.SOCIAL_SEARCH:
                promptSearch();
                break;
            case Screens.SOCIAL_REFRESH:
                if (online != null) {
                    if (mode == FRIENDS) online.fetchFriends();
                    else if (mode == LEADERBOARD) online.fetchLeaderboard();
                    else online.fetchInvites();
                }
                audio.play("click");
                break;
            case Screens.FRIEND_INVITE:
                if (online != null && online.selectedFriendId.length() > 0) {
                    online.sendInvite(online.selectedFriendId);
                } else {
                    notify("Pilih teman dulu.");
                    audio.play("deny");
                }
                break;
            case Screens.PROFILE_ACHIEVEMENTS:
                mode = ACHIEVEMENTS;
                audio.play("click");
                break;
            case Screens.PROFILE_FRIENDS:
                mode = FRIENDS;
                if (online != null && online.loggedIn) online.fetchFriends();
                audio.play("click");
                break;
            case Screens.PROFILE_LEADERBOARD:
                mode = LEADERBOARD;
                if (online != null && online.loggedIn) online.fetchLeaderboard();
                audio.play("click");
                break;
            case Screens.LEADERBOARD_REFRESH:
                if (online != null) online.fetchLeaderboard();
                audio.play("click");
                break;
            case Screens.HELP_PREV:
                helpPage = Math.max(0, helpPage - 1);
                audio.play("click");
                break;
            case Screens.HELP_NEXT:
                helpPage = Math.min((save.isOwner() ? 6 : 5) - 1, helpPage + 1);
                audio.play("click");
                break;
            case Screens.LAN_HOST:
                hostLan();
                break;
            case Screens.LAN_JOIN:
                promptJoinIp();
                break;
            case Screens.LAN_START:
                startLanHostGame();
                break;
            case Screens.LAN_LEAVE:
                leaveLan();
                mode = MENU;
                audio.play("click");
                break;
            case Screens.LAN_STAGE_PREV:
                lanStage = Gfx.clamp(lanStage - 1, 0, Catalog.STAGE_COUNT - 1);
                audio.play("click");
                break;
            case Screens.LAN_STAGE_NEXT:
                lanStage = Gfx.clamp(lanStage + 1, 0, Catalog.STAGE_COUNT - 1);
                audio.play("click");
                break;
            case Screens.ONLINE_USERNAME:
                promptOnlineUsername();
                break;
            case Screens.ONLINE_PASSWORD:
                promptOnlinePassword();
                break;
            case Screens.ONLINE_LOGIN:
                if (online != null) online.signIn(onlineUser, onlinePass);
                audio.play("click");
                break;
            case Screens.ONLINE_REGISTER:
                if (online != null) online.signUp(onlineUser, onlinePass);
                audio.play("click");
                break;
            case Screens.ONLINE_LOGOUT:
                if (online != null) online.logout();
                lan = null;
                onlineUser = "";
                onlinePass = "";
                onlineCode = "";
                audio.play("click");
                break;
            case Screens.ONLINE_CREATE:
                if (online != null) {
                    onlineStage = Gfx.clamp(save.maxStage, 0, Catalog.STAGE_COUNT - 1);
                    online.createRoom(4);
                }
                audio.play("click");
                break;
            case Screens.ONLINE_JOIN:
                if (online != null) online.joinRoom(onlineCode);
                audio.play("click");
                break;
            case Screens.ONLINE_START:
                if (online != null) online.startGame(onlineStage);
                audio.play("click");
                break;
            case Screens.ONLINE_LEAVE:
                if (online != null) online.leaveRoom();
                lan = null;
                audio.play("click");
                break;
            case Screens.ONLINE_STAGE_PREV:
                onlineStage = Gfx.clamp(onlineStage - 1, 0, Catalog.STAGE_COUNT - 1);
                if (online != null && online.host) online.sendStage(onlineStage);
                audio.play("click");
                break;
            case Screens.ONLINE_STAGE_NEXT:
                onlineStage = Gfx.clamp(onlineStage + 1, 0, Catalog.STAGE_COUNT - 1);
                if (online != null && online.host) online.sendStage(onlineStage);
                audio.play("click");
                break;
            case Screens.ONLINE_CODE:
                promptOnlineCode();
                break;
            case Screens.ONLINE_LAN:
                if (online != null) online.leaveRoom();
                lan = null;
                openLanLobby();
                break;
            case Screens.PAUSE_MENU:
                leaveLan();
                gp = null;
                mode = MENU;
                selItemId = null;
                save.save();
                audio.play("click");
                break;
            case Screens.INV_EQUIP:
                equipSelected();
                break;
            case Screens.INV_UPGRADE:
                upgradeSelected();
                break;
            case Screens.INV_PREV:
                if (itemPage > 0) itemPage--;
                audio.play("click");
                break;
            case Screens.INV_NEXT:
                itemPage++;
                audio.play("click");
                break;
            case Screens.SHOP_PREV:
                if (shopPage > 0) shopPage--;
                audio.play("click");
                break;
            case Screens.SHOP_NEXT:
                shopPage++;
                audio.play("click");
                break;
            case Screens.SET_MUSIC:
                save.music = !save.music;
                audio.setMusicOn(save.music);
                lastMusic = -99;
                save.save();
                audio.play("click");
                break;
            case Screens.SET_SFX:
                save.sfx = !save.sfx;
                audio.setSfxOn(save.sfx);
                save.save();
                audio.play("click");
                break;
            case Screens.SET_MUSIC_DOWN:
                changeMusicVolume(-0.1f);
                break;
            case Screens.SET_MUSIC_UP:
                changeMusicVolume(0.1f);
                break;
            case Screens.SET_SFX_DOWN:
                changeSfxVolume(-0.1f);
                break;
            case Screens.SET_SFX_UP:
                changeSfxVolume(0.1f);
                break;
            case Screens.SET_BTN_DOWN:
                save.buttonScale = Gfx.clamp(save.buttonScale - 0.05f, 0.75f, 1.35f);
                save.save();
                audio.play("click");
                break;
            case Screens.SET_BTN_UP:
                save.buttonScale = Gfx.clamp(save.buttonScale + 0.05f, 0.75f, 1.35f);
                save.save();
                audio.play("click");
                break;
            case Screens.SET_SENS_DOWN:
                save.sensitivity = Gfx.clamp(save.sensitivity - 0.1f, 0.4f, 2f);
                save.save();
                audio.play("click");
                break;
            case Screens.SET_SENS_UP:
                save.sensitivity = Gfx.clamp(save.sensitivity + 0.1f, 0.4f, 2f);
                save.save();
                audio.play("click");
                break;
            case Screens.SET_QUALITY:
                save.highQuality = !save.highQuality;
                save.save();
                audio.play("click");
                break;
            case Screens.RESULT_NEXT:
                if (gp != null) {
                    int next = gp.stageIndex + 1;
                    if (next < Catalog.STAGE_COUNT) {
                        startStage(next);
                    } else {
                        gp = null;
                        mode = WORLD_MAP;
                    }
                }
                break;
            case Screens.RESULT_RETRY:
                if (gp != null) startStage(gp.stageIndex);
                break;
            case Screens.RESULT_MAP:
                if (gp != null) mapWorld = Gfx.clamp(gp.stageIndex / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1);
                gp = null;
                mode = WORLD_MAP;
                save.save();
                audio.play("click");
                break;
            default:
                break;
        }
    }

    private void changeMusicVolume(float d) {
        save.musicVolume = Math.round(Gfx.clamp(save.musicVolume + d, 0f, 1f) * 10f) / 10f;
        save.music = save.musicVolume > 0.001f;
        audio.setMusicVolume(save.musicVolume);
        audio.setMusicOn(save.music);
        if (!save.music) lastMusic = -99;
        save.save();
        audio.play("click");
    }

    private void changeSfxVolume(float d) {
        save.sfxVolume = Math.round(Gfx.clamp(save.sfxVolume + d, 0f, 1f) * 10f) / 10f;
        save.sfx = save.sfxVolume > 0.001f;
        audio.setSfxVolume(save.sfxVolume);
        audio.setSfxOn(save.sfx);
        save.save();
        audio.play("click");
    }

    private void cycleDifficulty() {
        int max = Math.max(1, save.unlockedDifficulty());
        int next = save.difficulty + 1;
        if (next > max) next = 0;
        save.difficulty = next;
        save.save();
        audio.play("click");
        notify("Kesulitan: " + Catalog.DIFFICULTY[next]);
    }

    private void startStage(int idx) {
        startStage(idx, false);
    }

    private void startStage(int idx, boolean lanGame) {
        if (idx < 0 || idx >= Catalog.STAGE_COUNT) return;
        if (!lanGame && idx > save.maxStage) {
            audio.play("deny");
            notify("Stage masih terkunci.");
            return;
        }
        if (!lanGame) leaveLan();
        gp = new Gameplay(save, audio, idx, lanGame ? lan : null, lanGame && lan != null && lan.host);
        gp.viewW = W;
        gp.viewH = H;
        gp.zoom = zoomFor(H);
        if (lanGame && lan != null) lan.setLocalStage(idx);
        mapWorld = idx / Catalog.STAGES_PER_WORLD;
        if (!lanGame || idx <= save.maxStage) save.currentStage = idx;
        save.save();
        selItemId = null;
        mode = PLAY;
        lastMusic = -99;
        audio.play("click");
    }

    private void playCinematic(final String kind) {
        if (cinematicActive) return;
        cinematicActive = true;
        audio.pauseMusic();
        if (context instanceof MainActivity) {
            ((MainActivity) context).playCinematic(kind, new Runnable() {
                public void run() {
                    finishCinematic(kind);
                }
            });
        } else {
            finishCinematic(kind);
        }
    }

    private void finishCinematic(String kind) {
        cinematicActive = false;
        audio.resumeMusic();
        if ("opening".equals(kind)) {
            mode = WORLD_MAP;
            lastMusic = -99;
        } else if ("ending".equals(kind)) {
            mode = RESULT;
        }
    }

    private void openLanLobby() {
        mode = LAN_LOBBY;
        lanStage = Gfx.clamp(save.maxStage, 0, Catalog.STAGE_COUNT - 1);
        lanErrorShown = null;
        audio.play("click");
    }

    private void openOnlineLobby() {
        if (online == null) online = new OnlineCoop(save);
        mode = ONLINE_LOBBY;
        onlineStage = Gfx.clamp(save.maxStage, 0, Catalog.STAGE_COUNT - 1);
        lanErrorShown = null;
        audio.play("click");
    }

    private void hostLan() {
        leaveLan();
        lan = new LanCoop(save);
        lan.host();
        lanStage = Gfx.clamp(save.maxStage, 0, Catalog.STAGE_COUNT - 1);
        lanErrorShown = null;
        audio.play("click");
    }

    private void promptJoinIp() {
        openTextInput(INPUT_IP, "GABUNG CO-OP", "Masukkan alamat IP host di jaringan yang sama.",
                lanLastIp, "192.168.43.1", 64, true);
    }

    private void joinLan(String ip) {
        leaveLan();
        lanLastIp = ip;
        lan = new LanCoop(save);
        lan.join(ip);
        lanErrorShown = null;
        audio.play("click");
    }

    private void startLanHostGame() {
        if (lan == null || !lan.connected) {
            notify("Partner belum terhubung.");
            audio.play("deny");
            return;
        }
        int stage = Gfx.clamp(lanStage, 0, Catalog.STAGE_COUNT - 1);
        lan.sendStage(stage);
        startStage(stage, true);
    }

    private void leaveLan() {
        if (lan != null) {
            if (lan == online) {
                online.leaveRoom();
            } else {
                lan.close();
            }
            lan = null;
        }
        lanErrorShown = null;
    }

    public void promptPlayerName() {
        openTextInput(INPUT_NAME, "NAMA PEMAIN", "Maksimal 16 karakter. Nama ini tampil di menu dan co-op.",
                save.playerName, "Ketik nama...", 16, false);
    }

    private void promptOnlineUsername() {
        openTextInput(INPUT_USERNAME, "USERNAME ONLINE", "3-16 huruf, angka, atau _.",
                onlineUser, "username", 16, false, false);
    }

    private void promptOnlinePassword() {
        openTextInput(INPUT_PASSWORD, "PASSWORD ONLINE", "Minimal 6 karakter.",
                onlinePass, "password", 32, false, true);
    }

    private void promptOnlineCode() {
        openTextInput(INPUT_ROOM_CODE, "KODE ROOM", "Masukkan 6 karakter kode room.",
                onlineCode, "ABC123", 6, false, false);
    }

    private void promptSearch() {
        openTextInput(INPUT_SEARCH, "CARI PEMAIN", "Cari username teman untuk dikirimi permintaan.",
                searchQuery, "username", 16, false, false);
    }

    private void promptOwnerSearch() {
        openTextInput(INPUT_OWNER_SEARCH, "CARI PEMAIN", "Cari username untuk dikelola owner.",
                ownerQuery, "username", 16, false, false);
    }

    private void promptOwnerPassword() {
        openTextInput(INPUT_OWNER_PASSWORD, "PASSWORD OWNER",
                "Nama ini khusus owner. Masukkan password untuk membuka akses.",
                "", "password owner", 32, false, true);
    }

    private void openTextInput(int purpose, String title, String message, String initial,
                               String hint, int max, boolean numeric) {
        openTextInput(purpose, title, message, initial, hint, max, numeric, false);
    }

    private void openTextInput(int purpose, String title, String message, String initial,
                               String hint, int max, boolean numeric, boolean password) {
        inputActive = true;
        inputPurpose = purpose;
        inputTitle = title;
        inputMessage = message;
        inputHint = hint;
        inputValue = initial == null ? "" : initial;
        inputMax = Math.max(1, max);
        inputNumeric = numeric;
        inputPassword = password;
        inputShift = false;
        inputError = false;
        audio.play("click");
    }

    private void closeTextInput() {
        inputActive = false;
        inputValue = "";
        inputPassword = false;
        pendingOwnerName = null;
    }

    private void inputAction(int id) {
        if (id == Screens.INPUT_CANCEL) {
            closeTextInput();
            audio.play("click");
            return;
        }
        if (id == Screens.INPUT_OK) {
            acceptTextInput();
            return;
        }
        if (id == Screens.INPUT_BACKSPACE) {
            if (inputValue.length() > 0) {
                inputValue = inputValue.substring(0, inputValue.length() - 1);
            }
            audio.play("click");
            return;
        }
        if (id == Screens.INPUT_CLEAR) {
            inputValue = "";
            audio.play("click");
            return;
        }
        if (id == Screens.INPUT_SHIFT) {
            inputShift = !inputShift;
            audio.play("click");
            return;
        }
        if (id >= Screens.INPUT_KEY_BASE && id < Screens.INPUT_KEY_BASE + 128) {
            char ch = (char) (id - Screens.INPUT_KEY_BASE);
            if (ch == '-' && inputNumeric) return;
            inputChar(ch);
        }
    }

    private void inputChar(char ch) {
        if (inputValue.length() >= inputMax) {
            audio.play("deny");
            return;
        }
        inputError = false;
        if (inputNumeric) {
            if (!((ch >= '0' && ch <= '9') || ch == '.')) {
                audio.play("deny");
                return;
            }
        } else if (inputPassword) {
            ch = inputShift ? Character.toUpperCase(ch) : Character.toLowerCase(ch);
        } else if (ch != ' ' && ch != '-') {
            if (inputValue.length() == 0) {
                ch = Character.toUpperCase(ch);
            } else {
                ch = inputShift ? Character.toUpperCase(ch) : Character.toLowerCase(ch);
            }
        }
        inputValue += ch;
        audio.play("click");
    }

    private boolean inputKey(int code) {
        if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_BACK) {
            closeTextInput();
            audio.play("click");
            return true;
        }
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            acceptTextInput();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DEL) {
            if (inputValue.length() > 0) {
                inputValue = inputValue.substring(0, inputValue.length() - 1);
            }
            audio.play("click");
            return true;
        }
        if (code == KeyEvent.KEYCODE_SHIFT_LEFT || code == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            inputShift = !inputShift;
            audio.play("click");
            return true;
        }
        if (code == KeyEvent.KEYCODE_SPACE && !inputNumeric) {
            inputChar(' ');
            return true;
        }
        if (code == KeyEvent.KEYCODE_PERIOD) {
            inputChar('.');
            return true;
        }
        if (code == KeyEvent.KEYCODE_MINUS && !inputNumeric) {
            inputChar('-');
            return true;
        }
        if (code >= KeyEvent.KEYCODE_A && code <= KeyEvent.KEYCODE_Z) {
            inputChar((char) ('A' + (code - KeyEvent.KEYCODE_A)));
            return true;
        }
        if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) {
            inputChar((char) ('0' + (code - KeyEvent.KEYCODE_0)));
            return true;
        }
        return true;
    }

    private void acceptTextInput() {
        String value = inputValue.trim();
        if (value.length() == 0) {
            inputError = true;
            audio.play("deny");
            return;
        }
        if (inputPurpose == INPUT_NAME) {
            if (!save.ownerMode && SaveData.isOwnerName(value)) {
                pendingOwnerName = value;
                promptOwnerPassword();
                return;
            }
            save.setPlayerName(value);
            closeTextInput();
            notify("Nama disimpan: " + save.playerName);
            audio.play("level");
        } else if (inputPurpose == INPUT_OWNER_PASSWORD) {
            if (SaveData.verifyOwnerPassword(value)) {
                String ownerName = pendingOwnerName;
                save.ownerMode = true;
                if (ownerName != null) save.setPlayerName(ownerName);
                save.applyOwnerBoosts();
                save.save();
                closeTextInput();
                notify("Owner mode aktif.");
                audio.play("level");
            } else {
                inputValue = "";
                inputError = true;
                inputMessage = "Password owner salah. Coba lagi.";
                audio.play("deny");
            }
        } else if (inputPurpose == INPUT_IP) {
            closeTextInput();
            joinLan(value);
        } else if (inputPurpose == INPUT_USERNAME) {
            onlineUser = value;
            closeTextInput();
            notify("Username: " + onlineUser);
        } else if (inputPurpose == INPUT_PASSWORD) {
            onlinePass = value;
            closeTextInput();
        } else if (inputPurpose == INPUT_ROOM_CODE) {
            onlineCode = value.toUpperCase();
            closeTextInput();
            if (online != null) online.joinRoom(onlineCode);
        } else if (inputPurpose == INPUT_SEARCH) {
            searchQuery = value;
            closeTextInput();
            if (online != null) online.searchPlayers(searchQuery);
        } else if (inputPurpose == INPUT_OWNER_SEARCH) {
            ownerQuery = value;
            closeTextInput();
            if (online != null) online.adminSearch(ownerQuery);
        } else {
            closeTextInput();
        }
    }

    public List<String> itemList() {
        return new java.util.ArrayList<>(save.items.keySet());
    }

    public List<Catalog.Item> shopList() {
        int cap = Gfx.clamp(1 + save.worldsCleared() + mapWorld, 1, 6);
        return Catalog.shopStock(cap);
    }

    private void equipSelected() {
        Catalog.Item it = selItemId == null ? null : Catalog.item(selItemId);
        if (it == null) {
            notify("Pilih item terlebih dahulu.");
            audio.play("deny");
            return;
        }
        if (it.slot < 0) {
            notify("Item ini dipakai saat bertarung.");
            audio.play("deny");
            return;
        }
        if (save.count(it.id) <= 0) {
            notify("Item tidak tersedia.");
            return;
        }
        if (save.isEquipped(it.id)) {
            save.equip(it.slot, null);
            notify(it.name + " dilepas.");
        } else {
            save.equip(it.slot, it.id);
            notify(it.name + " dipasang.");
        }
        save.save();
        audio.play("click");
    }

    private void upgradeSelected() {
        Catalog.Item it = selItemId == null ? null : Catalog.item(selItemId);
        if (it == null || it.slot < 0) {
            notify("Pilih perlengkapan untuk di-upgrade.");
            audio.play("deny");
            return;
        }
        if (save.upgradeOf(it.id) >= 10) {
            notify("Sudah mencapai level maksimum.");
            audio.play("deny");
            return;
        }
        if (save.upgradeItem(it.id)) {
            audio.play("level");
            notify(it.name + " +" + save.upgradeOf(it.id));
            save.save();
        } else {
            audio.play("deny");
            notify("Butuh " + save.upgradeCostGold(it.id) + " gold dan "
                    + save.upgradeCostShards(it.id) + " shard.");
        }
    }

    private void buy(int idx) {
        List<Catalog.Item> stock = shopList();
        if (idx < 0 || idx >= stock.size()) return;
        Catalog.Item it = stock.get(idx);
        if (save.gold < it.price) {
            audio.play("deny");
            notify("Gold tidak cukup.");
            return;
        }
        save.gold -= it.price;
        save.addItem(it.id, 1);
        save.save();
        audio.play("coin");
        notify("Membeli " + it.name + ".");
    }

    private void upgradeSkill(int i) {
        if (i < 0 || i >= 3) return;
        if (save.isOwner()) {
            save.skillLv[i] = Catalog.MAX_SKILL_LEVEL;
            save.save();
            audio.play("level");
            notify(Catalog.SKILLS[save.classIndex][i].name + " dimaksimalkan.");
            return;
        }
        int lvl = save.skillLv[i];
        if (lvl >= Catalog.MAX_SKILL_LEVEL) {
            notify("Skill sudah maksimum.");
            audio.play("deny");
            return;
        }
        int cost = 100 * (lvl + 1);
        if (save.points < 1 || save.gold < cost) {
            notify("Butuh 1 poin dan " + cost + " gold.");
            audio.play("deny");
            return;
        }
        save.points--;
        save.gold -= cost;
        save.skillLv[i] = lvl + 1;
        save.save();
        audio.play("level");
        notify(Catalog.SKILLS[save.classIndex][i].name + "  Lv " + (lvl + 1));
    }

    private void ownerSkill(int i, int delta) {
        if (!save.isOwner() || i < 0 || i >= 3) return;
        save.skillLv[i] = Gfx.clamp(save.skillLv[i] + delta, 0, Catalog.MAX_SKILL_LEVEL);
        save.save();
        audio.play("click");
        notify(Catalog.SKILLS[save.classIndex][i].name + "  Lv " + save.skillLv[i]);
    }

    private void upgradeTrack(int i) {
        if (i < 0 || i >= 6) return;
        int lvl = save.tracks[i];
        if (lvl >= Catalog.MAX_TRACK_LEVEL) {
            notify("Talenta sudah maksimum.");
            audio.play("deny");
            return;
        }
        int cost = 60 * (lvl + 1);
        if (save.points < 1 || save.gold < cost) {
            notify("Butuh 1 poin dan " + cost + " gold.");
            audio.play("deny");
            return;
        }
        save.points--;
        save.gold -= cost;
        save.tracks[i] = lvl + 1;
        save.save();
        audio.play("level");
        notify(Catalog.TRACKS[i].name + "  Lv " + (lvl + 1));
    }
}
