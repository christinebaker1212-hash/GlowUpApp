package com.glowup.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** A number stepper and a stopwatch / countdown, used by the tests and the workouts. */
final class Widgets {
    private Widgets() {}

    static void buzz(Context c, long ms) {
        try {
            Vibrator v = c.getSystemService(Vibrator.class);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }

    static void beep(int ms) {
        try {
            ToneGenerator t = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
            t.startTone(ToneGenerator.TONE_PROP_BEEP, ms);
            new Handler(Looper.getMainLooper()).postDelayed(t::release, ms + 300L);
        } catch (Exception ignored) {
        }
    }

    static String clock(int sec) {
        return sec >= 60 ? String.format(Locale.US, "%d:%02d", sec / 60, sec % 60) : String.valueOf(sec);
    }

    /** [ - ]  12  [ + ] */
    static final class Stepper extends LinearLayout {
        double value, step, min, max;
        final TextView num;
        Runnable onChange;

        Stepper(Context c, double value, double step, double min, double max) {
            super(c);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER);
            this.value = value;
            this.step = step;
            this.min = min;
            this.max = max;
            TextView minus = Ui.head(c, "−", 32, Ui.TEXT);
            TextView plus = Ui.head(c, "+", 32, Ui.TEXT);
            for (TextView b : new TextView[]{minus, plus}) {
                b.setGravity(Gravity.CENTER);
                b.setBackground(Ui.round(Ui.CARD2, Ui.dp(c, 36)));
            }
            num = Ui.head(c, "", 56, Ui.TEXT);
            num.setGravity(Gravity.CENTER);
            minus.setOnClickListener(v -> set(this.value - this.step));
            plus.setOnClickListener(v -> set(this.value + this.step));
            minus.setOnLongClickListener(v -> { set(this.value - this.step * 5); return true; });
            plus.setOnLongClickListener(v -> { set(this.value + this.step * 5); return true; });
            int s = Ui.dp(c, 64);
            addView(minus, new LayoutParams(s, s));
            addView(num, new LayoutParams(Ui.dp(c, 140), LayoutParams.WRAP_CONTENT));
            addView(plus, new LayoutParams(s, s));
            set(value);
        }

        void set(double v) {
            value = Math.max(min, Math.min(max, v));
            num.setText(value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value));
            if (onChange != null) onChange.run();
        }
    }

    /**
     * A stopwatch (counts up; beeps at the target, stops itself at the cap) or a countdown (cap only, counts down).
     */
    static final class Watch extends LinearLayout {
        final TextView time, btn;
        final boolean countdown;
        final int target, cap;
        int seconds;
        long startedAt;
        boolean running, beeped;
        Runnable onStop, onTick;
        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                int el = (int) ((SystemClock.elapsedRealtime() - startedAt) / 1000);
                seconds = el;
                if (!beeped && target > 0 && el >= target) {
                    beeped = true;
                    buzz(getContext(), 400);
                    beep(250);
                    time.setTextColor(Ui.GOLD);
                }
                if (countdown && cap - el <= 3 && cap - el > 0) beep(80);
                if (cap > 0 && el >= cap) {
                    seconds = cap;
                    stop();
                    return;
                }
                show();
                if (onTick != null) onTick.run();
                h.postDelayed(this, 200);
            }
        };

        Watch(Context c, boolean countdown, int target, int cap) {
            super(c);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            this.countdown = countdown;
            this.target = target;
            this.cap = cap;
            time = Ui.head(c, "", 72, Ui.TEXT);
            time.setGravity(Gravity.CENTER);
            addView(time);
            btn = Ui.button(c, "START", Ui.GREEN, v -> {
                if (running) stop(); else start();
            });
            addView(btn, Ui.lp(c, 8));
            show();
        }

        void show() {
            int shown = countdown ? Math.max(0, cap - seconds) : seconds;
            time.setText(clock(shown));
        }

        void start() {
            running = true;
            beeped = false;
            seconds = 0;
            startedAt = SystemClock.elapsedRealtime();
            time.setTextColor(Ui.TEXT);
            btn.setText(countdown ? "STOP EARLY" : "STOP");
            btn.setBackground(Ui.round(Ui.RED, Ui.dp(getContext(), 14)));
            btn.setTextColor(Ui.TEXT);
            beep(150);
            h.post(tick);
        }

        void stop() {
            running = false;
            h.removeCallbacks(tick);
            show();
            buzz(getContext(), 200);
            btn.setText("RESTART");
            btn.setBackground(Ui.round(Ui.CARD2, Ui.dp(getContext(), 14)));
            if (onStop != null) onStop.run();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            running = false;
            h.removeCallbacks(tick);
        }
    }
}
