package com.mysticrealm;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

public class Gfx {
    public static final int BG = 0xFF0A0714;
    public static final int BG_DEEP = 0xFF060410;
    public static final int PANEL = 0xFF171125;
    public static final int PANEL_2 = 0xFF211A35;
    public static final int PANEL_3 = 0xFF2C2344;
    public static final int LINE = 0x33FFFFFF;
    public static final int TEXT = 0xFFF4EEE4;
    public static final int DIM = 0xFFA89CB8;
    public static final int GOLD = 0xFFE9C46A;
    public static final int ACCENT = 0xFF9B5DE5;
    public static final int GREEN = 0xFF43D17A;
    public static final int RED = 0xFFE5484D;
    public static final int BLUE = 0xFF4EA8DE;
    public static final int CYAN = 0xFF43E0D0;
    public static final int ORANGE = 0xFFF28C38;

    public static final int[] RARITY = {0xFF9AA0A6, 0xFF5BCB7A, 0xFF4EA8DE, 0xFFB07CFF, 0xFFF2A93B};
    public static final String[] RARITY_NAME = {"Common", "Uncommon", "Rare", "Epic", "Legendary"};

    public float W, H, u;

    public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint ps = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final RectF tmp = new RectF();

    public final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    public final Typeface normal = Typeface.create("sans-serif", Typeface.NORMAL);
    public final Typeface titleFont = Typeface.create("serif", Typeface.BOLD);

    public void setSize(float w, float h) {
        W = w;
        H = h;
        u = Math.max(0.68f, Math.min(2.8f, h / 720f));
    }

    public void resize(float w, float h) {
        setSize(w, h);
    }

    public static float clamp(float v, float a, float b) {
        return v < a ? a : (v > b ? b : v);
    }

    public static int clamp(int v, int a, int b) {
        return v < a ? a : (v > b ? b : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int mix(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ca = (int) lerp(aa, ba, t), cr = (int) lerp(ar, br, t);
        int cg = (int) lerp(ag, bg, t), cb = (int) lerp(ab, bb, t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    public static double dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void fill(int color) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
    }

    public void stroke(int color, float width) {
        ps.setShader(null);
        ps.setStyle(Paint.Style.STROKE);
        ps.setStrokeWidth(width);
        ps.setColor(color);
        ps.setStrokeCap(Paint.Cap.ROUND);
    }

    public void rect(Canvas c, float x, float y, float w, float h, int color) {
        fill(color);
        c.drawRect(x, y, x + w, y + h, p);
    }

    public void roundRect(Canvas c, float x, float y, float w, float h, float r, int color) {
        fill(color);
        c.drawRoundRect(x, y, x + w, y + h, r, r, p);
    }

    public void roundRectStroke(Canvas c, float x, float y, float w, float h, float r, int color, float width) {
        stroke(color, width);
        c.drawRoundRect(x, y, x + w, y + h, r, r, ps);
    }

    public void circle(Canvas c, float x, float y, float r, int color) {
        fill(color);
        c.drawCircle(x, y, r, p);
    }

    public void circleStroke(Canvas c, float x, float y, float r, int color, float width) {
        stroke(color, width);
        c.drawCircle(x, y, r, ps);
    }

    public void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
        stroke(color, width);
        c.drawLine(x1, y1, x2, y2, ps);
    }

    public void gradientRect(Canvas c, float x, float y, float w, float h, int top, int bottom) {
        p.setShader(new LinearGradient(x, y, x, y + h, top, bottom, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(255);
        c.drawRect(x, y, x + w, y + h, p);
        p.setShader(null);
    }

    public void vignette(Canvas c, float cx, float cy, float radius) {
        p.setShader(new RadialGradient(cx, cy, radius,
                0x00000000, 0x99000000, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        c.drawRect(cx - radius, cy - radius, cx + radius, cy + radius, p);
        p.setShader(null);
    }

    public void panel(Canvas c, float x, float y, float w, float h, int fillColor, int strokeColor) {
        roundRect(c, x, y, w, h, 10 * u, fillColor);
        if (strokeColor != 0) roundRectStroke(c, x, y, w, h, 10 * u, strokeColor, Math.max(1f, 1.2f * u));
    }

    public void text(Canvas c, String s, float x, float y, float size, int color, int align, boolean boldFont) {
        if (s == null) return;
        tp.setTextSize(size);
        tp.setTypeface(boldFont ? bold : normal);
        tp.setColor(color);
        tp.setTextAlign(align == 0 ? Paint.Align.LEFT : (align == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT));
        Paint.FontMetrics fm = tp.getFontMetrics();
        float by = y - (fm.ascent + fm.descent) / 2f;
        c.drawText(s, x, by, tp);
    }

    public void textShadow(Canvas c, String s, float x, float y, float size, int color, int align, boolean boldFont) {
        text(c, s, x + size * 0.06f, y + size * 0.08f, size, 0x99000000, align, boldFont);
        text(c, s, x, y, size, color, align, boldFont);
    }

    public float textWidth(String s, float size, boolean boldFont) {
        tp.setTextSize(size);
        tp.setTypeface(boldFont ? bold : normal);
        return tp.measureText(s == null ? "" : s);
    }

    public float wrap(Canvas c, String s, float x, float y, float maxWidth, float size, int color,
                      float lineGap, int align, boolean boldFont) {
        if (s == null || s.length() == 0) return y;
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        float cy = y;
        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            if (textWidth(candidate, size, boldFont) > maxWidth && line.length() > 0) {
                text(c, line.toString(), x, cy, size, color, align, boldFont);
                cy += size + lineGap;
                line.setLength(0);
                line.append(w);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            text(c, line.toString(), x, cy, size, color, align, boldFont);
        }
        return cy;
    }

    public void bar(Canvas c, float x, float y, float w, float h, float pct, int back, int front) {
        pct = clamp(pct, 0f, 1f);
        roundRect(c, x, y, w, h, h / 2f, back);
        if (pct > 0f) {
            roundRect(c, x + 1, y + 1, (w - 2) * pct, h - 2, (h - 2) / 2f, front);
        }
    }
}
