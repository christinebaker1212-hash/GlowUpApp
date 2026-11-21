package com.glowup.app;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Runs a session one set at a time, then shows the WORKOUT PASSED screen. */
final class WorkoutFlow {
    final MainActivity a;
    final State.Workout w;
    final String type;
    int item = 0, set = 0;
    long startedAt;
    final Handler h = new Handler(Looper.getMainLooper());

    WorkoutFlow(MainActivity a, State.Workout w, String type) {
        this.a = a;
        this.w = w;
        this.type = type;
    }

    void start() {
        startedAt = SystemClock.elapsedRealtime();
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showSet();
    }

    int totalSets() {
        int n = 0;
        for (State.Item it : w.items) n += it.target.length;
        return n;
    }

    int setsDone() {
        int n = 0;
        for (int i = 0; i < item; i++) n += w.items.get(i).target.length;
        return n + set;
    }

    void showSet() {
        State.Item it = w.items.get(item);
        Plan.Ex e = Plan.ex(it.ex);
        int target = it.target[set];
        LinearLayout col = a.scrollPage(false);

        TextView prog = Ui.text(a, String.format(Locale.US, "%s  •  EXERCISE %d/%d  •  SET %d/%d",
            w.tiny ? "TINY MODE" : Plan.RECOVERY.equals(type) ? "RECOVERY" : Science.DIFF[w.diff],
            item + 1, w.items.size(), set + 1, it.target.length), 12, Ui.DIM);
        col.addView(prog);
        Ui.XpBar bar = new Ui.XpBar(a, setsDone() / (float) totalSets());
        col.addView(bar, Ui.lp(a, 8));

        col.addView(Ui.text(a, e.cat >= 0 ? Science.CAT_LABEL[e.cat] : "WARM-UP / STRETCH", 13,
            e.cat >= 0 ? Ui.CAT_COLOR[e.cat] : Ui.DIM), Ui.lp(a, 18));
        col.addView(Ui.head(a, e.name.toUpperCase(Locale.US), 30, Ui.TEXT));
        col.addView(Ui.text(a, e.how, 15, Ui.TEXT), Ui.lp(a, 8));
        TextView why = Ui.text(a, "♀ " + e.why, 13, Ui.PINK);
        col.addView(why, Ui.lp(a, 8));

        LinearLayout box = Ui.card(a);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(Ui.text(a, "TARGET", 12, Ui.DIM));
        box.addView(Ui.head(a, (e.kind == Plan.HOLD ? Widgets.clock(target) : String.valueOf(target)) + " " + e.unit(), 26, Ui.GOLD));
        col.addView(box, Ui.lp(a, 16));

        if (e.kind == Plan.REPS) {
            col.addView(Ui.text(a, "How many did you do with good form?" + (e.side ? " (your weaker side)" : ""), 13, Ui.DIM), Ui.lp(a, 14));
            Widgets.Stepper st = new Widgets.Stepper(a, target, 1, 0, 300);
            col.addView(st, Ui.lp(a, 8));
            col.addView(Ui.button(a, "LOG SET", Ui.PINK, v -> log(st.value)), Ui.lp(a, 16));
        } else {
            int[] sides = new int[]{-1, -1};
            boolean twoSides = e.side;
            TextView sideLabel = Ui.head(a, twoSides ? "LEFT SIDE" : "", 16, Ui.CYAN);
            sideLabel.setGravity(Gravity.CENTER);
            if (twoSides) col.addView(sideLabel, Ui.lp(a, 12));
            Widgets.Watch watch = new Widgets.Watch(a, false, target, 0);
            col.addView(watch, Ui.lp(a, 8));
            TextView logBtn = Ui.button(a, "LOG " + Widgets.clock(target) + " (DONE IT)", Ui.PINK, null);
            Runnable finish = () -> {
                int got = twoSides ? Math.min(sides[0], sides[1]) : sides[0];
                log(got);
            };
            watch.onStop = () -> {
                if (twoSides && sides[0] < 0) {
                    sides[0] = watch.seconds;
                    sideLabel.setText("RIGHT SIDE (left: " + watch.seconds + "s)");
                    watch.time.setText("0");
                    watch.btn.setText("START RIGHT SIDE");
                    watch.btn.setBackground(Ui.round(Ui.GREEN, Ui.dp(a, 14)));
                    watch.btn.setTextColor(0xFF1A0F1F);
                } else {
                    if (twoSides) sides[1] = watch.seconds; else sides[0] = watch.seconds;
                    logBtn.setText("LOG " + Widgets.clock(twoSides ? Math.min(sides[0], sides[1]) : sides[0]));
                    logBtn.setOnClickListener(v -> finish.run());
                }
            };
            logBtn.setOnClickListener(v -> log(target));
            col.addView(logBtn, Ui.lp(a, 16));
            col.addView(Ui.text(a, "Press START, hold, press STOP when your form breaks. Or just log the target if you "
                + "timed it yourself.", 12, Ui.DIM), Ui.lp(a, 6));
        }
        if (e.cat >= 0) {
            col.addView(Ui.ghost(a, "COULDN'T DO THIS ONE", v -> {
                for (int i = set; i < it.target.length; i++) it.done[i] = 0;
                set = it.target.length - 1;
                next(false);
            }), Ui.lp(a, 12));
        } else {
            col.addView(Ui.ghost(a, "SKIP", v -> log(0)), Ui.lp(a, 12));
        }
        col.addView(Ui.text(a, "Stop right away if you feel chest pain, dizziness or sharp joint pain. A muscle burn "
            + "is normal; pain in a joint is not.", 11, Ui.DIM), Ui.lp(a, 18));
    }

