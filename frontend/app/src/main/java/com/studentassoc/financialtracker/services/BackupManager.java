package com.studentassoc.financialtracker.services;

import android.content.Context;
import android.util.Log;

import com.studentassoc.financialtracker.Model.BackupStatus;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Utils.Utils;

import java.io.IOException;
import java.util.List;
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

    /**
     * Outcome of a backup run. {@code retryable} is true for transient
     * failures (e.g. network IOExceptions) that are worth retrying later;
     * non-retryable failures (e.g. expired sign-in) need user action.
     */
    public static class BackupResult {
        public final boolean success;
        public final boolean retryable;
        public final String message;

        private BackupResult(boolean success, boolean retryable, String message) {
            this.success = success;
            this.retryable = retryable;
            this.message = message;
        }

        static BackupResult success(String message) {
            return new BackupResult(true, false, message);
        }

        static BackupResult failure(String message) {
            return new BackupResult(false, false, message);
        }

        static BackupResult retryableFailure(String message) {
            return new BackupResult(false, true, message);
        }
    }

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.driveService = new GoogleDriveService(context);
        this.incrementalService = new IncrementalBackupService(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Asynchronous backup on the internal single-thread executor.
     * Wraps {@link #performBackup(List, BackupCallback)} — the single
     * implementation of the backup logic shared with {@link #backupBlocking(List)}.
     */
    public void backup(List<Transaction> transactions, BackupCallback callback) {
        executorService.execute(() -> {
            BackupResult result = performBackup(transactions, callback);
            if (result.success) {
                callback.onSuccess(result.message);
            } else {
                callback.onError(result.message);
            }
        });
    }

    /**
     * Synchronous backup that runs inline on the calling thread. Intended for
     * background components such as WorkManager Workers, where an extra
     * executor only adds shutdown races. Shares the exact code path with
     * {@link #backup(List, BackupCallback)} via {@link #performBackup(List, BackupCallback)}.
     */
    public BackupResult backupBlocking(List<Transaction> transactions) {
        return performBackup(transactions, null);
    }

    /**
     * The one implementation of the backup flow. Emits progress through
     * {@code progressListener} (may be null) and returns the outcome instead of
     * reporting terminal success/error itself, so both the callback and the
     * blocking entry points can map it to their own style.
     */
    private BackupResult performBackup(List<Transaction> transactions, BackupCallback progressListener) {
        try {
            if (!driveService.isSignedIn()) {
                return BackupResult.failure("Not signed in to Google Drive");
            }

            if (!driveService.isReady()) {
                reportProgress(progressListener, "Reconnecting to Google Drive...");
                boolean restored = driveService.tryRestoreSignIn();
                if (!restored) {
                    return BackupResult.failure("Session expired. Please sign in again.");
                }
            }

            reportProgress(progressListener, "Preparing backup...");

            String backupJson;
            String fileName;
            int backupVersion;

            if (incrementalService.isFirstBackup()) {
                reportProgress(progressListener, "Creating full backup...");
                backupJson = incrementalService.createFullBackup(transactions);
                fileName = "full_backup_" + Utils.fileTimestamp() + ".json";
                backupVersion = incrementalService.getLastCreatedVersion();

                Log.d(TAG, "Created full backup: " + fileName);
            } else {
                reportProgress(progressListener, "Checking for changes...");
                backupJson = incrementalService.createIncrementalBackup(transactions);

                if (backupJson == null) {
                    return BackupResult.success("No changes to backup");
                }
                fileName = "incremental_" + Utils.fileTimestamp() + ".json";
                backupVersion = incrementalService.getLastCreatedVersion();
                Log.d(TAG, "Created incremental backup: " + fileName);
            }

            reportProgress(progressListener, "Uploading to Google Drive...");

            String appFolderId = driveService.getOrCreateAppFolder();
            String backupFolderId = driveService.getOrCreateBackupFolder(appFolderId);
            String fileId = driveService.uploadBackup(fileName, backupJson, backupFolderId);

            Log.d(TAG, "Upload successful: " + fileId);

            // Only advance the local snapshot after the upload succeeded.
            // If the upload failed (crash, network loss, 401/403), the
            // snapshot stays at its previous state so the next backup
            // still picks up these changes.
            incrementalService.commitSnapshot(
                    transactions,
                    incrementalService.getLastCreatedTimestamp(),
                    backupVersion);

            reportProgress(progressListener, "Cleaning up old backups...");
            driveService.deleteOldBackups(backupFolderId, KEEP_BACKUP_COUNT);

            return BackupResult.success("Backup successful: " + fileName);
        } catch (IOException e) {
            // Transient (network/drive) failure — safe to retry later.
            Log.e(TAG, "Backup error (retryable)", e);
            return BackupResult.retryableFailure("Backup failed: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Log.e(TAG, "Backup error", e);
            return BackupResult.failure("Backup failed: " + e.getLocalizedMessage());
        }
    }

    private static void reportProgress(BackupCallback progressListener, String status) {
        if (progressListener != null) {
            progressListener.onProgress(status);
        }
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

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "Executor service shutdown");
        }
    }
}
