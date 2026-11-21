package com.glowup.app;

import android.content.Context;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** What happens to the numbers: each day, after a workout, after a test. */
final class Logic {
    private Logic() {}

    static final String[][] ACH = {
        {"first_test", "BASELINE SET", "Took your first fitness test"},
        {"first_workout", "DAY ONE", "Finished your first workout"},
        {"streak3", "HAT TRICK", "3-day streak"},
        {"streak7", "ONE WEEK STRONG", "7-day streak"},
        {"streak30", "HABIT FORMED", "30-day streak"},
        {"streak100", "UNSTOPPABLE", "100-day streak"},
        {"streak365", "A WHOLE YEAR", "365-day streak"},
        {"w10", "TEN DOWN", "10 workouts"},
        {"w50", "FIFTY", "50 workouts"},
        {"w100", "CENTURY", "100 workouts"},
        {"w500", "LIFER", "500 workouts"},
        {"hard", "HARD CARRY", "Completed a HARD workout"},
        {"insane", "INSANE MODE", "Completed an INSANE workout"},
        {"steps5k", "OUT AND ABOUT", "5,000 steps in a day"},
        {"steps10k", "10K CLUB", "10,000 steps in a day"},
        {"steps15k", "TRAILBLAZER", "15,000 steps in a day"},
        {"pushup", "THE REAL THING", "A full push-up"},
        {"plank60", "ONE MINUTE", "A 60-second forearm plank"},
        {"glute_sl", "ONE-LEG WONDER", "Unlocked single-leg glute work"},
        {"sitrise8", "RISE UP", "Sitting-rising test score of 8+"},
        {"balance10", "STEADY", "10 seconds on one leg"},
        {"measure", "TAPE MEASURE", "Logged your measurements"},
        {"retest", "PROOF", "Took a retest"},
        {"tier1", "OFF THE COUCH!", "Reached OFF THE COUCH rank"},
        {"tier2", "WEEKEND MOVER", "Reached WEEKEND MOVER rank"},
        {"tier3", "ACTIVE", "Reached ACTIVE rank"},
        {"tier4", "ATHLETIC", "Reached ATHLETIC rank"},
        {"tier5", "COMPETITOR", "Reached COMPETITOR rank"},
        {"tier6", "ELITE", "Reached ELITE rank"},
        {"tier7", "OLYMPIAN", "Reached OLYMPIAN rank"},
        {"lv5", "LEVEL 5", "Reached level 5"},
        {"lv10", "LEVEL 10", "Reached level 10"},
        {"lv25", "LEVEL 25", "Reached level 25"},
        {"lv50", "LEVEL 50", "Reached level 50"},
    };

    static String achName(String id) {
        for (String[] a : ACH) if (a[0].equals(id)) return a[1];
        return id;
    }

    static int goalOn(Context c, State s, LocalDate d) {
        return Science.stepGoal(s.elo[Science.CARDIO], Steps.avg(c, d, 7));
    }

    static boolean active(Context c, State s, LocalDate d) {
        String ds = d.toString();
        for (State.Workout w : s.workouts) if (w.date.equals(ds)) return true;
        if (Plan.REST.equals(Plan.typeFor(s, d))) return true;
        return s.stepsOn && Steps.tracked(c, ds) && Steps.on(c, ds) >= goalOn(c, s, d);
    }

    static int streakNow(Context c, State s) {
        return s.streak + (active(c, s, LocalDate.now()) ? 1 : 0);
    }

    private static LocalDate parse(String d, LocalDate dflt) {
        try {
            return LocalDate.parse(d);
        } catch (Exception e) {
            return dflt;
        }
    }

