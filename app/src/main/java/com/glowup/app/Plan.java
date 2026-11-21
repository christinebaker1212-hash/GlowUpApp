package com.glowup.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The exercise library and the weekly plan.
 *
 * Every exercise's targets come from her current rating in its category: the rating says what she can do in the
 * category's test (Science.capacity), the exercise's factor says how much harder one rep / second of it is than one
 * of the test's. Harder versions unlock by themselves once she can do 8+ reps (or 20+ seconds) of them. If targets
 * are too hard she misses them, her rating drops and the targets drop with it: the plan calibrates itself.
 */
final class Plan {
    private Plan() {}

    static final int REPS = 0, HOLD = 1;
    static final int MOBILITY = -1; // unscored warm-up / cool-down

    static final class Ex {
        final String id, name, slot, test, how, why;
        final int cat, kind;
        final double factor;
        final boolean side;
        final int fixed; // mobility: fixed target

        Ex(String id, String name, int cat, String slot, String test, double factor, int kind, boolean side,
           String how, String why) {
            this(id, name, cat, slot, test, factor, kind, side, 0, how, why);
        }

        Ex(String id, String name, int cat, String slot, String test, double factor, int kind, boolean side,
           int fixed, String how, String why) {
            this.id = id; this.name = name; this.cat = cat; this.slot = slot; this.test = test;
            this.factor = factor; this.kind = kind; this.side = side; this.fixed = fixed; this.how = how; this.why = why;
        }

        String unit() {
            return (kind == HOLD ? "sec" : "reps") + (side ? " / side" : "");
        }
    }

    private static final int C = Science.CARDIO, U = Science.UPPER, G = Science.CURVES, K = Science.CORE,
        B = Science.BALANCE, M = MOBILITY;
    private static final String PU = Science.T_PUSHUP, BR = Science.T_BRIDGE, CH = Science.T_CHAIR,
        PL = Science.T_PLANK, BA = Science.T_BALANCE, CT = Science.T_CARDIOTIME;

