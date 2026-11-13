package com.mysticrealm;

import android.graphics.Canvas;
import android.graphics.Path;

import java.util.Random;

public class Enemy {
    public final Gameplay gp;
    public int netId;
    public boolean remote;
    public String name;
    public int type;
    public int color;
    public float x, y, dir;
    public float hp, maxHp, atk, def, spd;
    public float radius;
    public float scale;
    public boolean boss, elite, ranged, dead;
    public float exp, gold;
    public int world;
    public int bossIndex;

    public float spawnT = 0.4f;
    public float hitFlash;
    public float stun;
    public float slow;
    public float poison;
    public float poisonTick;
    public float poisonDmg;
    public float kbx, kby;
    public float atkCd;
    public float shootCd;
    public float wanderT;
    public float wanderDir;
    public float abilityCd;
    public float dashT;
    public float dashDir;
    public boolean dashHit;
    public float deathT;
    public int phase = 1;
    public boolean enraged;
    public float time;
    public float eliteCd;
    public float attackAnim;

    private final Random rnd = new Random();

    public interface Target {
        float x();
        float y();
        float radius();
        String uid();
        void hurt(float amount, Enemy src);
    }

    public Enemy(Gameplay gp) {
        this.gp = gp;
    }

    public Enemy(Gameplay gp, Catalog.MonsterDef d, float x, float y, float hpMul, float atkMul,
                 boolean elite, int world) {
        this.gp = gp;
        this.type = d.type;
        this.color = d.color;
        this.x = x;
        this.y = y;
        this.world = world;
        this.elite = elite;
        this.ranged = d.ranged;
        this.radius = d.radius * (elite ? 1.15f : 1f);
        this.scale = d.scale * (elite ? 1.16f : 1f);
        this.hp = d.hp * hpMul * (elite ? 1.95f : 1f);
        this.maxHp = hp;
        this.atk = d.atk * atkMul * (elite ? 1.3f : 1f);
        this.def = d.def * (elite ? 1.3f : 1f);
        this.spd = d.spd * (elite ? 1.06f : 1f);
        this.exp = d.exp;
        this.gold = d.gold;
        this.name = elite ? "Elite " + d.name : d.name;
        this.dir = rnd.nextFloat() * 6.28f;
        this.wanderDir = dir;
        this.atkCd = 0.6f + rnd.nextFloat() * 0.5f;
        this.shootCd = 1f + rnd.nextFloat();
        this.abilityCd = 2.4f;
        this.eliteCd = 2f;
    }

    public Enemy(Gameplay gp, Catalog.BossDef d, float x, float y, int world, float hpMul, float atkMul) {
        this.gp = gp;
        this.type = 10 + world;
        this.bossIndex = world;
        this.color = d.color;
        this.x = x;
        this.y = y;
        this.world = world;
        this.boss = true;
        this.radius = 58f;
        this.scale = d.scale;
        this.hp = d.hp * hpMul;
        this.maxHp = hp;
        this.atk = d.atk * atkMul;
        this.def = d.def;
        this.spd = d.spd;
        this.exp = 260 + world * 140;
        this.gold = 220 + world * 120;
        this.name = d.name;
        this.dir = 1.5708f;
        this.atkCd = 1.4f;
        this.abilityCd = 2.6f;
    }

    public boolean isDead() {
        return dead;
    }

    public void applyStun(float dur) {
        if (boss) dur *= 0.35f;
        if (elite) dur *= 0.7f;
        if (dur > stun) stun = dur;
    }

    public void applySlow(float dur) {
        if (boss) dur *= 0.5f;
        if (dur > slow) slow = dur;
    }

    public void damage(float dmg, float critChance, float kbx, float kby, boolean poisonHit, float dotDmg) {
        if (dead) return;
        boolean crit = rnd.nextFloat() < critChance;
        float taken = dmg - def * 0.6f;
        if (taken < 1f) taken = 1f;
        if (crit) taken *= 1.8f;
        hp -= taken;
        hitFlash = 0.16f;
        if (!boss) {
            float kbRes = elite ? 0.35f : 0.65f;
            this.kbx += kbx * kbRes;
            this.kby += kby * kbRes;
        }
        gp.floatText(x + (rnd.nextFloat() - 0.5f) * 18, y - radius - 12,
                (crit ? "CRIT " : "") + (int) taken, crit ? Gfx.GOLD : 0xFFFFE8D0);
        gp.fxHit(x, y - radius * 0.3f, crit ? Gfx.GOLD : 0xFFFFB4A0);
        if (poisonHit) {
            poison = Math.max(poison, 3f);
            poisonDmg = Math.max(poisonDmg, dotDmg);
        }
        if (rnd.nextFloat() < 0.4f) gp.audio.play("hit", 0.35f);
        if (hp <= 0) {
            hp = 0;
            die();
        }
    }

    private void die() {
        if (dead) return;
        dead = true;
        deathT = 0;
        gp.onEnemyKilled(this);
    }

    public void update(float dt, Target t) {
        time += dt;
        if (dead) {
            deathT += dt;
            return;
        }
        if (remote) {
            if (hitFlash > 0) hitFlash -= dt;
            if (spawnT > 0) spawnT -= dt;
            return;
        }
        if (t == null) return;
        if (spawnT > 0) {
            spawnT -= dt;
            return;
        }
        if (hitFlash > 0) hitFlash -= dt;
        if (attackAnim > 0) attackAnim -= dt;
        if (poison > 0) {
            poison -= dt;
            poisonTick -= dt;
            if (poisonTick <= 0) {
                poisonTick = 0.5f;
                float d = Math.max(2f, poisonDmg * 0.16f);
                hp -= d;
                gp.floatText(x, y - radius - 8, (int) d + "", 0xFF7FE08A);
                if (hp <= 0) {
                    hp = 0;
                    die();
                    return;
                }
            }
        }
        float kbDecay = (float) Math.pow(0.001f, dt);
        x += kbx * dt;
        y += kby * dt;
        kbx *= kbDecay;
        kby *= kbDecay;

        if (stun > 0) {
            stun -= dt;
            gp.world.resolve(this, radius);
            return;
        }
        if (slow > 0) slow -= dt;
        if (atkCd > 0) atkCd -= dt;
        if (shootCd > 0) shootCd -= dt;
        if (eliteCd > 0) eliteCd -= dt;

        if (!enraged && hp < maxHp * 0.28f) {
            enraged = true;
            atk *= 1.2f;
            spd *= 1.18f;
            gp.fxRing(x, y, radius + 30, 0xFFFF4D4D, 0.5f);
            if (boss) gp.toast(name + " mengamuk!");
        }

        float dx = t.x() - x;
        float dy = t.y() - y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);

