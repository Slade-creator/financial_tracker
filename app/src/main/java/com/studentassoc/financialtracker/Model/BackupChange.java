package com.studentassoc.financialtracker.Model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BackupChange {

    private List<Transaction> added;
    private List<Transaction> modified;
    private List<String> deleted;

    public BackupChange() {
        this.added = new ArrayList<>();
        this.modified = new ArrayList<>();
        this.deleted = new ArrayList<>();
    }

    public List<Transaction> getAdded() {
        return added;
    }

    public void setAdded(List<Transaction> added) {
        this.added = added;
    }

    public List<Transaction> getModified() {
        return modified;
    }

    public void setModified(List<Transaction> modified) {
        this.modified = modified;
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<String> deleted) {
        this.deleted = deleted;
    }

    public boolean isEmpty() {
        return added.isEmpty() && modified.isEmpty() && deleted.isEmpty();
    }

    public int getTotalChangeCount() {
        return added.size() + modified.size() + deleted.size();
    }

    public void addTransaction(Transaction transaction) {
        this.added.add(transaction);
    }

    public void modifyTransaction(Transaction transaction) {
        this.modified.add(transaction);
    }

    public void deleteTransaction(String transactionId) {
        this.deleted.add(transactionId);
    }

    public void clear() {
        added.clear();
        modified.clear();
        deleted.clear();
    }

    @NonNull
    @Override
    public String toString() {
        return "BackupChange{" +
                "added=" + added.size() +
                ", modified=" + modified.size() +
                ", deleted=" + deleted.size() +
                ", total=" + getTotalChangeCount() +
                '}';
    }
}
