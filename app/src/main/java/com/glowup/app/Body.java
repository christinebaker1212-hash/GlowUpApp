package com.glowup.app;

import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TRAINING ARC's BODY tab and FUEL missions: body fat from a tape measure (US Navy method), the GOD FORM goal, the
 * weight trend and how long the arc honestly takes, and simple daily food habits instead of calorie counting.
 */
final class Body {
    private Body() {}

    /** US Navy body-fat estimate for men (cm). About ±3-4% against a DEXA scan, but consistent over time. */
    static double bodyFat(double waistCm, double neckCm, double heightCm) {
        if (waistCm <= 0 || neckCm <= 0 || heightCm <= 0 || waistCm <= neckCm) return -1;
        double bf = 495 / (1.0324 - 0.19077 * Math.log10(waistCm - neckCm) + 0.15456 * Math.log10(heightCm)) - 450;
        return Math.max(3, Math.min(60, bf));
    }

    static String bfClass(double bf) {
        if (bf < 6) return "STAGE-LEAN";
        if (bf < 13) return "ATHLETE LEAN: abs showing";
        if (bf < 18) return "FIT";
        if (bf < 25) return "AVERAGE";
        return "ABOVE AVERAGE";
    }

    /** {date, bf} for every day with waist + neck (and the profile height). */
    static List<double[]> bfHistory(State s) {
        List<double[]> out = new ArrayList<>();
        for (State.Measure m : s.measures) {
            double bf = bodyFat(m.waist, m.neck, s.heightCm);
            if (bf > 0) out.add(new double[]{LocalDate.parse(m.date).toEpochDay(), bf});
        }
        return out;
    }

    static List<double[]> weights(State s) {
        List<double[]> out = new ArrayList<>();
        for (State.Measure m : s.measures) if (m.weight > 0) out.add(new double[]{LocalDate.parse(m.date).toEpochDay(), m.weight});
        return out;
    }

    /** The average of the weigh-ins in the 7 days up to a day. */
    static double avg7(List<double[]> w, double day) {
        double sum = 0;
        int n = 0;
        for (double[] p : w) if (p[0] <= day && p[0] > day - 7) { sum += p[1]; n++; }
        return n == 0 ? -1 : sum / n;
    }

    /** kg per week over the last 4 weeks of weigh-ins (least squares), or NaN without 2 weeks of data. */
    static double rate(List<double[]> w) {
        if (w.size() < 3) return Double.NaN;
        double last = w.get(w.size() - 1)[0];
        List<double[]> r = new ArrayList<>();
        for (double[] p : w) if (p[0] > last - 28) r.add(p);
        if (r.size() < 3 || last - r.get(0)[0] < 13) return Double.NaN;
        double mx = 0, my = 0;
        for (double[] p : r) { mx += p[0]; my += p[1]; }
        mx /= r.size();
        my /= r.size();
        double num = 0, den = 0;
        for (double[] p : r) { num += (p[0] - mx) * (p[1] - my); den += (p[0] - mx) * (p[0] - mx); }
        return den == 0 ? Double.NaN : num / den * 7;
    }

    // ---------------- FUEL ----------------

    static int fuelToday(State s) {
        Integer v = s.fuel.get(LocalDate.now().toString());
        return v == null ? 0 : v;
    }

    static int count(int bits) {
        return Integer.bitCount(bits & 31);
    }

