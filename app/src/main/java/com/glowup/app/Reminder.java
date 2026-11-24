package com.glowup.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.util.Calendar;

/** The daily nudge, and putting it (and the step reader) back after the phone restarts. */
public final class Reminder extends BroadcastReceiver {
    private static final String CH = "daily";

    static void schedule(Context c, State s) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(c, Reminder.class).setAction("glowup.REMIND"),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        if (!s.remind) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, s.remindH);
        cal.set(Calendar.MINUTE, s.remindM);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        State s = Store.load(c);
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            schedule(c, s);
            Steps.schedule(c);
            return;
        }
        if (!s.onboarded()) return;
        String today = LocalDate.now().toString();
        String type = Plan.typeFor(s, LocalDate.now());
        if (s.workoutOn(today) != null) return; // already done: no nagging
        String title, body;
        if (Plan.REST.equals(type)) {
            title = "Rest day";
            body = "Recovery is where muscle is built. A gentle walk still counts toward your streak.";
        } else {
            String[] lines = {
                "Today's mission: " + type + ". Even TINY MODE keeps your streak alive.",
                "Your streak is " + s.streak + " days. Never miss twice!",
                "10 minutes. That's all today asks. " + type + " is ready.",
                "XP is waiting. " + type + " today.",
            };
            title = Flavor.APP;
            body = lines[(int) (LocalDate.now().toEpochDay() % lines.length)];
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(CH, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build();
        try {
            nm.notify(1, n);
        } catch (SecurityException ignored) {
        }
    }
}
