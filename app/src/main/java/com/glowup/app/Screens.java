package com.glowup.app;

import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** STATS, HEALTH and ME. */
final class Screens {
    private Screens() {}

    static final String[] TIPS = {
        "Glutes grow from progressive overload: a little more each week (reps, a harder version, a slower lowering). The app does this for you.",
        "You can't burn fat from one spot. A smaller-looking waist comes from building the glutes and hips around it, a strong core, good posture and overall fat loss.",
        "Protein builds the muscle your workouts ask for: roughly 1.2–1.6 g per kg of body weight a day, spread over your meals.",
        "Sleep is when muscle repairs. 7–9 hours makes every workout count for more.",
        "Sore the day after? That's normal early on and fades within 2–3 days. Sharp or joint pain is not: back off.",
        "Never miss twice. One missed day is life; two is the start of a new habit. TINY MODE exists for exactly this.",
        "Posture changes how you look today: shoulders back and down, chest lifted, ribs stacked over hips.",
        "Walking after meals helps steady blood sugar. Even 10 minutes counts toward your steps.",
        "Strength training 2+ days a week builds bone density, keeps your metabolism up and makes everyday life easier.",
        "Your hip flexors tighten from sitting. Stretching them lets your glutes squeeze fully in every bridge.",
        "Slow the lowering part of each rep (3 seconds). It's harder, safer and builds more muscle.",
        "Water before, during and after. Thirsty muscles tire faster.",
        "Consistency beats intensity. A NORMAL workout every day you planned beats one INSANE workout a week.",
        "Squeeze at the top of every glute rep for a full second. That's where the shaping happens.",
        "Balance fades with age if you don't train it, and comes back fast if you do. Ten seconds on one leg is a great goal for life.",
        "Fitness compounds. In 6 months you'll warm up with what nearly beat you on day one.",
        "Breathe out on the hard part of the rep, in on the easy part. Don't hold your breath.",
        "Rest days aren't lazy days: they're when your body adapts. Light walking helps recovery.",
        "A harder version of an exercise unlocks automatically once you can do 8 clean reps of it.",
        "Hit a plateau? Pick HARD for a week, or retest: the plan recalibrates to you.",
    };

    static String height(State s, double cm) {
        if (s.imperial) {
            int in = (int) Math.round(cm / 2.54);
            return in / 12 + "'" + in % 12 + "\"";
        }
        return Math.round(cm) + " cm";
    }

    static String len(State s, double cm) {
        return s.imperial ? String.format(Locale.US, "%.1f in", cm / 2.54) : String.format(Locale.US, "%.1f cm", cm);
    }

    static String mass(State s, double kg) {
        return s.imperial ? String.format(Locale.US, "%.1f lb", kg / 0.45359237) : String.format(Locale.US, "%.1f kg", kg);
    }

    // ---------------- STATS ----------------