    /** The FUEL card: five daily habits, +15 XP each, +25 for all five. */
    static LinearLayout fuelCard(MainActivity a, Runnable refresh) {
        State s = a.st;
        LinearLayout c = Ui.card(a);
        int bits = fuelToday(s);
        LinearLayout head = Ui.hbox(a);
        head.addView(Ui.text(a, "TODAY'S FUEL", 12, Ui.PINK), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.head(a, count(bits) + " / 5" + (count(bits) == 5 ? "  FULL POWER" : ""), 14, count(bits) == 5 ? Ui.GOLD : Ui.TEXT));
        c.addView(head);
        c.addView(Ui.text(a, "No calorie counting. Five habits that do most of the work.", 12, Ui.DIM), Ui.lp(a, 2));
        for (int i = 0; i < Flavor.FUEL.length; i++) {
            final int bit = 1 << i;
            boolean on = (bits & bit) != 0;
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
            TextView box = Ui.head(a, on ? "✓" : "", 18, 0xFF0A0F1A);
            box.setGravity(Gravity.CENTER);
            box.setBackground(on ? Ui.round(Ui.PINK, Ui.dp(a, 8)) : outline(a));
            r.addView(box, new LinearLayout.LayoutParams(Ui.dp(a, 30), Ui.dp(a, 30)));
            LinearLayout t = Ui.vbox(a);
            t.setPadding(Ui.dp(a, 12), 0, 0, 0);
            t.addView(Ui.head(a, Flavor.FUEL[i][0], 14, on ? Ui.TEXT : Ui.DIM));
            t.addView(Ui.text(a, Flavor.FUEL[i][1], 12, Ui.DIM));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            r.setOnClickListener(v -> {
                int before = fuelToday(s), after = before ^ bit;
                s.fuel.put(LocalDate.now().toString(), after);
                s.xp += (after > before ? 15 : -15) + (count(after) == 5 ? 25 : 0) - (count(before) == 5 ? 25 : 0);
                if (count(after) == 5) Widgets.buzz(a, 150);
                Store.save(a, s);
                refresh.run();
            });
            c.addView(r);
        }
        return c;
    }

    static android.graphics.drawable.GradientDrawable outline(MainActivity a) {
        android.graphics.drawable.GradientDrawable g = Ui.round(0, Ui.dp(a, 8));
        g.setStroke(Ui.dp(a, 2), Ui.LINE);
        return g;
    }

    // ---------------- the BODY tab ----------------

