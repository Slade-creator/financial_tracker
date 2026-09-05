package com.studentassoc.financialtracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.studentassoc.financialtracker.Model.BackupChange;
import com.studentassoc.financialtracker.Model.BackupMetadata;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.services.BackupLogic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain JVM tests for {@link BackupLogic} — the pure diff/merge logic extracted
 * from IncrementalBackupService and RestoreService. No Android dependencies.
 */
public class BackupLogicTest {

    private static Transaction txn(String id, String updatedAt) {
        return new Transaction(
                id, "EXPENSE", 1000, null, "Stationery", "CASH", 1,
                "2026-03-15T10:00:00Z", null,
                "2026-03-15T09:00:00Z", updatedAt);
    }

    private static BackupMetadata metadata(String type, int version) {
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupType(type);
        metadata.setVersion(version);
        return metadata;
    }

    // ------------------------------------------------------------------
    // detectChanges
    // ------------------------------------------------------------------

    @Test
    public void detectChanges_newTransactionIsAdded() {
        Transaction a = txn("a", "2026-03-15T09:00:00Z");

        BackupChange changes = BackupLogic.detectChanges(
                Collections.singletonList(a), new HashMap<>());

        assertEquals(1, changes.getAdded().size());
        assertEquals("a", changes.getAdded().get(0).getId());
        assertTrue(changes.getModified().isEmpty());
        assertTrue(changes.getDeleted().isEmpty());
    }

    @Test
    public void detectChanges_updatedAtChangeIsModification() {
        Transaction current = txn("a", "2026-03-15T11:00:00Z");
        Map<String, Transaction> snapshot = new HashMap<>();
        snapshot.put("a", txn("a", "2026-03-15T09:00:00Z"));

        BackupChange changes = BackupLogic.detectChanges(
                Collections.singletonList(current), snapshot);

        assertEquals(1, changes.getModified().size());
        assertEquals("a", changes.getModified().get(0).getId());
        assertTrue(changes.getAdded().isEmpty());
        assertTrue(changes.getDeleted().isEmpty());
    }

    @Test
    public void detectChanges_unchangedTransactionsProduceNoChanges() {
        Transaction current = txn("a", "2026-03-15T09:00:00Z");
        Map<String, Transaction> snapshot = new HashMap<>();
        snapshot.put("a", txn("a", "2026-03-15T09:00:00Z"));

        BackupChange changes = BackupLogic.detectChanges(
                Collections.singletonList(current), snapshot);

        assertTrue(changes.isEmpty());
        assertEquals(0, changes.getTotalChangeCount());
    }

    @Test
    public void detectChanges_removedTransactionIsDeletion() {
        Map<String, Transaction> snapshot = new HashMap<>();
        snapshot.put("a", txn("a", "2026-03-15T09:00:00Z"));
        snapshot.put("b", txn("b", "2026-03-15T09:00:00Z"));

        BackupChange changes = BackupLogic.detectChanges(
                Collections.singletonList(txn("a", "2026-03-15T09:00:00Z")), snapshot);

        assertTrue(changes.getDeleted().contains("b"));
        assertEquals(1, changes.getDeleted().size());
    }

    @Test
    public void detectChanges_mixedChanges() {
        Map<String, Transaction> snapshot = new HashMap<>();
        snapshot.put("kept", txn("kept", "2026-03-15T09:00:00Z"));
        snapshot.put("removed", txn("removed", "2026-03-15T09:00:00Z"));
        // "edited" exists in the snapshot with an older updatedAt, so the
        // current copy counts as a modification rather than an addition.
        snapshot.put("edited", txn("edited", "2026-03-15T09:00:00Z"));

        List<Transaction> current = Arrays.asList(
                txn("kept", "2026-03-15T09:00:00Z"),
                txn("edited", "2026-03-15T12:00:00Z"),
                txn("brand-new", "2026-03-15T12:00:00Z"));

        BackupChange changes = BackupLogic.detectChanges(current, snapshot);

        assertEquals(1, changes.getAdded().size());
        assertEquals("brand-new", changes.getAdded().get(0).getId());
        assertEquals(1, changes.getModified().size());
        assertEquals("edited", changes.getModified().get(0).getId());
        assertEquals(Collections.singletonList("removed"), changes.getDeleted());
        assertEquals(3, changes.getTotalChangeCount());
    }

