package com.studentassoc.financialtracker.services;

import android.content.Context;
import android.util.Log;

import com.studentassoc.financialtracker.Model.BackupStatus;
import com.studentassoc.financialtracker.Model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final int KEEP_BACKUP_COUNT = 10;

    private Context context;
    private GoogleDriveService driveService;
    private IncrementalBackupService incrementalService;
    private ExecutorService executorService;

    public interface BackupCallback {
        void onSuccess(String message);

        void onError(String error);

        void onProgress(String status);
    }

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.driveService = new GoogleDriveService(context);
        this.incrementalService = new IncrementalBackupService(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void backup(List<Transaction> transactions, BackupCallback callback) {
        executorService.execute(() -> {
            try {
                if (!driveService.isSignedIn()) {
                    callback.onError("Not signed in to Google Drive");
                    return;
                }

                if (!driveService.isReady()) {
                    callback.onProgress("Reconnecting to Google Drive...");
                    boolean restored = driveService.tryRestoreSignIn();
                    if (!restored) {
                        callback.onError("Session expired. Please sign in again.");
                        return;
                    }
                }

                callback.onProgress("Preparing backup...");

                String backupJson;
                String fileName;

                if (incrementalService.isFirstBackup()) {
                    callback.onProgress("Creating full backup...");
                    backupJson = incrementalService.createFullBackup(transactions);
                    fileName = "full_backup_" + getDateString() + ".json";

                    Log.d(TAG, "Created full backup: " + fileName);
                } else {
                    callback.onProgress("Checking for changes...");
                    backupJson = incrementalService.createIncrementalBackup(transactions);

                    if (backupJson == null) {
                        callback.onSuccess("No changes to backup");
                        return;
                    }
                    fileName = "incremental_" + getDateString() + ".json";
                    Log.d(TAG, "Created incremental backup: " + fileName);
                }

                callback.onProgress("Uploading to Google Drive...");

                String appFolderId = driveService.getOrCreateAppFolder();
                String backupFolderId = driveService.getOrCreateBackupFolder(appFolderId);
                String fileId = driveService.uploadBackup(fileName, backupJson, backupFolderId);

                Log.d(TAG, "Upload successful: " + fileId);

                callback.onProgress("Cleaning up old backups...");
                driveService.deleteOldBackups(backupFolderId, KEEP_BACKUP_COUNT);

                callback.onSuccess("Backup successful: " + fileName);
            } catch (Exception e) {
                Log.e(TAG, "Backup error", e);
                callback.onError("Backup failed: " + e.getLocalizedMessage());
            }
        });
    }

    public BackupStatus getStatus() {
        BackupStatus status = new BackupStatus();

        if (incrementalService != null) {
            status.setLastBackupTime(incrementalService.getLastBackupTime());
            status.setBackupVersion(incrementalService.getCurrentVersion());
        }

        if (driveService != null) {
            status.setSignedIn(driveService.isSignedIn());
        } else {
            status.setSignedIn(false);
        }

        return status;
    }

    public GoogleDriveService getDriveService() {
        return driveService;
    }

    public IncrementalBackupService getIncrementalService() {
        return incrementalService;
    }

    public void resetBackupState() {
        incrementalService.resetBackupState();
        Log.w(TAG, "Backup state reset");
    }

    public void signOut() {
        driveService.signOut();
        Log.d(TAG, "Signed out from Google Drive");
    }

    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.ROOT);
        return sdf.format(new Date());
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "Executor service shutdown");
        }
    }
}
