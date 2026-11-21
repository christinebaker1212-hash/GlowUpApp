package com.glowup.app;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** An exercise's how-to page: the animated demonstration, numbered steps, what it works, mistakes to avoid. */
final class HowTo {
    private HowTo() {}

    /** Common mistakes, by the kind of exercise. */
    static String mistakes(Plan.Ex e) {
        switch (e.slot) {
            case "BRIDGE": return "Don't arch your lower back to get higher: the lift comes from squeezing your glutes.\nPush through your heels, not your toes.";
            case "SQUAT": return "Keep your heels down and your chest up.\nKnees follow your toes: don't let them cave in.";
            case "LEGS": return "Knees track over your toes.\nStand all the way up between reps.";
            case "ABDUCT": return "Go slowly and don't roll your hips backward to cheat the leg higher.\nSmall and controlled beats big and floppy.";
            case "KICK": return "Don't arch your back: only the leg moves.\nSqueeze the glute at the top.";
            case "PUSH": return "Don't let your hips sag or stick up: one straight line.\nElbows angle back toward your hips, not straight out to the sides.";
            case "POSTURE": return "Keep your shoulders down, away from your ears.\nMove slowly and squeeze between your shoulder blades.";
            case "DIP": return "Keep your shoulders down and your elbows pointing straight back.\nStop if you feel it at the front of the shoulder.";
            case "PLANK": case "SIDE": return "Don't hold your breath.\nDon't let your hips sag or pike up. Squeeze your glutes to stay straight.";
            case "CORE_DYN": return "Keep your lower back still (pressed down on your back, flat on all fours).\nSlow beats fast.";
            case "BALANCE": return "Stay near a wall and keep a soft knee.\nLook at one spot ahead of you.";
            case "CARDIO": return "Go at a pace where you can still talk.\nLand softly.";
            default: return "Breathe slowly and never bounce into a stretch.\nA gentle pull is right; pain is not.";
        }
    }

    static String works(Demo.Anim a) {
        List<String> w = new ArrayList<>();
        if ((a.parts & Demo.GLUTE) != 0) w.add("glutes");
        if ((a.parts & Demo.HIPSIDE) != 0) w.add("side of the hips (glute medius)");
        if ((a.parts & Demo.INNER) != 0) w.add("inner thighs");
        if ((a.parts & Demo.THIGH) != 0) w.add("thighs");
        if ((a.parts & Demo.SHIN) != 0) w.add("calves and ankles");
        if ((a.parts & Demo.CORE) != 0) w.add("core");
        if ((a.parts & Demo.CHEST) != 0) w.add("chest");
        if ((a.parts & Demo.ARM) != 0) w.add("arms");
        if ((a.parts & Demo.SHOULDER) != 0) w.add("shoulders");
        if ((a.parts & Demo.BACK) != 0) w.add("upper back");
        if ((a.parts & Demo.HEART) != 0) w.add("heart and lungs");
        if (w.isEmpty()) return "";
        String s = String.join(", ", w);
        return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
    }

    /** Easier and harder versions (progressions only). */
    static Plan.Ex[] neighbours(Plan.Ex e) {
        List<Plan.Ex> list = new ArrayList<>();
        for (Plan.Ex x : Plan.ALL) if (!e.slot.isEmpty() && e.slot.equals(x.slot)) list.add(x);
        boolean progression = false;
        for (String p : new String[]{"PUSH", "DIP", "BRIDGE", "SQUAT", "PLANK", "SIDE", "BALANCE"}) progression |= p.equals(e.slot);
        if (!progression) return new Plan.Ex[2];
        int i = list.indexOf(e);
        return new Plan.Ex[]{i > 0 ? list.get(i - 1) : null, i >= 0 && i < list.size() - 1 ? list.get(i + 1) : null};
    }

    /** A demonstration in a rounded card, tap to open the full page. */
    static FrameLayout card(MainActivity a, String id, int heightDp, boolean tapToOpen) {
        FrameLayout f = new FrameLayout(a);
        f.setBackground(Ui.round(0xFF1A1324, Ui.dp(a, 16)));
        f.addView(new DemoView(a, Demo.get(id), heightDp), new FrameLayout.LayoutParams(-1, Ui.dp(a, heightDp)));
        if (tapToOpen) {
            TextView hint = Ui.head(a, "TAP FOR HOW-TO", 11, Ui.DIM);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            lp.setMargins(0, Ui.dp(a, 8), Ui.dp(a, 10), 0);
            f.addView(hint, lp);
            f.setOnClickListener(v -> show(a, id));
        }
        return f;
    }

