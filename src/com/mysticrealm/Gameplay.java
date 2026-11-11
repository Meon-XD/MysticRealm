package com.mysticrealm;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gameplay {

    static class Proj {
        float x, y, dir, speed, dmg, critChance, radius, life, maxLife;
        int color, kind;
        boolean poison, enemy;
    }

    static class Blast {
        float x, y, r, delay, maxDelay, dmg, critChance, stun, slow;
        int color;
        boolean enemy;
    }

    static class Ring {
        float x, y, r, life, maxLife;
        int color;
    }

    static class Slash {
        float x, y, dir, range, life, maxLife;
        int color;
    }

    static class Boom {
        float x, y, r, life, maxLife;
        int color;
    }

    static class HitFx {
        float x, y, life, maxLife;
        int color;
    }

    static class Dust {
        float x, y, vx, vy, life, maxLife, r;
        int color;
    }

    static class Trail {
        float x1, y1, x2, y2, life, maxLife;
        int color;
    }

    static class Txt {
        float x, y, life, maxLife;
        String text;
        int color;
    }

    static class Pickup {
        float x, y, vx, vy, life, t;
        int type;
        int value;
    }

    static class Toast {
        String msg;
        float life;
    }

    public final SaveData save;
    public final Audio audio;
    public final World world;
    public Player player;

    public final ArrayList<Enemy> enemies = new ArrayList<>();
    public final ArrayList<Proj> projectiles = new ArrayList<>();
    public final ArrayList<Blast> blasts = new ArrayList<>();
    public final ArrayList<Pickup> pickups = new ArrayList<>();
    private final ArrayList<Ring> rings = new ArrayList<>();
    private final ArrayList<Slash> slashes = new ArrayList<>();
    private final ArrayList<Boom> booms = new ArrayList<>();
    private final ArrayList<HitFx> hits = new ArrayList<>();
    private final ArrayList<Dust> dust = new ArrayList<>();
    private final ArrayList<Trail> trails = new ArrayList<>();
    private final ArrayList<Txt> texts = new ArrayList<>();
    private final ArrayList<Toast> toasts = new ArrayList<>();

    public int stageIndex, stage, diff;
    public float time;
    public float camX, camY, viewW, viewH, zoom = 1.32f;
    public float shakeAmt;

    public Puzzle puzzle;
    public boolean puzzleOpen;
    public boolean puzzleResolved;
    public boolean perfectPuzzle;
    public float puzzleCooldown;

    public Enemy boss;
    public boolean bossSpawned;
    public boolean bossDefeated;
    public boolean clearQueued;
    public float clearTimer;

    public boolean finished, won;
    public int rExp, rGold, rShards;
    public float rTime;
    public String rTitle = "";
    public String rSub = "";

    public boolean pauseRequested;
    public float potionCd;
    public LanCoop lan;
    public boolean lanHost;
    public float mouseAimT;
    public float aimX, aimY;

    private Random rnd = new Random();
    private int joyPointer = -1;
    private float joyBaseX, joyBaseY, joyDX, joyDY, joyR;
    private final java.util.HashSet<Integer> keys = new java.util.HashSet<>();
    private float lanSendT;

    private final Path fxPath = new Path();
    private final RectF arcRect = new RectF();

    public Gameplay(SaveData save, Audio audio, int stageIndex) {
        this.save = save;
        this.audio = audio;
        this.stageIndex = Math.max(0, Math.min(24, stageIndex));
        this.stage = this.stageIndex + 1;
        this.diff = Math.max(0, Math.min(4, save.difficulty));
        this.world = new World(this.stageIndex);
        this.player = new Player(this, save);
        this.player.spawn(world.spawnX, world.spawnY);
        this.puzzle = null;
        this.puzzleResolved = false;
        this.puzzleCooldown = 0f;
        this.camX = player.x;
        this.camY = player.y;
        this.rnd = new Random(world.seed * 31L + 77L);
        save.currentStage = this.stageIndex;
        spawnEnemies();
    }

    public float ui() {
        return Gfx.clamp(viewH / 720f, 0.68f, 2.8f) * save.buttonScale;
    }

    public boolean joyActive() {
        if (joyPointer < 0) return false;
        float m = (float) Math.sqrt(joyDX * joyDX + joyDY * joyDY);
        return m > 0.13f;
    }

    public float joyX() {
        return joyR <= 0 ? 0 : Gfx.clamp(joyDX / joyR * save.sensitivity, -1f, 1f);
    }

    public float joyY() {
        return joyR <= 0 ? 0 : Gfx.clamp(joyDY / joyR * save.sensitivity, -1f, 1f);
    }

    // ---------------------------------------------------------------- spawn

    private void spawnEnemies() {
        Catalog.WorldDef wd = Catalog.WORLDS[world.theme];
        float em = Catalog.DIFF_ENEMY[diff];
        int count = 8 + world.stageNum * 2 + world.theme;
        if (world.stageNum == 4) count = 6;
        for (int i = 0; i < count; i++) {
            Catalog.MonsterDef md = wd.mobs[rnd.nextInt(wd.mobs.length)];
            float[] pos = world.randomFreePos(rnd, world.spawnX, world.spawnY, 470);
            boolean elite = md.type >= 2 && rnd.nextFloat() < (0.08f + world.theme * 0.035f + world.stageNum * 0.01f);
            Enemy e = new Enemy(this, md, pos[0], pos[1], em, em, elite, world.theme);
            enemies.add(e);
        }
    }

    private void spawnBoss() {
        if (bossSpawned) return;
        bossSpawned = true;
        Catalog.BossDef bd = Catalog.WORLDS[world.theme].boss;
        float x = world.w * 0.5f;
        float y = world.h * 0.30f;
        if (world.collides(x, y, 80)) {
            float[] pos = world.randomFreePos(rnd, player.x, player.y, 380);
            x = pos[0];
            y = pos[1];
        }
        boss = new Enemy(this, bd, x, y, world.theme, Catalog.DIFF_ENEMY[diff], 1f);
        enemies.add(boss);
        audio.play("boss");
        shake(14);
        fxRing(x, y, 260, bd.color, 0.7f);
        toast(bd.name + " bangkit!");
    }

    private void spawnPenalty() {
        Catalog.WorldDef wd = Catalog.WORLDS[world.theme];
        float em = Catalog.DIFF_ENEMY[diff];
        for (int i = 0; i < 2; i++) {
            Catalog.MonsterDef md = wd.mobs[rnd.nextInt(wd.mobs.length)];
            float ang = rnd.nextFloat() * 6.28f;
            float rad = 90 + rnd.nextFloat() * 110;
            float x = world.shrineX + (float) Math.cos(ang) * rad;
            float y = world.shrineY + (float) Math.sin(ang) * rad;
            x = Gfx.clamp(x, 90, world.w - 90);
            y = Gfx.clamp(y, 90, world.h - 90);
            Enemy e = new Enemy(this, md, x, y, em * 0.9f, em, false, world.theme);
            enemies.add(e);
            fxRing(x, y, 60, 0xFFFF5555, 0.4f);
        }
    }

    public void summonMinions(float x, float y, int count) {
        Catalog.WorldDef wd = Catalog.WORLDS[world.theme];
        float em = Catalog.DIFF_ENEMY[diff];
        for (int i = 0; i < count; i++) {
            Catalog.MonsterDef md = wd.mobs[rnd.nextInt(wd.mobs.length)];
            float ang = rnd.nextFloat() * 6.28f;
            float rad = 70 + rnd.nextFloat() * 120;
            float px = x + (float) Math.cos(ang) * rad;
            float py = y + (float) Math.sin(ang) * rad;
            for (int tries = 0; tries < 8 && world.collides(px, py, 30); tries++) {
                ang = rnd.nextFloat() * 6.28f;
                rad = 70 + rnd.nextFloat() * 120;
                px = x + (float) Math.cos(ang) * rad;
                py = y + (float) Math.sin(ang) * rad;
            }
            px = Gfx.clamp(px, 70, world.w - 70);
            py = Gfx.clamp(py, 70, world.h - 70);
            Enemy e = new Enemy(this, md, px, py, em * 0.7f, em * 0.85f, false, world.theme);
            enemies.add(e);
        }
    }

    // --------------------------------------------------------------- update

    public void update(float dt) {
        time += dt;
        updateEffects(dt);
        if (finished) return;

        if (shakeAmt > 0) shakeAmt = Math.max(0, shakeAmt - dt * 46f);
        if (potionCd > 0) potionCd = Math.max(0, potionCd - dt);
        if (puzzleCooldown > 0) puzzleCooldown = Math.max(0, puzzleCooldown - dt);
        if (mouseAimT > 0) mouseAimT = Math.max(0, mouseAimT - dt);
        if (lanSendT > 0) lanSendT -= dt;

        if (pauseRequested || player.dead) return;

        if (puzzleOpen) {
            if (puzzle != null) {
                puzzle.update(dt);
                if (puzzle.isClosing()) closePuzzle();
            }
        } else {
            float jx = joyX() + keyX();
            float jy = joyY() + keyY();
            float jm = (float) Math.sqrt(jx * jx + jy * jy);
            if (jm > 1f) {
                jx /= jm;
                jy /= jm;
            }
            player.update(dt, jx, jy);
            if (mouseAimT > 0) {
                player.dir = (float) Math.atan2(aimY - player.y, aimX - player.x);
            }
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                e.update(dt, player);
            }
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                if (e.dead && e.deathT > 0.6f) enemies.remove(i);
            }
            updateProjectiles(dt);
            updatePickups(dt);
            if (lan != null && lan.connected && lanSendT <= 0) {
                lan.sendState(player);
                lanSendT = 0.07f;
            }
        }

        updateBlasts(dt);
        updateWorldTriggers(dt);
        updateCamera(dt);

        if (clearQueued) {
            clearTimer -= dt;
            if (clearTimer <= 0f) finishStage(true);
        }
    }

    private void updateWorldTriggers(float dt) {
        if (finished) return;

        if (!world.chestOpen) {
            if (Gfx.dist(player.x, player.y, world.chestX, world.chestY) < 74f) {
                openChest();
            }
        }

        if (puzzleOpen) return;
        if (puzzleResolved) return;
        if (puzzleCooldown > 0) return;
        if (clearQueued) return;
        if (remainingEnemies() > 0) return;
        if (Gfx.dist(player.x, player.y, world.shrineX, world.shrineY) < 132f) {
            openPuzzle();
        }
    }

    private void updateCamera(float dt) {
        float halfW = viewW / (2f * zoom);
        float halfH = viewH / (2f * zoom);
        float tx = player.x;
        float ty = player.y;
        if (world.w <= halfW * 2) tx = world.w * 0.5f;
        else tx = Gfx.clamp(tx, halfW, world.w - halfW);
        if (world.h <= halfH * 2) ty = world.h * 0.5f;
        else ty = Gfx.clamp(ty, halfH, world.h - halfH);
        float k = 1f - (float) Math.pow(0.0015f, dt);
        camX += (tx - camX) * k;
        camY += (ty - camY) * k;
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Proj p = projectiles.get(i);
            p.x += (float) Math.cos(p.dir) * p.speed * dt;
            p.y += (float) Math.sin(p.dir) * p.speed * dt;
            p.life -= dt;
            boolean gone = p.life <= 0;
            if (!gone && world.collides(p.x, p.y, 3)) {
                gone = true;
                fxDust(p.x, p.y, p.color);
            }
            if (!gone) {
                if (p.enemy) {
                    if (Gfx.dist(p.x, p.y, player.x, player.y) < player.radius + p.radius) {
                        player.damage(p.dmg, null);
                        fxHit(p.x, p.y, p.color);
                        gone = true;
                    }
                } else {
                    for (int j = 0; j < enemies.size(); j++) {
                        Enemy e = enemies.get(j);
                        if (e.dead) continue;
                        if (Gfx.dist(p.x, p.y, e.x, e.y) < e.radius + p.radius) {
                            float kb = 90f;
                            e.damage(p.dmg, p.critChance,
                                    (float) Math.cos(p.dir) * kb, (float) Math.sin(p.dir) * kb,
                                    p.poison, p.dmg);
                            fxHit(p.x, p.y, p.color);
                            gone = true;
                            break;
                        }
                    }
                }
            }
            if (gone) projectiles.remove(i);
        }
    }

    private void updatePickups(float dt) {
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.t += dt;
            p.life -= dt;
            float d = (float) Gfx.dist(p.x, p.y, player.x, player.y);
            if (d < 190f) {
                float a = (float) Math.atan2(player.y - p.y, player.x - p.x);
                float sp = 300f + (190f - d) * 3.2f;
                p.vx += (float) Math.cos(a) * sp * dt * 6f;
                p.vy += (float) Math.sin(a) * sp * dt * 6f;
            }
            p.vx *= (float) Math.pow(0.02f, dt);
            p.vy *= (float) Math.pow(0.02f, dt);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (d < player.radius + 20f) {
                collect(p);
                pickups.remove(i);
                continue;
            }
            if (p.life <= 0) pickups.remove(i);
        }
    }

    private void collect(Pickup p) {
        if (p.type == 0) {
            save.gold += p.value;
            audio.play("coin", 0.7f);
        } else if (p.type == 1) {
            save.shards += p.value;
            audio.play("pickup");
            floatText(player.x, player.y - 64, "+" + p.value + " Shard", Gfx.CYAN);
        } else if (p.type == 2) {
            player.heal(p.value);
        }
    }

    private void updateBlasts(float dt) {
        for (int i = blasts.size() - 1; i >= 0; i--) {
            Blast b = blasts.get(i);
            b.delay -= dt;
            if (b.delay <= 0f) {
                if (b.enemy) {
                    if (Gfx.dist(b.x, b.y, player.x, player.y) < b.r + player.radius) {
                        player.damage(b.dmg, null);
                    }
                    fxExplosion(b.x, b.y, b.r, b.color);
                    audio.play("hit", 0.6f);
                } else {
                    areaDamage(b.x, b.y, b.r, b.dmg, b.critChance, b.stun, b.slow, b.color);
                    fxExplosion(b.x, b.y, b.r, b.color);
                    shake(9);
                }
                blasts.remove(i);
            }
        }
    }

    private void updateEffects(float dt) {
        for (int i = rings.size() - 1; i >= 0; i--) if ((rings.get(i).life -= dt) <= 0) rings.remove(i);
        for (int i = slashes.size() - 1; i >= 0; i--) if ((slashes.get(i).life -= dt) <= 0) slashes.remove(i);
        for (int i = booms.size() - 1; i >= 0; i--) if ((booms.get(i).life -= dt) <= 0) booms.remove(i);
        for (int i = hits.size() - 1; i >= 0; i--) if ((hits.get(i).life -= dt) <= 0) hits.remove(i);
        for (int i = trails.size() - 1; i >= 0; i--) if ((trails.get(i).life -= dt) <= 0) trails.remove(i);
        for (int i = texts.size() - 1; i >= 0; i--) {
            Txt t = texts.get(i);
            t.y -= 42f * dt;
            if ((t.life -= dt) <= 0) texts.remove(i);
        }
        for (int i = dust.size() - 1; i >= 0; i--) {
            Dust d = dust.get(i);
            d.x += d.vx * dt;
            d.y += d.vy * dt;
            d.vx *= (float) Math.pow(0.1f, dt);
            d.vy *= (float) Math.pow(0.1f, dt);
            if ((d.life -= dt) <= 0) dust.remove(i);
        }
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            if ((t.life -= dt) <= 0) toasts.remove(i);
        }
    }

    // --------------------------------------------------------------- puzzle

    private void openPuzzle() {
        int type = (world.theme * 2 + world.stageNum * 3 + 1) % 8;
        puzzle = new Puzzle(this);
        puzzle.open(type);
        puzzleOpen = true;
        joyPointer = -1;
        audio.play("skill", 0.5f);
        fxRing(world.shrineX, world.shrineY, 150, Gfx.CYAN, 0.6f);
    }

    private void closePuzzle() {
        boolean solved = puzzle != null && puzzle.solved;
        boolean failed = puzzle != null && puzzle.failed;
        boolean hint = puzzle != null && puzzle.usedHint;
        puzzle = null;
        puzzleOpen = false;
        puzzleCooldown = 1.4f;
        if (solved) {
            puzzleResolved = true;
            perfectPuzzle = !hint;
            audio.play("puzzle_ok");
            shake(5);
            fxRing(world.shrineX, world.shrineY, 220, Gfx.GOLD, 0.8f);
            if (world.stageNum == 4) {
                toast("Segel terbuka...");
                spawnBoss();
            } else {
                toast(hint ? "Teka-teki selesai." : "Teka-teki sempurna!");
                clearQueued = true;
                clearTimer = 1.2f;
            }
        } else if (failed) {
            audio.play("puzzle_bad");
            shake(10);
            toast("Teka-teki gagal! Musuh muncul.");
            spawnPenalty();
            puzzleCooldown = 2.2f;
        } else {
            toast("Kau meninggalkan altar.");
        }
    }

    // --------------------------------------------------------------- combat

    public Enemy nearestEnemy(float x, float y, float maxDist) {
        Enemy best = null;
        float bestD = maxDist;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            float d = (float) Gfx.dist(x, y, e.x, e.y);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    public void spawnProjectile(float x, float y, float dir, float speed, float dmg, float critChance,
                                int color, int kind, boolean poison, float radius, float life) {
        Proj p = new Proj();
        p.x = x;
        p.y = y;
        p.dir = dir;
        p.speed = speed;
        p.dmg = dmg;
        p.critChance = critChance;
        p.color = color;
        p.kind = kind;
        p.poison = poison;
        p.radius = radius;
        p.life = life;
        p.maxLife = life;
        p.enemy = false;
        projectiles.add(p);
    }

    public void spawnEnemyProjectile(float x, float y, float angle, float speed, float dmg,
                                     int color, float radius, int kind) {
        Proj p = new Proj();
        p.x = x;
        p.y = y;
        p.dir = angle;
        p.speed = speed;
        p.dmg = dmg;
        p.critChance = 0f;
        p.color = color;
        p.kind = kind;
        p.radius = radius;
        p.life = 4.2f;
        p.maxLife = 4.2f;
        p.enemy = true;
        projectiles.add(p);
    }

    public void meleePlayer(float dmg, float critChance, float range, float arc, float knock,
                            int color, float stun, float slow) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            float d = (float) Gfx.dist(player.x, player.y, e.x, e.y);
            if (d > range + e.radius) continue;
            float a = (float) Math.atan2(e.y - player.y, e.x - player.x);
            if (Math.abs(angDiff(a, player.dir)) > arc / 2f) continue;
            float kbx = (float) Math.cos(a) * knock;
            float kby = (float) Math.sin(a) * knock;
            e.damage(dmg, critChance, kbx, kby, false, 0);
            if (stun > 0) e.applyStun(stun);
            if (slow > 0) e.applySlow(slow);
            fxHit(e.x, e.y - e.radius * 0.3f, color);
        }
    }

    public void areaDamage(float x, float y, float r, float dmg, float critChance,
                           float stun, float slow, int color) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            if (Gfx.dist(x, y, e.x, e.y) > r + e.radius) continue;
            float a = (float) Math.atan2(e.y - y, e.x - x);
            e.damage(dmg, critChance, (float) Math.cos(a) * 110f, (float) Math.sin(a) * 110f, false, 0);
            if (stun > 0) e.applyStun(stun);
            if (slow > 0) e.applySlow(slow);
        }
    }

    public void dashStrike(float x1, float y1, float x2, float y2, float width, float dmg,
                           float critChance, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        float nx = dx / len, ny = dy / len;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            float px = e.x - x1, py = e.y - y1;
            float proj = Gfx.clamp(px * nx + py * ny, 0f, len);
            float cx = x1 + nx * proj, cy = y1 + ny * proj;
            if (Gfx.dist(cx, cy, e.x, e.y) > width + e.radius) continue;
            e.damage(dmg, critChance, nx * 190f, ny * 190f, false, 0);
            fxHit(e.x, e.y - e.radius * 0.3f, color);
        }
    }

    public void stunArea(float x, float y, float r, float dur) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            if (Gfx.dist(x, y, e.x, e.y) <= r + e.radius) e.applyStun(dur);
        }
    }

    public void spawnBlast(float x, float y, float r, float delay, float dmg, float critChance,
                           int color, float stun, float slow) {
        Blast b = new Blast();
        b.x = x;
        b.y = y;
        b.r = r;
        b.delay = delay;
        b.maxDelay = delay;
        b.dmg = dmg;
        b.critChance = critChance;
        b.color = color;
        b.stun = stun;
        b.slow = slow;
        b.enemy = false;
        blasts.add(b);
    }

    public void spawnEnemyBlast(float x, float y, float r, float delay, float dmg, int color) {
        Blast b = new Blast();
        b.x = x;
        b.y = y;
        b.r = r;
        b.delay = delay;
        b.maxDelay = delay;
        b.dmg = dmg;
        b.color = color;
        b.enemy = true;
        blasts.add(b);
    }

    public void onEnemyKilled(Enemy e) {
        fxExplosion(e.x, e.y, e.radius * 2.2f, e.color);
        audio.play("hit", 0.5f);
        int expGain = Math.round(e.exp * Catalog.DIFF_EXP[diff]);
        int gained = save.addExp(expGain);
        if (gained > 0) {
            audio.play("level");
            toast("Level " + save.level + "!");
            fxRing(player.x, player.y, 170, Gfx.GOLD, 0.8f);
        }
        int coins = e.boss ? 14 : 3;
        int per = Math.max(1, Math.round(e.gold * Catalog.DIFF_REWARD[diff] / coins));
        for (int i = 0; i < coins; i++) {
            Pickup p = new Pickup();
            p.x = e.x;
            p.y = e.y;
            p.type = 0;
            p.value = per;
            p.life = 26f;
            float a = rnd.nextFloat() * 6.28f;
            float sp = 60 + rnd.nextFloat() * 120;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp;
            pickups.add(p);
        }
        float shardChance = e.boss ? 1f : (e.elite ? 0.16f : 0.05f);
        if (rnd.nextFloat() < shardChance) {
            Pickup p = new Pickup();
            p.x = e.x;
            p.y = e.y;
            p.type = 1;
            p.value = 1;
            p.life = 30f;
            pickups.add(p);
        }
        if (!e.boss && player.hp < player.maxHp * 0.45f && rnd.nextFloat() < 0.16f) {
            Pickup p = new Pickup();
            p.x = e.x;
            p.y = e.y;
            p.type = 2;
            p.value = Math.round(player.maxHp * 0.16f);
            p.life = 24f;
            pickups.add(p);
        }
        if (e.boss) {
            bossDefeated = true;
            boss = null;
            clearQueued = true;
            clearTimer = 1.6f;
            shake(18);
            for (int i = 0; i < 2; i++) {
                Pickup p = new Pickup();
                p.x = e.x + (rnd.nextFloat() - 0.5f) * 80;
                p.y = e.y + (rnd.nextFloat() - 0.5f) * 80;
                p.type = 1;
                p.value = 1;
                p.life = 32f;
                pickups.add(p);
            }
        }
    }

    public void onPlayerDeath() {
        finishStage(false);
    }

    private void finishStage(boolean win) {
        if (finished) return;
        finished = true;
        won = win;
        rTime = time;
        joyPointer = -1;
        if (win) {
            int baseExp = 70 + stage * 26;
            int baseGold = 55 + stage * 22;
            rExp = Math.round(baseExp * Catalog.DIFF_EXP[diff]) + (perfectPuzzle ? 30 : 0);
            rGold = Math.round(baseGold * Catalog.DIFF_REWARD[diff]);
            rShards = (world.stageNum == 4 ? 3 : 1) + (perfectPuzzle ? 1 : 0);
            save.gold += rGold;
            save.shards += rShards;
            save.addExp(rExp);
            save.markCleared(stageIndex);
            rTitle = "STAGE CLEAR";
            rSub = Catalog.stageName(stageIndex);
            audio.play(world.stageNum == 4 ? "win" : "level");
            fxRing(player.x, player.y, 260, Gfx.GOLD, 1f);
            shake(8);
        } else {
            rExp = 0;
            rGold = 0;
            rShards = 0;
            rTitle = "KAU GUGUR";
            rSub = "Coba lagi, pahlawan.";
            audio.play("death");
        }
    }

    // -------------------------------------------------------------- effects

    public void fxRing(float x, float y, float r, int color, float life) {
        Ring e = new Ring();
        e.x = x;
        e.y = y;
        e.r = r;
        e.color = color;
        e.life = life;
        e.maxLife = life;
        rings.add(e);
    }

    public void fxSlash(float x, float y, float dir, float range, int color) {
        Slash s = new Slash();
        s.x = x;
        s.y = y;
        s.dir = dir;
        s.range = range;
        s.color = color;
        s.life = 0.22f;
        s.maxLife = 0.22f;
        slashes.add(s);
    }

    public void fxExplosion(float x, float y, float r, int color) {
        Boom b = new Boom();
        b.x = x;
        b.y = y;
        b.r = r;
        b.color = color;
        b.life = 0.5f;
        b.maxLife = 0.5f;
        booms.add(b);
        for (int i = 0; i < 6; i++) {
            float a = rnd.nextFloat() * 6.28f;
            fxDust(x + (float) Math.cos(a) * r * 0.3f, y + (float) Math.sin(a) * r * 0.3f, color);
        }
    }

    public void fxHit(float x, float y, int color) {
        HitFx h = new HitFx();
        h.x = x;
        h.y = y;
        h.color = color;
        h.life = 0.2f;
        h.maxLife = 0.2f;
        hits.add(h);
    }

    public void fxDust(float x, float y, int color) {
        Dust d = new Dust();
        d.x = x;
        d.y = y;
        float a = rnd.nextFloat() * 6.28f;
        float sp = 30 + rnd.nextFloat() * 90;
        d.vx = (float) Math.cos(a) * sp;
        d.vy = (float) Math.sin(a) * sp - 30;
        d.r = 3 + rnd.nextFloat() * 5;
        d.life = 0.4f + rnd.nextFloat() * 0.3f;
        d.maxLife = d.life;
        d.color = color;
        dust.add(d);
    }

    public void fxTrail(float x1, float y1, float x2, float y2, int color) {
        Trail t = new Trail();
        t.x1 = x1;
        t.y1 = y1;
        t.x2 = x2;
        t.y2 = y2;
        t.color = color;
        t.life = 0.4f;
        t.maxLife = 0.4f;
        trails.add(t);
    }

    public void floatText(float x, float y, String text, int color) {
        Txt t = new Txt();
        t.x = x;
        t.y = y;
        t.text = text;
        t.color = color;
        t.life = 0.9f;
        t.maxLife = 0.9f;
        texts.add(t);
    }

    public void shake(float v) {
        shakeAmt = Math.max(shakeAmt, v);
    }

    public void toast(String msg) {
        Toast t = new Toast();
        t.msg = msg;
        t.life = 2.8f;
        toasts.add(t);
        if (toasts.size() > 4) toasts.remove(0);
    }

    // --------------------------------------------------------------- chest

    private void openChest() {
        world.chestOpen = true;
        audio.play("pickup");
        fxRing(world.chestX, world.chestY, 90, Gfx.GOLD, 0.7f);
        int cap = Gfx.clamp(1 + stageIndex / 5, 0, 6);
        Catalog.Item it = Catalog.randomEquipment(rnd, cap, player.luck);
        if (it != null && rnd.nextFloat() < 0.72f) {
            save.addItem(it.id, 1);
            toast("Peti: " + it.name + " (" + Gfx.RARITY_NAME[it.rarity] + ")");
        } else {
            int g = 60 + rnd.nextInt(60 + stageIndex * 18);
            save.gold += g;
            toast("Peti: " + g + " gold");
        }
    }

    // ---------------------------------------------------------------- input

    public void pointerDown(int id, float x, float y) {
        if (finished) return;
        if (puzzleOpen) {
            float s = puzzleScale();
            if (puzzle != null) puzzle.pointerDown(x / s, y / s);
            return;
        }
        float u = ui();

        if (Gfx.dist(x, y, viewW - 34 * u, 34 * u) < 40 * u) {
            pauseRequested = true;
            audio.play("click");
            return;
        }
        float ax = attackX(u), ay = attackY(u), ar = attackR(u);
        if (Gfx.dist(x, y, ax, ay) < ar) {
            player.tryAttack();
            return;
        }
        for (int i = 0; i < 3; i++) {
            float sx = skillX(u, i), sy = skillY(u, i), sr = skillR(u);
            if (Gfx.dist(x, y, sx, sy) < sr) {
                player.castSkill(i);
                return;
            }
        }
        if (Gfx.dist(x, y, dodgeX(u), dodgeY(u)) < 42 * u) {
            player.dodge();
            return;
        }
        if (Gfx.dist(x, y, potionX(u), potionY(u)) < 38 * u) {
            usePotion();
            return;
        }
        if (x < viewW * 0.48f && y > viewH * 0.32f) {
            joyPointer = id;
            joyBaseX = x;
            joyBaseY = y;
            joyDX = 0;
            joyDY = 0;
            joyR = 82 * u;
        }
    }

    public void pointerMove(int id, float x, float y) {
        if (id != joyPointer) return;
        float dx = x - joyBaseX;
        float dy = y - joyBaseY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > joyR) {
            dx = dx / len * joyR;
            dy = dy / len * joyR;
        }
        joyDX = dx;
        joyDY = dy;
    }

    public void pointerUp(int id, float x, float y) {
        if (id == joyPointer) {
            joyPointer = -1;
            joyDX = 0;
            joyDY = 0;
        }
    }

    public void pointerCancel() {
        joyPointer = -1;
        joyDX = 0;
        joyDY = 0;
        keys.clear();
    }

    public boolean mouseAimActive() {
        return mouseAimT > 0;
    }

    public void mouseMoved(float x, float y) {
        aimX = camX + (x - viewW / 2f) / zoom;
        aimY = camY + (y - viewH / 2f) / zoom;
        mouseAimT = 2.8f;
    }

    public void mouseDown(int button, float x, float y) {
        if (finished) return;
        if (puzzleOpen) {
            pointerDown(-200, x, y);
            return;
        }
        float u = ui();
        if (Gfx.dist(x, y, viewW - 34 * u, 34 * u) < 40 * u) {
            pauseRequested = true;
            audio.play("click");
            return;
        }
        float ax = attackX(u), ay = attackY(u), ar = attackR(u);
        if (Gfx.dist(x, y, ax, ay) < ar) {
            player.tryAttack();
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (Gfx.dist(x, y, skillX(u, i), skillY(u, i)) < skillR(u)) {
                player.castSkill(i);
                return;
            }
        }
        if (Gfx.dist(x, y, dodgeX(u), dodgeY(u)) < 42 * u) {
            player.dodge();
            return;
        }
        if (Gfx.dist(x, y, potionX(u), potionY(u)) < 38 * u) {
            usePotion();
            return;
        }
        if (button == 1) {
            mouseMoved(x, y);
            player.tryAttack();
        } else if (button == 2) {
            player.dodge();
        } else if (button == 4) {
            usePotion();
        }
    }

    public void mouseUp(int button, float x, float y) {
    }

    public boolean keyDown(int code) {
        if (finished) return false;
        if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_BACK) {
            pauseRequested = true;
            audio.play("click");
            return true;
        }
        if (isMoveKey(code)) {
            keys.add(code);
            return true;
        }
        if (code == KeyEvent.KEYCODE_SPACE) {
            player.tryAttack();
            return true;
        }
        if (code == KeyEvent.KEYCODE_1) {
            player.castSkill(0);
            return true;
        }
        if (code == KeyEvent.KEYCODE_2) {
            player.castSkill(1);
            return true;
        }
        if (code == KeyEvent.KEYCODE_3) {
            player.castSkill(2);
            return true;
        }
        if (code == KeyEvent.KEYCODE_SHIFT_LEFT || code == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            player.dodge();
            return true;
        }
        if (code == KeyEvent.KEYCODE_Q) {
            usePotion();
            return true;
        }
        return false;
    }

    public void keyUp(int code) {
        keys.remove(code);
    }

    private static boolean isMoveKey(int code) {
        return code == KeyEvent.KEYCODE_W || code == KeyEvent.KEYCODE_A
                || code == KeyEvent.KEYCODE_S || code == KeyEvent.KEYCODE_D
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private float keyX() {
        float x = 0;
        if (keys.contains(KeyEvent.KEYCODE_A) || keys.contains(KeyEvent.KEYCODE_DPAD_LEFT)) x -= 1;
        if (keys.contains(KeyEvent.KEYCODE_D) || keys.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) x += 1;
        return x;
    }

    private float keyY() {
        float y = 0;
        if (keys.contains(KeyEvent.KEYCODE_W) || keys.contains(KeyEvent.KEYCODE_DPAD_UP)) y -= 1;
        if (keys.contains(KeyEvent.KEYCODE_S) || keys.contains(KeyEvent.KEYCODE_DPAD_DOWN)) y += 1;
        return y;
    }

    /** @return true when the pointer was consumed by the pause button. */
    public boolean consumePauseRequest() {
        boolean p = pauseRequested;
        pauseRequested = false;
        return p;
    }

    private void usePotion() {
        if (potionCd > 0) {
            toast("Potion belum siap");
            audio.play("deny");
            return;
        }
        String[] order = {"health_potion", "greater_health", "elixir", "mana_potion", "greater_mana"};
        for (int i = 0; i < order.length; i++) {
            String pid = order[i];
            if (save.count(pid) <= 0) continue;
            Catalog.Item it = Catalog.item(pid);
            if (it == null) continue;
            save.removeItem(pid, 1);
            potionCd = 1.4f;
            if (it.heal > 0) player.heal(it.heal);
            if (it.manaGain > 0) {
                player.addMp(it.manaGain);
                floatText(player.x, player.y - 74, "+" + it.manaGain + " MP", Gfx.BLUE);
            }
            audio.play("heal", 0.8f);
            return;
        }
        toast("Tidak ada potion");
        audio.play("deny");
    }

    private float angDiff(float a, float b) {
        float d = a - b;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    // --------------------------------------------------------------- layout

    private float attackX(float u) {
        return viewW - 108 * u;
    }

    private float attackY(float u) {
        return viewH - 112 * u;
    }

    private float attackR(float u) {
        return 66 * u;
    }

    private float skillR(float u) {
        return 44 * u;
    }

    private float skillX(float u, int i) {
        if (i == 0) return viewW - 250 * u;
        if (i == 1) return viewW - 232 * u;
        return viewW - 286 * u;
    }

    private float skillY(float u, int i) {
        if (i == 0) return viewH - 104 * u;
        if (i == 1) return viewH - 196 * u;
        return viewH - 214 * u;
    }

    private float dodgeX(float u) {
        return viewW - 108 * u;
    }

    private float dodgeY(float u) {
        return viewH - 252 * u;
    }

    private float potionX(float u) {
        return viewW - 336 * u;
    }

    private float potionY(float u) {
        return viewH - 118 * u;
    }

    public float puzzleScale() {
        return Gfx.clamp(viewH / 720f, 0.7f, 2.2f);
    }

    // ----------------------------------------------------------------- draw

    public void draw(Canvas c, Gfx g) {
        float halfW = viewW / (2f * zoom);
        float halfH = viewH / (2f * zoom);

        int baseSave = c.getSaveCount();
        c.save();
        c.translate(viewW / 2f, viewH / 2f);
        if (shakeAmt > 0) {
            c.translate((rnd.nextFloat() - 0.5f) * shakeAmt, (rnd.nextFloat() - 0.5f) * shakeAmt);
        }
        c.scale(zoom, zoom);
        c.translate(-camX, -camY);

        world.drawGround(c, g, camX, camY, halfW * 2f, halfH * 2f, time, save.highQuality);
        world.drawObstacles(c, g, time, save.highQuality);
        drawChestShrine(c, g);
        drawPickups(c, g);
        drawBlasts(c, g);
        drawProjectiles(c, g);

        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).draw(c, g, time);
        }
        drawRemoteAlly(c, g);
        if (!player.dead) player.draw(c, g);
        drawEffects(c, g);

        while (c.getSaveCount() > baseSave) c.restore();

        g.vignette(c, viewW / 2f, viewH / 2f, (float) Math.hypot(viewW, viewH) * 0.62f);

        if (!puzzleOpen) {
            drawHUD(c, g);
            drawToasts(c, g);
        } else {
            drawPuzzle(c, g);
        }
    }

    private void drawRemoteAlly(Canvas c, Gfx g) {
        if (lan == null || !lan.connected) return;
        List<LanCoop.Remote> allies = lan.remotes();
        for (int i = 0; i < allies.size(); i++) {
            LanCoop.Remote r = allies.get(i);
            if (r.self || r.stage != stageIndex) continue;
            float rx = r.x, ry = r.y;
            float bob = r.moving ? (float) Math.sin(time * 11f) * 2.2f : 0f;
            Catalog.ClassDef cd = Catalog.CLASSES[Gfx.clamp(r.classIndex, 0, 2)];
            g.circle(c, rx, ry + 20, 25, 0x44000000);
            c.save();
            c.translate(0, -4f);
            g.roundRect(c, rx - 15, ry - 28 + bob, 30, 42, 10, Gfx.withAlpha(cd.color, 210));
            g.circle(c, rx, ry - 36 + bob, 13, 0xFFE8D5C0);
            g.circle(c, rx + (float) Math.cos(r.dir) * 5, ry - 37 + bob,
                    3.2f, 0xFF2A2038);
            g.circleStroke(c, rx, ry - 28 + bob, 20, Gfx.withAlpha(cd.color, 90), 2);
            String name = r.name == null || r.name.length() == 0 ? "Partner" : r.name;
            g.textShadow(c, name, rx, ry - 62 + bob, 12, Gfx.CYAN, 1, true);
            float hp = Gfx.clamp(r.hp / Math.max(1f, r.maxHp), 0f, 1f);
            g.bar(c, rx - 26, ry - 54 + bob, 52, 6, hp, 0xFF3A1420, Gfx.GREEN);
            c.restore();
        }
    }

    private void drawChestShrine(Canvas c, Gfx g) {        float t = time;
        // chest
        float cx = world.chestX, cy = world.chestY;
        if (!world.chestOpen) {
            g.circle(c, cx, cy + 14, 26, 0x33000000);
            g.roundRect(c, cx - 26, cy - 16, 52, 40, 7, 0xFF8A5A2B);
            g.roundRect(c, cx - 26, cy - 16, 52, 14, 7, 0xFFB07A3F);
            g.line(c, cx - 22, cy - 2, cx + 22, cy - 2, 0xFF5C3A17, 3);
            g.roundRect(c, cx - 6, cy - 5, 12, 12, 3, Gfx.GOLD);
            g.circleStroke(c, cx, cy + 2, 34 + (float) Math.sin(t * 2) * 3, Gfx.withAlpha(Gfx.GOLD, 60), 2);
        } else {
            g.roundRect(c, cx - 26, cy - 12, 52, 36, 7, 0x664A3320);
            g.roundRect(c, cx - 26, cy - 12, 52, 10, 5, 0x66795A36);
        }
        // shrine
        float sx = world.shrineX, sy = world.shrineY;
        g.circle(c, sx, sy + 34, 46, 0x44000000);
        g.roundRect(c, sx - 44, sy + 6, 88, 44, 10, 0xFF2B2440);
        g.roundRect(c, sx - 52, sy + 40, 104, 18, 8, 0xFF1C1830);
        g.roundRectStroke(c, sx - 44, sy + 6, 88, 44, 10, 0xFF4A3E6E, 2);
        boolean active = remainingEnemies() == 0 && !puzzleResolved && !clearQueued;
        int orbCol = puzzleResolved ? Gfx.GREEN : (active ? Gfx.GOLD : 0xFF6E5E9E);
        float bob = (float) Math.sin(t * 2.4f) * 5;
        if (active) {
            g.circle(c, sx, sy - 18 + bob, 52 + (float) Math.sin(t * 3) * 6, Gfx.withAlpha(orbCol, 40));
        }
        g.circle(c, sx, sy - 18 + bob, 20, orbCol);
        g.circle(c, sx - 6, sy - 24 + bob, 7, 0xAAFFFFFF);
        g.circleStroke(c, sx, sy - 18 + bob, 30, Gfx.withAlpha(orbCol, 140), 2);
        if (active) {
            g.textShadow(c, "ALTAR", sx, sy - 78 + bob, 15, Gfx.GOLD, 1, true);
        }
    }

    private void drawPickups(Canvas c, Gfx g) {
        for (int i = 0; i < pickups.size(); i++) {
            Pickup p = pickups.get(i);
            float t = p.t;
            float bob = (float) Math.sin(t * 5f + p.x) * 3;
            if (p.type == 0) {
                g.circle(c, p.x, p.y + bob, 9, Gfx.GOLD);
                g.circle(c, p.x - 2.5f, p.y - 3 + bob, 3, 0xCCFFF6D0);
            } else if (p.type == 1) {
                g.circle(c, p.x, p.y + bob, 14, Gfx.withAlpha(Gfx.CYAN, 50));
                c.save();
                c.rotate(45, p.x, p.y + bob);
                c.drawRect(p.x - 6, p.y - 6 + bob, p.x + 6, p.y + 6 + bob, g.p);
                c.restore();
                g.fill(Gfx.CYAN);
                c.save();
                c.rotate(45, p.x, p.y + bob);
                c.drawRect(p.x - 6, p.y - 6 + bob, p.x + 6, p.y + 6 + bob, g.p);
                c.restore();
            } else {
                g.circle(c, p.x, p.y + bob, 11, Gfx.withAlpha(Gfx.GREEN, 55));
                g.circle(c, p.x - 4, p.y - 3 + bob, 5, 0xFFFF8FA0);
                g.circle(c, p.x + 4, p.y - 3 + bob, 5, 0xFFFF8FA0);
                g.circle(c, p.x, p.y + 3 + bob, 5, 0xFFFF8FA0);
            }
        }
    }

    private void drawBlasts(Canvas c, Gfx g) {
        for (int i = 0; i < blasts.size(); i++) {
            Blast b = blasts.get(i);
            float k = 1f - Gfx.clamp(b.delay / Math.max(0.01f, b.maxDelay), 0f, 1f);
            g.circle(c, b.x, b.y, b.r, Gfx.withAlpha(b.enemy ? 0xFFFF3B3B : b.color, (int) (36 + 40 * k)));
            g.circleStroke(c, b.x, b.y, b.r, Gfx.withAlpha(b.enemy ? 0xFFFF5555 : b.color, 190), 3);
            g.circleStroke(c, b.x, b.y, b.r * k, Gfx.withAlpha(0xFFFFFFFF, 150), 2);
        }
    }

    private void drawProjectiles(Canvas c, Gfx g) {
        for (int i = 0; i < projectiles.size(); i++) {
            Proj p = projectiles.get(i);
            float r = p.radius * (p.enemy ? 1.1f : 1f);
            int col = p.kind == 2 ? 0xFF7FE08A : p.color;
            g.circle(c, p.x, p.y, r * 2.1f, Gfx.withAlpha(col, 40));
            g.circle(c, p.x, p.y, r, col);
            g.circle(c, p.x - r * 0.3f, p.y - r * 0.3f, r * 0.35f, 0xCCFFFFFF);
            float tx = p.x - (float) Math.cos(p.dir) * r * 3f;
            float ty = p.y - (float) Math.sin(p.dir) * r * 3f;
            g.line(c, p.x, p.y, tx, ty, Gfx.withAlpha(col, 90), r * 1.2f);
        }
    }

    private void drawEffects(Canvas c, Gfx g) {
        for (int i = 0; i < trails.size(); i++) {
            Trail t = trails.get(i);
            float a = t.life / t.maxLife;
            g.line(c, t.x1, t.y1, t.x2, t.y2, Gfx.withAlpha(t.color, (int) (130 * a)), 10 * a + 1);
        }
        for (int i = 0; i < dust.size(); i++) {
            Dust d = dust.get(i);
            float a = d.life / d.maxLife;
            g.circle(c, d.x, d.y, d.r * (1.4f - a * 0.4f), Gfx.withAlpha(d.color, (int) (120 * a)));
        }
        for (int i = 0; i < slashes.size(); i++) {
            Slash s = slashes.get(i);
            float a = s.life / s.maxLife;
            float deg = (float) Math.toDegrees(s.dir);
            float half = 56f;
            arcRect.set(s.x - s.range, s.y - s.range, s.x + s.range, s.y + s.range);
            g.stroke(Gfx.withAlpha(s.color, (int) (220 * a)), 8 * a + 2);
            c.drawArc(arcRect, deg - half, half * 2f, false, g.ps);
            g.stroke(Gfx.withAlpha(0xFFFFFFFF, (int) (120 * a)), 3);
            c.drawArc(arcRect, deg - half * 0.7f, half * 1.4f, false, g.ps);
        }
        for (int i = 0; i < rings.size(); i++) {
            Ring r = rings.get(i);
            float a = r.life / r.maxLife;
            float rr = r.r * (1.25f - a * 0.25f);
            g.circleStroke(c, r.x, r.y, rr, Gfx.withAlpha(r.color, (int) (200 * a)), 4 * a + 1);
        }
        for (int i = 0; i < booms.size(); i++) {
            Boom b = booms.get(i);
            float a = b.life / b.maxLife;
            float rr = b.r * (1.15f - a * 0.3f);
            g.circle(c, b.x, b.y, rr, Gfx.withAlpha(b.color, (int) (70 * a)));
            g.circle(c, b.x, b.y, rr * 0.6f, Gfx.withAlpha(0xFFFFFFFF, (int) (60 * a)));
            g.circleStroke(c, b.x, b.y, rr, Gfx.withAlpha(b.color, (int) (230 * a)), 5 * a + 1);
        }
        for (int i = 0; i < hits.size(); i++) {
            HitFx h = hits.get(i);
            float a = h.life / h.maxLife;
            g.circle(c, h.x, h.y, 16 * (1.2f - a * 0.2f), Gfx.withAlpha(0xFFFFFFFF, (int) (150 * a)));
            g.line(c, h.x - 18, h.y - 18, h.x + 18, h.y + 18, Gfx.withAlpha(h.color, (int) (220 * a)), 4);
            g.line(c, h.x - 18, h.y + 18, h.x + 18, h.y - 18, Gfx.withAlpha(h.color, (int) (220 * a)), 4);
        }
        for (int i = 0; i < texts.size(); i++) {
            Txt t = texts.get(i);
            float a = t.life / t.maxLife;
            g.textShadow(c, t.text, t.x, t.y, 15, Gfx.withAlpha(t.color, (int) (255 * Math.min(1f, a * 1.6f))), 1, true);
        }
    }

    private void drawHUD(Canvas c, Gfx g) {
        float u = ui();
        float t = time;
        if (lan != null) {
            StringBuilder partners = new StringBuilder();
            List<LanCoop.Remote> allies = lan.remotes();
            for (int i = 0; i < allies.size(); i++) {
                LanCoop.Remote r = allies.get(i);
                if (r.self) continue;
                if (partners.length() > 0) partners.append(", ");
                partners.append(r.name == null || r.name.length() == 0 ? "Partner" : r.name);
            }
            String partner = partners.length() == 0 ? "menunggu partner" : partners.toString();
            g.textShadow(c, "CO-OP  •  " + partner, viewW / 2f, 20 * u, 12 * u,
                    lan.connected ? Gfx.CYAN : Gfx.ORANGE, 1, true);
        }

        // top left status
        float px = 16 * u, py = 14 * u;
        g.panel(c, px, py, 268 * u, 92 * u, 0xCC150F24, 0x556B5A96);
        Catalog.ClassDef cd = Catalog.CLASSES[save.classIndex];
        g.text(c, cd.name, px + 14 * u, py + 20 * u, 15 * u, cd.color, 0, true);
        g.text(c, "Lv " + save.level, px + 254 * u, py + 20 * u, 14 * u, Gfx.GOLD, 2, true);
        g.bar(c, px + 14 * u, py + 34 * u, 240 * u, 16 * u, player.hp / Math.max(1f, player.maxHp), 0xFF3A1420, Gfx.RED);
        g.bar(c, px + 14 * u, py + 56 * u, 240 * u, 12 * u, player.mp / Math.max(1f, player.maxMp), 0xFF14213A, Gfx.BLUE);
        g.text(c, (int) player.hp + " / " + (int) player.maxHp, px + 134 * u, py + 34 * u, 11 * u, Gfx.TEXT, 1, true);
        g.text(c, (int) player.mp + " / " + (int) player.maxMp, px + 134 * u, py + 56 * u, 10 * u, Gfx.TEXT, 1, true);
        float buffY = py + 76 * u;
        String buff = player.buffTimer > 0 ? "ATK+" : "";
        g.text(c, buff, px + 14 * u, buffY, 11 * u, Gfx.ORANGE, 0, true);

        // minimap
        float mw = 200 * u, mh = 126 * u;
        float mx = viewW - mw - 16 * u, my = 60 * u;
        g.panel(c, mx, my, mw, mh, 0xB3150F24, 0x556B5A96);
        float sc = Math.min((mw - 14 * u) / world.w, (mh - 14 * u) / world.h);
        float ox = mx + 7 * u, oy = my + 7 * u;
        g.roundRect(c, ox, oy, world.w * sc, world.h * sc, 4, 0x332E2444);
        g.circle(c, ox + world.chestX * sc, oy + world.chestY * sc, 3.2f * u,
                world.chestOpen ? 0x666A5A44 : Gfx.ORANGE);
        boolean shrineActive = remainingEnemies() == 0 && !puzzleResolved;
        g.circle(c, ox + world.shrineX * sc, oy + world.shrineY * sc, 4 * u,
                puzzleResolved ? Gfx.GREEN : (shrineActive ? Gfx.GOLD : 0xFF7A6AA6));
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead) continue;
            g.circle(c, ox + e.x * sc, oy + e.y * sc, (e.boss ? 4f : 2.2f) * u, e.boss ? 0xFFFF5A5A : Gfx.RED);
        }
        g.circle(c, ox + player.x * sc, oy + player.y * sc, 3.6f * u, Gfx.CYAN);
        g.circleStroke(c, ox + player.x * sc, oy + player.y * sc, 5.5f * u, Gfx.withAlpha(Gfx.CYAN, 120), 1.2f * u);

        // currencies
        g.text(c, "◆ " + save.shards, viewW - 16 * u, my + mh + 16 * u, 14 * u, Gfx.CYAN, 2, true);
        g.text(c, "● " + save.gold, viewW - 96 * u, my + mh + 16 * u, 14 * u, Gfx.GOLD, 2, true);

        // pause
        float pbx = viewW - 34 * u, pby = 34 * u;
        g.circle(c, pbx, pby, 26 * u, 0xAA201831);
        g.circleStroke(c, pbx, pby, 26 * u, 0x666B5A96, 1.5f * u);
        g.roundRect(c, pbx - 8 * u, pby - 9 * u, 6 * u, 18 * u, 2 * u, Gfx.TEXT);
        g.roundRect(c, pbx + 2 * u, pby - 9 * u, 6 * u, 18 * u, 2 * u, Gfx.TEXT);

        // objective
        String obj;
        if (boss != null && !boss.dead) obj = "Kalahkan " + boss.name;
        else if (clearQueued) obj = "Stage selesai...";
        else if (puzzleResolved && bossSpawned) obj = "Jalan menuju puncak terbuka";
        else if (remainingEnemies() > 0) obj = "Musuh tersisa: " + remainingEnemies();
        else if (!puzzleResolved) obj = "Altar terbuka";
        else obj = "Bersiap...";
        float ow = g.textWidth(obj, 16 * u, true) + 40 * u;
        g.panel(c, viewW / 2f - ow / 2f, 12 * u, ow, 30 * u, 0xAA150F24, 0x446B5A96);
        g.text(c, obj, viewW / 2f, 27 * u, 16 * u, Gfx.GOLD, 1, true);

        // boss bar
        if (boss != null && !boss.dead) {
            float bw = Math.min(viewW * 0.52f, 560 * u);
            float bx = viewW / 2f - bw / 2f, by = 52 * u;
            g.textShadow(c, boss.name, viewW / 2f, by - 10 * u, 19 * u, Gfx.TEXT, 1, true);
            g.bar(c, bx, by, bw, 16 * u, boss.hp / Math.max(1f, boss.maxHp), 0xFF2A0E14, 0xFFE5484D);
            g.roundRectStroke(c, bx, by, bw, 16 * u, 8 * u, 0x88FFFFFF, 1);
        }

        drawControls(c, g, u);

        // joystick
        if (joyPointer >= 0) {
            g.circle(c, joyBaseX, joyBaseY, joyR, 0x331C1533);
            g.circleStroke(c, joyBaseX, joyBaseY, joyR, 0x668A7AB8, 2 * u);
            g.circle(c, joyBaseX + joyDX, joyBaseY + joyDY, joyR * 0.42f, 0xCC9B5DE5);
            g.circleStroke(c, joyBaseX + joyDX, joyBaseY + joyDY, joyR * 0.42f, 0xFFFFFFFF, 1.5f * u);
        }

        // nearby chest prompt
        if (!world.chestOpen && Gfx.dist(player.x, player.y, world.chestX, world.chestY) < 160f) {
            g.textShadow(c, "Peti di dekatmu", player.x, player.y - 110, 15, Gfx.GOLD, 1, true);
        }
    }

    private void drawControls(Canvas c, Gfx g, float u) {
        int cls = save.classIndex;
        Catalog.Skill[] sk = Catalog.SKILLS[cls];

        // attack
        float ax = attackX(u), ay = attackY(u), ar = attackR(u);
        g.circle(c, ax, ay, ar + 5 * u, 0x33000000);
        g.circle(c, ax, ay, ar, player.attackCd > 0 ? 0xCC3A2A4A : 0xEEE0A64B);
        g.circleStroke(c, ax, ay, ar, 0xFFFFE8B0, 2.5f * u);
        g.line(c, ax - ar * 0.4f, ay + ar * 0.4f, ax + ar * 0.42f, ay - ar * 0.42f, 0xFF2A1A08, 7 * u);
        g.line(c, ax - ar * 0.12f, ay + ar * 0.05f, ax + ar * 0.18f, ay + ar * 0.35f, 0xFF2A1A08, 5 * u);
        g.text(c, "ATK", ax, ay + ar * 0.66f, 10 * u, 0xFF2A1A08, 1, true);

        // skills
        for (int i = 0; i < 3; i++) {
            Catalog.Skill s = sk[i];
            float sx = skillX(u, i), sy = skillY(u, i), sr = skillR(u);
            boolean ready = player.cd[i] <= 0 && player.mp >= s.mp;
            g.circle(c, sx, sy, sr + 4 * u, 0x33000000);
            g.circle(c, sx, sy, sr, ready ? 0xEE211A38 : 0xBB1A1428);
            g.circleStroke(c, sx, sy, sr, Gfx.withAlpha(s.color, ready ? 230 : 90), 2.2f * u);
            g.text(c, (i + 1) + "", sx, sy - 2 * u, 17 * u, ready ? s.color : Gfx.DIM, 1, true);
            if (player.cd[i] > 0) {
                g.circle(c, sx, sy, sr, 0x99201018);
                g.text(c, String.valueOf(Math.max(1, (int) Math.ceil(player.cd[i]))), sx, sy, 18 * u, Gfx.TEXT, 1, true);
            } else {
                g.text(c, s.name.split(" ")[0], sx, sy + sr * 0.62f, 8.5f * u, Gfx.DIM, 1, true);
            }
        }

        // dodge
        float dx = dodgeX(u), dy = dodgeY(u);
        boolean dodgeReady = player.dodgeCd <= 0;
        g.circle(c, dx, dy, 38 * u, dodgeReady ? 0xDD1C3340 : 0xAA161C28);
        g.circleStroke(c, dx, dy, 38 * u, dodgeReady ? Gfx.CYAN : 0x5555AAAA, 2 * u);
        g.line(c, dx - 13 * u, dy + 4 * u, dx + 5 * u, dy - 10 * u, dodgeReady ? Gfx.CYAN : Gfx.DIM, 5 * u);
        g.line(c, dx + 2 * u, dy + 3 * u, dx + 8 * u, dy + 3 * u, dodgeReady ? Gfx.CYAN : Gfx.DIM, 5 * u);
        if (!dodgeReady) g.text(c, String.valueOf(Math.max(1, (int) Math.ceil(player.dodgeCd))), dx, dy + 2 * u, 16 * u, Gfx.TEXT, 1, true);

        // potion
        float px = potionX(u), py = potionY(u);
        int potions = save.count("health_potion") + save.count("greater_health") + save.count("elixir")
                + save.count("mana_potion") + save.count("greater_mana");
        g.circle(c, px, py, 34 * u, potions > 0 ? 0xDD2A1830 : 0xAA1A1424);
        g.circleStroke(c, px, py, 34 * u, potions > 0 ? 0xFFE0679A : 0x55443344, 2 * u);
        g.roundRect(c, px - 8 * u, py - 14 * u, 16 * u, 28 * u, 6 * u, potions > 0 ? 0xFFE0679A : 0xFF554455);
        g.roundRect(c, px - 4 * u, py - 20 * u, 8 * u, 8 * u, 2 * u, 0xFFD8CBB8);
        g.text(c, "" + potions, px + 22 * u, py + 20 * u, 13 * u, Gfx.TEXT, 2, true);
        if (potionCd > 0) g.text(c, String.valueOf(Math.max(1, (int) Math.ceil(potionCd))), px, py + 2 * u, 15 * u, Gfx.TEXT, 1, true);
    }

    private void drawToasts(Canvas c, Gfx g) {
        float u = ui();
        float y = viewH - 190 * u;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            float a = Gfx.clamp(t.life / 0.6f, 0f, 1f);
            float w = g.textWidth(t.msg, 15 * u, true) + 34 * u;
            g.roundRect(c, viewW / 2f - w / 2f, y - 16 * u, w, 30 * u, 15 * u, Gfx.withAlpha(0xCC150F24, (int) (200 * a)));
            g.text(c, t.msg, viewW / 2f, y, 15 * u, Gfx.withAlpha(Gfx.TEXT, (int) (255 * a)), 1, true);
            y -= 36 * u;
        }
    }

    private void drawPuzzle(Canvas c, Gfx g) {
        float s = puzzleScale();
        g.fill(0xAA000000);
        c.drawRect(0, 0, viewW, viewH, g.p);
        c.save();
        c.scale(s, s);
        if (puzzle != null) puzzle.draw(c, g, (int) (viewW / s), (int) (viewH / s));
        c.restore();
    }

    public int remainingEnemies() {
        int n = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (!enemies.get(i).dead) n++;
        }
        return n;
    }
}