        if (boss) {
            updateBoss(dt, t, d, dx, dy);
        } else if (ranged) {
            updateRanged(dt, t, d, dx, dy);
        } else {
            updateMelee(dt, t, d, dx, dy);
        }

        if (elite && !boss && eliteCd <= 0 && d < 520 && d > 90) {
            eliteCd = 4.2f;
            dashT = 0.2f;
            dashDir = (float) Math.atan2(dy, dx);
            dashHit = false;
            gp.fxRing(x, y, radius + 12, Gfx.GOLD, 0.25f);
        }
        if (dashT > 0) {
            dashT -= dt;
            float sp = spd * 4.2f * (slow > 0 ? 0.55f : 1f);
            x += (float) Math.cos(dashDir) * sp * dt;
            y += (float) Math.sin(dashDir) * sp * dt;
            if (!dashHit && d < radius + t.radius() + 18) {
                dashHit = true;
                t.hurt(atk * 1.1f, this);
            }
        }

        gp.world.resolve(this, radius);
        separate();
    }

    private void separate() {
        for (Enemy e : gp.enemies) {
            if (e == this || e.dead) continue;
            float dx = e.x - x;
            float dy = e.y - y;
            float min = (radius + e.radius) * 0.8f;
            float d2 = dx * dx + dy * dy;
            if (d2 < min * min && d2 > 0.01f) {
                float d = (float) Math.sqrt(d2);
                float push = (min - d) * 0.5f;
                x -= dx / d * push;
                y -= dy / d * push;
            }
        }
    }

    private void updateMelee(float dt, Target t, float d, float dx, float dy) {
        boolean aggro = d < 640 || hitFlash > 0;
        if (aggro) {
            if (d > radius + t.radius() + 6) {
                float sp = spd * 1.9f * (slow > 0 ? 0.5f : 1f) * (enraged ? 1.15f : 1f);
                dir = (float) Math.atan2(dy, dx);
                x += dx / d * sp * dt;
                y += dy / d * sp * dt;
            } else if (atkCd <= 0) {
                atkCd = 1.15f + rnd.nextFloat() * 0.3f;
                dir = (float) Math.atan2(dy, dx);
                t.hurt(atk * (0.9f + rnd.nextFloat() * 0.25f), this);
                attackAnim = 0.24f;
                gp.fxSlash(x, y, dir, radius + 34, color);
            }
        } else {
            wanderT -= dt;
            if (wanderT <= 0) {
                wanderT = 1.2f + rnd.nextFloat() * 2.4f;
                wanderDir = rnd.nextFloat() * 6.28f;
            }
            float sp = spd * 0.5f;
            x += (float) Math.cos(wanderDir) * sp * dt;
            y += (float) Math.sin(wanderDir) * sp * dt;
            dir = wanderDir;
        }
    }

    private void updateRanged(float dt, Target t, float d, float dx, float dy) {
        if (d < 660) {
            float desired = 290f;
            float ang = (float) Math.atan2(dy, dx);
            dir = ang;
            if (d > desired + 90) {
                float sp = spd * 1.7f * (slow > 0 ? 0.5f : 1f);
                x += dx / d * sp * dt;
                y += dy / d * sp * dt;
            } else if (d < desired - 80) {
                float sp = spd * 1.5f * (slow > 0 ? 0.5f : 1f);
                x -= dx / d * sp * dt;
                y -= dy / d * sp * dt;
            } else {
                float strafe = (time * 0.7f + (float) Math.sin(time)) > 0 ? 1f : -1f;
                float sx = -(float) Math.sin(ang) * strafe;
                float sy = (float) Math.cos(ang) * strafe;
                float sp = spd * 0.9f * (slow > 0 ? 0.5f : 1f);
                x += sx * sp * dt;
                y += sy * sp * dt;
            }
            if (shootCd <= 0 && d < 600) {
                shootCd = 2.1f + rnd.nextFloat() * 0.8f;
                float tx = t.x();
                float ty = t.y();
                float sa = (float) Math.atan2(ty - y, tx - x);
                attackAnim = 0.2f;
                gp.spawnEnemyProjectile(x + (float) Math.cos(sa) * radius, y + (float) Math.sin(sa) * radius,
                        sa, 300f, atk * 0.95f, color, 9f, 0, t.uid());
                gp.audio.play("attack", 0.25f);
            }
        } else {
            wanderT -= dt;
            if (wanderT <= 0) {
                wanderT = 1.5f + rnd.nextFloat() * 2f;
                wanderDir = rnd.nextFloat() * 6.28f;
            }
            float sp = spd * 0.4f;
            x += (float) Math.cos(wanderDir) * sp * dt;
            y += (float) Math.sin(wanderDir) * sp * dt;
            dir = wanderDir;
        }
    }

    private void updateBoss(float dt, Target t, float d, float dx, float dy) {
        if (phase == 1 && hp < maxHp * 0.5f) {
            phase = 2;
            atk *= 1.25f;
            spd *= 1.2f;
            abilityCd = 1.2f;
            gp.toast(name + " memasuki fase kedua!");
            gp.fxExplosion(x, y, 260, color);
            gp.shake(16);
            gp.audio.play("boss");
        }
        float ang = (float) Math.atan2(dy, dx);
        dir = ang;
        if (dashT <= 0) {
            if (d > 190) {
                float sp = spd * 1.6f * (slow > 0 ? 0.5f : 1f);
                x += dx / d * sp * dt;
                y += dy / d * sp * dt;
            } else if (d <= radius + t.radius() + 55 && atkCd <= 0) {
                atkCd = 1.3f;
                t.hurt(atk * 1.05f, this);
                attackAnim = 0.26f;
                gp.fxSlash(x, y, ang, radius + 70, color);
            }
        }
        if (abilityCd > 0) {
            abilityCd -= dt;
            return;
        }
        abilityCd = phase == 1 ? 3.1f : 2.2f;
        int ability = rnd.nextInt(4);
        if (d > 480) ability = rnd.nextBoolean() ? 1 : 2;
        switch (ability) {
            case 0: {
                gp.spawnEnemyBlast(t.x(), t.y(), 135, 0.85f, atk * 1.5f, color, t.uid());
                gp.toast(name + " menghantam tanah!");
                break;
            }
            case 1: {
                int count = phase == 1 ? 10 : 14;
                float base = rnd.nextFloat() * 6.28f;
                for (int i = 0; i < count; i++) {
                    float a = base + i * (6.283f / count);
                    gp.spawnEnemyProjectile(x, y, a, 260f + world * 12, atk * 0.75f, color, 12f, 1, null);
                }
                gp.audio.play("skill", 0.6f);
                break;
            }
            case 2: {
                dashT = 0.5f;
                dashHit = false;
                dashDir = ang;
                gp.toast(name + " menerjang!");
                gp.audio.play("boss", 0.5f);
                break;
            }
            default: {
                int n = phase == 1 ? 2 : 3;
                gp.summonMinions(x, y, n);
                gp.fxRing(x, y, 220, color, 0.5f);
                gp.toast(name + " memanggil bawahannya!");
                break;
            }
        }
    }

    public void draw(Canvas c, Gfx g, float globalTime) {
        if (dead) {
            float t = deathT / 0.45f;
            if (t >= 1f) return;
            g.circle(c, x, y - radius * 0.2f, radius * (0.8f + t * 0.9f), Gfx.withAlpha(color, (int) (140 * (1 - t))));
            g.circleStroke(c, x, y - radius * 0.2f, radius * (0.9f + t * 1.2f), Gfx.withAlpha(0xFFFFFFFF, (int) (160 * (1 - t))), 2);
            return;
        }
        float sp = Gfx.clamp(1f - spawnT / 0.4f, 0.25f, 1f);
        float sc = scale * sp;
        float bobAmount = (type == 7 || type == 25 || type == 28) ? 6f : (type == 0 || type == 5 ? 4f : 2f);
        float bob = (float) Math.sin(time * (type == 7 ? 2.6f : 4f) + x * 0.01f) * bobAmount;
        g.fill(0x44000000);
        c.drawOval(x - radius, y + radius * 0.55f, x + radius, y + radius * 0.95f, g.p);

        if (boss || elite) {
            g.circleStroke(c, x, y, radius + 14 + (float) Math.sin(time * 4) * 3,
                    Gfx.withAlpha(boss ? color : Gfx.GOLD, 110), boss ? 3 : 2);
        }
        if (enraged) {
            g.circleStroke(c, x, y, radius + 8, 0x66FF4D4D, 3);
        }

        int body = hitFlash > 0 ? 0xFFFFFFFF : color;
        int dark = Gfx.mix(color, 0xFF0B0714, 0.45f);
        int light = Gfx.mix(color, 0xFFFFFFFF, 0.28f);
        float r = radius;
        c.save();
        c.translate(x, y + bob - (boss ? 8f : 4f));
        if (boss) c.scale(sc / 2f, sc / 2f);
        else c.scale(sc, sc);
        if (attackAnim > 0) {
            float k = attackAnim / 0.26f;
            c.translate((float) Math.cos(dir) * r * 0.55f * k, (float) Math.sin(dir) * r * 0.55f * k);
        }

        switch (type) {
            case 0: {
                g.fill(body);
                c.drawOval(-r, -r * 0.55f, r, r * 0.7f, g.p);
                g.circle(c, -r * 0.38f, -r * 0.55f, r * 0.28f, light);
                eyes(g, c, 0.35f);
                break;
            }
            case 1: {
                g.circle(c, 0, 0, r * 0.85f, body);
                g.circle(c, 0, -r * 0.75f, r * 0.55f, light);
                g.circle(c, r * 0.5f, -r * 0.9f, r * 0.22f, body);
                g.circle(c, -r * 0.5f, -r * 0.9f, r * 0.22f, body);
                eyes(g, c, 0.3f);
                g.line(c, r * 0.3f, -r * 0.1f, r * 1.25f, -r * 0.6f, 0xFF8A6A3A, 4);
                g.circle(c, r * 1.28f, -r * 0.62f, 4, 0xFFDDDDDD);
                break;
            }
            case 2: {
                c.save();
                c.rotate((float) Math.toDegrees(dir));
                g.fill(body);
                c.drawOval(-r, -r * 0.55f, r * 0.9f, r * 0.55f, g.p);
                g.circle(c, r * 0.75f, -r * 0.1f, r * 0.45f, light);
                g.circle(c, r * 0.55f, -r * 0.5f, r * 0.16f, body);
                g.circle(c, r * 0.95f, -r * 0.5f, r * 0.16f, body);
                g.line(c, -r * 0.2f, r * 0.45f, -r * 0.25f, r * 0.9f, dark, 4);
                g.line(c, r * 0.4f, r * 0.4f, r * 0.4f, r * 0.9f, dark, 4);
                g.line(c, -r * 0.9f, -r * 0.1f, -r * 1.35f, -r * 0.5f, body, 5);
                g.circle(c, r * 1.0f, -r * 0.12f, 3, 0xFFFF5544);
                c.restore();
                break;
            }
            case 3: {
                g.roundRect(c, -r * 0.8f, -r * 0.8f, r * 1.6f, r * 1.5f, 8, body);
                g.circle(c, 0, -r * 0.75f, r * 0.5f, light);
                g.roundRect(c, -r * 1.15f, -r * 0.5f, r * 0.4f, r * 1.1f, 5, dark);
                g.roundRect(c, r * 0.75f, -r * 0.5f, r * 0.4f, r * 1.1f, 5, dark);
                eyes(g, c, 0.35f);
                g.line(c, -r * 0.3f, -r * 0.2f, r * 0.1f, r * 0.4f, Gfx.withAlpha(0xFFFFFFFF, 80), 2);
                g.line(c, r * 0.2f, -r * 0.3f, r * 0.45f, r * 0.1f, Gfx.withAlpha(0xFFFFFFFF, 80), 2);
                break;
            }
            case 4: {
                Path p = new Path();
                p.moveTo(0, -r);
                p.lineTo(r * 0.85f, r * 0.8f);
                p.lineTo(-r * 0.85f, r * 0.8f);
                p.close();
                g.fill(body);
                c.drawPath(p, g.p);
                g.circle(c, 0, -r * 0.75f, r * 0.35f, 0xFF2A1E40);
                g.circle(c, 0, -r * 0.68f, r * 0.26f, 0xFF0B0714);
                g.circle(c, 0, -r * 0.7f, 3.2f, 0xFFFF4D4D);
                g.circle(c, 0, -r * 0.7f, 2.2f, 0xFFFF9999);
                float ox = r * 1.05f, oy = -r * 0.5f + (float) Math.sin(time * 4) * 4;
                g.circle(c, ox, oy, r * 0.22f, Gfx.withAlpha(color, 90));
                g.circle(c, ox, oy, r * 0.13f, light);
                break;
            }
            case 5: {
                g.fill(Gfx.withAlpha(body, 210));
                c.drawOval(-r * 0.75f, -r, r * 0.75f, r * 0.35f, g.p);
                Path tail = new Path();
                tail.moveTo(-r * 0.6f, r * 0.2f);
                tail.lineTo(0, r * 1.1f);
                tail.lineTo(r * 0.6f, r * 0.2f);
                tail.close();
                g.fill(Gfx.withAlpha(dark, 170));
                c.drawPath(tail, g.p);
                g.circle(c, -r * 0.25f, -r * 0.5f, 3f, 0xFFFF4D4D);
                g.circle(c, r * 0.25f, -r * 0.5f, 3f, 0xFFFF4D4D);
                g.circleStroke(c, 0, -r * 0.3f, r * 0.75f, Gfx.withAlpha(color, 70), 2);
                break;
            }
            case 6: {
                g.roundRect(c, -r * 0.7f, -r * 0.7f, r * 1.4f, r * 1.35f, 6, body);
                g.roundRect(c, -r * 0.5f, -r * 1.1f, r, r * 0.6f, 6, light);
                g.fill(0xFF120D1C);
                g.line(c, -r * 0.3f, -r * 0.85f, r * 0.3f, -r * 0.85f, 0xFF0B0714, 4);
                g.line(c, -r * 0.25f, -r * 0.88f, -r * 0.05f, -r * 0.88f, 0xFFFF4D4D, 2);
                g.line(c, r * 0.05f, -r * 0.88f, r * 0.25f, -r * 0.88f, 0xFFFF4D4D, 2);
                g.line(c, r * 0.6f, 0, r * 1.5f, -r * 0.6f, 0xFFC9C9D9, 5);
                g.circle(c, -r * 0.75f, 0, r * 0.4f, dark);
                break;
            }
            case 7: {
                g.circle(c, 0, 0, r * 0.9f, Gfx.withAlpha(color, 60));
                g.circle(c, 0, 0, r * 0.55f, body);
                g.circle(c, 0, 0, r * 0.28f, 0xFFFFFFFF);
                for (int i = 0; i < 4; i++) {
                    float a = time * 1.6f + i * 1.57f;
                    g.circle(c, (float) Math.cos(a) * r, (float) Math.sin(a) * r, 3f, Gfx.withAlpha(color, 180));
                }
                break;
            }
            case 8: {
                g.roundRect(c, -r * 0.85f, -r * 1.05f, r * 1.7f, r * 1.9f, 8, body);
                g.roundRect(c, -r * 0.62f, -r * 1.25f, r * 1.24f, r * 0.5f, 5, light);
                g.circle(c, 0, -r * 0.1f, r * 0.32f, Gfx.withAlpha(0xFFFFE08A, 200));
                g.circle(c, 0, -r * 0.1f, r * 0.16f, 0xFFFFFFFF);
                g.line(c, -r * 0.7f, r * 0.4f, r * 0.7f, r * 0.4f, Gfx.withAlpha(0xFFFFE08A, 90), 2);
                break;
            }
            case 9: {
                g.circle(c, 0, 0, r * 0.65f, body);
                g.circle(c, 0, -r * 0.45f, r * 0.4f, light);
                Path h1 = new Path();
                h1.moveTo(-r * 0.35f, -r * 0.75f);
                h1.lineTo(-r * 0.55f, -r * 1.15f);
                h1.lineTo(-r * 0.12f, -r * 0.85f);
                h1.close();
                g.fill(body);
                c.drawPath(h1, g.p);
                Path h2 = new Path();
                h2.moveTo(r * 0.35f, -r * 0.75f);
                h2.lineTo(r * 0.55f, -r * 1.15f);
                h2.lineTo(r * 0.12f, -r * 0.85f);
                h2.close();
                c.drawPath(h2, g.p);
                g.circle(c, -r * 0.2f, -r * 0.45f, 2.4f, 0xFFFFDD44);
                g.circle(c, r * 0.2f, -r * 0.45f, 2.4f, 0xFFFFDD44);
                g.fill(Gfx.withAlpha(dark, 170));
                c.drawOval(-r * 1.0f, -r * 0.2f, -r * 0.45f, r * 0.35f, g.p);
                c.drawOval(r * 0.45f, -r * 0.2f, r * 1.0f, r * 0.35f, g.p);
                break;
            }
            case 20: {
                g.circle(c, -r * 0.15f, r * 0.1f, r * 0.75f, Gfx.withAlpha(dark, 190));
                g.circle(c, r * 0.35f, r * 0.1f, r * 0.5f, body);
                g.fill(dark);
                c.drawOval(r * 0.25f, -r * 0.2f, r * 1.15f, r * 0.35f, g.p);
                g.circle(c, r * 0.95f, -r * 0.05f, r * 0.2f, light);
                Path scorpTail = new Path();
                scorpTail.moveTo(-r * 0.5f, 0);
                scorpTail.quadTo(-r * 1.1f, -r * 0.9f, -r * 0.45f, -r * 1.25f);
                g.stroke(body, Math.max(2f, r * 0.16f));
                c.drawPath(scorpTail, g.p);
                g.circle(c, -r * 0.45f, -r * 1.28f, r * 0.16f, 0xFFFFDD55);
                g.line(c, r * 0.5f, -r * 0.2f, r * 1.3f, -r * 0.65f, body, Math.max(2f, r * 0.16f));
                g.line(c, r * 0.5f, r * 0.35f, r * 1.3f, r * 0.75f, body, Math.max(2f, r * 0.16f));
                g.line(c, r * 1.3f, -r * 0.65f, r * 1.55f, -r * 0.4f, light, 2.5f);
                g.line(c, r * 1.3f, r * 0.75f, r * 1.55f, r * 0.5f, light, 2.5f);
                g.circle(c, r * 0.7f, -r * 0.15f, 3f, 0xFFFF5544);
                break;
            }
            case 21: {
                for (int i = 0; i < 4; i++) {
                    float sd = (i < 2 ? -1f : 1f);
                    float yy = -r * 0.5f + i * r * 0.35f;
                    g.line(c, sd * r * 0.2f, yy, sd * r * 1.25f, yy - r * 0.35f + (float) Math.sin(time * 6 + i) * 3f,
                            dark, Math.max(1.6f, r * 0.09f));
                    g.line(c, sd * r * 1.25f, yy - r * 0.35f, sd * r * 1.55f, yy + r * 0.1f, dark, 2f);
                }
                g.circle(c, 0, r * 0.15f, r * 0.7f, body);
                g.circle(c, 0, -r * 0.45f, r * 0.45f, light);
                g.circle(c, -r * 0.16f, -r * 0.5f, r * 0.1f, 0xFFFF3355);
                g.circle(c, r * 0.16f, -r * 0.5f, r * 0.1f, 0xFFFF3355);
                g.circle(c, 0, r * 0.3f, r * 0.28f, Gfx.withAlpha(0xFFFF3355, 90));
                break;
            }
            case 22: {
                g.roundRect(c, -r * 0.28f, -r * 0.1f, r * 0.56f, r * 1.05f, r * 0.25f, 0xFFE8DFC0);
                g.fill(Gfx.mix(body, 0xFF000000, 0.25f));
                c.drawOval(-r * 0.9f, -r * 1.0f, r * 0.9f, r * 0.25f, g.p);
                g.fill(body);
                c.drawOval(-r * 0.78f, -r * 1.12f, r * 0.78f, r * 0.2f, g.p);
                g.circle(c, -r * 0.35f, -r * 0.78f, r * 0.16f, light);
                g.circle(c, r * 0.3f, -r * 0.6f, r * 0.12f, light);
                g.circle(c, 0, -r * 0.95f, r * 0.1f, light);
                g.circle(c, -r * 0.12f, r * 0.45f, 2.6f, 0xFF302018);
                g.circle(c, r * 0.12f, r * 0.45f, 2.6f, 0xFF302018);
                break;
            }
            case 23: {
                g.fill(Gfx.withAlpha(0xFF1A1522, 120));
                c.drawOval(-r * 0.75f, r * 0.1f, r * 0.75f, r * 0.9f, g.p);
                g.fill(body);
                c.drawOval(-r * 0.42f, -r * 0.62f, r * 0.42f, r * 0.5f, g.p);
                g.circle(c, -r * 0.17f, -r * 0.4f, r * 0.11f, 0xFF0B0714);
                g.circle(c, r * 0.17f, -r * 0.4f, r * 0.11f, 0xFF0B0714);
                for (int i = 0; i < 3; i++) {
                    g.line(c, -r * 0.35f, -r * 0.05f + i * r * 0.22f, r * 0.35f, -r * 0.05f + i * r * 0.22f, 0xFFCFC6AD, 2.4f);
                }
                g.line(c, -r * 0.3f, r * 0.55f, -r * 0.55f, r * 1.0f, 0xFFCFC6AD, 3f);
                g.line(c, r * 0.3f, r * 0.55f, r * 0.55f, r * 1.0f, 0xFFCFC6AD, 3f);
                g.line(c, r * 0.45f, 0, r * 1.35f, -r * 0.6f, 0xFFC9C9D9, 4f);
                g.line(c, r * 0.6f, -r * 0.25f, r * 1.0f, -r * 0.15f, 0xFF8A6A3A, 3f);
                break;
            }
            case 24: {
                c.save();
                c.rotate(0.12f);
                g.roundRect(c, -r * 0.55f, -r * 0.55f, r * 1.1f, r * 1.4f, 7, body);
                g.roundRect(c, -r * 0.4f, -r * 0.9f, r * 0.8f, r * 0.5f, 6, light);
                g.circle(c, -r * 0.14f, -r * 0.68f, 2.8f, 0xFFFF4D4D);
                g.circle(c, r * 0.14f, -r * 0.68f, 2.8f, 0xFFFF4D4D);
                g.line(c, -r * 0.35f, -r * 0.2f, -r * 1.25f, r * 0.05f, body, Math.max(3f, r * 0.2f));
                g.line(c, r * 0.35f, -r * 0.2f, r * 1.2f, -r * 0.15f, body, Math.max(3f, r * 0.2f));
                g.circle(c, -r * 1.3f, r * 0.08f, r * 0.14f, light);
                g.circle(c, r * 1.25f, -r * 0.17f, r * 0.14f, light);
                g.line(c, -r * 0.2f, r * 0.35f, r * 0.15f, r * 0.85f, Gfx.withAlpha(0xFF3A1A18, 180), 2f);
                c.restore();
                break;
            }
            case 25: {
                float flap = (float) Math.sin(time * 7) * r * 0.25f;
                Path wl = new Path();
                wl.moveTo(-r * 0.2f, -r * 0.2f);
                wl.lineTo(-r * 1.5f, -r * 0.9f - flap);
                wl.lineTo(-r * 1.0f, r * 0.15f - flap * 0.4f);
                wl.close();
                g.fill(Gfx.withAlpha(dark, 200));
                c.drawPath(wl, g.p);
                Path wr = new Path();
                wr.moveTo(r * 0.2f, -r * 0.2f);
                wr.lineTo(r * 1.5f, -r * 0.9f + flap);
                wr.lineTo(r * 1.0f, r * 0.15f + flap * 0.4f);
                wr.close();
                c.drawPath(wr, g.p);
                g.roundRect(c, -r * 0.35f, -r * 0.5f, r * 0.7f, r * 1.1f, 8, body);
                g.circle(c, 0, -r * 0.65f, r * 0.32f, light);
                g.line(c, -r * 0.2f, -r * 0.9f, -r * 0.3f, -r * 1.25f, 0xFFFFD36E, 3f);
                g.line(c, r * 0.2f, -r * 0.9f, r * 0.3f, -r * 1.25f, 0xFFFFD36E, 3f);
                g.line(c, -r * 0.18f, r * 0.55f, -r * 0.28f, r * 1.05f, 0xFFFFD36E, 2.6f);
                g.line(c, r * 0.18f, r * 0.55f, r * 0.28f, r * 1.05f, 0xFFFFD36E, 2.6f);
                g.circle(c, -r * 0.13f, -r * 0.68f, 2.4f, 0xFF0B0714);
                g.circle(c, r * 0.13f, -r * 0.68f, 2.4f, 0xFF0B0714);
                break;
            }
            case 26: {
                for (int i = 3; i >= 0; i--) {
                    float a = time * 2.2f - i * 0.6f;
                    float sx = (float) Math.cos(dir) * (r * 0.5f * i) - (float) Math.sin(dir) * (float) Math.sin(a) * r * 0.3f;
                    float sy = (float) Math.sin(dir) * (r * 0.5f * i) + (float) Math.cos(dir) * (float) Math.sin(a) * r * 0.3f;
                    g.circle(c, -sx, -sy, r * (0.34f - i * 0.045f), i == 0 ? body : dark);
                }
                g.circle(c, r * 0.45f, 0, r * 0.42f, body);
                g.circle(c, r * 0.6f, -r * 0.12f, 2.6f, 0xFFFFE08A);
                g.line(c, r * 0.2f, -r * 0.35f, r * 0.1f, -r * 0.85f, light, 3f);
                g.line(c, -r * 0.3f, -r * 0.15f, -r * 0.7f, -r * 0.55f, Gfx.withAlpha(color, 180), 2.4f);
                break;
            }
            case 27: {
                for (int i = -1; i <= 1; i++) {
                    Path cr = new Path();
                    cr.moveTo(i * r * 0.45f, -r * 0.25f);
                    cr.lineTo(i * r * 0.45f - r * 0.18f, -r * 1.0f);
                    cr.lineTo(i * r * 0.45f + r * 0.18f, -r * 1.0f);
                    cr.close();
                    g.fill(Gfx.withAlpha(light, 180));
                    c.drawPath(cr, g.p);
                }
                g.fill(dark);
                c.drawOval(-r * 0.95f, -r * 0.35f, r * 0.95f, r * 0.5f, g.p);
                g.fill(body);
                c.drawOval(-r * 0.7f, -r * 0.45f, r * 0.7f, r * 0.4f, g.p);
                g.circle(c, -r * 0.62f, -r * 0.55f, 3f, 0xFFFF5544);
                g.circle(c, r * 0.62f, -r * 0.55f, 3f, 0xFFFF5544);
                g.line(c, -r * 0.85f, -r * 0.05f, -r * 1.35f, -r * 0.4f, body, 4f);
                g.line(c, r * 0.85f, -r * 0.05f, r * 1.35f, -r * 0.4f, body, 4f);
                break;
            }
            case 28: {
                Path body28 = new Path();
                body28.moveTo(0, -r * 1.05f);
                body28.quadTo(r * 0.85f, -r * 0.2f, r * 0.45f, r * 1.0f);
                body28.lineTo(0, r * 0.6f);
                body28.lineTo(-r * 0.45f, r * 1.0f);
                body28.quadTo(-r * 0.85f, -r * 0.2f, 0, -r * 1.05f);
                body28.close();
                g.fill(Gfx.withAlpha(body, 170));
                c.drawPath(body28, g.p);
                g.circle(c, 0, -r * 0.5f, r * 0.36f, 0xFF120D1C);
                g.circle(c, -r * 0.13f, -r * 0.55f, 3f, 0xFF7FE0FF);
                g.circle(c, r * 0.13f, -r * 0.55f, 3f, 0xFF7FE0FF);
                g.line(c, -r * 0.5f, 0, -r * 1.05f, r * 0.4f, Gfx.withAlpha(body, 150), 4f);
                g.line(c, r * 0.5f, 0, r * 1.05f, r * 0.4f, Gfx.withAlpha(body, 150), 4f);
                for (int i = 0; i < 3; i++) {
                    g.circle(c, (float) Math.sin(time * 2 + i) * r * 0.35f, r * 0.75f + i * r * 0.12f, r * 0.08f, Gfx.withAlpha(color, 140));
                }
                break;
            }
            case 29: {
                g.fill(dark);
                c.drawOval(-r * 0.68f, r * 0.05f, r * 0.68f, r * 0.8f, g.p);
                g.fill(body);
                c.drawOval(-r * 0.48f, -r * 0.75f, r * 0.48f, r * 0.55f, g.p);
                g.fill(0xFF1A1420);
                c.drawOval(-r * 0.5f, -r * 0.35f, r * 0.5f, r * 0.05f, g.p);
                g.circle(c, -r * 0.15f, -r * 0.55f, 2.6f, 0xFFFFD36E);
                g.circle(c, r * 0.15f, -r * 0.55f, 2.6f, 0xFFFFD36E);
                g.fill(0xFF7A2E2E);
                c.drawOval(-r * 0.5f, -r * 0.85f, r * 0.5f, -r * 0.6f, g.p);
                g.line(c, r * 0.4f, 0, r * 1.25f, -r * 0.55f, 0xFFC9C9D9, 3.4f);
                g.line(c, r * 0.3f, -r * 0.05f, r * 0.55f, 0.1f, 0xFF8A6A3A, 4f);
                g.line(c, -r * 0.4f, r * 0.2f, -r * 0.9f, r * 0.6f, Gfx.withAlpha(dark, 200), 4f);
                break;
            }
            default: {
                drawBoss(c, g, r, body, dark, light);
                break;
            }
        }
        g.fill(Gfx.withAlpha(light, 62));
        c.drawOval(-r * 0.52f, -r * 0.62f, -r * 0.1f, -r * 0.34f, g.p);
        g.line(c, -r * 0.52f, r * 0.48f, r * 0.48f, r * 0.48f,
                Gfx.withAlpha(dark, 95), Math.max(1.2f, r * 0.045f));
        c.restore();

        if (boss || elite || hp < maxHp) {
            float bw = boss ? 0 : Math.min(60f, radius * 1.3f);
            if (bw > 0) {
                g.bar(c, x - bw, y - radius * 2.1f - (boss ? 0 : 6), bw * 2, 5f,
                        hp / maxHp, 0xAA1A1220, elite ? Gfx.GOLD : 0xFFE5484D);
            }
        }
        if (stun > 0) {
            for (int i = 0; i < 3; i++) {
                float a = globalTime * 5 + i * 2.1f;
                g.circle(c, x + (float) Math.cos(a) * 18, y - radius * 2.1f + (float) Math.sin(a) * 6, 3.2f, Gfx.GOLD);
            }
        }
    }

    private void drawBoss(Canvas c, Gfx g, float r, int body, int dark, int light) {
        switch (bossIndex) {
            case 0: {
                g.fill(Gfx.withAlpha(color, 45));
                c.drawOval(-r * 1.3f, -r * 1.3f, r * 1.3f, r * 1.3f, g.p);
                g.fill(dark);
                c.drawOval(-r, -r * 0.9f, r * 0.9f, r * 1.0f, g.p);
                g.circle(c, r * 0.55f, -r * 0.35f, r * 0.55f, body);
                g.line(c, r * 0.1f, -r * 0.9f, r * 0.85f, -r * 1.5f, 0xFFDDEEBB, 8);
                g.line(c, r * 0.5f, -r * 0.9f, r * 1.1f, -r * 1.3f, 0xFFDDEEBB, 6);
                g.line(c, -r * 0.6f, r * 0.2f, -r * 1.4f, r * 0.9f, dark, 12);
                g.circle(c, r * 0.75f, -r * 0.45f, 5, 0xFFFFE08A);
                g.line(c, -r * 0.2f, -r * 0.4f, r * 0.2f, -r * 0.1f, Gfx.withAlpha(color, 120), 3);
                break;
            }
            case 1: {
                g.fill(Gfx.withAlpha(0xFF9BD4FF, 35));
                c.drawOval(-r * 1.35f, -r * 1.35f, r * 1.35f, r * 1.35f, g.p);
                g.fill(dark);
                c.drawOval(-r, -r * 0.9f, r * 0.9f, r * 0.9f, g.p);
                g.circle(c, r * 0.5f, -r * 0.5f, r * 0.55f, body);
                g.line(c, r * 0.7f, -r * 0.8f, r * 1.0f, -r * 1.5f, 0xFFDDEEFF, 7);
                g.line(c, r * 0.9f, -r * 0.7f, r * 1.4f, -r * 1.1f, 0xFFDDEEFF, 5);
                g.circle(c, r * 0.7f, -r * 0.55f, 5, 0xFFFFFFFF);
                Path wing = new Path();
                wing.moveTo(-r * 0.3f, -r * 0.4f);
                wing.lineTo(-r * 1.6f, -r * 1.3f);
                wing.lineTo(-r * 1.2f, -r * 0.2f);
                wing.close();
                g.fill(Gfx.withAlpha(body, 150));
                c.drawPath(wing, g.p);
                break;
            }
            case 2: {
                g.fill(Gfx.withAlpha(0xFFFF6B3D, 45));
                c.drawOval(-r * 1.3f, -r * 1.3f, r * 1.3f, r * 1.3f, g.p);
                g.roundRect(c, -r * 0.8f, -r, r * 1.6f, r * 1.8f, 10, body);
                g.circle(c, 0, -r * 1.0f, r * 0.55f, light);
                Path h1 = new Path();
                h1.moveTo(-r * 0.4f, -r * 1.3f);
                h1.lineTo(-r * 0.75f, -r * 1.9f);
                h1.lineTo(-r * 0.1f, -r * 1.45f);
                h1.close();
                g.fill(Gfx.GOLD);
                c.drawPath(h1, g.p);
                Path h2 = new Path();
                h2.moveTo(r * 0.4f, -r * 1.3f);
                h2.lineTo(r * 0.75f, -r * 1.9f);
                h2.lineTo(r * 0.1f, -r * 1.45f);
                h2.close();
                c.drawPath(h2, g.p);
                g.circle(c, -r * 0.22f, -r * 1.02f, 4, 0xFFFFDD44);
                g.circle(c, r * 0.22f, -r * 1.02f, 4, 0xFFFFDD44);
                g.line(c, r * 0.7f, 0, r * 1.7f, -r * 0.9f, 0xFFFFE0A0, 9);
                break;
            }
            case 3: {
                g.fill(Gfx.withAlpha(0xFFB07CFF, 40));
                c.drawOval(-r * 1.35f, -r * 1.35f, r * 1.35f, r * 1.35f, g.p);
                Path cloak = new Path();
                cloak.moveTo(0, -r * 1.1f);
                cloak.lineTo(r * 0.9f, r * 1.0f);
                cloak.lineTo(-r * 0.9f, r * 1.0f);
                cloak.close();
                g.fill(dark);
                c.drawPath(cloak, g.p);
                g.circle(c, 0, -r * 0.85f, r * 0.5f, 0xFF120D1C);
                g.circle(c, -r * 0.16f, -r * 0.9f, 5, 0xFFFF3355);
                g.circle(c, r * 0.16f, -r * 0.9f, 5, 0xFFFF3355);
                g.circleStroke(c, 0, -r * 0.6f, r * 1.1f, Gfx.withAlpha(0xFFB07CFF, 110), 3);
                g.line(c, r * 0.6f, -r * 0.2f, r * 1.6f, -r * 0.8f, 0xFFB07CFF, 6);
                g.line(c, -r * 0.6f, -r * 0.2f, -r * 1.6f, -r * 0.8f, 0xFFB07CFF, 6);
                break;
            }
            case 5: {
                g.fill(Gfx.withAlpha(color, 45));
                c.drawOval(-r * 1.4f, -r * 1.2f, r * 1.4f, r * 1.2f, g.p);
                for (int i = 4; i >= 1; i--) {
                    float a = time * 1.8f - i * 0.5f;
                    g.circle(c, -r * i * 0.32f, (float) Math.sin(a) * r * 0.35f, r * (0.55f - i * 0.07f), i % 2 == 0 ? dark : body);
                }
                Path fin = new Path();
                fin.moveTo(-r * 0.2f, -r * 0.9f);
                fin.lineTo(-r * 1.2f, -r * 1.9f);
                fin.lineTo(-r * 0.7f, -r * 0.5f);
                fin.close();
                g.fill(Gfx.withAlpha(0xFF9BE8FF, 170));
                c.drawPath(fin, g.p);
                g.circle(c, r * 0.35f, -r * 0.2f, r * 0.6f, body);
                g.circle(c, r * 0.55f, -r * 0.35f, 6f, 0xFFFFE08A);
                g.line(c, r * 0.1f, -r * 0.6f, -r * 0.1f, -r * 1.15f, 0xFFE8FBFF, 6f);
                g.line(c, r * 0.5f, -r * 0.55f, r * 0.7f, -r * 1.0f, 0xFFE8FBFF, 5f);
                break;
            }
            case 6: {
                g.fill(Gfx.withAlpha(color, 45));
                c.drawOval(-r * 1.4f, -r * 1.35f, r * 1.4f, r * 1.35f, g.p);
                Path cw = new Path();
                cw.moveTo(-r * 0.3f, -r * 0.4f);
                cw.lineTo(-r * 1.6f, -r * 1.2f);
                cw.lineTo(-r * 1.2f, r * 0.2f);
                cw.close();
                g.fill(Gfx.withAlpha(Gfx.mix(color, 0xFFFFFFFF, 0.3f), 150));
                c.drawPath(cw, g.p);
                Path cw2 = new Path();
                cw2.moveTo(r * 0.3f, -r * 0.4f);
                cw2.lineTo(r * 1.6f, -r * 1.2f);
                cw2.lineTo(r * 1.2f, r * 0.2f);
                cw2.close();
                c.drawPath(cw2, g.p);
                g.roundRect(c, -r * 0.7f, -r * 0.9f, r * 1.4f, r * 1.7f, 10, body);
                g.circle(c, 0, -r * 1.0f, r * 0.52f, light);
                g.circle(c, -r * 0.18f, -r * 1.05f, 4f, 0xFFD9C4FF);
                g.circle(c, r * 0.18f, -r * 1.05f, 4f, 0xFFD9C4FF);
                for (int i = -1; i <= 1; i++) {
                    Path sh = new Path();
                    sh.moveTo(i * r * 0.4f - r * 0.16f, -r * 1.35f);
                    sh.lineTo(i * r * 0.4f, -r * 1.95f);
                    sh.lineTo(i * r * 0.4f + r * 0.16f, -r * 1.35f);
                    sh.close();
                    g.fill(Gfx.withAlpha(Gfx.mix(color, 0xFFFFFFFF, 0.5f), 210));
                    c.drawPath(sh, g.p);
                }
                break;
            }
            case 7: {
                g.fill(Gfx.withAlpha(color, 55));
                c.drawOval(-r * 1.45f, -r * 1.45f, r * 1.45f, r * 1.45f, g.p);
                float flap = (float) Math.sin(time * 5) * r * 0.35f;
                Path w = new Path();
                w.moveTo(-r * 0.3f, -r * 0.5f);
                w.lineTo(-r * 2.0f, -r * 1.2f - flap);
                w.lineTo(-r * 1.3f, r * 0.1f);
                w.close();
                g.fill(Gfx.withAlpha(Gfx.mix(color, 0xFFFFFFFF, 0.25f), 150));
                c.drawPath(w, g.p);
                Path w2 = new Path();
                w2.moveTo(r * 0.3f, -r * 0.5f);
                w2.lineTo(r * 2.0f, -r * 1.2f + flap);
                w2.lineTo(r * 1.3f, r * 0.1f);
                w2.close();
                c.drawPath(w2, g.p);
                g.roundRect(c, -r * 0.55f, -r * 0.9f, r * 1.1f, r * 1.6f, 9, body);
                g.circle(c, 0, -r * 0.95f, r * 0.42f, light);
                g.circle(c, -r * 0.16f, -r * 0.98f, 4f, 0xFFFFFFFF);
                g.circle(c, r * 0.16f, -r * 0.98f, 4f, 0xFFFFFFFF);
                Path bolt = new Path();
                bolt.moveTo(r * 0.8f, -r * 0.9f);
                bolt.lineTo(r * 1.05f, -r * 0.3f);
                bolt.lineTo(r * 0.82f, -r * 0.25f);
                bolt.lineTo(r * 1.0f, r * 0.45f);
                bolt.lineTo(r * 0.6f, -r * 0.15f);
                bolt.lineTo(r * 0.85f, -r * 0.2f);
                bolt.close();
                g.fill(0xFFFFF2A8);
                c.drawPath(bolt, g.p);
                break;
            }
            case 8: {
                g.fill(Gfx.withAlpha(0xFF6B8A3A, 55));
                c.drawOval(-r * 1.45f, -r * 1.3f, r * 1.45f, r * 1.3f, g.p);
                for (int i = 0; i < 4; i++) {
                    float a = i * 1.57f + time * 0.4f;
                    float vx = (float) Math.cos(a) * r * 1.0f;
                    float vy = (float) Math.sin(a) * r * 0.7f;
                    g.line(c, 0, r * 0.5f, vx, vy, Gfx.withAlpha(Gfx.mix(color, 0xFFFFFFFF, 0.2f), 160), 4f);
                    g.circle(c, vx, vy, r * 0.09f, 0xFFB9E06A);
                }
                g.roundRect(c, -r * 0.75f, -r * 0.9f, r * 1.5f, r * 1.7f, 12, body);
                g.circle(c, 0, -r * 1.0f, r * 0.5f, light);
                g.circle(c, -r * 0.18f, -r * 1.02f, 4.4f, 0xFFC6FF5E);
                g.circle(c, r * 0.18f, -r * 1.02f, 4.4f, 0xFFC6FF5E);
                for (int i = -1; i <= 1; i++) {
                    g.line(c, i * r * 0.42f, -r * 1.25f, i * r * 0.55f, -r * 1.75f, 0xFF7A9A3A, 4f);
                    g.circle(c, i * r * 0.55f, -r * 1.78f, r * 0.08f, 0xFFE07AA0);
                }
                g.circle(c, 0, r * 0.4f, r * 0.22f, Gfx.withAlpha(0xFF9BD44A, 120));
                break;
            }
            case 9: {
                g.fill(Gfx.withAlpha(color, 60));
                c.drawOval(-r * 1.5f, -r * 1.4f, r * 1.5f, r * 1.4f, g.p);
                float flap = (float) Math.sin(time * 4) * r * 0.3f;
                Path dw = new Path();
                dw.moveTo(-r * 0.3f, -r * 0.6f);
                dw.lineTo(-r * 2.1f, -r * 1.5f - flap);
                dw.lineTo(-r * 1.5f, r * 0.1f);
                dw.close();
                g.fill(Gfx.withAlpha(dark, 200));
                c.drawPath(dw, g.p);
                Path dw2 = new Path();
                dw2.moveTo(r * 0.3f, -r * 0.6f);
                dw2.lineTo(r * 2.1f, -r * 1.5f + flap);
                dw2.lineTo(r * 1.5f, r * 0.1f);
                dw2.close();
                c.drawPath(dw2, g.p);
                g.roundRect(c, -r * 0.8f, -r * 0.8f, r * 1.6f, r * 1.7f, 12, body);
                g.circle(c, 0, -r * 1.05f, r * 0.5f, body);
                g.circle(c, -r * 0.16f, -r * 1.08f, 5f, 0xFFFFE08A);
                g.circle(c, r * 0.16f, -r * 1.08f, 5f, 0xFFFFE08A);
                Path h1 = new Path();
                h1.moveTo(-r * 0.35f, -r * 1.35f);
                h1.lineTo(-r * 0.7f, -r * 1.95f);
                h1.lineTo(-r * 0.1f, -r * 1.45f);
                h1.close();
                g.fill(0xFFFFD9A0);
                c.drawPath(h1, g.p);
                Path h2 = new Path();
                h2.moveTo(r * 0.35f, -r * 1.35f);
                h2.lineTo(r * 0.7f, -r * 1.95f);
                h2.lineTo(r * 0.1f, -r * 1.45f);
                h2.close();
                c.drawPath(h2, g.p);
                g.circle(c, 0, r * 0.1f, r * 0.34f, Gfx.withAlpha(0xFFFFCC66, 80));
                g.circle(c, 0, r * 0.1f, r * 0.18f, 0xFFFFF0B0);
                break;
            }
            default: {
                g.fill(Gfx.withAlpha(0xFFFFE08A, 45));
                c.drawOval(-r * 1.4f, -r * 1.4f, r * 1.4f, r * 1.4f, g.p);
                g.circle(c, 0, -r * 0.85f, r * 0.5f, 0xFFF3EDE3);
                g.circle(c, 0, -r * 0.95f, r * 0.24f, 0xFFFFE08A);
                g.roundRect(c, -r * 0.55f, -r * 0.45f, r * 1.1f, r * 1.35f, 8, body);
                Path wing = new Path();
                wing.moveTo(-r * 0.4f, -r * 0.3f);
                wing.lineTo(-r * 1.8f, -r * 1.0f);
                wing.lineTo(-r * 1.1f, r * 0.3f);
                wing.close();
                g.fill(Gfx.withAlpha(0xFFFFE08A, 130));
                c.drawPath(wing, g.p);
                Path wing2 = new Path();
                wing2.moveTo(r * 0.4f, -r * 0.3f);
                wing2.lineTo(r * 1.8f, -r * 1.0f);
                wing2.lineTo(r * 1.1f, r * 0.3f);
                wing2.close();
                c.drawPath(wing2, g.p);
                g.circleStroke(c, 0, -r * 0.9f, r * 0.75f, Gfx.withAlpha(0xFFFFE08A, 200), 4);
                g.circle(c, 0, 0, r * 0.2f, 0xFFFFE08A);
                break;
            }
        }
    }

    private void eyes(Gfx g, Canvas c, float off) {
        g.circle(c, -radius * off, -radius * 0.2f, 3.2f, 0xFFFFFFFF);
        g.circle(c, radius * off, -radius * 0.2f, 3.2f, 0xFFFFFFFF);
        g.circle(c, -radius * off, -radius * 0.2f, 1.6f, 0xFF0B0714);
        g.circle(c, radius * off, -radius * 0.2f, 1.6f, 0xFF0B0714);
    }
}
