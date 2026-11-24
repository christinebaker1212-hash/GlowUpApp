package com.glowup.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Welcome, profile, health check and the fitness test (also the 4-weekly retest). */
final class TestFlow {
    final MainActivity a;
    final boolean first;
    final State.Test t = new State.Test();
    double[] before;

    TestFlow(MainActivity a, boolean first) {
        this.a = a;
        this.first = first;
    }

    void start() {
        before = a.st.elo.clone();
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (first) welcome(); else intro();
    }

    // ---------------- onboarding ----------------

    void welcome() {
        LinearLayout col = a.scrollPage(false);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(Ui.space(a, 50));
        TextView logo = Ui.head(a, Flavor.APP, Flavor.MALE ? 46 : 56, Ui.PINK);
        col.addView(logo);
        col.addView(Ui.text(a, Flavor.TAGLINE, 14, Ui.DIM));
        LinearLayout c = Ui.card(a);
        c.addView(Ui.text(a, Flavor.WELCOME, 15, Ui.TEXT));
        col.addView(c, Ui.lp(a, 30));
        col.addView(Ui.button(a, "LET'S GO", Ui.PINK, v -> profile()), Ui.lp(a, 24));
    }

    void profile() {
        LinearLayout col = a.scrollPage(false);
        State s = a.st;
        col.addView(Ui.head(a, "ABOUT YOU", 30, Ui.TEXT));
        col.addView(Ui.text(a, "Your birth date lets the app age-grade your results, so your rating stays fair for the "
            + "next 30 years and beyond.", 14, Ui.DIM), Ui.lp(a, 6));
        TextView name = row(col, "NAME", s.name.isEmpty() ? "tap to set" : s.name);
        name.setOnClickListener(v -> a.askText("Your name", s.name, n -> { s.name = n; profile(); }));
        TextView birth = row(col, "BIRTH DATE", s.birth.isEmpty() ? "tap to set" : s.birth);
        birth.setOnClickListener(v -> a.askDate(s.birth, d -> { s.birth = d.toString(); profile(); }));
        TextView height = row(col, Flavor.MALE ? "HEIGHT (for your body fat)" : "HEIGHT (optional)", s.heightCm <= 0 ? "tap to set" : Screens.height(s, s.heightCm));
        height.setOnClickListener(v -> a.askNumber(s.imperial ? "Height in inches" : "Height in cm", s.imperial ? "e.g. 65" : "e.g. 165",
            0, val -> { s.heightCm = s.imperial ? val * 2.54 : val; profile(); }));
        TextView units = row(col, "UNITS", s.imperial ? "inches / pounds" : "cm / kg");
        units.setOnClickListener(v -> { s.imperial = !s.imperial; profile(); });
        TextView next = Ui.button(a, "NEXT", s.birth.isEmpty() ? Ui.CARD2 : Ui.PINK, v -> {
            if (s.birth.isEmpty()) return;
            safety();
        });
        col.addView(next, Ui.lp(a, 24));
    }

    TextView row(LinearLayout col, String label, String value) {
        LinearLayout r = Ui.card(a);
        r.addView(Ui.text(a, label, 12, Ui.DIM));
        TextView v = Ui.bold(a, value, 18, Ui.TEXT);
        r.addView(v);
        col.addView(r, Ui.lp(a, 12));
        r.setOnClickListener(x -> v.performClick());
        return v;
    }

    static final String[] PARQ = {
        "Has a doctor ever said you have a heart condition or high blood pressure?",
        "Do you feel pain in your chest at rest, during daily activities, or when you do physical activity?",
        "Do you lose balance because of dizziness, or have you lost consciousness in the last 12 months?",
        "Have you been diagnosed with another chronic condition (other than heart disease or high blood pressure)?",
        "Do you take prescribed medication for a chronic condition?",
        "Do you have a bone, joint or soft-tissue problem that could get worse with more physical activity?",
        "Has a doctor ever said you should only do medically supervised physical activity?",
    };

