package com.studentassoc.financialtracker.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.studentassoc.financialtracker.Model.BackupChange;
import com.studentassoc.financialtracker.Model.BackupMetadata;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Utils.Utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IncrementalBackupService {

    private static final String TAG = "IncrementalBackup";
    private static final String PREFS_NAME = "BackupPrefs";
    private static final String KEY_LAST_BACKUP_TIME = "last_backup_timestamp";
    private static final String KEY_BACKUP_VERSION = "backup_version";
    private static final String KEY_TRANSACTION_SNAPSHOTS = "transaction_snapshots";
    private static final String KEY_DEVICE_ID = "device_id";

    private Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Timestamp/version of the most recently built backup payload. These are
    // only persisted via commitSnapshot() after the payload has been
    // successfully uploaded.
    private String lastCreatedTimestamp;
    private int lastCreatedVersion;

    public IncrementalBackupService(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public BackupChange detectChanges(List<Transaction> currentTransactions) {
        Log.d(TAG, "Detecting changes in " + currentTransactions.size() + " transactions");

        BackupChange changes = BackupLogic.detectChanges(currentTransactions, getLastSnapshot());

        Log.d(TAG, "Change detection complete: " + changes);
        return changes;
    }

    public String createFullBackup(List<Transaction> transactions) {
        Log.d(TAG, "Creating full backup with " + transactions.size() + " transactions");

        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupType("FULL");
        metadata.setTimestamp(Utils.getCurrentTimestamp());
        metadata.setDeviceId(getDeviceId());
        metadata.setVersion(1);
        metadata.setTransactionCount(transactions.size());
        metadata.setLastBackupTimestamp(Utils.getCurrentTimestamp());

        Map<String, Object> backup = new HashMap<>();
        backup.put("metadata", metadata);
        backup.put("transactions", transactions);

        lastCreatedTimestamp = metadata.getTimestamp();
        lastCreatedVersion = 1;

        String json = gson.toJson(backup);
        Log.d(TAG, "Full backup created: " + json.length() + " bytes");

        return json;
    }

    public String createIncrementalBackup(List<Transaction> currentTransactions) {
        Log.d(TAG, "Creating incremental backup");

        BackupChange changes = detectChanges(currentTransactions);

        if (changes.isEmpty()) {
            Log.d(TAG, "No changes detected, skipping backup");
            return null;
        }

        int currentVersion = prefs.getInt(KEY_BACKUP_VERSION, 0);
        int newVersion = currentVersion + 1;

        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupType("INCREMENTAL");
        metadata.setTimestamp(Utils.getCurrentTimestamp());
        metadata.setDeviceId(getDeviceId());
        metadata.setVersion(newVersion);
        metadata.setBaseVersion(currentVersion);
        metadata.setTransactionCount(changes.getTotalChangeCount());
        metadata.setLastBackupTimestamp(Utils.getCurrentTimestamp());

        Map<String, Object> backup = new HashMap<>();
        backup.put("metadata", metadata);
        backup.put("changes", changes);

        lastCreatedTimestamp = metadata.getTimestamp();
        lastCreatedVersion = newVersion;

        String json = gson.toJson(backup);
        Log.d(TAG, "Incremental backup created: " + json.length() + " bytes, version " + newVersion);

        return json;
    }

    /**
     * Persists the snapshot state for a backup payload that has already been
     * successfully uploaded to Drive. Must NOT be called before the upload
     * succeeds — otherwise a failed upload would leave the snapshot advanced
     * and the next incremental backup would silently skip those changes.
     */
    public void commitSnapshot(List<Transaction> current, String timestamp, int version) {
        Log.d(TAG, "Committing snapshot after successful upload (version " + version + ")");
        saveSnapshot(current);
        prefs.edit()
                .putString(KEY_LAST_BACKUP_TIME, timestamp)
                .putInt(KEY_BACKUP_VERSION, version)
                .apply();
    }

    public String getLastCreatedTimestamp() {
        return lastCreatedTimestamp;
    }

    public int getLastCreatedVersion() {
        return lastCreatedVersion;
    }

    private void saveSnapshot(List<Transaction> transactions) {
        Log.d(TAG, "Saving snapshot of " + transactions.size() + " transactions");

        Map<String, Transaction> snapshot = new HashMap<>();
        for (Transaction tx : transactions) {
            snapshot.put(tx.getId(), tx);
        }

        String snapshotJson = gson.toJson(snapshot);
        prefs.edit()
                .putString(KEY_TRANSACTION_SNAPSHOTS, snapshotJson)
                .apply();

        Log.d(TAG, "Snapshot saved: " + snapshotJson.length() + " bytes");
    }

    private Map<String, Transaction> getLastSnapshot() {
        String snapshotJson = prefs.getString(KEY_TRANSACTION_SNAPSHOTS, "{}");

        try {
            Type type = new TypeToken<Map<String, Transaction>>(){}.getType();
            Map<String, Transaction> snapshot = gson.fromJson(snapshotJson, type);

            if (snapshot == null) {
                snapshot = new HashMap<>();
            }

            Log.d(TAG, "Retrieved snapshot with " + snapshot.size() + " transactions");
            return snapshot;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing snapshot: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public boolean isFirstBackup() {
        boolean isFirst = !prefs.contains(KEY_BACKUP_VERSION);
        Log.d(TAG, "Is first backup: " + isFirst);
        return isFirst;
    }

    public String getLastBackupTime() {
        return prefs.getString(KEY_LAST_BACKUP_TIME, null);
    }

    public int getCurrentVersion() {
        return prefs.getInt(KEY_BACKUP_VERSION, 0);
    }

    public void resetBackupState() {
        Log.w(TAG, "Resetting backup state - next backup will be FULL");
        prefs.edit()
                .remove(KEY_LAST_BACKUP_TIME)
                .remove(KEY_BACKUP_VERSION)
                .remove(KEY_TRANSACTION_SNAPSHOTS)
                .apply();
    }

    private String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
            Log.d(TAG, "Created new device ID: " + deviceId);
        }
        return deviceId;
    }

    public Map<String, Object> getBackupStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("version", getCurrentVersion());
        stats.put("lastBackupTime", getLastBackupTime());
        stats.put("isFirstBackup", isFirstBackup());
        stats.put("deviceId", getDeviceId());

        Map<String, Transaction> snapshot = getLastSnapshot();
        stats.put("snapshotSize", snapshot.size());

        return stats;
    }
}
