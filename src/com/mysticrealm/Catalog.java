package com.mysticrealm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public final class Catalog {
    public static final int SLOT_WEAPON = 0;
    public static final int SLOT_ARMOR = 1;
    public static final int SLOT_HELMET = 2;
    public static final int SLOT_BOOTS = 3;
    public static final String[] SLOT_NAME = {"Senjata", "Armor", "Helm", "Sepatu"};

    public static final int SKILL_WAR_WHIRL = 1;
    public static final int SKILL_WAR_CRY = 2;
    public static final int SKILL_WAR_SLAM = 3;
    public static final int SKILL_MAG_BOLTS = 4;
    public static final int SKILL_MAG_NOVA = 5;
    public static final int SKILL_MAG_METEOR = 6;
    public static final int SKILL_ASS_STEP = 7;
    public static final int SKILL_ASS_VENOM = 8;
    public static final int SKILL_ASS_EXECUTE = 9;

    public static final int MAX_SKILL_LEVEL = 5;
    public static final int MAX_TRACK_LEVEL = 25;

    public static class Item {
        public String id, name, desc;
        public int slot = -1;
        public int rarity;
        public int tier;
        public int price;
        public int atk, def, hp, mp, spd, crit;
        public String use;
        public int heal, manaGain;
        public boolean material;
    }

    public static class ClassDef {
        public String id, name, desc;
        public int hp, mp, atk, def, spd, crit;
        public int color;
        public String startWeapon;
        public String role;
    }

    public static class Skill {
        public String name, desc;
        public int mp;
        public float cd;
        public int kind;
        public int color;
        public float power;
    }

    public static class Track {
        public String name, desc;
        public int color;
    }

    public static class MonsterDef {
        public String name;
        public int type;
        public int color;
        public int hp, atk, def, spd;
        public int exp, gold;
        public float scale = 1f;
        public float radius = 28f;
        public boolean ranged;
    }

    public static class BossDef {
        public String name, title;
        public int color;
        public int hp, atk, def, spd;
        public float scale = 2.2f;
    }

    public static class WorldDef {
        public String name, subtitle, tag;
        public int groundA, groundB, path, accent, deco;
        public MonsterDef[] mobs;
        public BossDef boss;
    }

    public static class Riddle {
        public String q;
        public String[] opts;
        public int answer;
        public Riddle(String q, String[] o, int a) {
            this.q = q;
            opts = o;
            answer = a;
        }
    }

    public static final LinkedHashMap<String, Item> ITEMS = new LinkedHashMap<>();

    public static final ClassDef[] CLASSES = new ClassDef[3];
    public static final Skill[][] SKILLS = new Skill[3][3];
    public static final Track[] TRACKS = new Track[6];
    public static final WorldDef[] WORLDS = new WorldDef[5];
    public static final String[] DIFFICULTY = {"Mudah", "Normal", "Sulit", "Mimpi Buruk", "Jurang"};
    public static final String[] DIFFICULTY_DESC = {
            "Musuh lebih lemah, cocok untuk cerita.",
            "Pengalaman seimbang, sesuai rancangan.",
            "Musuh lebih kuat dan agresif.",
            "Tantangan ekstrem untuk veteran.",
            "Hanya untuk yang tak takut mati."};
    public static final float[] DIFF_ENEMY = {0.75f, 1f, 1.28f, 1.6f, 2.0f};
    public static final float[] DIFF_REWARD = {0.9f, 1f, 1.25f, 1.5f, 1.8f};
    public static final float[] DIFF_EXP = {0.9f, 1f, 1.3f, 1.6f, 2f};

    public static final Riddle[] RIDDLES = {
            new Riddle("Aku punya kota, tapi tidak punya rumah. Punya hutan, tapi tidak punya pohon. Apa aku?",
                    new String[]{"Peta", "Kerajaan", "Mimpi", "Buku"}, 0),
            new Riddle("Semakin banyak kamu ambil, semakin besar aku. Apa aku?",
                    new String[]{"Lubang", "Api", "Bayangan", "Waktu"}, 0),
            new Riddle("Aku berjalan tanpa kaki, mengalir tanpa air. Apa aku?",
                    new String[]{"Angin", "Waktu", "Sungai", "Asap"}, 1),
            new Riddle("Besi perlu api, api perlu angin, angin perlu ruang. Apa yang paling dulu?",
                    new String[]{"Ruang", "Api", "Besi", "Angin"}, 0),
            new Riddle("Makin kau tinggalkan, makin aku mengikuti. Apa aku?",
                    new String[]{"Bayangan", "Jejak", "Utang", "Rindu"}, 0),
            new Riddle("Banyak gigi tapi tak bisa menggigit. Apa aku?",
                    new String[]{"Sisir", "Gergaji", "Ular", "Roda gigi"}, 0),
            new Riddle("Aku bisa pecah tanpa pernah disentuh. Apa aku?",
                    new String[]{"Janji", "Kaca", "Hati", "Suara"}, 1),
            new Riddle("Kepalaku di ujung, ekorku di awal. Apa aku?",
                    new String[]{"Kata", "Ular", "Panah", "Jalan"}, 0),
            new Riddle("Selalu di depanmu, tapi tak bisa kau lihat. Apa aku?",
                    new String[]{"Masa depan", "Angin", "Bayangan", "Cahaya"}, 0),
            new Riddle("Dua bersaudara, satu di kiri satu di kanan, tak pernah bertemu. Apa mereka?",
                    new String[]{"Sepatu", "Mata", "Telinga", "Sayap"}, 1)
    };

    static {
        initItems();
        initClasses();
        initSkills();
        initTracks();
        initWorlds();
    }

    private static int tierRarity(int t) {
        if (t <= 0) return 0;
        if (t == 1) return 1;
        if (t <= 3) return 2;
        if (t <= 5) return 3;
        return 4;
    }

    private static Item equip(String id, String name, String desc, int slot, int tier, int price,
                              int atk, int def, int hp, int mp, int spd, int crit) {
        Item it = new Item();
        it.id = id;
        it.name = name;
        it.desc = desc;
        it.slot = slot;
        it.tier = tier;
        it.rarity = tierRarity(tier);
        it.price = price;
        it.atk = atk;
        it.def = def;
        it.hp = hp;
        it.mp = mp;
        it.spd = spd;
        it.crit = crit;
        ITEMS.put(id, it);
        return it;
    }

    private static Item use(String id, String name, String desc, int price, String use,
                            int heal, int mana, int rarity, boolean material) {
        Item it = new Item();
        it.id = id;
        it.name = name;
        it.desc = desc;
        it.price = price;
        it.use = use;
        it.heal = heal;
        it.manaGain = mana;
        it.rarity = rarity;
        it.material = material;
        ITEMS.put(id, it);
        return it;
    }

    private static void initItems() {
        equip("wood_sword", "Wooden Sword", "Pedang latihan dari kayu keras.", SLOT_WEAPON, 0, 70, 5, 0, 0, 0, 0, 0);
        equip("iron_sword", "Iron Sword", "Pedang besi standar milik penjaga desa.", SLOT_WEAPON, 1, 200, 11, 0, 0, 0, 0, 0);
        equip("steel_blade", "Steel Blade", "Bilah baja tajam buatan pandai besi.", SLOT_WEAPON, 2, 520, 19, 0, 0, 0, 0, 1);
        equip("frostfang", "Frostfang", "Gigi es abadi yang membekukan luka.", SLOT_WEAPON, 3, 1200, 30, 0, 0, 8, 0, 3);
        equip("emberblade", "Emberblade", "Pedang api dari inti gunung berapi.", SLOT_WEAPON, 4, 2400, 44, 0, 20, 0, 0, 5);
        equip("shadowfang", "Shadowfang", "Taring bayangan yang menembus cahaya.", SLOT_WEAPON, 5, 4300, 60, 0, 0, 20, 4, 8);
        equip("celestial_edge", "Celestial Edge", "Bilah cahaya peninggalan para dewa.", SLOT_WEAPON, 6, 8000, 82, 0, 40, 30, 6, 12);

        equip("dagger", "Rusty Dagger", "Belati kecil yang cepat dan licik.", SLOT_WEAPON, 0, 90, 6, 0, 0, 0, 2, 3);
        equip("venom_dagger", "Venom Dagger", "Belati berlapis racun laba-laba.", SLOT_WEAPON, 2, 560, 17, 0, 0, 0, 4, 6);
        equip("shadow_dagger", "Shadow Dagger", "Belati yang lahir dari kegelapan murni.", SLOT_WEAPON, 4, 2500, 38, 0, 0, 10, 6, 10);

        equip("apprentice_staff", "Apprentice Staff", "Tongkat magang penyihir.", SLOT_WEAPON, 0, 90, 3, 0, 0, 15, 0, 0);
        equip("runed_staff", "Runed Staff", "Tongkat berukir rune kuno.", SLOT_WEAPON, 2, 560, 13, 0, 0, 30, 0, 2);
        equip("arcane_staff", "Arcane Staff", "Tongkat kristal berisi mana murni.", SLOT_WEAPON, 4, 2500, 30, 0, 0, 60, 0, 4);

        equip("cloth_robe", "Cloth Robe", "Jubah sederhana dari kain tenun.", SLOT_ARMOR, 0, 70, 0, 3, 10, 0, 0, 0);
        equip("leather_armor", "Leather Armor", "Armor kulit yang ringan dan lentur.", SLOT_ARMOR, 1, 200, 0, 7, 20, 0, 0, 0);
        equip("chainmail", "Chainmail", "Rantai baja yang melindungi tubuh.", SLOT_ARMOR, 2, 520, 0, 13, 35, 0, 0, 0);
        equip("frost_mail", "Frost Mail", "Armor es yang menyerap hawa dingin.", SLOT_ARMOR, 3, 1200, 0, 20, 60, 0, 0, 2);
        equip("magma_plate", "Magma Plate", "Pelat magma yang mengeras seiring waktu.", SLOT_ARMOR, 4, 2400, 0, 30, 100, 0, 0, 0);
        equip("shadow_cloak", "Shadow Cloak", "Mantel bayangan yang menipu mata.", SLOT_ARMOR, 5, 4300, 0, 42, 140, 0, 0, 5);
        equip("celestial_aegis", "Celestial Aegis", "Perisai cahaya dari reruntuhan langit.", SLOT_ARMOR, 6, 8000, 0, 58, 210, 0, 0, 0);

        equip("leather_cap", "Leather Cap", "Topi kulit penjaga hutan.", SLOT_HELMET, 0, 60, 0, 2, 5, 0, 0, 0);
        equip("iron_helm", "Iron Helm", "Helm besi yang sedikit berkarat.", SLOT_HELMET, 1, 180, 0, 4, 10, 0, 0, 0);
        equip("crystal_helm", "Crystal Helm", "Helm kristal yang memantulkan cahaya.", SLOT_HELMET, 2, 480, 0, 8, 18, 0, 0, 1);
        equip("frost_crown", "Frost Crown", "Mahkota es milik raja musim dingin.", SLOT_HELMET, 3, 1100, 0, 13, 28, 0, 0, 2);
        equip("demon_horns", "Demon Horns", "Tanduk setan yang menakutkan musuh.", SLOT_HELMET, 4, 2300, 4, 19, 42, 0, 0, 0);
        equip("shadow_hood", "Shadow Hood", "Kerudung yang menyembunyikan wajah.", SLOT_HELMET, 5, 4200, 0, 26, 58, 0, 0, 6);
        equip("celestial_halo", "Celestial Halo", "Lingkar cahaya para malaikat.", SLOT_HELMET, 6, 7800, 0, 36, 85, 0, 0, 4);

        equip("worn_boots", "Worn Boots", "Sepatu usang tapi masih berguna.", SLOT_BOOTS, 0, 50, 0, 1, 0, 0, 3, 0);
        equip("swift_boots", "Swift Boots", "Sepatu ringan untuk berlari cepat.", SLOT_BOOTS, 1, 170, 0, 3, 0, 0, 6, 0);
        equip("iron_greaves", "Iron Greaves", "Pelindung kaki dari besi tempa.", SLOT_BOOTS, 2, 450, 0, 5, 4, 0, 9, 0);
        equip("frost_treads", "Frost Treads", "Sepatu yang membekukan jejak kaki.", SLOT_BOOTS, 3, 1050, 0, 8, 8, 0, 12, 0);
        equip("lava_striders", "Lava Striders", "Sepatu dari kulit magma.", SLOT_BOOTS, 4, 2200, 0, 12, 12, 0, 16, 0);
        equip("shadow_steps", "Shadow Steps", "Langkah yang tak terdengar.", SLOT_BOOTS, 5, 4000, 0, 16, 18, 0, 21, 2);
        equip("celestial_wings", "Celestial Wings", "Sayap kecil di tumit pemberani.", SLOT_BOOTS, 6, 7500, 0, 22, 25, 0, 28, 1);

        use("health_potion", "Health Potion", "Memulihkan 50 HP.", 35, "heal", 50, 0, 0, false);
        use("mana_potion", "Mana Potion", "Memulihkan 40 MP.", 35, "mana", 0, 40, 0, false);
        use("greater_health", "Greater Health", "Memulihkan 150 HP.", 110, "heal", 150, 0, 1, false);
        use("greater_mana", "Greater Mana", "Memulihkan 120 MP.", 110, "mana", 0, 120, 1, false);
        use("elixir", "Mystic Elixir", "Memulihkan seluruh HP dan MP.", 280, "full", 9999, 9999, 2, false);
        use("shard", "Mystic Shard", "Pecahan kristal untuk meningkatkan equipment.", 45, null, 0, 0, 1, true);
    }

    private static void initClasses() {
        ClassDef w = new ClassDef();
        w.id = "warrior";
        w.name = "Warrior";
        w.role = "Petarung jarak dekat";
        w.desc = "Badan kokoh dan tebasan mematikan. Menyerap banyak kerusakan sebelum tumbang.";
        w.hp = 220;
        w.mp = 60;
        w.atk = 18;
        w.def = 12;
        w.spd = 98;
        w.crit = 5;
        w.color = 0xFFE07A5F;
        w.startWeapon = "wood_sword";

        ClassDef m = new ClassDef();
        m.id = "mage";
        m.name = "Mage";
        m.role = "Penyihir elemental";
        m.desc = "Rapuh namun menghancurkan dari jauh. Kuasai api, es, dan meteor.";
        m.hp = 150;
        m.mp = 160;
        m.atk = 14;
        m.def = 7;
        m.spd = 100;
        m.crit = 5;
        m.color = 0xFF6C8CFF;
        m.startWeapon = "apprentice_staff";

        ClassDef a = new ClassDef();
        a.id = "assassin";
        a.name = "Assassin";
        a.role = "Pembunuh bayangan";
        a.desc = "Kilat dan mematikan. Menyerang dari sisi lemah dan menghilang sebelum dibalas.";
        a.hp = 170;
        a.mp = 90;
        a.atk = 16;
        a.def = 8;
        a.spd = 118;
        a.crit = 15;
        a.color = 0xFF43D17A;
        a.startWeapon = "dagger";

        CLASSES[0] = w;
        CLASSES[1] = m;
        CLASSES[2] = a;
    }

    private static Skill sk(String name, String desc, int mp, float cd, int kind, int color, float power) {
        Skill s = new Skill();
        s.name = name;
        s.desc = desc;
        s.mp = mp;
        s.cd = cd;
        s.kind = kind;
        s.color = color;
        s.power = power;
        return s;
    }

    private static void initSkills() {
        SKILLS[0][0] = sk("Whirlwind", "Berputar menebas semua musuh di sekitar.", 18, 4f, SKILL_WAR_WHIRL, 0xFFFFB13B, 1.8f);
        SKILLS[0][1] = sk("War Cry", "Serangan +45% selama 8 detik dan mengagetkan musuh.", 22, 12f, SKILL_WAR_CRY, 0xFFE07A5F, 0f);
        SKILLS[0][2] = sk("Earth Shatter", "Menghantam tanah: ledakan luas dan stun.", 35, 18f, SKILL_WAR_SLAM, 0xFFF28C38, 2.8f);

        SKILLS[1][0] = sk("Arcane Bolts", "Tiga proyektil sihir mengejar arah serang.", 20, 3f, SKILL_MAG_BOLTS, 0xFF6C8CFF, 1.1f);
        SKILLS[1][1] = sk("Frost Nova", "Ledakan es di sekitar, memperlambat musuh.", 30, 9f, SKILL_MAG_NOVA, 0xFF4EA8DE, 1.5f);
        SKILLS[1][2] = sk("Meteor", "Memanggil meteor di area musuh terdekat.", 45, 20f, SKILL_MAG_METEOR, 0xFFFF6B4A, 3.4f);

        SKILLS[2][0] = sk("Shadow Step", "Berkedip ke depan, menebas sepanjang jalan.", 16, 5f, SKILL_ASS_STEP, 0xFF43D17A, 1.7f);
        SKILLS[2][1] = sk("Venom Blades", "Melempar lima belati beracun.", 22, 8f, SKILL_ASS_VENOM, 0xFF7FE08A, 1.1f);
        SKILLS[2][2] = sk("Assassinate", "Berpindah ke musuh terdekat dan menebas fatal.", 40, 16f, SKILL_ASS_EXECUTE, 0xFFB07CFF, 4.4f);
    }

    private static Track tr(String name, String desc, int color) {
        Track t = new Track();
        t.name = name;
        t.desc = desc;
        t.color = color;
        return t;
    }

    private static void initTracks() {
        TRACKS[0] = tr("Vitality", "+8 Max HP per level", 0xFF43D17A);
        TRACKS[1] = tr("Power", "+3 Attack per level", 0xFFE5484D);
        TRACKS[2] = tr("Guard", "+2 Defense per level", 0xFF4EA8DE);
        TRACKS[3] = tr("Spirit", "+6 Max MP per level", 0xFF6C8CFF);
        TRACKS[4] = tr("Agility", "+1% Crit & +1 Speed", 0xFFF2A93B);
        TRACKS[5] = tr("Fortune", "+2 Luck (drop & emas)", 0xFFB07CFF);
    }

    private static MonsterDef mob(String name, int type, int color, int hp, int atk, int def, int spd,
                                  int exp, int gold, boolean ranged, float scale, float radius) {
        MonsterDef m = new MonsterDef();
        m.name = name;
        m.type = type;
        m.color = color;
        m.hp = hp;
        m.atk = atk;
        m.def = def;
        m.spd = spd;
        m.exp = exp;
        m.gold = gold;
        m.ranged = ranged;
        m.scale = scale;
        m.radius = radius;
        return m;
    }

    private static BossDef boss(String name, String title, int color, int hp, int atk, int def, int spd, float scale) {
        BossDef b = new BossDef();
        b.name = name;
        b.title = title;
        b.color = color;
        b.hp = hp;
        b.atk = atk;
        b.def = def;
        b.spd = spd;
        b.scale = scale;
        return b;
    }

    private static void initWorlds() {
        WorldDef w1 = new WorldDef();
        w1.name = "Whispering Forest";
        w1.subtitle = "Hutan Bisikan";
        w1.tag = "Awal dari segalanya. Kabut menyembunyikan rahasia tua.";
        w1.groundA = 0xFF1C3527;
        w1.groundB = 0xFF203C2B;
        w1.path = 0xFF2B4E36;
        w1.accent = 0xFF7FD17F;
        w1.deco = 0xFF14291D;
        w1.mobs = new MonsterDef[]{
                mob("Slime", 0, 0xFF6FBF73, 40, 9, 1, 55, 14, 10, false, 1f, 26),
                mob("Goblin", 1, 0xFFC7A15A, 55, 12, 2, 78, 18, 14, false, 1f, 24),
                mob("Dire Wolf", 2, 0xFF8C8F9B, 48, 14, 1, 100, 20, 12, false, 1f, 27)
        };
        w1.boss = boss("Forest Guardian", "Penjaga Hutan", 0xFF7FD17F, 700, 26, 6, 55, 2.3f);

        WorldDef w2 = new WorldDef();
        w2.name = "Frozen Abyss";
        w2.subtitle = "Jurang Beku";
        w2.tag = "Dingin menusuk tulang. Es menyimpan kisah yang membeku.";
        w2.groundA = 0xFF18283F;
        w2.groundB = 0xFF1D3049;
        w2.path = 0xFF294568;
        w2.accent = 0xFF9BD4FF;
        w2.deco = 0xFF122034;
        w2.mobs = new MonsterDef[]{
                mob("Ice Wolf", 2, 0xFF8FD4FF, 70, 18, 3, 105, 26, 18, false, 1.05f, 27),
                mob("Frost Mage", 4, 0xFF6C8CFF, 55, 20, 2, 70, 30, 22, true, 1f, 25),
                mob("Snow Golem", 3, 0xFFBFD8E8, 120, 16, 8, 40, 34, 26, false, 1.2f, 34)
        };
        w2.boss = boss("Frost Dragon", "Naga Es", 0xFF9BD4FF, 1500, 40, 10, 62, 2.6f);

        WorldDef w3 = new WorldDef();
        w3.name = "Infernal Kingdom";
        w3.subtitle = "Kerajaan Neraka";
        w3.tag = "Langit terbakar. Takhta api menanti pemburu yang berani.";
        w3.groundA = 0xFF3A1A18;
        w3.groundB = 0xFF42201C;
        w3.path = 0xFF542A24;
        w3.accent = 0xFFFF8A5B;
        w3.deco = 0xFF2A1210;
        w3.mobs = new MonsterDef[]{
                mob("Fire Imp", 9, 0xFFFF7A45, 70, 24, 3, 80, 40, 26, true, 0.95f, 23),
                mob("Lava Golem", 3, 0xFFB34A2E, 150, 26, 10, 42, 46, 32, false, 1.25f, 35),
                mob("Flame Demon", 5, 0xFFFF4D4D, 95, 30, 5, 95, 52, 36, false, 1.1f, 28)
        };
        w3.boss = boss("Inferno King", "Raja Neraka", 0xFFFF6B3D, 2800, 55, 14, 68, 2.8f);

        WorldDef w4 = new WorldDef();
        w4.name = "Shadow Realm";
        w4.subtitle = "Alam Bayangan";
        w4.tag = "Cahaya tak sampai di sini. Hanya bayangan yang berbisik.";
        w4.groundA = 0xFF191326;
        w4.groundB = 0xFF1F1730;
        w4.path = 0xFF2B2043;
        w4.accent = 0xFFB07CFF;
        w4.deco = 0xFF120D1C;
        w4.mobs = new MonsterDef[]{
                mob("Wraith", 5, 0xFF9A7CFF, 100, 34, 5, 85, 60, 40, true, 1.05f, 27),
                mob("Dark Knight", 6, 0xFF5E5470, 170, 32, 14, 60, 68, 48, false, 1.15f, 30),
                mob("Shadow Howler", 2, 0xFF7A6A9B, 110, 38, 6, 115, 62, 42, false, 1.1f, 28)
        };
        w4.boss = boss("Shadow Emperor", "Kaisar Bayangan", 0xFFB07CFF, 4600, 72, 18, 74, 3f);

        WorldDef w5 = new WorldDef();
        w5.name = "Celestial Ruins";
        w5.subtitle = "Reruntuhan Langit";
        w5.tag = "Reruntuhan para dewa. Jawaban terakhir menunggu di puncak.";
        w5.groundA = 0xFF2A2942;
        w5.groundB = 0xFF32315A;
        w5.path = 0xFF43416E;
        w5.accent = 0xFFFFE08A;
        w5.deco = 0xFF1E1D33;
        w5.mobs = new MonsterDef[]{
                mob("Ancient Guardian", 8, 0xFFD9C89A, 200, 42, 16, 55, 85, 60, false, 1.25f, 34),
                mob("Void Wisp", 7, 0xFF7FE0FF, 120, 46, 6, 80, 88, 55, true, 1f, 24),
                mob("Celestial Sentinel", 6, 0xFFE8D9FF, 230, 48, 18, 65, 95, 65, false, 1.2f, 32)
        };
        w5.boss = boss("Ancient God", "Dewa Purba", 0xFFFFE08A, 7500, 95, 24, 80, 3.2f);

        WORLDS[0] = w1;
        WORLDS[1] = w2;
        WORLDS[2] = w3;
        WORLDS[3] = w4;
        WORLDS[4] = w5;
    }

    public static Item item(String id) {
        return ITEMS.get(id);
    }

    public static List<Item> itemsOfSlot(int slot) {
        List<Item> list = new ArrayList<>();
        for (Item it : ITEMS.values()) {
            if (it.slot == slot) list.add(it);
        }
        return list;
    }

    public static List<Item> shopStock(int tierCap) {
        List<Item> list = new ArrayList<>();
        for (Item it : ITEMS.values()) {
            if (it.material) continue;
            if (it.slot >= 0 && it.tier > tierCap) continue;
            if (it.price <= 0) continue;
            list.add(it);
        }
        return list;
    }

    public static Item randomEquipment(Random r, int maxTier, int luck) {
        int cap = Math.max(0, Math.min(6, maxTier));
        List<Item> pool = new ArrayList<>();
        for (Item it : ITEMS.values()) {
            if (it.slot >= 0 && it.tier <= cap) {
                int weight = 1 + (cap - it.tier) * 2 + luck;
                if (it.tier >= 3) weight = Math.max(1, weight - 2);
                for (int i = 0; i < weight; i++) pool.add(it);
            }
        }
        if (pool.isEmpty()) return null;
        return pool.get(r.nextInt(pool.size()));
    }

    public static String statLine(Item it) {
        if (it == null) return "";
        if (it.slot < 0) {
            if (it.heal > 0 && it.manaGain > 0) return "HP +" + it.heal + "  MP +" + it.manaGain;
            if (it.heal > 0) return "HP +" + it.heal;
            if (it.manaGain > 0) return "MP +" + it.manaGain;
            return it.material ? "Material" : "";
        }
        StringBuilder sb = new StringBuilder();
        if (it.atk != 0) sb.append("ATK +").append(it.atk).append("  ");
        if (it.def != 0) sb.append("DEF +").append(it.def).append("  ");
        if (it.hp != 0) sb.append("HP +").append(it.hp).append("  ");
        if (it.mp != 0) sb.append("MP +").append(it.mp).append("  ");
        if (it.spd != 0) sb.append("SPD +").append(it.spd).append("  ");
        if (it.crit != 0) sb.append("CRIT +").append(it.crit).append("%");
        return sb.toString().trim();
    }

    public static int upgraded(int base, int up) {
        if (up <= 0) return base;
        return base + Math.round(base * (up * 0.08f));
    }

    public static int expNeed(int level) {
        return 70 + level * 45 + level * level * 3;
    }

    public static String stageName(int stageIndex) {
        int wi = stageIndex / 5;
        int si = stageIndex % 5;
        if (wi < 0) wi = 0;
        if (wi > 4) wi = 4;
        return WORLDS[wi].subtitle + " " + (si + 1);
    }

    public static int diffUnlocked(int worldsCleared) {
        return Math.min(4, worldsCleared);
    }
}
