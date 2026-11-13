package com.mysticrealm;

import android.graphics.Canvas;

import java.util.Random;

public class Player {
    public final Gameplay gp;
    public final SaveData save;

    public float x, y;
    public float dir = -1.5708f;
    public float hp, maxHp, mp, maxMp;
    public int atk, def, spd, crit, luck;

    public float[] cd = new float[3];
    public float dodgeCd;
    public float attackCd;
    public float attackAnim;
    public float hitFlash;
    public float invuln;
    public float buffTimer;
    public float guardTimer;
    public float dash;
    public float dashDir;
    public float kbx, kby;
    public float time;
    public float lastHurt;
    public float lastMove;
    public float animMove;
    public boolean moving;
    public boolean dead;
    public int combo;
    public float comboTimer;

    public final float radius = 22f;
    private final Random rnd = new Random();

    public Player(Gameplay gp, SaveData save) {
        this.gp = gp;
        this.save = save;
        spawn(0, 0);
    }

    public void recompute() {
        int[] s = save.computeStats();
        maxHp = s[0];
        maxMp = s[1];
        atk = s[2];
        def = s[3];
        spd = s[4];
        crit = s[5];
        luck = s[6];
    }

    public Catalog.ClassDef classDef() {
        return Catalog.CLASSES[save.classIndex];
    }

    public void spawn(float px, float py) {
        x = px;
        y = py;
        recompute();
        hp = maxHp;
        mp = maxMp;
        dead = false;
        invuln = 1.2f;
        hitFlash = 0;
        buffTimer = 0;
        guardTimer = 0;
        dash = 0;
        attackCd = 0;
        attackAnim = 0;
        dodgeCd = 0;
        for (int i = 0; i < 3; i++) cd[i] = 0;
        kbx = kby = 0;
    }

    public float speedNow() {
        float base = spd * 2.35f;
        if (buffTimer > 0) base *= 1.08f;
        if (dash > 0) base *= 2.6f;
        return base;
    }

    public void update(float dt, float jx, float jy) {
        time += dt;
        float mag = (float) Math.sqrt(jx * jx + jy * jy);
        moving = mag > 0.08f;
        if (moving) {
            if (mag > 1f) {
                jx /= mag;
                jy /= mag;
                mag = 1f;
            }
            dir = (float) Math.atan2(jy, jx);
            animMove += dt;
            lastMove = time;
            if (rnd.nextFloat() < dt * 12f) {
                gp.fxDust(x, y + 18, classDef().color);
            }
        }
        float sp = speedNow();
        float mx, my;
        if (dash > 0) {
            mx = (float) Math.cos(dashDir) * sp;
            my = (float) Math.sin(dashDir) * sp;
            dash -= dt;
        } else {
            mx = jx * sp;
            my = jy * sp;
        }
        x += (mx + kbx) * dt;
        y += (my + kby) * dt;
        kbx *= (float) Math.pow(0.0005f, dt);
        kby *= (float) Math.pow(0.0005f, dt);
        gp.world.resolve(this, radius);

        if (attackCd > 0) attackCd -= dt;
        if (attackAnim > 0) attackAnim -= dt;
        if (dodgeCd > 0) dodgeCd -= dt;
        if (invuln > 0) invuln -= dt;
        if (hitFlash > 0) hitFlash -= dt;
        if (comboTimer > 0) {
            comboTimer -= dt;
            if (comboTimer <= 0) combo = 0;
        }
        for (int i = 0; i < 3; i++) {
            if (cd[i] > 0) cd[i] -= dt;
        }
        if (buffTimer > 0) buffTimer -= dt;
        if (guardTimer > 0) guardTimer -= dt;

        if (mp < maxMp) {
            mp += 2.4f * dt;
            if (mp > maxMp) mp = maxMp;
        }
        if (time - lastHurt > 7f && hp < maxHp && !dead) {
            hp += 0.5f * dt;
            if (hp > maxHp) hp = maxHp;
        }
        if (moving && dash <= 0 && rnd.nextFloat() < dt * 6f) {
            gp.fxDust(x, y + 20, 0x55A89CB8);
        }
    }