    static final Ex[] ALL = {
        // ---- CURVES: glutes & hips (the shape-builders: trained 3x a week) ----
        new Ex("glute_bridge", "Glute Bridge", G, "BRIDGE", BR, 1.0, REPS, false,
            "On your back, knees bent, feet flat hip-width. Squeeze your glutes and lift your hips until knees, hips and shoulders line up. Pause 1 second at the top, lower slowly.",
            "The safest, best glute builder for beginners. Stronger, rounder glutes lift and shape the hips."),
        new Ex("couch_thrust", "Couch Hip Thrust", G, "BRIDGE", BR, 1.5, REPS, false,
            "Upper back against the edge of the couch, feet flat. Drive through your heels, lift hips level with your shoulders, chin tucked. Squeeze 1 second at the top.",
            "Hip thrusts load the glutes at their shortest, hardest point: the top lift research favours for glute growth."),
        new Ex("sl_bridge", "Single-Leg Glute Bridge", G, "BRIDGE", BR, 2.5, REPS, true,
            "Like a glute bridge with one foot lifted. Keep your hips level, don't let one side drop.",
            "Twice the load per glute with no equipment, and it evens out left / right."),
        new Ex("sl_thrust", "Single-Leg Couch Thrust", G, "BRIDGE", BR, 3.5, REPS, true,
            "Hip thrust on the couch edge, one foot planted, the other leg up. Slow down, pause at the top.",
            "The hardest bodyweight glute move: heavy work for the glutes without a barbell."),
        new Ex("chair_squat", "Chair Squat", G, "SQUAT", CH, 0.7, REPS, false,
            "Stand in front of a chair, feet shoulder-width. Sit back until you lightly touch the seat, then stand up tall. Use your hands at first if you need to.",
            "Builds legs and glutes and makes standing up effortless for life."),
        new Ex("squat", "Bodyweight Squat", G, "SQUAT", CH, 0.8, REPS, false,
            "Feet shoulder-width, toes slightly out. Sit your hips back and down, chest up, knees tracking over toes. Stand and squeeze.",
            "The foundation of lower-body strength and shape."),
        new Ex("reverse_lunge", "Reverse Lunge", G, "SQUAT", CH, 1.8, REPS, true,
            "Step one foot back and lower until both knees bend about 90 degrees. Push through the front heel to come back. Hold a wall if needed.",
            "Stepping back puts more of the work into the glutes and is kind to the knees."),
        new Ex("split_squat", "Couch Split Squat", G, "SQUAT", CH, 3.0, REPS, true,
            "Back foot on the couch, front foot a big step ahead. Lean slightly forward and lower straight down.",
            "One of the best glute and thigh builders there is, with a slight forward lean for glutes."),
        new Ex("sumo_squat", "Sumo Squat", G, "LEGS", CH, 0.85, REPS, false,
            "Wide stance, toes turned out. Lower with your knees pushing out over your toes, then squeeze up.",
            "Works the inner thighs and glutes for a smooth, toned leg line."),
        new Ex("curtsy_lunge", "Curtsy Lunge", G, "LEGS", CH, 2.0, REPS, true,
            "Step one leg diagonally behind the other, like a curtsy, and lower. Return and switch.",
            "Hits the side glute (glute medius) that shapes the upper hip."),
        new Ex("step_up", "Stair Step-Up", G, "LEGS", CH, 1.2, REPS, true,
            "Step up onto a stair with one foot, push through that heel to stand tall, step down slowly.",
            "Glutes plus a little cardio, and stairs get easy forever."),
        new Ex("calf_raise", "Calf Raise", G, "LEGS", CH, 0.4, REPS, false,
            "Hold a wall, rise onto the balls of your feet, pause, lower slowly.",
            "Shapely calves and stronger ankles."),
        new Ex("clamshell", "Clamshell", G, "ABDUCT", BR, 0.5, REPS, true,
            "Lie on your side, knees bent, heels together. Keep your feet touching and open your top knee like a clamshell. Don't roll back.",
            "Targets the glute medius: the muscle that gives the hips their curve."),
        new Ex("side_leg_raise", "Side-Lying Leg Raise", G, "ABDUCT", BR, 0.6, REPS, true,
            "Lie on your side, legs straight. Lift the top leg slightly behind you, toes pointing forward, and lower slowly.",
            "Upper-hip shaping, the side of the glutes."),
        new Ex("fire_hydrant", "Fire Hydrant", G, "ABDUCT", BR, 0.7, REPS, true,
            "On hands and knees, lift one bent knee out to the side to hip height without tipping your body.",
            "Side glutes and hip control."),
        new Ex("standing_abduction", "Standing Side Kick", G, "ABDUCT", BR, 0.5, REPS, true,
            "Hold a wall, stand tall and lift one straight leg out to the side, slowly. Don't lean.",
            "Glute medius plus balance in one."),
        new Ex("donkey_kick", "Donkey Kick", G, "KICK", BR, 0.7, REPS, true,
            "On hands and knees, keep the knee bent and press one foot up toward the ceiling. Squeeze, don't arch your back.",
            "Isolates the main glute for that lifted look."),
        new Ex("frog_pump", "Frog Pump", G, "KICK", BR, 0.6, REPS, false,
            "On your back, soles of the feet together, knees out. Lift your hips with a hard squeeze, lower.",
            "A glute-only bridge: lots of glute work with little thigh."),
        new Ex("glute_kickback", "Standing Glute Kickback", G, "KICK", BR, 0.5, REPS, true,
            "Hold a wall, kick one straight leg back and slightly up, squeezing the glute. Keep your back still.",
            "Glute focus you can do anywhere, even while the kettle boils."),

        // ---- STRENGTH: arms, back & posture (toned, not bulky) ----
        new Ex("wall_pushup", "Wall Push-Up", U, "PUSH", PU, 0.15, REPS, false,
            "Hands on a wall at shoulder height, step back. Body straight, bend your elbows to bring your chest to the wall, push back.",
            "Starts the push-up journey: toned arms and chest support."),
        new Ex("counter_pushup", "Counter Push-Up", U, "PUSH", PU, 0.3, REPS, false,
            "Hands on a kitchen counter or table edge, body straight from head to heels. Lower your chest to the edge, push away.",
            "The next step down toward the floor."),
        new Ex("knee_pushup", "Knee Push-Up", U, "PUSH", PU, 0.5, REPS, false,
            "On the floor on your knees, body straight from knees to head. Lower your chest between your hands, push up.",
            "Real push-up strength with less load."),
        new Ex("pushup", "Push-Up", U, "PUSH", PU, 1.0, REPS, false,
            "Hands under your shoulders, body in one straight line. Lower your chest to a fist's height off the floor, push up.",
            "A full push-up is a milestone: strong arms, chest and core together."),
        new Ex("decline_pushup", "Decline Push-Up", U, "PUSH", PU, 1.35, REPS, false,
            "Push-up with your feet on the couch.",
            "Advanced upper-body strength."),
        new Ex("wall_angel", "Wall Angels", U, "POSTURE", PU, 0.12, REPS, false,
            "Back, head and arms against a wall, elbows bent like a goalpost. Slide your arms up and down without losing contact.",
            "Opens the chest and pulls the shoulders back: posture is the quickest way to look taller and more graceful."),
        new Ex("prone_ytw", "Floor Y-T-W", U, "POSTURE", PU, 0.12, REPS, false,
            "Lie face down, lift your arms into a Y, then a T, then a W, squeezing your shoulder blades. One Y-T-W = 1 rep.",
            "Strengthens the upper back for an upright, elegant posture."),
        new Ex("superman", "Superman Hold", U, "POSTURE", PL, 0.8, HOLD, false,
            "Lie face down, lift your arms, chest and legs a little off the floor and hold. Breathe.",
            "Back strength that protects the spine and keeps you standing tall."),
        new Ex("chair_dip_bent", "Chair Dip (Bent Knees)", U, "DIP", PU, 0.4, REPS, false,
            "Hands on the edge of a sturdy chair behind you, knees bent, feet flat. Bend your elbows to lower, then press up.",
            "Firms the backs of the arms."),
        new Ex("chair_dip", "Chair Dip", U, "DIP", PU, 0.7, REPS, false,
            "Like the bent-knee dip with your legs straighter. Keep your shoulders down, away from your ears.",
            "More work for the triceps."),

        // ---- CORE: waist & trunk ----
        new Ex("wall_plank", "Wall Plank", K, "PLANK", PL, 0.2, HOLD, false,
            "Forearms on a wall, step back until your body leans in a straight line. Squeeze your glutes and brace your stomach.",
            "The first step to a strong, flat-feeling core."),
        new Ex("counter_plank", "Counter Plank", K, "PLANK", PL, 0.35, HOLD, false,
            "Forearms on a counter or table, body straight from head to heels. Brace and breathe.",
            "Closer to the floor, more core."),
        new Ex("knee_plank", "Knee Plank", K, "PLANK", PL, 0.55, HOLD, false,
            "Forearms and knees on the floor, body straight from knees to head. Don't let your hips sag.",
            "A strong trunk: the base of every other move and a healthy back."),
        new Ex("plank", "Forearm Plank", K, "PLANK", PL, 1.0, HOLD, false,
            "Forearms and toes, one straight line. Squeeze glutes, pull your belly button in, breathe.",
            "Deep core strength for a firm, cinched waist."),
        new Ex("plank_taps", "Plank Shoulder Taps", K, "PLANK", PL, 1.4, HOLD, false,
            "High plank on your hands, feet wide. Tap each shoulder in turn without rocking your hips.",
            "Anti-rotation core work that trains the obliques without bulking the waist."),
        new Ex("knee_side_plank", "Knee Side Plank", K, "SIDE", PL, 0.8, HOLD, true,
            "On your side, elbow under shoulder, knees bent. Lift your hips into a straight line and hold.",
            "Side core and the side glute, together."),
        new Ex("side_plank", "Side Plank", K, "SIDE", PL, 1.4, HOLD, true,
            "On your side, elbow under shoulder, legs straight. Lift your hips and hold.",
            "Strong obliques and hips."),
        new Ex("dead_bug", "Dead Bug", K, "CORE_DYN", PL, 4.0, REPS, true,
            "On your back, arms up, knees over hips. Lower the opposite arm and leg slowly while your lower back stays pressed down.",
            "Trains the deep core (transverse abdominis) that holds the waist in like a corset."),
        new Ex("bird_dog", "Bird Dog", K, "CORE_DYN", PL, 3.0, REPS, true,
            "On hands and knees, reach one arm forward and the opposite leg back. Pause, return, switch.",
            "Core and glutes together, and kind to the back."),
        new Ex("reverse_crunch", "Reverse Crunch", K, "CORE_DYN", PL, 3.0, REPS, false,
            "On your back, knees bent up. Curl your hips off the floor toward your chest, lower slowly.",
            "Lower-stomach strength."),
        new Ex("vacuum", "Stomach Vacuum", K, "CORE_DYN", PL, 1.5, HOLD, false,
            "Standing or on all fours, breathe all the way out and pull your belly button in toward your spine. Hold, then breathe normally.",
            "Trains the deep corset muscle; good for control and posture (it won't burn fat on its own)."),

        // ---- BALANCE & mobility ----
        new Ex("tandem_stance", "Tandem Stance", B, "BALANCE", BA, 0.4, HOLD, true,
            "Stand with one foot directly in front of the other, heel to toe, near a wall. Hold.",
            "Steadier on your feet, every day. It starts here."),
        new Ex("single_leg_stand", "Single-Leg Stand", B, "BALANCE", BA, 1.0, HOLD, true,
            "Stand on one leg near a wall, the other foot lifted. Stand tall, eyes ahead.",
            "Balance, ankle strength and hip control together. Aim for 30 seconds a side."),
        new Ex("sl_stand_closed", "Single-Leg Stand, Eyes Closed", B, "BALANCE", BA, 3.0, HOLD, true,
            "Next to a wall for safety: stand on one leg and close your eyes.",
            "Elite balance."),

        // ---- STAMINA ----
        new Ex("march", "Marching in Place", C, "CARDIO", CT, 1.0, HOLD, false,
            "March in place, lifting your knees and swinging your arms. Breathe, you should be able to talk.",
            "Heart and lungs: more energy, better sleep and a better mood."),
        new Ex("dance", "Dance Break", C, "CARDIO", CT, 1.0, HOLD, false,
            "Put on a song you love and dance. Any style. Keep moving the whole time.",
            "Cardio that's actually fun, and it works your hips."),
        new Ex("step_jacks", "Step Jacks", C, "CARDIO", CT, 1.3, HOLD, false,
            "Step one foot out to the side while raising your arms overhead, step back, switch sides. No jumping.",
            "Low-impact cardio that's easy on the joints."),
        new Ex("stair_climb", "Stair Laps", C, "CARDIO", CT, 1.4, HOLD, false,
            "Walk up and down a flight of stairs, holding the rail. Steady pace.",
            "Stair climbing builds glutes and stamina together."),
        new Ex("high_knees", "High Knees", C, "CARDIO", CT, 2.0, HOLD, false,
            "Run in place, bringing your knees up high. Light on your feet.",
            "High-intensity cardio for when you're ready."),

        // ---- Mobility (warm-up / cool-down; unscored) ----
        new Ex("cat_cow", "Cat-Cow", M, "", "", 1, REPS, false, 8,
            "On hands and knees, round your back up like a cat, then let it sink and look up. Slow, with your breath.",
            "Wakes up the spine."),
        new Ex("hip_circles", "Hip Circles", M, "", "", 1, REPS, false, 10,
            "Hands on hips, draw big slow circles with your hips, then the other way.",
            "Loosens the hips before glute work."),
        new Ex("arm_circles", "Arm Circles", M, "", "", 1, REPS, false, 10,
            "Arms out to the sides, small circles growing bigger, forward then back.",
            "Warms up the shoulders."),
        new Ex("lunge_reach", "Lunge & Reach", M, "", "", 1, REPS, true, 4,
            "Step into a lunge, reach both arms overhead and lean gently back. Hold a breath, switch.",
            "Opens the hip flexors that sitting tightens."),
        new Ex("hip_flexor", "Kneeling Hip Flexor Stretch", M, "", "", 1, HOLD, true, 30,
            "Kneel on one knee, squeeze that side's glute and shift forward until you feel a stretch at the front of the hip.",
            "Undoes hours of sitting: better posture and a stronger glute squeeze."),
        new Ex("figure4", "Figure-4 Stretch", M, "", "", 1, HOLD, true, 30,
            "On your back, cross one ankle over the other knee, pull the bottom leg toward you.",
            "Releases the glutes and hips."),
        new Ex("childs_pose", "Child's Pose", M, "", "", 1, HOLD, false, 40,
            "Kneel, sit back on your heels, reach your arms forward and rest your forehead down. Breathe slowly.",
            "Calms you down and stretches the back."),
        new Ex("butterfly", "Butterfly Stretch", M, "", "", 1, HOLD, false, 30,
            "Sit, soles of the feet together, let your knees fall open. Sit tall.",
            "Inner thigh and hip mobility."),
    };