    /** Run when the app opens: steps' XP and STAMINA, the streak, detraining. Returns news for the player. */
    static List<String> daily(Context c, State s) {
        List<String> news = new ArrayList<>();
        if (!s.onboarded()) return news;
        LocalDate today = LocalDate.now(), yesterday = today.minusDays(1);
        boolean changed = false;

        // XP for steps, live (the last week, in case the app wasn't opened)
        for (int i = 0; i <= 7; i++) {
            LocalDate d = today.minusDays(i);
            String ds = d.toString();
            if (!Steps.tracked(c, ds)) continue;
            int steps = Steps.on(c, ds);
            int earned = steps / 100 + (steps >= goalOn(c, s, d) ? 50 : 0);
            int had = s.stepXp.containsKey(ds) ? s.stepXp.get(ds) : 0;
            if (earned > had) {
                s.xp += earned - had;
                s.stepXp.put(ds, earned);
                changed = true;
            }
        }
        // keep the map small
        if (s.stepXp.size() > 30) s.stepXp.keySet().removeIf(k -> parse(k, today).isBefore(today.minusDays(14)));

        // Each finished day's steps are a STAMINA "match" against the day's goal
        LocalDate from = parse(s.stepEvalThrough, yesterday).plusDays(1);
        if (from.isBefore(today.minusDays(30))) from = today.minusDays(30);
        for (LocalDate d = from; !d.isAfter(yesterday); d = d.plusDays(1)) {
            String ds = d.toString();
            if (!s.stepsOn || !Steps.tracked(c, ds)) continue;
            int goal = goalOn(c, s, d);
            double frac = Math.min(1.0, Steps.on(c, ds) / (double) goal);
            double sc = frac >= 1 ? 1 : Math.pow(frac, 3);
            double delta = 6 * (sc - 0.5);
            s.elo[Science.CARDIO] = clamp(s.elo[Science.CARDIO] + delta);
            if (d.equals(yesterday) && sc >= 1) news.add(String.format("Yesterday's step goal hit: STAMINA +%.0f", delta));
            changed = true;
        }
        s.stepEvalThrough = yesterday.toString();

        // The streak (rest days count; a missed day spends a STREAK FREEZE if there is one)
        LocalDate sf = parse(s.streakThrough, yesterday).plusDays(1);
        if (sf.isBefore(today.minusDays(60))) sf = today.minusDays(60);
        for (LocalDate d = sf; !d.isAfter(yesterday); d = d.plusDays(1)) {
            if (active(c, s, d)) {
                s.streak++;
                if (s.streak % 7 == 0 && s.freezes < 3) {
                    s.freezes++;
                    news.add("7 more days! You earned a STREAK FREEZE.");
                }
            } else if (s.freezes > 0 && s.streak > 0) {
                s.freezes--;
                news.add("A STREAK FREEZE saved your streak for " + d.getDayOfWeek() + ". Never miss twice!");
            } else {
                if (s.streak >= 3) news.add("Streak reset. That's okay: today is day one again.");
                s.streak = 0;
            }
            s.bestStreak = Math.max(s.bestStreak, s.streak);
            changed = true;
        }
        s.streakThrough = yesterday.toString();

        // Detraining: fitness fades after about two weeks with no training (roughly, to keep the rating honest)
        LocalDate last = parse(s.lastTest().date, today);
        for (State.Workout w : s.workouts) {
            LocalDate wd = parse(w.date, last);
            if (wd.isAfter(last)) last = wd;
        }
        LocalDate decayFrom = last.plusDays(15);
        LocalDate ld = parse(s.lastDecay, decayFrom.minusDays(1)).plusDays(1);
        if (ld.isAfter(decayFrom)) decayFrom = ld;
        long idle = ChronoUnit.DAYS.between(decayFrom, today) + 1;
        if (idle > 0) {
            for (int i = 0; i < 5; i++) s.elo[i] = Math.max(30, s.elo[i] - 2 * Math.min(idle, 60));
            news.add("It's been a while: your ratings faded a little. One workout starts the climb back.");
            s.lastDecay = today.toString();
            changed = true;
        }

        if (changed) s.snapshot(today.toString());
        news.addAll(checkAch(c, s));
        Store.save(c, s);
        return news;
    }

