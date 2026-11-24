package com.glowup.app;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** The numbers simulated over months, and every screen drawn at a phone's size (pictures in app/build/screens). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppTest {
    static final int W = 1080, H = 2340;

    static void shot(MainActivity a, String name) throws Exception {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2500));
        View v = a.findViewById(android.R.id.content);
        v.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, W, H);
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        b.eraseColor(Ui.BG);
        v.draw(new Canvas(b));
        File dir = new File(System.getProperty("screens.out", "build/screens"));
        dir.mkdirs();
        try (FileOutputStream o = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
        }
    }

    /** A literal couch potato's baseline. */
    static State.Test couchPotato() {
        State.Test t = new State.Test();
        t.raw.put(Science.T_SITRISE, 4.0);
        t.raw.put(Science.T_BALANCE, 8.0);
        t.raw.put(Science.T_CHAIR, 9.0);
        t.raw.put(Science.T_PUSHUP, 10 * 0.15); // 10 wall push-ups
        t.note.put(Science.T_PUSHUP, "wall");
        t.raw.put(Science.T_BRIDGE, 12.0);
        t.raw.put(Science.T_PLANK, 15 * 0.55); // 15 s knee plank
        t.note.put(Science.T_PLANK, "knee");
        t.raw.put(Science.T_MARCH, 45.0);
        return t;
    }

    static State fresh(MainActivity a) {
        State s = new State();
        s.name = "Christine";
        s.birth = LocalDate.now().minusYears(32).minusDays(40).toString();
        s.heightCm = 165;
        a.st = s;
        return s;
    }

    /**
     * Someone whose true fitness grows like a beginner's does her NORMAL workouts honestly for 16 weeks: does the
     * rating follow her true fitness, without running away?
     */
    @Test
    public void ratingFollowsRealFitness() {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        State s = fresh(a);
        double[] base = Logic.finishTest(a, s, couchPotato());
        System.out.println(String.format(Locale.US, "BASELINE: overall %.0f (%s)  cats %.0f %.0f %.0f %.0f %.0f",
            Science.overall(s.elo), Science.rankName(Science.overall(s.elo)), base[0], base[1], base[2], base[3], base[4]));
        assertTrue("a couch potato starts as a COUCH POTATO", Science.tier(Science.overall(s.elo)) == 0);

        LocalDate day0 = LocalDate.now().minusDays(16 * 7);
        s.start = day0.toString();
        double age = Science.age(s.birth, day0);
        for (int d = 0; d < 16 * 7; d++) {
            LocalDate day = day0.plusDays(d);
            String type = Plan.typeFor(s, day);
            if (Plan.REST.equals(type) || Plan.RECOVERY.equals(type)) continue;
            double weeks = d / 7.0;
            double[] truth = new double[5];
            for (int c = 0; c < 5; c++) truth[c] = base[c] + 45 * weeks * (1 - weeks / 40); // beginner gains, slowing
            State.Workout w = new State.Workout();
            w.date = day.toString();
            w.type = type;
            w.diff = 1;
            w.items = Plan.build(s, day, type, 1, false);
            for (State.Item it : w.items) {
                Plan.Ex e = Plan.ex(it.ex);
                double max = e.cat < 0 ? 999 : Science.capacity(e.test, truth[e.cat], age) / e.factor;
                for (int i = 0; i < it.target.length; i++)
                    it.done[i] = (int) Math.min(it.target[i], Math.floor(max * (1 - 0.12 * i)));
            }
            Logic.finishWorkout(a, s, w);
            if (d % 28 == 26) { // the 4-weekly retest, done honestly
                State.Test t = new State.Test();
                double ag = Science.age(s.birth, day);
                t.raw.put(Science.T_PUSHUP, Science.capacity(Science.T_PUSHUP, truth[1], ag));
                t.raw.put(Science.T_BRIDGE, Science.capacity(Science.T_BRIDGE, truth[2], ag));
                t.raw.put(Science.T_CHAIR, Science.capacity(Science.T_CHAIR, truth[2], ag));
                t.raw.put(Science.T_PLANK, Science.capacity(Science.T_PLANK, truth[3], ag));
                t.raw.put(Science.T_MARCH, Science.capacity(Science.T_MARCH, truth[0], ag));
                t.raw.put(Science.T_BALANCE, Science.capacity(Science.T_BALANCE, truth[4], ag));
                t.raw.put(Science.T_SITRISE, Science.capacity(Science.T_SITRISE, truth[4], ag));
                Logic.finishTest(a, s, t);
                System.out.println("   (retest)");
            }
            if (d % 14 == 0 || d == 16 * 7 - 2) {
                StringBuilder sb = new StringBuilder(String.format(Locale.US, "week %4.1f %-15s %3.0f%%  app/true:", weeks, type, w.completion * 100));
                for (int c = 0; c < 5; c++) sb.append(String.format(Locale.US, " %s %.0f/%.0f", Science.CAT_LABEL[c].substring(0, 3), s.elo[c], truth[c]));
                System.out.println(sb);
                StringBuilder tg = new StringBuilder("   ");
                for (State.Item it : w.items) {
                    Plan.Ex e = Plan.ex(it.ex);
                    if (e.cat >= 0) tg.append(String.format(Locale.US, " %s %dx%d%s;", e.name, it.target.length, it.target[0], e.kind == Plan.HOLD ? "s" : ""));
                }
                System.out.println(tg);
            }
            for (int c = 0; c < 5; c++) {
                if (w.before[c] != w.after[c] && d > 21) {
                    assertTrue("rating near true fitness: " + Science.CAT_LABEL[c] + " " + s.elo[c] + " vs " + truth[c],
                        Math.abs(s.elo[c] - truth[c]) < 180);
                }
            }
        }
        System.out.println(String.format(Locale.US, "After 16 weeks: overall %.0f (%s), level %d",
            Science.overall(s.elo), Science.rankName(Science.overall(s.elo)), Science.level(s.xp)[0]));
    }

    /** Her own baseline (2026-11-21), on the current scale. */
    @Test
    public void herBaseline() {
        org.junit.Assume.assumeFalse("GLOW UP's norms", Flavor.MALE);
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        State s = fresh(a);
        State.Test t = new State.Test();
        t.raw.put(Science.T_SITRISE, 5.0);
        t.raw.put(Science.T_BALANCE, (53 + 43) / 2.0);
        t.raw.put(Science.T_CHAIR, 13.0);
        t.raw.put(Science.T_PUSHUP, 2.3);
        t.raw.put(Science.T_BRIDGE, 14.0);
        t.raw.put(Science.T_PLANK, 2.0);
        t.raw.put(Science.T_MARCH, 108.0);
        double[] c = Logic.finishTest(a, s, t);
        double age = Science.age(s.birth, LocalDate.now());
        for (String k : new String[]{Science.T_SITRISE, Science.T_BALANCE, Science.T_CHAIR, Science.T_PUSHUP,
            Science.T_BRIDGE, Science.T_PLANK, Science.T_MARCH}) {
            double r = Science.testRating(k, t.raw.get(k), age);
            System.out.println(String.format(Locale.US, "  %-8s %6.1f -> %4.0f  %s", k, t.raw.get(k), r, Science.standing(r)));
        }
        for (int i = 0; i < 5; i++)
            System.out.println(String.format(Locale.US, "  %-8s %4.0f  %s", Science.CAT_LABEL[i], c[i], Science.standing(c[i])));
        double o = Science.overall(s.elo);
        System.out.println(String.format(Locale.US, "HER BASELINE: %.0f %s (%s)", o, Science.rankName(o), Science.standing(o)));
        assertTrue("not a WEEKEND MOVER any more", Science.tier(o) <= 1);
        assertTrue("the march can't push STAMINA high", c[Science.CARDIO] < 1200);

        // the same save in the old format converts to the same numbers
        State old = new State();
        old.birth = s.birth;
        old.schemaLoaded = 1;
        State.Test ot = new State.Test();
        ot.raw.putAll(t.raw);
        ot.age = t.age;
        ot.cat = new double[]{1637, 297, 502, 100, 1110};
        old.tests.add(ot);
        old.elo = new double[]{1650, 310, 515, 100, 1110};
        Logic.migrate(old);
        for (int i = 0; i < 5; i++)
            assertTrue("migrated " + i, Math.abs(old.elo[i] - (c[i] + (new double[]{13, 13, 13, 0, 0})[i])) < 1);
        assertTrue(old.schemaLoaded == State.SCHEMA);
    }

    /** TRAINING ARC: a big guy with plenty of muscle and push-up practice but little conditioning. */
    @Test
    public void hisExample() {
        org.junit.Assume.assumeTrue("TRAINING ARC's norms", Flavor.MALE);
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        State s = fresh(a);
        s.heightCm = 180;
        State.Test t = new State.Test();
        t.raw.put(Science.T_SITRISE, 6.0);
        t.raw.put(Science.T_BALANCE, 30.0);
        t.raw.put(Science.T_CHAIR, 16.0);
        t.raw.put(Science.T_PUSHUP, 30.0);
        t.raw.put(Science.T_BRIDGE, 25.0);
        t.raw.put(Science.T_PLANK, 45.0);
        t.raw.put(Science.T_MARCH, 100.0);
        double[] c = Logic.finishTest(a, s, t);
        for (int i = 0; i < 5; i++)
            System.out.println(String.format(Locale.US, "  %-8s %4.0f  %s", Science.CAT_LABEL[i], c[i], Science.standing(c[i])));
        double o = Science.overall(s.elo);
        System.out.println(String.format(Locale.US, "HIS EXAMPLE: power level %.0f, %s (%s)", o, Science.rankName(o), Science.standing(o)));
        assertTrue("30 push-ups is 75th percentile for a man of 30", Math.abs(c[Science.UPPER] - 1600) < 60);
        // body fat: 108 cm waist, 41 cm neck, 180 cm -> about 30% (US Navy)
        double bf = Body.bodyFat(108, 41, 180);
        System.out.println(String.format(Locale.US, "  body fat at a 42.5 in waist: %.1f%%", bf));
        assertTrue(bf > 25 && bf < 35);
        assertTrue(Plan.WEEK[0].equals(Plan.PUSH_DAY));
        // the jumping moves wait for the joints
        java.util.List<State.Item> legs = Plan.build(s, LocalDate.now(), Plan.LEG_DAY, 1, false);
        for (State.Item it : legs) assertTrue("no jump squats at POWER " + s.elo[Science.CURVES], !it.ex.equals("jump_squat"));
        assertTrue(Updater.section("## GLOW UP\nhers\n## TRAINING ARC\nhis").equals("his"));
    }

    @Test
    public void scaleTopIsReallyOlympic() {
        // a 3.9 km (women) / 4.1 km (men) 12-minute run is Olympic; a fit club runner's 2.6 km is not close
        assertTrue(Science.testRating(Science.T_COOPER, Flavor.MALE ? 4100 : 3900, 30) >= 2799);
        assertTrue(Science.testRating(Science.T_COOPER, 2600, 30) < 2100);
        // no march count can claim more than ATHLETIC
        assertTrue(Science.testRating(Science.T_MARCH, 250, 30) <= 1800);
        assertTrue(Science.rankName(2800).startsWith(Science.TIERS[7]));
        assertTrue(Science.rankIndex(3199) == 39 && Science.rankIndex(0) == 0);
        assertTrue(Updater.newer("1.10.0", "1.9.3") && !Updater.newer("1.1.0", "1.1.0") && Updater.newer("v2.0", "1.9"));
    }

    /** Every exercise's keyframes drawn on sheets (app/build/screens/demo_*.png), to check the poses by eye. */
    @Test
    public void demoSheets() throws Exception {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (Plan.Ex e : Plan.ALL) {
            ids.add(e.id);
            assertTrue("every exercise has its demonstration: " + e.id, Demo.get(e.id).keys.size() >= 2);
        }
        ids.add("sit_rise");
        ids.add("jog");
        int cw = 300, ch = 260, perSheet = 8;
        File dir = new File(System.getProperty("screens.out", "build/screens"));
        dir.mkdirs();
        for (int sheet = 0; sheet * perSheet < ids.size(); sheet++) {
            int n = Math.min(perSheet, ids.size() - sheet * perSheet), cols = 5;
            Bitmap b = Bitmap.createBitmap(cw * cols, (ch + 24) * n, Bitmap.Config.ARGB_8888);
            b.eraseColor(0xFF140E1C);
            Canvas c = new Canvas(b);
            android.graphics.Paint tp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xFFFFC94D);
            tp.setTextSize(18);
            for (int r = 0; r < n; r++) {
                String id = ids.get(sheet * perSheet + r);
                Demo.Anim an = Demo.get(id);
                c.drawText(id + "  (" + an.keys.size() + " keys)", 6, r * (ch + 24) + 18, tp);
                for (int k = 0; k < Math.min(cols, an.keys.size()); k++) {
                    c.save();
                    c.translate(k * cw, r * (ch + 24) + 24);
                    c.clipRect(0, 0, cw - 4, ch);
                    c.drawColor(0xFF1A1324);
                    DemoView.draw(c, an, an.keys.get(k), cw - 4, ch, 1.5f, true);
                    c.restore();
                }
            }
            try (FileOutputStream o = new FileOutputStream(new File(dir, "demo_" + sheet + ".png"))) {
                b.compress(Bitmap.CompressFormat.PNG, 100, o);
            }
        }
    }

    @Test
    public void screens() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().resume().get();
        shot(a, "01_welcome");
        State s = fresh(a);
        TestFlow tf = new TestFlow(a, true);
        tf.profile();
        shot(a, "02_profile");
        tf.safety();
        shot(a, "03_health_check");
        tf.sitRise();
        shot(a, "04_test_sitrise");
        tf.balance();
        shot(a, "05_test_balance");
        tf.pushup();
        shot(a, "06_test_pushups");
        tf.march();
        shot(a, "07_test_march");
        tf.cooper();
        shot(a, "07b_test_run");
        for (java.util.Map.Entry<String, Double> e : couchPotato().raw.entrySet()) tf.t.raw.put(e.getKey(), e.getValue());
        tf.t.note.putAll(couchPotato().note);
        tf.results();
        shot(a, "08_test_results");

        // a few weeks of history for the charts
        LocalDate today = LocalDate.now();
        s.start = today.minusDays(35).toString();
        s.tests.get(0).date = s.start;
        s.snaps.clear();
        android.content.SharedPreferences.Editor ed = Steps.prefs(a).edit();
        for (int d = 35; d >= 1; d--) {
            LocalDate day = today.minusDays(d);
            int steps = 1800 + (35 - d) * 140 + (d % 3) * 700;
            ed.putInt("d:" + day, steps).putBoolean("t:" + day, true);
        }
        ed.putInt("d:" + today, 3420).putBoolean("t:" + today, true).apply();
        for (int d = 35; d >= 1; d--) {
            LocalDate day = today.minusDays(d);
            String type = Plan.typeFor(s, day);
            if (Plan.REST.equals(type) || d % 9 == 4) continue;
            State.Workout w = new State.Workout();
            w.date = day.toString();
            w.type = type;
            w.diff = d % 5 == 0 ? 2 : 1;
            w.items = Plan.build(s, day, type, w.diff, false);
            for (State.Item it : w.items) for (int i = 0; i < it.target.length; i++)
                it.done[i] = d % 7 == 3 && i == it.target.length - 1 ? it.target[i] * 2 / 3 : it.target[i];
            w.durSec = 900 + d * 13;
            w.feel = 3 + d % 3;
            Logic.finishWorkout(a, s, w);
        }
        s.streak = 11;
        s.freezes = 1;
        s.streakThrough = today.minusDays(1).toString();
        s.stepEvalThrough = today.minusDays(1).toString();
        State.Measure m1 = new State.Measure();
        m1.date = today.minusDays(30).toString(); m1.waist = 84; m1.hip = 99; m1.bust = 92; m1.weight = 68;
        State.Measure m2 = new State.Measure();
        m2.date = today.minusDays(2).toString(); m2.waist = 81.5; m2.hip = 100.5; m2.bust = 92; m2.weight = 67;
        s.measures.add(m1);
        s.measures.add(m2);
        Store.save(a, s);

        a.showTab(0);
        shot(a, "10_home");
        a.showTab(1);
        shot(a, "11_train");
        a.showTab(2);
        shot(a, "12_stats");
        a.showTab(3);
        shot(a, "13_health");
        a.showTab(4);
        shot(a, "14_me");

        // a workout
        State.Workout w = new State.Workout();
        w.date = today.toString();
        w.type = Plan.GLUTES_HIPS;
        w.diff = 1;
        w.items = Plan.build(s, today, Plan.GLUTES_HIPS, 1, false);
        WorkoutFlow wf = new WorkoutFlow(a, w, Plan.GLUTES_HIPS);
        wf.start();
        shot(a, "20_workout_warmup");
        wf.item = 2;
        wf.showSet();
        shot(a, "21_workout_reps");
        for (int i = 0; i < w.items.size(); i++) {
            if (Plan.ex(w.items.get(i).ex).kind == Plan.HOLD && Plan.ex(w.items.get(i).ex).cat >= 0) {
                wf.item = i;
                break;
            }
        }
        wf.showSet();
        shot(a, "22_workout_hold");
        wf.showRest(45, Plan.ex(w.items.get(3).ex));
        shot(a, "23_workout_rest");
        s.xp += 0;
        for (State.Item it : w.items) System.arraycopy(it.target, 0, it.done, 0, it.target.length);
        wf.finish();
        shot(a, "24_workout_passed");

        // the how-to page and the library (dialogs: drawn from their content views)
        android.app.Dialog d = new android.app.Dialog(a);
        for (String id : new String[]{"glute_bridge", "clamshell", "pushup"}) {
            android.widget.LinearLayout c = Ui.vbox(a);
            c.setBackgroundColor(Ui.BG);
            c.setPadding(48, 48, 48, 48);
            HowTo.fill(a, d, c, id);
            android.widget.FrameLayout holder = new android.widget.FrameLayout(a);
            holder.setBackgroundColor(Ui.BG);
            holder.addView(c);
            a.setScreen(holder, false);
            shot(a, "30_howto_" + id);
        }
        HowTo.library(a);
        android.app.Dialog lib = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        View lv = lib.getWindow().getDecorView();
        lv.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        lv.layout(0, 0, W, H);
        Bitmap lb = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        lv.draw(new Canvas(lb));
        try (FileOutputStream o = new FileOutputStream(new File(System.getProperty("screens.out", "build/screens"), "31_library.png"))) {
            lb.compress(Bitmap.CompressFormat.PNG, 100, o);
        }
        lib.dismiss();

        List<State.Item> rec = Plan.build(s, today, Plan.RECOVERY, 1, false);
        assertTrue(rec.size() > 3);
    }
}
