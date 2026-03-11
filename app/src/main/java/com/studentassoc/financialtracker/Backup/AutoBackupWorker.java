package com.studentassoc.financialtracker.Backup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.studentassoc.financialtracker.DTO.TransactionDao;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Repository.AppDatabase;
import com.studentassoc.financialtracker.services.BackupManager;

import java.util.List;

public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";
    private static final String NOTIFICATION_CHANNEL_ID = "backup_channel";
    private static final int NOTIFICATION_ID = 1001;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Auto-backup worker started");

        try {
            BackupManager backupManager = new BackupManager(getApplicationContext());

            // Step 1: Check if signed in to Google Drive
            if (!backupManager.getDriveService().isSignedIn()) {
                Log.w(TAG, "Not signed in to Google Drive, skipping auto-backup");
                return Result.failure();
            }

            // Step 2: Get transactions from your database
            List<Transaction> transactions = getTransactionsFromDatabase();

            if (transactions.isEmpty()) {
                Log.d(TAG, "No transactions to backup");
                return Result.success();
            }

            Log.d(TAG, "Starting auto-backup with " + transactions.size() + " transactions");

            // Step 3: Perform backup (synchronously since we're already in background)
            boolean[] success = {false};
            String[] errorMessage = {null};
            String[] successMessage = {null};

            backupManager.backup(transactions, new BackupManager.BackupCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Auto-backup successful: " + message);
                    success[0] = true;
                    successMessage[0] = message;
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Auto-backup failed: " + error);
                    errorMessage[0] = error;
                }

                @Override
                public void onProgress(String status) {
                    Log.d(TAG, "Auto-backup progress: " + status);
                }
            });

            // Step 4: Wait for backup to complete (with timeout)
            int maxWaitSeconds = 60;
            int waitedSeconds = 0;

            while (waitedSeconds < maxWaitSeconds && !success[0] && errorMessage[0] == null) {
                Thread.sleep(1000);
                waitedSeconds++;
            }

            backupManager.shutdown();

            // Step 5: Show notification and return result
            if (success[0]) {
                showNotification("Backup Successful", successMessage[0]);
                return Result.success();
            } else if (errorMessage[0] != null) {
                showNotification("Backup Failed", errorMessage[0]);
                return Result.retry(); // Retry later
            } else {
                Log.w(TAG, "Auto-backup timeout");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Auto-backup worker error: " + e.getMessage(), e);
            showNotification("Backup Error", e.getMessage());
            return Result.retry();
        }
    }

    private List<Transaction> getTransactionsFromDatabase() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        TransactionDao dao = db.transactionDao();

        return dao.getAllTransactionsSync();
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Backup Notifications";
            String description = "Notifications for automatic backups";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    getApplicationContext().getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