    void log(double v) {
        State.Item it = w.items.get(item);
        it.done[set] = (int) Math.round(v);
        next(true);
    }

    void next(boolean rest) {
        State.Item it = w.items.get(item);
        Plan.Ex e = Plan.ex(it.ex);
        set++;
        if (set >= it.target.length) {
            set = 0;
            item++;
        }
        if (item >= w.items.size()) {
            finish();
            return;
        }
        Plan.Ex nx = Plan.ex(w.items.get(item).ex);
        if (rest && e.cat >= 0 && nx.cat >= 0) showRest(set == 0 ? 60 : 45, nx);
        else showSet();
    }

    void showRest(int secs, Plan.Ex nx) {
        LinearLayout col = a.scrollPage(false);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(Ui.space(a, 60));
        col.addView(Ui.head(a, "REST", 28, Ui.CYAN));
        Widgets.Watch cd = new Widgets.Watch(a, true, 0, secs);
        cd.btn.setVisibility(View.GONE);
        col.addView(cd, Ui.lp(a, 12));
        col.addView(Ui.text(a, "Breathe. Sip some water.", 14, Ui.DIM), Ui.lp(a, 4));
        col.addView(Ui.text(a, "UP NEXT", 12, Ui.DIM), Ui.lp(a, 30));
        col.addView(Ui.head(a, nx.name.toUpperCase(Locale.US), 22, Ui.TEXT));
        cd.onStop = this::showSet;
        cd.start();
        col.addView(Ui.button(a, "SKIP REST", Ui.PINK, v -> {
            cd.onStop = null;
            cd.running = false;
            showSet();
        }), Ui.lp(a, 30));
        col.addView(Ui.ghost(a, "+20 SECONDS", v -> {
            cd.onStop = null;
            cd.running = false;
            showRest(Math.max(0, secs - cd.seconds) + 20, nx);
        }), Ui.lp(a, 10));
    }