    static void stats(MainActivity a) {
        State s = a.st;
        LinearLayout col = a.scrollPage(true);
        col.addView(Ui.head(a, "STATS", 30, Ui.TEXT));

        LinearLayout ch = Ui.card(a);
        ch.addView(Ui.text(a, "FITNESS RATING OVER TIME", 12, Ui.DIM));
        Ui.LineChart lc = new Ui.LineChart(a, 200);
        for (int c = 0; c < 5; c++) {
            Ui.LineChart.Series se = new Ui.LineChart.Series();
            se.color = (Ui.CAT_COLOR[c] & 0x00FFFFFF) | 0xAA000000;
            se.width = 1.5f;
            for (State.Snap sn : s.snaps) se.pts.add(new float[]{LocalDate.parse(sn.date).toEpochDay(), (float) sn.elo[c]});
            lc.series.add(se);
        }
        Ui.LineChart.Series ov = new Ui.LineChart.Series();
        ov.color = Ui.TEXT;
        ov.width = 3;
        for (State.Snap sn : s.snaps) ov.pts.add(new float[]{LocalDate.parse(sn.date).toEpochDay(), (float) Science.overall(sn.elo)});
        lc.series.add(ov);
        lc.bands = new float[]{400, 800, 1200, 1600, 2000, 2400, 2800};
        lc.bandLabels = new String[]{"400", "800", "1200", "1600", "2000", "2400", "2800"};
        ch.addView(lc, Ui.lp(a, 8));
        LinearLayout leg = Ui.hbox(a), leg2 = Ui.hbox(a);
        leg.addView(Ui.text(a, "■ ALL   ", 11, Ui.TEXT));
        for (int c = 0; c < 5; c++) (c < 2 ? leg : leg2).addView(Ui.text(a, "■ " + Science.CAT_LABEL[c] + "   ", 11, Ui.CAT_COLOR[c]));
        ch.addView(leg, Ui.lp(a, 6));
        ch.addView(leg2, Ui.lp(a, 2));
        col.addView(ch, Ui.lp(a, 14));

        // numbers
        int workouts = 0, minutes = 0;
        for (State.Workout w : s.workouts) {
            if (!Plan.RECOVERY.equals(w.type)) workouts++;
            minutes += w.durSec / 60;
        }
        LinearLayout nums = Ui.hbox(a);
        nums.setBaselineAligned(false);
        String[][] cells = {
            {String.valueOf(workouts), "workouts"}, {String.valueOf(minutes), "minutes"},
            {String.valueOf(Math.max(s.bestStreak, Logic.streakNow(a, s))), "best streak"},
            {s.ach.size() + "/" + Logic.ACH.length, "trophies"},
        };
        for (String[] cell : cells) {
            LinearLayout cc = Ui.card(a);
            cc.setGravity(Gravity.CENTER_HORIZONTAL);
            cc.setPadding(Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12));
            cc.addView(Ui.head(a, cell[0], 22, Ui.TEXT));
            cc.addView(Ui.text(a, cell[1], 11, Ui.DIM));
            nums.addView(cc, Ui.weight(a, 1, 3));
        }
        col.addView(nums, Ui.lp(a, 12));

        // steps
        LinearLayout sc = Ui.card(a);
        sc.addView(Ui.text(a, "STEPS, LAST 30 DAYS", 12, Ui.DIM));
        Ui.Bars bars = new Ui.Bars(a, 110);
        LocalDate today = LocalDate.now();
        bars.values = new int[30];
        for (int i = 0; i < 30; i++) bars.values[i] = Steps.on(a, today.minusDays(29 - i).toString());
        bars.goal = Logic.goalOn(a, s, today);
        sc.addView(bars, Ui.lp(a, 8));
        sc.addView(Ui.text(a, String.format(Locale.US, "7-day average %,d  •  today's goal %,d", Steps.avg(a, today, 7), bars.goal), 12, Ui.DIM), Ui.lp(a, 6));
        col.addView(sc, Ui.lp(a, 12));