    public void faceNearest() {
        Enemy e = gp.nearestEnemy(x, y, 620);
        if (e != null) {
            dir = (float) Math.atan2(e.y - y, e.x - x);
        }
    }

    public void tryAttack() {
        boolean owner = save.isOwner();
        if (dead || (!owner && attackCd > 0) || gp.puzzleOpen) return;
        Catalog.ClassDef c = classDef();
        int cls = save.classIndex;
        attackCd = owner ? 0.08f : (cls == 2 ? 0.27f : (cls == 1 ? 0.46f : (cls == 3 ? 0.34f : (cls == 4 ? 0.5f : (cls == 5 ? 0.52f : 0.38f)))));
        attackAnim = 0.2f;
        if (!gp.mouseAimActive()) faceNearest();

        if (cls == 1) {
            float dmg = atk * 1.05f;
            gp.spawnProjectile(x + (float) Math.cos(dir) * 22, y + (float) Math.sin(dir) * 22,
                    dir, 690f, dmg, crit / 100f, c.color, 0, false, 12f, 1.1f);
            gp.audio.play("attack", 0.5f);
        } else if (cls == 3) {
            float dmg = atk * 0.96f;
            gp.spawnProjectile(x + (float) Math.cos(dir) * 24, y + (float) Math.sin(dir) * 24,
                    dir, 800f, dmg, crit / 100f, c.color, 3, false, 9f, 1f);
            gp.audio.play("attack", 0.4f);
        } else if (cls == 4) {
            float dmg = atk * 1.08f;
            gp.spawnProjectile(x + (float) Math.cos(dir) * 25, y + (float) Math.sin(dir) * 25,
                    dir, 620f, dmg, crit / 100f, c.color, 4, false, 12f, 1.2f);
            gp.audio.play("attack", 0.45f);
        } else {
            float range = cls == 2 ? 96f : (cls == 5 ? 132f : 118f);
            float arc = cls == 2 ? 1.5f : (cls == 5 ? 2.1f : 1.9f);
            float dmg = atk * (cls == 2 ? 1.02f : (cls == 5 ? 1.22f : 1.18f));
            gp.fxSlash(x, y, dir, range, c.color);
            gp.meleePlayer(dmg, Math.min(0.9f, crit / 100f + (cls == 2 ? 0.08f : 0)), range, arc, cls == 5 ? 190f : 150f, c.color, 0, 0);
            gp.audio.play("attack", 0.55f);
            gp.shake(cls == 5 ? 3.4f : 2.2f);
        }
    }

