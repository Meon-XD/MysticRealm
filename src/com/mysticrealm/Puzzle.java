package com.mysticrealm;

import android.graphics.Canvas;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Puzzle {
    public static final String[] NAMES = {
            "Rangkaian Rune", "Kartu Ingatan", "Saklar Rune", "Geser Blok",
            "Elemen", "Labirin", "Teka-teki", "Waktu Tepat"
    };
    public static final String[] DESC = {
            "Ingat urutan rune lalu ulangi.",
            "Temukan semua pasangan kartu.",
            "Nyalakan semua saklar. Saklar saling memengaruhi.",
            "Geser kepingan hingga urut kembali.",
            "Pilih elemen yang mengalahkan target.",
            "Capai gerbang keluar labirin.",
            "Jawab teka-teki penjaga kuno.",
            "Hentikan jarum di zona emas."
    };

    public final Gameplay gp;
    private final Random rnd = new Random();

    public int type;
    public boolean done, solved, failed, usedHint;
    public int mistakes;
    public float t, closeT, timeLeft;
    public boolean timed;

    private int w, h;
    private float px, py, pw, ph;
    private float hintX, hintY, hintW, hintH;
    private float quitX, quitY, quitW, quitH;

    private int[] seq;
    private float runeClock;
    private boolean showingRunes;
    private int inputPos;

    private int[] cards;
    private boolean[] up, matched;
    private int firstCard, secondCard;
    private float peekT, mismatchT;

    private boolean[] switches;
    private float switchFlash;

    private int[] board;
    private int blank;
    private float blockFlash;

    private int elemRound, elemTarget;
    private float elemRevealT;

    private final int MW = 7, MH = 7;
    private boolean[][] wallV, wallH;
    private int mpx, mpy;
    private boolean[][] hintPath;
    private float pathT;

    private Catalog.Riddle riddle;
    private int[] optOrder;
    private boolean[] optGone;
    private int logicWrong;
    private float logicFlash;

    private float timingPos, timingDir, zoneStart, zoneW;
    private int timingHits;
    private float timingFlash, slowT;

    public Puzzle(Gameplay gp) {
        this.gp = gp;
    }

    public void open(int type) {
        this.type = type;
        done = solved = failed = usedHint = false;
        mistakes = 0;
        t = 0;
        closeT = 0;
        timed = false;
        timeLeft = 0;
        seq = null;
        cards = null;
        switches = null;
        board = null;
        hintPath = null;
        riddle = null;
        rnd.setSeed(System.nanoTime() + gp.stage * 131L + type * 977L);
        switch (type) {
            case 0: setupRune(); break;
            case 1: setupMemory(); break;
            case 2: setupSwitch(); break;
            case 3: setupBlock(); break;
            case 4: setupElement(); break;
            case 5: setupMaze(); break;
            case 6: setupLogic(); break;
            default: setupTiming(); break;
        }
    }

    private void setupRune() {
        seq = new int[4];
        for (int i = 0; i < 4; i++) seq[i] = rnd.nextInt(4);
        runeClock = -0.5f;
        showingRunes = true;
        inputPos = 0;
    }

    private void setupMemory() {
        cards = new int[8];
        for (int i = 0; i < 4; i++) {
            cards[i * 2] = i;
            cards[i * 2 + 1] = i;
        }
        for (int i = cards.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = cards[i];
            cards[i] = cards[j];
            cards[j] = tmp;
        }
        up = new boolean[8];
        matched = new boolean[8];
        firstCard = secondCard = -1;
        peekT = 0;
        mismatchT = 0;
        timed = true;
        timeLeft = 78;
    }

    private void setupSwitch() {
        switches = new boolean[4];
        int pattern = 1 + rnd.nextInt(14);
        for (int i = 0; i < 4; i++) switches[i] = ((pattern >> i) & 1) == 1;
        boolean allOn = true;
        for (boolean b : switches) if (!b) allOn = false;
        if (allOn) switches[0] = false;
        switchFlash = 0;
    }

    private void setupBlock() {
        board = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0};
        blank = 8;
        for (int i = 0; i < 40; i++) {
            int bx = blank % 3, by = blank / 3;
            int dir = rnd.nextInt(4);
            int nx = bx, ny = by;
            if (dir == 0) nx--;
            else if (dir == 1) nx++;
            else if (dir == 2) ny--;
            else ny++;
            if (nx < 0 || nx > 2 || ny < 0 || ny > 2) continue;
            int ni = ny * 3 + nx;
            board[blank] = board[ni];
            board[ni] = 0;
            blank = ni;
        }
        if (isBlockSolved()) {
            board = new int[]{2, 5, 3, 1, 0, 6, 4, 7, 8};
            blank = 4;
        }
        blockFlash = 0;
    }

    private void setupElement() {
        elemRound = 0;
        elemTarget = rnd.nextInt(3);
        elemRevealT = 0;
    }

    private void setupMaze() {
        wallV = new boolean[MH][MW - 1];
        wallH = new boolean[MH - 1][MW];
        for (int y = 0; y < MH; y++)
            for (int x = 0; x < MW - 1; x++) wallV[y][x] = true;
        for (int y = 0; y < MH - 1; y++)
            for (int x = 0; x < MW; x++) wallH[y][x] = true;
        boolean[][] seen = new boolean[MH][MW];
        ArrayList<int[]> stack = new ArrayList<>();
        stack.add(new int[]{0, 0});
        seen[0][0] = true;
        while (!stack.isEmpty()) {
            int[] cur = stack.get(stack.size() - 1);
            int cx = cur[0], cy = cur[1];
            ArrayList<int[]> nbrs = new ArrayList<>();
            if (cx > 0 && !seen[cy][cx - 1]) nbrs.add(new int[]{cx - 1, cy, 0});
            if (cx < MW - 1 && !seen[cy][cx + 1]) nbrs.add(new int[]{cx + 1, cy, 1});
            if (cy > 0 && !seen[cy - 1][cx]) nbrs.add(new int[]{cx, cy - 1, 2});
            if (cy < MH - 1 && !seen[cy + 1][cx]) nbrs.add(new int[]{cx, cy + 1, 3});
            if (nbrs.isEmpty()) {
                stack.remove(stack.size() - 1);
                continue;
            }
            int[] n = nbrs.get(rnd.nextInt(nbrs.size()));
            int nx = n[0], ny = n[1], dir = n[2];
            if (dir == 0) wallV[cy][nx] = false;
            else if (dir == 1) wallV[cy][cx] = false;
            else if (dir == 2) wallH[ny][cx] = false;
            else wallH[cy][cx] = false;
            seen[ny][nx] = true;
            stack.add(new int[]{nx, ny});
        }
        mpx = 0;
        mpy = 0;
        hintPath = null;
        pathT = 0;
        timed = true;
        timeLeft = 60;
    }

    private void setupLogic() {
        riddle = Catalog.RIDDLES[rnd.nextInt(Catalog.RIDDLES.length)];
        optOrder = new int[riddle.opts.length];
        for (int i = 0; i < optOrder.length; i++) optOrder[i] = i;
        for (int i = optOrder.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = optOrder[i];
            optOrder[i] = optOrder[j];
            optOrder[j] = tmp;
        }
        optGone = new boolean[optOrder.length];
        logicWrong = 0;
        logicFlash = 0;
    }

    private void setupTiming() {
        timingPos = 0.05f;
        timingDir = 1f;
        zoneW = 0.17f;
        zoneStart = 0.25f + rnd.nextFloat() * 0.5f;
        timingHits = 0;
        timingFlash = 0;
        slowT = 0;
        timed = true;
        timeLeft = 60;
    }

    public void update(float dt) {
        if (done) {
            closeT += dt;
            return;
        }
        t += dt;
        if (timed) {
            timeLeft -= dt;
            if (timeLeft <= 0) {
                fail();
                return;
            }
        }
        if (mismatchT > 0) {
            mismatchT -= dt;
            if (mismatchT <= 0) {
                if (firstCard >= 0) up[firstCard] = false;
                if (secondCard >= 0) up[secondCard] = false;
                firstCard = secondCard = -1;
            }
        }
        if (peekT > 0) peekT -= dt;
        if (elemRevealT > 0) elemRevealT -= dt;
        if (pathT > 0) pathT -= dt;
        if (logicFlash > 0) logicFlash -= dt;
        if (timingFlash > 0) timingFlash -= dt;
        if (switchFlash > 0) switchFlash -= dt;
        if (blockFlash > 0) blockFlash -= dt;
        if (slowT > 0) slowT -= dt;

        if (type == 0 && showingRunes) {
            runeClock += dt;
            if (runeClock > 4 * 0.68f) {
                showingRunes = false;
                inputPos = 0;
            }
        }
        if (type == 7) {
            float speed = 0.55f + timingHits * 0.14f;
            if (slowT > 0) speed *= 0.45f;
            timingPos += timingDir * speed * dt;
            if (timingPos > 1f) {
                timingPos = 1f;
                timingDir = -1f;
            }
            if (timingPos < 0f) {
                timingPos = 0f;
                timingDir = 1f;
            }
        }
    }

    public void pointerDown(float mx, float my) {
        if (done) return;
        if (mx >= hintX && mx <= hintX + hintW && my >= hintY && my <= hintY + hintH) {
            hint();
            return;
        }
        if (mx >= quitX && mx <= quitX + quitW && my >= quitY && my <= quitY + quitH) {
            fail();
            return;
        }
        switch (type) {
            case 0: tapRune(mx, my); break;
            case 1: tapMemory(mx, my); break;
            case 2: tapSwitch(mx, my); break;
            case 3: tapBlock(mx, my); break;
            case 4: tapElement(mx, my); break;
            case 5: tapMaze(mx, my); break;
            case 6: tapLogic(mx, my); break;
            default: tapTiming(mx, my); break;
        }
    }

    private void tapRune(float mx, float my) {
        if (showingRunes) return;
        for (int i = 0; i < 4; i++) {
            if (runePadContains(i, mx, my)) {
                if (seq[inputPos] == i) {
                    inputPos++;
                    gp.audio.play("puzzle_ok", 0.6f);
                    gp.fxRing(gp.player.x, gp.player.y, 40 + inputPos * 8, Gfx.CYAN, 0.3f);
                    if (inputPos >= seq.length) win();
                } else {
                    mistakes++;
                    gp.audio.play("puzzle_bad");
                    if (mistakes >= 3) {
                        fail();
                    } else {
                        inputPos = 0;
                        runeClock = -0.5f;
                        showingRunes = true;
                        gp.toast("Urutan salah, perhatikan lagi");
                    }
                }
                return;
            }
        }
    }

    private void tapMemory(float mx, float my) {
        if (mismatchT > 0) return;
        int idx = memoryCardAt(mx, my);
        if (idx < 0 || up[idx] || matched[idx]) return;
        up[idx] = true;
        gp.audio.play("click", 0.5f);
        if (firstCard < 0) {
            firstCard = idx;
        } else if (secondCard < 0) {
            secondCard = idx;
            if (cards[firstCard] == cards[secondCard]) {
                matched[firstCard] = true;
                matched[secondCard] = true;
                firstCard = secondCard = -1;
                gp.audio.play("puzzle_ok", 0.6f);
                boolean all = true;
                for (boolean b : matched) if (!b) all = false;
                if (all) win();
            } else {
                mistakes++;
                mismatchT = 0.65f;
                gp.audio.play("puzzle_bad", 0.6f);
                if (mistakes >= 3) fail();
            }
        }
    }

    private void tapSwitch(float mx, float my) {
        for (int i = 0; i < 4; i++) {
            if (switchContains(i, mx, my)) {
                toggleSwitch(i);
                gp.audio.play("click", 0.6f);
                boolean all = true;
                for (boolean b : switches) if (!b) all = false;
                if (all) win();
                return;
            }
        }
    }

    private void toggleSwitch(int i) {
        switches[i] = !switches[i];
        if (i > 0) switches[i - 1] = !switches[i - 1];
        if (i < 3) switches[i + 1] = !switches[i + 1];
        switchFlash = 0.2f;
    }

    private void tapBlock(float mx, float my) {
        int idx = blockCellAt(mx, my);
        if (idx < 0) return;
        int bx = blank % 3, by = blank / 3;
        int cx = idx % 3, cy = idx / 3;
        if (Math.abs(bx - cx) + Math.abs(by - cy) != 1) return;
        board[blank] = board[idx];
        board[idx] = 0;
        blank = idx;
        gp.audio.play("click", 0.5f);
        blockFlash = 0.15f;
        if (isBlockSolved()) win();
    }

    private void tapElement(float mx, float my) {
        if (elemRevealT > 0) return;
        for (int i = 0; i < 3; i++) {
            if (elementContains(i, mx, my)) {
                if (i == (elemTarget + 1) % 3) {
                    elemRound++;
                    gp.audio.play("puzzle_ok", 0.6f);
                    if (elemRound >= 3) {
                        win();
                    } else {
                        int nt = rnd.nextInt(3);
                        if (nt == elemTarget) nt = (nt + 1) % 3;
                        elemTarget = nt;
                    }
                } else {
                    mistakes++;
                    elemRevealT = 0.9f;
                    gp.audio.play("puzzle_bad");
                    if (mistakes >= 3) fail();
                }
                return;
            }
        }
    }

    private void tapMaze(float mx, float my) {
        int cx = mazeCellX(mx), cy = mazeCellY(my);
        if (cx < 0 || cy < 0) return;
        int dx = cx - mpx, dy = cy - mpy;
        if (Math.abs(dx) + Math.abs(dy) != 1) return;
        if (dx == 1 && wallV[mpy][mpx]) return;
        if (dx == -1 && wallV[mpy][cx]) return;
        if (dy == 1 && wallH[mpy][mpx]) return;
        if (dy == -1 && wallH[cy][mpx]) return;
        mpx = cx;
        mpy = cy;
        gp.audio.play("click", 0.35f);
        if (mpx == MW - 1 && mpy == MH - 1) win();
    }

    private void tapLogic(float mx, float my) {
        if (logicFlash > 0) return;
        int n = optOrder.length;
        for (int i = 0; i < n; i++) {
            if (optGone[i]) continue;
            if (logicOptionContains(i, mx, my)) {
                if (optOrder[i] == riddle.answer) {
                    win();
                } else {
                    mistakes++;
                    logicWrong++;
                    logicFlash = 0.7f;
                    gp.audio.play("puzzle_bad");
                    if (mistakes >= 3) fail();
                    else gp.toast("Jawaban salah");
                }
                return;
            }
        }
    }

    private void tapTiming(float mx, float my) {
        if (!timingBarContains(mx, my)) return;
        boolean hit = timingPos >= zoneStart && timingPos <= zoneStart + zoneW;
        timingFlash = 0.25f;
        if (hit) {
            timingHits++;
            gp.audio.play("puzzle_ok", 0.6f);
            if (timingHits >= 3) {
                win();
            } else {
                zoneW = Math.max(0.1f, zoneW - 0.022f);
                zoneStart = 0.15f + rnd.nextFloat() * 0.7f;
            }
        } else {
            mistakes++;
            gp.audio.play("puzzle_bad");
            if (mistakes >= 3) fail();
        }
    }

    private void hint() {
        if (done) return;
        usedHint = true;
        gp.audio.play("click", 0.7f);
        switch (type) {
            case 0:
                runeClock = -0.5f;
                showingRunes = true;
                inputPos = 0;
                break;
            case 1:
                peekT = 1.4f;
                break;
            case 2: {
                for (int mask = 0; mask < 16; mask++) {
                    boolean[] sim = switches.clone();
                    for (int i = 0; i < 4; i++) {
                        if ((mask & (1 << i)) != 0) {
                            sim[i] = !sim[i];
                            if (i > 0) sim[i - 1] = !sim[i - 1];
                            if (i < 3) sim[i + 1] = !sim[i + 1];
                        }
                    }
                    boolean all = true;
                    for (boolean b : sim) if (!b) all = false;
                    if (all) {
                        for (int i = 0; i < 4; i++) {
                            if ((mask & (1 << i)) != 0) {
                                toggleSwitch(i);
                                boolean all2 = true;
                                for (boolean b : switches) if (!b) all2 = false;
                                if (all2) win();
                                return;
                            }
                        }
                    }
                }
                break;
            }
            case 3: {
                int best = -1;
                int bestScore = Integer.MAX_VALUE;
                int[] bestBoard = board.clone();
                int bx = blank % 3, by = blank / 3;
                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : dirs) {
                    int nx = bx + d[0], ny = by + d[1];
                    if (nx < 0 || nx > 2 || ny < 0 || ny > 2) continue;
                    int ni = ny * 3 + nx;
                    int[] sim = board.clone();
                    sim[blank] = sim[ni];
                    sim[ni] = 0;
                    int score = manhattan(sim);
                    if (score < bestScore) {
                        bestScore = score;
                        best = ni;
                        bestBoard = sim;
                    }
                }
                if (best >= 0) {
                    board = bestBoard;
                    blank = best;
                    blockFlash = 0.3f;
                    gp.audio.play("click", 0.5f);
                    if (isBlockSolved()) win();
                }
                break;
            }
            case 4:
                elemRevealT = 1.5f;
                break;
            case 5:
                computeMazeHint();
                pathT = 3f;
                break;
            case 6: {
                int shown = 0;
                int n = optOrder.length;
                for (int i = 0; i < n && shown < 2; i++) {
                    if (optOrder[i] != riddle.answer && !optGone[i]) {
                        optGone[i] = true;
                        shown++;
                    }
                }
                logicFlash = 0.3f;
                break;
            }
            default:
                slowT = 3f;
                break;
        }
    }

    private void computeMazeHint() {
        hintPath = new boolean[MH][MW];
        int[][] prevX = new int[MH][MW];
        int[][] prevY = new int[MH][MW];
        for (int y = 0; y < MH; y++)
            for (int x = 0; x < MW; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        boolean[][] seen = new boolean[MH][MW];
        ArrayList<int[]> q = new ArrayList<>();
        q.add(new int[]{mpx, mpy});
        seen[mpy][mpx] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] cur = q.remove(0);
            int cx = cur[0], cy = cur[1];
            if (cx == MW - 1 && cy == MH - 1) break;
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || nx > MW - 1 || ny < 0 || ny > MH - 1 || seen[ny][nx]) continue;
                if (d[0] == 1 && wallV[cy][cx]) continue;
                if (d[0] == -1 && wallV[cy][nx]) continue;
                if (d[1] == 1 && wallH[cy][cx]) continue;
                if (d[1] == -1 && wallH[ny][cx]) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = cx;
                prevY[ny][nx] = cy;
                q.add(new int[]{nx, ny});
            }
        }
        int cx = MW - 1, cy = MH - 1;
        while (cx >= 0 && cy >= 0) {
            hintPath[cy][cx] = true;
            int nx = prevX[cy][cx];
            int ny = prevY[cy][cx];
            if (nx < 0) break;
            cx = nx;
            cy = ny;
        }
    }

    private void win() {
        if (done) return;
        solved = true;
        done = true;
        closeT = 0;
        gp.audio.play("puzzle_ok");
        gp.shake(4);
    }

    private void fail() {
        if (done) return;
        failed = true;
        done = true;
        closeT = 0;
        gp.audio.play("puzzle_bad");
        gp.shake(9);
    }

    private boolean isBlockSolved() {
        for (int i = 0; i < 8; i++) if (board[i] != i + 1) return false;
        return true;
    }

    private int manhattan(int[] b) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            if (b[i] == 0) continue;
            int goal = b[i] - 1;
            sum += Math.abs(i % 3 - goal % 3) + Math.abs(i / 3 - goal / 3);
        }
        return sum;
    }

    private void layout(int w, int h) {
        this.w = w;
        this.h = h;
        pw = Math.min(w - 100, 1000);
        ph = Math.min(h - 60, 720);
        px = (w - pw) / 2f;
        py = (h - ph) / 2f;
        float bwb = Math.min(pw * 0.24f, 190);
        hintW = bwb;
        hintH = 46;
        hintX = px + pw * 0.5f - bwb - 8;
        hintY = py + ph - 66;
        quitW = bwb;
        quitH = 46;
        quitX = px + pw * 0.5f + 8;
        quitY = py + ph - 66;
    }

    private float contentX() { return px + pw * 0.07f; }
    private float contentW() { return pw * 0.86f; }
    private float contentY() { return py + ph * 0.22f; }
    private float contentH() { return ph * 0.56f; }

    private float runePadSize() {
        float gap = pw * 0.02f;
        return Math.min((contentW() - gap * 3) / 4f, contentH() * 0.72f);
    }

    private float runePadX(int i) {
        float size = runePadSize();
        float gap = pw * 0.02f;
        float total = size * 4 + gap * 3;
        float sx = px + (pw - total) / 2f;
        return sx + i * (size + gap);
    }

    private float runePadY() {
        return contentY() + contentH() * 0.22f;
    }

    private boolean runePadContains(int i, float mx, float my) {
        float s = runePadSize();
        float x = runePadX(i);
        float y = runePadY();
        return mx >= x && mx <= x + s && my >= y && my <= y + s;
    }

    private float memCardW() {
        return (contentW() - pw * 0.06f) / 4f;
    }

    private float memCardH() {
        return memCardW() * 0.82f;
    }

    private float memX(int i) {
        int col = i % 4;
        float gap = pw * 0.02f;
        float total = memCardW() * 4 + gap * 3;
        float sx = px + (pw - total) / 2f;
        return sx + col * (memCardW() + gap);
    }

    private float memY(int i) {
        int row = i / 4;
        float gap = pw * 0.02f;
        float totalH = memCardH() * 2 + gap;
        float sy = contentY() + (contentH() - totalH) / 2f;
        return sy + row * (memCardH() + gap);
    }

    private int memoryCardAt(float mx, float my) {
        for (int i = 0; i < 8; i++) {
            float x = memX(i), y = memY(i);
            if (mx >= x && mx <= x + memCardW() && my >= y && my <= y + memCardH()) return i;
        }
        return -1;
    }

    private float swSize() {
        return Math.min(contentW() / 6.2f, contentH() * 0.7f);
    }

    private float swX(int i) {
        float s = swSize();
        float gap = pw * 0.05f;
        float total = s * 4 + gap * 3;
        float sx = px + (pw - total) / 2f;
        return sx + i * (s + gap);
    }

    private float swY() {
        return contentY() + contentH() * 0.3f;
    }

    private boolean switchContains(int i, float mx, float my) {
        float s = swSize();
        float x = swX(i), y = swY();
        return mx >= x && mx <= x + s && my >= y && my <= y + s;
    }

    private float blockSize() {
        return Math.min(contentW() * 0.5f, contentH() * 0.8f);
    }

    private float blockX() {
        return px + (pw - blockSize()) / 2f;
    }

    private float blockY() {
        return contentY() + (contentH() - blockSize()) / 2f;
    }

    private int blockCellAt(float mx, float my) {
        float s = blockSize() / 3f;
        float x = blockX(), y = blockY();
        if (mx < x || my < y || mx >= x + blockSize() || my >= y + blockSize()) return -1;
        return (int) ((my - y) / s) * 3 + (int) ((mx - x) / s);
    }

    private float elemSize() {
        return Math.min(contentW() / 5.2f, contentH() * 0.5f);
    }

    private float elemX(int i) {
        float s = elemSize();
        float gap = pw * 0.05f;
        float total = s * 3 + gap * 2;
        float sx = px + (pw - total) / 2f;
        return sx + i * (s + gap);
    }

    private float elemY() {
        return contentY() + contentH() * 0.42f;
    }

    private boolean elementContains(int i, float mx, float my) {
        float s = elemSize();
        float x = elemX(i), y = elemY();
        return mx >= x && mx <= x + s && my >= y && my <= y + s;
    }

    private float mazeSize() {
        return Math.min(contentW() * 0.62f, contentH() * 0.92f);
    }

    private float mazeX() {
        return px + (pw - mazeSize()) / 2f;
    }

    private float mazeY() {
        return contentY() + (contentH() - mazeSize()) / 2f;
    }

    private float mazeCell() {
        return mazeSize() / MW;
    }

    private int mazeCellX(float mx) {
        float x = mazeX();
        if (mx < x || mx >= x + mazeSize()) return -1;
        return (int) ((mx - x) / mazeCell());
    }

    private int mazeCellY(float my) {
        float y = mazeY();
        if (my < y || my >= y + mazeSize()) return -1;
        return (int) ((my - y) / mazeCell());
    }

    private float logicOptH() {
        return Math.min(64, contentH() * 0.19f);
    }

    private float logicOptX(int i) {
        float gap = pw * 0.03f;
        float ww = (contentW() - gap) / 2f;
        return contentX() + (i % 2) * (ww + gap);
    }

    private float logicOptY(int i) {
        float gap = pw * 0.018f;
        return contentY() + contentH() * 0.52f + (i / 2) * (logicOptH() + gap);
    }

    private boolean logicOptionContains(int i, float mx, float my) {
        float x = logicOptX(i), y = logicOptY(i);
        float ww = (contentW() - pw * 0.03f) / 2f;
        return mx >= x && mx <= x + ww && my >= y && my <= y + logicOptH();
    }

    private float timingZoneX() {
        return contentX() + timingPos * contentW();
    }

    private boolean timingBarContains(float mx, float my) {
        float barY = contentY() + contentH() * 0.45f;
        return mx >= contentX() && mx <= contentX() + contentW() && my >= barY - 50 && my <= barY + 50;
    }

    public void draw(Canvas c, Gfx g, int w, int h) {
        layout(w, h);
        g.rect(c, 0, 0, w, h, 0xC8100B1E);
        g.panel(c, px, py, pw, ph, 0xFF1B1430, 0x33E9C46A);
        g.text(c, "TEKA-TEKI", px + pw / 2f, py + 30, 14, Gfx.GOLD, 1, true);
        g.text(c, NAMES[type], px + pw / 2f, py + 56, 26, Gfx.TEXT, 1, true);
        g.text(c, DESC[type], px + pw / 2f, py + 82, 13, Gfx.DIM, 1, false);

        if (timed) {
            float tw = pw * 0.34f;
            g.bar(c, px + pw / 2f - tw / 2f, py + 102, tw, 7, timeLeft / 60f, 0x66201633,
                    timeLeft < 12 ? Gfx.RED : Gfx.CYAN);
        }

        switch (type) {
            case 0: drawRune(c, g); break;
            case 1: drawMemory(c, g); break;
            case 2: drawSwitch(c, g); break;
            case 3: drawBlock(c, g); break;
            case 4: drawElement(c, g); break;
            case 5: drawMaze(c, g); break;
            case 6: drawLogic(c, g); break;
            default: drawTiming(c, g); break;
        }

        g.panel(c, hintX, hintY, hintW, hintH, 0xFF2A2140, Gfx.withAlpha(Gfx.GOLD, 120));
        g.text(c, "PETUNJUK", hintX + hintW / 2f, hintY + hintH / 2f, 14, Gfx.GOLD, 1, true);
        g.panel(c, quitX, quitY, quitW, quitH, 0xFF2A2140, Gfx.withAlpha(Gfx.RED, 110));
        g.text(c, "MENYERAH", quitX + quitW / 2f, quitY + quitH / 2f, 14, 0xFFF08A8A, 1, true);

        String mk = "";
        for (int i = 0; i < 3; i++) mk += i < mistakes ? "\u2716 " : "\u25CB ";
        g.text(c, mk, px + pw - 36, py + 30, 15, mistakes > 0 ? Gfx.RED : Gfx.DIM, 2, true);
        if (usedHint) g.text(c, "Hadiah berkurang", px + 30, py + 30, 12, Gfx.DIM, 0, false);
    }

    private void drawRune(Canvas c, Gfx g) {
        float s = runePadSize();
        int active = -1;
        if (showingRunes && runeClock >= 0) {
            int step = (int) (runeClock / 0.68f);
            float local = runeClock % 0.68f;
            if (step < 4 && local < 0.46f) active = seq[step];
        }
        for (int i = 0; i < 4; i++) {
            float x = runePadX(i);
            float y = runePadY();
            boolean act = active == i;
            g.roundRect(c, x, y, s, s, 12, act ? Gfx.withAlpha(Gfx.CYAN, 80) : 0xFF231A3A);
            g.roundRectStroke(c, x, y, s, s, 12,
                    act ? Gfx.CYAN : (mistakes > 0 ? Gfx.withAlpha(Gfx.RED, 90) : 0x44FFFFFF), act ? 3 : 1.4f);
            drawRuneIcon(c, g, x + s / 2f, y + s / 2f, s * 0.28f, i, act ? 0xFFFFFFFF : Gfx.CYAN);
        }
        if (showingRunes) {
            g.text(c, "Perhatikan...", px + pw / 2f, contentY() + contentH() * 0.06f, 18, Gfx.GOLD, 1, true);
        } else {
            g.text(c, "Ulangi " + inputPos + "/4", px + pw / 2f, contentY() + contentH() * 0.06f, 16, Gfx.DIM, 1, false);
        }
    }

    private void drawRuneIcon(Canvas c, Gfx g, float x, float y, float r, int id, int color) {
        Path p = new Path();
        if (id == 0) {
            p.moveTo(x, y - r);
            p.lineTo(x + r, y);
            p.lineTo(x, y + r);
            p.lineTo(x - r, y);
            p.close();
        } else if (id == 1) {
            p.moveTo(x, y - r);
            p.lineTo(x + r, y + r);
            p.lineTo(x - r, y + r);
            p.close();
        } else if (id == 2) {
            g.line(c, x - r, y - r, x + r, y + r, color, 3 + r * 0.08f);
            g.line(c, x + r, y - r, x - r, y + r, color, 3 + r * 0.08f);
            g.circleStroke(c, x, y, r * 0.55f, color, 2);
            return;
        } else {
            g.circleStroke(c, x, y, r, color, 3);
            g.circle(c, x, y, r * 0.3f, color);
            return;
        }
        g.fill(color);
        c.drawPath(p, g.p);
    }

    private void drawMemory(Canvas c, Gfx g) {
        int[] symColors = {0xFFE9C46A, 0xFF4EA8DE, 0xFF43D17A, 0xFFB07CFF};
        for (int i = 0; i < 8; i++) {
            float x = memX(i), y = memY(i);
            float ww = memCardW(), hh = memCardH();
            boolean reveal = up[i] || matched[i] || peekT > 0;
            if (matched[i]) {
                g.roundRect(c, x, y, ww, hh, 8, 0xFF1B1730);
                g.roundRectStroke(c, x, y, ww, hh, 8, Gfx.withAlpha(Gfx.GREEN, 90), 1);
                g.circle(c, x + ww / 2f, y + hh / 2f, ww * 0.16f, Gfx.withAlpha(symColors[cards[i]], 100));
            } else if (reveal) {
                g.roundRect(c, x, y, ww, hh, 8, 0xFF2C2344);
                g.roundRectStroke(c, x, y, ww, hh, 8, Gfx.withAlpha(symColors[cards[i]], 200), 2);
                drawMemSymbol(c, g, x + ww / 2f, y + hh / 2f, ww * 0.26f, cards[i], symColors[cards[i]]);
            } else {
                g.roundRect(c, x, y, ww, hh, 8, 0xFF231A3A);
                g.roundRectStroke(c, x, y, ww, hh, 8, 0x33FFFFFF, 1.4f);
                g.text(c, "?", x + ww / 2f, y + hh / 2f, ww * 0.34f, Gfx.withAlpha(Gfx.GOLD, 180), 1, true);
            }
        }
        g.text(c, "Pasangan: " + matchedCount() + "/4", px + pw / 2f, contentY() + contentH() * 0.02f, 14, Gfx.DIM, 1, false);
    }

    private int matchedCount() {
        int n = 0;
        for (int i = 0; i < 8; i++) if (matched[i] && i % 2 == 0) n++;
        int pairs = 0;
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 8; i++) {
            if (matched[i] && !seen[cards[i]]) {
                seen[cards[i]] = true;
                pairs++;
            }
        }
        return pairs;
    }

    private void drawMemSymbol(Canvas c, Gfx g, float x, float y, float r, int id, int color) {
        if (id == 0) {
            g.circle(c, x, y, r, color);
        } else if (id == 1) {
            g.roundRect(c, x - r, y - r, r * 2, r * 2, 4, color);
        } else if (id == 2) {
            Path p = new Path();
            p.moveTo(x, y - r);
            p.lineTo(x + r, y + r);
            p.lineTo(x - r, y + r);
            p.close();
            g.fill(color);
            c.drawPath(p, g.p);
        } else {
            g.line(c, x - r, y, x + r, y, color, 5);
            g.line(c, x, y - r, x, y + r, color, 5);
        }
    }

    private void drawSwitch(Canvas c, Gfx g) {
        float s = swSize();
        for (int i = 0; i < 4; i++) {
            float x = swX(i), y = swY();
            boolean on = switches[i];
            g.roundRect(c, x, y, s, s, 10, on ? Gfx.withAlpha(Gfx.CYAN, 40) : 0xFF201832);
            g.roundRectStroke(c, x, y, s, s, 10,
                    on ? Gfx.CYAN : 0x44FFFFFF, on ? 3 : 1.3f);
            float cx = x + s / 2f, cy = y + s / 2f;
            g.circle(c, cx, cy, s * 0.3f, on ? Gfx.withAlpha(Gfx.CYAN, 80) : 0xFF2C2344);
            drawRuneIcon(c, g, cx, cy, s * 0.18f, i, on ? Gfx.CYAN : 0xFF6B5E80);
            g.text(c, on ? "ON" : "OFF", cx, y + s + 16, 13, on ? Gfx.CYAN : Gfx.DIM, 1, true);
        }
        g.text(c, "Saklar menyala: " + countOn() + "/4", px + pw / 2f, contentY() + contentH() * 0.06f, 16, Gfx.DIM, 1, false);
    }

    private int countOn() {
        int n = 0;
        for (boolean b : switches) if (b) n++;
        return n;
    }

    private void drawBlock(Canvas c, Gfx g) {
        float s = blockSize() / 3f;
        float bx = blockX(), by = blockY();
        g.roundRect(c, bx - 6, by - 6, blockSize() + 12, blockSize() + 12, 10, 0xFF140E24);
        for (int i = 0; i < 9; i++) {
            int v = board[i];
            float x = bx + (i % 3) * s;
            float y = by + (i / 3) * s;
            if (v == 0) {
                g.roundRectStroke(c, x + 3, y + 3, s - 6, s - 6, 8, Gfx.withAlpha(Gfx.GOLD, 90), 2);
                continue;
            }
            boolean good = v == i + 1;
            g.roundRect(c, x + 3, y + 3, s - 6, s - 6, 8, good ? 0xFF2E3A52 : 0xFF2C2344);
            g.roundRectStroke(c, x + 3, y + 3, s - 6, s - 6, 8,
                    good ? Gfx.withAlpha(Gfx.GREEN, 140) : 0x44FFFFFF, 1.4f);
            g.text(c, "" + v, x + s / 2f, y + s / 2f, s * 0.36f, Gfx.TEXT, 1, true);
        }
        g.text(c, "Susun 1 - 8", px + pw / 2f, contentY() + contentH() * 0.02f, 14, Gfx.DIM, 1, false);
    }

    private void drawElement(Canvas c, Gfx g) {
        float s = elemSize();
        int[] cols = {0xFFF26B3A, 0xFF4EA8DE, 0xFF43D17A};
        String[] names = {"API", "AIR", "ALAM"};
        float ty = contentY() + contentH() * 0.02f;
        g.text(c, "Target:", px + pw / 2f, ty, 14, Gfx.DIM, 1, false);
        float tr = Math.min(34, s * 0.4f);
        float tyy = ty + tr + 22;
        g.circle(c, px + pw / 2f, tyy, tr + 8, Gfx.withAlpha(cols[elemTarget], 55));
        drawElementIcon(c, g, px + pw / 2f, tyy, tr, elemTarget);
        for (int i = 0; i < 3; i++) {
            float x = elemX(i), y = elemY();
            boolean good = elemRevealT > 0 && i == (elemTarget + 1) % 3;
            g.roundRect(c, x, y, s, s, 12, Gfx.withAlpha(cols[i], good ? 60 : 26));
            g.roundRectStroke(c, x, y, s, s, 12, good ? Gfx.GOLD : Gfx.withAlpha(cols[i], 170), good ? 3 : 1.6f);
            drawElementIcon(c, g, x + s / 2f, y + s / 2f - 8, s * 0.28f, i);
            g.text(c, names[i], x + s / 2f, y + s - 20, 13, Gfx.TEXT, 1, true);
        }
        g.text(c, "Ronde " + Math.min(elemRound + 1, 3) + "/3  -  air > api > alam > air",
                px + pw / 2f, contentY() + contentH() * 0.95f, 12, Gfx.DIM, 1, false);
    }

    private void drawElementIcon(Canvas c, Gfx g, float x, float y, float r, int id) {
        if (id == 0) {
            Path p = new Path();
            p.moveTo(x, y - r);
            p.quadTo(x + r, y - r * 0.1f, x + r * 0.6f, y + r * 0.5f);
            p.quadTo(x, y + r, x - r * 0.6f, y + r * 0.5f);
            p.quadTo(x - r, y - r * 0.1f, x, y - r);
            g.fill(0xFFF26B3A);
            c.drawPath(p, g.p);
            g.fill(0xFFFFC46B);
            c.drawOval(x - r * 0.3f, y - r * 0.1f, x + r * 0.3f, y + r * 0.5f, g.p);
        } else if (id == 1) {
            Path p = new Path();
            p.moveTo(x, y - r);
            p.quadTo(x + r * 0.9f, y + r * 0.1f, x, y + r);
            p.quadTo(x - r * 0.9f, y + r * 0.1f, x, y - r);
            g.fill(0xFF4EA8DE);
            c.drawPath(p, g.p);
            g.circle(c, x - r * 0.2f, y + r * 0.15f, r * 0.14f, 0xFFBFE6FF);
        } else {
            Path p = new Path();
            p.moveTo(x - r, y + r * 0.6f);
            p.quadTo(x - r * 0.6f, y - r, x + r, y - r * 0.9f);
            p.quadTo(x + r * 0.2f, y - r * 0.1f, x - r, y + r * 0.6f);
            g.fill(0xFF43D17A);
            c.drawPath(p, g.p);
            g.line(c, x - r * 0.9f, y + r * 0.5f, x + r * 0.8f, y - r * 0.6f, 0xFF2A8F52, 2);
        }
    }

    private void drawMaze(Canvas c, Gfx g) {
        float ms = mazeSize();
        float cs = mazeCell();
        float mx0 = mazeX(), my0 = mazeY();
        g.roundRect(c, mx0 - 8, my0 - 8, ms + 16, ms + 16, 8, 0xFF140E24);
        for (int y = 0; y < MH; y++) {
            for (int x = 0; x < MW; x++) {
                float x0 = mx0 + x * cs, y0 = my0 + y * cs;
                if (hintPath != null && pathT > 0 && hintPath[y][x]) {
                    g.rect(c, x0 + 4, y0 + 4, cs - 8, cs - 8, Gfx.withAlpha(Gfx.CYAN, 55));
                }
                if (x == MW - 1 && y == MH - 1) {
                    g.rect(c, x0 + 4, y0 + 4, cs - 8, cs - 8, Gfx.withAlpha(Gfx.GOLD, 45));
                    g.text(c, "\u2726", x0 + cs / 2f, y0 + cs / 2f, cs * 0.5f, Gfx.GOLD, 1, true);
                }
                if (x == mpx && y == mpy) {
                    g.circle(c, x0 + cs / 2f, y0 + cs / 2f, cs * 0.28f, Gfx.ACCENT);
                    g.circle(c, x0 + cs / 2f, y0 + cs / 2f, cs * 0.14f, 0xFFFFFFFF);
                }
            }
        }
        for (int y = 0; y < MH; y++) {
            for (int x = 0; x < MW - 1; x++) {
                if (wallV[y][x]) {
                    float x0 = mx0 + (x + 1) * cs;
                    float y0 = my0 + y * cs;
                    g.line(c, x0, y0, x0, y0 + cs, 0xFF8B7BB0, 2.2f);
                }
            }
        }
        for (int y = 0; y < MH - 1; y++) {
            for (int x = 0; x < MW; x++) {
                if (wallH[y][x]) {
                    float x0 = mx0 + x * cs;
                    float y0 = my0 + (y + 1) * cs;
                    g.line(c, x0, y0, x0 + cs, y0, 0xFF8B7BB0, 2.2f);
                }
            }
        }
        g.roundRectStroke(c, mx0, my0, ms, ms, 4, 0x66FFFFFF, 2);
    }

    private void drawLogic(Canvas c, Gfx g) {
        float y = contentY();
        y = g.wrap(c, riddle.q, px + pw / 2f, y + 8, contentW() * 0.92f, 17, Gfx.TEXT, 5, 1, false);
        g.text(c, logicWrong > 0 ? "Percobaan salah: " + logicWrong + "/3" : "",
                px + pw / 2f, contentY() + contentH() * 0.46f, 12, Gfx.DIM, 1, false);
        float ww = (contentW() - pw * 0.03f) / 2f;
        for (int i = 0; i < optOrder.length; i++) {
            if (optGone[i]) continue;
            float x = logicOptX(i), yy = logicOptY(i);
            g.roundRect(c, x, yy, ww, logicOptH(), 8, 0xFF251C3C);
            g.roundRectStroke(c, x, yy, ww, logicOptH(), 8, 0x44FFFFFF, 1.3f);
            g.text(c, riddle.opts[optOrder[i]], x + ww / 2f, yy + logicOptH() / 2f, 15, Gfx.TEXT, 1, false);
        }
    }

    private void drawTiming(Canvas c, Gfx g) {
        float barY = contentY() + contentH() * 0.45f;
        float barH = 34;
        float bx = contentX(), bw = contentW();
        g.roundRect(c, bx, barY - barH / 2f, bw, barH, 10, 0xFF140E24);
        float zx = bx + zoneStart * bw;
        float zw = zoneW * bw;
        g.roundRect(c, zx, barY - barH / 2f, zw, barH, 10, Gfx.withAlpha(Gfx.GOLD, 90));
        g.roundRectStroke(c, zx, barY - barH / 2f, zw, barH, 10, Gfx.GOLD, 2);
        for (int i = 1; i < 10; i++) {
            g.line(c, bx + bw * i / 10f, barY - barH / 2f + 4, bx + bw * i / 10f, barY + barH / 2f - 4,
                    0x22FFFFFF, 1);
        }
        float mx = bx + timingPos * bw;
        int mc = timingFlash > 0 ? 0xFFFFFFFF : Gfx.CYAN;
        g.rect(c, mx - 4, barY - barH / 2f - 16, 8, barH + 32, mc);
        g.circle(c, mx, barY - barH / 2f - 16, 6, mc);
        for (int i = 0; i < 3; i++) {
            g.circle(c, px + pw / 2f - 34 + i * 34, barY + barH / 2f + 34, 8,
                    i < timingHits ? Gfx.GOLD : 0xFF3A2E55);
        }
        g.text(c, "Ketuk saat jarum di zona emas", px + pw / 2f, contentY() + contentH() * 0.04f, 14, Gfx.DIM, 1, false);
        if (slowT > 0) g.text(c, "Waktu melambat", px + pw / 2f, barY + barH / 2f + 66, 12, Gfx.CYAN, 1, false);
    }

    public boolean isClosing() {
        return done && closeT > 0.45f;
    }
}