    static Ex ex(String id) {
        for (Ex e : ALL) if (e.id.equals(id)) return e;
        return ALL[0];
    }

    // ---- The week ----
    static final String GLUTES_HIPS = "GLUTES & HIPS", UPPER_POSTURE = "ARMS & POSTURE", RECOVERY = "RECOVERY FLOW",
        GLUTES_LEGS = "GLUTES & LEGS", CORE_BALANCE = "CORE & BALANCE", FULL_BODY = "FULL BODY", REST = "REST DAY";
    static final String[] WEEK = {GLUTES_HIPS, UPPER_POSTURE, RECOVERY, GLUTES_LEGS, CORE_BALANCE, FULL_BODY, REST};

    static String typeFor(State s, LocalDate d) {
        LocalDate start;
        try {
            start = LocalDate.parse(s.start);
        } catch (Exception e) {
            start = d;
        }
        long n = ChronoUnit.DAYS.between(start, d);
        return WEEK[(int) (((n % 7) + 7) % 7)];
    }

    /** {warm-up..., slots..., cool-down...}: slots in capitals, mobility moves by id. */
    static String[] template(String type) {
        switch (type) {
            case GLUTES_HIPS: return new String[]{"hip_circles", "cat_cow", "BRIDGE", "ABDUCT", "SQUAT", "KICK", "PLANK", "figure4"};
            case UPPER_POSTURE: return new String[]{"arm_circles", "cat_cow", "PUSH", "POSTURE", "DIP", "CORE_DYN", "CARDIO", "childs_pose"};
            case GLUTES_LEGS: return new String[]{"hip_circles", "lunge_reach", "SQUAT", "BRIDGE", "LEGS", "ABDUCT", "BALANCE", "hip_flexor"};
            case CORE_BALANCE: return new String[]{"cat_cow", "arm_circles", "PLANK", "CORE_DYN", "SIDE", "BALANCE", "CARDIO", "childs_pose"};
            case FULL_BODY: return new String[]{"hip_circles", "arm_circles", "SQUAT", "PUSH", "BRIDGE", "CORE_DYN", "CARDIO", "figure4"};
            case RECOVERY: return new String[]{"cat_cow", "hip_circles", "lunge_reach", "hip_flexor", "figure4", "butterfly", "childs_pose"};
            default: return new String[0];
        }
    }