    @Test
    public void detectChanges_nullSnapshotTreatedAsEmpty() {
        BackupChange changes = BackupLogic.detectChanges(
                Collections.singletonList(txn("a", "2026-03-15T09:00:00Z")), null);

        assertEquals(1, changes.getAdded().size());
    }

    @Test
    public void detectChanges_emptyCurrentListDeletesEverything() {
        Map<String, Transaction> snapshot = new HashMap<>();
        snapshot.put("a", txn("a", "2026-03-15T09:00:00Z"));

        BackupChange changes = BackupLogic.detectChanges(
                new ArrayList<Transaction>(), snapshot);

        assertEquals(1, changes.getDeleted().size());
        assertTrue(changes.getAdded().isEmpty());
    }

    // ------------------------------------------------------------------
    // mergeBackups
    // ------------------------------------------------------------------

    @Test
    public void mergeBackups_fullBackupOnly() {
        BackupLogic.BackupPayload full = new BackupLogic.BackupPayload(
                metadata("FULL", 1),
                Arrays.asList(txn("a", "t1"), txn("b", "t1")), null);

        List<Transaction> merged = BackupLogic.mergeBackups(
                Collections.singletonList(full));

        assertEquals(2, merged.size());
    }

    @Test
    public void mergeBackups_appliesIncrementalAddsAndModifications() {
        BackupLogic.BackupPayload full = new BackupLogic.BackupPayload(
                metadata("FULL", 1), Collections.singletonList(txn("a", "t1")), null);

        BackupChange changes = new BackupChange();
        changes.modifyTransaction(txn("a", "t2"));
        changes.addTransaction(txn("c", "t2"));
        BackupLogic.BackupPayload incremental = new BackupLogic.BackupPayload(
                metadata("INCREMENTAL", 2), null, changes);

        List<Transaction> merged = BackupLogic.mergeBackups(Arrays.asList(full, incremental));

        Map<String, Transaction> byId = new HashMap<>();
        for (Transaction t : merged) {
            byId.put(t.getId(), t);
        }
        assertEquals(2, merged.size());
        assertEquals("t2", byId.get("a").getUpdatedAt());
        assertNotNull(byId.get("c"));
    }

    @Test
    public void mergeBackups_appliesIncrementalDeletions() {
        BackupLogic.BackupPayload full = new BackupLogic.BackupPayload(
                metadata("FULL", 1), Arrays.asList(txn("a", "t1"), txn("b", "t1")), null);

        BackupChange changes = new BackupChange();
        changes.deleteTransaction("b");
        BackupLogic.BackupPayload incremental = new BackupLogic.BackupPayload(
                metadata("INCREMENTAL", 2), null, changes);

        List<Transaction> merged = BackupLogic.mergeBackups(Arrays.asList(full, incremental));

        assertEquals(1, merged.size());
        assertEquals("a", merged.get(0).getId());
    }

    @Test
    public void mergeBackups_newerIncrementalWins() {
        // Incrementals are listed newest-first from Drive
        // (createdTime desc — see GoogleDriveService.listBackups), so
        // mergeBackups applies them in reverse list order: the newest
        // incremental (first in the list) is applied last and must win.
        BackupLogic.BackupPayload full = new BackupLogic.BackupPayload(
                metadata("FULL", 1), Collections.singletonList(txn("a", "t1")), null);

        BackupChange older = new BackupChange();
        older.modifyTransaction(txn("a", "t2"));
        BackupChange newer = new BackupChange();
        newer.modifyTransaction(txn("a", "t3"));

        List<Transaction> merged = BackupLogic.mergeBackups(Arrays.asList(
                new BackupLogic.BackupPayload(metadata("INCREMENTAL", 3), null, newer),
                new BackupLogic.BackupPayload(metadata("INCREMENTAL", 2), null, older),
                full));

        assertEquals(1, merged.size());
        assertEquals("t3", merged.get(0).getUpdatedAt());
    }

    @Test
    public void mergeBackups_withoutFullBackupReturnsEmptyList() {
        BackupChange changes = new BackupChange();
        changes.addTransaction(txn("a", "t1"));

        List<Transaction> merged = BackupLogic.mergeBackups(Collections.singletonList(
                new BackupLogic.BackupPayload(metadata("INCREMENTAL", 1), null, changes)));

        assertTrue(merged.isEmpty());
    }

