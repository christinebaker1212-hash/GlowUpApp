package com.glowup.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    State st;
    FrameLayout content;
    LinearLayout nav;
    int tab = 0;
    TextView liveSteps;
    final List<String> pendingNews = new ArrayList<>();
    private static final int REQ_EXPORT = 11, REQ_IMPORT = 12, REQ_PERMS = 13;

    static final String[] TABS = {"HOME", "TRAIN", "STATS", "HEALTH", "ME"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = Store.load(this);
        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = Ui.hbox(this);
        nav.setBackgroundColor(0xFF140E1C);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else {
                v.setPadding(ins.getSystemWindowInsetLeft(), ins.getSystemWindowInsetTop(),
                    ins.getSystemWindowInsetRight(), ins.getSystemWindowInsetBottom());
            }
            return ins;
        });
        setContentView(root);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(0xFF140E1C);
        if (!st.onboarded()) {
            new TestFlow(this, true).start();
        } else {
            showTab(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (st.onboarded()) {
            pendingNews.addAll(Logic.daily(this, st));
            Steps.schedule(this);
            Reminder.schedule(this, st);
            Updater.check(this, false);
            if (nav.getVisibility() == View.VISIBLE && tab == 0) showTab(0);
            flushNews();
        }
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm != null && st.stepsOn && Steps.permitted(this)) {
            Sensor s = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm != null) sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        Steps.reading(this, e.values[0]);
        if (liveSteps != null) liveSteps.setText(String.format(Locale.US, "%,d", Steps.on(this, LocalDate.now().toString())));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    void flushNews() {
        if (pendingNews.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String n : pendingNews) sb.append("• ").append(n).append("\n\n");
        pendingNews.clear();
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("WHILE YOU WERE AWAY").setMessage(sb.toString().trim()).setPositiveButton("NICE", null).show();
    }

    @Override
    public void onBackPressed() {
        if (nav.getVisibility() != View.VISIBLE && st.onboarded()) {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Leave?").setMessage("Progress on this screen won't be saved.")
                .setPositiveButton("LEAVE", (d, w) -> showTab(tab)).setNegativeButton("STAY", null).show();
            return;
        }
        if (tab != 0 && st.onboarded()) {
            showTab(0);
            return;
        }
        super.onBackPressed();
    }

    // ---------------- frame ----------------

    void setScreen(View v, boolean showNav) {
        content.removeAllViews();
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        nav.setVisibility(showNav ? View.VISIBLE : View.GONE);
        liveSteps = null;
    }

    LinearLayout scrollPage(boolean showNav) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout col = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        col.setPadding(p, p, p, p * 2);
        sv.addView(col);
        setScreen(sv, showNav);
        return col;
    }

    void buildNav() {
        nav.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            TextView t = Ui.head(this, TABS[i], 13, i == tab ? Ui.PINK : Ui.DIM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            t.setOnClickListener(v -> showTab(idx));
            nav.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        }
    }

    void showTab(int i) {
        tab = i;
        buildNav();
        switch (i) {
            case 0: showHome(); break;
            case 1: showTrain(); break;
            case 2: Screens.stats(this); break;
            case 3: Screens.health(this); break;
            default: Screens.me(this); break;
        }
    }

    // ---------------- HOME ----------------

    void showHome() {
        LinearLayout col = scrollPage(true);
        LocalDate today = LocalDate.now();
        double overall = Science.overall(st.elo);
        int tier = Science.tier(overall);

        // header: greeting + level
        LinearLayout top = Ui.hbox(this);
        LinearLayout hl = Ui.vbox(this);
        hl.addView(Ui.text(this, today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).toUpperCase(Locale.US), 12, Ui.DIM));
        hl.addView(Ui.head(this, "HEY " + (st.name.isEmpty() ? "YOU" : st.name.toUpperCase(Locale.US)), 26, Ui.TEXT));
        top.addView(hl, new LinearLayout.LayoutParams(0, -2, 1));
        int[] lv = Science.level(st.xp);
        TextView lvBadge = Ui.head(this, "LV " + lv[0], 22, 0xFF1A0F1F);
        lvBadge.setBackground(Ui.round(Ui.GOLD, Ui.dp(this, 12)));
        lvBadge.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        top.addView(lvBadge);
        col.addView(top);

        // XP bar
        Ui.XpBar xp = new Ui.XpBar(this, lv[1] / (float) lv[2]);
        col.addView(xp, Ui.lp(this, 12));
        TextView xpt = Ui.text(this, lv[1] + " / " + lv[2] + " XP to level " + (lv[0] + 1), 12, Ui.DIM);
        col.addView(xpt, Ui.lp(this, 4));

        // rank card
        LinearLayout rank = Ui.card(this);
        LinearLayout rr = Ui.hbox(this);
        LinearLayout rl = Ui.vbox(this);
        rl.addView(Ui.text(this, "FITNESS RATING", 12, Ui.DIM));
        rl.addView(Ui.head(this, Science.rankName(overall), 22, Science.TIER_COLOR[tier]));
        rr.addView(rl, new LinearLayout.LayoutParams(0, -2, 1));
        TextView elo = Ui.head(this, Ui.fmt(overall), 40, Ui.TEXT);
        rr.addView(elo);
        rank.addView(rr);
        rank.addView(Ui.text(this, Science.standing(overall).substring(0, 1).toUpperCase(Locale.US)
            + Science.standing(overall).substring(1), 13, Ui.TEXT), Ui.lp(this, 2));
        double next = Science.nextRank(overall);
        if (next > 0) rank.addView(Ui.text(this, Ui.fmt(Math.ceil(next - overall)) + " to " + Science.rankName(next)
            + "  •  rank " + (Science.rankIndex(overall) + 1) + " of 40", 12, Ui.DIM), Ui.lp(this, 2));
        // stat bars
        for (int c = 0; c < 5; c++) {
            LinearLayout row = Ui.hbox(this);
            row.addView(Ui.head(this, Science.CAT_LABEL[c], 13, Ui.TEXT), new LinearLayout.LayoutParams(Ui.dp(this, 82), -2));
            Ui.SegBar bar = new Ui.SegBar(this, (float) (st.elo[c] / Science.MAX), Ui.CAT_COLOR[c]);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Ui.dp(this, 12), 1);
            row.addView(bar, bp);
            TextView v = Ui.head(this, Ui.fmt(st.elo[c]), 13, Ui.DIM);
            v.setGravity(Gravity.END);
            row.addView(v, new LinearLayout.LayoutParams(Ui.dp(this, 44), -2));
            rank.addView(row, Ui.lp(this, 10));
        }
        col.addView(rank, Ui.lp(this, 16));

        // today's mission
        String type = Plan.typeFor(st, today);
        State.Workout done = st.workoutOn(today.toString());
        LinearLayout m = Ui.card(this);
        m.setBackground(Ui.round(Ui.CARD2, Ui.dp(this, 16)));
        m.addView(Ui.text(this, "TODAY'S MISSION", 12, Ui.PINK));
        m.addView(Ui.head(this, type, 26, Ui.TEXT));
        if (done != null) {
            m.addView(Ui.text(this, "Complete! +" + done.xp + " XP. " + (Plan.REST.equals(type) ? "" :
                "Come back tomorrow, or go again for a BONUS round."), 14, Ui.GREEN), Ui.lp(this, 6));
            m.addView(Ui.ghost(this, "BONUS ROUND", v -> showTrain()), Ui.lp(this, 12));
        } else if (Plan.REST.equals(type)) {
            m.addView(Ui.text(this, "Muscle grows while you rest. Today counts for your streak by itself. "
                + "A walk or the RECOVERY FLOW is a bonus.", 14, Ui.DIM), Ui.lp(this, 6));
            m.addView(Ui.ghost(this, "DO THE RECOVERY FLOW ANYWAY", v -> startWorkout(Plan.RECOVERY, 0, false, false)), Ui.lp(this, 12));
        } else {
            m.addView(Ui.text(this, missionBlurb(type), 14, Ui.DIM), Ui.lp(this, 6));
            m.addView(Ui.button(this, "START", Ui.PINK, v -> showTrain()), Ui.lp(this, 12));
        }
        col.addView(m, Ui.lp(this, 16));

        // steps + streak
        LinearLayout two = Ui.hbox(this);
        two.setBaselineAligned(false);
        LinearLayout sc = Ui.card(this);
        sc.setGravity(Gravity.CENTER_HORIZONTAL);
        int steps = Steps.on(this, today.toString());
        int goal = Logic.goalOn(this, st, today);
        sc.addView(Ui.text(this, "STEPS", 12, Ui.DIM));
        FrameLayout ringBox = new FrameLayout(this);
        ringBox.addView(new Ui.Ring(this, steps / (float) goal, Ui.CYAN, 96));
        liveSteps = Ui.head(this, String.format(Locale.US, "%,d", steps), 20, Ui.TEXT);
        liveSteps.setGravity(Gravity.CENTER);
        ringBox.addView(liveSteps, new FrameLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 96)));
        sc.addView(ringBox, Ui.lp(this, 6));
        String stepNote = !st.stepsOn ? "Step counting is off" : !Steps.available(this) ? "No step sensor"
            : !Steps.permitted(this) ? "Tap ME to allow steps" : String.format(Locale.US, "goal %,d", goal);
        sc.addView(Ui.text(this, stepNote, 12, Ui.DIM), Ui.lp(this, 6));
        two.addView(sc, Ui.weight(this, 1, 0));
        two.addView(Ui.space(this, 1), new LinearLayout.LayoutParams(Ui.dp(this, 12), 1));
        LinearLayout stc = Ui.card(this);
        stc.setGravity(Gravity.CENTER_HORIZONTAL);
        stc.addView(Ui.text(this, "STREAK", 12, Ui.DIM));
        int streak = Logic.streakNow(this, st);
        stc.addView(Ui.head(this, String.valueOf(streak), 44, streak > 0 ? Ui.ORANGE : Ui.DIM), Ui.lp(this, 6));
        stc.addView(Ui.text(this, streak == 1 ? "day" : "days", 13, Ui.DIM));
        StringBuilder fz = new StringBuilder();
        for (int i = 0; i < 3; i++) fz.append(i < st.freezes ? "❄ " : "· ");
        stc.addView(Ui.text(this, fz.toString().trim() + "\nfreezes", 12, Ui.CYAN), Ui.lp(this, 8));
        ((TextView) stc.getChildAt(stc.getChildCount() - 1)).setGravity(Gravity.CENTER);
        two.addView(stc, Ui.weight(this, 1, 0));
        col.addView(two, Ui.lp(this, 16));

        // retest
        State.Test lt = st.lastTest();
        long since = ChronoUnit.DAYS.between(LocalDate.parse(lt.date), today);
        if (since >= 28) {
            LinearLayout rc = Ui.card(this);
            rc.addView(Ui.head(this, "RETEST AVAILABLE", 18, Ui.GOLD));
            rc.addView(Ui.text(this, "It's been " + since + " days since your last fitness test. Retest to prove your "
                + "progress and recalibrate your ratings (+150 XP).", 14, Ui.DIM), Ui.lp(this, 4));
            rc.addView(Ui.button(this, "TAKE THE TEST", Ui.GOLD, v -> new TestFlow(this, false).start()), Ui.lp(this, 10));
            col.addView(rc, Ui.lp(this, 16));
        }

        // tip
        LinearLayout tip = Ui.card(this);
        tip.addView(Ui.text(this, "COACH'S TIP", 12, Ui.PURPLE));
        tip.addView(Ui.text(this, Screens.TIPS[(int) (today.toEpochDay() % Screens.TIPS.length)], 14, Ui.TEXT), Ui.lp(this, 4));
        col.addView(tip, Ui.lp(this, 16));
    }

    static String missionBlurb(String type) {
        switch (type) {
            case Plan.GLUTES_HIPS: return "Bridges, side-glute work and squats: the moves that lift and round the hips.";
            case Plan.UPPER_POSTURE: return "Toned arms and an upright, graceful posture, plus a little cardio.";
            case Plan.GLUTES_LEGS: return "Legs and glutes, plus balance. The second shape-building day of the week.";
            case Plan.CORE_BALANCE: return "A strong, cinched waist and steady balance.";
            case Plan.FULL_BODY: return "A bit of everything: the third glute day of the week.";
            case Plan.RECOVERY: return "Gentle stretching that undoes all that sitting, plus a walk for your steps.";
            default: return "";
        }
    }

    // ---------------- TRAIN ----------------

    int chosenDiff = 1;

    void showTrain() {
        LinearLayout col = scrollPage(true);
        LocalDate today = LocalDate.now();
        String type = Plan.typeFor(st, today);
        boolean doneToday = st.workoutOn(today.toString()) != null;
        boolean rest = Plan.REST.equals(type);
        String playType = (doneToday || rest) ? Plan.FULL_BODY : type;
        boolean bonus = doneToday;

        col.addView(Ui.text(this, bonus ? "BONUS ROUND (half XP, half rating change)" : rest ? "REST DAY: OPTIONAL WORKOUT" : "TODAY", 12, Ui.PINK));
        col.addView(Ui.head(this, playType, 30, Ui.TEXT));
        if (Plan.RECOVERY.equals(playType)) {
            col.addView(Ui.text(this, missionBlurb(playType) + "\nNo rating at stake today, just XP and your streak.", 14, Ui.DIM), Ui.lp(this, 6));
            List<State.Item> items = Plan.build(st, today, playType, 1, false);
            listItems(col, items);
            col.addView(Ui.button(this, "START RECOVERY FLOW", Ui.GREEN, v -> startWorkout(playType, 1, false, false)), Ui.lp(this, 18));
            return;
        }
        col.addView(Ui.text(this, "Tap any exercise below to see how it's done. Choose your difficulty. The workout is a rated match: beat it and your rating "
            + "climbs, fall short and it drops.", 14, Ui.DIM), Ui.lp(this, 6));

        LinearLayout diffRow = Ui.hbox(this);
        TextView[] chips = new TextView[4];
        LinearLayout preview = Ui.vbox(this);
        TextView stakes = Ui.text(this, "", 14, Ui.TEXT);
        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    chips[i].setBackground(Ui.round(i == chosenDiff ? Ui.DIFF_COLOR[i] : Ui.CARD, Ui.dp(MainActivity.this, 12)));
                    chips[i].setTextColor(i == chosenDiff ? 0xFF1A0F1F : Ui.DIFF_COLOR[i]);
                }
                List<State.Item> items = Plan.build(st, today, playType, chosenDiff, false);
                double win = 0, lose = 0, oppSum = 0, youSum = 0, wSum = 0;
                double k = st.workouts.size() < 10 ? 40 : 24;
                if (bonus) k /= 2;
                int n = 0;
                boolean[] seen = new boolean[5];
                for (State.Item it : items) {
                    int c = Plan.ex(it.ex).cat;
                    if (c >= 0 && !seen[c]) {
                        seen[c] = true;
                        double opp = Science.opponent(c, st.elo[c], chosenDiff);
                        double e = Science.expected(st.elo[c], opp);
                        win += k * (1 - e);
                        lose += k * (Science.score(0.75) - e);
                        oppSum += opp * Science.CAT_WEIGHT[c];
                        youSum += st.elo[c] * Science.CAT_WEIGHT[c];
                        wSum += Science.CAT_WEIGHT[c];
                        n++;
                    }
                }
                stakes.setText(String.format(Locale.US, "Opponent rating: %s (you: %s)\nFinish everything: about %s per stat\nPeter out at 75%%: about %s per stat\nXP: up to %d   •   about %d min",
                    Ui.fmt(oppSum / Math.max(0.01, wSum)), Ui.fmt(youSum / Math.max(0.01, wSum)),
                    Ui.signed(win / Math.max(1, n)), Ui.signed(lose / Math.max(1, n)),
                    (int) (Science.DIFF_XP[chosenDiff] * (bonus ? 0.5 : 1)), Plan.minutesEstimate(items)));
                preview.removeAllViews();
                listItems(preview, items);
            }
        };
        for (int i = 0; i < 4; i++) {
            final int d = i;
            chips[i] = Ui.head(this, Science.DIFF[i], 14, Ui.DIFF_COLOR[i]);
            chips[i].setGravity(Gravity.CENTER);
            chips[i].setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
            chips[i].setOnClickListener(v -> {
                chosenDiff = d;
                refresh.run();
            });
            diffRow.addView(chips[i], Ui.weight(this, 1, 3));
        }
        col.addView(diffRow, Ui.lp(this, 14));
        LinearLayout sk = Ui.card(this);
        sk.addView(stakes);
        col.addView(sk, Ui.lp(this, 12));
        col.addView(preview, Ui.lp(this, 4));
        refresh.run();
        col.addView(Ui.button(this, "START " + (bonus ? "BONUS" : "WORKOUT"), Ui.PINK,
            v -> startWorkout(playType, chosenDiff, false, bonus)), Ui.lp(this, 18));
        if (!bonus) {
            col.addView(Ui.ghost(this, "LOW ENERGY? TINY MODE: 1 SET, EASY", v -> startWorkout(playType, 0, true, false)), Ui.lp(this, 10));
            col.addView(Ui.text(this, "Tiny Mode still counts for your streak. Showing up on a bad day is the habit that "
                + "matters most.", 12, Ui.DIM), Ui.lp(this, 6));
        }
        if (st.parqFlag) {
            col.addView(Ui.text(this, "You answered YES to a health question. Please get your doctor's OK before HARD or INSANE.", 12, Ui.ORANGE), Ui.lp(this, 10));
        }
    }

    void listItems(LinearLayout col, List<State.Item> items) {
        for (State.Item it : items) {
            Plan.Ex e = Plan.ex(it.ex);
            LinearLayout row = Ui.hbox(this);
            row.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 12)));
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            View dot = new View(this);
            dot.setBackground(Ui.round(e.cat >= 0 ? Ui.CAT_COLOR[e.cat] : Ui.LINE, Ui.dp(this, 4)));
            row.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 8), Ui.dp(this, 30)));
            LinearLayout t = Ui.vbox(this);
            t.setPadding(Ui.dp(this, 12), 0, 0, 0);
            t.addView(Ui.bold(this, e.name, 15, Ui.TEXT));
            t.addView(Ui.text(this, e.cat >= 0 ? Science.CAT_LABEL[e.cat] : "WARM-UP / STRETCH", 11, Ui.DIM));
            row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(Ui.head(this, it.target.length + " × " + it.target[0] + " " + e.unit(), 13, Ui.TEXT));
            TextView info = Ui.head(this, "  ▶", 14, Ui.PINK);
            row.addView(info);
            final String id = e.id;
            row.setOnClickListener(v -> HowTo.show(this, id));
            col.addView(row, Ui.lp(this, 6));
        }
    }

    void startWorkout(String type, int diff, boolean tiny, boolean bonus) {
        LocalDate today = LocalDate.now();
        State.Workout w = new State.Workout();
        w.date = today.toString();
        w.type = bonus ? "BONUS" : type;
        w.diff = diff;
        w.tiny = tiny;
        w.items = Plan.build(st, today, type, diff, tiny);
        new WorkoutFlow(this, w, type).start();
    }

    // ---------------- permissions, files ----------------

    void askPermissions() {
        List<String> want = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29 && checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            want.add(android.Manifest.permission.ACTIVITY_RECOGNITION);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            want.add(android.Manifest.permission.POST_NOTIFICATIONS);
        if (!want.isEmpty()) requestPermissions(want.toArray(new String[0]), REQ_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Steps.schedule(this);
        onResume();
    }

    void exportData() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, "glowup-backup-" + LocalDate.now() + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    void importData() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (req == REQ_EXPORT) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    out.write(Store.export(this, st).getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "Backup saved", Toast.LENGTH_LONG).show();
            } else if (req == REQ_IMPORT) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                }
                st = Store.importJson(this, bo.toString("UTF-8"));
                Toast.makeText(this, "Backup restored", Toast.LENGTH_LONG).show();
                showTab(0);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't do that: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- small dialogs ----------------

    interface NumCb { void got(double v); }

    void askNumber(String title, String hint, double current, NumCb cb) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setHint(hint);
        if (current > 0) e.setText(current == Math.rint(current) ? String.valueOf((long) current) : String.valueOf(current));
        FrameLayout f = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        f.setPadding(p, p / 2, p, 0);
        f.addView(e);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(title).setView(f)
            .setPositiveButton("OK", (d, w) -> {
                try {
                    cb.got(Double.parseDouble(e.getText().toString().trim()));
                } catch (Exception ignored) {
                }
            }).setNegativeButton("CANCEL", null).show();
    }

    void askText(String title, String current, java.util.function.Consumer<String> cb) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        e.setText(current);
        FrameLayout f = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        f.setPadding(p, p / 2, p, 0);
        f.addView(e);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(title).setView(f)
            .setPositiveButton("OK", (d, w) -> cb.accept(e.getText().toString().trim())).setNegativeButton("CANCEL", null).show();
    }

    void askDate(String current, java.util.function.Consumer<LocalDate> cb) {
        LocalDate d;
        try {
            d = LocalDate.parse(current);
        } catch (Exception ex) {
            d = LocalDate.now().minusYears(30);
        }
        new DatePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert,
            (v, y, m, day) -> cb.accept(LocalDate.of(y, m + 1, day)), d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
    }

    void askTime(int h, int m, java.util.function.BiConsumer<Integer, Integer> cb) {
        new TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert, (v, hh, mm) -> cb.accept(hh, mm), h, m, false).show();
    }

    static void removeFromParent(View v) {
        if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
    }
}
