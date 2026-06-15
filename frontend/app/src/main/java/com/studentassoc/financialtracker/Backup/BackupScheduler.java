package com.studentassoc.financialtracker.Backup;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BackupScheduler {
    private static final String TAG = "BackupScheduler";
    private static final String BACKUP_WORK_TAG = "auto_backup";
    private static final String BACKUP_WORK_NAME = "periodic_backup";

    public static void scheduleAutoBackup(Context context, int intervalHours) {
        Log.d(TAG, "Scheduling auto-backup every " + intervalHours + " hours");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest backupWork = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class,
                intervalHours,
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(BACKUP_WORK_TAG)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        BACKUP_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        backupWork
                );
        Log.d(TAG, "Auto-backup scheduled successfully");
    }

    public static void cancelAutoBackup(Context context) {
        Log.d(TAG, "Cancelling auto-backup");

        WorkManager.getInstance(context)
                .cancelAllWorkByTag(BACKUP_WORK_TAG);

        Log.d(TAG, "Auto-backup cancelled");
    }

    public static boolean isAutoBackupScheduled(Context context) {
        return context.getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE)
                .getBoolean("auto_backup_enabled", false);
    }
}