        // recent workouts
        LinearLayout wc = Ui.card(a);
        wc.addView(Ui.text(a, "RECENT WORKOUTS", 12, Ui.DIM));
        if (s.workouts.isEmpty()) wc.addView(Ui.text(a, "None yet. Your first one is waiting on the TRAIN tab.", 14, Ui.TEXT), Ui.lp(a, 6));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d");
        for (int i = s.workouts.size() - 1; i >= 0 && i >= s.workouts.size() - 12; i--) {
            State.Workout w = s.workouts.get(i);
            LinearLayout r = Ui.hbox(a);
            r.addView(Ui.text(a, LocalDate.parse(w.date).format(df), 13, Ui.DIM), new LinearLayout.LayoutParams(Ui.dp(a, 56), -2));
            String nm = Plan.RECOVERY.equals(w.type) ? "RECOVERY" : (w.tiny ? "TINY " : Science.DIFF[w.diff] + " ") + w.type;
            r.addView(Ui.bold(a, nm, 13, Plan.RECOVERY.equals(w.type) ? Ui.GREEN : Ui.DIFF_COLOR[w.diff]), new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(Ui.text(a, Math.round(w.completion * 100) + "%  +" + w.xp, 13, Ui.TEXT));
            wc.addView(r, Ui.lp(a, 8));
        }
        col.addView(wc, Ui.lp(a, 12));

        // tests
        LinearLayout tc = Ui.card(a);
        tc.addView(Ui.text(a, "FITNESS TESTS", 12, Ui.DIM));
        String[][] rows = {
            {Science.T_SITRISE, "Sit & rise (0-10)"}, {Science.T_BALANCE, "One-leg stand (s)"},
            {Science.T_CHAIR, "Chair stands (30 s)"}, {Science.T_PUSHUP, "Push-ups (full equiv.)"},
            {Science.T_BRIDGE, "Glute bridges"}, {Science.T_PLANK, "Plank (forearm equiv. s)"},
            {Science.T_MARCH, "2-min march (knee lifts)"}, {Science.T_COOPER, "12-min run (" + (s.imperial ? "miles)" : "metres)")},
        };
        int from = Math.max(0, s.tests.size() - 3);
        LinearLayout hdr = Ui.hbox(a);
        hdr.addView(Ui.text(a, "", 12, Ui.DIM), new LinearLayout.LayoutParams(0, -2, 1));
        for (int i = from; i < s.tests.size(); i++) {
            TextView d = Ui.text(a, LocalDate.parse(s.tests.get(i).date).format(df), 12, Ui.DIM);
            d.setGravity(Gravity.END);
            hdr.addView(d, new LinearLayout.LayoutParams(Ui.dp(a, 62), -2));
        }
        tc.addView(hdr, Ui.lp(a, 6));
        for (String[] row : rows) {
            LinearLayout r = Ui.hbox(a);
            r.addView(Ui.text(a, row[1], 13, Ui.TEXT), new LinearLayout.LayoutParams(0, -2, 1));
            for (int i = from; i < s.tests.size(); i++) {
                Double v = s.tests.get(i).raw.get(row[0]);
                if (v != null && row[0].equals(Science.T_COOPER)) v = v <= 0 ? null : s.imperial ? Math.round(v / 1609.344 * 100) / 100.0 : (double) Math.round(v);
                TextView tv = Ui.bold(a, v == null ? "-" : (v == Math.rint(v) ? String.valueOf(v.longValue()) : String.format(Locale.US, "%.1f", v)), 13, i == s.tests.size() - 1 ? Ui.GOLD : Ui.TEXT);
                tv.setGravity(Gravity.END);
                r.addView(tv, new LinearLayout.LayoutParams(Ui.dp(a, 62), -2));
            }
            tc.addView(r, Ui.lp(a, 6));
        }
        tc.addView(Ui.ghost(a, "TAKE A RETEST NOW", v -> new TestFlow(a, false).start()), Ui.lp(a, 12));
        col.addView(tc, Ui.lp(a, 12));

        // trophies
        LinearLayout ac = Ui.card(a);
        ac.addView(Ui.text(a, "TROPHIES", 12, Ui.DIM));
        for (String[] t : Logic.ACH) {
            boolean got = s.ach.contains(t[0]);
            LinearLayout r = Ui.hbox(a);
            r.addView(Ui.text(a, got ? "🏆" : "🔒", 16, Ui.TEXT), new LinearLayout.LayoutParams(Ui.dp(a, 32), -2));
            LinearLayout tx = Ui.vbox(a);
            tx.addView(Ui.head(a, t[1], 14, got ? Ui.GOLD : Ui.DIM));
            tx.addView(Ui.text(a, t[2], 12, Ui.DIM));
            r.addView(tx);
            ac.addView(r, Ui.lp(a, 8));
        }
        col.addView(ac, Ui.lp(a, 12));
    }

    // ---------------- HEALTH ----------------