    void safety() {
        LinearLayout col = a.scrollPage(false);
        col.addView(Ui.head(a, "QUICK HEALTH CHECK", 28, Ui.TEXT));
        col.addView(Ui.text(a, "The standard pre-exercise questions (PAR-Q+). Tap any that are YES.", 14, Ui.DIM), Ui.lp(a, 6));
        boolean[] yes = new boolean[PARQ.length];
        for (int i = 0; i < PARQ.length; i++) {
            final int q = i;
            TextView b = Ui.text(a, "○  " + PARQ[i], 15, Ui.TEXT);
            b.setBackground(Ui.round(Ui.CARD, Ui.dp(a, 12)));
            b.setPadding(Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 14), Ui.dp(a, 12));
            b.setOnClickListener(v -> {
                yes[q] = !yes[q];
                b.setText((yes[q] ? "●  YES: " : "○  ") + PARQ[q]);
                b.setTextColor(yes[q] ? Ui.ORANGE : Ui.TEXT);
            });
            col.addView(b, Ui.lp(a, 8));
        }
        col.addView(Ui.button(a, "CONTINUE", Ui.PINK, v -> {
            boolean any = false;
            for (boolean y : yes) any |= y;
            a.st.parqFlag = any;
            if (any) {
                new android.app.AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Check with your doctor")
                    .setMessage("Because you answered YES, please talk to your doctor before starting a new exercise "
                        + "program, and show them this plan. You can still take the gentle test, going at your own pace "
                        + "and stopping whenever something doesn't feel right.")
                    .setPositiveButton("I UNDERSTAND", (d, w) -> intro()).show();
            } else intro();
        }), Ui.lp(a, 20));
    }

    // ---------------- the test ----------------

    void intro() {
        if (first) a.askPermissions(); // (steps: the march test can count for you)
        LinearLayout col = a.scrollPage(false);
        col.addView(Ui.head(a, first ? "THE BASELINE TEST" : "RETEST", 30, Ui.TEXT));
        col.addView(Ui.text(a, "Seven short tests, about 15 minutes, plus an optional run. You need: a sturdy chair without wheels, a wall, a "
            + "couch or bed edge, and a clear bit of floor.\n\n"
            + "• Go to the point where your form breaks, not to pain.\n"
            + "• Rest a minute or two between tests.\n"
            + "• If you can't do one at all, tap CAN'T DO THIS. That's a real result: it's your starting line, and "
            + "you'll beat it.\n"
            + "• Stop if you feel chest pain, dizziness or sharp joint pain.", 15, Ui.TEXT), Ui.lp(a, 8));
        col.addView(Ui.button(a, "BEGIN", Ui.PINK, v -> sitRise()), Ui.lp(a, 24));
    }

    LinearLayout page(int n, String title, String measures, String how) {
        LinearLayout col = a.scrollPage(false);
        col.addView(Ui.text(a, "TEST " + n + " OF 8", 12, Ui.PINK));
        Ui.XpBar xb = new Ui.XpBar(a, (n - 1) / 8f);
        col.addView(xb, Ui.lp(a, 6));
        col.addView(Ui.head(a, title, 28, Ui.TEXT), Ui.lp(a, 12));
        col.addView(Ui.text(a, measures, 13, Ui.CYAN), Ui.lp(a, 2));
        demoBox = new android.widget.FrameLayout(a);
        col.addView(demoBox, Ui.lp(a, 10));
        String demo = DEMOS[n - 1];
        if (title.startsWith("BONUS")) demo = "sl_stand_closed";
        setDemo(demo);
        col.addView(Ui.text(a, how, 15, Ui.TEXT), Ui.lp(a, 10));
        return col;
    }

    /** Each test's demonstration (push-ups and planks follow the version picked). */
    static final String[] DEMOS = {"sit_rise", "single_leg_stand", "chair_squat", "wall_pushup", "glute_bridge", "knee_plank", "march", "jog"};
    static final String[] PU_DEMO = {"wall_pushup", "counter_pushup", "knee_pushup", "pushup"};
    static final String[] PL_DEMO = {"wall_plank", "counter_plank", "knee_plank", "plank"};
    android.widget.FrameLayout demoBox;

    void setDemo(String id) {
        demoBox.removeAllViews();
        demoBox.addView(HowTo.card(a, id, 200, Plan.ex(id).id.equals(id)));
    }

    void cant(LinearLayout col, String key, Runnable next) {
        col.addView(Ui.ghost(a, "CAN'T DO THIS", v -> {
            t.raw.put(key, 0.0);
            next.run();
        }), Ui.lp(a, 10));
    }

    void sitRise() {
        LinearLayout col = page(1, "SIT & RISE", "Strength, flexibility and balance working together.",
            "Stand barefoot on a clear floor. Cross your legs and lower yourself to sit, then stand back up, using as "
                + "little support as you can.\n\nStart at 10 points. Take away 1 point every time you use a hand, a "
                + "knee, a forearm or the side of a leg to get down or up, and half a point if you wobble. Use a chair "
                + "or wall nearby for safety.");
        Widgets.Stepper sp = new Widgets.Stepper(a, 5, 0.5, 0, 10);
        col.addView(Ui.text(a, "YOUR SCORE", 12, Ui.DIM), Ui.lp(a, 16));
        col.addView(sp, Ui.lp(a, 6));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_SITRISE, sp.value);
            balance();
        }), Ui.lp(a, 18));
        cant(col, Science.T_SITRISE, this::balance);
    }

    void balance() {
        LinearLayout col = page(2, "ONE-LEG STAND", "Balance: steadiness on your feet. It improves quickly with practice.",
            "Stand near a wall. Lift one foot off the floor (don't rest it on the other leg), arms however you like, "
                + "eyes open. Press START as you lift, STOP when your foot touches down or you grab the wall. It stops "
                + "by itself at 60 seconds. Then the other leg.");
        int[] legs = {-1, -1};
        TextView label = Ui.head(a, "LEFT LEG", 18, Ui.CYAN);
        label.setGravity(Gravity.CENTER);
        col.addView(label, Ui.lp(a, 14));
        Widgets.Watch wt = new Widgets.Watch(a, false, 0, 60);
        col.addView(wt, Ui.lp(a, 4));
        TextView next = Ui.button(a, "NEXT", Ui.CARD2, null);
        wt.onStop = () -> {
            if (legs[0] < 0) {
                legs[0] = wt.seconds;
                label.setText("RIGHT LEG (left: " + legs[0] + "s)");
                wt.time.setText("0");
                wt.btn.setText("START RIGHT LEG");
                wt.btn.setBackground(Ui.round(Ui.GREEN, Ui.dp(a, 14)));
                wt.btn.setTextColor(0xFF1A0F1F);
            } else {
                legs[1] = wt.seconds;
                double avg = (legs[0] + legs[1]) / 2.0;
                label.setText("LEFT " + legs[0] + "s  •  RIGHT " + legs[1] + "s");
                next.setBackground(Ui.round(Ui.PINK, Ui.dp(a, 14)));
                next.setOnClickListener(v -> {
                    if (avg >= 59.5) eyesClosed(); else {
                        t.raw.put(Science.T_BALANCE, avg);
                        chair();
                    }
                });
            }
        };
        col.addView(next, Ui.lp(a, 18));
        cant(col, Science.T_BALANCE, this::chair);
    }

    void eyesClosed() {
        LinearLayout col = page(2, "BONUS: EYES CLOSED", "You maxed it out! This tells elite balance apart.",
            "Right next to a wall: stand on your stronger leg and close your eyes. START as you close them, STOP "
                + "when you touch down or open your eyes.");
        Widgets.Watch wt = new Widgets.Watch(a, false, 0, 60);
        col.addView(wt, Ui.lp(a, 14));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_BALANCE, 60.0 + wt.seconds * 3.0);
            chair();
        }), Ui.lp(a, 18));
    }

    void chair() {
        LinearLayout col = page(3, "30-SECOND CHAIR STAND", "Leg and glute strength (the CDC's standard test).",
            "Sit in the middle of a sturdy chair (against a wall so it can't slide), feet flat, arms crossed over "
                + "your chest. When the timer starts, stand all the way up and sit all the way down as many times as "
                + "you can in 30 seconds. Count each full stand.");
        Widgets.Watch wt = new Widgets.Watch(a, true, 0, 30);
        col.addView(wt, Ui.lp(a, 14));
        col.addView(Ui.text(a, "FULL STANDS", 12, Ui.DIM), Ui.lp(a, 16));
        Widgets.Stepper sp = new Widgets.Stepper(a, 0, 1, 0, 80);
        col.addView(sp, Ui.lp(a, 6));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_CHAIR, sp.value);
            pushup();
        }), Ui.lp(a, 18));
        cant(col, Science.T_CHAIR, this::pushup);
    }

    static final String[] PU_VAR = {"WALL", "COUNTER", "KNEE", "FULL"};
    static final double[] PU_F = {0.15, 0.3, 0.5, 1.0};
    int puVar = 0;

    void pushup() {
        LinearLayout col = page(4, "PUSH-UPS", "Upper-body strength and endurance.",
            "Pick the hardest version you can do at least a few of with good form: body in a straight line, chest "
                + "close to the wall / counter / floor each rep. Do as many as you can without stopping to rest; stop "
                + "when your form breaks.");
        LinearLayout chips = Ui.hbox(a);
        TextView[] cs = new TextView[4];
        Runnable paint = () -> {
            for (int i = 0; i < 4; i++) {
                cs[i].setBackground(Ui.round(i == puVar ? Ui.ORANGE : Ui.CARD, Ui.dp(a, 12)));
                cs[i].setTextColor(i == puVar ? 0xFF1A0F1F : Ui.TEXT);
            }
        };
        for (int i = 0; i < 4; i++) {
            final int k = i;
            cs[i] = Ui.head(a, PU_VAR[i], 14, Ui.TEXT);
            cs[i].setGravity(Gravity.CENTER);
            cs[i].setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 12));
            cs[i].setOnClickListener(v -> { puVar = k; paint.run(); setDemo(PU_DEMO[k]); });
            chips.addView(cs[i], Ui.weight(a, 1, 3));
        }
        paint.run();
        setDemo(PU_DEMO[puVar]);
        col.addView(chips, Ui.lp(a, 14));
        col.addView(Ui.text(a, "REPS", 12, Ui.DIM), Ui.lp(a, 16));
        Widgets.Stepper sp = new Widgets.Stepper(a, 0, 1, 0, 150);
        col.addView(sp, Ui.lp(a, 6));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_PUSHUP, sp.value * PU_F[puVar]);
            t.note.put(Science.T_PUSHUP, PU_VAR[puVar].toLowerCase(Locale.US));
            bridge();
        }), Ui.lp(a, 18));
        cant(col, Science.T_PUSHUP, this::bridge);
    }

    void bridge() {
        LinearLayout col = page(5, "GLUTE BRIDGES", "Glute strength and endurance: your CURVES stat.",
            "Lie on your back, knees bent, feet flat and hip-width apart. Squeeze your glutes and lift your hips until "
                + "knees, hips and shoulders line up, then lower until you nearly touch the floor. Steady pace, about "
                + "one every 2 seconds. Count until you can't lift all the way.");
        col.addView(Ui.text(a, "REPS", 12, Ui.DIM), Ui.lp(a, 16));
        Widgets.Stepper sp = new Widgets.Stepper(a, 0, 1, 0, 300);
        col.addView(sp, Ui.lp(a, 6));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_BRIDGE, sp.value);
            plank();
        }), Ui.lp(a, 18));
        cant(col, Science.T_BRIDGE, this::plank);
    }

    static final String[] PL_VAR = {"WALL", "COUNTER", "KNEE", "FOREARM"};
    static final double[] PL_F = {0.2, 0.35, 0.55, 1.0};
    int plVar = 2;

    void plank() {
        LinearLayout col = page(6, "PLANK HOLD", "Core strength and endurance.",
            "Choose a version, get into position on your forearms with a straight body, glutes squeezed. START, and "
                + "STOP when your hips sag or lift, or you need to drop.");
        LinearLayout chips = Ui.hbox(a);
        TextView[] cs = new TextView[4];
        Runnable paint = () -> {
            for (int i = 0; i < 4; i++) {
                cs[i].setBackground(Ui.round(i == plVar ? Ui.PURPLE : Ui.CARD, Ui.dp(a, 12)));
                cs[i].setTextColor(Ui.TEXT);
            }
        };
        for (int i = 0; i < 4; i++) {
            final int k = i;
            cs[i] = Ui.head(a, PL_VAR[i], 14, Ui.TEXT);
            cs[i].setGravity(Gravity.CENTER);
            cs[i].setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 12));
            cs[i].setOnClickListener(v -> { plVar = k; paint.run(); setDemo(PL_DEMO[k]); });
            chips.addView(cs[i], Ui.weight(a, 1, 3));
        }
        paint.run();
        setDemo(PL_DEMO[plVar]);
        col.addView(chips, Ui.lp(a, 14));
        Widgets.Watch wt = new Widgets.Watch(a, false, 0, 600);
        col.addView(wt, Ui.lp(a, 14));
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            t.raw.put(Science.T_PLANK, wt.seconds * PL_F[plVar]);
            t.note.put(Science.T_PLANK, PL_VAR[plVar].toLowerCase(Locale.US));
            march();
        }), Ui.lp(a, 18));
        cant(col, Science.T_PLANK, this::march);
    }

    void march() {
        LinearLayout col = page(7, "2-MINUTE MARCH", "Stamina: heart and lungs (the 2-minute step test).",
            "Hold your phone or put it in a pocket. March in place for 2 minutes, lifting each knee to halfway between "
                + "your knee and hip (mark the height on a wall if you like). Slow down or rest if you need to; the "
                + "clock keeps going. The phone counts your steps; fix the count afterwards if it's off.\n\n"
                + "The score is how many times your RIGHT knee came up.");
        Widgets.Watch wt = new Widgets.Watch(a, true, 0, 120);
        col.addView(wt, Ui.lp(a, 14));
        col.addView(Ui.text(a, "RIGHT-KNEE LIFTS", 12, Ui.DIM), Ui.lp(a, 16));
        Widgets.Stepper sp = new Widgets.Stepper(a, 0, 1, 0, 250);
        col.addView(sp, Ui.lp(a, 6));
        SensorManager sm = (SensorManager) a.getSystemService(MainActivity.SENSOR_SERVICE);
        Sensor det = sm == null || !Steps.permitted(a) ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        int[] count = {0};
        SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                if (!wt.running) return;
                count[0]++;
                sp.set(count[0] / 2);
            }

            @Override
            public void onAccuracyChanged(Sensor s, int acc) {}
        };
        if (det != null) {
            sm.registerListener(l, det, SensorManager.SENSOR_DELAY_FASTEST);
            col.addView(Ui.text(a, "Step sensor ready: it counts while the timer runs.", 12, Ui.GREEN), Ui.lp(a, 6));
        } else {
            col.addView(Ui.text(a, "Count your right knee yourself and enter it here.", 12, Ui.DIM), Ui.lp(a, 6));
        }
        Runnable done = () -> { if (sm != null) sm.unregisterListener(l); };
        col.addView(Ui.button(a, "NEXT", Ui.PINK, v -> {
            done.run();
            t.raw.put(Science.T_MARCH, sp.value);
            cooper();
        }), Ui.lp(a, 18));
        col.addView(Ui.ghost(a, "CAN'T DO THIS", v -> {
            done.run();
            t.raw.put(Science.T_MARCH, 0.0);
            cooper();
        }), Ui.lp(a, 10));
    }

    void cooper() {
        boolean mi = a.st.imperial;
        LinearLayout col = page(8, "12-MINUTE RUN (OPTIONAL)", "Stamina, measured properly: the Cooper test, which also estimates your VO2max.",
            "Only if you can jog for 12 minutes without stopping; skip it until then. The march can only rate STAMINA "
                + "up to ATHLETIC, so this is how STAMINA climbs past it.\n\nWarm up, then cover as much distance as you "
                + "can in 12 minutes on a track, a treadmill or with a running app. Walking breaks are allowed but cost "
                + "distance. Enter the distance.");
        Widgets.Watch wt = new Widgets.Watch(a, true, 0, 720);
        col.addView(wt, Ui.lp(a, 14));
        col.addView(Ui.text(a, mi ? "DISTANCE (MILES)" : "DISTANCE (METRES)", 12, Ui.DIM), Ui.lp(a, 16));
        Widgets.Stepper sp = mi ? new Widgets.Stepper(a, 1.0, 0.05, 0, 3) : new Widgets.Stepper(a, 1600, 50, 0, 5000);
        col.addView(sp, Ui.lp(a, 6));
        col.addView(Ui.button(a, "SEE MY RESULTS", Ui.GOLD, v -> {
            t.raw.put(Science.T_COOPER, mi ? sp.value * 1609.344 : sp.value);
            results();
        }), Ui.lp(a, 18));
        col.addView(Ui.ghost(a, "SKIP: SEE MY RESULTS", v -> results()), Ui.lp(a, 10));
    }

    // ---------------- results ----------------

    void results() {
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (String k : new String[]{Science.T_SITRISE, Science.T_BALANCE, Science.T_CHAIR, Science.T_PUSHUP,
            Science.T_BRIDGE, Science.T_PLANK, Science.T_MARCH}) t.raw.putIfAbsent(k, 0.0);
        double[] cat = Logic.finishTest(a, a.st, t);
        LinearLayout col = a.scrollPage(false);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        double overall = Science.overall(a.st.elo);
        int tier = Science.tier(overall);
        col.addView(Ui.text(a, first ? (Flavor.MALE ? "YOUR STARTING POWER LEVEL" : "YOUR STARTING RANK") : "RETEST RESULTS", 13, Ui.DIM), Ui.lp(a, 20));
        TextView rk = Ui.head(a, Science.rankName(overall), 34, Science.TIER_COLOR[tier]);
        rk.setGravity(Gravity.CENTER);
        rk.setAlpha(0);
        rk.animate().alpha(1).setDuration(800).start();
        col.addView(rk, Ui.lp(a, 4));
        col.addView(Ui.head(a, Flavor.RATING + " " + Ui.fmt(overall), 20, Ui.TEXT), Ui.lp(a, 4));
        TextView stand = Ui.text(a, Science.standing(overall), 14, Ui.DIM);
        stand.setGravity(Gravity.CENTER);
        col.addView(stand, Ui.lp(a, 2));

        LinearLayout card = Ui.card(a);
        for (int c = 0; c < 5; c++) {
            LinearLayout row = Ui.hbox(a);
            LinearLayout lab = Ui.vbox(a);
            lab.addView(Ui.head(a, Science.CAT_LABEL[c], 14, Ui.TEXT));
            lab.addView(Ui.text(a, Science.CAT_SUB[c], 11, Ui.DIM));
            lab.addView(Ui.text(a, "~" + pct(a.st.elo[c]), 11, Ui.CAT_COLOR[c]));
            row.addView(lab, new LinearLayout.LayoutParams(Ui.dp(a, 120), -2));
            Ui.SegBar sb = new Ui.SegBar(a, (float) (a.st.elo[c] / Science.MAX), Ui.CAT_COLOR[c]);
            if (!first) sb.ghost = (float) (before[c] / Science.MAX);
            row.addView(sb, new LinearLayout.LayoutParams(0, Ui.dp(a, 12), 1));
            TextView v = Ui.head(a, Ui.fmt(a.st.elo[c]), 15, Ui.TEXT);
            v.setGravity(Gravity.END);
            row.addView(v, new LinearLayout.LayoutParams(Ui.dp(a, 48), -2));
            card.addView(row, Ui.lp(a, c == 0 ? 0 : 12));
        }
        col.addView(card, Ui.lp(a, 18));
        if (!first) {
            col.addView(Ui.text(a, "Test scores alone: " + Ui.fmt(Science.overall(cat)) + ". Your new ratings average the "
                + "test with your workout record, so one good or bad day can't swing them too far.", 13, Ui.DIM), Ui.lp(a, 10));
        }

        Double coop = t.raw.get(Science.T_COOPER);
        if (coop != null && coop > 0) {
            col.addView(Ui.text(a, String.format(Locale.US, "Estimated VO2max from your run: %.0f ml/kg/min", Science.vo2max(coop)), 14, Ui.CYAN), Ui.lp(a, 10));
        }

        LinearLayout scale = Ui.card(a);
        scale.addView(Ui.text(a, "THE LADDER: 40 RANKS, V (LOW) TO I (HIGH) IN EACH", 12, Ui.DIM));
        for (int i = 7; i >= 0; i--) {
            LinearLayout r = Ui.hbox(a);
            LinearLayout nmBox = Ui.vbox(a);
            nmBox.addView(Ui.head(a, Science.TIERS[i], 15, i == tier ? Science.TIER_COLOR[i] : Ui.DIM));
            nmBox.addView(Ui.text(a, Science.TIER_MEANS[i], 11, Ui.DIM));
            r.addView(nmBox, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(Ui.text(a, (i * 400) + (i == 7 ? "+" : "–" + (i * 400 + 399)), 13, i == tier ? Ui.TEXT : Ui.DIM));
            if (i == tier) r.setBackground(Ui.round(Ui.CARD2, Ui.dp(a, 8)));
            r.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4));
            scale.addView(r, Ui.lp(a, 2));
        }
        scale.addView(Ui.text(a, "Ratings compare you with " + Flavor.PEERS + " your age, using published fitness norms, and "
            + Science.TIERS[7] + " takes results Olympic athletes post. Most people never get there, and that's fine: the point is "
            + "watching your own numbers climb.", 12, Ui.DIM), Ui.lp(a, 8));
        col.addView(scale, Ui.lp(a, 16));
        col.addView(Ui.text(a, "+150 XP", 18, Ui.PINK), Ui.lp(a, 14));
        col.addView(Ui.button(a, first ? "START MY JOURNEY" : "BACK TO HOME", Ui.PINK, v -> {
            if (first) {
                a.askPermissions();
                Steps.schedule(a);
                Reminder.schedule(a, a.st);
            }
            a.showTab(0);
        }), Ui.lp(a, 20));
        if (first) col.addView(Ui.text(a, "Next, the app will ask to count your steps (for STAMINA) and to send a "
            + "daily reminder. Both stay on your phone.", 12, Ui.DIM), Ui.lp(a, 8));
    }

    static String pct(double r) {
        double p = Science.percentile(r);
        if (r >= 2800) return "Olympic level";
        if (r >= 2600) return "national elite";
        if (p >= 99) return "top 1%";
        if (p < 1) return "bottom 1%";
        long n = Math.round(p);
        String suf = n % 100 >= 11 && n % 100 <= 13 ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suf + " percentile";
    }
}
