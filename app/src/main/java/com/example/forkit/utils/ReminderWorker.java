package com.example.forkit.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.forkit.R;

/**
 * Shows a single reminder notification, then relies on the scheduler
 * to enqueue future reminders when settings change.
 */
public class ReminderWorker extends Worker {

    private static final String CHANNEL_ID = "forkit_reminders";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        PrefsHelper p = new PrefsHelper(ctx);
        if (!p.getRemindersEnabled()) return Result.success();

        ensureChannel(ctx);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_streak_flame)
                .setContentTitle("ForkIt reminder")
                .setContentText("Time to log your meal and water.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(ctx).notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), b.build());
        } catch (SecurityException ignored) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
        }
        return Result.success();
    }

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = nm.getNotificationChannel(CHANNEL_ID);
        if (ch != null) return;
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID,
                "ForkIt reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        ));
    }
}