    public boolean castSkill(int i) {
        if (dead || i < 0 || i > 2) return false;
        Catalog.Skill s = Catalog.SKILLS[save.classIndex][i];
        boolean owner = save.isOwner();
        if (!owner && cd[i] > 0) {
            gp.toast("Skill masih cooldown");
            gp.audio.play("deny");
            return false;
        }
        if (!owner && mp < s.mp) {
            gp.toast("MP tidak cukup");
            gp.audio.play("deny");
            return false;
        }
        if (!gp.mouseAimActive()) faceNearest();
        int lvl = save.skillLv[i];
        float mult = s.power * (1f + 0.16f * lvl);
        float cc = Math.min(0.9f, crit / 100f + 0.03f * lvl);
        Catalog.ClassDef c = classDef();

        if (s.kind == Catalog.SKILL_ASS_EXECUTE) {
            Enemy target = gp.nearestEnemy(x, y, 620);
            if (target == null) {
                gp.toast("Tidak ada target");
                gp.audio.play("deny");
                return false;
            }
            mp -= s.mp;
            cd[i] = owner ? 0f : s.cd * (1f - 0.035f * lvl);
            float tx = target.x - (float) Math.cos(Math.atan2(target.y - y, target.x - x)) * 55;
            float ty = target.y - (float) Math.sin(Math.atan2(target.y - y, target.x - x)) * 55;
            gp.fxTrail(x, y, tx, ty, c.color);
            x = tx;
            y = ty;
            gp.world.resolve(this, radius);
            dir = (float) Math.atan2(target.y - y, target.x - x);
            gp.areaDamage(target.x, target.y, 110, atk * mult, 1f, 0.4f, 0, c.color);
            invuln = Math.max(invuln, 0.55f);
            if (owner) mp = maxMp;
            gp.fxExplosion(target.x, target.y, 120, c.color);
            gp.shake(10);
            gp.audio.play("skill");
            return true;
        }

        if (s.kind == Catalog.SKILL_ASS_STEP) {
            mp -= s.mp;
            cd[i] = owner ? 0f : s.cd * (1f - 0.035f * lvl);
            float mag = 190f;
            float nx = x + (float) Math.cos(dir) * mag;
            float ny = y + (float) Math.sin(dir) * mag;
            gp.fxTrail(x, y, nx, ny, c.color);
            gp.dashStrike(x, y, nx, ny, 62f, atk * mult, cc, c.color);
            x = nx;
            y = ny;
            gp.world.resolve(this, radius);
            invuln = Math.max(invuln, 0.5f);
            if (owner) mp = maxMp;
            gp.audio.play("dodge");
            gp.shake(5);
            return true;
        }

        mp -= s.mp;
        cd[i] = owner ? 0f : s.cd * (1f - 0.035f * lvl);
        gp.audio.play("skill");

        switch (s.kind) {
            case Catalog.SKILL_WAR_WHIRL: {
                gp.areaDamage(x, y, 155, atk * mult, cc, 0.25f, 0, c.color);
                gp.fxRing(x, y, 155, c.color, 0.4f);
                gp.fxSlash(x, y, dir, 150, c.color);
                gp.shake(6);
                break;
            }
            case Catalog.SKILL_WAR_CRY: {
                buffTimer = 8f;
                gp.stunArea(x, y, 280, 1.2f);
                gp.fxRing(x, y, 280, c.color, 0.6f);
                gp.floatText(x, y - 60, "WAR CRY!", c.color);
                gp.shake(7);
                break;
            }
            case Catalog.SKILL_WAR_SLAM: {
                gp.areaDamage(x, y, 240, atk * mult, cc, 1.6f, 0, c.color);
                gp.fxExplosion(x, y, 240, c.color);
                gp.shake(14);
                break;
            }
            case Catalog.SKILL_MAG_BOLTS: {
                float dmg = atk * mult;
                for (int k = -1; k <= 1; k++) {
                    float d = dir + k * 0.18f;
                    gp.spawnProjectile(x + (float) Math.cos(d) * 24, y + (float) Math.sin(d) * 24,
                            d, 650f, dmg, cc, c.color, 1, false, 11f, 1.2f);
                }
                gp.shake(3);
                break;
            }
            case Catalog.SKILL_MAG_NOVA: {
                gp.areaDamage(x, y, 205, atk * mult, cc, 0, 3f, 0xFF9BD4FF);
                gp.fxRing(x, y, 205, 0xFF9BD4FF, 0.55f);
                gp.fxExplosion(x, y, 150, 0xFF4EA8DE);
                gp.shake(5);
                break;
            }
            case Catalog.SKILL_MAG_METEOR: {
                Enemy e = gp.nearestEnemy(x, y, 720);
                float tx, ty;
                if (e != null) {
                    tx = e.x;
                    ty = e.y;
                } else {
                    tx = x + (float) Math.cos(dir) * 240;
                    ty = y + (float) Math.sin(dir) * 240;
                }
                gp.spawnBlast(tx, ty, 170, 0.75f, atk * mult, cc, 0xFFFF6B4A, 0.8f, 0);
                gp.audio.play("boss", 0.35f);
                break;
            }
            case Catalog.SKILL_ASS_VENOM: {
                float dmg = atk * mult;
                for (int k = -2; k <= 2; k++) {
                    float d = dir + k * 0.16f;
                    gp.spawnProjectile(x + (float) Math.cos(d) * 22, y + (float) Math.sin(d) * 22,
                            d, 720f, dmg, cc, 0xFF7FE08A, 2, true, 10f, 1f);
                }
                break;
            }
            case Catalog.SKILL_ARC_VOLLEY: {
                float dmg = atk * mult;
                for (int k = -1; k <= 1; k++) {
                    float d = dir + k * 0.15f;
                    gp.spawnProjectile(x + (float) Math.cos(d) * 24, y + (float) Math.sin(d) * 24,
                            d, 820f, dmg, cc, c.color, 3, false, 9f, 1.1f);
                }
                gp.shake(3);
                break;
            }
            case Catalog.SKILL_ARC_TRAP: {
                Enemy e = gp.nearestEnemy(x, y, 560);
                float tx = e != null ? e.x : x + (float) Math.cos(dir) * 150;
                float ty = e != null ? e.y : y + (float) Math.sin(dir) * 150;
                gp.areaDamage(tx, ty, 120, atk * mult, cc, 1.4f, 3.2f, 0xFF7FD17F);
                gp.fxRing(tx, ty, 120, 0xFF7FD17F, 0.6f);
                gp.fxExplosion(tx, ty, 80, 0xFF9BD44A);
                break;
            }
            case Catalog.SKILL_ARC_RAIN: {
                Enemy e = gp.nearestEnemy(x, y, 700);
                float tx = e != null ? e.x : x + (float) Math.cos(dir) * 220;
                float ty = e != null ? e.y : y + (float) Math.sin(dir) * 220;
                for (int k = 0; k < 5; k++) {
                    float ang = k * 1.2566f;
                    float rad = k == 0 ? 0 : 70f;
                    gp.spawnBlast(tx + (float) Math.cos(ang) * rad, ty + (float) Math.sin(ang) * rad,
                            105, 0.55f + k * 0.07f, atk * mult, cc, 0xFFF2C94C, 0.25f, 0);
                }
                gp.fxRing(tx, ty, 150, 0xFFF2C94C, 0.7f);
                gp.audio.play("boss", 0.3f);
                break;
            }
            case Catalog.SKILL_PRI_HEAL: {
                heal(maxHp * mult);
                gp.fxRing(x, y, 150, 0xFFFFF0B0, 0.7f);
                break;
            }
            case Catalog.SKILL_PRI_SMITE: {
                gp.spawnProjectile(x + (float) Math.cos(dir) * 24, y + (float) Math.sin(dir) * 24,
                        dir, 640f, atk * mult, cc, c.color, 4, false, 14f, 1.3f);
                gp.stunArea(x + (float) Math.cos(dir) * 40, y + (float) Math.sin(dir) * 40, 40, 0.35f);
                break;
            }
            case Catalog.SKILL_PRI_SANCTUARY: {
                gp.areaDamage(x, y, 230, atk * mult, cc, 0.5f, 0, c.color);
                gp.fxExplosion(x, y, 220, c.color);
                gp.fxRing(x, y, 230, c.color, 0.8f);
                heal(maxHp * 0.12f);
                gp.shake(7);
                break;
            }
            case Catalog.SKILL_KNI_BASH: {
                gp.fxSlash(x, y, dir, 140, c.color);
                gp.meleePlayer(atk * mult, cc, 145, 2.2f, 240f, c.color, 0.9f, 0);
                gp.shake(8);
                break;
            }
            case Catalog.SKILL_KNI_GUARD: {
                guardTimer = 8f;
                heal(maxHp * 0.15f);
                gp.fxRing(x, y, 120, c.color, 0.7f);
                gp.floatText(x, y - 62, "IRON GUARD", c.color);
                break;
            }
            case Catalog.SKILL_KNI_JUDGMENT: {
                Enemy e = gp.nearestEnemy(x, y, 620);
                float tx = e != null ? e.x : x + (float) Math.cos(dir) * 170;
                float ty = e != null ? e.y : y + (float) Math.sin(dir) * 170;
                gp.spawnBlast(tx, ty, 185, 0.35f, atk * mult, cc, 0xFFFFF1C1, 1.1f, 0);
                gp.fxSlash(tx, ty, dir, 160, c.color);
                gp.shake(12);
                break;
            }
            default:
                break;
        }
        if (owner) mp = maxMp;
        return true;
    }

