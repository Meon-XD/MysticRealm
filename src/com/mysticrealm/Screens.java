package com.mysticrealm;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

public class Screens {

    public static final int NONE = -1;
    public static final int BACK = 1;
    public static final int CONTINUE = 2;
    public static final int NEW_GAME = 3;
    public static final int QUIT = 4;
    public static final int START = 5;
    public static final int CLASS_BASE = 10;
    public static final int PREV_WORLD = 20;
    public static final int NEXT_WORLD = 21;
    public static final int DIFF = 22;
    public static final int STAGE_BASE = 30;
    public static final int RESUME = 40;
    public static final int PAUSE_INV = 41;
    public static final int PAUSE_SKILLS = 42;
    public static final int PAUSE_SETTINGS = 43;
    public static final int PAUSE_MENU = 44;
    public static final int PAUSE_SHOP = 45;
    public static final int MAP_INV = 46;
    public static final int MAP_SHOP = 47;
    public static final int MAP_SKILLS = 48;
    public static final int INV_EQUIP = 50;
    public static final int INV_UPGRADE = 51;
    public static final int INV_PREV = 52;
    public static final int INV_NEXT = 53;
    public static final int SHOP_PREV = 60;
    public static final int SHOP_NEXT = 61;
    public static final int SKILL_UP_BASE = 70;
    public static final int TRACK_UP_BASE = 80;
    public static final int SET_MUSIC = 90;
    public static final int SET_SFX = 91;
    public static final int SET_QUALITY = 92;
    public static final int SET_MUSIC_DOWN = 120;
    public static final int SET_MUSIC_UP = 121;
    public static final int SET_SFX_DOWN = 122;
    public static final int SET_SFX_UP = 123;
    public static final int SET_SENS_DOWN = 124;
    public static final int SET_SENS_UP = 125;
    public static final int SET_BTN_DOWN = 126;
    public static final int SET_BTN_UP = 127;
    public static final int RESULT_NEXT = 100;
    public static final int RESULT_RETRY = 101;
    public static final int RESULT_MAP = 102;
    public static final int CONFIRM_YES = 110;
    public static final int CONFIRM_NO = 111;
    public static final int MENU_SETTINGS = 112;
    public static final int MENU_MULTIPLAYER = 113;
    public static final int LAN_HOST = 114;
    public static final int LAN_JOIN = 115;
    public static final int LAN_START = 116;
    public static final int LAN_LEAVE = 117;
    public static final int MENU_NAME = 118;
    public static final int MENU_LAN = 119;
    public static final int LAN_STAGE_PREV = 130;
    public static final int LAN_STAGE_NEXT = 131;
    public static final int ONLINE_USERNAME = 140;
    public static final int ONLINE_PASSWORD = 141;
    public static final int ONLINE_LOGIN = 142;
    public static final int ONLINE_REGISTER = 143;
    public static final int ONLINE_LOGOUT = 144;
    public static final int ONLINE_CREATE = 145;
    public static final int ONLINE_JOIN = 146;
    public static final int ONLINE_START = 147;
    public static final int ONLINE_LEAVE = 148;
    public static final int ONLINE_STAGE_PREV = 149;
    public static final int ONLINE_STAGE_NEXT = 150;
    public static final int ONLINE_CODE = 151;
    public static final int ONLINE_LAN = 152;
    public static final int MENU_PROFILE = 153;
    public static final int MENU_ACHIEVEMENTS = 154;
    public static final int MENU_FRIENDS = 155;
    public static final int MENU_LEADERBOARD = 156;
    public static final int MENU_HELP = 157;
    public static final int MENU_OWNER = 158;
    public static final int INVITE_ACCEPT = 160;
    public static final int INVITE_DECLINE = 161;
    public static final int SOCIAL_SEARCH = 162;
    public static final int SOCIAL_REFRESH = 163;
    public static final int FRIEND_INVITE = 164;
    public static final int PROFILE_ACHIEVEMENTS = 165;
    public static final int PROFILE_FRIENDS = 166;
    public static final int PROFILE_LEADERBOARD = 167;
    public static final int LEADERBOARD_REFRESH = 168;
    public static final int HELP_PREV = 169;
    public static final int HELP_NEXT = 170;
    public static final int OWNER_SEARCH = 171;
    public static final int OWNER_REFRESH = 172;
    public static final int OWNER_VIP = 173;
    public static final int OWNER_BAN = 174;
    public static final int OWNER_GOLD = 175;
    public static final int OWNER_DIAMOND = 176;
    public static final int INPUT_BACKSPACE = 550;
    public static final int INPUT_CLEAR = 551;
    public static final int INPUT_CANCEL = 552;
    public static final int INPUT_OK = 553;
    public static final int INPUT_SHIFT = 554;
    public static final int INPUT_KEY_BASE = 600;
    public static final int ITEM_ROW_BASE = 300;
    public static final int SHOP_ROW_BASE = 400;
    public static final int FRIEND_ROW_BASE = 900;
    public static final int FRIEND_ACCEPT_BASE = 920;
    public static final int FRIEND_DECLINE_BASE = 940;
    public static final int ADD_FRIEND_BASE = 960;
    public static final int OWNER_SKILL_DOWN_BASE = 980;
    public static final int OWNER_SKILL_UP_BASE = 990;
    public static final int OWNER_ROW_BASE = 1000;

    static class Btn {
        float x, y, w, h;
        int id;
    }

    private final Game game;
    private final ArrayList<Btn> buttons = new ArrayList<>();

    public Screens(Game game) {
        this.game = game;
    }

    private SaveData save() {
        return game.save;
    }

    private float W() {
        return game.W;
    }

    private float H() {
        return game.H;
    }

    private float u() {
        return game.gfx.u;
    }

