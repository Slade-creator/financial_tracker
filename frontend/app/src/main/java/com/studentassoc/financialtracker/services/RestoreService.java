package com.studentassoc.financialtracker.services;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.studentassoc.financialtracker.Model.BackupChange;
import com.studentassoc.financialtracker.Model.BackupMetadata;
import com.studentassoc.financialtracker.Model.Transaction;

import java.lang.reflect.Type;
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

    private BackupData parseBackup(String fileName, String json) {
        try {
            Log.d(TAG, "Parsing '" + fileName + "' — first 300 chars: "
                    + json.substring(0, Math.min(300, json.length())));

            JsonObject obj = gson.fromJson(json, JsonObject.class);

            if (obj == null) {
                Log.e(TAG, "gson returned null for: " + fileName + " — JSON may be malformed");
                return null;
            }

            Log.d(TAG, "Keys in '" + fileName + "': " + obj.keySet());

            JsonElement metaElement = obj.get("metadata");

            if (metaElement == null || metaElement.isJsonNull()) {
                metaElement = obj.get("metadate");
            }

            if (metaElement == null || metaElement.isJsonNull()) {
                Log.e(TAG, "No metadata/metadate field in: " + fileName);
                return null;
            }

            BackupMetadata metadata = gson.fromJson(
                    metaElement, BackupMetadata.class);

            BackupData data = new BackupData();
            data.metadata = metadata;

            if (metadata.isFullBackup()) {
                JsonElement txElement = obj.get("transactions");
                if (txElement == null || txElement.isJsonNull()) {
                    txElement = obj.get("transaction");
                }
                if (txElement == null || txElement.isJsonNull()) {
                    Log.e(TAG, "No transactions/transaction field in full backup: " + fileName);
                    return null;
                }
                Type listType = new TypeToken<List<Transaction>>(){}.getType();
                data.transactions = gson.fromJson(txElement, listType);

                if (data.transactions == null) {
                    Log.e(TAG, "Transaction list is null after parsing: " + fileName);
                    return null;
                }
                Log.d(TAG, "Parsed full backup '" + fileName + "' — "
                        + data.transactions.size() + " transactions");
            } else {
                JsonElement changesElement = obj.get("changes");
                if (changesElement == null || changesElement.isJsonNull()) {
                    Log.e(TAG, "No changes field in incremental backup: " + fileName);
                    return null;
                }
                data.changes = gson.fromJson(changesElement, BackupChange.class);
                Log.d(TAG, "Parsed incremental backup: " + fileName);
            }

            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing backup: " + e.getMessage());
            return null;
        }
    }

    private List<Transaction> mergeBackups(List<BackupData> backups) {
        BackupData fullBackup = null;

        for (BackupData backup : backups) {
            if (backup.metadata.isFullBackup()) {
                fullBackup = backup;
                break;
            }
        }

        if (fullBackup == null) {
            Log.e(TAG, "No full backup found");
            return new ArrayList<>();
        }

        Map<String, Transaction> transactionMap = new HashMap<>();

        for (Transaction tx : fullBackup.transactions) {
            transactionMap.put(tx.getId(), tx);
        }

        Log.d(TAG, "Starting with " + transactionMap.size() + " transactions from full backup");

        for (int i = backups.size() - 1; i >= 0; i--) {
            BackupData backup = backups.get(i);
            if (backup.metadata != null
                    && backup.metadata.isIncrementalBackup()
                    && backup.changes != null) {
                applyChanges(transactionMap, backup.changes);
            }
        }

        Log.d(TAG, "After merging: " + transactionMap.size() + " transactions");

        return new ArrayList<>(transactionMap.values());
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

    private static class BackupData {
        BackupMetadata metadata;
        List<Transaction> transactions;
        BackupChange changes;
    }

}