    private static final String[] PROGRESSIONS = {"PUSH", "DIP", "BRIDGE", "SQUAT", "PLANK", "SIDE", "BALANCE"};

    /** How many reps / seconds of this exercise she could do in one go right now. */
    static double maxFor(Ex e, State s, double age) {
        if (e.cat == MOBILITY) return e.fixed;
        return Science.capacity(e.test, s.elo[e.cat], age) / e.factor;
    }

    static Ex pick(String slot, State s, double age, LocalDate d) {
        List<Ex> list = new ArrayList<>();
        for (Ex e : ALL) if (slot.equals(e.slot)) list.add(e);
        boolean progression = false;
        for (String p : PROGRESSIONS) if (p.equals(slot)) progression = true;
        if (progression) {
            Ex best = list.get(0);
            for (Ex e : list) {
                double need = e.kind == HOLD ? (e.cat == Science.CARDIO ? 60 : 20) : 8;
                if (maxFor(e, s, age) >= need) best = e;
            }
            return best;
        }
        List<Ex> ok = new ArrayList<>();
        for (Ex e : list) {
            double need = e.kind == HOLD ? (e.cat == Science.CARDIO ? 60 : 15) : 5;
            if (maxFor(e, s, age) >= need) ok.add(e);
        }
        if (ok.isEmpty()) ok.add(list.get(0));
        int h = Math.abs(slot.hashCode() % 7);
        return ok.get((int) ((d.toEpochDay() / 7 * 3 + d.toEpochDay() + h) % ok.size()));
    }

