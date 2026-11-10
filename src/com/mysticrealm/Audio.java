package com.mysticrealm;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class Audio {
    private static final int RATE = 22050;

    private final Context ctx;
    private final SoundPool pool;
    private final HashMap<String, Integer> sfx = new HashMap<>();
    private final Object lock = new Object();

    private MediaPlayer musicPlayer;
    private volatile int requestedTheme = -1;
    private int currentTheme = -1;
    private volatile boolean musicOn = true;
    private volatile boolean sfxOn = true;
    private volatile float musicVolume = 1f;
    private volatile float sfxVolume = 1f;
    private boolean paused;
    private Thread worker;

    public Audio(Context ctx) {
        this.ctx = ctx;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(14).setAudioAttributes(attrs).build();
        genSfx();
    }

    public void setMusicOn(boolean on) {
        musicOn = on;
        if (!on) {
            stopMusic();
        } else {
            int t = requestedTheme;
            requestedTheme = -2;
            music(t);
        }
    }

    public void setSfxOn(boolean on) {
        sfxOn = on;
    }

    public void setMusicVolume(float v) {
        musicVolume = v < 0f ? 0f : (v > 1f ? 1f : v);
        synchronized (lock) {
            if (musicPlayer != null) {
                try {
                    musicPlayer.setVolume(0.9f * musicVolume, 0.9f * musicVolume);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void setSfxVolume(float v) {
        sfxVolume = v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public boolean isMusicOn() {
        return musicOn;
    }

    public boolean isSfxOn() {
        return sfxOn;
    }

    public void play(String name) {
        if (!sfxOn) return;
        Integer id = sfx.get(name);
        if (id == null) return;
        float v = 0.7f * sfxVolume;
        pool.play(id, v, v, 1, 0, 1f);
    }

    public void play(String name, float volume) {
        if (!sfxOn) return;
        Integer id = sfx.get(name);
        if (id == null) return;
        float v = volume * sfxVolume;
        pool.play(id, v, v, 1, 0, 1f);
    }

    public void music(int theme) {
        if (!musicOn) {
            requestedTheme = theme;
            return;
        }
        if (theme == currentTheme && musicPlayer != null && !paused) return;
        requestedTheme = theme;
        synchronized (lock) {
            if (worker != null && worker.isAlive()) {
                return;
            }
            worker = new Thread(new Runnable() {
                public void run() {
                    musicWorker();
                }
            }, "music-synth");
            worker.start();
        }
    }

    private void musicWorker() {
        while (true) {
            int theme;
            synchronized (lock) {
                theme = requestedTheme;
                if (theme == -1) {
                    stopPlayerLocked();
                    currentTheme = -1;
                    worker = null;
                    return;
                }
                if (theme == currentTheme && musicPlayer != null) {
                    worker = null;
                    return;
                }
            }
            try {
                File f = ensureMusicFile(theme);
                synchronized (lock) {
                    if (requestedTheme != theme || !musicOn) {
                        continue;
                    }
                    stopPlayerLocked();
                    MediaPlayer mp = new MediaPlayer();
                    mp.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
                    mp.setDataSource(f.getAbsolutePath());
                    mp.setLooping(true);
                    mp.setVolume(0.9f * musicVolume, 0.9f * musicVolume);
                    mp.prepare();
                    mp.start();
                    musicPlayer = mp;
                    currentTheme = theme;
                    if (paused) mp.pause();
                    worker = null;
                    return;
                }
            } catch (Exception e) {
                synchronized (lock) {
                    worker = null;
                    return;
                }
            }
        }
    }

    private void stopPlayerLocked() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                musicPlayer.release();
            } catch (Exception ignored) {
            }
            musicPlayer = null;
        }
    }

    public void stopMusic() {
        synchronized (lock) {
            requestedTheme = -1;
            stopPlayerLocked();
            currentTheme = -1;
        }
    }

    public void pauseMusic() {
        synchronized (lock) {
            paused = true;
            if (musicPlayer != null) {
                try {
                    musicPlayer.pause();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void resumeMusic() {
        synchronized (lock) {
            paused = false;
            if (musicOn && musicPlayer != null) {
                try {
                    musicPlayer.start();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void shutdown() {
        stopMusic();
        try {
            pool.release();
        } catch (Exception ignored) {
        }
    }

    private void genSfx() {
        add("click", buildClick());
        add("attack", buildAttack());
        add("hit", buildHit());
        add("hurt", buildHurt());
        add("pickup", buildPickup());
        add("coin", buildCoin());
        add("level", buildLevel());
        add("skill", buildSkill());
        add("puzzle_ok", buildPuzzleOk());
        add("puzzle_bad", buildPuzzleBad());
        add("death", buildDeath());
        add("heal", buildHeal());
        add("dodge", buildDodge());
        add("deny", buildDeny());
        add("boss", buildBoss());
        add("win", buildWin());
    }

    private void add(String name, float[] data) {
        try {
            File f = new File(ctx.getCacheDir(), "sfx_" + name + ".wav");
            if (!f.exists() || f.length() < 44) {
                writeWav(f, data);
            }
            int id = pool.load(f.getAbsolutePath(), 1);
            sfx.put(name, id);
        } catch (Exception ignored) {
        }
    }

    private float[] buf(float seconds) {
        return new float[(int) (seconds * RATE)];
    }

    private void tone(float[] b, float start, float dur, float f0, float f1, float amp, int wave) {
        int s = (int) (start * RATE);
        int n = (int) (dur * RATE);
        if (s >= b.length) return;
        if (s + n > b.length) n = b.length - s;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float f = f0 + (f1 - f0) * t;
            phase += 2 * Math.PI * f / RATE;
            float v;
            switch (wave) {
                case 1:
                    v = Math.sin(phase) >= 0 ? 1f : -1f;
                    break;
                case 2:
                    v = (float) (2 * ((phase / (2 * Math.PI)) % 1.0) - 1);
                    break;
                case 3:
                    v = (float) (Math.random() * 2 - 1);
                    break;
                case 4:
                    v = (float) (Math.abs(((phase / (2 * Math.PI)) % 1.0) * 4 - 2) - 1);
                    break;
                default:
                    v = (float) Math.sin(phase);
            }
            float env;
            if (t < 0.05f) env = t / 0.05f;
            else env = (float) Math.pow(1 - t, 1.8);
            b[s + i] += v * amp * env;
        }
    }

    private float[] buildClick() {
        float[] b = buf(0.07f);
        tone(b, 0, 0.07f, 880, 880, 0.35f, 0);
        tone(b, 0, 0.04f, 1760, 1760, 0.12f, 0);
        return b;
    }

    private float[] buildAttack() {
        float[] b = buf(0.18f);
        tone(b, 0, 0.18f, 1400, 300, 0.4f, 3);
        tone(b, 0, 0.12f, 600, 200, 0.2f, 2);
        return b;
    }

    private float[] buildHit() {
        float[] b = buf(0.16f);
        tone(b, 0, 0.1f, 180, 60, 0.55f, 0);
        tone(b, 0, 0.12f, 3000, 900, 0.22f, 3);
        return b;
    }

    private float[] buildHurt() {
        float[] b = buf(0.24f);
        tone(b, 0, 0.24f, 320, 120, 0.4f, 2);
        tone(b, 0, 0.12f, 520, 240, 0.2f, 1);
        return b;
    }

    private float[] buildPickup() {
        float[] b = buf(0.2f);
        tone(b, 0, 0.08f, 660, 660, 0.3f, 0);
        tone(b, 0.07f, 0.12f, 990, 990, 0.3f, 0);
        return b;
    }

    private float[] buildCoin() {
        float[] b = buf(0.22f);
        tone(b, 0, 0.06f, 1046, 1046, 0.28f, 0);
        tone(b, 0.06f, 0.14f, 1568, 1568, 0.28f, 0);
        return b;
    }

    private float[] buildLevel() {
        float[] b = buf(0.6f);
        float[] notes = {523, 659, 784, 1046};
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * 0.09f, 0.25f, notes[i], notes[i], 0.3f, 0);
        }
        return b;
    }

    private float[] buildSkill() {
        float[] b = buf(0.35f);
        tone(b, 0, 0.3f, 300, 1500, 0.3f, 0);
        tone(b, 0.05f, 0.25f, 450, 2200, 0.15f, 4);
        return b;
    }

    private float[] buildPuzzleOk() {
        float[] b = buf(0.55f);
        float[] notes = {523, 659, 784, 1046};
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * 0.08f, 0.3f, notes[i], notes[i], 0.28f, 0);
            tone(b, i * 0.08f, 0.3f, notes[i] * 2, notes[i] * 2, 0.1f, 0);
        }
        return b;
    }

    private float[] buildPuzzleBad() {
        float[] b = buf(0.45f);
        float[] notes = {392, 330, 262, 196};
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * 0.09f, 0.2f, notes[i], notes[i] * 0.97f, 0.3f, 2);
        }
        return b;
    }

    private float[] buildDeath() {
        float[] b = buf(0.9f);
        tone(b, 0, 0.85f, 392, 60, 0.4f, 4);
        tone(b, 0.05f, 0.7f, 196, 40, 0.25f, 0);
        return b;
    }

    private float[] buildHeal() {
        float[] b = buf(0.4f);
        tone(b, 0, 0.18f, 520, 780, 0.22f, 0);
        tone(b, 0.1f, 0.28f, 780, 1040, 0.22f, 0);
        return b;
    }

    private float[] buildDodge() {
        float[] b = buf(0.2f);
        tone(b, 0, 0.2f, 2000, 500, 0.25f, 3);
        return b;
    }

    private float[] buildDeny() {
        float[] b = buf(0.2f);
        tone(b, 0, 0.18f, 160, 120, 0.35f, 1);
        return b;
    }

    private float[] buildBoss() {
        float[] b = buf(0.8f);
        tone(b, 0, 0.8f, 60, 40, 0.5f, 2);
        tone(b, 0, 0.5f, 90, 60, 0.25f, 1);
        tone(b, 0.1f, 0.5f, 120, 70, 0.2f, 3);
        return b;
    }

    private float[] buildWin() {
        float[] b = buf(1.4f);
        float[] notes = {523, 659, 784, 1046, 1318};
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * 0.12f, 0.5f, notes[i], notes[i], 0.24f, 0);
            tone(b, i * 0.12f, 0.5f, notes[i] * 0.5f, notes[i] * 0.5f, 0.1f, 4);
        }
        return b;
    }

    private static final int[] MUSIC_ROOT = {57, 60, 62, 55, 53, 64, 50};
    private static final int[] MUSIC_BPM = {76, 92, 82, 112, 88, 72, 132};
    private static final int[][] MUSIC_SCALE = {
            {0, 3, 5, 7, 10},
            {0, 2, 4, 7, 9},
            {0, 3, 5, 7, 10},
            {0, 1, 3, 5, 7, 8, 10},
            {0, 3, 5, 7, 10},
            {0, 2, 4, 7, 9},
            {0, 3, 5, 7, 10}
    };
    private static final int[][] MUSIC_CHORDS = {
            {0, -4, -9, -2},
            {0, -3, -5, -7},
            {0, -4, -9, -2},
            {0, -2, -4, -6},
            {0, -4, -9, -2},
            {0, -3, -5, -7},
            {0, -4, -9, -2}
    };

    private File ensureMusicFile(int theme) throws IOException {
        File f = new File(ctx.getCacheDir(), "music_" + theme + ".wav");
        if (f.exists() && f.length() > 2000) return f;
        float[] data = buildMusic(theme);
        writeWav(f, data);
        return f;
    }

    private float[] buildMusic(int theme) {
        int idx = Math.max(0, Math.min(6, theme));
        int bpm = MUSIC_BPM[idx];
        int root = MUSIC_ROOT[idx];
        int[] scale = MUSIC_SCALE[idx];
        int[] chords = MUSIC_CHORDS[idx];
        float beat = 60f / bpm;
        int beats = 32;
        float seconds = beat * beats + 1.6f;
        float[] b = buf(seconds);
        Random r = new Random(1000 + idx * 77);
        int bars = 8;
        for (int bar = 0; bar < bars; bar++) {
            int chordRoot = chords[bar % chords.length];
            float t0 = bar * 4 * beat;
            int[] chordNotes = {
                    root + chordRoot,
                    root + chordRoot + scale[2],
                    root + chordRoot + scale[4]
            };
            for (int cn : chordNotes) {
                tonePad(b, t0, 4 * beat, midi(cn), 0.05f);
                tonePad(b, t0, 4 * beat, midi(cn + 12), 0.02f);
            }
            for (int beatI = 0; beatI < 4; beatI++) {
                float bt = t0 + beatI * beat;
                tone(b, bt, beat * 0.9f, midi(root + chordRoot - 12), midi(root + chordRoot - 12), 0.09f, 4);
                if (beatI % 2 == 1) {
                    noiseHat(b, bt + beat * 0.5f, 0.03f, 0.018f);
                }
            }
            for (int step = 0; step < 8; step++) {
                if (r.nextInt(100) < 62) {
                    int deg = scale[r.nextInt(scale.length)];
                    int oct = (r.nextInt(100) < 25) ? 12 : 0;
                    float mt = t0 + step * beat * 0.5f;
                    float f = midi(root + 12 + deg + oct);
                    tone(b, mt, beat * 0.45f, f, f, 0.075f, 0);
                    int ds = (int) ((mt + beat * 0.75f) * RATE);
                    if (ds + 400 < b.length) {
                        for (int k = 0; k < 400; k++) {
                            b[ds + k] += (float) Math.sin(2 * Math.PI * f * k / RATE) * 0.02f * (1 - k / 400f);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = (float) Math.tanh(b[i] * 1.6f);
        }
        return b;
    }

    private void tonePad(float[] b, float start, float dur, float freq, float amp) {
        int s = (int) (start * RATE);
        int n = (int) (dur * RATE);
        if (s + n > b.length) n = b.length - s;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double ph = 2 * Math.PI * freq * i / RATE;
            float env = (float) (Math.min(1, t * 6) * Math.min(1, (1 - t) * 4));
            float v = (float) (Math.sin(ph) + 0.4 * Math.sin(ph * 2.001));
            b[s + i] += v * amp * env;
        }
    }

    private void noiseHat(float[] b, float start, float dur, float amp) {
        int s = (int) (start * RATE);
        int n = (int) (dur * RATE);
        if (s + n > b.length) n = b.length - s;
        Random r = new Random(s);
        for (int i = 0; i < n; i++) {
            float env = 1 - i / (float) n;
            b[s + i] += (r.nextFloat() * 2 - 1) * amp * env;
        }
    }

    private static float midi(int note) {
        return (float) (440.0 * Math.pow(2.0, (note - 69) / 12.0));
    }

    private void writeWav(File f, float[] data) throws IOException {
        int n = data.length;
        byte[] out = new byte[44 + n * 2];
        int len = n * 2;
        out[0] = 'R';
        out[1] = 'I';
        out[2] = 'F';
        out[3] = 'F';
        putInt(out, 4, 36 + len);
        out[8] = 'W';
        out[9] = 'A';
        out[10] = 'V';
        out[11] = 'E';
        out[12] = 'f';
        out[13] = 'm';
        out[14] = 't';
        out[15] = ' ';
        putInt(out, 16, 16);
        putShort(out, 20, 1);
        putShort(out, 22, 1);
        putInt(out, 24, RATE);
        putInt(out, 28, RATE * 2);
        putShort(out, 32, 2);
        putShort(out, 34, 16);
        out[36] = 'd';
        out[37] = 'a';
        out[38] = 't';
        out[39] = 'a';
        putInt(out, 40, len);
        for (int i = 0; i < n; i++) {
            float v = data[i];
            if (v > 1f) v = 1f;
            if (v < -1f) v = -1f;
            int s = (int) (v * 32767f);
            out[44 + i * 2] = (byte) (s & 0xFF);
            out[45 + i * 2] = (byte) ((s >> 8) & 0xFF);
        }
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(out);
        } finally {
            fos.close();
        }
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
        b[off + 2] = (byte) ((v >> 16) & 0xFF);
        b[off + 3] = (byte) ((v >> 24) & 0xFF);
    }

    private static void putShort(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
    }
}