    static void show(MainActivity a) {
        State s = a.st;
        LocalDate today = LocalDate.now();
        LinearLayout col = a.scrollPage(true);
        col.addView(Ui.head(a, "BODY", 30, Ui.TEXT));
        col.addView(Ui.text(a, "The arc to GOD FORM: lean enough to see the muscle you've built.", 14, Ui.DIM), Ui.lp(a, 4));

        State.Measure last = null;
        for (State.Measure m : s.measures) if (bodyFat(m.waist, m.neck, s.heightCm) > 0) last = m;
        List<double[]> bfs = bfHistory(s), ws = weights(s);
        double weightNow = ws.isEmpty() ? -1 : avg7(ws, ws.get(ws.size() - 1)[0]);

        LinearLayout god = Ui.card(a);
        god.setBackground(Ui.round(Ui.CARD2, Ui.dp(a, 16)));
        god.addView(Ui.text(a, "GOD FORM: " + Math.round(s.bfGoal) + "% BODY FAT", 12, Ui.PINK));
        if (last == null) {
            god.addView(Ui.head(a, "MEASURE UP TO START", 22, Ui.TEXT), Ui.lp(a, 4));
            god.addView(Ui.text(a, "A tape measure is enough: your waist at the belly button and your neck give your body fat "
                + "within a few percent, the way the US Navy measures it." + (s.heightCm <= 0 ? " Your height is needed too (ME > HEIGHT)." : ""), 14, Ui.DIM), Ui.lp(a, 4));
        } else {
            double bf = bodyFat(last.waist, last.neck, s.heightCm);
            LinearLayout r = Ui.hbox(a);
            LinearLayout l = Ui.vbox(a);
            l.addView(Ui.head(a, String.format(Locale.US, "%.1f%%", bf), 44, Ui.TEXT));
            l.addView(Ui.text(a, bfClass(bf), 13, Ui.DIM));
            r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
            if (bfs.size() >= 2) {
                double d = bf - bfs.get(0)[1];
                r.addView(Ui.head(a, (d <= 0 ? "▼ " : "▲ ") + String.format(Locale.US, "%.1f", Math.abs(d)) + "%\nsince start", 15, d <= 0 ? Ui.GREEN : Ui.ORANGE));
            }
            god.addView(r, Ui.lp(a, 4));
            double start = bfs.get(0)[1];
            float prog = (float) (start <= s.bfGoal ? 1 : Math.max(0, Math.min(1, (start - bf) / (start - s.bfGoal))));
            god.addView(new Ui.XpBar(a, prog), Ui.lp(a, 10));
            god.addView(Ui.text(a, Math.round(prog * 100) + "% of the way from " + String.format(Locale.US, "%.0f%%", start) + " to " + Math.round(s.bfGoal) + "%", 12, Ui.DIM), Ui.lp(a, 4));
            double w = last.weight > 0 ? last.weight : weightNow;
            if (w > 0 && bf > s.bfGoal) {
                double lean = w * (1 - bf / 100), goalW = lean / (1 - s.bfGoal / 100), toLose = w - goalW;
                god.addView(Ui.text(a, String.format(Locale.US, "Muscle and bone: %s  •  fat: %s\nGOD FORM keeping all that muscle: about %s, %s to lose.",
                    Screens.mass(s, lean), Screens.mass(s, w - lean), Screens.mass(s, goalW), Screens.mass(s, toLose)), 14, Ui.TEXT), Ui.lp(a, 10));
                double rt = rate(ws);
                String arc;
                if (!Double.isNaN(rt) && rt < -0.05) {
                    double weeks = toLose / -rt;
                    arc = String.format(Locale.US, "At your current pace (%s a week): about %d weeks.", Screens.mass(s, -rt), Math.round(weeks));
                } else {
                    double fast = w * 0.01, slow = w * 0.005;
                    arc = String.format(Locale.US, "At a steady 0.5–1%% of your weight a week (%s–%s, the pace that keeps muscle): %d to %d weeks.",
                        Screens.mass(s, slow), Screens.mass(s, fast), Math.round(toLose / fast), Math.round(toLose / slow));
                    if (Double.isNaN(rt)) arc += " Weigh in a few times a week and this turns into your real pace after two weeks.";
                    else arc += " Right now the trend is flat or up: the FUEL habits are where it moves.";
                }
                god.addView(Ui.head(a, "ARC LENGTH", 13, Ui.PINK), Ui.lp(a, 10));
                god.addView(Ui.text(a, arc, 14, Ui.TEXT), Ui.lp(a, 2));
            } else if (bf <= s.bfGoal) {
                god.addView(Ui.head(a, "GOD FORM REACHED. Now build.", 18, Ui.GOLD), Ui.lp(a, 10));
            }
        }
        LinearLayout btns = Ui.hbox(a);
        btns.addView(Ui.button(a, "MEASURE", Ui.PINK, v -> Screens.logMeasure(a, new State.Measure(), 0)), Ui.weight(a, 1, 0));
        btns.addView(Ui.space(a, 1), new LinearLayout.LayoutParams(Ui.dp(a, 10), 1));
        btns.addView(Ui.ghost(a, "WEIGH IN", v -> a.askNumber(s.imperial ? "Weight (lb)" : "Weight (kg)", "", 0, val -> {
            State.Measure m = new State.Measure();
            m.date = today.toString();
            m.weight = s.imperial ? val * 0.45359237 : val;
            boolean firstToday = true;
            for (State.Measure x : s.measures) if (x.date.equals(m.date) && x.weight > 0) firstToday = false;
            Screens.addMeasure(s, m);
            if (firstToday) s.xp += 5;
            Store.save(a, s);
            show(a);
        })), Ui.weight(a, 1, 0));
        god.addView(btns, Ui.lp(a, 14));
        TextView goal = Ui.text(a, "Change the goal (" + Math.round(s.bfGoal) + "%)", 12, Ui.DIM);
        goal.setGravity(Gravity.END);
        goal.setOnClickListener(v -> new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("GOD FORM goal")
            .setItems(new String[]{"10%: shredded, every ab", "12%: abs and veins (recommended)", "15%: lean and athletic"}, (d, wch) -> {
                s.bfGoal = wch == 0 ? 10 : wch == 1 ? 12 : 15;
                Store.save(a, s);
                show(a);
            }).show());
        god.addView(goal, Ui.lp(a, 8));
        col.addView(god, Ui.lp(a, 14));

        // weight trend
        if (ws.size() >= 2) {
            LinearLayout wc = Ui.card(a);
            wc.addView(Ui.text(a, "WEIGHT: 7-DAY AVERAGE (BRIGHT) AND WEIGH-INS", 12, Ui.DIM));
            Ui.LineChart lc = new Ui.LineChart(a, 150);
            Ui.LineChart.Series raw = new Ui.LineChart.Series(), av = new Ui.LineChart.Series();
            raw.color = 0x66FFFFFF;
            raw.width = 1.5f;
            av.color = Ui.PINK;
            av.width = 3;
            double k = s.imperial ? 1 / 0.45359237 : 1;
            for (double[] p : ws) {
                raw.pts.add(new float[]{(float) p[0], (float) (p[1] * k)});
                av.pts.add(new float[]{(float) p[0], (float) (avg7(ws, p[0]) * k)});
            }
            lc.series.add(raw);
            lc.series.add(av);
            wc.addView(lc, Ui.lp(a, 8));
            wc.addView(Ui.text(a, "Water makes one morning's number jump around by a few pounds. The average line is the truth.", 12, Ui.DIM), Ui.lp(a, 6));
            col.addView(wc, Ui.lp(a, 12));
        }

        // fuel
        col.addView(fuelCard(a, () -> show(a)), Ui.lp(a, 12));
        LinearLayout fw = Ui.card(a);
        fw.addView(Ui.text(a, "FUEL, LAST 14 DAYS", 12, Ui.DIM));
        Ui.Bars bars = new Ui.Bars(a, 70);
        bars.values = new int[14];
        for (int i = 0; i < 14; i++) {
            Integer v = s.fuel.get(today.minusDays(13 - i).toString());
            bars.values[i] = v == null ? 0 : count(v) * 1000;
        }
        bars.goal = 5000;
        fw.addView(bars, Ui.lp(a, 8));
        fw.addView(Ui.text(a, "Gold = all five. Four out of five most days is a winning arc.", 12, Ui.DIM), Ui.lp(a, 6));
        col.addView(fw, Ui.lp(a, 12));

        // training habits (shared with GLOW UP's health targets)
        int strengthDays = 0;
        java.util.Set<String> days = new java.util.HashSet<>();
        for (State.Workout w : s.workouts)
            if (ChronoUnit.DAYS.between(LocalDate.parse(w.date), today) < 7 && !Plan.RECOVERY.equals(w.type)) days.add(w.date);
        strengthDays = days.size();
        Screens.score(a, col, "TRAINING DAYS (7 days)", strengthDays + " / 3+", strengthDays >= 3 ? 2 : strengthDays >= 2 ? 1 : 0,
            "Three or more training days a week keeps your muscle while the fat comes off.");
        int avg = Steps.avg(a, today.plusDays(1), 7), goalSteps = Logic.goalOn(a, s, today);
        Screens.score(a, col, "DAILY STEPS (7-day avg)", String.format(Locale.US, "%,d / %,d", avg, goalSteps),
            avg >= goalSteps ? 2 : avg >= goalSteps * 0.6 ? 1 : 0,
            "Walking is the most underrated fat burner: easy on the joints and it adds up to hundreds of calories a day.");
        if (last != null && s.heightCm > 0) {
            double whtr = last.waist / s.heightCm;
            Screens.score(a, col, "WAIST-TO-HEIGHT", String.format(Locale.US, "%.2f / under 0.50", whtr), whtr < 0.5 ? 2 : whtr < 0.6 ? 1 : 0,
                "Keep your waist under half your height: the simplest marker of healthy body fat.");
        }

        // measurement log
        LinearLayout mc = Ui.card(a);
        mc.addView(Ui.text(a, "MEASUREMENTS", 12, Ui.DIM));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d");
        int shown = 0;
        for (int i = s.measures.size() - 1; i >= 0 && shown < 8; i--, shown++) {
            State.Measure m = s.measures.get(i);
            StringBuilder sb = new StringBuilder();
            if (m.weight > 0) sb.append(Screens.mass(s, m.weight)).append("   ");
            if (m.waist > 0) sb.append("waist ").append(Screens.len(s, m.waist)).append("   ");
            if (m.neck > 0) sb.append("neck ").append(Screens.len(s, m.neck)).append("   ");
            if (m.bust > 0) sb.append("chest ").append(Screens.len(s, m.bust)).append("   ");
            if (m.arm > 0) sb.append("arm ").append(Screens.len(s, m.arm));
            double bf = bodyFat(m.waist, m.neck, s.heightCm);
            if (bf > 0) sb.append(String.format(Locale.US, "\nbody fat %.1f%%", bf));
            LinearLayout r = Ui.vbox(a);
            r.addView(Ui.bold(a, LocalDate.parse(m.date).format(df), 13, Ui.TEXT));
            r.addView(Ui.text(a, sb.toString().trim(), 13, Ui.DIM));
            mc.addView(r, Ui.lp(a, 8));
        }
        if (s.measures.isEmpty()) mc.addView(Ui.text(a, "Nothing yet. Measure once a week, same time of day, before eating.", 13, Ui.DIM), Ui.lp(a, 6));
        col.addView(mc, Ui.lp(a, 12));
        col.addView(Ui.text(a, Flavor.APP + " is a fitness game, not medical advice.", 11, Ui.DIM), Ui.lp(a, 14));
    }
}