    static void health(MainActivity a) {
        State s = a.st;
        LocalDate today = LocalDate.now();
        double age = Science.age(s.birth, today);
        LinearLayout col = a.scrollPage(true);
        col.addView(Ui.head(a, "HEALTH", 30, Ui.TEXT));
        col.addView(Ui.text(a, "Your habits and body markers, with targets for your age (" + (int) age + ").", 14, Ui.DIM), Ui.lp(a, 4));

        int strengthDays = 0, minutes = 0;
        java.util.Set<String> days = new java.util.HashSet<>();
        for (State.Workout w : s.workouts) {
            LocalDate d = LocalDate.parse(w.date);
            if (ChronoUnit.DAYS.between(d, today) < 7) {
                minutes += w.durSec / 60;
                if (!Plan.RECOVERY.equals(w.type)) days.add(w.date);
            }
        }
        strengthDays = days.size();
        int walkMin = 0;
        for (int i = 0; i < 7; i++) walkMin += Math.max(0, Steps.on(a, today.minusDays(i).toString()) - 3000) / 110;
        int avg = Steps.avg(a, today.plusDays(1), 7);
        State.Test lt = s.lastTest();
        double sr = lt.raw.containsKey(Science.T_SITRISE) ? lt.raw.get(Science.T_SITRISE) : 0;
        double bal = lt.raw.containsKey(Science.T_BALANCE) ? lt.raw.get(Science.T_BALANCE) : 0;
        int stepT = Science.stepTarget(age);

        score(a, col, "STRENGTH DAYS (7 days)", strengthDays + " / 2+", strengthDays >= 2 ? 2 : strengthDays == 1 ? 1 : 0,
            "WHO guideline: muscle-strengthening on 2 or more days a week, for strength, bone density and metabolism.");
        int act = minutes + walkMin;
        score(a, col, "ACTIVE MINUTES (7 days, est.)", act + " / 150", act >= 150 ? 2 : act >= 75 ? 1 : 0,
            "WHO: 150–300 minutes of moderate activity a week, for heart health, energy, mood and sleep. Workout time plus an estimate from walking above 3,000 steps a day.");
        score(a, col, "DAILY STEPS (7-day avg)", String.format(Locale.US, "%,d / %,d", avg, stepT),
            avg >= stepT ? 2 : avg >= stepT * 0.6 ? 1 : 0,
            "The health benefits of walking keep building up to about " + String.format(Locale.US, "%,d", stepT)
                + " steps a day at your age.");
        score(a, col, "SIT & RISE SCORE", fmt1(sr) + " / 10", sr >= 8 ? 2 : sr >= 6 ? 1 : 0,
            "Strength, flexibility and balance working together: getting down to the floor and back up with no hands. "
                + "Every point counts.");
        score(a, col, "ONE-LEG STAND", (bal >= 60 ? "60+" : fmt1(bal)) + " s / 10 s", bal >= 10 ? 2 : bal >= 5 ? 1 : 0,
            "Steadiness on your feet. 10 seconds is the first goal; 30 or more is great balance.");

        State.Measure lm = s.measures.isEmpty() ? null : s.measures.get(s.measures.size() - 1);
        if (lm != null && lm.waist > 0 && s.heightCm > 0) {
            double whtr = lm.waist / s.heightCm;
            score(a, col, "WAIST-TO-HEIGHT", String.format(Locale.US, "%.2f / under 0.50", whtr), whtr < 0.5 ? 2 : whtr < 0.6 ? 1 : 0,
                "Keep your waist under half your height: a simple marker of healthy body fat (NICE, 2022).");
        }
        if (lm != null && lm.waist > 0 && lm.hip > 0) {
            double whr = lm.waist / lm.hip;
            score(a, col, "WAIST-TO-HIP", String.format(Locale.US, "%.2f / 0.80 or less", whr), whr <= 0.8 ? 2 : whr <= 0.85 ? 1 : 0,
                "Lower is healthier (WHO's guide for women: 0.85 or less) and it's the classic hourglass measure: "
                    + "building the glutes and hips while trimming the waist lowers it.");
        }
        int feelN = 0, feelSum = 0;
        for (int i = s.workouts.size() - 1; i >= 0 && feelN < 10; i--) {
            if (s.workouts.get(i).feel > 0) {
                feelSum += s.workouts.get(i).feel;
                feelN++;
            }
        }
        if (feelN > 0) {
            double f = feelSum / (double) feelN;
            score(a, col, "HOW YOU FEEL AFTER", String.format(Locale.US, "%.1f / 5", f), f >= 3.5 ? 2 : f >= 2.5 ? 1 : 0,
                "Feeling good is the point. If this drops, choose EASY or TINY MODE for a few days and sleep more.");
        }

        // measurements
        LinearLayout mc = Ui.card(a);
        mc.addView(Ui.head(a, "MEASUREMENTS", 18, Ui.TEXT));
        mc.addView(Ui.text(a, "The tape measure shows shape changes the scale can't. Measure every 2–4 weeks, same time "
            + "of day: waist at the narrowest point, hips at the fullest part of the glutes.", 13, Ui.DIM), Ui.lp(a, 4));
        if (s.measures.size() >= 2) {
            Ui.LineChart lc = new Ui.LineChart(a, 140);
            Ui.LineChart.Series w = new Ui.LineChart.Series(), h = new Ui.LineChart.Series();
            w.color = Ui.PURPLE; w.width = 2.5f;
            h.color = Ui.PINK; h.width = 2.5f;
            for (State.Measure m : s.measures) {
                float x = LocalDate.parse(m.date).toEpochDay();
                if (m.waist > 0) w.pts.add(new float[]{x, (float) (s.imperial ? m.waist / 2.54 : m.waist)});
                if (m.hip > 0) h.pts.add(new float[]{x, (float) (s.imperial ? m.hip / 2.54 : m.hip)});
            }
            lc.series.add(w);
            lc.series.add(h);
            mc.addView(lc, Ui.lp(a, 8));
            LinearLayout lg = Ui.hbox(a);
            lg.addView(Ui.text(a, "■ waist   ", 11, Ui.PURPLE));
            lg.addView(Ui.text(a, "■ hips", 11, Ui.PINK));
            mc.addView(lg, Ui.lp(a, 4));
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d, yyyy");
        for (int i = s.measures.size() - 1; i >= 0 && i >= s.measures.size() - 6; i--) {
            State.Measure m = s.measures.get(i);
            StringBuilder sb = new StringBuilder();
            if (m.waist > 0) sb.append("waist ").append(len(s, m.waist)).append("   ");
            if (m.hip > 0) sb.append("hips ").append(len(s, m.hip)).append("   ");
            if (m.bust > 0) sb.append("bust ").append(len(s, m.bust)).append("   ");
            if (m.thigh > 0) sb.append("thigh ").append(len(s, m.thigh)).append("   ");
            if (m.weight > 0) sb.append(mass(s, m.weight));
            if (m.waist > 0 && m.hip > 0) sb.append(String.format(Locale.US, "\nwaist-to-hip %.2f", m.waist / m.hip));
            LinearLayout r = Ui.vbox(a);
            r.addView(Ui.bold(a, LocalDate.parse(m.date).format(df), 13, Ui.TEXT));
            r.addView(Ui.text(a, sb.toString().trim(), 13, Ui.DIM));
            mc.addView(r, Ui.lp(a, 10));
        }
        mc.addView(Ui.button(a, "LOG MEASUREMENTS", Ui.PURPLE, v -> logMeasure(a, new State.Measure(), 0)), Ui.lp(a, 12));
        col.addView(mc, Ui.lp(a, 16));
        col.addView(Ui.text(a, "GLOW UP is a fitness game, not medical advice. Talk to your doctor about your health, "
            + "especially before big changes.", 11, Ui.DIM), Ui.lp(a, 14));
    }

    static String fmt1(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v);
    }

