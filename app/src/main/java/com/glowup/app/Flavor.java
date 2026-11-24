package com.glowup.app;

/**
 * What differs between the two apps built from this code: GLOW UP (hers) and TRAINING ARC (his: a fighting-game /
 * anime training arc toward a lean, strong "god form" physique). Everything else is shared.
 */
final class Flavor {
    private Flavor() {}

    static final boolean MALE = BuildConfig.MALE;
    static final String APP = MALE ? "TRAINING ARC" : "GLOW UP";
    static final String TAGLINE = MALE ? "every legend has a training arc" : "level up your body, one day at a time";
    static final String PEERS = MALE ? "men" : "women";

    // the five stats (same order everywhere: cardio, upper, lower, core, balance)
    static final String[] CAT_LABEL = MALE
        ? new String[]{"STAMINA", "STRENGTH", "POWER", "CORE", "CONTROL"}
        : new String[]{"STAMINA", "STRENGTH", "CURVES", "CORE", "BALANCE"};
    static final String[] CAT_SUB = MALE
        ? new String[]{"heart, lungs & fat burn", "chest, shoulders, back & arms", "legs & glutes", "abs & trunk", "balance & mobility"}
        : new String[]{"heart & lungs", "arms, back & posture", "glutes, hips & legs", "waist & trunk", "balance & mobility"};
    static final double[] CAT_WEIGHT = MALE
        ? new double[]{0.25, 0.25, 0.20, 0.15, 0.15}
        : new double[]{0.25, 0.15, 0.25, 0.20, 0.15};

    static final String RATING = MALE ? "POWER LEVEL" : "FITNESS RATING";
    static final String[] TIERS = MALE
        ? new String[]{"ROOKIE", "BRAWLER", "FIGHTER", "WARRIOR", "CHAMPION", "MASTER", "LEGEND", "GOD TIER"}
        : new String[]{"COUCH POTATO", "OFF THE COUCH", "WEEKEND MOVER", "ACTIVE", "ATHLETIC", "COMPETITOR", "ELITE", "OLYMPIAN"};
    static final String TOP_MEANS = MALE ? "Olympic-level results: true god tier" : "Olympic-level results";

    // ---- workouts ----
    static String passTitle(double completion) {
        if (!MALE) return completion >= 0.9 ? "WORKOUT PASSED!" : completion >= 0.5 ? "WORKOUT COMPLETE" : "TOUGH ONE";
        if (completion >= 0.999) return "PERFECT!";
        if (completion >= 0.9) return "K.O.!";
        if (completion >= 0.5) return "ROUND WON";
        return "TOUGH ROUND";
    }

    static final String TOUGH = MALE
        ? "You showed up and fought. The plan adjusts to you, so tomorrow's targets fit better."
        : "Showing up is the win. The plan adjusts to you, so tomorrow's targets fit better.";

    static final String WELCOME = MALE
        ? "Every workout is a fight against a rated opponent: the workout itself. Win and your POWER LEVEL climbs, from "
            + "ROOKIE toward GOD TIER, a rank that takes Olympic-level numbers to reach. Every set earns XP.\n\n"
            + "The arc: burn the fat that's covering the muscle you already have, build a wide chest, shoulders and back, "
            + "a hard core and real conditioning. Your body fat is tracked toward GOD FORM, lean enough to see the work.\n\n"
            + "First: a 15-minute fitness test to find your starting power level."
        : "Every workout is a rated match. Beat it and your FITNESS RATING climbs from COUCH POTATO "
            + "toward OLYMPIAN. Every set earns XP toward your next level.\n\n"
            + "The plan builds curves: glutes and hips three days a week, a strong cinched waist, toned arms, graceful "
            + "posture, balance and stamina, starting from wherever you are today, even if that's the couch.\n\n"
            + "First: a 15-minute fitness test to find your starting point.";

    static final String[] TIPS_M = {
        "Fat loss happens in the kitchen; muscle is built in training. The arc needs both, but it doesn't need perfect.",
        "Protein keeps your muscle while the fat comes off: about 1 gram per pound of your goal weight a day. Hit it and cravings drop too.",
        "Pizza night isn't the problem. The other 20 meals of the week are what decide the arc.",
        "Have the treat after a meal, not instead of one. Same donut, a fraction of the damage.",
        "Steps burn more fat than you'd think: 10,000 a day is roughly an extra 400 calories for a big guy.",
        "Sleep 7 to 9 hours. Short sleep raises hunger hormones and makes junk food hit harder.",
        "The scale bounces around with water. Watch the 7-day average and your waist, not one morning's number.",
        "Push-ups every day is a great habit. Rows and pull-ups build the back that makes the V-shape.",
        "Lose about 1% of your body weight a week and you keep the muscle. Faster than that costs strength.",
        "Liquid calories don't fill you up. Swapping soda for zero-sugar or water is the easiest win there is.",
        "Abs are made in training and revealed by fat loss. Around 12% body fat they start to show.",
        "Slow the lowering part of each rep. More muscle per rep, and kinder to your joints at a heavier weight.",
        "Missing a day is fine. Missing two starts a new habit. TINY MODE exists for the bad days.",
        "Hit a plateau? Retest. The plan recalibrates to the fighter you are now.",
        "Eat slowly. It takes about 20 minutes for your stomach to tell your brain it's full.",
        "Every legend's arc has a slow part in the middle. Keep the streak; the change shows up all at once.",
        "Water before meals helps you feel full and keeps your training sharp.",
        "Big muscles burn more energy even at rest. Keep lifting while you cut and you keep the engine.",
        "A harder version of each move unlocks by itself once you can do 8 clean reps.",
        "Consistency beats intensity. A NORMAL workout every planned day beats one INSANE workout a week.",
    };

    // ---- the FUEL missions (his): simple daily food habits, no calorie counting ----
    static final String[][] FUEL = {
        {"PROTEIN AT EVERY MEAL", "A palm or two of meat, fish, eggs, Greek yogurt or a shake."},
        {"FRUIT OR VEG TWICE", "With two of today's meals. Fills you up for few calories."},
        {"WATER, NOT CALORIES", "No sugary drinks today. Water, zero-sugar or black coffee."},
        {"TREATS AFTER A MEAL", "Chocolate, pizza, a donut: fine, after real food, not instead of it."},
        {"KITCHEN CLOSED 2H BEFORE BED", "Late-night snacking is where most of the extra goes."},
    };
}
