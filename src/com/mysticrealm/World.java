package com.mysticrealm;

import android.graphics.Canvas;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class World {
    public static final float TILE = 96f;

    public static class Obstacle {
        public float x, y, r;
        public int type;
        public float seed;
        public boolean solid = true;
    }

    public int stageIndex;
    public int theme;
    public int stageNum;
    public float w, h;
    public float spawnX, spawnY;
    public float chestX, chestY;
    public float shrineX, shrineY;
    public boolean chestOpen;
    public int seed;
    public final ArrayList<Obstacle> obstacles = new ArrayList<>();
    private final ArrayList<Obstacle> drawOrder = new ArrayList<>();

    public World(int stageIndex) {
        this.stageIndex = stageIndex;
        this.theme = Math.max(0, Math.min(Catalog.WORLD_COUNT - 1, stageIndex / Catalog.STAGES_PER_WORLD));
        this.stageNum = Math.max(0, Math.min(Catalog.STAGES_PER_WORLD - 1, stageIndex % Catalog.STAGES_PER_WORLD));
        this.seed = 7919 + stageIndex * 104729;
        w = 2400 + theme * 260 + stageNum * 40;
        h = 1600 + theme * 150 + stageNum * 30;
        spawnX = w * 0.5f;
        spawnY = h - 240;
        shrineX = w * 0.5f;
        shrineY = 280;
        chestX = w * 0.16f;
        chestY = h * 0.55f;
        generate();
        drawOrder.addAll(obstacles);
        Collections.sort(drawOrder, new Comparator<Obstacle>() {
            public int compare(Obstacle a, Obstacle b) {
                return Float.compare(a.y, b.y);
            }
        });
    }

    private int hash(int a, int b) {
        int hsh = seed;
        hsh = hsh * 374761393 + a * 668265263 + b * 1442695041;
        hsh = (hsh ^ (hsh >>> 13)) * 1274126177;
        return hsh ^ (hsh >>> 16);
    }

    private void generate() {
        Random r = new Random(seed);
        int margin = 70;
        int stepX = 150;
        for (float x = margin; x <= w - margin; x += stepX) {
            addObstacle(x, margin, 62, 99, r);
            addObstacle(x, h - margin, 62, 99, r);
        }
        int stepY = 150;
        for (float y = margin + stepY; y < h - margin; y += stepY) {
            addObstacle(margin, y, 62, 99, r);
            addObstacle(w - margin, y, 62, 99, r);
        }
        int count = 18 + stageNum * 3 + theme * 7;
        for (int i = 0; i < count; i++) {
            float ox = 180 + r.nextFloat() * (w - 360);
            float oy = 240 + r.nextFloat() * (h - 480);
            if (near(ox, oy, spawnX, spawnY, 260)) continue;
            if (near(ox, oy, shrineX, shrineY, 220)) continue;
            if (near(ox, oy, chestX, chestY, 180)) continue;
            if (near(ox, oy, w * 0.5f, h * 0.45f, 120)) continue;
            int type = obstacleType(r);
            float rad = typeRadius(type);
            boolean clear = true;
            for (Obstacle o : obstacles) {
                if (near(ox, oy, o.x, o.y, rad + o.r + 46)) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            addObstacle(ox, oy, rad, type, r);
        }
        int buildings = 4 + theme / 2;
        for (int i = 0; i < buildings; i++) {
            float ox = 260 + r.nextFloat() * (w - 520);
            float oy = 320 + r.nextFloat() * (h - 640);
            if (near(ox, oy, spawnX, spawnY, 340)) continue;
            if (near(ox, oy, shrineX, shrineY, 280)) continue;
            if (near(ox, oy, chestX, chestY, 220)) continue;
            if (near(ox, oy, w * 0.5f, h * 0.45f, 220)) continue;
            int type = 13 + r.nextInt(8);
            float rad = typeRadius(type);
            if (!clearOfBuildings(ox, oy, rad)) continue;
            if (!clearOfSolids(ox, oy, rad, 40)) continue;
            addObstacle(ox, oy, rad, type, r, true);
        }
        int decos = 36 + theme * 3;
        for (int i = 0; i < decos; i++) {
            float ox = 120 + r.nextFloat() * (w - 240);
            float oy = 170 + r.nextFloat() * (h - 340);
            if (near(ox, oy, spawnX, spawnY, 150)) continue;
            if (near(ox, oy, shrineX, shrineY, 130)) continue;
            if (near(ox, oy, chestX, chestY, 110)) continue;
            int type = 21 + r.nextInt(14);
            float rad = typeRadius(type);
            if (!clearOfSolids(ox, oy, rad, 6)) continue;
            addObstacle(ox, oy, rad, type, r, false);
        }
    }

    private boolean near(float x1, float y1, float x2, float y2, float d) {
        float dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy < d * d;
    }

    private void addObstacle(float x, float y, float r, int type, Random rnd) {
        addObstacle(x, y, r, type, rnd, true);
    }

    private void addObstacle(float x, float y, float r, int type, Random rnd, boolean solid) {
        Obstacle o = new Obstacle();
        o.x = x;
        o.y = y;
        o.r = r;
        o.type = type;
        o.seed = rnd.nextFloat() * 100f;
        o.solid = solid;
        obstacles.add(o);
    }

    private boolean clearOfBuildings(float x, float y, float rad) {
        for (Obstacle o : obstacles) {
            if (o.type < 13 || o.type > 20) continue;
            if (near(x, y, o.x, o.y, rad + o.r + 120)) return false;
        }
        return true;
    }

    private boolean clearOfSolids(float x, float y, float rad, float gap) {
        for (Obstacle o : obstacles) {
            if (!o.solid) continue;
            if (near(x, y, o.x, o.y, rad + o.r + gap)) return false;
        }
        return true;
    }

    private int obstacleType(Random r) {
        int n = r.nextInt(100);
        switch (theme) {
            case 0:
                if (n < 52) return 0;
                if (n < 72) return 1;
                return 2;
            case 1:
                if (n < 45) return 3;
                if (n < 70) return 4;
                return 2;
            case 2:
                if (n < 40) return 5;
                if (n < 66) return 6;
                return 7;
            case 3:
                if (n < 40) return 8;
                if (n < 66) return 6;
                return 9;
            case 4:
                if (n < 45) return 10;
                if (n < 66) return 11;
                return 12;
            case 5:
                if (n < 38) return 1;
                if (n < 62) return 2;
                return 12;
            case 6:
                if (n < 40) return 3;
                if (n < 62) return 4;
                if (n < 80) return 10;
                return 12;
            case 7:
                if (n < 45) return 6;
                if (n < 70) return 10;
                return 12;
            case 8:
                if (n < 40) return 1;
                if (n < 68) return 3;
                return 12;
            default:
                if (n < 40) return 5;
                if (n < 66) return 2;
                return 12;
        }
    }

    private float typeRadius(int type) {
        switch (type) {
            case 1: return 26;
            case 2: return 40;
            case 3: return 30;
            case 5: return 42;
            case 6: return 34;
            case 7: return 30;
            case 8: return 34;
            case 9: return 32;
            case 10: return 34;
            case 11: return 38;
            case 12: return 30;
            case 0: return 40;
            case 4: return 38;
            case 13: return 68;
            case 14: return 46;
            case 15: return 52;
            case 16: return 50;
            case 17: return 60;
            case 18: return 40;
            case 19: return 52;
            case 20: return 34;
            case 21: return 22;
            case 22: return 20;
            case 23: return 22;
            case 24: return 20;
            case 25: return 18;
            case 26: return 24;
            case 27: return 24;
            case 28: return 20;
            case 29: return 20;
            case 30: return 26;
            case 31: return 24;
            case 32: return 26;
            case 33: return 26;
            case 34: return 22;
            case 99: return 62;
            default: return 34;
        }
    }

    public void drawGround(Canvas c, Gfx g, float camX, float camY, float viewW, float viewH, float time, boolean quality) {
        Catalog.WorldDef wd = Catalog.WORLDS[theme];
        int x0 = (int) Math.floor((camX - viewW / 2) / TILE) - 1;
        int x1 = (int) Math.ceil((camX + viewW / 2) / TILE) + 1;
        int y0 = (int) Math.floor((camY - viewH / 2) / TILE) - 1;
        int y1 = (int) Math.ceil((camY + viewH / 2) / TILE) + 1;
        g.fill(Gfx.BG_DEEP);
        c.drawRect(camX - viewW / 2 - 30, camY - viewH / 2 - 30, camX + viewW / 2 + 30, camY + viewH / 2 + 30, g.p);

        for (int tx = x0; tx <= x1; tx++) {
            for (int ty = y0; ty <= y1; ty++) {
                float cx = tx * TILE;
                float cy = ty * TILE;
                if (cx + TILE < 0 || cy + TILE < 0 || cx > w || cy > h) continue;
                int hs = hash(tx, ty);
                float variation = 0.12f + ((hs >>> 8) & 255) / 255f * 0.34f;
                int col = Gfx.mix(wd.groundA, wd.groundB, variation);
                g.fill(col);
                c.drawRect(cx, cy, cx + TILE + 1, cy + TILE + 1, g.p);
                g.line(c, cx + 2, cy + TILE - 2, cx + TILE - 2, cy + TILE - 2,
                        Gfx.withAlpha(wd.deco, 72), 1.4f);
                g.line(c, cx + TILE - 2, cy + 2, cx + TILE - 2, cy + TILE - 2,
                        Gfx.withAlpha(0xFFFFFFFF, 12), 1.2f);

                float tcx = cx + TILE / 2, tcy = cy + TILE / 2;
                if (pointSegDist(tcx, tcy, spawnX, spawnY, shrineX, shrineY) < 82f
                        || pointSegDist(tcx, tcy, spawnX, spawnY, chestX, chestY) < 60f) {
                    g.fill(Gfx.withAlpha(wd.path, 132));
                    c.drawRect(cx + 4, cy + 4, cx + TILE - 4, cy + TILE - 4, g.p);
                    if ((hs & 3) == 0) {
                        g.line(c, cx + 18, cy + 20, cx + 42, cy + 16,
                                Gfx.withAlpha(wd.accent, 38), 2f);
                    }
                }

                if (quality) {
                    float dx = cx + 14 + (hs & 63);
                    float dy = cy + 14 + ((hs >>> 6) & 63);
                    if ((hs & 7) == 0) {
                        g.fill(Gfx.withAlpha(Gfx.mix(col, wd.accent, 0.35f), 30));
                        c.drawOval(dx - 12, dy - 7, dx + 18, dy + 10, g.p);
                        switch (theme) {
                            case 0:
                                g.line(c, dx, dy + 4, dx - 3, dy - 5, Gfx.withAlpha(wd.accent, 100), 1.5f);
                                g.line(c, dx + 4, dy + 4, dx + 7, dy - 4, Gfx.withAlpha(wd.accent, 82), 1.5f);
                                break;
                            case 1:
                                g.line(c, dx - 6, dy + 2, dx + 2, dy - 6, Gfx.withAlpha(0xFFBFE8FF, 110), 1.6f);
                                g.line(c, dx + 2, dy - 6, dx + 8, dy - 1, Gfx.withAlpha(0xFFBFE8FF, 90), 1.3f);
                                g.line(c, dx + 2, dy - 6, dx + 1, dy + 4, Gfx.withAlpha(0xFFBFE8FF, 70), 1.2f);
                                break;
                            case 2:
                                g.line(c, dx - 6, dy + 3, dx - 1, dy - 2, Gfx.withAlpha(0xFFFF7A45, 120), 1.6f);
                                g.line(c, dx - 1, dy - 2, dx + 5, dy - 6, Gfx.withAlpha(0xFFFF9A3D, 100), 1.4f);
                                g.circle(c, dx - 1, dy - 2, 1.4f, Gfx.withAlpha(0xFFFFE08A, 150));
                                break;
                            case 3:
                                g.line(c, dx - 6, dy, dx, dy - 4, Gfx.withAlpha(wd.accent, 90), 1.6f);
                                g.line(c, dx, dy - 4, dx + 6, dy + 1, Gfx.withAlpha(wd.accent, 70), 1.4f);
                                g.circle(c, dx + 6, dy + 1, 1.2f, Gfx.withAlpha(wd.accent, 120));
                                break;
                            case 4:
                                g.line(c, dx - 5, dy - 4, dx + 1, dy + 1, Gfx.withAlpha(wd.accent, 90), 1.5f);
                                g.line(c, dx + 1, dy + 1, dx + 8, dy - 3, Gfx.withAlpha(wd.accent, 75), 1.3f);
                                break;
                            case 5:
                                g.line(c, dx - 8, dy + 4, dx - 1, dy + 1, Gfx.withAlpha(0xFFD8F0E8, 80), 1.5f);
                                g.line(c, dx - 1, dy + 1, dx + 8, dy + 5, Gfx.withAlpha(0xFFD8F0E8, 65), 1.4f);
                                g.line(c, dx + 2, dy - 5, dx + 6, dy - 2, Gfx.withAlpha(0xFFFFF0C8, 100), 1.3f);
                                break;
                            case 6:
                                g.line(c, dx + 2, dy - 7, dx + 6, dy + 1, Gfx.withAlpha(0xFFD9C8FF, 120), 1.5f);
                                g.line(c, dx + 6, dy + 1, dx - 1, dy + 3, Gfx.withAlpha(0xFFD9C8FF, 95), 1.4f);
                                g.circle(c, dx + 2, dy - 7, 1.3f, Gfx.withAlpha(0xFFFFFFFF, 150));
                                break;
                            case 7:
                                g.line(c, dx - 7, dy + 1, dx + 1, dy - 2, Gfx.withAlpha(0xFFFFFFFF, 55), 2f);
                                g.line(c, dx + 1, dy - 2, dx + 8, dy + 2, Gfx.withAlpha(0xFFFFFFFF, 40), 1.6f);
                                break;
                            case 8:
                                g.fill(Gfx.withAlpha(0xFF2A3A44, 110));
                                c.drawOval(dx - 9, dy - 4, dx + 7, dy + 5, g.p);
                                g.circle(c, dx - 4, dy - 1, 1.6f, Gfx.withAlpha(0xFF9BD44A, 110));
                                g.circle(c, dx + 2, dy + 1, 1.4f, Gfx.withAlpha(0xFF9BD44A, 90));
                                break;
                            default:
                                g.line(c, dx - 6, dy + 3, dx - 1, dy - 2, Gfx.withAlpha(0xFFFF6B3D, 130), 1.7f);
                                g.line(c, dx - 1, dy - 2, dx + 6, dy + 2, Gfx.withAlpha(0xFFFF8A3D, 105), 1.5f);
                                g.circle(c, dx + 6, dy + 2, 1.5f, Gfx.withAlpha(0xFFFFE08A, 160));
                                break;
                        }
                    } else if ((hs & 15) == 3) {
                        g.line(c, dx, dy, dx + 6, dy - 10, Gfx.withAlpha(wd.deco, 150), 2f);
                        g.line(c, dx + 6, dy, dx + 10, dy - 8, Gfx.withAlpha(wd.deco, 125), 1.6f);
                    }
                }
            }
        }
        if (quality) {
            g.gradientRect(c, camX - viewW / 2, camY - viewH / 2, viewW, viewH,
                    0x0CFFFFFF, 0x1C000000);
            for (int i = 0; i < 5; i++) {
                int hs = hash(i, 91);
                float span = w + 420f;
                float cx = ((hs & 0xFFFF) / 65535f * span + time * (7f + i * 1.6f)) % span - 210f;
                float cy = ((hs >> 16) & 0xFFFF) / 65535f * h;
                float cw = 150f + (hs & 127) * 1.4f;
                g.fill(0x0D000000);
                c.drawOval(cx - cw, cy - cw * 0.42f, cx + cw, cy + cw * 0.42f, g.p);
                g.fill(0x0A000000);
                c.drawOval(cx - cw * 0.65f, cy - cw * 0.28f, cx + cw * 0.8f, cy + cw * 0.3f, g.p);
            }
        }
        if (quality) {
            for (int i = 0; i < 28; i++) {
                int hs = hash(i, 77 + theme);
                float baseX = (hs & 0xFFFF) / 65535f * w;
                float baseY = ((hs >> 16) & 0xFFFF) / 65535f * h;
                float ph = (hs & 255) / 255f * 6.28f;
                float sx, sy, sz = 1.6f;
                int pcol;
                switch (theme) {
                    case 1:
                        sx = (baseX + time * 6f) % w;
                        sy = (baseY + time * 18f * (0.5f + (hs & 63) / 63f)) % h;
                        pcol = Gfx.withAlpha(0xFFFFFFFF, 150);
                        sz = 1.9f;
                        break;
                    case 2:
                    case 9:
                        sx = baseX + (float) Math.sin(time * 0.8f + ph) * 9f;
                        sy = h - ((baseY + time * 14f * (0.4f + (hs & 63) / 63f)) % h);
                        pcol = Gfx.withAlpha(theme == 2 ? 0xFFFF9A3D : 0xFFFFC46B, 170);
                        sz = 1.7f + (hs & 7) * 0.12f;
                        break;
                    case 3:
                        sx = baseX + (float) Math.sin(time * 0.5f + ph) * 14f;
                        sy = baseY + (float) Math.cos(time * 0.42f + ph) * 10f;
                        pcol = Gfx.withAlpha(wd.accent, 110);
                        break;
                    case 5:
                        sx = (baseX + time * 10f + (float) Math.sin(time + ph) * 6f) % w;
                        sy = baseY + (float) Math.sin(time * 1.4f + ph) * 12f;
                        pcol = Gfx.withAlpha(0xFFBFF4FF, 110);
                        sz = 1.4f;
                        break;
                    case 6:
                        sx = baseX + (float) Math.sin(time * 0.7f + ph) * 8f;
                        sy = baseY + (float) Math.cos(time * 0.6f + ph) * 12f;
                        pcol = Gfx.withAlpha(wd.accent, (int) (90 + 80 * Math.abs(Math.sin(time + ph))));
                        sz = 1.9f;
                        break;
                    case 7:
                        sx = (baseX + time * 34f) % (w + 200f) - 100f;
                        sy = baseY + 4f;
                        pcol = Gfx.withAlpha(0xFFFFFFFF, 34);
                        sz = 2.4f;
                        break;
                    case 8:
                        sx = baseX + (float) Math.sin(time * 0.9f + ph) * 10f;
                        sy = baseY + (float) Math.cos(time * 0.7f + ph) * 8f;
                        int bl = (int) (120 * Math.max(0f, Math.sin(time * 2f + ph)));
                        pcol = Gfx.withAlpha(0xFFC6FF5E, bl);
                        sz = 1.7f;
                        break;
                    default:
                        sx = baseX + (float) Math.sin(time * 0.6f + ph) * 10f;
                        sy = (baseY + time * 8f) % h;
                        pcol = theme == 0 ? Gfx.withAlpha(0xFFB8E88A, 90) : Gfx.withAlpha(0xFFFFE08A, (int) (120 * (0.4f + 0.6f * Math.abs(Math.sin(time * 1.5f + i)))));
                        break;
                }
                g.circle(c, sx, sy, sz, pcol);
            }
        }
        g.roundRectStroke(c, 0, 0, w, h, 4, Gfx.withAlpha(wd.accent, 60), 6);
    }

    public void drawObstacles(Canvas c, Gfx g, float time, boolean quality) {
        Catalog.WorldDef wd = Catalog.WORLDS[theme];
        for (Obstacle o : drawOrder) {
            drawObstacle(c, g, o, wd, time, quality);
        }
    }

    private void drawObstacle(Canvas c, Gfx g, Obstacle o, Catalog.WorldDef wd, float time, boolean quality) {
        float x = o.x, y = o.y, r = o.r;
        int rock = Gfx.mix(wd.deco, 0xFF444444, 0.4f);
        int accent = wd.accent;
        if (o.type < 21 || o.type == 99) {
            g.fill(0x33000000);
            c.drawOval(x - r * 1.05f, y + r * 0.25f, x + r * 1.05f, y + r * 0.75f, g.p);
        }

        switch (o.type) {
            case 0: {
                Path trunk = new Path();
                trunk.moveTo(x - r * 0.22f, y - r * 0.15f);
                trunk.lineTo(x + r * 0.2f, y - r * 0.15f);
                trunk.lineTo(x + r * 0.28f, y + r * 0.72f);
                trunk.lineTo(x - r * 0.3f, y + r * 0.72f);
                trunk.close();
                g.fill(0xFF4A3423);
                c.drawPath(trunk, g.p);
                g.line(c, x - r * 0.08f, y + r * 0.15f, x - r * 0.12f, y + r * 0.58f,
                        0x557A5A36, 2f);
                int leaf = Gfx.mix(wd.accent, 0xFF1C4028, 0.55f);
                Path crown = new Path();
                crown.moveTo(x, y - r * 1.55f);
                crown.lineTo(x + r * 0.42f, y - r * 1.02f);
                crown.lineTo(x + r * 0.88f, y - r * 0.76f);
                crown.lineTo(x + r * 0.68f, y - r * 0.36f);
                crown.lineTo(x + r * 0.48f, y - r * 0.12f);
                crown.lineTo(x - r * 0.52f, y - r * 0.12f);
                crown.lineTo(x - r * 0.86f, y - r * 0.58f);
                crown.lineTo(x - r * 0.56f, y - r * 0.98f);
                crown.close();
                g.fill(leaf);
                c.drawPath(crown, g.p);
                Path shade = new Path();
                shade.moveTo(x + r * 0.08f, y - r * 1.38f);
                shade.lineTo(x + r * 0.42f, y - r * 0.92f);
                shade.lineTo(x + r * 0.72f, y - r * 0.64f);
                shade.lineTo(x + r * 0.42f, y - r * 0.18f);
                shade.lineTo(x - r * 0.06f, y - r * 0.28f);
                shade.close();
                g.fill(Gfx.mix(leaf, wd.deco, 0.22f));
                c.drawPath(shade, g.p);
                g.line(c, x - r * 0.36f, y - r * 0.72f, x - r * 0.08f, y - r * 1.12f,
                        Gfx.withAlpha(0xFFFFFFFF, 45), 2.2f);
                break;
            }
            case 1: {
                g.circle(c, x - r * 0.35f, y, r * 0.62f, Gfx.mix(wd.accent, wd.deco, 0.55f));
                g.circle(c, x + r * 0.35f, y, r * 0.6f, Gfx.mix(wd.accent, wd.deco, 0.45f));
                g.circle(c, x, y - r * 0.3f, r * 0.54f, Gfx.mix(wd.accent, wd.deco, 0.35f));
                break;
            }
            case 2: {
                Path stone = new Path();
                stone.moveTo(x - r * 0.92f, y + r * 0.34f);
                stone.lineTo(x - r * 0.72f, y - r * 0.36f);
                stone.lineTo(x - r * 0.18f, y - r * 0.88f);
                stone.lineTo(x + r * 0.48f, y - r * 0.7f);
                stone.lineTo(x + r * 0.92f, y - r * 0.05f);
                stone.lineTo(x + r * 0.63f, y + r * 0.62f);
                stone.lineTo(x - r * 0.28f, y + r * 0.82f);
                stone.close();
                g.fill(rock);
                c.drawPath(stone, g.p);
                Path facet = new Path();
                facet.moveTo(x - r * 0.66f, y - r * 0.3f);
                facet.lineTo(x - r * 0.18f, y - r * 0.78f);
                facet.lineTo(x + r * 0.4f, y - r * 0.58f);
                facet.lineTo(x + r * 0.08f, y - r * 0.05f);
                facet.close();
                g.fill(Gfx.mix(rock, 0xFFFFFFFF, 0.13f));
                c.drawPath(facet, g.p);
                g.line(c, x - r * 0.56f, y + r * 0.33f, x + r * 0.45f, y + r * 0.3f,
                        Gfx.withAlpha(0xFF000000, 55), 2f);
                break;
            }
            case 3: {
                g.circle(c, x, y - r * 0.2f, r * 0.9f, Gfx.withAlpha(accent, 40));
                int col = Gfx.mix(accent, 0xFF224466, 0.35f);
                Path p = new Path();
                p.moveTo(x, y - r * 1.2f);
                p.lineTo(x + r * 0.34f, y - r * 0.1f);
                p.lineTo(x + r * 0.12f, y + r * 0.55f);
                p.lineTo(x - r * 0.2f, y + r * 0.35f);
                p.lineTo(x - r * 0.32f, y - r * 0.2f);
                p.close();
                g.fill(col);
                c.drawPath(p, g.p);
                g.line(c, x - r * 0.1f, y - r * 0.75f, x + r * 0.08f, y + r * 0.2f, Gfx.withAlpha(0xFFFFFFFF, 120), 2.4f);
                break;
            }
            case 4: {
                g.roundRect(c, x - r * 0.14f, y - r * 0.3f, r * 0.28f, r * 1.1f, 4, 0xFF4A4F63);
                g.line(c, x, y - r * 0.3f, x - r * 0.5f, y - r * 0.85f, 0xFF4A4F63, 5);
                g.line(c, x, y - r * 0.4f, x + r * 0.5f, y - r * 0.9f, 0xFF4A4F63, 5);
                g.circle(c, x - r * 0.5f, y - r * 0.9f, r * 0.18f, Gfx.withAlpha(accent, 170));
                g.circle(c, x + r * 0.5f, y - r * 0.95f, r * 0.16f, Gfx.withAlpha(accent, 150));
                break;
            }
            case 5: {
                g.circle(c, x, y, r * 0.95f, 0xFF3A201C);
                g.circle(c, x - r * 0.2f, y - r * 0.25f, r * 0.5f, 0xFF4A2A22);
                g.line(c, x - r * 0.5f, y + r * 0.2f, x + r * 0.1f, y - r * 0.4f, 0xFFFF7A45, 3);
                g.line(c, x + r * 0.1f, y - r * 0.4f, x + r * 0.45f, y + r * 0.3f, 0xFFFF7A45, 2.4f);
                g.circle(c, x + r * 0.1f, y - r * 0.4f, 4, 0xFFFFC46B);
                break;
            }
            case 6: {
                g.line(c, x, y + r * 0.5f, x, y - r * 0.9f, 0xFF3B2D26, 7);
                g.line(c, x, y - r * 0.3f, x - r * 0.55f, y - r * 0.75f, 0xFF3B2D26, 4);
                g.line(c, x, y - r * 0.55f, x + r * 0.5f, y - r * 0.95f, 0xFF3B2D26, 4);
                g.line(c, x - r * 0.55f, y - r * 0.75f, x - r * 0.68f, y - r * 0.5f, 0xFF3B2D26, 3);
                break;
            }
            case 7: {
                g.circle(c, x, y - r * 0.3f, r * 0.85f, Gfx.withAlpha(0xFFFF7A45, 35));
                g.circle(c, x, y - r * 0.15f, r * 0.55f, 0xFFFF7A45);
                g.circle(c, x - r * 0.15f, y - r * 0.3f, r * 0.22f, 0xFFFFD08A);
                break;
            }
            case 8: {
                g.roundRect(c, x - r * 0.55f, y - r * 1.4f, r * 1.1f, r * 1.75f, 6, 0xFF2A2140);
                g.roundRect(c, x - r * 0.7f, y - r * 1.55f, r * 1.4f, r * 0.3f, 4, 0xFF382B55);
                g.circle(c, x, y - r * 0.75f, r * 0.2f, Gfx.withAlpha(accent, 180));
                g.line(c, x, y - r * 0.4f, x, y + r * 0.1f, Gfx.withAlpha(accent, 90), 2);
                break;
            }
            case 9: {
                g.circle(c, x, y, r * 0.9f, 0xFF160F24);
                g.circleStroke(c, x, y, r * 0.75f, Gfx.withAlpha(0xFFB07CFF, 120), 3);
                g.circle(c, x - r * 0.15f, y - r * 0.2f, r * 0.22f, Gfx.withAlpha(0xFFB07CFF, 50));
                break;
            }
            case 10: {
                g.roundRect(c, x - r * 0.32f, y - r * 1.5f, r * 0.64f, r * 1.8f, 3, 0xFF8D89B8);
                g.roundRect(c, x - r * 0.5f, y - r * 1.62f, r, r * 0.22f, 3, 0xFFA9A5D0);
                g.roundRect(c, x - r * 0.5f, y + r * 0.2f, r, r * 0.18f, 3, 0xFFA9A5D0);
                g.line(c, x - r * 0.18f, y - r * 1.2f, x + r * 0.18f, y - r * 0.3f, Gfx.withAlpha(accent, 90), 2);
                break;
            }
            case 11: {
                g.roundRect(c, x - r * 0.62f, y + r * 0.3f, r * 1.24f, r * 0.42f, 4, 0xFF6F6A94);
                g.circle(c, x, y - r * 0.35f, r * 0.42f, 0xFFB3AFD8);
                g.circle(c, x, y - r * 0.62f, r * 0.26f, 0xFFC9C5E6);
                g.line(c, x - r * 0.22f, y - r * 0.1f, x + r * 0.22f, y - r * 0.5f, Gfx.withAlpha(0xFFFFE08A, 130), 3);
                break;
            }
            case 12: {
                g.circle(c, x, y - r * 0.2f, r * 0.9f, Gfx.withAlpha(0xFFFFE08A, 36));
                Path p = new Path();
                p.moveTo(x, y - r * 1.25f);
                p.lineTo(x + r * 0.4f, y + r * 0.5f);
                p.lineTo(x - r * 0.4f, y + r * 0.5f);
                p.close();
                g.fill(0xFFFFE08A);
                c.drawPath(p, g.p);
                g.line(c, x, y - r * 0.8f, x, y + r * 0.3f, Gfx.withAlpha(0xFFFFFFFF, 180), 2);
                break;
            }
            case 13: {
                int wall = Gfx.mix(wd.deco, 0xFF8A7050, 0.45f);
                g.fill(Gfx.withAlpha(0xFF000000, 60));
                c.drawRect(x - r * 0.95f, y + r * 0.2f, x + r * 0.95f, y + r * 0.45f, g.p);
                g.fill(wall);
                c.drawRect(x - r * 0.85f, y - r * 0.35f, x + r * 0.85f, y + r * 0.3f, g.p);
                g.fill(Gfx.mix(wall, 0xFF000000, 0.25f));
                c.drawRect(x - r * 0.85f, y + r * 0.2f, x + r * 0.85f, y + r * 0.32f, g.p);
                Path roof = new Path();
                roof.moveTo(x - r * 1.05f, y + r * 0.1f);
                roof.lineTo(x, y - r * 0.95f);
                roof.lineTo(x + r * 1.05f, y + r * 0.1f);
                roof.close();
                g.fill(Gfx.mix(wd.accent, 0xFF4A3423, 0.55f));
                c.drawPath(roof, g.p);
                g.fill(Gfx.withAlpha(0xFF000000, 55));
                c.drawRect(x + r * 0.1f, y - r * 0.8f, x + r * 0.85f, y + r * 0.1f, g.p);
                g.roundRect(c, x - r * 0.24f, y - r * 0.02f, r * 0.48f, r * 0.34f, 3, 0xFF3A2A1C);
                g.circle(c, x - r * 0.22f, y - r * 0.08f, r * 0.12f, Gfx.withAlpha(0xFFFFD98A, (int) (170 + 40 * Math.sin(time * 2f))));
                g.circle(c, x + r * 0.22f, y - r * 0.08f, r * 0.12f, Gfx.withAlpha(0xFFFFD98A, (int) (170 + 40 * Math.sin(time * 2f + 1f))));
                break;
            }
            case 14: {
                int stone = Gfx.mix(wd.deco, 0xFF9AA0B0, 0.5f);
                g.fill(stone);
                c.drawRect(x - r * 0.55f, y - r * 1.5f, x + r * 0.55f, y + r * 0.4f, g.p);
                g.fill(Gfx.withAlpha(0xFF000000, 40));
                c.drawRect(x + r * 0.1f, y - r * 1.5f, x + r * 0.55f, y + r * 0.4f, g.p);
                g.roundRect(c, x - r * 0.8f, y - r * 1.75f, r * 1.6f, r * 0.3f, 4, Gfx.mix(wd.accent, stone, 0.5f));
                for (int i = 0; i < 4; i++) {
                    g.line(c, x - r * 0.3f, y - r * 1.1f + i * r * 0.32f, x + r * 0.3f, y - r * 1.1f + i * r * 0.32f, Gfx.withAlpha(0xFF000000, 60), 2f);
                }
                g.line(c, x + r * 0.55f, y - r * 1.6f, x + r * 0.95f, y - r * 1.6f, 0xFF6A5A40, 3f);
                Path flag = new Path();
                flag.moveTo(x + r * 0.95f, y - r * 1.6f);
                flag.lineTo(x + r * 1.45f, y - r * 1.45f + (float) Math.sin(time * 3) * 3f);
                flag.lineTo(x + r * 0.95f, y - r * 1.2f);
                flag.close();
                g.fill(Gfx.withAlpha(wd.accent, 200));
                c.drawPath(flag, g.p);
                break;
            }
            case 15: {
                Path tent = new Path();
                tent.moveTo(x, y - r * 1.15f);
                tent.lineTo(x + r * 1.0f, y + r * 0.35f);
                tent.lineTo(x - r * 1.0f, y + r * 0.35f);
                tent.close();
                g.fill(Gfx.mix(wd.accent, 0xFF8A6A4A, 0.45f));
                c.drawPath(tent, g.p);
                g.line(c, x, y - r * 1.1f, x, y + r * 0.3f, Gfx.withAlpha(0xFFFFFFFF, 55), 2f);
                Path flap = new Path();
                flap.moveTo(x - r * 0.3f, y + r * 0.3f);
                flap.lineTo(x, y - r * 0.45f);
                flap.lineTo(x + r * 0.3f, y + r * 0.3f);
                flap.close();
                g.fill(Gfx.withAlpha(0xFF120D0A, 190));
                c.drawPath(flap, g.p);
                g.line(c, x - r * 1.02f, y + r * 0.36f, x + r * 1.02f, y + r * 0.36f, Gfx.withAlpha(0xFF000000, 80), 3f);
                break;
            }
            case 16: {
                int wood = 0xFF5A4228;
                g.line(c, x - r * 0.8f, y + r * 0.4f, x - r * 0.8f, y - r * 0.5f, wood, 5f);
                g.line(c, x + r * 0.8f, y + r * 0.4f, x + r * 0.8f, y - r * 0.5f, wood, 5f);
                g.roundRect(c, x - r * 0.95f, y - r * 0.62f, r * 1.9f, r * 0.22f, 3, wood);
                for (int i = 0; i < 4; i++) {
                    g.roundRect(c, x - r * 0.95f + i * r * 0.475f, y - r * 0.95f, r * 0.475f, r * 0.4f, 3,
                            (i % 2 == 0) ? Gfx.withAlpha(wd.accent, 220) : 0xFFE8E2D2);
                }
                g.roundRect(c, x - r * 0.7f, y + r * 0.05f, r * 0.55f, r * 0.36f, 3, 0xFF6A4E2E);
                g.roundRect(c, x + r * 0.05f, y + r * 0.05f, r * 0.5f, r * 0.32f, 3, 0xFF7A5A34);
                g.circle(c, x - r * 0.38f, y + r * 0.12f, r * 0.12f, 0xFFE0A05A);
                g.circle(c, x + r * 0.3f, y + r * 0.1f, r * 0.1f, 0xFF9BD44A);
                break;
            }
            case 17: {
                int stone = Gfx.mix(wd.deco, 0xFFB0B4C0, 0.5f);
                g.roundRect(c, x - r * 0.95f, y - r * 1.15f, r * 0.34f, r * 1.5f, 3, stone);
                g.roundRect(c, x + r * 0.61f, y - r * 1.15f, r * 0.34f, r * 1.5f, 3, stone);
                Path arch = new Path();
                arch.moveTo(x - r * 0.95f, y - r * 1.0f);
                arch.quadTo(x, y - r * 1.95f, x + r * 0.95f, y - r * 1.0f);
                g.stroke(stone, r * 0.22f);
                c.drawPath(arch, g.p);
                g.line(c, x - r * 0.95f, y - r * 1.3f, x - r * 0.61f, y - r * 1.3f, Gfx.withAlpha(wd.accent, 120), 2f);
                g.circle(c, x, y - r * 1.62f, r * 0.13f, Gfx.withAlpha(wd.accent, (int) (150 + 70 * Math.sin(time * 1.6f))));
                g.fill(Gfx.withAlpha(0xFF000000, 45));
                c.drawRect(x - r * 0.61f, y - r * 0.1f, x + r * 0.61f, y + r * 0.35f, g.p);
                break;
            }
            case 18: {
                int stone = Gfx.mix(wd.deco, 0xFFC0C4D0, 0.45f);
                g.roundRect(c, x - r * 0.7f, y + r * 0.05f, r * 1.4f, r * 0.35f, 4, stone);
                g.roundRect(c, x - r * 0.55f, y - r * 0.15f, r * 1.1f, r * 0.2f, 3, Gfx.mix(stone, 0xFFFFFFFF, 0.1f));
                g.circle(c, x, y - r * 0.55f, r * 0.24f, stone);
                g.roundRect(c, x - r * 0.24f, y - r * 0.45f, r * 0.48f, r * 0.42f, 6, stone);
                g.line(c, x - r * 0.12f, y - r * 0.42f, x - r * 0.45f, y - r * 0.05f, stone, 4f);
                g.line(c, x + r * 0.12f, y - r * 0.42f, x + r * 0.45f, y - r * 0.05f, stone, 4f);
                g.circleStroke(c, x, y - r * 0.78f, r * 0.42f, Gfx.withAlpha(wd.accent, 70), 2f);
                break;
            }
            case 19: {
                int stone = Gfx.mix(wd.deco, 0xFFAEB6C4, 0.5f);
                g.fill(stone);
                c.drawOval(x - r * 0.95f, y - r * 0.1f, x + r * 0.95f, y + r * 0.6f, g.p);
                g.fill(Gfx.withAlpha(0xFF2A3B44, 220));
                c.drawOval(x - r * 0.8f, y - r * 0.02f, x + r * 0.8f, y + r * 0.48f, g.p);
                g.fill(Gfx.withAlpha(wd.accent, 130));
                c.drawOval(x - r * 0.62f, y + r * 0.02f, x + r * 0.62f, y + r * 0.36f, g.p);
                g.roundRect(c, x - r * 0.1f, y - r * 0.7f, r * 0.2f, r * 0.62f, 3, stone);
                float jet = (float) Math.abs(Math.sin(time * 2.2f)) * r * 0.35f;
                g.circle(c, x, y - r * 0.75f - jet, r * 0.1f, Gfx.withAlpha(0xFFBFF4FF, 190));
                g.circle(c, x - r * 0.25f, y - r * 0.4f, r * 0.05f, Gfx.withAlpha(0xFFBFF4FF, 140));
                g.circle(c, x + r * 0.25f, y - r * 0.4f, r * 0.05f, Gfx.withAlpha(0xFFBFF4FF, 140));
                break;
            }
            case 20: {
                int stone = Gfx.mix(wd.deco, 0xFFB8BCC8, 0.45f);
                Path ob = new Path();
                ob.moveTo(x - r * 0.3f, y + r * 0.35f);
                ob.lineTo(x - r * 0.2f, y - r * 1.4f);
                ob.lineTo(x, y - r * 1.6f);
                ob.lineTo(x + r * 0.2f, y - r * 1.4f);
                ob.lineTo(x + r * 0.3f, y + r * 0.35f);
                ob.close();
                g.fill(stone);
                c.drawPath(ob, g.p);
                g.fill(Gfx.withAlpha(0xFFFFFFFF, 25));
                c.drawRect(x - r * 0.2f, y - r * 1.35f, x - r * 0.02f, y + r * 0.3f, g.p);
                for (int i = 0; i < 3; i++) {
                    g.circle(c, x, y - r * 0.95f + i * r * 0.45f, r * 0.07f,
                            Gfx.withAlpha(wd.accent, (int) (120 + 80 * Math.sin(time * 2f + i))));
                }
                break;
            }
            case 21: {
                int[] petal = {wd.accent, 0xFFFFD86E, 0xFFFF8AB0};
                for (int i = 0; i < 5; i++) {
                    float a = o.seed + i * 1.26f;
                    float fx = x + (float) Math.cos(a) * r * 0.7f;
                    float fy = y + (float) Math.sin(a) * r * 0.45f;
                    int col = petal[i % petal.length];
                    for (int k = 0; k < 4; k++) {
                        float pa = k * 1.57f + a;
                        g.circle(c, fx + (float) Math.cos(pa) * r * 0.11f, fy + (float) Math.sin(pa) * r * 0.11f, r * 0.1f, col);
                    }
                    g.circle(c, fx, fy, r * 0.07f, 0xFFFFF3C4);
                }
                break;
            }
            case 22: {
                for (int i = 0; i < 7; i++) {
                    float a = o.seed + i * 0.9f;
                    float bx = x + (float) Math.cos(a) * r * 0.55f;
                    float by = y + (float) Math.sin(a) * r * 0.4f;
                    float sway = (float) Math.sin(time * 1.8f + i) * r * 0.12f;
                    g.line(c, bx, by + r * 0.35f, bx + sway, by - r * 0.7f, Gfx.withAlpha(wd.accent, 150), 2.2f);
                    g.line(c, bx + sway, by - r * 0.7f, bx + sway + r * 0.16f, by - r * 0.95f, Gfx.withAlpha(wd.accent, 130), 1.8f);
                }
                break;
            }
            case 23: {
                for (int i = 0; i < 3; i++) {
                    float mx = x + (i - 1) * r * 0.45f;
                    float my = y + (i == 1 ? -r * 0.12f : r * 0.1f);
                    float mr = r * (i == 1 ? 0.4f : 0.3f);
                    g.roundRect(c, mx - mr * 0.18f, my, mr * 0.36f, mr * 0.9f, 3, 0xFFE6DCC0);
                    g.fill(Gfx.mix(wd.accent, 0xFFC0405A, 0.4f));
                    c.drawOval(mx - mr, my - mr * 0.4f, mx + mr, my + mr * 0.35f, g.p);
                    g.circle(c, mx - mr * 0.3f, my - mr * 0.05f, mr * 0.12f, Gfx.withAlpha(0xFFFFE8F0, 200));
                }
                break;
            }
            case 24: {
                g.line(c, x, y + r * 0.35f, x, y - r * 0.85f, 0xFF3A3A44, 3.4f);
                g.circle(c, x, y - r * 0.95f, r * 0.24f, Gfx.withAlpha(0xFFFFE9A8, (int) (40 + 30 * Math.sin(time * 3f + o.seed))));
                g.circle(c, x, y - r * 0.95f, r * 0.13f, 0xFFFFF0C0);
                g.line(c, x - r * 0.12f, y - r * 1.05f, x + r * 0.12f, y - r * 1.05f, 0xFF3A3A44, 2.4f);
                break;
            }
            case 25: {
                g.line(c, x, y + r * 0.35f, x, y - r * 0.6f, 0xFF4A3423, 4f);
                float fl = 1f + 0.18f * (float) Math.sin(time * 9f + o.seed);
                g.circle(c, x, y - r * 0.85f, r * 0.35f * fl, Gfx.withAlpha(0xFFFF9A3D, 60));
                Path flame = new Path();
                flame.moveTo(x, y - r * 1.3f * fl);
                flame.quadTo(x + r * 0.28f, y - r * 0.7f, x, y - r * 0.5f);
                flame.quadTo(x - r * 0.28f, y - r * 0.7f, x, y - r * 1.3f * fl);
                flame.close();
                g.fill(0xFFFF9A3D);
                c.drawPath(flame, g.p);
                g.circle(c, x, y - r * 0.78f, r * 0.12f, 0xFFFFF0B0);
                break;
            }
            case 26: {
                g.roundRect(c, x - r * 0.42f, y - r * 0.5f, r * 0.84f, r * 0.9f, 6, 0xFF6A4A28);
                g.line(c, x - r * 0.44f, y - r * 0.18f, x + r * 0.44f, y - r * 0.18f, 0xFF3A3028, 3f);
                g.line(c, x - r * 0.44f, y + r * 0.16f, x + r * 0.44f, y + r * 0.16f, 0xFF3A3028, 3f);
                g.line(c, x - r * 0.2f, y - r * 0.45f, x - r * 0.2f, y + r * 0.35f, Gfx.withAlpha(0xFFFFFFFF, 25), 2f);
                break;
            }
            case 27: {
                g.roundRect(c, x - r * 0.45f, y - r * 0.4f, r * 0.9f, r * 0.8f, 3, 0xFF7A5A34);
                g.line(c, x - r * 0.42f, y - r * 0.37f, x + r * 0.42f, y + r * 0.37f, Gfx.withAlpha(0xFF3A2A18, 140), 2.4f);
                g.line(c, x + r * 0.42f, y - r * 0.37f, x - r * 0.42f, y + r * 0.37f, Gfx.withAlpha(0xFF3A2A18, 140), 2.4f);
                break;
            }
            case 28: {
                g.line(c, x, y + r * 0.4f, x, y - r * 1.15f, 0xFF4A3A28, 4f);
                Path cloth = new Path();
                cloth.moveTo(x, y - r * 1.1f);
                cloth.lineTo(x + r * 0.75f, y - r * 0.95f + (float) Math.sin(time * 2.4f + o.seed) * 4f);
                cloth.lineTo(x + r * 0.7f, y - r * 0.2f);
                cloth.lineTo(x, y - r * 0.4f);
                cloth.close();
                g.fill(Gfx.withAlpha(wd.accent, 210));
                c.drawPath(cloth, g.p);
                g.circle(c, x + r * 0.36f, y - r * 0.62f, r * 0.13f, Gfx.withAlpha(0xFFFFFFFF, 150));
                break;
            }
            case 29: {
                g.line(c, x, y + r * 0.4f, x, y - r * 0.75f, 0xFF5A4228, 4f);
                g.roundRect(c, x - r * 0.55f, y - r * 0.95f, r * 1.1f, r * 0.34f, 3, 0xFF7A5A34);
                Path tip = new Path();
                tip.moveTo(x + r * 0.55f, y - r * 0.95f);
                tip.lineTo(x + r * 0.8f, y - r * 0.78f);
                tip.lineTo(x + r * 0.55f, y - r * 0.61f);
                tip.close();
                g.fill(0xFF7A5A34);
                c.drawPath(tip, g.p);
                g.line(c, x - r * 0.35f, y - r * 0.84f, x + r * 0.35f, y - r * 0.84f, Gfx.withAlpha(0xFF3A2A18, 130), 2f);
                break;
            }
            case 30: {
                g.circle(c, x, y - r * 0.2f, r * 0.9f, Gfx.withAlpha(0xFFFF9A3D, 40));
                g.line(c, x - r * 0.6f, y + r * 0.2f, x + r * 0.35f, y - r * 0.1f, 0xFF4A3423, 5f);
                g.line(c, x - r * 0.35f, y + r * 0.05f, x + r * 0.6f, y + r * 0.15f, 0xFF4A3423, 5f);
                float fl = 1f + 0.2f * (float) Math.sin(time * 8f + o.seed);
                Path fire = new Path();
                fire.moveTo(x, y - r * 1.0f * fl);
                fire.quadTo(x + r * 0.32f, y - r * 0.4f, x, y - r * 0.15f);
                fire.quadTo(x - r * 0.32f, y - r * 0.4f, x, y - r * 1.0f * fl);
                fire.close();
                g.fill(0xFFFF7A3D);
                c.drawPath(fire, g.p);
                g.circle(c, x, y - r * 0.42f, r * 0.13f, 0xFFFFE9A8);
                break;
            }
            case 31: {
                g.circle(c, x, y - r * 0.2f, r * 0.9f, Gfx.withAlpha(wd.accent, 40));
                for (int i = 0; i < 3; i++) {
                    float a = o.seed + i * 2.1f;
                    float bx = x + (float) Math.cos(a) * r * 0.35f;
                    float by = y + (float) Math.sin(a) * r * 0.2f;
                    float hh = r * (0.8f + 0.25f * (i % 2));
                    Path sh = new Path();
                    sh.moveTo(bx, by - hh);
                    sh.lineTo(bx + r * 0.17f, by + r * 0.1f);
                    sh.lineTo(bx - r * 0.17f, by + r * 0.1f);
                    sh.close();
                    g.fill(Gfx.withAlpha(wd.accent, 190));
                    c.drawPath(sh, g.p);
                    g.line(c, bx, by - hh * 0.8f, bx, by, Gfx.withAlpha(0xFFFFFFFF, 120), 1.6f);
                }
                break;
            }
            case 32: {
                for (int i = 0; i < 3; i++) {
                    Path v = new Path();
                    float sx = x - r * 0.5f + i * r * 0.5f;
                    v.moveTo(sx, y - r * 0.8f);
                    v.quadTo(sx + r * 0.22f, y - r * 0.2f, sx - r * 0.12f, y + r * 0.5f);
                    g.stroke(Gfx.withAlpha(0xFF4A7A3A, 190), 2.4f);
                    c.drawPath(v, g.p);
                    g.circle(c, sx + r * 0.08f, y - r * 0.25f, r * 0.1f, Gfx.withAlpha(wd.accent, 170));
                    g.circle(c, sx - r * 0.05f, y + r * 0.1f, r * 0.09f, Gfx.withAlpha(wd.accent, 150));
                }
                break;
            }
            case 33: {
                int stone = Gfx.mix(wd.deco, 0xFFC0C4D0, 0.4f);
                g.roundRect(c, x - r * 0.35f, y - r * 0.75f, r * 0.7f, r * 1.2f, 4, stone);
                Path broken = new Path();
                broken.moveTo(x - r * 0.35f, y - r * 0.75f);
                broken.lineTo(x - r * 0.12f, y - r * 1.0f);
                broken.lineTo(x + r * 0.08f, y - r * 0.72f);
                broken.lineTo(x + r * 0.35f, y - r * 0.88f);
                broken.lineTo(x + r * 0.35f, y - r * 0.75f);
                broken.close();
                g.fill(Gfx.mix(stone, 0xFF000000, 0.3f));
                c.drawPath(broken, g.p);
                g.line(c, x - r * 0.35f, y - r * 0.4f, x + r * 0.35f, y - r * 0.4f, Gfx.withAlpha(0xFF000000, 50), 2f);
                g.line(c, x - r * 0.32f, y + r * 0.1f, x + r * 0.32f, y + r * 0.1f, Gfx.withAlpha(0xFF000000, 50), 2f);
                break;
            }
            case 34: {
                g.circle(c, x - r * 0.25f, y - r * 0.1f, r * 0.28f, 0xFFE8E2D0);
                g.circle(c, x - r * 0.33f, y - r * 0.15f, r * 0.08f, 0xFF2A2420);
                g.circle(c, x - r * 0.17f, y - r * 0.15f, r * 0.08f, 0xFF2A2420);
                g.line(c, x - r * 0.08f, y + r * 0.15f, x + r * 0.6f, y + r * 0.32f, 0xFFE8E2D0, 3.4f);
                g.line(c, x + r * 0.1f, y + r * 0.05f, x + r * 0.7f, y - r * 0.05f, 0xFFE8E2D0, 3f);
                g.circle(c, x + r * 0.62f, y + r * 0.33f, r * 0.07f, 0xFFE8E2D0);
                g.circle(c, x + r * 0.72f, y - r * 0.06f, r * 0.065f, 0xFFE8E2D0);
                break;
            }
            case 99: {
                int wall = Gfx.mix(wd.deco, 0xFF3A3A44, 0.55f);
                g.circle(c, x - r * 0.35f, y + r * 0.1f, r * 0.62f, wall);
                g.circle(c, x + r * 0.3f, y - r * 0.05f, r * 0.58f, Gfx.mix(wall, 0xFFFFFFFF, 0.08f));
                g.circle(c, x + r * 0.05f, y - r * 0.4f, r * 0.4f, Gfx.mix(wall, wd.accent, 0.12f));
                break;
            }
            default: {
                g.circle(c, x, y, r * 0.95f, 0xFF2B2436);
                g.circleStroke(c, x, y, r * 0.8f, Gfx.withAlpha(accent, 80), 3);
                break;
            }
        }
    }

    public float[] resolvePos(float px, float py, float radius) {
        float x = px, y = py;
        for (int iter = 0; iter < 2; iter++) {
            for (Obstacle o : obstacles) {
                if (!o.solid) continue;
                float dx = x - o.x;
                float dy = y - o.y;
                float min = radius + o.r;
                float d2 = dx * dx + dy * dy;
                if (d2 < min * min && d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (min - d);
                    x += dx / d * push;
                    y += dy / d * push;
                }
            }
        }
        float m = radius + 40;
        if (x < m) x = m;
        if (y < m) y = m;
        if (x > w - m) x = w - m;
        if (y > h - m) y = h - m;
        return new float[]{x, y};
    }

    public void resolve(Player p, float radius) {
        float[] r = resolvePos(p.x, p.y, radius);
        p.x = r[0];
        p.y = r[1];
    }

    public void resolve(Enemy e, float radius) {
        float[] r = resolvePos(e.x, e.y, radius);
        e.x = r[0];
        e.y = r[1];
    }

    public boolean collides(float x, float y, float radius) {
        for (Obstacle o : obstacles) {
            if (!o.solid) continue;
            float dx = x - o.x, dy = y - o.y;
            float min = radius + o.r;
            if (dx * dx + dy * dy < min * min) return true;
        }
        return x < 40 || y < 40 || x > w - 40 || y > h - 40;
    }

    public float[] randomFreePos(Random rnd, float awayX, float awayY, float minAway) {
        for (int i = 0; i < 90; i++) {
            float x = 200 + rnd.nextFloat() * (w - 400);
            float y = 240 + rnd.nextFloat() * (h - 480);
            if (near(x, y, awayX, awayY, minAway)) continue;
            if (near(x, y, chestX, chestY, 110)) continue;
            if (near(x, y, shrineX, shrineY, 150)) continue;
            if (collides(x, y, 34)) continue;
            return new float[]{x, y};
        }
        return new float[]{w * 0.5f, h * 0.3f};
    }

    private static float pointSegDist(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float l2 = dx * dx + dy * dy;
        if (l2 == 0) return (float) Gfx.dist(px, py, x1, y1);
        float t = ((px - x1) * dx + (py - y1) * dy) / l2;
        t = Gfx.clamp(t, 0f, 1f);
        return (float) Gfx.dist(px, py, x1 + t * dx, y1 + t * dy);
    }
}