    static double clamp(double r) {
        return Math.max(30, Math.min(3300, r));
    }

    /** Result of a workout, for the WORKOUT PASSED screen. */
    static final class Result {
        double[] before, after;
        double completion;
        int xp, levelBefore, levelAfter;
        String rankBefore, rankAfter;
        List<String> ach = new ArrayList<>();
    }

    static Result finishWorkout(Context c, State s, State.Workout w) {
        Result r = new Result();
        r.before = s.elo.clone();
        r.levelBefore = Science.level(s.xp)[0];
        r.rankBefore = Science.rankName(Science.overall(s.elo));
        double[] got = new double[5], want = new double[5];
        double gotAll = 0, wantAll = 0;
        for (State.Item it : w.items) {
            Plan.Ex e = Plan.ex(it.ex);
            for (int i = 0; i < it.target.length; i++) {
                double d = Math.max(0, Math.min(it.target[i], it.done[i]));
                if (e.cat >= 0) {
                    got[e.cat] += d;
                    want[e.cat] += it.target[i];
                }
                gotAll += d / it.target[i];
                wantAll += 1;
            }
        }
        w.completion = wantAll == 0 ? 1 : gotAll / wantAll;
        boolean scored = !Plan.RECOVERY.equals(w.type);
        if (scored) {
            int done = 0;
            for (State.Workout x : s.workouts) if (!Plan.RECOVERY.equals(x.type)) done++;
            double k = done < 10 ? 40 : 24;          // placement games move faster
            if (w.tiny) k *= 0.5;
            if ("BONUS".equals(w.type)) k *= 0.5;
            for (int cat = 0; cat < 5; cat++) {
                if (want[cat] <= 0) continue;
                double score = Science.score(got[cat] / want[cat]);
                double opp = Science.opponent(cat, s.elo[cat], w.tiny ? 0 : w.diff);
                s.elo[cat] = clamp(s.elo[cat] + k * (score - Science.expected(s.elo[cat], opp)));
            }
        }
        int base = Plan.RECOVERY.equals(w.type) ? 50 : Science.DIFF_XP[w.tiny ? 0 : w.diff];
        double mult = w.tiny ? 0.5 : "BONUS".equals(w.type) ? 0.5 : 1;
        w.xp = (int) Math.round(base * mult * w.completion) + Math.min(10, s.streak) * 5;
        w.before = r.before;
        w.after = s.elo.clone();
        s.xp += w.xp;
        s.workouts.add(w);
        s.snapshot(w.date);
        r.ach.addAll(checkAch(c, s));
        r.after = s.elo.clone();
        r.completion = w.completion;
        r.xp = w.xp;
        r.levelAfter = Science.level(s.xp)[0];
        r.rankAfter = Science.rankName(Science.overall(s.elo));
        Store.save(c, s);
        return r;
    }

    /** A finished fitness test (raw results already age-free: rated at today's age). */
    static double[] finishTest(Context c, State s, State.Test t) {
        LocalDate today = LocalDate.now();
        t.date = today.toString();
        t.age = Science.age(s.birth, today);
        t.cat = Science.categories(t.raw, t.age);
        boolean first = !s.onboarded();
        for (int i = 0; i < 5; i++) s.elo[i] = first ? t.cat[i] : clamp((s.elo[i] + t.cat[i]) / 2);
        if (first) {
            s.start = today.toString();
            s.streakThrough = today.minusDays(1).toString();
            s.stepEvalThrough = today.minusDays(1).toString();
        }
        s.tests.add(t);
        s.xp += 150;
        s.snapshot(today.toString());
        checkAch(c, s);
        Store.save(c, s);
        return t.cat;
    }

