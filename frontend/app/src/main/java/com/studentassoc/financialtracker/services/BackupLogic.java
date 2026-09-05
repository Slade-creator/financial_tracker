package com.studentassoc.financialtracker.services;

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

/**
 * Pure (Android-free) backup diff and merge logic, extracted from
 * {@link IncrementalBackupService} and {@link RestoreService} so it can be
 * unit-tested on the plain JVM without an Android Context.
 *
 * The Android wrappers own logging, SharedPreferences and Drive I/O; this class
 * contains only the deterministic data transformations.
 */
public final class BackupLogic {

    private BackupLogic() {
        // static utility class
    }

    /**
     * In-memory payload for one backup file: a FULL backup's transaction list
     * or an INCREMENTAL backup's change set. Mirrors the parsed JSON layout of
     * a backup document.
     */
    public static class BackupPayload {
        public BackupMetadata metadata;
        public List<Transaction> transactions;
        public BackupChange changes;

        public BackupPayload() {
        }

        public BackupPayload(BackupMetadata metadata,
                             List<Transaction> transactions,
                             BackupChange changes) {
            this.metadata = metadata;
            this.transactions = transactions;
            this.changes = changes;
        }
    }

    /**
     * Computes the difference between the current transaction list and the
     * last backed-up snapshot.
     *
     * @param currentTransactions transactions currently in the local database
     * @param lastSnapshot        id -> transaction map captured at the last
     *                            successful backup (may be null or empty)
     * @return added / modified / deleted changes, keyed off updatedAt equality
     */
    public static BackupChange detectChanges(List<Transaction> currentTransactions,
                                             Map<String, Transaction> lastSnapshot) {
        BackupChange changes = new BackupChange();
        if (lastSnapshot == null) {
            lastSnapshot = new HashMap<>();
        }

        Map<String, Transaction> currentMap = new HashMap<>();
        for (Transaction tx : currentTransactions) {
            currentMap.put(tx.getId(), tx);
        }

        for (Transaction current : currentTransactions) {
            Transaction last = lastSnapshot.get(current.getId());

            if (last == null) {
                changes.addTransaction(current);
            } else if (!current.getUpdatedAt().equals(last.getUpdatedAt())) {
                changes.modifyTransaction(current);
            }
        }

        for (String id : lastSnapshot.keySet()) {
            if (!currentMap.containsKey(id)) {
                changes.deleteTransaction(id);
            }
        }

        return changes;
    }

    /**
     * Rebuilds the current transaction list from a FULL backup plus any
     * INCREMENTAL backups. Incremental change sets are applied in reverse list
     * order (newest-first, as produced by the backup listing).
     *
     * @param backups parsed backup payloads (full and/or incremental)
     * @return merged transaction list; empty if no FULL backup is present
     */
    public static List<Transaction> mergeBackups(List<BackupPayload> backups) {
        BackupPayload fullBackup = null;

        for (BackupPayload backup : backups) {
            if (backup.metadata != null && backup.metadata.isFullBackup()) {
                fullBackup = backup;
                break;
            }
        }

        if (fullBackup == null || fullBackup.transactions == null) {
            return new ArrayList<>();
        }

        Map<String, Transaction> transactionMap = new HashMap<>();
        for (Transaction tx : fullBackup.transactions) {
            transactionMap.put(tx.getId(), tx);
        }

        for (int i = backups.size() - 1; i >= 0; i--) {
            BackupPayload backup = backups.get(i);
            if (backup.metadata != null
                    && backup.metadata.isIncrementalBackup()
                    && backup.changes != null) {
                applyChanges(transactionMap, backup.changes);
            }
        }

        return new ArrayList<>(transactionMap.values());
    }

    private static void applyChanges(Map<String, Transaction> map, BackupChange changes) {
        for (Transaction tx : changes.getAdded()) {
            map.put(tx.getId(), tx);
        }

        for (Transaction tx : changes.getModified()) {
            map.put(tx.getId(), tx);
        }

        for (String id : changes.getDeleted()) {
            map.remove(id);
        }
    }

    /**
     * Parses the JSON of one backup file (full or incremental) into a
     * {@link BackupPayload}.
     *
     * Key compatibility: backups created before the full-backup key typo was
     * fixed (MNT-002) stored FULL-backup metadata under "metadate" and the
     * transaction list under "transaction" (singular). New backups use
     * "metadata"/"transactions"; INCREMENTAL backups have always used
     * "metadata"/"changes". The legacy fallbacks below must be kept so old
     * backups already stored on Drive remain restorable.
     *
     * @param fileName backup file name (for diagnostics only)
     * @param json     raw backup file content
     * @param gson     Gson instance used for parsing
     * @return the parsed payload, or null if the JSON is malformed or the
     *         required fields are missing
     */
    public static BackupPayload parseBackupJson(String fileName, String json, Gson gson) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);

            if (obj == null) {
                return null;
            }

            JsonElement metaElement = obj.get("metadata");

            // Legacy format compat: full backups created before the MNT-002
            // key fix stored metadata under the typo'd key "metadate".
            if (metaElement == null || metaElement.isJsonNull()) {
                metaElement = obj.get("metadate");
            }

            if (metaElement == null || metaElement.isJsonNull()) {
                return null;
            }

            BackupMetadata metadata = gson.fromJson(metaElement, BackupMetadata.class);

            BackupPayload data = new BackupPayload();
            data.metadata = metadata;

            if (metadata.isFullBackup()) {
                JsonElement txElement = obj.get("transactions");
                // Legacy format compat: full backups created before the
                // MNT-002 key fix stored the transaction list under the
                // typo'd singular key "transaction".
                if (txElement == null || txElement.isJsonNull()) {
                    txElement = obj.get("transaction");
                }
                if (txElement == null || txElement.isJsonNull()) {
                    return null;
                }
                Type listType = new TypeToken<List<Transaction>>(){}.getType();
                data.transactions = gson.fromJson(txElement, listType);

                if (data.transactions == null) {
                    return null;
                }
            } else {
                JsonElement changesElement = obj.get("changes");
                if (changesElement == null || changesElement.isJsonNull()) {
                    return null;
                }
                data.changes = gson.fromJson(changesElement, BackupChange.class);
            }

            return data;
        } catch (Exception e) {
            return null;
        }
    }
}