    public int hit(float x, float y) {
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Btn b = buttons.get(i);
            if (x >= b.x && y >= b.y && x <= b.x + b.w && y <= b.y + b.h) return b.id;
        }
        return NONE;
    }

    public static boolean isInputId(int id) {
        return id >= INPUT_BACKSPACE && id < INPUT_KEY_BASE + 128;
    }

    private Btn reg(float x, float y, float w, float h, int id) {
        Btn b = new Btn();
        b.x = x;
        b.y = y;
        b.w = w;
        b.h = h;
        b.id = id;
        buttons.add(b);
        return b;
    }

    private void button(Canvas c, Gfx g, float x, float y, float w, float h, String label, int id) {
        button(c, g, x, y, w, h, label, id, 0);
    }

    private void button(Canvas c, Gfx g, float x, float y, float w, float h, String label, int id, int style) {
        reg(x, y, w, h, id);
        float u = u();
        int fill;
        int stroke;
        int txt;
        if (style == 1) {
            fill = 0xEE8A6A24;
            stroke = 0xFFFFD98A;
            txt = 0xFFFFF4D8;
        } else if (style == 2) {
            fill = 0xCC3A1520;
            stroke = 0x88E5484D;
            txt = 0xFFFFC9CB;
        } else if (style == 3) {
            fill = 0xCC1B1530;
            stroke = 0x556B5A96;
            txt = Gfx.DIM;
        } else {
            fill = 0xDD211A38;
            stroke = 0x667A64B4;
            txt = Gfx.TEXT;
        }
        g.roundRect(c, x, y, w, h, 12 * u, fill);
        g.roundRectStroke(c, x, y, w, h, 12 * u, stroke, 1.6f * u);
        float fs = style == 1 ? 18 * u : 16 * u;
        g.text(c, label, x + w / 2f, y + h / 2f, fs, txt, 1, style == 1);
    }

    private void bg(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        g.gradientRect(c, 0, 0, w, h, 0xFF140D26, 0xFF05040B);
        int accent = Catalog.WORLDS[Gfx.clamp(game.mapWorld, 0, Catalog.WORLD_COUNT - 1)].accent;
        for (int i = 0; i < 26; i++) {
            float px = (i * 379.7f + game.uiTime * (6 + i % 5)) % w;
            float py = (i * 211.3f + (float) Math.sin(game.uiTime * 0.4f + i) * 30 + h * 0.1f) % h;
            g.circle(c, px, py, (1.4f + (i % 3)) * u, Gfx.withAlpha(accent, 26 + (i % 4) * 8));
        }
        g.vignette(c, w / 2f, h / 2f, (float) Math.hypot(w, h) * 0.62f);
    }

    private void title(Canvas c, Gfx g, String text, float y, float size) {
        g.textShadow(c, text, W() / 2f, y, size, Gfx.TEXT, 1, true);
        g.circle(c, W() / 2f, y + size * 0.7f, 3 * u(), Gfx.GOLD);
        g.line(c, W() / 2f - 60 * u(), y + size * 0.7f, W() / 2f - 10 * u(), y + size * 0.7f, 0x66E9C46A, 1.5f * u());
        g.line(c, W() / 2f + 10 * u(), y + size * 0.7f, W() / 2f + 60 * u(), y + size * 0.7f, 0x66E9C46A, 1.5f * u());
    }

    public void draw(Canvas c, Gfx g, int mode) {
        buttons.clear();
        switch (mode) {
            case Game.LOADING:
                drawLoading(c, g);
                break;
            case Game.MENU:
                drawMenu(c, g);
                break;
            case Game.CLASS_SELECT:
                drawClassSelect(c, g);
                break;
            case Game.WORLD_MAP:
                drawMap(c, g);
                break;
            case Game.PAUSE:
                drawPause(c, g);
                break;
            case Game.INVENTORY:
                drawInventory(c, g);
                break;
            case Game.SHOP:
                drawShop(c, g);
                break;
            case Game.SKILLS:
                drawSkills(c, g);
                break;
            case Game.SETTINGS:
                drawSettings(c, g);
                break;
            case Game.RESULT:
                drawResult(c, g);
                break;
            case Game.LAN_LOBBY:
                drawLan(c, g);
                break;
            case Game.ONLINE_LOBBY:
                drawOnline(c, g);
                break;
            case Game.PROFILE:
                drawProfile(c, g);
                break;
            case Game.ACHIEVEMENTS:
                drawAchievements(c, g);
                break;
            case Game.HELP:
                drawHelp(c, g);
                break;
            case Game.FRIENDS:
                drawFriends(c, g);
                break;
            case Game.LEADERBOARD:
                drawLeaderboard(c, g);
                break;
            case Game.OWNER_PANEL:
                drawOwner(c, g);
                break;
            default:
                drawMenu(c, g);
                break;
        }
        if (game.inputActive) drawInput(c, g);
        else drawInvite(c, g);
        drawNotice(c, g);
    }

    // ---------------------------------------------------------------- menu

    private void drawMenu(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);

        g.textShadow(c, "MYSTIC", 44 * u, 62 * u, 34 * u, Gfx.TEXT, 0, true);
        g.textShadow(c, "REALM", 44 * u, 94 * u, 34 * u, Gfx.GOLD, 0, true);
        g.text(c, "v1.2  •  Meon-XD", w - 30 * u, 42 * u, 12 * u, Gfx.DIM, 2, false);
        if (save().isOwner()) {
            g.roundRect(c, w - 150 * u, 58 * u, 120 * u, 26 * u, 13 * u, 0x553FD68A);
            g.text(c, "OWNER", w - 90 * u, 71 * u, 11 * u, Gfx.GREEN, 1, true);
        }

        float lx = 40 * u, ly = 140 * u;
        float lw = w * 0.34f, lh = h - 240 * u;
        g.panel(c, lx, ly, lw, lh, 0xCC150F24, 0x446B5A96);

        Catalog.ClassDef cd = Catalog.CLASSES[Gfx.clamp(save().classIndex, 0, Catalog.CLASSES.length - 1)];
        float ax = lx + lw / 2f, ay = ly + 96 * u;
        g.circle(c, ax, ay, 64 * u, Gfx.withAlpha(cd.color, 34));
        g.circleStroke(c, ax, ay, 54 * u, cd.color, 3 * u);
        drawClassIcon(c, g, save().classIndex, ax, ay, cd.color, u * 1.35f);
        g.textShadow(c, save().playerName, ax, ly + 190 * u, 26 * u, Gfx.TEXT, 1, true);
        if (save().isOwner()) g.text(c, "VERIFIED OWNER", ax, ly + 214 * u, 11 * u, Gfx.GREEN, 1, true);
        g.text(c, cd.name + "  •  Lv " + save().level, ax, ly + 242 * u, 15 * u, cd.color, 1, true);
        g.text(c, "Stage " + Math.max(1, save().maxStage + 1) + "  •  " + save().worldsCleared() + "/" + Catalog.WORLD_COUNT + " dunia",
                ax, ly + 268 * u, 13 * u, Gfx.DIM, 1, false);
        g.text(c, save().gold + " gold  •  " + save().shards + " shard", ax, ly + 292 * u, 13 * u, Gfx.GOLD, 1, false);
        g.text(c, "Enam jalan, satu takdir.", ax, ly + 330 * u, 13 * u, Gfx.DIM, 1, false);
        button(c, g, lx + 24 * u, ly + lh - 66 * u, lw - 48 * u, 42 * u,
                "Ubah Nama: " + save().playerName, MENU_NAME, 3);

        float gx = w * 0.44f, gy = 140 * u;
        float gw = w - gx - 40 * u;
        float gap = 10 * u;
        float bw = (gw - gap) / 2f, bh = 58 * u;
        int row = 0;
        int col = 0;
        if (save().hasCharacter) {
            menuButton(c, g, gx, gy, bw, bh, "Lanjutkan", "Lv " + save().level, CONTINUE, 1, row, col);
            col++;
        }
        menuButton(c, g, gx + col * (bw + gap), gy, bw, bh, save().hasCharacter ? "Baru" : "Mulai", "Petualangan", NEW_GAME,
                save().hasCharacter ? 0 : 1, row, col);
        col++;
        if (col == 2) { col = 0; row++; }
        menuButton(c, g, gx + col * (bw + gap), gy + row * (bh + gap), bw, bh, "Co-op", "Online", MENU_MULTIPLAYER, 0, row, col);
        col++;
        menuButton(c, g, gx + col * (bw + gap), gy + row * (bh + gap), bw, bh, "Co-op", "LAN", MENU_LAN, 0, row, col);
        col = 0; row++;
        menuButton(c, g, gx, gy + row * (bh + gap), bw, bh, "Teman", "Cari & undang", MENU_FRIENDS, 0, row, col);
        col++;
        menuButton(c, g, gx + col * (bw + gap), gy + row * (bh + gap), bw, bh, "Peringkat", "Leaderboard", MENU_LEADERBOARD, 0, row, col);
        col = 0; row++;
        menuButton(c, g, gx, gy + row * (bh + gap), bw, bh, "Profil", save().playerName, MENU_PROFILE, 0, row, col);
        col++;
        menuButton(c, g, gx + col * (bw + gap), gy + row * (bh + gap), bw, bh, "Pencapaian", "Koleksi lencana", MENU_ACHIEVEMENTS, 0, row, col);
        col = 0; row++;
        menuButton(c, g, gx, gy + row * (bh + gap), bw, bh, "Cara", "Bermain", MENU_HELP, 0, row, col);
        col++;
        menuButton(c, g, gx + col * (bw + gap), gy + row * (bh + gap), bw, bh, "Pengaturan", "Audio & kontrol", MENU_SETTINGS, 0, row, col);
        if (save().isOwner()) {
            col = 0; row++;
            menuButton(c, g, gx, gy + row * (bh + gap), bw, bh, "Owner", "Panel admin", MENU_OWNER, 1, row, col);
        }

        button(c, g, w - 190 * u, h - 78 * u, 150 * u, 46 * u, "Keluar", QUIT, 2);
        g.text(c, "Dibuat oleh Meon-XD", 40 * u, h - 28 * u, 12 * u, Gfx.GOLD, 0, true);

        if (game.confirmNew) drawConfirm(c, g);
    }

    private void menuButton(Canvas c, Gfx g, float x, float y, float w, float h,
                            String title, String sub, int id, int style, int row, int col) {
        button(c, g, x, y, w, h, "", id, style);
        float u = u();
        int color = style == 1 ? 0xFFFFF4D8 : Gfx.TEXT;
        g.text(c, title, x + 18 * u, y + 24 * u, 16 * u, color, 0, true);
        g.text(c, sub, x + 18 * u, y + 43 * u, 11 * u, style == 1 ? 0xFFE9C46A : Gfx.DIM, 0, false);
    }

    private int friendCount() {
        OnlineCoop o = game.online;
        return o == null ? 0 : o.friends.size();
    }

    private void drawInvite(Canvas c, Gfx g) {
        OnlineCoop o = game.online;
        if (o == null || !o.loggedIn || o.pendingInvite == null) return;
        float w = W(), h = H(), u = u();
        g.fill(0xAA000000);
        c.drawRect(0, 0, w, h, g.p);
        float pw = Math.min(560 * u, w - 60 * u), ph = 260 * u;
        float px = w / 2f - pw / 2f, py = h / 2f - ph / 2f;
        g.panel(c, px, py, pw, ph, 0xF01A1430, 0x889B5DE5);
        g.text(c, "UNDANGAN CO-OP", w / 2f, py + 42 * u, 20 * u, Gfx.GOLD, 1, true);
        String from = o.pendingInvite.fromName.length() == 0 ? "Pemain" : o.pendingInvite.fromName;
        g.wrap(c, from + " mengundang anda bermain bersama", w / 2f, py + 92 * u,
                pw - 60 * u, 17 * u, Gfx.TEXT, 2, 1, true);
        g.text(c, "Room " + o.pendingInvite.roomCode, w / 2f, py + 138 * u, 14 * u, Gfx.CYAN, 1, false);
        button(c, g, px + 34 * u, py + ph - 76 * u, (pw - 82 * u) / 2f, 48 * u,
                "Terima", INVITE_ACCEPT, 1);
        button(c, g, px + pw / 2f + 7 * u, py + ph - 76 * u, (pw - 82 * u) / 2f, 48 * u,
                "Tolak", INVITE_DECLINE, 2);
    }

    private void drawProfile(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "PROFIL", 50 * u, 28 * u);
        Catalog.ClassDef cd = Catalog.CLASSES[Gfx.clamp(save().classIndex, 0, Catalog.CLASSES.length - 1)];
        float px = 60 * u, py = 130 * u, pw = w - 120 * u, ph = h - 220 * u;
        g.panel(c, px, py, pw, ph, 0xCC150F24, 0x446B5A96);
        float ax = px + 150 * u, ay = py + 130 * u;
        g.circle(c, ax, ay, 78 * u, Gfx.withAlpha(cd.color, 34));
        g.circleStroke(c, ax, ay, 66 * u, cd.color, 3 * u);
        drawClassIcon(c, g, save().classIndex, ax, ay, cd.color, u * 1.7f);
        g.textShadow(c, save().playerName, px + 300 * u, py + 76 * u, 30 * u, Gfx.TEXT, 0, true);
        if (save().isOwner()) g.text(c, "VERIFIED OWNER", px + 302 * u, py + 102 * u, 12 * u, Gfx.GREEN, 0, true);
        g.text(c, cd.name + "  •  Level " + save().level + "  •  " + Catalog.DIFFICULTY[save().difficulty],
                px + 300 * u, py + 138 * u, 15 * u, cd.color, 0, true);
        g.text(c, "Stage " + Math.max(1, save().maxStage + 1) + "  •  " + save().worldsCleared() + "/" + Catalog.WORLD_COUNT + " dunia",
                px + 300 * u, py + 166 * u, 13 * u, Gfx.DIM, 0, false);
        g.text(c, save().gold + " gold  •  " + save().shards + " shard  •  " + save().points + " poin",
                px + 300 * u, py + 190 * u, 13 * u, Gfx.GOLD, 0, false);
        g.text(c, Achievements.unlockedCount(save(), friendCount()) + "/" + Achievements.LIST.length + " pencapaian",
                px + 300 * u, py + 214 * u, 13 * u, Gfx.CYAN, 0, false);
        g.text(c, "Item " + save().items.size() + "  •  Teman " + friendCount(), px + 300 * u, py + 238 * u, 13 * u, Gfx.DIM, 0, false);

        float by = py + ph - 150 * u;
        float bw = (pw - 160 * u) / 3f;
        button(c, g, px + 40 * u, by, bw, 48 * u, "Pencapaian", PROFILE_ACHIEVEMENTS, 0);
        button(c, g, px + 60 * u + bw, by, bw, 48 * u, "Teman", PROFILE_FRIENDS, 0);
        button(c, g, px + 80 * u + bw * 2, by, bw, 48 * u, "Peringkat", PROFILE_LEADERBOARD, 0);
        g.text(c, "Terus bertarung untuk membuka lencana dan menaiki peringkat.",
                w / 2f, py + ph - 56 * u, 12 * u, Gfx.DIM, 1, false);
    }

    private void drawAchievements(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "PENCAPAIAN", 50 * u, 28 * u);
        int fc = friendCount();
        g.text(c, Achievements.unlockedCount(save(), fc) + " / " + Achievements.LIST.length,
                w / 2f, 102 * u, 14 * u, Gfx.GOLD, 1, true);
        float left = 40 * u, top = 130 * u;
        float cw = (w - 100 * u) / 2f, ch = 74 * u, gap = 10 * u;
        for (int i = 0; i < Achievements.LIST.length; i++) {
            Achievements.Def d = Achievements.LIST[i];
            int col = i % 2, row = i / 2;
            float x = left + col * (cw + gap), y = top + row * (ch + gap);
            boolean ok = Achievements.unlocked(save(), i, fc);
            int p = Achievements.progress(save(), i, fc);
            g.roundRect(c, x, y, cw, ch, 10 * u, ok ? 0xCC1D2C24 : 0xCC171226);
            g.roundRectStroke(c, x, y, cw, ch, 10 * u, ok ? 0x883FD68A : 0x446B5A96, 1.4f * u);
            g.circle(c, x + 34 * u, y + 37 * u, 18 * u, ok ? 0x553FD68A : 0x442E2844);
            g.text(c, ok ? "✓" : "•", x + 34 * u, y + 37 * u, 16 * u, ok ? Gfx.GREEN : Gfx.DIM, 1, true);
            g.text(c, d.name, x + 62 * u, y + 26 * u, 15 * u, ok ? Gfx.TEXT : Gfx.DIM, 0, true);
            g.text(c, d.desc, x + 62 * u, y + 46 * u, 11 * u, Gfx.DIM, 0, false);
            String val = d.kind == Achievements.SKILL ? (ok ? "Selesai" : "0/" + d.target)
                    : Math.min(p, d.target) + "/" + d.target;
            g.text(c, val, x + cw - 16 * u, y + 26 * u, 11 * u, ok ? Gfx.GREEN : Gfx.DIM, 2, true);
            float pct = d.target <= 0 ? 1f : Gfx.clamp(p / (float) d.target, 0f, 1f);
            g.roundRect(c, x + 62 * u, y + ch - 16 * u, cw - 80 * u, 4 * u, 2 * u, 0x55101018);
            g.roundRect(c, x + 62 * u, y + ch - 16 * u, (cw - 80 * u) * pct, 4 * u, 2 * u,
                    ok ? Gfx.GREEN : Gfx.ACCENT);
        }
    }

    private void drawHelp(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "CARA BERMAIN", 50 * u, 28 * u);
        boolean owner = save().isOwner();
        int pages = owner ? 6 : 5;
        int page = Gfx.clamp(game.helpPage, 0, pages - 1);
        String[] titles = {"DASAR", "ALUR PERMAINAN", "PROGRES", "CO-OP", "KONTROL", "OWNER"};
        String[] bodies = {
                "Pilih satu dari tiga kelas: Ksatria tahan banting, Penyihir dengan sihir jarak jauh, atau Pembunuh yang lincah.\n\nSetiap kelas punya tiga skill dan enam jalur talenta. Gunakan auto-aim dengan menekan tombol serangan; arahkan joystick untuk bergerak dan menghindar.",
                "Selesaikan stage berurutan di setiap dunia. Kalahkan musuh, aktifkan altar teka-teki, lalu hadapi bos di akhir dunia. Stage yang selesai membuka stage berikutnya dan meningkatkan hadiah.",
                "Naik level untuk mendapat poin skill dan talenta. Kumpulkan gold dan shard dari musuh serta peti. Equip item terbaik, tingkatkan skill, dan selesaikan dunia untuk membuka kesulitan yang lebih tinggi.",
                "Co-op Online memakai server internet dan mendukung 2-4 pemain. Co-op LAN memakai hotspot atau WiFi yang sama. Host memilih stage; semua pemain mengikuti. Pemain yang terhubung akan tampil di lobby.",
                "Gerak: joystick kiri. Serang: tombol serangan (auto-aim). Skill: tombol 1-3. Dodge: tombol dash. Menu jeda tersedia di pojok layar. Mouse dan keyboard juga didukung di desktop.",
                "Mode owner aktif untuk nama meon_xd. Mana, cooldown, dan biaya upgrade dibebaskan. Gunakan tombol - dan + pada layar Skill untuk mengatur level secara langsung. Badge owner tampil di profil dan lobby."
        };
        float pw = Math.min(860 * u, w - 80 * u), ph = h - 260 * u;
        float px = w / 2f - pw / 2f, py = 130 * u;
        g.panel(c, px, py, pw, ph, 0xCC150F24, 0x446B5A96);
        g.text(c, titles[page], px + 34 * u, py + 42 * u, 20 * u, Gfx.GOLD, 0, true);
        g.wrap(c, bodies[page], px + 34 * u, py + 84 * u, pw - 68 * u, 15 * u, Gfx.TEXT, 12, 0, false);
        for (int i = 0; i < pages; i++) {
            g.circle(c, px + pw / 2f + (i - (pages - 1) / 2f) * 18 * u, py + ph - 34 * u,
                    4 * u, i == page ? Gfx.GOLD : 0x556B5A96);
        }
        if (page > 0) button(c, g, px + 30 * u, py + ph - 70 * u, 110 * u, 42 * u, "‹ Sebelumnya", HELP_PREV, 3);
        if (page < pages - 1) button(c, g, px + pw - 140 * u, py + ph - 70 * u, 110 * u, 42 * u, "Berikutnya ›", HELP_NEXT, 0);
    }

    private void drawFriends(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "TEMAN", 50 * u, 28 * u);
        OnlineCoop o = game.online;
        if (o == null || !o.loggedIn) {
            g.panel(c, w / 2f - 280 * u, h / 2f - 80 * u, 560 * u, 160 * u, 0xCC150F24, 0x446B5A96);
            g.text(c, "Masuk ke akun online terlebih dahulu.", w / 2f, h / 2f - 20 * u, 16 * u, Gfx.TEXT, 1, true);
            g.text(c, "Buka Co-op Online untuk masuk atau mendaftar.", w / 2f, h / 2f + 12 * u, 12 * u, Gfx.DIM, 1, false);
            return;
        }
        float lx = 40 * u, ly = 120 * u, lw = (w - 100 * u) * 0.58f;
        float rx = lx + lw + 20 * u, rw = w - rx - 40 * u;
        g.panel(c, lx, ly, lw, h - 190 * u, 0xCC150F24, 0x446B5A96);
        g.panel(c, rx, ly, rw, h - 190 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "CARI PEMAIN", lx + 20 * u, ly + 28 * u, 14 * u, Gfx.GOLD, 0, true);
        button(c, g, lx + 20 * u, ly + 44 * u, lw - 150 * u, 42 * u,
                game.searchQuery.length() == 0 ? "Ketik username..." : game.searchQuery, SOCIAL_SEARCH, 3);
        button(c, g, lx + lw - 120 * u, ly + 44 * u, 100 * u, 42 * u, "Cari", SOCIAL_SEARCH, 0);
        float y = ly + 104 * u;
        if (!o.searchResults.isEmpty()) {
            g.text(c, "HASIL", lx + 20 * u, y, 11 * u, Gfx.DIM, 0, true);
            y += 18 * u;
            for (int i = 0; i < o.searchResults.size() && i < 3; i++) {
                OnlineCoop.PlayerRow r = o.searchResults.get(i);
                g.text(c, r.username + "  Lv " + r.level, lx + 20 * u, y + 24 * u, 13 * u, Gfx.TEXT, 0, true);
                String st = r.friendStatus;
                if (st.equals("none")) button(c, g, lx + lw - 130 * u, y + 4 * u, 110 * u, 34 * u, "Tambah", ADD_FRIEND_BASE + i, 0);
                else g.text(c, st.equals("accepted") ? "Teman" : "Menunggu", lx + lw - 80 * u, y + 24 * u, 11 * u, Gfx.DIM, 2, false);
                y += 44 * u;
            }
            y += 8 * u;
        }
        g.text(c, "TEMAN (" + o.friends.size() + ")", lx + 20 * u, y + 12 * u, 12 * u, Gfx.GOLD, 0, true);
        y += 28 * u;
        for (int i = 0; i < o.friends.size() && i < 5; i++) {
            OnlineCoop.FriendRow r = o.friends.get(i);
            boolean sel = r.id.equals(o.selectedFriendId);
            g.roundRect(c, lx + 16 * u, y, lw - 32 * u, 42 * u, 8 * u, sel ? 0x662A2044 : 0x44120E20);
            g.text(c, r.username + (r.verified ? "  ✓" : ""), lx + 30 * u, y + 22 * u, 13 * u,
                    sel ? Gfx.GOLD : Gfx.TEXT, 0, true);
            g.text(c, "Lv " + r.level, lx + lw - 70 * u, y + 22 * u, 11 * u, Gfx.DIM, 2, false);
            reg(lx + 16 * u, y, lw - 32 * u, 42 * u, FRIEND_ROW_BASE + i);
            y += 48 * u;
        }
        if (o.friends.isEmpty()) g.text(c, "Belum ada teman. Cari username di atas.", lx + 20 * u, y + 18 * u, 12 * u, Gfx.DIM, 0, false);

        g.text(c, "PERMINTAAN MASUK", rx + 20 * u, ly + 28 * u, 13 * u, Gfx.GOLD, 0, true);
        float ry = ly + 52 * u;
        for (int i = 0; i < o.requests.size() && i < 5; i++) {
            OnlineCoop.FriendRow r = o.requests.get(i);
            g.text(c, r.username, rx + 20 * u, ry + 18 * u, 13 * u, Gfx.TEXT, 0, true);
            button(c, g, rx + rw - 150 * u, ry, 62 * u, 34 * u, "Ya", FRIEND_ACCEPT_BASE + i, 1);
            button(c, g, rx + rw - 80 * u, ry, 62 * u, 34 * u, "Tidak", FRIEND_DECLINE_BASE + i, 2);
            ry += 44 * u;
        }
        if (o.requests.isEmpty()) g.text(c, "Tidak ada permintaan baru.", rx + 20 * u, ry + 16 * u, 12 * u, Gfx.DIM, 0, false);
        button(c, g, rx + 20 * u, ly + h - 340 * u, rw - 40 * u, 46 * u, "Undang Teman ke Room", FRIEND_INVITE, 1);
        g.text(c, o.host && o.hasRoom() ? "Room siap diundang." : "Buat room sebagai host untuk mengundang.",
                rx + rw / 2f, ly + h - 306 * u, 11 * u, Gfx.DIM, 1, false);
        button(c, g, rx + 20 * u, ly + h - 240 * u, rw - 40 * u, 40 * u, "Muat Ulang", SOCIAL_REFRESH, 3);
        if (o.socialMessage.length() > 0) {
            g.wrap(c, o.socialMessage, w / 2f, h - 56 * u, w - 100 * u, 12 * u, Gfx.CYAN, 2, 1, false);
        }
    }

    private void drawOwner(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "OWNER PANEL", 50 * u, 28 * u);
        OnlineCoop o = game.online;
        if (o == null || !o.loggedIn || !o.ownerVerified) {
            g.panel(c, w / 2f - 320 * u, h / 2f - 90 * u, 640 * u, 180 * u, 0xCC150F24, 0x446B5A96);
            g.text(c, "Akses owner butuh login online.", w / 2f, h / 2f - 30 * u, 18 * u, Gfx.TEXT, 1, true);
            g.text(c, "Masuk sebagai akun owner di Co-op Online, lalu buka panel ini lagi.",
                    w / 2f, h / 2f + 10 * u, 13 * u, Gfx.DIM, 1, false);
            g.text(c, o != null && o.loggedIn ? "Akun: " + o.username : "Belum login online.",
                    w / 2f, h / 2f + 44 * u, 12 * u, Gfx.GOLD, 1, false);
            return;
        }
        float lx = 40 * u, ly = 120 * u, lw = (w - 100 * u) * 0.54f;
        float rx = lx + lw + 20 * u, rw = w - rx - 40 * u;
        float ph = h - 190 * u;
        g.panel(c, lx, ly, lw, ph, 0xCC150F24, 0x446B5A96);
        g.panel(c, rx, ly, rw, ph, 0xCC150F24, 0x446B5A96);
        g.text(c, "CARI PEMAIN", lx + 20 * u, ly + 28 * u, 14 * u, Gfx.GOLD, 0, true);
        button(c, g, lx + 20 * u, ly + 44 * u, lw - 150 * u, 42 * u,
                o.adminQuery.length() == 0 ? "Ketik username..." : o.adminQuery, OWNER_SEARCH, 3);
        button(c, g, lx + lw - 120 * u, ly + 44 * u, 100 * u, 42 * u, "Cari", OWNER_SEARCH, 0);
        float y = ly + 104 * u;
        g.text(c, "HASIL (" + o.adminResults.size() + ")", lx + 20 * u, y, 11 * u, Gfx.DIM, 0, true);
        y += 20 * u;
        for (int i = 0; i < o.adminResults.size() && i < 8; i++) {
            OnlineCoop.PlayerRow r = o.adminResults.get(i);
            boolean sel = r.id.equals(o.adminSelectedId);
            g.roundRect(c, lx + 16 * u, y, lw - 32 * u, 46 * u, 8 * u, sel ? 0x662A2044 : 0x44120E20);
            g.text(c, r.username + (r.vip ? "  VIP" : "") + (r.banned ? "  BAN" : ""),
                    lx + 30 * u, y + 24 * u, 13 * u, sel ? Gfx.GOLD : Gfx.TEXT, 0, true);
            g.text(c, "Lv " + r.level + "  •  " + r.gold + "g", lx + lw - 30 * u, y + 24 * u,
                    11 * u, Gfx.DIM, 2, false);
            reg(lx + 16 * u, y, lw - 32 * u, 46 * u, OWNER_ROW_BASE + i);
            y += 52 * u;
        }
        if (o.adminResults.isEmpty()) {
            g.text(c, o.adminBusy ? "Mencari..." : "Cari username lalu pilih pemain.",
                    lx + 20 * u, y + 20 * u, 12 * u, Gfx.DIM, 0, false);
        }
        button(c, g, lx + 20 * u, ly + ph - 60 * u, lw - 40 * u, 42 * u, "Muat Ulang", OWNER_REFRESH, 3);

        g.text(c, "AKSI OWNER", rx + 20 * u, ly + 28 * u, 14 * u, Gfx.GOLD, 0, true);
        OnlineCoop.PlayerRow sel = o.adminSelected();
        if (sel == null) {
            g.text(c, "Pilih pemain di daftar kiri.", rx + rw / 2f, ly + 100 * u, 14 * u, Gfx.DIM, 1, false);
        } else {
            g.text(c, sel.username, rx + 20 * u, ly + 62 * u, 18 * u, Gfx.TEXT, 0, true);
            g.text(c, "Lv " + sel.level + "  •  " + sel.gold + " gold", rx + 20 * u, ly + 88 * u,
                    12 * u, Gfx.DIM, 0, false);
            g.text(c, sel.vip ? "VIP aktif" : "Bukan VIP", rx + 20 * u, ly + 112 * u,
                    12 * u, sel.vip ? Gfx.GREEN : Gfx.DIM, 0, true);
            g.text(c, sel.banned ? "DIBLOKIR" : "Tidak diblokir", rx + 20 * u, ly + 136 * u,
                    12 * u, sel.banned ? Gfx.RED : Gfx.DIM, 0, true);
            g.text(c, "ID: " + sel.id, rx + 20 * u, ly + 162 * u, 10 * u, Gfx.DIM, 0, false);
            float by = ly + 200 * u;
            button(c, g, rx + 20 * u, by, rw - 40 * u, 44 * u,
                    sel.vip ? "Cabut VIP" : "Beri VIP", OWNER_VIP, sel.vip ? 2 : 1);
            button(c, g, rx + 20 * u, by + 54 * u, rw - 40 * u, 44 * u,
                    sel.banned ? "Cabut Blokir" : "Blokir Pemain", OWNER_BAN, sel.banned ? 1 : 2);
            button(c, g, rx + 20 * u, by + 108 * u, (rw - 50 * u) / 2f, 44 * u,
                    "+10.000 Gold", OWNER_GOLD, 0);
            button(c, g, rx + 30 * u + (rw - 50 * u) / 2f, by + 108 * u, (rw - 50 * u) / 2f, 44 * u,
                    "+100 Diamond", OWNER_DIAMOND, 0);
        }
        if (o.adminMessage.length() > 0) {
            g.wrap(c, o.adminMessage, w / 2f, h - 56 * u, w - 100 * u, 12 * u,
                    o.adminBusy ? Gfx.CYAN : Gfx.GOLD, 2, 1, false);
        }
    }

    private void drawLeaderboard(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "PERINGKAT", 50 * u, 28 * u);
        OnlineCoop o = game.online;
        if (o == null || !o.loggedIn) {
            g.panel(c, w / 2f - 280 * u, h / 2f - 80 * u, 560 * u, 160 * u, 0xCC150F24, 0x446B5A96);
            g.text(c, "Masuk ke akun online untuk melihat peringkat.", w / 2f, h / 2f - 20 * u, 16 * u, Gfx.TEXT, 1, true);
            g.text(c, "Buka Co-op Online untuk masuk atau mendaftar.", w / 2f, h / 2f + 12 * u, 12 * u, Gfx.DIM, 1, false);
            return;
        }
        float pw = Math.min(880 * u, w - 80 * u), ph = h - 240 * u;
        float px = w / 2f - pw / 2f, py = 130 * u;
        g.panel(c, px, py, pw, ph, 0xCC150F24, 0x446B5A96);
        g.text(c, "PERINGKAT PEMAIN", px + 28 * u, py + 34 * u, 14 * u, Gfx.GOLD, 0, true);
        button(c, g, px + pw - 150 * u, py + 14 * u, 120 * u, 38 * u, "Muat Ulang", LEADERBOARD_REFRESH, 3);
        float y = py + 66 * u;
        g.text(c, "#", px + 28 * u, y, 11 * u, Gfx.DIM, 0, true);
        g.text(c, "PEMAIN", px + 80 * u, y, 11 * u, Gfx.DIM, 0, true);
        g.text(c, "LV", px + pw - 240 * u, y, 11 * u, Gfx.DIM, 2, true);
        g.text(c, "MENANG", px + pw - 160 * u, y, 11 * u, Gfx.DIM, 2, true);
        g.text(c, "STAGE", px + pw - 70 * u, y, 11 * u, Gfx.DIM, 2, true);
        y += 12 * u;
        for (int i = 0; i < o.leaderboard.size() && i < 12; i++) {
            OnlineCoop.PlayerRow r = o.leaderboard.get(i);
            float ry = y + i * 40 * u;
            if (r.me) g.roundRect(c, px + 16 * u, ry, pw - 32 * u, 36 * u, 8 * u, 0x552A2044);
            g.text(c, "" + (r.rank > 0 ? r.rank : i + 1), px + 28 * u, ry + 22 * u, 13 * u, Gfx.GOLD, 0, true);
            g.text(c, r.username + (r.verified ? "  ✓" : ""), px + 80 * u, ry + 22 * u, 13 * u,
                    r.me ? Gfx.GOLD : Gfx.TEXT, 0, true);
            g.text(c, "" + r.level, px + pw - 240 * u, ry + 22 * u, 12 * u, Gfx.CYAN, 2, false);
            g.text(c, "" + r.wins, px + pw - 160 * u, ry + 22 * u, 12 * u, Gfx.GREEN, 2, false);
            g.text(c, "" + r.maxStage, px + pw - 70 * u, ry + 22 * u, 12 * u, Gfx.DIM, 2, false);
        }
        if (o.leaderboard.isEmpty()) {
            g.text(c, o.socialBusy ? "Memuat peringkat..." : "Belum ada data peringkat.",
                    w / 2f, py + ph / 2f, 14 * u, Gfx.DIM, 1, false);
        }
        if (o.socialMessage.length() > 0) {
            g.text(c, o.socialMessage, w / 2f, h - 54 * u, 12 * u, Gfx.CYAN, 1, false);
        }
    }

    private void drawConfirm(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        g.fill(0xAA000000);
        c.drawRect(0, 0, w, h, g.p);
        float pw = 520 * u, ph = 260 * u;
        float px = w / 2f - pw / 2f, py = h / 2f - ph / 2f;
        g.panel(c, px, py, pw, ph, 0xF01A1430, 0x889B5DE5);
        g.text(c, "Mulai ulang petualangan?", w / 2f, py + 50 * u, 22 * u, Gfx.TEXT, 1, true);
        g.wrap(c, "Semua level, item, dan progres akan dihapus. Tindakan ini tidak bisa dibatalkan.",
                w / 2f, py + 95 * u, pw - 60 * u, 14 * u, Gfx.DIM, 4, 1, false);
        button(c, g, px + 40 * u, py + ph - 80 * u, 200 * u, 50 * u, "Ya, mulai baru", CONFIRM_YES, 2);
        button(c, g, px + pw - 240 * u, py + ph - 80 * u, 200 * u, 50 * u, "Batal", CONFIRM_NO, 0);
    }

    // --------------------------------------------------------- class select

    private void drawClassSelect(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        title(c, g, "PILIH JALANMU", 52 * u, 30 * u);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);

        float cw = Math.min(258 * u, (w - 120 * u) / 3f);
        float gap = 16 * u;
        float ch = Math.min(252 * u, (h - 232 * u) / 2f);
        float total = cw * 3 + gap * 2;
        float x0 = w / 2f - total / 2f;
        float y0 = h * 0.135f;
        for (int i = 0; i < Catalog.CLASSES.length; i++) {
            Catalog.ClassDef cd = Catalog.CLASSES[i];
            float x = x0 + (i % 3) * (cw + gap);
            float y = y0 + (i / 3) * (ch + gap);
            boolean sel = game.selClass == i;
            int fill = sel ? 0xF02A2044 : 0xDD1B1530;
            g.roundRect(c, x, y, cw, ch, 14 * u, fill);
            g.roundRectStroke(c, x, y, cw, ch, 14 * u, sel ? cd.color : 0x556B5A96, sel ? 3 * u : 1.5f * u);
            float icx = x + cw / 2f, icy = y + 42 * u;
            if (sel) {
                g.circle(c, icx, icy, 38 * u + (float) Math.sin(game.uiTime * 3) * 3, Gfx.withAlpha(cd.color, 34));
            }
            g.circle(c, icx, icy, 27 * u, Gfx.withAlpha(cd.color, 60));
            g.circleStroke(c, icx, icy, 27 * u, cd.color, 2.2f * u);
            drawClassIcon(c, g, i, icx, icy, cd.color, u * 0.85f);

            g.text(c, cd.name, icx, y + 80 * u, 18 * u, cd.color, 1, true);
            g.text(c, cd.role, icx, y + 98 * u, 10.5f * u, Gfx.DIM, 1, false);
            g.wrap(c, cd.desc, x + 14 * u, y + 112 * u, cw - 28 * u, 10.5f * u, Gfx.DIM, 2, 1, false);

            float sy = y + ch - 76 * u;
            statLine(c, g, "HP", cd.hp, x + 20 * u, sy, cw - 40 * u, Gfx.RED, u);
            statLine(c, g, "ATK", cd.atk, x + 20 * u, sy + 19 * u, cw - 40 * u, Gfx.ORANGE, u);
            statLine(c, g, "DEF", cd.def, x + 20 * u, sy + 38 * u, cw - 40 * u, Gfx.CYAN, u);
            statLine(c, g, "SPD", cd.spd, x + 20 * u, sy + 57 * u, cw - 40 * u, Gfx.GREEN, u);

            reg(x, y, cw, ch, CLASS_BASE + i);
            if (sel) {
                g.circle(c, x + 18 * u, y + 18 * u, 10 * u, cd.color);
                g.text(c, "✓", x + 18 * u, y + 18 * u, 13 * u, 0xFF1A1024, 1, true);
            }
        }
        float bw = 320 * u;
        button(c, g, w / 2f - bw / 2f, h - 74 * u, bw, 56 * u, "Mulai Petualangan", START, 1);
    }

    private void drawClassIcon(Canvas c, Gfx g, int cls, float x, float y, int color, float u) {
        g.stroke(color, 4 * u);
        if (cls == 0) {
            g.line(c, x - 16 * u, y + 16 * u, x + 16 * u, y - 16 * u, color, 5 * u);
            g.line(c, x + 2 * u, y + 2 * u, x + 18 * u, y + 18 * u, color, 4 * u);
        } else if (cls == 1) {
            g.circleStroke(c, x, y, 14 * u, color, 3.5f * u);
            g.circle(c, x, y, 5 * u, color);
            g.line(c, x, y - 22 * u, x, y - 14 * u, color, 3 * u);
            g.line(c, x, y + 14 * u, x, y + 22 * u, color, 3 * u);
        } else if (cls == 2) {
            g.line(c, x - 14 * u, y + 14 * u, x + 12 * u, y - 12 * u, color, 4 * u);
            g.line(c, x - 2 * u, y - 2 * u, x + 16 * u, y - 16 * u, color, 3 * u);
            g.circle(c, x + 12 * u, y - 12 * u, 3 * u, color);
        } else if (cls == 3) {
            g.circleStroke(c, x + 2 * u, y, 13 * u, color, 2.8f * u);
            g.line(c, x + 2 * u, y - 13 * u, x + 2 * u, y + 13 * u, color, 1.5f * u);
            g.line(c, x - 16 * u, y, x + 20 * u, y, color, 2.2f * u);
            g.line(c, x + 20 * u, y, x + 14 * u, y - 4 * u, color, 2.2f * u);
            g.line(c, x + 20 * u, y, x + 14 * u, y + 4 * u, color, 2.2f * u);
        } else if (cls == 4) {
            g.line(c, x - 6 * u, y + 20 * u, x + 4 * u, y - 14 * u, color, 3.2f * u);
            g.circleStroke(c, x + 5 * u, y - 18 * u, 6 * u, color, 2.4f * u);
            g.circle(c, x + 5 * u, y - 18 * u, 2.5f * u, color);
            g.circleStroke(c, x - 2 * u, y - 2 * u, 17 * u, Gfx.withAlpha(color, 150), 1.6f * u);
        } else {
            android.graphics.Path sh = new android.graphics.Path();
            sh.moveTo(x, y - 14 * u);
            sh.lineTo(x + 16 * u, y - 8 * u);
            sh.lineTo(x + 13 * u, y + 9 * u);
            sh.lineTo(x, y + 19 * u);
            sh.lineTo(x - 13 * u, y + 9 * u);
            sh.lineTo(x - 16 * u, y - 8 * u);
            sh.close();
            g.stroke(color, 2.6f * u);
            c.drawPath(sh, g.p);
            g.line(c, x, y - 8 * u, x, y + 10 * u, color, 2.4f * u);
            g.line(c, x - 8 * u, y - 2 * u, x + 8 * u, y - 2 * u, color, 2.4f * u);
        }
    }

    private void statLine(Canvas c, Gfx g, String name, int v, float x, float y, float w, int color, float u) {
        g.text(c, name, x, y, 12 * u, Gfx.DIM, 0, true);
        g.text(c, "" + v, x + w, y, 12 * u, color, 2, true);
        float pct = Gfx.clamp(v / 260f, 0.06f, 1f);
        g.roundRect(c, x, y + 9 * u, w, 4 * u, 2 * u, 0x55101018);
        g.roundRect(c, x, y + 9 * u, w * pct, 4 * u, 2 * u, Gfx.withAlpha(color, 200));
    }

    // ----------------------------------------------------------------- map

    private void drawMap(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        int wi = Gfx.clamp(game.mapWorld, 0, Catalog.WORLD_COUNT - 1);
        Catalog.WorldDef wd = Catalog.WORLDS[wi];
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);

        title(c, g, "PETA DUNIA", 50 * u, 28 * u);

        g.textShadow(c, wd.name, w / 2f, 124 * u, 32 * u, wd.accent, 1, true);
        g.text(c, wd.subtitle + "  •  " + wd.tag, w / 2f, 154 * u, 14 * u, Gfx.DIM, 1, false);

        float cellW = 170 * u, cellH = 170 * u, gap = 18 * u;
        float total = cellW * 5 + gap * 4;
        float x0 = w / 2f - total / 2f;
        float y0 = h * 0.29f;
        for (int i = 0; i < Catalog.STAGES_PER_WORLD; i++) {
            int idx = wi * Catalog.STAGES_PER_WORLD + i;
            boolean cleared = save().cleared[idx];
            boolean unlocked = idx <= save().maxStage;
            float x = x0 + i * (cellW + gap);
            int fill = cleared ? 0xEE1C3A2C : (unlocked ? 0xEE2A2044 : 0xCC151020);
            g.roundRect(c, x, y0, cellW, cellH, 16 * u, fill);
            g.roundRectStroke(c, x, y0, cellW, cellH, 16 * u,
                    cleared ? 0x8843D17A : (unlocked ? 0x99E9C46A : 0x44554466), unlocked ? 2 * u : 1.2f * u);
            g.text(c, "" + (i + 1), x + cellW / 2f, y0 + 56 * u, 42 * u,
                    cleared ? Gfx.GREEN : (unlocked ? Gfx.GOLD : 0xFF5A5070), 1, true);
            String nm = i == Catalog.STAGES_PER_WORLD - 1 ? "BOSS" : "Stage " + (idx + 1);
            g.text(c, nm, x + cellW / 2f, y0 + 100 * u, 13 * u, unlocked ? Gfx.TEXT : Gfx.DIM, 1, true);
            if (i == Catalog.STAGES_PER_WORLD - 1) {
                Catalog.BossDef bd = wd.boss;
                g.text(c, bd.name, x + cellW / 2f, y0 + 122 * u, 11 * u, unlocked ? wd.accent : Gfx.DIM, 1, false);
            }
            int fin = unlocked ? (cleared ? 2 : 1) : 0;
            g.text(c, fin == 2 ? "✓ Selesai" : (fin == 1 ? "Tersedia" : "🔒 Terkunci"),
                    x + cellW / 2f, y0 + cellH - 28 * u, 11 * u,
                    fin == 2 ? Gfx.GREEN : (fin == 1 ? Gfx.GOLD : Gfx.DIM), 1, true);
            if (unlocked) reg(x, y0, cellW, cellH, STAGE_BASE + i);
        }

        float ay = y0 + cellH + 34 * u;
        button(c, g, x0 - 62 * u, ay - 4 * u, 46 * u, 46 * u, "‹", PREV_WORLD, wi > 0 ? 0 : 3);
        button(c, g, x0 + total + 16 * u, ay - 4 * u, 46 * u, 46 * u, "›", NEXT_WORLD, wi < Catalog.WORLD_COUNT - 1 ? 0 : 3);
        g.text(c, "Dunia " + (wi + 1) + " / " + Catalog.WORLD_COUNT, w / 2f, ay + 20 * u, 14 * u, Gfx.DIM, 1, false);

        float iy = ay + 76 * u;
        int[] st = save().computeStats();
        g.panel(c, w / 2f - 470 * u, iy, 430 * u, 128 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "PAHLAWAN  •  Lv " + save().level, w / 2f - 444 * u, iy + 22 * u, 14 * u, Gfx.GOLD, 0, true);
        g.text(c, "ATK " + st[2] + "   DEF " + st[3] + "   HP " + st[0] + "   MP " + st[1], w / 2f - 444 * u, iy + 48 * u, 13 * u, Gfx.TEXT, 0, false);
        g.text(c, "Gold " + save().gold + "   Shard " + save().shards + "   Poin " + save().points, w / 2f - 444 * u, iy + 74 * u, 13 * u, Gfx.CYAN, 0, false);
        g.text(c, "Kesulitan: " + Catalog.DIFFICULTY[save().difficulty], w / 2f - 444 * u, iy + 102 * u, 13 * u, Gfx.ORANGE, 0, true);

        float dw = 430 * u;
        button(c, g, w / 2f + 40 * u, iy, dw, 54 * u, "Ubah Kesulitan", DIFF, 0);
        g.text(c, Catalog.DIFFICULTY_DESC[save().difficulty], w / 2f + 40 * u + dw / 2f, iy + 80 * u, 11 * u, Gfx.DIM, 1, false);
        g.text(c, "Kesulitan terbuka: " + (Math.max(1, save().unlockedDifficulty()) + 1) + "/5",
                w / 2f + 40 * u + dw / 2f, iy + 100 * u, 11 * u, Gfx.DIM, 1, false);

        float my = iy + 128 * u + 14 * u;
        float mw = 150 * u, mg = 12 * u;
        float mx = w / 2f - (mw * 3 + mg * 2) / 2f;
        button(c, g, mx, my, mw, 42 * u, "Inventaris", MAP_INV, 0);
        button(c, g, mx + mw + mg, my, mw, 42 * u, "Toko", MAP_SHOP, 0);
        button(c, g, mx + (mw + mg) * 2, my, mw, 42 * u, "Skill", MAP_SKILLS, 0);

        g.text(c, "Kalahkan semua musuh, pecahkan teka-teki altar, lalu tumbangkan bos dunia.",
                w / 2f, h - 24 * u, 12 * u, Gfx.DIM, 1, false);
    }

    // --------------------------------------------------------------- pause

    private void drawPause(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        if (game.gp != null) {
            game.gp.draw(c, g);
        } else {
            bg(c, g);
        }
        g.fill(0xAA05040B);
        c.drawRect(0, 0, w, h, g.p);

        float pw = 520 * u, ph = 430 * u;
        float px = w / 2f - pw / 2f, py = h / 2f - ph / 2f;
        g.panel(c, px, py, pw, ph, 0xF0191330, 0x889B5DE5);
        g.textShadow(c, "JEDA", w / 2f, py + 42 * u, 34 * u, Gfx.GOLD, 1, true);
        if (game.gp != null) {
            g.text(c, Catalog.WORLDS[game.gp.world.theme].name + "  •  " + Catalog.stageName(game.gp.stageIndex),
                    w / 2f, py + 74 * u, 14 * u, Gfx.DIM, 1, false);
        }
        float bw = pw - 80 * u, bx = px + 40 * u, bh = 48 * u;
        float y = py + 104 * u;
        button(c, g, bx, y, bw, bh, "Lanjutkan", RESUME, 1);
        y += bh + 12 * u;
        button(c, g, bx, y, bw * 0.48f, bh, "Toko", PAUSE_SHOP, 0);
        button(c, g, bx + bw * 0.52f, y, bw * 0.48f, bh, "Inventaris", PAUSE_INV, 0);
        y += bh + 12 * u;
        button(c, g, bx, y, bw * 0.48f, bh, "Skill", PAUSE_SKILLS, 0);
        button(c, g, bx + bw * 0.52f, y, bw * 0.48f, bh, "Pengaturan", PAUSE_SETTINGS, 0);
        y += bh + 12 * u;
        button(c, g, bx, y, bw, bh, "Menu Utama", PAUSE_MENU, 2);
    }

    // ----------------------------------------------------------- inventory

    private List<String> itemList() {
        return new ArrayList<>(save().items.keySet());
    }

    private void drawInventory(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "INVENTARIS", 50 * u, 28 * u);
        g.text(c, "Gold " + save().gold + "   ◆ " + save().shards, w - 30 * u, 34 * u, 15 * u, Gfx.GOLD, 2, true);

        float left = 40 * u, top = h * 0.16f;
        float colW = (w - 80 * u) * 0.46f;

        // equipment
        g.panel(c, left, top, colW, 260 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "PERLENGKAPAN", left + 20 * u, top + 24 * u, 15 * u, Gfx.GOLD, 0, true);
        for (int s = 0; s < 4; s++) {
            float ry = top + 54 * u + s * 46 * u;
            Catalog.Item it = save().equippedItem(s);
            g.roundRect(c, left + 18 * u, ry, colW - 36 * u, 38 * u, 8 * u, 0x88120E20);
            g.text(c, Catalog.SLOT_NAME[s], left + 32 * u, ry + 19 * u, 12 * u, Gfx.DIM, 0, true);
            if (it != null) {
                g.text(c, it.name + " +" + save().upgradeOf(it.id), left + 132 * u, ry + 19 * u, 13 * u, Gfx.RARITY[it.rarity], 0, true);
                if (save().isEquipped(it.id)) g.text(c, "✓", left + colW - 40 * u, ry + 19 * u, 14 * u, Gfx.GREEN, 2, true);
            } else {
                g.text(c, "-", left + 132 * u, ry + 19 * u, 13 * u, Gfx.DIM, 0, false);
            }
        }

        // stats
        int[] st = save().computeStats();
        float sx = left + colW + 24 * u;
        g.panel(c, sx, top, colW, 260 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "STATISTIK", sx + 20 * u, top + 24 * u, 15 * u, Gfx.GOLD, 0, true);
        String[] names = {"HP", "MP", "ATK", "DEF", "SPD", "CRIT", "LUCK"};
        int[] colors = {Gfx.RED, Gfx.BLUE, Gfx.ORANGE, Gfx.CYAN, Gfx.GREEN, Gfx.GOLD, Gfx.ACCENT};
        for (int i = 0; i < 7; i++) {
            float ry = top + 58 * u + i * 27 * u;
            g.text(c, names[i], sx + 24 * u, ry, 13 * u, Gfx.DIM, 0, true);
            g.text(c, "" + st[i] + (i == 5 ? "%" : ""), sx + colW - 24 * u, ry, 14 * u, colors[i], 2, true);
        }

        // item list
        float ly = top + 280 * u;
        float lw = w - 80 * u, lh = h - ly - 96 * u;
        g.panel(c, left, ly, lw, lh, 0xCC150F24, 0x446B5A96);
        g.text(c, "TAS", left + 20 * u, ly + 22 * u, 15 * u, Gfx.GOLD, 0, true);
        g.text(c, "Pilih item untuk dipakai atau di-upgrade", left + 90 * u, ly + 22 * u, 11 * u, Gfx.DIM, 0, false);

        List<String> list = itemList();
        int perPage = 4;
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        game.itemPage = Gfx.clamp(game.itemPage, 0, pages - 1);
        int start = game.itemPage * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            String id = list.get(idx);
            Catalog.Item it = Catalog.item(id);
            if (it == null) continue;
            float ry = ly + 44 * u + i * 44 * u;
            boolean sel = id.equals(game.selItemId);
            g.roundRect(c, left + 16 * u, ry, lw - 32 * u, 38 * u, 8 * u, sel ? 0xCC2E2450 : 0x88120E20);
            if (sel) g.roundRectStroke(c, left + 16 * u, ry, lw - 32 * u, 38 * u, 8 * u, Gfx.ACCENT, 1.5f * u);
            int col = it.slot >= 0 ? Gfx.RARITY[it.rarity] : Gfx.TEXT;
            g.text(c, it.name, left + 34 * u, ry + 19 * u, 14 * u, col, 0, sel);
            g.text(c, "x" + save().count(id), left + 330 * u, ry + 19 * u, 12 * u, Gfx.DIM, 0, false);
            g.text(c, Catalog.statLine(it), left + 420 * u, ry + 19 * u, 11 * u, Gfx.DIM, 0, false);
            if (it.slot >= 0 && save().isEquipped(id)) {
                g.text(c, "TERPASANG", lw - 30 * u, ry + 19 * u, 11 * u, Gfx.GREEN, 2, true);
            }
            reg(left + 16 * u, ry, lw - 32 * u, 38 * u, ITEM_ROW_BASE + idx);
        }
        button(c, g, left + lw - 220 * u, ly + 8 * u, 60 * u, 30 * u, "‹", INV_PREV, 3);
        g.text(c, (game.itemPage + 1) + "/" + pages, left + lw - 122 * u, ly + 23 * u, 12 * u, Gfx.DIM, 1, false);
        button(c, g, left + lw - 84 * u, ly + 8 * u, 60 * u, 30 * u, "›", INV_NEXT, 3);

        Catalog.Item sel = game.selItemId == null ? null : Catalog.item(game.selItemId);
        boolean canAct = sel != null;
        button(c, g, left, h - 72 * u, 220 * u, 50 * u, "Pakai / Lepas", INV_EQUIP, canAct ? 1 : 3);
        int ug = sel == null ? 0 : save().upgradeCostGold(sel.id);
        int us = sel == null ? 0 : save().upgradeCostShards(sel.id);
        String upLabel = sel == null ? "Upgrade"
                : "Upgrade +" + (save().upgradeOf(sel.id) + 1) + "  (" + ug + "g/" + us + "s)";
        boolean canUp = sel != null && sel.slot >= 0 && save().upgradeOf(sel.id) < 10
                && save().gold >= ug && save().shards >= us;
        button(c, g, left + 236 * u, h - 72 * u, 320 * u, 50 * u, upLabel, INV_UPGRADE, canUp ? 0 : 3);
        g.text(c, "Potion dipakai lewat tombol ramuan saat bertarung.", left + 580 * u, h - 47 * u, 12 * u, Gfx.DIM, 0, false);
    }

    // ---------------------------------------------------------------- shop

    private void drawShop(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "TOKO KELILING", 50 * u, 28 * u);
        g.text(c, "Gold " + save().gold + "   ◆ " + save().shards, w - 30 * u, 34 * u, 15 * u, Gfx.GOLD, 2, true);

        List<Catalog.Item> stock = game.shopList();
        int perPage = 6;
        int pages = Math.max(1, (stock.size() + perPage - 1) / perPage);
        game.shopPage = Gfx.clamp(game.shopPage, 0, pages - 1);
        int start = game.shopPage * perPage;
        float left = 40 * u, top = h * 0.17f;
        float lw = w - 80 * u;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= stock.size()) break;
            Catalog.Item it = stock.get(idx);
            float ry = top + i * 88 * u;
            boolean afford = save().gold >= it.price;
            g.roundRect(c, left, ry, lw, 76 * u, 12 * u, afford ? 0xCC1B1530 : 0x99140F22);
            g.roundRectStroke(c, left, ry, lw, 76 * u, 12 * u, afford ? 0x557A64B4 : 0x33302040, 1.2f * u);
            int col = it.slot >= 0 ? Gfx.RARITY[it.rarity] : Gfx.TEXT;
            g.text(c, it.name, left + 24 * u, ry + 26 * u, 18 * u, col, 0, true);
            g.text(c, Catalog.statLine(it), left + 24 * u, ry + 52 * u, 12 * u, Gfx.DIM, 0, false);
            g.text(c, it.desc, left + 430 * u, ry + 38 * u, 12 * u, Gfx.DIM, 0, false);
            g.text(c, it.price + " g", left + lw - 30 * u, ry + 38 * u, 18 * u, afford ? Gfx.GOLD : Gfx.RED, 2, true);
            reg(left, ry, lw, 76 * u, SHOP_ROW_BASE + idx);
        }
        button(c, g, w - 200 * u, h - 70 * u, 60 * u, 40 * u, "‹", SHOP_PREV, 3);
        g.text(c, (game.shopPage + 1) + "/" + pages, w - 126 * u, h - 50 * u, 12 * u, Gfx.DIM, 1, false);
        button(c, g, w - 84 * u, h - 70 * u, 60 * u, 40 * u, "›", SHOP_NEXT, 3);
        g.text(c, "Sentuh barang untuk membeli. Stok mengikuti duniamu.", left, h - 50 * u, 12 * u, Gfx.DIM, 0, false);
    }

    // -------------------------------------------------------------- skills

    private void drawSkills(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "SKILL & TALENTA", 50 * u, 28 * u);
        g.text(c, "Poin: " + save().points + "   Gold: " + save().gold, w - 30 * u, 34 * u, 15 * u, Gfx.GOLD, 2, true);

        float left = 40 * u, top = h * 0.16f;
        float colW = (w - 110 * u) / 2f;
        g.panel(c, left, top, colW, h - top - 40 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "SKILL KELAS", left + 20 * u, top + 26 * u, 16 * u, Gfx.GOLD, 0, true);
        int cls = save().classIndex;
        for (int i = 0; i < 3; i++) {
            Catalog.Skill s = Catalog.SKILLS[cls][i];
            int lvl = save().skillLv[i];
            float ry = top + 56 * u + i * 130 * u;
            g.roundRect(c, left + 16 * u, ry, colW - 32 * u, 116 * u, 10 * u, 0x99120E20);
            g.circle(c, left + 50 * u, ry + 40 * u, 20 * u, Gfx.withAlpha(s.color, 60));
            g.circleStroke(c, left + 50 * u, ry + 40 * u, 20 * u, s.color, 2 * u);
            g.text(c, "" + (i + 1), left + 50 * u, ry + 40 * u, 18 * u, s.color, 1, true);
            g.text(c, s.name, left + 82 * u, ry + 28 * u, 17 * u, Gfx.TEXT, 0, true);
            g.text(c, "Lv " + lvl + "/" + Catalog.MAX_SKILL_LEVEL + "  •  MP " + s.mp + "  •  CD " + s.cd + "s",
                    left + 82 * u, ry + 50 * u, 11 * u, Gfx.DIM, 0, false);
            g.wrap(c, s.desc, left + 82 * u, ry + 70 * u, colW - 120 * u, 11 * u, Gfx.DIM, 2, 0, false);
            int cost = 100 * (lvl + 1);
            boolean can = lvl < Catalog.MAX_SKILL_LEVEL && save().points >= 1 && save().gold >= cost;
            if (save().isOwner()) {
                button(c, g, left + colW - 250 * u, ry + 66 * u, 52 * u, 38 * u,
                        "-", OWNER_SKILL_DOWN_BASE + i, lvl > 0 ? 0 : 3);
                button(c, g, left + colW - 192 * u, ry + 66 * u, 52 * u, 38 * u,
                        "+", OWNER_SKILL_UP_BASE + i, lvl < Catalog.MAX_SKILL_LEVEL ? 0 : 3);
                button(c, g, left + colW - 132 * u, ry + 66 * u, 100 * u, 38 * u,
                        "MAX", SKILL_UP_BASE + i, 1);
            } else {
                button(c, g, left + colW - 180 * u, ry + 66 * u, 150 * u, 38 * u,
                        lvl >= Catalog.MAX_SKILL_LEVEL ? "MAX" : "+1  (" + cost + "g)", SKILL_UP_BASE + i, can ? 1 : 3);
            }
        }

        float rx = left + colW + 30 * u;
        g.panel(c, rx, top, colW, h - top - 40 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, "TALENTA", rx + 20 * u, top + 26 * u, 16 * u, Gfx.GOLD, 0, true);
        for (int i = 0; i < 6; i++) {
            Catalog.Track tr = Catalog.TRACKS[i];
            int lvl = save().tracks[i];
            float ry = top + 56 * u + i * 76 * u;
            g.roundRect(c, rx + 16 * u, ry, colW - 32 * u, 64 * u, 10 * u, 0x99120E20);
            g.circle(c, rx + 44 * u, ry + 32 * u, 14 * u, Gfx.withAlpha(tr.color, 60));
            g.text(c, "" + lvl, rx + 44 * u, ry + 32 * u, 13 * u, tr.color, 1, true);
            g.text(c, tr.name, rx + 68 * u, ry + 22 * u, 14 * u, Gfx.TEXT, 0, true);
            g.text(c, tr.desc, rx + 68 * u, ry + 42 * u, 11 * u, Gfx.DIM, 0, false);
            boolean can = lvl < Catalog.MAX_TRACK_LEVEL && save().points >= 1;
            int cost = 60 * (lvl + 1);
            can = can && save().gold >= cost;
            button(c, g, rx + colW - 150 * u, ry + 14 * u, 120 * u, 36 * u,
                    lvl >= Catalog.MAX_TRACK_LEVEL ? "MAX" : "+1 (" + cost + "g)", TRACK_UP_BASE + i, can ? 1 : 3);
        }
        g.text(c, "Dapatkan poin dengan naik level.", rx + colW / 2f, h - 52 * u, 11 * u, Gfx.DIM, 1, false);
    }

    // -------------------------------------------------------------- co-op

    private void drawLan(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "CO-OP LAN", 52 * u, 30 * u);
        g.text(c, "Satu hotspot / WiFi  •  tanpa server internet", w / 2f, 98 * u, 13 * u, Gfx.DIM, 1, false);

        LanCoop lan = game.lan;
        boolean connected = lan != null && lan.connected;
        float pw = Math.min(760 * u, w - 80 * u);
        float px = w / 2f - pw / 2f;
        float py = h * 0.22f;
        float ph = 390 * u;
        g.panel(c, px, py, pw, ph, 0xE6150F24, 0x779B5DE5);

        g.text(c, "Pemain", px + 34 * u, py + 38 * u, 14 * u, Gfx.DIM, 0, true);
        g.text(c, save().playerName, px + 150 * u, py + 38 * u, 18 * u, Gfx.TEXT, 0, true);
        g.text(c, "Peran", px + 34 * u, py + 74 * u, 14 * u, Gfx.DIM, 0, true);
        String role = lan == null ? "Belum terhubung" : (lan.host ? "Host" : "Client");
        g.text(c, role, px + 150 * u, py + 74 * u, 16 * u, Gfx.GOLD, 0, true);

        String status;
        int statusColor = Gfx.DIM;
        if (lan == null) {
            status = "Pilih Host atau Gabung.";
        } else if (lan.error != null) {
            status = lan.error;
            statusColor = Gfx.RED;
        } else if (!connected) {
            status = lan.host ? "Menunggu partner bergabung..." : "Menghubungi host...";
            statusColor = Gfx.ORANGE;
        } else {
            status = "Terhubung dengan " + (lan.remoteName.length() == 0 ? "partner" : lan.remoteName);
            statusColor = Gfx.GREEN;
        }
        g.text(c, status, px + 34 * u, py + 112 * u, 15 * u, statusColor, 0, true);

        if (lan != null) {
            String addr = lan.host ? lan.localIp + ":" + LanCoop.PORT : "Host: " + LanCoop.PORT;
            g.text(c, "Alamat", px + 34 * u, py + 152 * u, 13 * u, Gfx.DIM, 0, false);
            g.text(c, addr, px + 150 * u, py + 152 * u, 18 * u, Gfx.CYAN, 0, true);
            if (!lan.host) {
                g.text(c, "Tunggu host memilih stage dan memulai permainan.",
                        px + 34 * u, py + 190 * u, 12 * u, Gfx.DIM, 0, false);
            }
        }

        float by = py + ph - 70 * u;
        if (lan == null) {
            float bw = (pw - 100 * u) / 2f;
            button(c, g, px + 34 * u, by, bw, 48 * u, "Jadi Host", LAN_HOST, 1);
            button(c, g, px + 66 * u + bw, by, bw, 48 * u, "Gabung", LAN_JOIN, 0);
        } else if (lan.host) {
            float sy = py + 220 * u;
            g.text(c, "Stage", px + 34 * u, sy, 15 * u, Gfx.DIM, 0, true);
            button(c, g, px + 130 * u, sy - 20 * u, 42 * u, 40 * u, "‹", LAN_STAGE_PREV, 3);
            g.text(c, "Stage " + (game.lanStage + 1), px + 220 * u, sy, 20 * u, Gfx.TEXT, 1, true);
            button(c, g, px + 300 * u, sy - 20 * u, 42 * u, 40 * u, "›", LAN_STAGE_NEXT, 3);
            g.text(c, Catalog.WORLDS[Gfx.clamp(game.lanStage / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1)].name,
                    px + 470 * u, sy, 13 * u, Gfx.DIM, 0, false);
            button(c, g, px + 34 * u, by, 260 * u, 48 * u, "Mulai Co-op", LAN_START, connected ? 1 : 3);
            button(c, g, px + pw - 214 * u, by, 180 * u, 48 * u, "Keluar", LAN_LEAVE, 2);
        } else {
            button(c, g, px + pw / 2f - 90 * u, by, 180 * u, 48 * u, "Keluar", LAN_LEAVE, 2);
        }

        g.text(c, "Pastikan kedua perangkat berada di jaringan yang sama.",
                w / 2f, h - 26 * u, 12 * u, Gfx.DIM, 1, false);
    }

    // --------------------------------------------------------------- online
    private void drawOnline(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Menu", BACK, 3);
        title(c, g, "CO-OP ONLINE", 54 * u, 30 * u);
        g.text(c, "Server internet  •  2-4 pemain  •  tanpa hotspot", w / 2f, 98 * u,
                13 * u, Gfx.DIM, 1, false);

        OnlineCoop o = game.online;
        float pw = Math.min(860 * u, w - 40 * u);
        float ph = Math.min(620 * u, h - 150 * u);
        float px = w / 2f - pw / 2f;
        float py = h * 0.16f;
        g.panel(c, px, py, pw, ph, 0xE6150F24, 0x779B5DE5);

        if (o == null) {
            g.text(c, "Menyiapkan layanan online...", w / 2f, py + ph / 2f, 16 * u,
                    Gfx.DIM, 1, false);
            return;
        }

        boolean logged = o.loggedIn;
        if (!logged) {
            g.text(c, "Masuk dengan username dan password.", w / 2f, py + 66 * u,
                    15 * u, Gfx.TEXT, 1, true);
            float rowX = px + 50 * u;
            float rowW = pw - 100 * u;
            String user = game.onlineUser.length() == 0 ? "Belum diisi" : game.onlineUser;
            String pass = game.onlinePass.length() == 0 ? "Belum diisi" : "********";
            button(c, g, rowX, py + 100 * u, rowW, 52 * u, "Username: " + user,
                    ONLINE_USERNAME, 3);
            button(c, g, rowX, py + 164 * u, rowW, 52 * u, "Password: " + pass,
                    ONLINE_PASSWORD, 3);
            float half = (rowW - 12 * u) / 2f;
            button(c, g, rowX, py + 238 * u, half, 50 * u, "Masuk", ONLINE_LOGIN, 1);
            button(c, g, rowX + half + 12 * u, py + 238 * u, half, 50 * u,
                    "Daftar", ONLINE_REGISTER, 0);
            g.text(c, "Username 3-16 huruf/angka/_  •  password minimal 6.",
                    w / 2f, py + 316 * u, 12 * u, Gfx.DIM, 1, false);
            g.text(c, "Akun ini hanya untuk co-op Mystic Realm.", w / 2f,
                    py + 340 * u, 12 * u, Gfx.DIM, 1, false);
        } else {
            g.text(c, "Masuk sebagai", px + 50 * u, py + 50 * u, 13 * u, Gfx.DIM, 0, false);
            g.textShadow(c, o.username, px + 50 * u, py + 76 * u, 24 * u, Gfx.GOLD, 0, true);
            button(c, g, px + pw - 180 * u, py + 34 * u, 130 * u, 42 * u,
                    "Keluar Akun", ONLINE_LOGOUT, 2);

            if (!o.hasRoom()) {
                g.text(c, "Buat room baru atau masukkan kode room teman.",
                        w / 2f, py + 128 * u, 14 * u, Gfx.TEXT, 1, true);
                float rowX = px + 50 * u;
                float rowW = pw - 100 * u;
                String code = game.onlineCode.length() == 0 ? "Kode room" : game.onlineCode;
                button(c, g, rowX, py + 164 * u, rowW, 54 * u, code, ONLINE_CODE, 3);
                float half = (rowW - 12 * u) / 2f;
                button(c, g, rowX, py + 238 * u, half, 50 * u, "Buat Room", ONLINE_CREATE, 1);
                button(c, g, rowX + half + 12 * u, py + 238 * u, half, 50 * u,
                        "Gabung Room", ONLINE_JOIN, 0);
                g.text(c, "Host memilih stage; semua pemain mengikuti.",
                        w / 2f, py + 320 * u, 12 * u, Gfx.DIM, 1, false);
            } else {
                g.text(c, "Kode Room", px + 50 * u, py + 122 * u, 13 * u, Gfx.DIM, 0, false);
                g.textShadow(c, o.roomCode, px + 50 * u, py + 160 * u, 34 * u, Gfx.CYAN, 0, true);
                g.text(c, o.host ? "Kamu host" : "Kamu anggota", px + 260 * u,
                        py + 142 * u, 15 * u, o.host ? Gfx.GOLD : Gfx.DIM, 0, true);
                g.text(c, o.memberCount() + "/" + o.maxPlayers + " pemain",
                        px + 260 * u, py + 164 * u, 13 * u, Gfx.DIM, 0, false);

                g.text(c, "Pemain", px + 50 * u, py + 210 * u, 13 * u, Gfx.DIM, 0, true);
                List<LanCoop.Remote> list = o.remotes();
                float ly = py + 238 * u;
                int shown = 0;
                for (int i = 0; i < list.size() && shown < 4; i++) {
                    LanCoop.Remote r = list.get(i);
                    String name = r.name == null || r.name.length() == 0 ? "Pemain" : r.name;
                    g.text(c, (r.self ? "• " : "  ") + name + (r.self ? " (kamu)" : ""),
                            px + 50 * u, ly, 14 * u, r.self ? Gfx.GOLD : Gfx.TEXT, 0, true);
                    if (r.id.equals(o.hostId)) {
                        g.text(c, "HOST", px + 360 * u, ly, 11 * u, Gfx.CYAN, 0, true);
                    }
                    ly += 26 * u;
                    shown++;
                }

                if (o.host) {
                    int stage = Gfx.clamp(game.onlineStage, 0, Catalog.STAGE_COUNT - 1);
                    g.text(c, "Stage", px + 50 * u, py + 380 * u, 13 * u, Gfx.DIM, 0, true);
                    button(c, g, px + 130 * u, py + 358 * u, 42 * u, 40 * u,
                            "‹", ONLINE_STAGE_PREV, 3);
                    g.text(c, "Stage " + (stage + 1), px + 196 * u, py + 380 * u,
                            18 * u, Gfx.TEXT, 0, true);
                    button(c, g, px + 300 * u, py + 358 * u, 42 * u, 40 * u,
                            "›", ONLINE_STAGE_NEXT, 3);
                    g.text(c, Catalog.WORLDS[Gfx.clamp(stage / Catalog.STAGES_PER_WORLD, 0, Catalog.WORLD_COUNT - 1)].name,
                            px + 50 * u, py + 410 * u, 12 * u, Gfx.DIM, 0, false);
                    button(c, g, px + 50 * u, py + ph - 76 * u, 240 * u, 50 * u,
                            "Mulai Co-op", ONLINE_START, o.connected ? 1 : 3);
                    button(c, g, px + pw - 190 * u, py + ph - 76 * u, 140 * u, 50 * u,
                            "Keluar", ONLINE_LEAVE, 2);
                } else {
                    g.text(c, "Menunggu host memilih stage dan memulai...",
                            px + 50 * u, py + 390 * u, 14 * u, Gfx.ORANGE, 0, true);
                    button(c, g, px + 50 * u, py + ph - 76 * u, 180 * u, 50 * u,
                            "Keluar Room", ONLINE_LEAVE, 2);
                }
            }
        }

        button(c, g, px + 50 * u, py + ph - 132 * u, 160 * u, 42 * u,
                "Pindah ke LAN", ONLINE_LAN, 3);

        String status;
        int color = Gfx.DIM;
        if (o.error != null) {
            status = o.error;
            color = Gfx.RED;
        } else if (o.busy) {
            status = "Memproses...";
            color = Gfx.ORANGE;
        } else if (!o.loggedIn) {
            status = "Belum login.";
        } else if (!o.hasRoom()) {
            status = "Siap membuat atau bergabung ke room.";
            color = Gfx.GREEN;
        } else if (!o.connected) {
            status = "Menyambung ke room...";
            color = Gfx.ORANGE;
        } else {
            status = "Room aktif  •  " + o.memberCount() + "/" + o.maxPlayers + " pemain";
            color = Gfx.GREEN;
        }
        g.text(c, status, w / 2f, h - 28 * u, 12 * u, color, 1, false);
    }

    // ------------------------------------------------------------ settings
    private void drawSettings(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        bg(c, g);
        button(c, g, 18 * u, 18 * u, 90 * u, 42 * u, "‹ Kembali", BACK, 3);
        title(c, g, "PENGATURAN", 54 * u, 30 * u);

        float pw = Math.min(720 * u, w - 60 * u);
        float px = w / 2f - pw / 2f;
        float rowH = 64 * u;
        float gap = 10 * u;
        float y = (h - (rowH * 5 + gap * 4)) / 2f + 12 * u;
        sliderRow(c, g, px, y, pw, "Volume Musik", save().musicVolume,
                Math.round(save().musicVolume * 100) + "%", SET_MUSIC_DOWN, SET_MUSIC_UP, u);
        sliderRow(c, g, px, y + rowH + gap, pw, "Volume Efek Suara", save().sfxVolume,
                Math.round(save().sfxVolume * 100) + "%", SET_SFX_DOWN, SET_SFX_UP, u);
        sliderRow(c, g, px, y + (rowH + gap) * 2, pw, "Ukuran Tombol",
                (save().buttonScale - 0.75f) / 0.6f,
                Math.round(save().buttonScale * 100) + "%", SET_BTN_DOWN, SET_BTN_UP, u);
        sliderRow(c, g, px, y + (rowH + gap) * 3, pw, "Sensitivitas Analog",
                (save().sensitivity - 0.4f) / 1.6f,
                (Math.round(save().sensitivity * 10) / 10f) + "x", SET_SENS_DOWN, SET_SENS_UP, u);
        toggleRow(c, g, px, y + (rowH + gap) * 4, pw, "Grafik Tinggi", save().highQuality, SET_QUALITY, u);
        button(c, g, px, y + (rowH + gap) * 5 + 4 * u, pw, 44 * u, "Nama Pemain: " + save().playerName, MENU_NAME, 0);
        g.text(c, "Kualitas tinggi menambah dekorasi dunia dan partikel.",
                w / 2f, y + (rowH + gap) * 5 + 64 * u, 12 * u, Gfx.DIM, 1, false);
        g.text(c, "Mystic Realm  •  v1.2  •  dibuat murni dengan Canvas  •  Meon-XD",
                w / 2f, h - 26 * u, 12 * u, Gfx.GOLD, 1, true);
    }

    private void sliderRow(Canvas c, Gfx g, float x, float y, float w, String label,
                           float pct, String valueText, int downId, int upId, float u) {
        pct = Gfx.clamp(pct, 0f, 1f);
        g.panel(c, x, y, w, 64 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, label, x + 22 * u, y + 22 * u, 15 * u, Gfx.TEXT, 0, true);
        float bw = 120 * u;
        float bx = x + w - bw - 84 * u;
        g.roundRect(c, bx, y + 18 * u, bw, 12 * u, 6 * u, 0xCC332C48);
        g.roundRect(c, bx + 1, y + 19 * u, (bw - 2) * pct, 10 * u, 5 * u, 0xCC9B5DE5);
        g.text(c, valueText, bx + bw / 2f, y + 48 * u,
                10 * u, Gfx.DIM, 1, false);
        button(c, g, x + w - 74 * u, y + 13 * u, 40 * u, 38 * u, "+", upId, 3);
        button(c, g, bx - 52 * u, y + 13 * u, 40 * u, 38 * u, "-", downId, 3);
    }

    private void toggleRow(Canvas c, Gfx g, float x, float y, float w, String label, boolean on, int id, float u) {
        g.panel(c, x, y, w, 62 * u, 0xCC150F24, 0x446B5A96);
        g.text(c, label, x + 24 * u, y + 31 * u, 18 * u, Gfx.TEXT, 0, true);
        float tw = 110 * u, th = 42 * u;
        float tx = x + w - tw - 20 * u, ty = y + 10 * u;
        g.roundRect(c, tx, ty, tw, th, th / 2f, on ? 0xCC43D17A : 0xCC332C48);
        g.circle(c, on ? tx + tw - th / 2f : tx + th / 2f, ty + th / 2f, th / 2f - 4 * u, 0xFFF4EEE4);
        g.text(c, on ? "ON" : "OFF", tx + tw / 2f + (on ? -14 * u : 14 * u), ty + th / 2f, 12 * u,
                on ? 0xFF0E2418 : Gfx.DIM, 1, true);
        reg(x, y, w, 62 * u, id);
    }

    // -------------------------------------------------------------- result

    private void drawResult(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        Gameplay gp = game.gp;
        boolean won = gp != null && gp.won;
        boolean finalWin = won && gp != null && gp.stageIndex >= Catalog.STAGE_COUNT - 1;
        bg(c, g);

        int accent = won ? Gfx.GOLD : Gfx.RED;
        g.circle(c, w / 2f, h * 0.30f, 160 * u, Gfx.withAlpha(accent, 22));
        g.circleStroke(c, w / 2f, h * 0.30f, 160 * u, Gfx.withAlpha(accent, 80), 2 * u);
        String title = finalWin ? "DUNIA TERSELAMATKAN" : (won ? "STAGE CLEAR" : "KAU GUGUR");
        g.textShadow(c, title, w / 2f, h * 0.22f, finalWin ? 46 * u : 52 * u, accent, 1, true);
        g.text(c, gp == null ? "" : gp.rSub, w / 2f, h * 0.22f + 44 * u, 16 * u, Gfx.DIM, 1, false);
        if (finalWin) {
            g.text(c, "Sepuluh dunia, sepuluh penjaga, satu takdir.", w / 2f, h * 0.22f + 70 * u, 14 * u, Gfx.CYAN, 1, false);
        }

        float pw = Math.min(520 * u, w - 80 * u);
        float px = w / 2f - pw / 2f, py = h * 0.40f;
        if (gp != null) {
            g.panel(c, px, py, pw, 178 * u, 0xEE160F28, 0x557A64B4);
            if (won) {
                g.text(c, "EXP  +" + gp.rExp, px + 30 * u, py + 30 * u, 15 * u, Gfx.CYAN, 0, true);
                g.text(c, "GOLD  +" + gp.rGold, px + 30 * u, py + 58 * u, 15 * u, Gfx.GOLD, 0, true);
                g.text(c, "SHARD  +" + gp.rShards, px + 30 * u, py + 86 * u, 15 * u, Gfx.ACCENT, 0, true);
                g.text(c, "Level " + save().level, px + pw - 30 * u, py + 30 * u, 15 * u, Gfx.TEXT, 2, true);
                g.text(c, "Gold total " + save().gold, px + pw - 30 * u, py + 58 * u, 13 * u, Gfx.DIM, 2, false);
                g.text(c, "Waktu " + (gp.rTime < 60 ? String.format("%.1fs", gp.rTime) : (int) (gp.rTime / 60) + "m " + (int) (gp.rTime % 60) + "s"),
                        px + pw - 30 * u, py + 86 * u, 13 * u, Gfx.DIM, 2, false);
            } else {
                g.text(c, "Kau terjatuh sebelum menyelesaikan stage.", px + pw / 2f, py + 40 * u, 15 * u, Gfx.TEXT, 1, false);
                g.text(c, "Tingkatkan perlengkapan, lalu coba lagi.", px + pw / 2f, py + 70 * u, 13 * u, Gfx.DIM, 1, false);
                g.text(c, "Level " + save().level + "   Gold " + save().gold + "   ◆ " + save().shards,
                        px + pw / 2f, py + 108 * u, 14 * u, Gfx.GOLD, 1, true);
            }
            g.text(c, "Teka-teki: " + (gp.puzzleResolved ? (gp.perfectPuzzle ? "Sempurna" : "Terpecahkan") : "Tidak"),
                    px + pw / 2f, py + 148 * u, 12 * u, gp.puzzleResolved ? Gfx.GREEN : Gfx.DIM, 1, false);
        }

        float bw = 200 * u, gap = 16 * u;
        float bx = w / 2f - (bw * 2 + gap) / 2f;
        float by = py + 208 * u;
        if (won && gp != null && gp.stageIndex < Catalog.STAGE_COUNT - 1) {
            button(c, g, bx, by, bw, 56 * u, "Lanjut", RESULT_NEXT, 1);
            button(c, g, bx + bw + gap, by, bw, 56 * u, "Peta", RESULT_MAP, 0);
        } else if (won) {
            button(c, g, bx, by, bw, 56 * u, "Peta Dunia", RESULT_MAP, 1);
            button(c, g, bx + bw + gap, by, bw, 56 * u, "Ulangi", RESULT_RETRY, 0);
        } else {
            button(c, g, bx, by, bw, 56 * u, "Coba Lagi", RESULT_RETRY, 1);
            button(c, g, bx + bw + gap, by, bw, 56 * u, "Peta", RESULT_MAP, 0);
        }
    }

    // ------------------------------------------------------------- loading

    private void drawLoading(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        float t = Gfx.clamp(game.loadingT / game.loadingDuration, 0f, 1f);
        float cx = w / 2f, cy = h * 0.47f;
        float rot = game.loadingT;

        g.gradientRect(c, 0, 0, w, h, 0xFF090617, 0xFF020107);
        g.circle(c, w * 0.18f, h * 0.24f, 250 * u, Gfx.withAlpha(Gfx.ACCENT, 18));
        g.circle(c, w * 0.84f, h * 0.32f, 210 * u, Gfx.withAlpha(Gfx.CYAN, 13));
        g.circle(c, w * 0.52f, h * 0.74f, 280 * u, Gfx.withAlpha(Gfx.GOLD, 9));
        g.vignette(c, w / 2f, h / 2f, (float) Math.hypot(w, h) * 0.64f);

        for (int i = 0; i < 54; i++) {
            float a = i * 2.399f + rot * 0.12f;
            float rr = (50 + (i * 47) % 230) * u;
            float px = cx + (float) Math.cos(a) * rr;
            float py = cy + (float) Math.sin(a) * rr * 0.58f;
            float size = (1.1f + (i % 3) * 0.7f) * u;
            g.circle(c, px, py, size, Gfx.withAlpha(i % 4 == 0 ? Gfx.GOLD : Gfx.TEXT, 35 + (i % 5) * 16));
        }

        float ringX = 158 * u, ringY = 116 * u;
        for (int i = 0; i < 12; i++) {
            float a = rot * 0.55f + i * 0.5235988f;
            float px = cx + (float) Math.cos(a) * ringX;
            float py = cy + (float) Math.sin(a) * ringY;
            float s = (5 + (i % 3) * 2) * u;
            int col = Gfx.withAlpha(i % 2 == 0 ? Gfx.GOLD : Gfx.CYAN, 95 + (i % 4) * 28);
            g.line(c, px, py - s, px + s, py, col, 1.4f * u);
            g.line(c, px + s, py, px, py + s, col, 1.4f * u);
            g.line(c, px, py + s, px - s, py, col, 1.4f * u);
            g.line(c, px - s, py, px, py - s, col, 1.4f * u);
        }

        float pulse = 1f + 0.035f * (float) Math.sin(rot * 2.4f);
        float pr = 112 * u * pulse;
        g.circle(c, cx, cy, pr * 1.65f, Gfx.withAlpha(Gfx.ACCENT, 10));
        g.circle(c, cx, cy, pr * 1.25f, Gfx.withAlpha(Gfx.CYAN, 12));
        g.circle(c, cx, cy, pr, Gfx.withAlpha(Gfx.ACCENT, 24));
        g.circleStroke(c, cx, cy, pr, Gfx.withAlpha(Gfx.GOLD, 120), 2.2f * u);
        g.circleStroke(c, cx, cy, pr * 0.78f, Gfx.withAlpha(Gfx.CYAN, 85), 1.4f * u);

        g.tmp.set(cx - pr * 1.1f, cy - pr * 1.1f, cx + pr * 1.1f, cy + pr * 1.1f);
        g.stroke(Gfx.withAlpha(Gfx.GOLD, 185), 2.4f * u);
        c.drawArc(g.tmp, rot * 48f, 108f, false, g.ps);
        g.stroke(Gfx.withAlpha(Gfx.CYAN, 170), 1.8f * u);
        c.drawArc(g.tmp, -rot * 67f + 180f, 86f, false, g.ps);
        g.tmp.set(cx - pr * 0.88f, cy - pr * 0.88f, cx + pr * 0.88f, cy + pr * 0.88f);
        g.stroke(Gfx.withAlpha(Gfx.ACCENT, 190), 1.6f * u);
        c.drawArc(g.tmp, rot * 95f + 40f, 60f, false, g.ps);

        for (int i = 0; i < 6; i++) {
            float a = -rot * 0.9f + i * 1.0471976f;
            float px = cx + (float) Math.cos(a) * 34 * u;
            float py = cy + (float) Math.sin(a) * 34 * u;
            g.circle(c, px, py, 3.2f * u, Gfx.withAlpha(Gfx.GOLD, 190));
        }
        float core = 18 * u * (1f + 0.08f * (float) Math.sin(rot * 4f));
        g.circle(c, cx, cy, core * 2.8f, Gfx.withAlpha(Gfx.GOLD, 26));
        g.circle(c, cx, cy, core, Gfx.withAlpha(Gfx.TEXT, 235));

        int titleA = (int) (255 * Gfx.clamp((game.loadingT - 0.15f) / 0.9f, 0f, 1f));
        g.textShadow(c, "MYSTIC", cx, h * 0.165f, 62 * u, Gfx.withAlpha(Gfx.TEXT, titleA), 1, true);
        g.textShadow(c, "REALM", cx, h * 0.165f + 58 * u, 62 * u, Gfx.withAlpha(Gfx.GOLD, titleA), 1, true);
        int subA = (int) (255 * Gfx.clamp((game.loadingT - 0.55f) / 0.8f, 0f, 1f));
        g.text(c, "GERBANG TAKDIR SEDANG DIBUKA", cx, h * 0.285f, 13 * u,
                Gfx.withAlpha(Gfx.DIM, subA), 1, true);

        float bw = Math.min(420 * u, w - 80 * u);
        float bh = 10 * u;
        float bx = cx - bw / 2f;
        float by = h * 0.79f;
        g.roundRect(c, bx, by, bw, bh, bh / 2f, 0x66100C1E);
        g.roundRect(c, bx, by, bw * t, bh, bh / 2f, Gfx.withAlpha(Gfx.ACCENT, 225));
        if (t > 0.01f) {
            g.circle(c, bx + bw * t, by + bh / 2f, 5 * u, Gfx.withAlpha(Gfx.GOLD, 190));
        }
        g.text(c, (int) (t * 100) + "%", cx, by + 28 * u, 12 * u, Gfx.DIM, 1, true);

        int dots = ((int) (game.loadingT * 3f)) % 4;
        g.text(c, "Menyiapkan dunia" + (dots == 0 ? "" : dots == 1 ? "." : dots == 2 ? ".." : "..."),
                cx, by + 52 * u, 13 * u, Gfx.DIM, 1, false);
        if (game.loadingT > 0.7f) {
            float a = 0.55f + 0.25f * (float) Math.sin(game.loadingT * 3f);
            g.text(c, "Ketuk untuk melewati", cx, h - 28 * u, 11 * u,
                    Gfx.withAlpha(Gfx.DIM, (int) (200 * a)), 1, false);
        }
    }

    // --------------------------------------------------------------- input

    private void key(Canvas c, Gfx g, float x, float y, float w, float h, String label, int id, int style) {
        button(c, g, x, y, w, h, label, id, style);
    }

    private void drawInput(Canvas c, Gfx g) {
        float w = W(), h = H(), u = u();
        g.rect(c, 0, 0, w, h, 0xB8000000);

        float pw = Math.min(1000 * u, w - 40 * u);
        float ph = Math.min(650 * u, h - 40 * u);
        float px = w / 2f - pw / 2f;
        float py = h / 2f - ph / 2f;
        g.roundRect(c, px + 4 * u, py + 8 * u, pw, ph, 20 * u, 0x66000000);
        g.roundRect(c, px, py, pw, ph, 20 * u, 0xF0100B20);
        g.roundRectStroke(c, px, py, pw, ph, 20 * u, Gfx.withAlpha(Gfx.GOLD, 150), 2 * u);

        g.textShadow(c, game.inputTitle, px + pw / 2f, py + 40 * u, 24 * u, Gfx.GOLD, 1, true);
        g.line(c, px + pw / 2f - 70 * u, py + 62 * u, px + pw / 2f + 70 * u, py + 62 * u,
                Gfx.withAlpha(Gfx.GOLD, 90), 1.2f * u);
        g.text(c, game.inputMessage, px + pw / 2f, py + 84 * u, 13 * u, Gfx.DIM, 1, false);

        float fx = px + 40 * u, fy = py + 112 * u;
        float fw = pw - 80 * u, fh = 72 * u;
        g.roundRect(c, fx, fy, fw, fh, 12 * u, 0xCC0B0716);
        g.roundRectStroke(c, fx, fy, fw, fh, 12 * u,
                game.inputError ? Gfx.withAlpha(Gfx.RED, 220) : Gfx.withAlpha(Gfx.ACCENT, 190), 1.8f * u);
        boolean empty = game.inputValue.length() == 0;
        String shown;
        if (empty) {
            shown = game.inputHint;
        } else if (game.inputPassword) {
            StringBuilder mask = new StringBuilder();
            for (int i = 0; i < game.inputValue.length(); i++) mask.append('*');
            shown = mask.toString();
        } else {
            shown = game.inputValue;
        }
        float maxTextW = fw - 120 * u;
        while (g.textWidth(shown, 28 * u, true) > maxTextW && shown.length() > 1) {
            shown = shown.substring(1);
        }
        g.text(c, shown, fx + 24 * u, fy + fh / 2f, 28 * u,
                empty ? (game.inputError ? Gfx.RED : Gfx.DIM) : Gfx.TEXT, 0, true);
        if (!empty && ((int) (game.uiTime * 2f)) % 2 == 0) {
            float tw = g.textWidth(shown, 28 * u, true);
            float cursorX = Math.min(fx + 24 * u + tw + 3 * u, fx + fw - 74 * u);
            g.rect(c, cursorX, fy + 17 * u, 2 * u, fh - 34 * u, Gfx.GOLD);
        }
        g.text(c, game.inputValue.length() + "/" + game.inputMax, fx + fw - 20 * u,
                fy + fh / 2f, 11 * u, Gfx.DIM, 2, false);

        float gap = 8 * u;
        float margin = 40 * u;
        float kh = 54 * u;
        float top = fy + fh + 24 * u;

        if (game.inputNumeric) {
            char[][] rows = {{'1', '2', '3', '4', '5'}, {'6', '7', '8', '9', '0'}};
            float kw = (pw - 2 * margin - 4 * gap) / 5f;
            for (int r = 0; r < rows.length; r++) {
                float rowW = rows[r].length * kw + (rows[r].length - 1) * gap;
                float sx = px + (pw - rowW) / 2f;
                float yy = top + r * (kh + gap);
                for (int col = 0; col < rows[r].length; col++) {
                    char ch = rows[r][col];
                    key(c, g, sx + col * (kw + gap), yy, kw, kh, String.valueOf(ch),
                            INPUT_KEY_BASE + ch, 0);
                }
            }
            float yy = top + 2 * (kh + gap);
            key(c, g, px + margin, yy, kw, kh, ".", INPUT_KEY_BASE + '.', 0);
            button(c, g, px + margin + kw + gap, yy, kw * 2 + gap, kh, "HAPUS", INPUT_BACKSPACE, 2);
            button(c, g, px + margin + kw * 3 + gap * 2, yy, kw * 2 + gap, kh, "KOSONG", INPUT_CLEAR, 0);
        } else {
            char[][] rows = {
                    {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'},
                    {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'},
                    {'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'},
                    {'Z', 'X', 'C', 'V', 'B', 'N', 'M'}
            };
            float kw = (pw - 2 * margin - 9 * gap) / 10f;
            for (int r = 0; r < rows.length; r++) {
                float rowW = rows[r].length * kw + (rows[r].length - 1) * gap;
                float sx = px + (pw - rowW) / 2f;
                float yy = top + r * (kh + gap);
                for (int col = 0; col < rows[r].length; col++) {
                    char ch = rows[r][col];
                    String label = game.inputShift ? String.valueOf(ch) : String.valueOf(Character.toLowerCase(ch));
                    key(c, g, sx + col * (kw + gap), yy, kw, kh, label, INPUT_KEY_BASE + ch, 0);
                }
            }
            float yy = top + 4 * (kh + gap);
            float shiftW = 120 * u;
            float dashW = 64 * u;
            float underW = 64 * u;
            float backW = 120 * u;
            float spaceW = pw - 2 * margin - shiftW - dashW - underW - backW - 4 * gap;
            key(c, g, px + margin, yy, shiftW, kh, "SHIFT", INPUT_SHIFT, game.inputShift ? 1 : 0);
            key(c, g, px + margin + shiftW + gap, yy, spaceW, kh, "SPASI", INPUT_KEY_BASE + ' ', 0);
            key(c, g, px + margin + shiftW + spaceW + 2 * gap, yy, dashW, kh, "-", INPUT_KEY_BASE + '-', 0);
            key(c, g, px + margin + shiftW + spaceW + dashW + 3 * gap, yy, underW, kh,
                    "_", INPUT_KEY_BASE + '_', 0);
            key(c, g, px + margin + shiftW + spaceW + dashW + underW + 4 * gap, yy, backW, kh,
                    "HAPUS", INPUT_BACKSPACE, 2);
        }

        float by = py + ph - 58 * u;
        float clearW = 130 * u;
        float cancelW = 150 * u;
        float okW = 190 * u;
        float total = clearW + cancelW + okW + 2 * gap;
        float sx = px + pw - margin - total;
        button(c, g, sx, by, clearW, 46 * u, "KOSONG", INPUT_CLEAR, 0);
        button(c, g, sx + clearW + gap, by, cancelW, 46 * u, "Batal", INPUT_CANCEL, 2);
        String okLabel = "OK";
        if (game.inputPurpose == Game.INPUT_NAME) okLabel = "Simpan";
        else if (game.inputPurpose == Game.INPUT_IP) okLabel = "Gabung";
        else if (game.inputPurpose == Game.INPUT_USERNAME) okLabel = "Pakai";
        else if (game.inputPurpose == Game.INPUT_PASSWORD) okLabel = "Pakai";
        else if (game.inputPurpose == Game.INPUT_OWNER_PASSWORD) okLabel = "Buka";
        else if (game.inputPurpose == Game.INPUT_ROOM_CODE) okLabel = "Gabung";
        button(c, g, sx + clearW + cancelW + 2 * gap, by, okW, 46 * u,
                okLabel, INPUT_OK, 1);
    }

    private void drawNotice(Canvas c, Gfx g) {
        if (game.noticeT <= 0 || game.notice == null) return;
        float u = u();
        float a = Gfx.clamp(game.noticeT / 0.5f, 0f, 1f);
        float w = g.textWidth(game.notice, 15 * u, true) + 44 * u;
        float y = H() - 120 * u;
        g.roundRect(c, W() / 2f - w / 2f, y - 18 * u, w, 36 * u, 18 * u, Gfx.withAlpha(0xE0150F24, (int) (230 * a)));
        g.text(c, game.notice, W() / 2f, y, 15 * u, Gfx.withAlpha(Gfx.TEXT, (int) (255 * a)), 1, true);
    }
}
