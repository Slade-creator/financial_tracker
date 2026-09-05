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

        BackupManager backupManager = new BackupManager(getApplicationContext());

        try {
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

            // Step 3: Perform backup synchronously on this worker thread.
            // No executor, no polling loop, no timeout race with shutdown().
            BackupManager.BackupResult result = backupManager.backupBlocking(transactions);

            // Step 4: Show notification and return result
            if (result.success) {
                Log.d(TAG, "Auto-backup successful: " + result.message);
                showNotification("Backup Successful", result.message);
                return Result.success();
            }

            Log.e(TAG, "Auto-backup failed: " + result.message);
            showNotification("Backup Failed", result.message);
            // Retry only transient failures (e.g. network IOExceptions);
            // non-retryable failures (e.g. expired sign-in) need user action.
            return result.retryable ? Result.retry() : Result.failure();

        } catch (Exception e) {
            Log.e(TAG, "Auto-backup worker error: " + e.getMessage(), e);
            showNotification("Backup Error", e.getMessage());
            return Result.retry();
        } finally {
            backupManager.shutdown();
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
