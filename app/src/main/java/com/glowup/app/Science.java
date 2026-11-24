package com.glowup.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The numbers behind the game.
 *
 * Every test result becomes a rating through anchor points taken from population norms for women aged about 30:
 * the 1st, 10th, 30th, 50th, 75th, 90th and 99th percentiles land on 100, 400, 800, 1200, 1600, 2000 and 2400,
 * national-elite athletes on 2600, Olympic athletes on 2800, the best in the world on 3200. So a rating says where
 * she stands among women her age, and OLYMPIAN means Olympic-level results, not a title. Results are age-graded
 * first (as masters athletics does), so the same rating means the same thing at 32 or at 62.
 */
final class Science {
    private Science() {}

    // Categories: the five stat bars
    static final int CARDIO = 0, UPPER = 1, CURVES = 2, CORE = 3, BALANCE = 4;
    static final String[] CAT_LABEL = Flavor.CAT_LABEL;
    static final String[] CAT_SUB = Flavor.CAT_SUB;
    static final double[] CAT_WEIGHT = Flavor.CAT_WEIGHT;

    // Tests
    static final String T_SITRISE = "sitrise", T_BALANCE = "balance", T_CHAIR = "chair", T_PUSHUP = "pushup",
        T_BRIDGE = "bridge", T_PLANK = "plank", T_MARCH = "march", T_COOPER = "cooper", T_CARDIOTIME = "cardiotime";

    /**
     * Anchor points {result, rating} for a woman of about 30: percentiles 1 / 10 / 30 / 50 / 75 / 90 / 99, then
     * national elite, Olympic, world best. Sources: plank from college-women norms (Chase 2014, Strand 2014) scaled
     * down for a general population; push-ups, chair stand and step test from ACSM / Senior Fitness Test norms
     * extrapolated to age 30; one-leg stand from Springer 2007; 12-minute run from the Cooper Institute norms and
     * elite race paces. Glute bridges have no published norms: those anchors are estimates.
     */
    static double[][] anchors(String test) {
        return Flavor.MALE ? anchorsM(test) : anchorsF(test);
    }

    /**
     * The same scale for men of about 30: push-ups and plank from ACSM and college norms (Strand 2014: men hold
     * planks about 50% longer), chair stand and step test a little above women's, the 12-minute run from the Cooper
     * Institute's men's table and elite race paces. Balance and sit & rise differ little between the sexes.
     */
    static double[][] anchorsM(String test) {
        switch (test) {
            case T_PUSHUP:
                return new double[][]{{0, 30}, {2, 100}, {8, 400}, {15, 800}, {21, 1200}, {30, 1600}, {40, 2000},
                    {60, 2400}, {75, 2600}, {90, 2800}, {130, 3200}};
            case T_BRIDGE:
                return new double[][]{{0, 30}, {6, 100}, {15, 400}, {25, 800}, {35, 1200}, {50, 1600}, {70, 2000},
                    {110, 2400}, {150, 2600}, {200, 2800}, {270, 3200}};
            case T_CHAIR:
                return new double[][]{{0, 30}, {9, 100}, {14, 400}, {17, 800}, {20, 1200}, {24, 1600}, {28, 2000},
                    {35, 2400}, {40, 2600}, {45, 2800}, {50, 3200}};
            case T_PLANK:
                return new double[][]{{0, 30}, {10, 100}, {30, 400}, {60, 800}, {90, 1200}, {130, 1600}, {180, 2000},
                    {300, 2400}, {420, 2600}, {540, 2800}, {900, 3200}};
            case T_MARCH:
                return new double[][]{{0, 30}, {65, 100}, {95, 400}, {110, 800}, {120, 1200}, {133, 1600},
                    {145, 2000}, {165, 2400}};
            case T_COOPER:
                return new double[][]{{0, 30}, {1200, 100}, {1700, 400}, {2000, 800}, {2200, 1200}, {2500, 1600},
                    {2800, 2000}, {3200, 2400}, {3700, 2600}, {4100, 2800}, {4700, 3200}};
            default:
                return anchorsF(test);
        }
    }

