package com.glowup.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything the app remembers. Saved as versioned JSON so the data stays readable for decades. */
final class State {
    static final int SCHEMA = 2; // 2: the ratings scale built on population norms (1.1.0)

    int schemaLoaded = SCHEMA;
    String name = "", birth = "", start = "";
    double heightCm = 0;
    boolean imperial = true, remind = true, stepsOn = true, parqFlag = false;
    int remindH = 18, remindM = 0;
    double[] elo = new double[5];
    long xp = 0;
    int streak = 0, bestStreak = 0, freezes = 0;
    String streakThrough = "", stepEvalThrough = "", lastDecay = "";
    List<Test> tests = new ArrayList<>();
    List<Workout> workouts = new ArrayList<>();
    List<Measure> measures = new ArrayList<>();
    List<Snap> snaps = new ArrayList<>();
    Set<String> ach = new HashSet<>();
    Map<String, Integer> stepXp = new HashMap<>();
    Map<String, Integer> fuel = new HashMap<>(); // his FUEL missions: date -> bits done
    double bfGoal = 12;                          // his GOD FORM body fat goal (%)

    static final class Test {
        String date;
        double age;
        Map<String, Double> raw = new HashMap<>();
        Map<String, String> note = new HashMap<>();
        double[] cat = new double[5];
    }

    static final class Item {
        String ex;
        int[] target;
        int[] done;
    }

    static final class Workout {
        String date, type;
        int diff, durSec, xp, feel;
        boolean tiny;
        double completion;
        double[] before = new double[5], after = new double[5];
        List<Item> items = new ArrayList<>();
    }

    static final class Measure { // always metric inside
        String date;
        double waist, hip, bust, thigh, weight, neck, arm; // (neck, arm: his body-fat estimate and arms)
    }

    static final class Snap {
        String date;
        double[] elo = new double[5];
    }

    boolean onboarded() {
        return !tests.isEmpty();
    }

    Test lastTest() {
        return tests.isEmpty() ? null : tests.get(tests.size() - 1);
    }

    Workout workoutOn(String date) {
        for (int i = workouts.size() - 1; i >= 0; i--)
            if (workouts.get(i).date.equals(date) && !"BONUS".equals(workouts.get(i).type)) return workouts.get(i);
        return null;
    }

    void snapshot(String date) {
        Snap s;
        if (!snaps.isEmpty() && snaps.get(snaps.size() - 1).date.equals(date)) s = snaps.get(snaps.size() - 1);
        else {
            s = new Snap();
            s.date = date;
            snaps.add(s);
        }
        s.elo = elo.clone();
    }

    // ---- JSON ----
    static JSONArray arr(double[] d) throws JSONException {
        JSONArray a = new JSONArray();
        for (double v : d) a.put(Math.round(v * 10) / 10.0);
        return a;
    }

    static double[] darr(JSONArray a, int n) {
        double[] d = new double[n];
        if (a != null) for (int i = 0; i < n && i < a.length(); i++) d[i] = a.optDouble(i, 0);
        return d;
    }

    static JSONArray iarr(int[] d) {
        JSONArray a = new JSONArray();
        for (int v : d) a.put(v);
        return a;
    }

    static int[] iarr(JSONArray a) {
        if (a == null) return new int[0];
        int[] d = new int[a.length()];
        for (int i = 0; i < d.length; i++) d[i] = a.optInt(i, 0);
        return d;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("app", Flavor.APP);
        o.put("schema", SCHEMA);
        o.put("name", name).put("birth", birth).put("start", start).put("heightCm", heightCm);
        o.put("imperial", imperial).put("remind", remind).put("stepsOn", stepsOn).put("parqFlag", parqFlag);
        o.put("remindH", remindH).put("remindM", remindM);
        o.put("elo", arr(elo)).put("xp", xp);
        o.put("streak", streak).put("bestStreak", bestStreak).put("freezes", freezes);
        o.put("streakThrough", streakThrough).put("stepEvalThrough", stepEvalThrough).put("lastDecay", lastDecay);
        JSONArray ta = new JSONArray();
        for (Test t : tests) {
            JSONObject j = new JSONObject();
            j.put("date", t.date).put("age", t.age).put("cat", arr(t.cat));
            JSONObject r = new JSONObject();
            for (Map.Entry<String, Double> e : t.raw.entrySet()) r.put(e.getKey(), e.getValue());
            j.put("raw", r);
            JSONObject n = new JSONObject();
            for (Map.Entry<String, String> e : t.note.entrySet()) n.put(e.getKey(), e.getValue());
            j.put("note", n);
            ta.put(j);
        }
        o.put("tests", ta);
        JSONArray wa = new JSONArray();
        for (Workout w : workouts) {
            JSONObject j = new JSONObject();
            j.put("date", w.date).put("type", w.type).put("diff", w.diff).put("durSec", w.durSec).put("xp", w.xp)
                .put("feel", w.feel).put("tiny", w.tiny).put("completion", w.completion)
                .put("before", arr(w.before)).put("after", arr(w.after));
            JSONArray ia = new JSONArray();
            for (Item it : w.items) {
                ia.put(new JSONObject().put("ex", it.ex).put("target", iarr(it.target)).put("done", iarr(it.done)));
            }
            j.put("items", ia);
            wa.put(j);
        }
        o.put("workouts", wa);
        JSONArray ma = new JSONArray();
        for (Measure m : measures) {
            ma.put(new JSONObject().put("date", m.date).put("waist", m.waist).put("hip", m.hip).put("bust", m.bust)
                .put("thigh", m.thigh).put("weight", m.weight).put("neck", m.neck).put("arm", m.arm));
        }
        o.put("measures", ma);
        JSONArray sa = new JSONArray();
        for (Snap s : snaps) sa.put(new JSONObject().put("date", s.date).put("elo", arr(s.elo)));
        o.put("snaps", sa);
        o.put("ach", new JSONArray(ach));
        JSONObject sx = new JSONObject();
        for (Map.Entry<String, Integer> e : stepXp.entrySet()) sx.put(e.getKey(), e.getValue());
        o.put("stepXp", sx);
        JSONObject fu = new JSONObject();
        for (Map.Entry<String, Integer> e : fuel.entrySet()) fu.put(e.getKey(), e.getValue());
        o.put("fuel", fu).put("bfGoal", bfGoal);
        return o;
    }

