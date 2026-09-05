package com.studentassoc.financialtracker.services;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.studentassoc.financialtracker.Model.BackupChange;
import com.studentassoc.financialtracker.Model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestoreService {
    private static final String TAG = "RestoreService";

    private Context context;
    private GoogleDriveService driveService;
    private final Gson gson;


    public interface RestoreCallback {
        void onSuccess(List<Transaction> transactions, String message);
        void onError(String error);
        void onProgress(String status);
    }

    public RestoreService(Context context, GoogleDriveService driveService) {
        this.context = context;
        this.driveService = driveService;
        this.gson = new Gson();
    }

    public void restore(RestoreCallback callback) {
        new Thread( () -> {
            try {
                if (!driveService.isReady()) {
                    callback.onError("Not connected to Google Drive");
                    return;
                }

                callback.onProgress("Fetching backup list...");

                String appFolderId = driveService.getOrCreateAppFolder();
                String backupFolderId = driveService.getOrCreateBackupFolder(appFolderId);

                List<File> backupFiles = driveService.listBackups(backupFolderId);

                if (backupFiles == null || backupFiles.isEmpty()) {
                    callback.onError("No backups found");
                    return;
                }

                callback.onProgress("Downloading backups...");
                Log.d(TAG, "Found " + backupFiles.size() + " backup files");

                // Download and parse backups
                List<BackupData> backups = new ArrayList<>();
                for (File file : backupFiles) {
                    try {
                        String content = driveService.downloadBackup(file.getId());

                        if (content == null || content.isEmpty()) {
                            Log.w(TAG, "Skipping empty file: " + file.getName());
                            continue;
                        }
                        BackupData backup = parseBackup(file.getName(), content);
                        if (backup != null) {
                            backups.add(backup);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to download/parse: " + file.getName(), e);
                    }
                }

                callback.onProgress("Merging backups...");

                List<Transaction> transactions = mergeBackups(backups);

                callback.onSuccess(transactions,
                        "Restored " + transactions.size() + " transactions from " + backups.size() + " backups");
            } catch (Exception e) {
                Log.e(TAG, "Restore error: " + e.getMessage(), e);
                callback.onError("Restore failed: " + e.getMessage());
            }
        }).start();
    }

    private BackupLogic.BackupPayload parseBackup(String fileName, String json) {
        try {
            Log.d(TAG, "Parsing '" + fileName + "' — first 300 chars: "
                    + json.substring(0, Math.min(300, json.length())));

            JsonObject obj = gson.fromJson(json, JsonObject.class);

            if (obj == null) {
                Log.e(TAG, "gson returned null for: " + fileName + " — JSON may be malformed");
            } else {
                Log.d(TAG, "Keys in '" + fileName + "': " + obj.keySet());
            }

            BackupLogic.BackupPayload data =
                    BackupLogic.parseBackupJson(fileName, json, gson);

            if (data == null) {
                Log.e(TAG, "Could not parse backup (missing metadata/transactions/changes "
                        + "or malformed JSON): " + fileName);
                return null;
            }

            if (data.metadata.isFullBackup()) {
                Log.d(TAG, "Parsed full backup '" + fileName + "' — "
                        + data.transactions.size() + " transactions");
            } else {
                Log.d(TAG, "Parsed incremental backup: " + fileName);
            }

            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing backup: " + e.getMessage());
            return null;
        }
    }

    private List<Transaction> mergeBackups(List<BackupLogic.BackupPayload> backups) {
        List<Transaction> merged = BackupLogic.mergeBackups(backups);

        if (merged.isEmpty()) {
            Log.e(TAG, "No full backup found");
        }

        Log.d(TAG, "After merging: " + merged.size() + " transactions");
        return merged;
    }

    private void applyChanges(Map<String, Transaction> map, BackupChange changes) {

        for (Transaction tx : changes.getAdded()) {
            map.put(tx.getId(), tx);
        }

        for (Transaction tx : changes.getModified()) {
            map.put(tx.getId(), tx);
        }

        for (String id : changes.getDeleted()) {
            map.remove(id);
        }

        Log.d(TAG, "Applied changes: +" + changes.getAdded().size() +
                " ~" + changes.getModified().size() +
                " -" + changes.getDeleted().size());
    }

}