    static double[][] anchorsF(String test) {
        switch (test) {
            case T_PUSHUP: // full push-up equivalents (wall / counter / knee push-ups count for part of one)
                return new double[][]{{0, 30}, {0.5, 100}, {1.5, 400}, {3.5, 800}, {6, 1200}, {12, 1600}, {20, 2000},
                    {35, 2400}, {50, 2600}, {70, 2800}, {100, 3200}};
            case T_BRIDGE: // glute bridges in a row (estimated)
                return new double[][]{{0, 30}, {5, 100}, {12, 400}, {22, 800}, {30, 1200}, {45, 1600}, {65, 2000},
                    {100, 2400}, {140, 2600}, {180, 2800}, {250, 3200}};
            case T_CHAIR: // 30-second chair stand
                return new double[][]{{0, 30}, {8, 100}, {12, 400}, {16, 800}, {19, 1200}, {23, 1600}, {27, 2000},
                    {33, 2400}, {38, 2600}, {42, 2800}, {48, 3200}};
            case T_PLANK: // forearm plank seconds (easier planks count for part)
                return new double[][]{{0, 30}, {5, 100}, {20, 400}, {45, 800}, {70, 1200}, {105, 1600}, {150, 2000},
                    {240, 2400}, {360, 2600}, {480, 2800}, {720, 3200}};
            case T_MARCH: // 2-minute step test: right-knee lifts (stops telling people apart past ~135: see CEILING)
                return new double[][]{{0, 30}, {60, 100}, {90, 400}, {105, 800}, {115, 1200}, {128, 1600},
                    {140, 2000}, {160, 2400}};
            case T_COOPER: // metres run in 12 minutes
                return new double[][]{{0, 30}, {1000, 100}, {1450, 400}, {1750, 800}, {1950, 1200}, {2250, 1600},
                    {2550, 2000}, {3000, 2400}, {3500, 2600}, {3900, 2800}, {4300, 3200}};
            case T_BALANCE: // one-leg stand seconds, eyes open (past 60: 60 + eyes-closed seconds x 3)
                return new double[][]{{0, 30}, {5, 100}, {15, 400}, {32, 800}, {43, 1200}, {55, 1600}, {105, 2000},
                    {150, 2400}, {195, 2600}, {240, 2800}, {300, 3200}};
            case T_SITRISE: // sitting-rising test score, 0-10 (a 10 is as far as this test can see: see CEILING)
                return new double[][]{{0, 30}, {2, 100}, {5, 400}, {7, 800}, {8, 1200}, {9, 1600}, {10, 2000}};
            case T_CARDIOTIME: // seconds of continuous moderate cardio a rating can sustain (for workout targets)
            default:
                return new double[][]{{0, 30}, {45, 100}, {90, 400}, {180, 800}, {300, 1200}, {480, 1600},
                    {720, 2000}, {1080, 2400}, {1500, 2800}, {1800, 3200}};
        }
    }

    /**
     * The highest rating a test can prove. Past it everyone maxes the test out, so it can't tell a fit person from
     * an Olympian: the march stops at 1800 (the 12-minute run goes higher), a perfect sit & rise at 2000.
     */
    static double ceiling(String test) {
        switch (test) {
            case T_MARCH: return 1800;
            case T_SITRISE: return 2000;
            default: return 3200;
        }
    }

    static double curve(String test, double v) {
        double[][] a = anchors(test);
        if (v <= a[0][0]) return a[0][1];
        for (int i = 1; i < a.length; i++) {
            if (v <= a[i][0]) {
                double t = (v - a[i - 1][0]) / (a[i][0] - a[i - 1][0]);
                return a[i - 1][1] + t * (a[i][1] - a[i - 1][1]);
            }
        }
        int n = a.length - 1;
        double slope = (a[n][1] - a[n - 1][1]) / (a[n][0] - a[n - 1][0]);
        return Math.min(3300, a[n][1] + (v - a[n][0]) * slope);
    }

    static double inverse(String test, double r) {
        double[][] a = anchors(test);
        if (r <= a[0][1]) return a[0][0];
        for (int i = 1; i < a.length; i++) {
            if (r <= a[i][1]) {
                double t = (r - a[i - 1][1]) / (a[i][1] - a[i - 1][1]);
                return a[i - 1][0] + t * (a[i][0] - a[i - 1][0]);
            }
        }
        int n = a.length - 1;
        double slope = (a[n][0] - a[n - 1][0]) / (a[n][1] - a[n - 1][1]);
        return a[n][0] + (r - a[n][1]) * slope;
    }

