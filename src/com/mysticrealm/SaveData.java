package com.mysticrealm;

import android.content.Context;
import android.content.SharedPreferences;

import com.mysticrealm.Catalog.Item;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SaveData {
    private static final String PREFS = "mystic_realm_save";
    private static final String KEY = "save_v1";

    public boolean hasCharacter;
    public String playerName = "Pemain";
    public int classIndex = 0;
    public int level = 1;
    public int exp = 0;
    public int gold = 120;
    public int shards = 2;
    public int points = 0;
    public int[] tracks = new int[6];
    public int[] skillLv = new int[3];
    public int[] equipped = new int[4];
    public LinkedHashMap<String, Integer> items = new LinkedHashMap<>();
    public LinkedHashMap<String, Integer> ups = new LinkedHashMap<>();

    public int maxStage = 0;
    public boolean[] cleared = new boolean[25];
    public int currentStage = 0;
    public int difficulty = 1;
    public int seenWorld = 0;

    public boolean music = true;
    public boolean sfx = true;
    public float musicVolume = 0.8f;
    public float sfxVolume = 0.9f;
    public float sensitivity = 1f;
    public float buttonScale = 1f;
    public boolean highQuality = true;

    private final SharedPreferences prefs;

    public SaveData(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int i = 0; i < equipped.length; i++) equipped[i] = -1;
        load();
    }

    public Item item(String id) {
        return Catalog.item(id);
    }

    public boolean hasItem(String id) {
        return items.containsKey(id) && items.get(id) > 0;
    }

    public int count(String id) {
        Integer c = items.get(id);
        return c == null ? 0 : c;
    }

    public void addItem(String id, int count) {
        if (id == null || count <= 0) return;
        items.put(id, count(id) + count);
    }

    public void removeItem(String id, int count) {
        int c = count(id) - count;
        if (c <= 0) items.remove(id);
        else items.put(id, c);
    }

    public int upgradeOf(String id) {
        Integer u = ups.get(id);
        return u == null ? 0 : u;
    }

    public Item equippedItem(int slot) {
        if (slot < 0 || slot >= equipped.length) return null;
        int idx = equipped[slot];
        if (idx < 0) return null;
        List<Item> list = Catalog.itemsOfSlot(slot);
        if (idx >= list.size()) return null;
        return list.get(idx);
    }

    public String equippedId(int slot) {
        Item it = equippedItem(slot);
        return it == null ? null : it.id;
    }

    public void equip(int slot, String id) {
        if (id == null) {
            equipped[slot] = -1;
            return;
        }
        List<Item> list = Catalog.itemsOfSlot(slot);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                equipped[slot] = i;
                return;
            }
        }
    }

    public boolean isEquipped(String id) {
        for (int i = 0; i < equipped.length; i++) {
            if (id.equals(equippedId(i))) return true;
        }
        return false;
    }

    public int[] computeStats() {
        Catalog.ClassDef c = Catalog.CLASSES[Gfx.clamp(classIndex, 0, 2)];
        int hp = c.hp + (level - 1) * 14 + tracks[0] * 8;
        int mp = c.mp + (level - 1) * 5 + tracks[3] * 6;
        int atk = c.atk + (level - 1) * 3 + tracks[1] * 3;
        int def = c.def + (level - 1) + tracks[2] * 2;
        int spd = c.spd + tracks[4];
        int crit = c.crit + tracks[4];
        int luck = tracks[5] * 2;
        for (int slot = 0; slot < 4; slot++) {
            Item it = equippedItem(slot);
            if (it == null) continue;
            int up = upgradeOf(it.id);
            hp += Catalog.upgraded(it.hp, up);
            mp += Catalog.upgraded(it.mp, up);
            atk += Catalog.upgraded(it.atk, up);
            def += Catalog.upgraded(it.def, up);
            spd += Catalog.upgraded(it.spd, up);
            crit += Catalog.upgraded(it.crit, up);
        }
        if (spd < 40) spd = 40;
        if (crit > 70) crit = 70;
        return new int[]{hp, mp, atk, def, spd, crit, luck};
    }

    public int upgradeCostGold(String id) {
        Item it = item(id);
        if (it == null) return 9999;
        int up = upgradeOf(id);
        if (up >= 10) return 0;
        return (40 + it.price / 8) * (up + 1);
    }

    public int upgradeCostShards(String id) {
        int up = upgradeOf(id);
        if (up >= 10) return 0;
        return 1 + up / 2;
    }

    public boolean upgradeItem(String id) {
        Item it = item(id);
        if (it == null || it.slot < 0) return false;
        int up = upgradeOf(id);
        if (up >= 10) return false;
        int gc = upgradeCostGold(id);
        int sc = upgradeCostShards(id);
        if (gold < gc || shards < sc) return false;
        gold -= gc;
        shards -= sc;
        ups.put(id, up + 1);
        return true;
    }

    public int expToNext() {
        return Catalog.expNeed(level);
    }

    public int addExp(int amount) {
        exp += Math.max(0, amount);
        int gained = 0;
        while (level < 99 && exp >= expToNext()) {
            exp -= expToNext();
            level++;
            points++;
            gained++;
        }
        return gained;
    }

    public void reset(int cls) {
        hasCharacter = true;
        classIndex = Gfx.clamp(cls, 0, 2);
        level = 1;
        exp = 0;
        gold = 120;
        shards = 2;
        points = 0;
        tracks = new int[6];
        skillLv = new int[3];
        equipped = new int[]{-1, -1, -1, -1};
        items = new LinkedHashMap<>();
        ups = new LinkedHashMap<>();
        maxStage = 0;
        cleared = new boolean[25];
        currentStage = 0;
        difficulty = 1;

        Catalog.ClassDef c = Catalog.CLASSES[classIndex];
        addItem(c.startWeapon, 1);
        addItem("cloth_robe", 1);
        addItem("worn_boots", 1);
        addItem("health_potion", 3);
        addItem("mana_potion", 2);
        addItem("shard", 2);
        equip(Catalog.SLOT_WEAPON, c.startWeapon);
        equip(Catalog.SLOT_ARMOR, "cloth_robe");
        equip(Catalog.SLOT_BOOTS, "worn_boots");
    }

    public int worldsCleared() {
        int n = 0;
        for (int w = 0; w < 5; w++) {
            if (cleared[w * 5 + 4]) n++;
        }
        return n;
    }

    public void markCleared(int stageIndex) {
        if (stageIndex < 0 || stageIndex >= 25) return;
        cleared[stageIndex] = true;
        if (stageIndex + 1 < 25) maxStage = Math.max(maxStage, stageIndex + 1);
        else maxStage = 24;
    }

    public int unlockedDifficulty() {
        return Catalog.diffUnlocked(worldsCleared());
    }

    public void load() {
        String raw = prefs.getString(KEY, null);
        if (raw == null) return;
        try {
            JSONObject o = new JSONObject(raw);
            hasCharacter = o.optBoolean("has", false);
            playerName = cleanPlayerName(o.optString("playerName", "Pemain"));
            classIndex = o.optInt("class", 0);
            level = o.optInt("level", 1);
            exp = o.optInt("exp", 0);
            gold = o.optInt("gold", 120);
            shards = o.optInt("shards", 2);
            points = o.optInt("points", 0);
            tracks = new int[6];
            JSONArray tr = o.optJSONArray("tracks");
            if (tr != null) for (int i = 0; i < Math.min(6, tr.length()); i++) tracks[i] = tr.optInt(i, 0);
            skillLv = new int[3];
            JSONArray sk = o.optJSONArray("skills");
            if (sk != null) for (int i = 0; i < Math.min(3, sk.length()); i++) skillLv[i] = sk.optInt(i, 0);
            equipped = new int[]{-1, -1, -1, -1};
            JSONArray eq = o.optJSONArray("equipped");
            if (eq != null) for (int i = 0; i < Math.min(4, eq.length()); i++) equipped[i] = eq.optInt(i, -1);
            items = new LinkedHashMap<>();
            JSONObject inv = o.optJSONObject("items");
            if (inv != null) {
                JSONArray names = inv.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String k = names.optString(i, "");
                        if (k.length() > 0) items.put(k, inv.optInt(k, 0));
                    }
                }
            }
            ups = new LinkedHashMap<>();
            JSONObject up = o.optJSONObject("ups");
            if (up != null) {
                JSONArray names = up.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String k = names.optString(i, "");
                        if (k.length() > 0) ups.put(k, up.optInt(k, 0));
                    }
                }
            }
            maxStage = o.optInt("maxStage", 0);
            cleared = new boolean[25];
            JSONArray cl = o.optJSONArray("cleared");
            if (cl != null) for (int i = 0; i < Math.min(25, cl.length()); i++) cleared[i] = cl.optBoolean(i, false);
            currentStage = o.optInt("currentStage", 0);
            difficulty = o.optInt("difficulty", 1);
            seenWorld = o.optInt("seenWorld", 0);
            music = o.optBoolean("music", true);
            sfx = o.optBoolean("sfx", true);
            musicVolume = Gfx.clamp((float) o.optDouble("musicVolume", music ? 0.8 : 0.0), 0f, 1f);
            sfxVolume = Gfx.clamp((float) o.optDouble("sfxVolume", sfx ? 0.9 : 0.0), 0f, 1f);
            sensitivity = Gfx.clamp((float) o.optDouble("sensitivity", 1.0), 0.4f, 2f);
            buttonScale = Gfx.clamp((float) o.optDouble("buttonScale", 1.0), 0.7f, 1.35f);
            highQuality = o.optBoolean("quality", true);
            if (hasCharacter) {
                int[] st = computeStats();
                if (st[0] <= 0) level = 1;
            }
        } catch (Exception e) {
            hasCharacter = false;
        }
    }

    public void save() {
        try {
            JSONObject o = new JSONObject();
            o.put("has", hasCharacter);
            o.put("playerName", cleanPlayerName(playerName));
            o.put("class", classIndex);
            o.put("level", level);
            o.put("exp", exp);
            o.put("gold", gold);
            o.put("shards", shards);
            o.put("points", points);
            JSONArray tr = new JSONArray();
            for (int i = 0; i < 6; i++) tr.put(tracks[i]);
            o.put("tracks", tr);
            JSONArray sk = new JSONArray();
            for (int i = 0; i < 3; i++) sk.put(skillLv[i]);
            o.put("skills", sk);
            JSONArray eq = new JSONArray();
            for (int i = 0; i < 4; i++) eq.put(equipped[i]);
            o.put("equipped", eq);
            JSONObject inv = new JSONObject();
            for (LinkedHashMap.Entry<String, Integer> e : items.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) inv.put(e.getKey(), e.getValue());
            }
            o.put("items", inv);
            JSONObject up = new JSONObject();
            for (LinkedHashMap.Entry<String, Integer> e : ups.entrySet()) {
                up.put(e.getKey(), e.getValue());
            }
            o.put("ups", up);
            o.put("maxStage", maxStage);
            JSONArray cl = new JSONArray();
            for (int i = 0; i < 25; i++) cl.put(cleared[i]);
            o.put("cleared", cl);
            o.put("currentStage", currentStage);
            o.put("difficulty", difficulty);
            o.put("seenWorld", seenWorld);
            o.put("music", music);
            o.put("sfx", sfx);
            o.put("musicVolume", musicVolume);
            o.put("sfxVolume", sfxVolume);
            o.put("sensitivity", sensitivity);
            o.put("buttonScale", buttonScale);
            o.put("quality", highQuality);
            prefs.edit().putString(KEY, o.toString()).apply();
        } catch (Exception e) {
            // ignore save failure
        }
    }

    public void setPlayerName(String name) {
        playerName = cleanPlayerName(name);
        save();
    }

    private static String cleanPlayerName(String name) {
        if (name == null) return "Pemain";
        name = name.replace('\n', ' ').replace('\r', ' ').trim();
        if (name.length() == 0) return "Pemain";
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    public void erase() {
        prefs.edit().remove(KEY).apply();
        hasCharacter = false;
    }

    public List<String> consumables() {
        List<String> out = new ArrayList<>();
        for (LinkedHashMap.Entry<String, Integer> e : items.entrySet()) {
            Item it = item(e.getKey());
            if (it != null && it.use != null && e.getValue() != null && e.getValue() > 0) {
                out.add(e.getKey());
            }
        }
        return out;
    }
}