    void finish() {
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.durSec = (int) ((SystemClock.elapsedRealtime() - startedAt) / 1000);
        Logic.Result r = Logic.finishWorkout(a, a.st, w);
        LinearLayout col = a.scrollPage(false);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean recovery = Plan.RECOVERY.equals(type);
        String title = r.completion >= 0.9 ? "WORKOUT PASSED!" : r.completion >= 0.5 ? "WORKOUT COMPLETE" : "TOUGH ONE";
        int tc = r.completion >= 0.9 ? Ui.GOLD : r.completion >= 0.5 ? Ui.CYAN : Ui.ORANGE;
        col.addView(Ui.space(a, 20));
        TextView t = Ui.head(a, title, 40, tc);
        t.setGravity(Gravity.CENTER);
        t.setScaleX(0.3f);
        t.setScaleY(0.3f);
        t.animate().scaleX(1).scaleY(1).setDuration(450).start();
        col.addView(t);
        col.addView(Ui.text(a, String.format(Locale.US, "%d%% complete  •  %d min", Math.round(r.completion * 100),
            Math.max(1, w.durSec / 60)), 14, Ui.DIM), Ui.lp(a, 4));
        if (r.completion < 0.5) col.addView(Ui.text(a, "Showing up is the win. The plan adjusts to you, so tomorrow's "
            + "targets fit better.", 14, Ui.TEXT), Ui.lp(a, 8));
        Widgets.buzz(a, 120);

        TextView xpT = Ui.head(a, "+" + r.xp + " XP", 34, Ui.PINK);
        col.addView(xpT, Ui.lp(a, 18));
        int[] lvB = Science.level(a.st.xp - r.xp), lvA = Science.level(a.st.xp);
        Ui.XpBar xb = new Ui.XpBar(a, lvB[1] / (float) lvB[2]);
        col.addView(xb, Ui.lp(a, 8));
        TextView lvT = Ui.text(a, "LEVEL " + lvA[0], 13, Ui.DIM);
        col.addView(lvT, Ui.lp(a, 4));
        ValueAnimator va = ValueAnimator.ofFloat(lvB[1] / (float) lvB[2] + (lvA[0] - lvB[0]), lvA[0] - lvB[0] + lvA[1] / (float) lvA[2]);
        va.setDuration(1200);
        va.setStartDelay(400);
        float base = lvB[1] / (float) lvB[2];
        va.setFloatValues(base, (lvA[0] - lvB[0]) + lvA[1] / (float) lvA[2]);
        va.addUpdateListener(an -> {
            float v = (float) an.getAnimatedValue();
            xb.set(v - (float) Math.floor(v));
        });
        va.start();

        if (r.levelAfter > r.levelBefore) {
            TextView lu = Ui.head(a, "LEVEL UP!  " + r.levelAfter, 30, Ui.GOLD);
            lu.setGravity(Gravity.CENTER);
            lu.setBackground(Ui.round(0x33FFC94D, Ui.dp(a, 14)));
            lu.setPadding(Ui.dp(a, 16), Ui.dp(a, 10), Ui.dp(a, 16), Ui.dp(a, 10));
            col.addView(lu, Ui.lp(a, 16));
            h.postDelayed(() -> Widgets.buzz(a, 300), 1500);
        }
        if (!r.rankAfter.equals(r.rankBefore)) {
            boolean up = Science.overall(r.after) > Science.overall(r.before);
            TextView ru = Ui.head(a, (up ? "RANK UP: " : "RANK: ") + r.rankAfter, 22, up ? Ui.GREEN : Ui.ORANGE);
            ru.setGravity(Gravity.CENTER);
            col.addView(ru, Ui.lp(a, 12));
        }

        if (!recovery) {
            LinearLayout card = Ui.card(a);
            card.addView(Ui.text(a, "RATINGS", 12, Ui.DIM));
            for (int c = 0; c < 5; c++) {
                if (Math.abs(r.after[c] - r.before[c]) < 0.05) continue;
                LinearLayout row = Ui.hbox(a);
                row.addView(Ui.head(a, Science.CAT_LABEL[c], 14, Ui.TEXT), new LinearLayout.LayoutParams(Ui.dp(a, 86), -2));
                Ui.SegBar sb = new Ui.SegBar(a, (float) (r.after[c] / Science.MAX), Ui.CAT_COLOR[c]);
                sb.ghost = (float) (r.before[c] / Science.MAX);
                row.addView(sb, new LinearLayout.LayoutParams(0, Ui.dp(a, 12), 1));
                double d = r.after[c] - r.before[c];
                TextView dv = Ui.head(a, Ui.signed(d), 15, d >= 0 ? Ui.GREEN : Ui.RED);
                dv.setGravity(Gravity.END);
                row.addView(dv, new LinearLayout.LayoutParams(Ui.dp(a, 48), -2));
                card.addView(row, Ui.lp(a, 10));
            }
            double ob = Science.overall(r.before), oa = Science.overall(r.after);
            card.addView(Ui.text(a, String.format(Locale.US, "Fitness Rating %s → %s (%s)", Ui.fmt(ob), Ui.fmt(oa), Ui.signed(oa - ob)), 14, Ui.TEXT), Ui.lp(a, 12));
            col.addView(card, Ui.lp(a, 18));
        }
        for (String s : r.ach) {
            TextView at = Ui.head(a, "🏆 " + s, 16, Ui.GOLD);
            col.addView(at, Ui.lp(a, 10));
        }

        col.addView(Ui.text(a, "HOW DO YOU FEEL?", 12, Ui.DIM), Ui.lp(a, 22));
        LinearLayout feel = Ui.hbox(a);
        String[] faces = {"😫", "😕", "😐", "🙂", "🤩"};
        TextView[] fb = new TextView[5];
        for (int i = 0; i < 5; i++) {
            final int f = i + 1;
            fb[i] = Ui.text(a, faces[i], 30, Ui.TEXT);
            fb[i].setGravity(Gravity.CENTER);
            fb[i].setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
            fb[i].setOnClickListener(v -> {
                w.feel = f;
                Store.save(a, a.st);
                for (int j = 0; j < 5; j++) fb[j].setBackground(j == f - 1 ? Ui.round(Ui.CARD2, Ui.dp(a, 12)) : null);
            });
            feel.addView(fb[i], new LinearLayout.LayoutParams(0, -2, 1));
        }
        col.addView(feel, Ui.lp(a, 6));
        col.addView(Ui.button(a, "CONTINUE", Ui.PINK, v -> a.showTab(0)), Ui.lp(a, 20));
    }
}