    /**
     * How much of her age-30 performance a healthy woman keeps at this age, by kind of test (approximated from
     * published age norms: ACSM push-up norms, Springer 2007 single-leg stance, VO2max ~10% a decade).
     */
    static double ageFactor(String test, double age) {
        double a, b;
        switch (test) {
            case T_PUSHUP: case T_BRIDGE: case T_CHAIR: a = 0.006; b = 0.0002; break;
            case T_PLANK: a = 0.004; b = 0.0001; break;
            case T_COOPER: case T_MARCH: a = 0.007; b = 0.00005; break;
            case T_BALANCE: a = 0.004; b = 0.00025; break;
            case T_SITRISE: a = 0.003; b = 0.0002; break;
            default: a = 0.007; b = 0.00005; break; // cardio
        }
        if (age < 20) return Math.max(0.6, 1 - 0.015 * (20 - age));
        if (age <= 30) return 1;
        double d = age - 30;
        return Math.max(0.25, 1 - a * d - b * d * d);
    }

    /** A raw test result at this age -> rating. */
    static double testRating(String test, double raw, double age) {
        return Math.min(ceiling(test), curve(test, raw / ageFactor(test, age)));
    }

    /** True when a result is at its test's ceiling (it only says "at least this good"). */
    static boolean atCeiling(String test, double rating) {
        return rating >= ceiling(test) - 0.5;
    }

    /**
     * A test's results -> the five categories. Two tests in a category are averaged, except that a test at its
     * ceiling (or a march next to a 12-minute run) only shows "at least", so it never pulls the other one down.
     */
    static double[] categories(java.util.Map<String, Double> raw, double age) {
        double pu = testRating(T_PUSHUP, val(raw, T_PUSHUP), age);
        double br = testRating(T_BRIDGE, val(raw, T_BRIDGE), age);
        double ch = testRating(T_CHAIR, val(raw, T_CHAIR), age);
        double pl = testRating(T_PLANK, val(raw, T_PLANK), age);
        double ma = testRating(T_MARCH, val(raw, T_MARCH), age);
        double ba = testRating(T_BALANCE, val(raw, T_BALANCE), age);
        double sr = testRating(T_SITRISE, val(raw, T_SITRISE), age);
        double[] c = new double[5];
        c[CARDIO] = val(raw, T_COOPER) > 0 ? Math.max(ma, testRating(T_COOPER, val(raw, T_COOPER), age)) : ma;
        c[UPPER] = pu;
        c[CURVES] = (br + ch) / 2;
        c[CORE] = pl;
        c[BALANCE] = atCeiling(T_SITRISE, sr) ? Math.max(ba, (ba + sr) / 2) : (ba + sr) / 2;
        return c;
    }

    static double val(java.util.Map<String, Double> raw, String k) {
        Double v = raw.get(k);
        return v == null ? 0 : v;
    }

    /** Estimated VO2max (ml/kg/min) from the 12-minute run (Cooper 1968). */
    static double vo2max(double metres) {
        return (metres - 504.9) / 44.73;
    }

    /** Roughly what share of women her age she does better than, for a rating. */
    static double percentile(double r) {
        double[][] a = {{0, 0.1}, {100, 1}, {400, 10}, {800, 30}, {1200, 50}, {1600, 75}, {2000, 90}, {2400, 99},
            {2600, 99.9}, {2800, 99.99}};
        if (r <= 0) return 0.1;
        for (int i = 1; i < a.length; i++)
            if (r <= a[i][0]) return a[i - 1][1] + (r - a[i - 1][0]) / (a[i][0] - a[i - 1][0]) * (a[i][1] - a[i - 1][1]);
        return 99.99;
    }

    static String standing(double r) {
        if (r >= 2800) return "Olympic level";
        if (r >= 2600) return "national-elite level";
        if (r >= 2400) return "top 1% of " + Flavor.PEERS + " your age";
        double p = percentile(r);
        if (p < 1) return "bottom 1% of " + Flavor.PEERS + " your age";
        return "better than about " + Math.round(p) + "% of " + Flavor.PEERS + " your age";
    }

    /** What someone of this rating and age can do in this test (or exercise basis). */
    static double capacity(String test, double rating, double age) {
        return inverse(test, rating) * ageFactor(test, age);
    }

    static double age(String birth, LocalDate on) {
        try {
            LocalDate b = LocalDate.parse(birth);
            return ChronoUnit.DAYS.between(b, on) / 365.2425;
        } catch (Exception e) {
            return 30;
        }
    }

