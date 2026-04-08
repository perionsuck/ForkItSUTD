package com.example.forkit.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {

    private ReminderScheduler() {}

    private static final String WORK_PREFIX = "forkit_reminder_";

    public static void apply(@NonNull Context ctx, boolean enabled, @NonNull String timesCsv) {
        WorkManager wm = WorkManager.getInstance(ctx.getApplicationContext());

        // cancel all existing reminder works (we use fixed slots 0..9).
        for (int i = 0; i < 10; i++) {
            wm.cancelUniqueWork(WORK_PREFIX + i);
        }
        if (!enabled) return;

        List<int[]> times = parseTimes(timesCsv);
        for (int i = 0; i < Math.min(10, times.size()); i++) {
            int[] hm = times.get(i);
            long delayMs = delayUntilNext(hm[0], hm[1]);
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(WORK_PREFIX + i)
                    .build();
            wm.enqueueUniqueWork(WORK_PREFIX + i, ExistingWorkPolicy.REPLACE, req);
        }
    }

    static long delayUntilNext(int hour, int minute) {
        TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
        Calendar now = Calendar.getInstance(tz);
        Calendar next = Calendar.getInstance(tz);
        next.setTimeInMillis(now.getTimeInMillis());
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);
        return Math.max(0, next.getTimeInMillis() - now.getTimeInMillis());
    }

    static List<int[]> parseTimes(String csv) {
        ArrayList<int[]> out = new ArrayList<>();
        if (csv == null) return out;
        String[] parts = csv.split(",");
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            String[] hm = s.split(":");
            if (hm.length != 2) continue;
            try {
                int h = Integer.parseInt(hm[0].trim());
                int m = Integer.parseInt(hm[1].trim());
                if (h < 0 || h > 23 || m < 0 || m > 59) continue;
                out.add(new int[]{h, m});
            } catch (Exception ignored) {}
        }
        if (out.isEmpty()) {
            out.add(new int[]{8, 0});
            out.add(new int[]{12, 0});
            out.add(new int[]{18, 0});
        }
        return out;
    }

    public static String normalizeTimesCsv(String csv) {
        List<int[]> times = parseTimes(csv);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.US, "%d:%02d", times.get(i)[0], times.get(i)[1]));
        }
        return sb.toString();
    }
}