    public void dodge() {
        boolean owner = save.isOwner();
        if (dead || (!owner && dodgeCd > 0)) return;
        dodgeCd = owner ? 0f : 1.6f;
        dash = 0.24f;
        dashDir = dir;
        invuln = Math.max(invuln, 0.5f);
        gp.fxRing(x, y, 60, classDef().color, 0.3f);
        gp.audio.play("dodge");
    }

    public void damage(float amount, Enemy src) {
        if (dead || invuln > 0) return;
        float taken = amount - def * 0.75f;
        if (guardTimer > 0) taken *= 0.6f;
        if (taken < 1f) taken = 1f;
        if (amount > 0 && taken > amount) taken = amount;
        hp -= taken;
        invuln = 0.55f;
        hitFlash = 0.25f;
        lastHurt = time;
        combo = 0;
        if (src != null) {
            float a = (float) Math.atan2(y - src.y, x - src.x);
            kbx += (float) Math.cos(a) * 220f;
            kby += (float) Math.sin(a) * 220f;
        }
        gp.floatText(x, y - 46, "-" + (int) taken, Gfx.RED);
        gp.fxHit(x, y, 0xFFFF6B6B);
        gp.shake(7);
        gp.audio.play("hurt");
        if (hp <= 0) {
            hp = 0;
            dead = true;
            gp.onPlayerDeath();
        }
    }

