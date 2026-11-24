package com.glowup.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Colours, little builders and the drawn widgets (stat bars, XP bar, charts). */
final class Ui {
    private Ui() {}

    // (PINK / PURPLE are the accent colours: in TRAINING ARC they're electric orange and blue)
    static final boolean M = Flavor.MALE;
    static final int BG = M ? 0xFF070A12 : 0xFF0D0912, CARD = M ? 0xFF111827 : 0xFF1A1324,
        CARD2 = M ? 0xFF1B2538 : 0xFF261B35, LINE = M ? 0xFF2C3A55 : 0xFF3A2C4E,
        PINK = M ? 0xFFFF7A1A : 0xFFFF4FA3, PURPLE = M ? 0xFF3D7BFF : 0xFF9B5CFF, GOLD = 0xFFFFC94D,
        TEXT = M ? 0xFFF2F5FF : 0xFFF7F0FF, DIM = M ? 0xFF8C9AB8 : 0xFFA898BF,
        GREEN = 0xFF4CE0A0, RED = 0xFFFF5A6E, CYAN = 0xFF4FD8FF, ORANGE = 0xFFFF9A4D;
    static final int[] CAT_COLOR = M ? new int[]{CYAN, 0xFFFF7A1A, 0xFFFF4D6A, 0xFFB78CFF, GREEN} : new int[]{CYAN, ORANGE, PINK, PURPLE, GREEN};
    static final int[] DIFF_COLOR = {GREEN, CYAN, ORANGE, RED};

    static final Typeface HEAD = Typeface.create("sans-serif-condensed", Flavor.MALE ? Typeface.BOLD_ITALIC : Typeface.BOLD);
    static final Typeface BODY = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(BODY);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    static TextView head(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(HEAD);
        t.setLetterSpacing(0.04f);
        return t;
    }