    @Test
    public void mergeBackups_incrementalWithoutChangesIsIgnored() {
        BackupLogic.BackupPayload full = new BackupLogic.BackupPayload(
                metadata("FULL", 1), Collections.singletonList(txn("a", "t1")), null);
        BackupLogic.BackupPayload broken = new BackupLogic.BackupPayload(
                metadata("INCREMENTAL", 2), null, null);

        List<Transaction> merged = BackupLogic.mergeBackups(Arrays.asList(full, broken));

        assertEquals(1, merged.size());
    }

    // ------------------------------------------------------------------
    // Backup JSON parsing (BackupLogic.parseBackupJson)
    // ------------------------------------------------------------------

    private static String fullBackupJson(String metadataKey, String transactionsKey) {
        Map<String, Object> backup = new HashMap<>();
        backup.put(metadataKey, metadata("FULL", 1));
        backup.put(transactionsKey, Arrays.asList(txn("a", "t1"), txn("b", "t1")));
        return new Gson().toJson(backup);
    }

    @Test
    public void parseBackupJson_newFormatFullBackup() {
        String json = fullBackupJson("metadata", "transactions");

        BackupLogic.BackupPayload parsed =
                BackupLogic.parseBackupJson("full.json", json, new Gson());

        assertNotNull(parsed);
        assertEquals("FULL", parsed.metadata.getBackupType());
        assertEquals(2, parsed.transactions.size());
        assertEquals("a", parsed.transactions.get(0).getId());
    }

    @Test
    public void parseBackupJson_legacyFullBackupWithTypoKeysIsStillRestorable() {
        // Backups already on Drive from before the MNT-002 key fix used the
        // typo'd keys "metadate"/"transaction" (singular). They must still parse.
        String json = fullBackupJson("metadate", "transaction");

        BackupLogic.BackupPayload parsed =
                BackupLogic.parseBackupJson("legacy-full.json", json, new Gson());

        assertNotNull(parsed);
        assertEquals("FULL", parsed.metadata.getBackupType());
        assertEquals(2, parsed.transactions.size());
        assertEquals("b", parsed.transactions.get(1).getId());
    }

    @Test
    public void parseBackupJson_incrementalBackup() {
        BackupChange changes = new BackupChange();
        changes.addTransaction(txn("a", "t2"));

        Map<String, Object> backup = new HashMap<>();
        backup.put("metadata", metadata("INCREMENTAL", 2));
        backup.put("changes", changes);
        String json = new Gson().toJson(backup);

        BackupLogic.BackupPayload parsed =
                BackupLogic.parseBackupJson("incr.json", json, new Gson());

        assertNotNull(parsed);
        assertTrue(parsed.metadata.isIncrementalBackup());
        assertEquals(1, parsed.changes.getAdded().size());
    }

    @Test
    public void parseBackupJson_malformedJsonReturnsNull() {
        assertNull(BackupLogic.parseBackupJson("bad.json", "not-json", new Gson()));
    }

    @Test
    public void parseBackupJson_missingMetadataReturnsNull() {
        Map<String, Object> backup = new HashMap<>();
        backup.put("transactions", Collections.singletonList(txn("a", "t1")));

        assertNull(BackupLogic.parseBackupJson(
                "nometa.json", new Gson().toJson(backup), new Gson()));
    }

    @Test
    public void parseBackupJson_legacyFullBackupMergesWithNewIncrementals() {
        // End-to-end: a legacy-format FULL backup plus a new-format incremental
        // must merge back into the full transaction list.
        BackupLogic.BackupPayload legacyFull = BackupLogic.parseBackupJson(
                "legacy-full.json",
                fullBackupJson("metadate", "transaction"),
                new Gson());

        BackupChange changes = new BackupChange();
        changes.modifyTransaction(txn("a", "t2"));
        BackupLogic.BackupPayload incremental = new BackupLogic.BackupPayload(
                metadata("INCREMENTAL", 2), null, changes);

        List<Transaction> merged = BackupLogic.mergeBackups(
                Arrays.asList(legacyFull, incremental));

        Map<String, Transaction> byId = new HashMap<>();
        for (Transaction t : merged) {
            byId.put(t.getId(), t);
        }
        assertEquals(2, merged.size());
        assertEquals("t2", byId.get("a").getUpdatedAt());
    }
}