    public void heal(float amount) {
        if (dead) return;
        hp += amount;
        if (hp > maxHp) hp = maxHp;
        gp.floatText(x, y - 50, "+" + (int) amount, Gfx.GREEN);
        gp.fxRing(x, y, 70, Gfx.GREEN, 0.4f);
        gp.audio.play("heal");
    }

    public void addMp(float amount) {
        mp += amount;
        if (mp > maxMp) mp = maxMp;
    }

    public void draw(Canvas c, Gfx g) {
        float bob = moving ? (float) Math.sin(time * 13) * 2.4f : (float) Math.sin(time * 3) * 1.2f;
        boolean blink = invuln > 0.75f && ((int) (time * 16) % 2 == 0);
        int cls = save.classIndex;
        int color = classDef().color;
        int cloth = 0xFF2B2340;
        if (save.classIndex == 0) cloth = 0xFF5A2F32;
        if (save.classIndex == 1) cloth = 0xFF28335E;
        if (save.classIndex == 2) cloth = 0xFF1F3B2A;
        if (save.classIndex == 3) cloth = 0xFF3E3418;
        if (save.classIndex == 4) cloth = 0xFF3A3524;
        if (save.classIndex == 5) cloth = 0xFF2C3742;

        g.fill(0x33000000);
        c.drawOval(x - 27, y + 15, x + 23, y + 35, g.p);
        g.fill(0x33000000);
        c.drawOval(x - 19, y + 13, x + 19, y + 28, g.p);

        if (save.equippedId(Catalog.SLOT_BOOTS) != null) {
            Catalog.Item boots = save.equippedItem(Catalog.SLOT_BOOTS);
            int bc = Gfx.RARITY[boots.rarity];
            g.line(c, x - 9, y + 14, x - 9, y + 24, bc, 5);
            g.line(c, x + 9, y + 14, x + 9, y + 24, bc, 5);
        }

        if (buffTimer > 0) {
            g.circleStroke(c, x, y + 8, 34 + (float) Math.sin(time * 6) * 3, 0x88FFB13B, 3);
        }
        if (guardTimer > 0) {
            g.circleStroke(c, x, y + 8, 37 + (float) Math.sin(time * 8) * 2, 0xAA9FB4C7, 3);
        }
        g.circleStroke(c, x, y + 18, 28, Gfx.withAlpha(color, 90), 2);

        float lift = 5f;
        c.save();
        c.translate(x, y + bob - lift);
        c.rotate((float) Math.toDegrees(dir));

        float sway = (float) Math.sin(time * 4f) * 2.5f + (moving ? 2f : 0f);
        android.graphics.Path cape = new android.graphics.Path();
        cape.moveTo(-4, -8);
        cape.quadTo(-22 + sway, 8, -12 + sway, 22);
        cape.lineTo(-2, 12);
        cape.close();
        g.fill(Gfx.withAlpha(Gfx.mix(color, 0xFF000000, 0.55f), 150));
        c.drawPath(cape, g.p);

        int bodyCol = blink ? Gfx.mix(cloth, 0xFFFFFFFF, 0.4f) : cloth;
        int skin = 0xFFE8C39E;
        float swing = 1f - Math.max(0f, attackAnim) / 0.2f;
        float stride = moving && dash <= 0 ? (float) Math.sin(animMove * 15f) * 5f : 0f;
        int legColor = Gfx.mix(bodyCol, 0xFF080812, 0.28f);
        g.line(c, -7, 7, -8 + stride, 20, legColor, 6);
        g.line(c, 7, 7, 7 - stride, 20, legColor, 6);
        g.line(c, -8 + stride, 20, -3 + stride, 21, 0xFF3A302A, 4);
        g.line(c, 7 - stride, 20, 12 - stride, 21, 0xFF3A302A, 4);

        if (cls == 0) {
            g.fill(0xFF3A2340);
            c.drawOval(-20, -15, 20, 17, g.p);
            g.fill(color);
            c.drawRoundRect(-14, -17, 16, 15, 10, 10, g.p);
            g.circle(c, 8, -2 + bob * 0.3f, 10, skin);
            Catalog.Item helm = save.equippedItem(Catalog.SLOT_HELMET);
            if (helm != null) {
                g.circle(c, 8, -4, 11, Gfx.RARITY[helm.rarity]);
            } else {
                g.fill(0xFF3A2E24);
                c.drawOval(0, -12, 18, 4, g.p);
            }
            float wa = -0.9f + swing * 1.9f;
            int wc = weaponColor();
            c.save();
            c.rotate(wa);
            g.line(c, 10, 8, 40, 8, wc, 5);
            g.circle(c, 41, 8, 4, Gfx.mix(wc, 0xFFFFFFFF, 0.35f));
            c.restore();
        } else if (cls == 1) {
            g.fill(bodyCol);
            float[] pts = {-20, 15, 20, 15, 12, -12, -12, -12};
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(pts[0], pts[1]);
            path.lineTo(pts[2], pts[3]);
            path.lineTo(pts[4], pts[5]);
            path.lineTo(pts[6], pts[7]);
            path.close();
            c.drawPath(path, g.p);
            g.circle(c, 4, -12, 11, 0xFF3A2F55);
            g.circle(c, 11, -8, 7, skin);
            g.fill(color);
            c.drawOval(-7, -25, 12, -6, g.p);
            g.line(c, -4, 6, -16, 26, 0xFF8A6A3A, 4);
            g.circle(c, -17, 27, 7 + (float) Math.sin(time * 5) * 1.5f, color);
            g.circle(c, -17, 27, 3, 0xFFFFFFFF);
        } else if (cls == 2) {
            g.fill(bodyCol);
            c.drawOval(-16, -16, 16, 16, g.p);
            g.fill(0xFF243A2E);
            c.drawOval(-14, -20, 14, 10, g.p);
            g.circle(c, 9, -3, 9, skin);
            g.fill(0xFF1B2A22);
            c.drawOval(-2, -15, 18, 3, g.p);
            float wa = -0.7f + swing * 1.7f;
            c.save();
            c.rotate(wa);
            g.line(c, 6, 10, 30, 10, weaponColor(), 4);
            c.restore();
            c.save();
            c.rotate(-wa * 0.8f);
            g.line(c, 6, -10, 28, -10, weaponColor(), 4);
            c.restore();
        } else if (cls == 3) {
            g.fill(bodyCol);
            c.drawOval(-15, -14, 15, 15, g.p);
            g.fill(0xFF2F2A16);
            c.drawOval(-12, -19, 12, 8, g.p);
            g.circle(c, 8, -3, 9, skin);
            g.fill(0xFF6B5A2A);
            c.drawOval(-4, -14, 16, 2, g.p);
            g.line(c, -10, -6, -18, 8, 0xFF6B4A2A, 5);
            g.line(c, -16, 4, -13, -4, 0xFFE3B23C, 2);
            android.graphics.Path bow = new android.graphics.Path();
            bow.moveTo(12, -18);
            bow.quadTo(28, 0, 12, 18);
            g.stroke(0xFF9A6B3A, 3f);
            c.drawPath(bow, g.p);
            g.line(c, 12, -18, 12, 18, Gfx.withAlpha(0xFFFFFFFF, 140), 1.4f);
            if (attackAnim > 0) {
                g.line(c, -2, 0, 24, 0, 0xFFF2E3B0, 3);
                g.line(c, 24, 0, 20, -4, 0xFFF2E3B0, 2);
            }
        } else if (cls == 4) {
            g.fill(bodyCol);
            android.graphics.Path robe = new android.graphics.Path();
            robe.moveTo(-18, 18);
            robe.lineTo(18, 18);
            robe.lineTo(11, -12);
            robe.lineTo(-11, -12);
            robe.close();
            c.drawPath(robe, g.p);
            g.circle(c, 3, -13, 10, 0xFFE8D9B0);
            g.circle(c, 10, -9, 6, skin);
            g.fill(0xFFFFE9A8);
            c.drawOval(-7, -26, 10, -16, g.p);
            g.circleStroke(c, 2, -30, 8 + (float) Math.sin(time * 3) * 1.2f, Gfx.withAlpha(0xFFFFE9A8, 220), 2);
            g.line(c, -10, 4, -18, 26, 0xFF8A6A3A, 4);
            g.circle(c, -18, 27, 6 + (float) Math.sin(time * 5) * 1.2f, 0xFFFFE9A8);
            g.circle(c, -18, 27, 2.5f, 0xFFFFFFFF);
        } else {
            g.fill(bodyCol);
            c.drawOval(-18, -15, 18, 16, g.p);
            g.fill(0xFF1E2733);
            c.drawOval(-15, -20, 15, 8, g.p);
            g.circle(c, 8, -3, 9, skin);
            g.fill(0xFF38495C);
            c.drawRoundRect(-12, -19, 12, 1, 8, 6, g.p);
            g.fill(0xFF9FB4C7);
            c.drawOval(-4, -25, 8, -15, g.p);
            float kwa = -0.85f + swing * 1.9f;
            c.save();
            c.rotate(kwa);
            g.line(c, 8, 6, 38, 6, weaponColor(), 7);
            g.line(c, 10, 0, 10, 12, 0xFF7A5A2A, 4);
            c.restore();
            g.fill(0xFF53687C);
            c.drawOval(6, -14, 24, 10, g.p);
            g.circleStroke(c, 15, -2, 8, Gfx.withAlpha(0xFFD8E2F0, 160), 2);
        }
        g.line(c, -7, 2, 8, 2, Gfx.withAlpha(0xFFE7C46A, 150), 2f);
        g.circle(c, -5, -8, 2.2f, Gfx.withAlpha(0xFFFFFFFF, 110));
        c.restore();

        g.line(c, x - 10, y + 24, x - 6, y + 31, Gfx.withAlpha(0xFF000000, 60), 2);
        g.line(c, x + 10, y + 24, x + 6, y + 31, Gfx.withAlpha(0xFF000000, 60), 2);
        if (dash > 0) {
            g.circleStroke(c, x, y, 34, Gfx.withAlpha(color, 140), 2);
        }
    }

    private int weaponColor() {
        Catalog.Item w = save.equippedItem(Catalog.SLOT_WEAPON);
        if (w == null) return 0xFFBFBFBF;
        return Gfx.RARITY[w.rarity];
    }
}