    static int setsFor(double rating, boolean tiny) {
        if (tiny) return 1;
        return rating < 700 ? 2 : rating < 1500 ? 3 : 4;
    }

    static int target(Ex e, State s, double age, int diff, boolean tiny) {
        if (e.cat == MOBILITY) return e.fixed;
        double pct = Science.DIFF_PCT[tiny ? 0 : diff];
        double t = maxFor(e, s, age) * pct;
        if (e.kind == HOLD) {
            int min = e.cat == Science.CARDIO ? 30 : 10;
            int v = (int) Math.round(t / 5.0) * 5;
            return Math.max(min, Math.min(e.cat == Science.CARDIO ? 900 : 300, v));
        }
        return Math.max(3, Math.min(60, (int) Math.round(t)));
    }

    /** Builds a day's session. */
    static List<State.Item> build(State s, LocalDate d, String type, int diff, boolean tiny) {
        double age = Science.age(s.birth, d);
        List<State.Item> items = new ArrayList<>();
        for (String part : template(type)) {
            Ex e = Character.isUpperCase(part.charAt(0)) ? pick(part, s, age, d) : ex(part);
            State.Item it = new State.Item();
            it.ex = e.id;
            int sets = e.cat == MOBILITY ? 1 : setsFor(s.elo[e.cat], tiny);
            if (e.cat == Science.CARDIO) sets = Math.min(sets, 2);
            it.target = new int[sets];
            it.done = new int[sets];
            for (int i = 0; i < sets; i++) {
                it.target[i] = target(e, s, age, diff, tiny);
                it.done[i] = -1;
            }
            items.add(it);
        }
        return items;
    }

    static int minutesEstimate(List<State.Item> items) {
        int sec = 0;
        for (State.Item it : items) {
            Ex e = ex(it.ex);
            for (int t : it.target) {
                int one = e.kind == HOLD ? t : t * 3;
                sec += one * (e.side ? 2 : 1) + 40; // + rest
            }
        }
        return Math.max(5, Math.round(sec / 60f));
    }
}