    // ---- Elo ----
    static final String[] DIFF = {"EASY", "NORMAL", "HARD", "INSANE"};
    static final double[] DIFF_PCT = {0.60, 0.75, 0.85, 0.95}; // share of estimated max per set
    static final int[] DIFF_XP = {60, 100, 150, 220};

    static double expected(double r, double opponent) {
        return 1.0 / (1.0 + Math.pow(10, (opponent - r) / 400.0));
    }

    /** The test each category's targets are scaled by. */
    static final String[] CAT_TEST = {T_CARDIOTIME, T_PUSHUP, T_BRIDGE, T_PLANK, T_BALANCE};

    /**
     * The workout's own rating: the rating whose NORMAL targets equal this difficulty's targets. EASY plays someone
     * weaker than you, INSANE someone stronger, by exactly as much as the reps say.
     */
    static double opponent(int cat, double r, int diff) {
        String t = CAT_TEST[cat];
        return Math.max(30, curve(t, inverse(t, r) * DIFF_PCT[diff] / DIFF_PCT[1]));
    }

    /**
     * A workout's match score from the share of targets hit. Steep on purpose: finishing everything is a win (1),
     * 84% is a draw (0.5), half is nearly a loss (0.06). So the rating settles where NORMAL is a true, hard effort.
     */
    static double score(double completion) {
        return Math.pow(Math.max(0, Math.min(1, completion)), 4);
    }

    static double overall(double[] elo) {
        double s = 0;
        for (int i = 0; i < 5; i++) s += elo[i] * CAT_WEIGHT[i];
        return s;
    }

    // ---- Ranks: 8 tiers x 5 divisions = 40 ranks, 80 points each ----
    static final String[] TIERS = Flavor.TIERS;
    static final String[] TIER_MEANS = {"bottom 10% of " + Flavor.PEERS + " your age", "10th to 30th percentile",
        "30th to 50th percentile", "50th to 75th percentile", "75th to 90th percentile", "90th to 99th percentile",
        "top 1% to national elite", Flavor.TOP_MEANS};
    static final int[] TIER_COLOR = {0xFF9C8A7A, 0xFFC9A27E, 0xFF7FD1AE, 0xFF4FD8FF, 0xFF9B7BFF, 0xFFFF6FB5,
        0xFFFFC94D, 0xFFFFFFFF};
    static final String[] DIVS = {"V", "IV", "III", "II", "I"};
    static final double MAX = 3200; // the best in the world

    static int tier(double r) {
        return Math.max(0, Math.min(7, (int) (r / 400)));
    }

    static int rankIndex(double r) { // 0..39
        return Math.max(0, Math.min(39, (int) (r / 80)));
    }

    static String rankName(double r) {
        int i = rankIndex(r);
        return TIERS[i / 5] + " " + DIVS[i % 5];
    }

    /** Rating of the next rank, or -1 at the top. */
    static double nextRank(double r) {
        int i = rankIndex(r);
        return i >= 39 ? -1 : (i + 1) * 80;
    }

    // ---- Levels (XP) ----
    static int xpToNext(int level) {
        return 100 + 30 * (level - 1);
    }

    /** {level, xp into level, xp needed for next} */
    static int[] level(long xp) {
        int lv = 1;
        long left = xp;
        while (left >= xpToNext(lv)) {
            left -= xpToNext(lv);
            lv++;
        }
        return new int[]{lv, (int) left, xpToNext(lv)};
    }

    // ---- Steps ----
    /** Daily step goal: grows with STAMINA and with what she has actually been walking lately. */
    static int stepGoal(double cardioElo, int avg7) {
        // (his arc is a fat-loss arc: steps matter more, so his goals start higher)
        int byRating = (int) Math.max(Flavor.MALE ? 5000 : 2500, Math.min(Flavor.MALE ? 14000 : 12000,
            (Flavor.MALE ? 4000 : 2000) + (cardioElo - 100) * 3));
        int byHabit = (int) Math.min(15000, avg7 * 1.05);
        int g = Math.max(byRating, byHabit);
        return (g + 249) / 250 * 250;
    }

    /** Steps a day where the health benefits of walking level off at this age (Paluch et al. 2022). */
    static int stepTarget(double age) {
        return age >= 60 ? 7000 : 8500;
    }
}