    static void score(MainActivity a, LinearLayout col, String title, String value, int level, String why) {
        LinearLayout c = Ui.card(a);
        LinearLayout r = Ui.hbox(a);
        int color = level >= 2 ? Ui.GREEN : level == 1 ? Ui.GOLD : Ui.RED;
        TextView dot = Ui.text(a, "●", 18, color);
        r.addView(dot, new LinearLayout.LayoutParams(Ui.dp(a, 26), -2));
        r.addView(Ui.head(a, title, 15, Ui.TEXT), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.head(a, value, 15, color));
        c.addView(r);
        c.addView(Ui.text(a, why, 12, Ui.DIM), Ui.lp(a, 6));
        col.addView(c, Ui.lp(a, 10));
    }

    static final String[] M_NAMES = {"Waist (narrowest point)", "Hips (fullest part of the glutes)", "Bust / chest", "Thigh (fullest part)", "Weight"};

    static void logMeasure(MainActivity a, State.Measure m, int step) {
        State s = a.st;
        if (step >= M_NAMES.length) {
            m.date = LocalDate.now().toString();
            if (m.waist + m.hip + m.bust + m.thigh + m.weight <= 0) return;
            if (!s.measures.isEmpty() && s.measures.get(s.measures.size() - 1).date.equals(m.date)) s.measures.remove(s.measures.size() - 1);
            s.measures.add(m);
            s.xp += 20;
            Logic.checkAch(a, s);
            Store.save(a, s);
            health(a);
            return;
        }
        boolean weight = step == 4;
        String unit = weight ? (s.imperial ? "lb" : "kg") : (s.imperial ? "inches" : "cm");
        AlertDialog.Builder b = new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        android.widget.EditText e = new android.widget.EditText(a);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setHint(unit + " (leave empty to skip)");
        android.widget.FrameLayout f = new android.widget.FrameLayout(a);
        f.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), 0);
        f.addView(e);
        b.setTitle(M_NAMES[step]).setView(f).setPositiveButton("NEXT", (d, w) -> {
            double v = 0;
            try {
                v = Double.parseDouble(e.getText().toString().trim());
            } catch (Exception ignored) {
            }
            if (v > 0) {
                double metric = weight ? (s.imperial ? v * 0.45359237 : v) : (s.imperial ? v * 2.54 : v);
                switch (step) {
                    case 0: m.waist = metric; break;
                    case 1: m.hip = metric; break;
                    case 2: m.bust = metric; break;
                    case 3: m.thigh = metric; break;
                    default: m.weight = metric; break;
                }
            }
            logMeasure(a, m, step + 1);
        }).setNegativeButton("CANCEL", null).show();
    }

    // ---------------- ME ----------------

    static void me(MainActivity a) {
        State s = a.st;
        LinearLayout col = a.scrollPage(true);
        col.addView(Ui.head(a, "ME", 30, Ui.TEXT));

        item(a, col, "NAME", s.name.isEmpty() ? "-" : s.name, () -> a.askText("Your name", s.name, n -> { s.name = n; save(a); }));
        item(a, col, "BIRTH DATE", s.birth + "  (age " + (int) Science.age(s.birth, LocalDate.now()) + ")",
            () -> a.askDate(s.birth, d -> { s.birth = d.toString(); save(a); }));
        item(a, col, "HEIGHT", s.heightCm > 0 ? height(s, s.heightCm) : "not set", () -> a.askNumber(
            s.imperial ? "Height in inches" : "Height in cm", "", 0, v -> { s.heightCm = s.imperial ? v * 2.54 : v; save(a); }));
        item(a, col, "UNITS", s.imperial ? "inches / pounds" : "cm / kg", () -> { s.imperial = !s.imperial; save(a); });
        item(a, col, "DAILY REMINDER", s.remind ? String.format(Locale.US, "on, %d:%02d %s", (s.remindH + 11) % 12 + 1, s.remindM, s.remindH < 12 ? "AM" : "PM") : "off",
            () -> new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Daily reminder")
                .setItems(new String[]{"Set the time", s.remind ? "Turn off" : "Turn on"}, (d, w) -> {
                    if (w == 0) a.askTime(s.remindH, s.remindM, (hh, mm) -> { s.remindH = hh; s.remindM = mm; s.remind = true; save(a); a.askPermissions(); });
                    else { s.remind = !s.remind; save(a); }
                }).show());
        String stepState = !Steps.available(a) ? "this phone has no step sensor" : !Steps.permitted(a) ? "needs permission: tap"
            : s.stepsOn ? "on" : "off";
        item(a, col, "STEP COUNTING", stepState, () -> {
            if (!Steps.permitted(a)) { a.askPermissions(); return; }
            s.stepsOn = !s.stepsOn;
            save(a);
        });
        item(a, col, "PLAN DAY ONE", s.start + " (" + Plan.typeFor(s, LocalDate.now()) + " today)", () ->
            new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Restart the weekly plan today?")
                .setMessage("Today becomes " + Plan.WEEK[0] + ", and the 7-day cycle runs from here. Your ratings and history stay.")
                .setPositiveButton("RESTART TODAY", (d, w) -> { s.start = LocalDate.now().toString(); save(a); })
                .setNegativeButton("CANCEL", null).show());

        col.addView(Ui.head(a, "YOUR DATA", 16, Ui.DIM), Ui.lp(a, 22));
        col.addView(Ui.text(a, "Everything stays on this phone (and in your phone's own Google backup). Save a backup "
            + "file now and then: it's plain JSON, readable by any future app, so your history can follow you for decades.", 13, Ui.DIM), Ui.lp(a, 4));
        LinearLayout r = Ui.hbox(a);
        r.addView(Ui.button(a, "SAVE BACKUP", Ui.CYAN, v -> a.exportData()), Ui.weight(a, 1, 0));
        r.addView(Ui.space(a, 1), new LinearLayout.LayoutParams(Ui.dp(a, 10), 1));
        r.addView(Ui.ghost(a, "RESTORE", v -> a.importData()), Ui.weight(a, 1, 0));
        col.addView(r, Ui.lp(a, 10));

        col.addView(Ui.ghost(a, "HOW THE RATING WORKS", v -> new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("How the rating works").setMessage(SCIENCE).setPositiveButton("GOT IT", null).show()), Ui.lp(a, 22));
        col.addView(Ui.ghost(a, "RESET EVERYTHING", v -> new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Erase all your data?").setMessage("Your ratings, history and trophies will be gone for good. Save a backup first if you might want them.")
            .setPositiveButton("ERASE", (d, w) -> {
                a.st = new State();
                Store.save(a, a.st);
                a.getSharedPreferences("steps", 0).edit().clear().apply();
                new TestFlow(a, true).start();
            }).setNegativeButton("CANCEL", null).show()), Ui.lp(a, 10));
        col.addView(Ui.ghost(a, "CHECK FOR UPDATES", v -> Updater.check(a, true)), Ui.lp(a, 10));
        col.addView(Ui.button(a, "EXERCISE LIBRARY: HOW TO DO EVERY MOVE", Ui.PURPLE, v -> HowTo.library(a)), 1, Ui.lp(a, 12));
        col.addView(Ui.text(a, "GLOW UP " + Updater.installed(a), 11, Ui.DIM), Ui.lp(a, 20));
    }

    static void save(MainActivity a) {
        Store.save(a, a.st);
        Reminder.schedule(a, a.st);
        me(a);
    }

    static void item(MainActivity a, LinearLayout col, String label, String value, Runnable click) {
        LinearLayout c = Ui.card(a);
        c.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        c.addView(Ui.text(a, label, 11, Ui.DIM));
        c.addView(Ui.bold(a, value, 16, Ui.TEXT));
        c.setOnClickListener(v -> click.run());
        col.addView(c, Ui.lp(a, 8));
    }

    static final String SCIENCE =
        "YOUR FITNESS RATING works like a chess Elo, on a scale built from published fitness norms for women. Each test "
            + "result is placed by where it falls among women your age: 400 is the 10th percentile, 800 the 30th, 1200 the "
            + "median, 1600 the 75th, 2000 the 90th, 2400 the top 1%. 2600 is national-elite and 2800 is Olympic: OLYMPIAN "
            + "takes the results Olympic athletes post, like a 12-minute run of about 3.9 km (2.4 miles). 40 ranks, 80 "
            + "points apart. Results are age-graded first, the way masters athletes are scored, so the same rating means "
            + "the same thing at 32 or 62.\n\n"
            + "SOME TESTS HAVE A CEILING. The 2-minute march can't tell a fit person from an athlete, so it rates STAMINA "
            + "up to 1800 at most; the 12-minute run goes all the way. A perfect sit & rise stops at 2000.\n\n"
            + "EVERY WORKOUT IS A MATCH against an opponent: the workout itself. Its rating is the rating whose NORMAL "
            + "targets match the reps you picked, so EASY plays someone weaker than you, NORMAL your equal, HARD and "
            + "INSANE someone stronger. Finish everything and you win; finish about 84% and it's a draw; peter out "
            + "around half and it's nearly a loss. Beat a strong opponent and you gain a lot; fall short against a weak "
            + "one and you lose some. Can't do an exercise at all, and that category drops.\n\n"
            + "TARGETS come from your rating: it says how many reps you can do in one go, and EASY asks for 60% of that "
            + "each set, NORMAL 75%, HARD 85%, INSANE 95%. Harder versions of each exercise unlock once you can do 8+ reps. "
            + "If targets feel wrong, they fix themselves: missed targets lower the rating, and the targets follow.\n\n"
            + "STAMINA also plays a small match every day against your step goal. Every 4 weeks a RETEST recalibrates "
            + "everything. After two weeks without training ratings slowly fade, as real fitness does.\n\n"
            + "XP is separate: it only ever goes up. It rewards showing up (workouts, steps, tests, trophies), while the "
            + "rating measures how fit you are.";
}