    static State fromJson(JSONObject o) {
        State s = new State();
        // older files are converted after loading (Logic.migrate), never dropping data
        s.schemaLoaded = o.optInt("schema", 1);
        s.name = o.optString("name", "");
        s.birth = o.optString("birth", "");
        s.start = o.optString("start", "");
        s.heightCm = o.optDouble("heightCm", 0);
        s.imperial = o.optBoolean("imperial", true);
        s.remind = o.optBoolean("remind", true);
        s.stepsOn = o.optBoolean("stepsOn", true);
        s.parqFlag = o.optBoolean("parqFlag", false);
        s.remindH = o.optInt("remindH", 18);
        s.remindM = o.optInt("remindM", 0);
        s.elo = darr(o.optJSONArray("elo"), 5);
        s.xp = o.optLong("xp", 0);
        s.streak = o.optInt("streak", 0);
        s.bestStreak = o.optInt("bestStreak", 0);
        s.freezes = o.optInt("freezes", 0);
        s.streakThrough = o.optString("streakThrough", "");
        s.stepEvalThrough = o.optString("stepEvalThrough", "");
        s.lastDecay = o.optString("lastDecay", "");
        JSONArray ta = o.optJSONArray("tests");
        if (ta != null) for (int i = 0; i < ta.length(); i++) {
            JSONObject j = ta.optJSONObject(i);
            if (j == null) continue;
            Test t = new Test();
            t.date = j.optString("date");
            t.age = j.optDouble("age", 30);
            t.cat = darr(j.optJSONArray("cat"), 5);
            JSONObject r = j.optJSONObject("raw");
            if (r != null) for (Iterator<String> k = r.keys(); k.hasNext(); ) {
                String key = k.next();
                t.raw.put(key, r.optDouble(key, 0));
            }
            JSONObject n = j.optJSONObject("note");
            if (n != null) for (Iterator<String> k = n.keys(); k.hasNext(); ) {
                String key = k.next();
                t.note.put(key, n.optString(key, ""));
            }
            s.tests.add(t);
        }
        JSONArray wa = o.optJSONArray("workouts");
        if (wa != null) for (int i = 0; i < wa.length(); i++) {
            JSONObject j = wa.optJSONObject(i);
            if (j == null) continue;
            Workout w = new Workout();
            w.date = j.optString("date");
            w.type = j.optString("type");
            w.diff = j.optInt("diff");
            w.durSec = j.optInt("durSec");
            w.xp = j.optInt("xp");
            w.feel = j.optInt("feel");
            w.tiny = j.optBoolean("tiny");
            w.completion = j.optDouble("completion", 0);
            w.before = darr(j.optJSONArray("before"), 5);
            w.after = darr(j.optJSONArray("after"), 5);
            JSONArray ia = j.optJSONArray("items");
            if (ia != null) for (int k = 0; k < ia.length(); k++) {
                JSONObject ij = ia.optJSONObject(k);
                if (ij == null) continue;
                Item it = new Item();
                it.ex = ij.optString("ex");
                it.target = iarr(ij.optJSONArray("target"));
                it.done = iarr(ij.optJSONArray("done"));
                w.items.add(it);
            }
            s.workouts.add(w);
        }
        JSONArray ma = o.optJSONArray("measures");
        if (ma != null) for (int i = 0; i < ma.length(); i++) {
            JSONObject j = ma.optJSONObject(i);
            if (j == null) continue;
            Measure m = new Measure();
            m.date = j.optString("date");
            m.waist = j.optDouble("waist", 0);
            m.hip = j.optDouble("hip", 0);
            m.bust = j.optDouble("bust", 0);
            m.thigh = j.optDouble("thigh", 0);
            m.neck = j.optDouble("neck", 0);
            m.arm = j.optDouble("arm", 0);
            m.weight = j.optDouble("weight", 0);
            s.measures.add(m);
        }
        JSONArray sa = o.optJSONArray("snaps");
        if (sa != null) for (int i = 0; i < sa.length(); i++) {
            JSONObject j = sa.optJSONObject(i);
            if (j == null) continue;
            Snap sn = new Snap();
            sn.date = j.optString("date");
            sn.elo = darr(j.optJSONArray("elo"), 5);
            s.snaps.add(sn);
        }
        JSONArray aa = o.optJSONArray("ach");
        if (aa != null) for (int i = 0; i < aa.length(); i++) s.ach.add(aa.optString(i));
        JSONObject sx = o.optJSONObject("stepXp");
        if (sx != null) for (Iterator<String> k = sx.keys(); k.hasNext(); ) {
            String key = k.next();
            s.stepXp.put(key, sx.optInt(key));
        }
        JSONObject fu = o.optJSONObject("fuel");
        if (fu != null) for (Iterator<String> k = fu.keys(); k.hasNext(); ) {
            String key = k.next();
            s.fuel.put(key, fu.optInt(key));
        }
        s.bfGoal = o.optDouble("bfGoal", 12);
        return s;
    }
}
