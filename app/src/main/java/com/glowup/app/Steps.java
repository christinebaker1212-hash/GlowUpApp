package com.glowup.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.time.LocalDate;

/**
 * The phone's own hardware step counter (TYPE_STEP_COUNTER: counts in a low-power chip all day, even with the app
 * closed). It counts since the phone started, so the app reads it every ~15 minutes and adds the difference to the day.
 */
final class Steps {
    private Steps() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("steps", Context.MODE_PRIVATE);
    }

    static boolean available(Context c) {
        SensorManager sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        return sm != null && sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
    }

    static boolean permitted(Context c) {
        return Build.VERSION.SDK_INT < 29
            || c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    static int on(Context c, String day) {
        return prefs(c).getInt("d:" + day, 0);
    }

    static boolean tracked(Context c, String day) {
        return prefs(c).getBoolean("t:" + day, false);
    }

    static int avg(Context c, LocalDate end, int days) {
        int sum = 0, n = 0;
        for (int i = 1; i <= days; i++) {
            String d = end.minusDays(i).toString();
            if (tracked(c, d)) {
                sum += on(c, d);
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    /** A reading of the cumulative counter. */
    static synchronized void reading(Context c, float counter) {
        SharedPreferences p = prefs(c);
        long boot = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        float last = p.getFloat("last", -1);
        long lastBoot = p.getLong("boot", 0);
        float delta;
        if (last < 0) delta = 0;                                                // first reading ever
        else if (Math.abs(boot - lastBoot) > 5 * 60_000 || counter < last) delta = counter; // the phone restarted
        else delta = counter - last;
        if (delta < 0 || delta > 60000) delta = 0;
        String today = LocalDate.now().toString();
        p.edit()
            .putInt("d:" + today, p.getInt("d:" + today, 0) + Math.round(delta))
            .putBoolean("t:" + today, true)
            .putFloat("last", counter)
            .putLong("boot", boot)
            .apply();
    }

    static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        if (js.getPendingJob(1) != null) return;
        js.schedule(new JobInfo.Builder(1, new ComponentName(c, StepJob.class))
            .setPeriodic(15 * 60_000L)
            .setPersisted(true)
            .build());
    }

    /** Wakes every ~15 minutes, takes one reading of the step counter and goes back to sleep. */
    public static final class StepJob extends JobService {
        @Override
        public boolean onStartJob(JobParameters params) {
            if (!permitted(this) || !Store.load(this).stepsOn) return false;
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (s == null) return false;
            Handler h = new Handler(Looper.getMainLooper());
            SensorEventListener l = new SensorEventListener() {
                boolean done;

                @Override
                public void onSensorChanged(SensorEvent e) {
                    if (done) return;
                    done = true;
                    reading(StepJob.this, e.values[0]);
                    sm.unregisterListener(this);
                    jobFinished(params, false);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };
            sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL);
            h.postDelayed(() -> {
                sm.unregisterListener(l);
                jobFinished(params, false);
            }, 20_000);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            return true;
        }
    }
}
