package com.studentassoc.financialtracker.Model;

import androidx.annotation.NonNull;

import com.studentassoc.financialtracker.Utils.Utils;

public class BackupStatus {

    private String lastBackupTime;
    private int backupVersion;
    private boolean isSignedIn;
    private boolean autoBackupEnabled;
    private int totalBackups;
    private String lastError;

    public BackupStatus() {
        this.backupVersion = 0;
        this.isSignedIn = false;
        this.autoBackupEnabled = false;
        this.totalBackups = 0;
    }

    public String getLastBackupTime() {
        return lastBackupTime;
    }

    public void setLastBackupTime(String lastBackupTime) {
        this.lastBackupTime = lastBackupTime;
    }

    public int getBackupVersion() {
        return backupVersion;
    }

    public void setBackupVersion(int backupVersion) {
        this.backupVersion = backupVersion;
    }
    public boolean isSignedIn() {
        return isSignedIn;
    }

    public void setSignedIn(boolean signedIn) {
        isSignedIn = signedIn;
    }

    public boolean isAutoBackupEnabled() {
        return autoBackupEnabled;
    }

    public void setAutoBackupEnabled(boolean autoBackupEnabled) {
        this.autoBackupEnabled = autoBackupEnabled;
    }

    public int getTotalBackups() {
        return totalBackups;
    }

    public void setTotalBackups(int totalBackups) {
        this.totalBackups = totalBackups;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean hasBackup() {
        return backupVersion > 0 && lastBackupTime != null;
    }

    public boolean isReadyToBackup() {
        return isSignedIn && lastError == null;
    }

    public String getStatusMessage() {
        if (!isSignedIn) {
            return "Not signed in to Google Drive";
        }

        if (lastError != null) {
            return "Last backup failed: " + lastError;
        }

        if (!hasBackup()) {
            return "No backups yet. Tap 'Backup Now' to create your first backup.";
        }

        return "Last backup: " + formatTime(lastBackupTime) + " (Version " + backupVersion + ")";
    }

    /**
     * Format timestamp for display
     * @param isoTime ISO 8601 timestamp
     * @return Formatted time string
     */
    private String formatTime(String isoTime) {
        if (isoTime == null) return "Never";

        try {
            return Utils.formatDateForDisplay(isoTime);
        } catch (Exception e) {
            return isoTime;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "BackupStatus{" +
                "lastBackupTime='" + lastBackupTime + '\'' +
                ", backupVersion=" + backupVersion +
                ", isSignedIn=" + isSignedIn +
                ", autoBackupEnabled=" + autoBackupEnabled +
                ", totalBackups=" + totalBackups +
                '}';
    }
}
