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
        this.theme = Math.max(0, Math.min(4, stageIndex / 5));
        this.stageNum = Math.max(0, Math.min(4, stageIndex % 5));
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
            addObstacle(x, margin, 62, 20, r);
            addObstacle(x, h - margin, 62, 20, r);
        }
        int stepY = 150;
        for (float y = margin + stepY; y < h - margin; y += stepY) {
            addObstacle(margin, y, 62, 20, r);
            addObstacle(w - margin, y, 62, 20, r);
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
    }

    private boolean near(float x1, float y1, float x2, float y2, float d) {
        float dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy < d * d;
    }

    private void addObstacle(float x, float y, float r, int type, Random rnd) {
        Obstacle o = new Obstacle();
        o.x = x;
        o.y = y;
        o.r = r;
        o.type = type;
        o.seed = rnd.nextFloat() * 100f;
        obstacles.add(o);
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
            default:
                if (n < 45) return 10;
                if (n < 66) return 11;
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
                boolean alt = ((tx + ty) & 1) == 0;
                int col = alt ? wd.groundA : wd.groundB;
                g.fill(col);
                c.drawRect(cx, cy, cx + TILE + 1, cy + TILE + 1, g.p);

                float tcx = cx + TILE / 2, tcy = cy + TILE / 2;
                if (pointSegDist(tcx, tcy, spawnX, spawnY, shrineX, shrineY) < 82f
                        || pointSegDist(tcx, tcy, spawnX, spawnY, chestX, chestY) < 60f) {
                    g.fill(Gfx.withAlpha(wd.path, 150));
                    c.drawRect(cx + 4, cy + 4, cx + TILE - 4, cy + TILE - 4, g.p);
                }

                int hs = hash(tx, ty);
                if (quality) {
                    if ((hs & 7) == 0) {
                        float dx = cx + 14 + (hs & 63);
                        float dy = cy + 14 + ((hs >> 6) & 63);
                        g.circle(c, dx, dy, 3.5f, Gfx.withAlpha(wd.accent, 70));
                        g.circle(c, dx + 10, dy + 6, 2.2f, Gfx.withAlpha(wd.accent, 50));
                    } else if ((hs & 15) == 3) {
                        float dx = cx + 20 + (hs & 47);
                        float dy = cy + 20 + ((hs >> 6) & 47);
                        g.line(c, dx, dy, dx + 6, dy - 10, Gfx.withAlpha(wd.deco, 220), 2.4f);
                        g.line(c, dx + 6, dy, dx + 10, dy - 8, Gfx.withAlpha(wd.deco, 200), 2f);
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
        if (theme == 4) {
            for (int i = 0; i < 34; i++) {
                int hs = hash(i, 77);
                float sx = (hs & 0xFFFF) / 65535f * w;
                float sy = ((hs >> 16) & 0xFFFF) / 65535f * h;
                float tw = 0.4f + 0.6f * (float) Math.abs(Math.sin(time * 1.5f + i));
                g.circle(c, sx, sy, 1.6f * tw, Gfx.withAlpha(0xFFFFE08A, (int) (120 * tw)));
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
        g.fill(0x33000000);
        c.drawOval(x - r * 1.05f, y + r * 0.25f, x + r * 1.05f, y + r * 0.75f, g.p);

        switch (o.type) {
            case 0: {
                g.roundRect(c, x - r * 0.18f, y - r * 0.2f, r * 0.36f, r * 1.0f, 5, 0xFF4A3423);
                int leaf = Gfx.mix(wd.accent, 0xFF1C4028, 0.55f);
                g.circle(c, x - r * 0.35f, y - r * 0.5f, r * 0.62f, leaf);
                g.circle(c, x + r * 0.35f, y - r * 0.55f, r * 0.66f, leaf);
                g.circle(c, x, y - r * 0.95f, r * 0.6f, Gfx.mix(leaf, 0xFFFFFFFF, 0.08f));
                g.circle(c, x + r * 0.15f, y - r * 0.9f, r * 0.22f, Gfx.withAlpha(accent, 60));
                break;
            }
            case 1: {
                g.circle(c, x - r * 0.35f, y, r * 0.62f, Gfx.mix(wd.accent, wd.deco, 0.55f));
                g.circle(c, x + r * 0.35f, y, r * 0.6f, Gfx.mix(wd.accent, wd.deco, 0.45f));
                g.circle(c, x, y - r * 0.3f, r * 0.54f, Gfx.mix(wd.accent, wd.deco, 0.35f));
                break;
            }
            case 2: {
                g.circle(c, x, y, r * 0.9f, rock);
                g.circle(c, x - r * 0.3f, y - r * 0.2f, r * 0.45f, Gfx.mix(rock, 0xFFFFFFFF, 0.18f));
                g.circle(c, x + r * 0.35f, y + r * 0.1f, r * 0.3f, Gfx.mix(rock, 0xFFFFFFFF, 0.1f));
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