    static void show(MainActivity a, String id) {
        Dialog d = new Dialog(a, android.R.style.Theme_DeviceDefault_NoActionBar);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout col = Ui.vbox(a);
        int p = Ui.dp(a, 16);
        col.setPadding(p, p, p, p * 2);
        sv.addView(col);
        fill(a, d, col, id);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setStatusBarColor(Ui.BG);
            w.setNavigationBarColor(Ui.BG);
        }
        sv.setOnApplyWindowInsetsListener((v, ins) -> {
            v.setPadding(0, ins.getSystemWindowInsetTop(), 0, ins.getSystemWindowInsetBottom());
            return ins;
        });
        d.show();
    }

    static void fill(MainActivity a, Dialog d, LinearLayout col, String id) {
        col.removeAllViews();
        Plan.Ex e = Plan.ex(id);
        boolean known = e.id.equals(id);
        Demo.Anim anim = Demo.get(id);

        LinearLayout top = Ui.hbox(a);
        LinearLayout tl = Ui.vbox(a);
        tl.addView(Ui.text(a, !known ? "FITNESS TEST" : e.cat >= 0 ? Science.CAT_LABEL[e.cat] : "WARM-UP / STRETCH", 12,
            known && e.cat >= 0 ? Ui.CAT_COLOR[e.cat] : Ui.DIM));
        tl.addView(Ui.head(a, (known ? e.name : id.replace('_', ' ')).toUpperCase(Locale.US), 26, Ui.TEXT));
        top.addView(tl, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = Ui.head(a, "✕", 24, Ui.TEXT);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(Ui.CARD2, Ui.dp(a, 22)));
        close.setOnClickListener(v -> d.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 44)));
        col.addView(top);

        FrameLayout box = new FrameLayout(a);
        box.setBackground(Ui.round(0xFF1A1324, Ui.dp(a, 18)));
        DemoView dv = new DemoView(a, anim, 320);
        box.addView(dv, new FrameLayout.LayoutParams(-1, Ui.dp(a, 320)));
        col.addView(box, Ui.lp(a, 12));
        TextView speed = Ui.ghost(a, "SLOW MOTION", null);
        speed.setOnClickListener(v -> {
            dv.speed = dv.speed == 1 ? 0.45 : 1;
            speed.setText(dv.speed == 1 ? "SLOW MOTION" : "NORMAL SPEED");
        });
        col.addView(speed, Ui.lp(a, 8));
        String w = works(anim);
        if (!w.isEmpty()) {
            TextView wt = Ui.text(a, "● WORKS: " + w, 13, Ui.PINK);
            col.addView(wt, Ui.lp(a, 10));
        }

        if (known) {
            LinearLayout steps = Ui.card(a);
            steps.addView(Ui.text(a, "HOW TO DO IT", 12, Ui.DIM));
            String[] parts = e.how.split("(?<=\\.)\\s+");
            for (int i = 0; i < parts.length; i++) {
                LinearLayout r = Ui.hbox(a);
                r.setGravity(Gravity.TOP);
                TextView n = Ui.head(a, String.valueOf(i + 1), 15, 0xFF1A0F1F);
                n.setGravity(Gravity.CENTER);
                n.setBackground(Ui.round(Ui.GOLD, Ui.dp(a, 12)));
                r.addView(n, new LinearLayout.LayoutParams(Ui.dp(a, 24), Ui.dp(a, 24)));
                TextView t = Ui.text(a, parts[i].trim(), 15, Ui.TEXT);
                t.setPadding(Ui.dp(a, 10), 0, 0, 0);
                r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
                steps.addView(r, Ui.lp(a, 10));
            }
            col.addView(steps, Ui.lp(a, 12));

            LinearLayout mc = Ui.card(a);
            mc.addView(Ui.text(a, "AVOID", 12, Ui.ORANGE));
            for (String m : mistakes(e).split("\n")) mc.addView(Ui.text(a, "✗  " + m, 14, Ui.TEXT), Ui.lp(a, 6));
            col.addView(mc, Ui.lp(a, 12));

            LinearLayout why = Ui.card(a);
            why.addView(Ui.text(a, "WHY IT HELPS", 12, Ui.PINK));
            why.addView(Ui.text(a, e.why, 14, Ui.TEXT), Ui.lp(a, 4));
            col.addView(why, Ui.lp(a, 12));

            Plan.Ex[] nb = neighbours(e);
            if (nb[0] != null || nb[1] != null) {
                LinearLayout row = Ui.hbox(a);
                if (nb[0] != null) row.addView(Ui.ghost(a, "EASIER:\n" + nb[0].name.toUpperCase(Locale.US), v -> fill(a, d, col, nb[0].id)), Ui.weight(a, 1, 3));
                if (nb[1] != null) row.addView(Ui.ghost(a, "HARDER:\n" + nb[1].name.toUpperCase(Locale.US), v -> fill(a, d, col, nb[1].id)), Ui.weight(a, 1, 3));
                col.addView(row, Ui.lp(a, 12));
                col.addView(Ui.text(a, "The app moves you to the harder version by itself once you can do 8 clean reps.", 12, Ui.DIM), Ui.lp(a, 6));
            }
        }
        col.addView(Ui.button(a, "GOT IT", Ui.PINK, v -> d.dismiss()), Ui.lp(a, 18));
    }

    /** ME > EXERCISE LIBRARY: every move, by stat, with a moving picture of each. */
    static void library(MainActivity a) {
        Dialog d = new Dialog(a, android.R.style.Theme_DeviceDefault_NoActionBar);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout col = Ui.vbox(a);
        int p = Ui.dp(a, 16);
        col.setPadding(p, p, p, p * 2);
        sv.addView(col);
        LinearLayout top = Ui.hbox(a);
        top.addView(Ui.head(a, "EXERCISE LIBRARY", 26, Ui.TEXT), new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = Ui.head(a, "✕", 24, Ui.TEXT);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(Ui.CARD2, Ui.dp(a, 22)));
        close.setOnClickListener(v -> d.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 44)));
        col.addView(top);
        col.addView(Ui.text(a, "Tap any move to see how it's done.", 14, Ui.DIM), Ui.lp(a, 4));
        int[] order = {Science.CURVES, Science.UPPER, Science.CORE, Science.BALANCE, Science.CARDIO, Plan.MOBILITY};
        for (int cat : order) {
            col.addView(Ui.head(a, cat >= 0 ? Science.CAT_LABEL[cat] : "WARM-UPS & STRETCHES", 16, cat >= 0 ? Ui.CAT_COLOR[cat] : Ui.DIM), Ui.lp(a, 20));
            LinearLayout grid = null;
            int n = 0;
            for (Plan.Ex e : Plan.ALL) {
                if (e.cat != cat) continue;
                if (n % 2 == 0) {
                    grid = Ui.hbox(a);
                    grid.setGravity(Gravity.TOP);
                    col.addView(grid, Ui.lp(a, 8));
                }
                LinearLayout cell = Ui.vbox(a);
                cell.setBackground(Ui.round(Ui.CARD, Ui.dp(a, 14)));
                cell.setPadding(0, 0, 0, Ui.dp(a, 8));
                DemoView dv = new DemoView(a, Demo.get(e.id), 120);
                dv.showLabel = false;
                cell.addView(dv, new LinearLayout.LayoutParams(-1, Ui.dp(a, 120)));
                TextView nm = Ui.bold(a, e.name, 13, Ui.TEXT);
                nm.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 6), 0);
                cell.addView(nm);
                final String id = e.id;
                cell.setOnClickListener(v -> show(a, id));
                grid.addView(cell, Ui.weight(a, 1, 4));
                n++;
            }
            if (n % 2 == 1) grid.addView(new View(a), Ui.weight(a, 1, 4));
        }
        d.setContentView(sv);
        if (d.getWindow() != null) {
            d.getWindow().setStatusBarColor(Ui.BG);
            d.getWindow().setNavigationBarColor(Ui.BG);
        }
        sv.setOnApplyWindowInsetsListener((v, ins) -> {
            v.setPadding(0, ins.getSystemWindowInsetTop(), 0, ins.getSystemWindowInsetBottom());
            return ins;
        });
        d.show();
    }
}