    static TextView bold(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(BOLD);
        return t;
    }

    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(round(CARD, dp(c, 16)));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, top);
        return p;
    }

    static LinearLayout.LayoutParams weight(Context c, float w, int sideMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
        p.leftMargin = dp(c, sideMargin);
        p.rightMargin = dp(c, sideMargin);
        return p;
    }

    static TextView button(Context c, String label, int color, View.OnClickListener l) {
        TextView b = head(c, label, 17, color == GOLD || color == GREEN || color == CYAN ? 0xFF1A0F1F : TEXT);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(color, dp(c, 14)));
        int ph = dp(c, 14);
        b.setPadding(ph, dp(c, 13), ph, dp(c, 13));
        b.setOnClickListener(l);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static TextView ghost(Context c, String label, View.OnClickListener l) {
        TextView b = head(c, label, 15, DIM);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = round(0, dp(c, 14));
        g.setStroke(dp(c, 1.5f), LINE);
        b.setBackground(g);
        b.setPadding(dp(c, 12), dp(c, 11), dp(c, 12), dp(c, 11));
        b.setOnClickListener(l);
        return b;
    }

    static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    static String fmt(double v) {
        return String.valueOf(Math.round(v));
    }

    static String signed(double v) {
        long r = Math.round(v);
        return (r >= 0 ? "+" : "") + r;
    }

    // ---------------- drawn widgets ----------------

    /** A drawn widget with a fixed height (and optionally width), whatever layout params it is added with. */
    abstract static class Fixed extends View {
        final int wantW, wantH;

        Fixed(Context c, int wDp, int hDp) {
            super(c);
            wantW = wDp > 0 ? dp(c, wDp) : 0;
            wantH = dp(c, hDp);
        }

        @Override
        protected void onMeasure(int ws, int hs) {
            int w = wantW > 0 ? wantW : MeasureSpec.getSize(ws);
            setMeasuredDimension(w, wantH);
        }
    }

    /** A segmented bar like the stat bars in open-world games: 10 blocks, partly filled. */
    static final class SegBar extends Fixed {
        float value; // 0..1
        float ghost = -1; // previous value (shows growth)
        int color;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        SegBar(Context c, float value, int color) {
            super(c, 0, 12);
            this.value = value;
            this.color = color;
        }

        @Override
        protected void onDraw(Canvas cv) {
            int n = 10;
            float gap = dp(getContext(), 3);
            float w = (getWidth() - gap * (n - 1)) / n, h = getHeight();
            float r = dp(getContext(), 2);
            for (int i = 0; i < n; i++) {
                float x = i * (w + gap);
                p.setColor(0xFF2C2238);
                cv.drawRoundRect(new RectF(x, 0, x + w, h), r, r, p);
                float fill = Math.max(0, Math.min(1, value * n - i));
                if (ghost >= 0 && ghost < value) {
                    float g = Math.max(0, Math.min(1, ghost * n - i));
                    if (fill > g) {
                        p.setColor(GOLD);
                        cv.drawRoundRect(new RectF(x, 0, x + w * fill, h), r, r, p);
                    }
                    fill = g;
                }
                if (fill > 0) {
                    p.setColor(color);
                    cv.drawRoundRect(new RectF(x, 0, x + w * fill, h), r, r, p);
                }
            }
        }
    }

    /** The XP bar. */
    static final class XpBar extends Fixed {
        float value;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        XpBar(Context c, float value) {
            super(c, 0, 14);
            this.value = value;
        }

        void set(float v) {
            value = v;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float h = getHeight(), w = getWidth(), r = h / 2;
            p.setShader(null);
            p.setColor(0xFF2C2238);
            cv.drawRoundRect(new RectF(0, 0, w, h), r, r, p);
            float fw = Math.max(h, w * Math.max(0, Math.min(1, value)));
            p.setShader(new LinearGradient(0, 0, fw, 0, PURPLE, PINK, Shader.TileMode.CLAMP));
            if (value > 0) cv.drawRoundRect(new RectF(0, 0, fw, h), r, r, p);
            p.setShader(null);
        }
    }

    /** A ring (steps). */
    static final class Ring extends Fixed {
        float value;
        int color;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Ring(Context c, float value, int color, int sizeDp) {
            super(c, sizeDp, sizeDp);
            this.value = value;
            this.color = color;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = dp(getContext(), 9);
            RectF r = new RectF(s / 2, s / 2, getWidth() - s / 2, getHeight() - s / 2);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xFF2C2238);
            cv.drawArc(r, 0, 360, false, p);
            p.setColor(value >= 1 ? GOLD : color);
            cv.drawArc(r, -90, 360 * Math.min(1, value), false, p);
        }
    }

    /** A line chart of several series over days. */
    static final class LineChart extends Fixed {
        static final class Series {
            int color;
            float width;
            List<float[]> pts = new ArrayList<>(); // {x, y}
        }

        final List<Series> series = new ArrayList<>();
        float[] bands; // horizontal guide values
        String[] bandLabels;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineChart(Context c, int heightDp) {
            super(c, 0, heightDp);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (Series s : series) for (float[] q : s.pts) {
                minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]);
                minY = Math.min(minY, q[1]); maxY = Math.max(maxY, q[1]);
            }
            p.setTextSize(dp(getContext(), 10));
            if (minX == Float.MAX_VALUE) {
                p.setColor(DIM);
                cv.drawText("Your progress appears here.", dp(getContext(), 8), getHeight() / 2f, p);
                return;
            }
            if (maxX - minX < 6) minX = maxX - 6;
            float pad = (maxY - minY) * 0.15f + 20;
            minY -= pad;
            maxY += pad;
            float left = dp(getContext(), 34), w = getWidth() - left - dp(getContext(), 6),
                top = dp(getContext(), 6), h = getHeight() - top - dp(getContext(), 6);
            p.setStyle(Paint.Style.FILL);
            if (bands != null) for (int i = 0; i < bands.length; i++) {
                if (bands[i] < minY || bands[i] > maxY) continue;
                float y = top + h - (bands[i] - minY) / (maxY - minY) * h;
                p.setColor(0xFF2C2238);
                cv.drawRect(left, y, left + w, y + 1.5f, p);
                p.setColor(DIM);
                cv.drawText(bandLabels[i], 0, y + dp(getContext(), 3), p);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
            for (Series s : series) {
                p.setColor(s.color);
                p.setStrokeWidth(dp(getContext(), s.width));
                Path path = new Path();
                boolean first = true;
                for (float[] q : s.pts) {
                    float x = left + (q[0] - minX) / (maxX - minX) * w;
                    float y = top + h - (q[1] - minY) / (maxY - minY) * h;
                    if (first) path.moveTo(x, y); else path.lineTo(x, y);
                    first = false;
                }
                if (s.pts.size() == 1) {
                    float[] q = s.pts.get(0);
                    float x = left + (q[0] - minX) / (maxX - minX) * w;
                    float y = top + h - (q[1] - minY) / (maxY - minY) * h;
                    cv.drawCircle(x, y, dp(getContext(), 3), p);
                }
                cv.drawPath(path, p);
            }
            p.setStyle(Paint.Style.FILL);
        }
    }

    /** Bars (steps per day) with a goal line. */
    static final class Bars extends Fixed {
        int[] values = new int[0];
        int goal;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Bars(Context c, int heightDp) {
            super(c, 0, heightDp);
        }

        @Override
        protected void onDraw(Canvas cv) {
            int max = Math.max(goal, 1000);
            for (int v : values) max = Math.max(max, v);
            int n = Math.max(1, values.length);
            float gap = dp(getContext(), 2), w = (getWidth() - gap * (n - 1)) / n, h = getHeight();
            for (int i = 0; i < values.length; i++) {
                float bh = h * values[i] / (float) max;
                p.setColor(values[i] >= goal ? GOLD : CYAN);
                float x = i * (w + gap);
                cv.drawRoundRect(new RectF(x, h - bh, x + w, h), dp(getContext(), 2), dp(getContext(), 2), p);
            }
            p.setColor(0x88FFFFFF);
            float gy = h - h * goal / (float) max;
            cv.drawRect(0, gy, getWidth(), gy + 2, p);
        }
    }
}