    /**
     * Old saves (schema 1, the first scale) re-rated on the current scale: every test is scored again from its raw
     * results, and her ratings, history and workouts move by how much her latest test's scores moved.
     */
    static void migrate(State s) {
        if (s.schemaLoaded >= State.SCHEMA) return;
        if (!s.tests.isEmpty()) {
            double[] old = s.lastTest().cat.clone();
            for (State.Test t : s.tests) t.cat = Science.categories(t.raw, t.age);
            double[] now = s.lastTest().cat;
            double[] d = new double[5];
            for (int i = 0; i < 5; i++) d[i] = now[i] - old[i];
            for (int i = 0; i < 5; i++) s.elo[i] = clamp(s.elo[i] + d[i]);
            for (State.Snap sn : s.snaps) for (int i = 0; i < 5; i++) sn.elo[i] = clamp(sn.elo[i] + d[i]);
            for (State.Workout w : s.workouts) for (int i = 0; i < 5; i++) {
                w.before[i] = clamp(w.before[i] + d[i]);
                w.after[i] = clamp(w.after[i] + d[i]);
            }
            // rank trophies earned on the old, generous scale are kept: they were earned
        }
        s.schemaLoaded = State.SCHEMA;
    }

    static List<String> checkAch(Context c, State s) {
        List<String> got = new ArrayList<>();
        int workouts = 0;
        boolean hard = false, insane = false, pushup = false, plank60 = false, sl = false;
        for (State.Workout w : s.workouts) {
            if (!Plan.RECOVERY.equals(w.type)) workouts++;
            if (!w.tiny && w.completion >= 0.999 && w.diff >= 2) hard = true;
            if (!w.tiny && w.completion >= 0.999 && w.diff >= 3) insane = true;
            for (State.Item it : w.items) {
                for (int d : it.done) {
                    if (it.ex.equals("pushup") && d >= 1) pushup = true;
                    if (it.ex.equals("decline_pushup") && d >= 1) pushup = true;
                    if (it.ex.equals("plank") && d >= 60) plank60 = true;
                    if ((it.ex.equals("sl_bridge") || it.ex.equals("sl_thrust")) && d >= 1) sl = true;
                }
            }
        }
        double sitrise = 0, balance = 0;
        for (State.Test t : s.tests) {
            Double v = t.raw.get(Science.T_SITRISE);
            if (v != null) sitrise = Math.max(sitrise, v);
            v = t.raw.get(Science.T_BALANCE);
            if (v != null) balance = Math.max(balance, v);
            v = t.raw.get(Science.T_PUSHUP);
            if (v != null && v >= 1 && "full".equals(t.note.get(Science.T_PUSHUP))) pushup = true;
            v = t.raw.get(Science.T_PLANK);
            if (v != null && v >= 60) plank60 = true;
        }
        int bestSteps = 0;
        for (int i = 0; i < 400; i++) bestSteps = Math.max(bestSteps, Steps.on(c, LocalDate.now().minusDays(i).toString()));
        int streak = streakNow(c, s);
        int best = Math.max(s.bestStreak, streak);
        int tier = Science.tier(Science.overall(s.elo));
        int lv = Science.level(s.xp)[0];
        boolean[] ok = {
            !s.tests.isEmpty(), workouts >= 1, best >= 3, best >= 7, best >= 30, best >= 100, best >= 365,
            workouts >= 10, workouts >= 50, workouts >= 100, workouts >= 500, hard, insane,
            bestSteps >= 5000, bestSteps >= 10000, bestSteps >= 15000, pushup, plank60, sl,
            sitrise >= 8, balance >= 10, !s.measures.isEmpty(), s.tests.size() >= 2,
            tier >= 1, tier >= 2, tier >= 3, tier >= 4, tier >= 5, tier >= 6, tier >= 7,
            lv >= 5, lv >= 10, lv >= 25, lv >= 50,
        };
        for (int i = 0; i < ACH.length && i < ok.length; i++) {
            if (ok[i] && s.ach.add(ACH[i][0])) {
                s.xp += 100;
                got.add("ACHIEVEMENT: " + ACH[i][1] + " (+100 XP)");
            }
        }
        return got;
    }
}
