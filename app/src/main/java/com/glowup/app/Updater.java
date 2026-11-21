package com.glowup.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Updates from the app's GitHub releases: reads the latest release, downloads its APK and installs it through
 * Android's PackageInstaller. Android installs it only if it's signed with the same key as the installed app.
 */
final class Updater {
    private Updater() {}

    static final String REPO = "christinebaker1212-hash/GlowUpApp";
    static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final long CHECK_EVERY = 6 * 3600_000L;

    static final class Release {
        String version, notes, apkUrl;
        long size;
    }

    static String installed(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    /** "1.10.2" > "1.9.9" */
    static boolean newer(String a, String b) {
        String[] x = a.replaceAll("[^0-9.]", "").split("\\."), y = b.replaceAll("[^0-9.]", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }

    static Release fetch() throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(LATEST).openConnection();
        h.setRequestProperty("Accept", "application/vnd.github+json");
        h.setRequestProperty("User-Agent", "GlowUp-Updater");
        h.setConnectTimeout(15000);
        h.setReadTimeout(15000);
        if (h.getResponseCode() == 404) return null; // no release yet
        if (h.getResponseCode() != 200) throw new Exception("GitHub said " + h.getResponseCode());
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (InputStream in = h.getInputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        }
        JSONObject o = new JSONObject(bo.toString("UTF-8"));
        Release r = new Release();
        r.version = o.optString("tag_name", "").replaceFirst("^v", "");
        r.notes = o.optString("body", "");
        JSONArray as = o.optJSONArray("assets");
        if (as != null) for (int i = 0; i < as.length(); i++) {
            JSONObject a = as.getJSONObject(i);
            if (a.optString("name").toLowerCase(Locale.US).endsWith(".apk")) {
                r.apkUrl = a.optString("browser_download_url");
                r.size = a.optLong("size");
            }
        }
        return r.apkUrl == null ? null : r;
    }

    /** On opening the app (at most every few hours, quietly), or from ME > CHECK FOR UPDATES (manual). */
    static void check(Activity a, boolean manual) {
        SharedPreferences p = a.getSharedPreferences("update", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!manual && now - p.getLong("checked", 0) < CHECK_EVERY) return;
        p.edit().putLong("checked", now).apply();
        Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Release r = fetch();
                String mine = installed(a);
                ui.post(() -> {
                    if (a.isFinishing()) return;
                    if (r != null && newer(r.version, mine)) {
                        if (!manual && r.version.equals(p.getString("skipped", ""))) return;
                        offer(a, r);
                    } else if (manual) {
                        Toast.makeText(a, "You have the latest version (" + mine + ")", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                if (manual) ui.post(() -> Toast.makeText(a, "Couldn't check for updates: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    static void offer(Activity a, Release r) {
        String notes = r.notes.replaceAll("(?m)^#+\\s*", "").trim();
        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("UPDATE: GLOW UP " + r.version)
            .setMessage((notes.isEmpty() ? "A new version is ready." : notes) + "\n\nYour progress stays exactly as it is.")
            .setPositiveButton("UPDATE NOW", (d, w) -> install(a, r))
            .setNegativeButton("LATER", null)
            .setNeutralButton("SKIP THIS ONE", (d, w) -> a.getSharedPreferences("update", Context.MODE_PRIVATE)
                .edit().putString("skipped", r.version).apply())
            .show();
    }

    static void install(Activity a, Release r) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("One-time permission")
                .setMessage("Android needs your OK for GLOW UP to install its own updates. Turn on \"Allow from this "
                    + "source\", come back, and tap UPDATE NOW again.")
                .setPositiveButton("OPEN SETTINGS", (d, w) -> a.startActivity(new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()))))
                .setNegativeButton("CANCEL", null).show();
            return;
        }
        TextView msg = Ui.text(a, "Downloading… 0%", 16, Ui.TEXT);
        int pd = Ui.dp(a, 24);
        msg.setPadding(pd, pd, pd, pd / 2);
        AlertDialog dlg = new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("UPDATING").setView(msg).setCancelable(false).show();
        Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                PackageInstaller pi = a.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sp.setAppPackageName(a.getPackageName());
                if (Build.VERSION.SDK_INT >= 31) sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                int id = pi.createSession(sp);
                try (PackageInstaller.Session s = pi.openSession(id)) {
                    HttpURLConnection h = (HttpURLConnection) new URL(r.apkUrl).openConnection();
                    h.setRequestProperty("User-Agent", "GlowUp-Updater");
                    h.setInstanceFollowRedirects(true);
                    long total = h.getContentLengthLong() > 0 ? h.getContentLengthLong() : r.size;
                    try (InputStream in = h.getInputStream(); OutputStream out = s.openWrite("glowup.apk", 0, total > 0 ? total : -1)) {
                        byte[] buf = new byte[65536];
                        long got = 0;
                        int n, lastPct = -1;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            got += n;
                            int pct = total > 0 ? (int) (got * 100 / total) : 0;
                            if (pct != lastPct) {
                                lastPct = pct;
                                ui.post(() -> msg.setText("Downloading… " + pct + "%"));
                            }
                        }
                        s.fsync(out);
                    }
                    Intent done = new Intent(a, Done.class).setAction("glowup.UPDATE_DONE");
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                    s.commit(PendingIntent.getBroadcast(a, id, done, flags).getIntentSender());
                }
                ui.post(() -> msg.setText("Installing…"));
                ui.postDelayed(() -> { if (dlg.isShowing()) dlg.dismiss(); }, 4000);
            } catch (Exception e) {
                ui.post(() -> {
                    dlg.dismiss();
                    Toast.makeText(a, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** The installer's answer: ask the user to confirm when Android wants that, report failures. */
    public static final class Done extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(confirm);
                }
            } else if (status != PackageInstaller.STATUS_SUCCESS) {
                String m = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Toast.makeText(c, "Update didn't install" + (m == null ? "" : ": " + m), Toast.LENGTH_LONG).show();
            }
        }
    }
}
