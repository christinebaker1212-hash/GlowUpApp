package com.glowup.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/** Loads / saves the state (written to a temp file, then swapped in, so a crash never corrupts it). */
final class Store {
    private Store() {}

    private static File file(Context c) {
        return new File(c.getFilesDir(), "glowup.json");
    }

    static State load(Context c) {
        try {
            File f = file(c);
            if (!f.exists()) return new State();
            byte[] b = new byte[(int) f.length()];
            try (FileInputStream in = new FileInputStream(f)) {
                int off = 0;
                while (off < b.length) {
                    int n = in.read(b, off, b.length - off);
                    if (n < 0) break;
                    off += n;
                }
            }
            State s = State.fromJson(new JSONObject(new String(b, StandardCharsets.UTF_8)));
            if (s.schemaLoaded < State.SCHEMA) {
                Logic.migrate(s);
                save(c, s);
            }
            return s;
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            file(c).renameTo(new File(c.getFilesDir(), "glowup-unreadable-" + System.currentTimeMillis() + ".json"));
            return new State();
        }
    }

    static void save(Context c, State s) {
        try {
            File tmp = new File(c.getFilesDir(), "glowup.json.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(s.toJson().toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.renameTo(file(c));
        } catch (Exception ignored) {
        }
    }

    /** A backup: the state plus the step history. */
    static String export(Context c, State s) throws Exception {
        JSONObject o = s.toJson();
        JSONObject steps = new JSONObject();
        SharedPreferences p = Steps.prefs(c);
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith("d:") && e.getValue() instanceof Integer)
                steps.put(e.getKey().substring(2), e.getValue());
        }
        o.put("steps", steps);
        return o.toString(1);
    }

    static State importJson(Context c, String text) throws Exception {
        JSONObject o = new JSONObject(text);
        if (!o.has("schema") || !o.has("elo")) throw new IllegalArgumentException("Not a GLOW UP backup");
        State s = State.fromJson(o);
        Logic.migrate(s);
        JSONObject steps = o.optJSONObject("steps");
        if (steps != null) {
            SharedPreferences.Editor ed = Steps.prefs(c).edit();
            for (Iterator<String> k = steps.keys(); k.hasNext(); ) {
                String day = k.next();
                int v = steps.optInt(day);
                if (v > Steps.prefs(c).getInt("d:" + day, 0)) ed.putInt("d:" + day, v);
                ed.putBoolean("t:" + day, true);
            }
            ed.apply();
        }
        save(c, s);
        return s;
    }
}
