package com.mysticrealm;

public final class Achievements {
    public static final int CLEARED = 0;
    public static final int WORLDS = 1;
    public static final int GOLD = 2;
    public static final int LEVEL = 3;
    public static final int ITEMS = 4;
    public static final int EQUIPPED = 5;
    public static final int SKILL = 6;
    public static final int TRACKS = 7;
    public static final int FRIENDS = 8;

    public static class Def {
        public String name;
        public String desc;
        public int kind;
        public int target;
    }

    public static final Def[] LIST = {
            def("Langkah Pertama", "Selesaikan stage pertamamu.", CLEARED, 1),
            def("Penjelajah", "Selesaikan 5 stage.", CLEARED, 5),
            def("Penakluk", "Selesaikan 15 stage.", CLEARED, 15),
            def("Legenda", "Selesaikan seluruh 25 stage.", CLEARED, 25),
            def("Kolektor", "Miliki 5 jenis item.", ITEMS, 5),
            def("Kaya Raya", "Kumpulkan 1.000 gold.", GOLD, 1000),
            def("Veteran", "Capai level 10.", LEVEL, 10),
            def("Ahli Skill", "Maksimalkan satu skill.", SKILL, 1),
            def("Bintang Talenta", "Total 20 level talenta.", TRACKS, 20),
            def("Penyelamat Dunia", "Tuntaskan semua dunia.", WORLDS, 5),
            def("Teman Sejati", "Punya 3 teman online.", FRIENDS, 3)
    };

    private static Def def(String name, String desc, int kind, int target) {
        Def d = new Def();
        d.name = name;
        d.desc = desc;
        d.kind = kind;
        d.target = target;
        return d;
    }

    public static int clearedCount(SaveData s) {
        int n = 0;
        for (int i = 0; i < s.cleared.length; i++) if (s.cleared[i]) n++;
        return n;
    }

    public static int progress(SaveData s, int index, int friendCount) {
        if (index < 0 || index >= LIST.length) return 0;
        Def d = LIST[index];
        switch (d.kind) {
            case CLEARED:
                return clearedCount(s);
            case WORLDS:
                return s.worldsCleared();
            case GOLD:
                return Math.max(0, s.gold);
            case LEVEL:
                return s.level;
            case ITEMS:
                return s.items.size();
            case EQUIPPED: {
                int n = 0;
                for (int i = 0; i < 4; i++) if (s.equippedId(i) != null) n++;
                return n;
            }
            case SKILL: {
                int best = 0;
                for (int i = 0; i < 3; i++) best = Math.max(best, s.skillLv[i]);
                return best >= Catalog.MAX_SKILL_LEVEL ? 1 : 0;
            }
            case TRACKS: {
                int n = 0;
                for (int i = 0; i < 6; i++) n += s.tracks[i];
                return n;
            }
            case FRIENDS:
                return friendCount;
            default:
                return 0;
        }
    }

    public static boolean unlocked(SaveData s, int index, int friendCount) {
        if (index < 0 || index >= LIST.length) return false;
        return progress(s, index, friendCount) >= LIST[index].target;
    }

    public static int unlockedCount(SaveData s, int friendCount) {
        int n = 0;
        for (int i = 0; i < LIST.length; i++) if (unlocked(s, i, friendCount)) n++;
        return n;
    }
}
