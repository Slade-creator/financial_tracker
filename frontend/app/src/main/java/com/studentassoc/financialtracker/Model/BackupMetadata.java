package com.studentassoc.financialtracker.Model;

import androidx.annotation.NonNull;

public class BackupMetadata {

    private String backupType;
    private String timestamp;
    private String deviceId;
    private int version;
    private Integer baseVersion;
    private String lastBackupTimestamp;
    private int transactionCount;
    private long fileSize;

    public BackupMetadata() {

    }

    public BackupMetadata(
            String backupType, String timestamp, String deviceId, int version
    ) {
        this.backupType = backupType;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.version = version;
    }

    public String getBackupType() {
        return backupType;
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Integer getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(Integer baseVersion) {
        this.baseVersion = baseVersion;
    }

    public String getLastBackupTimestamp() {
        return lastBackupTimestamp;
    }

    public void setLastBackupTimestamp(String lastBackupTimestamp) {
        this.lastBackupTimestamp = lastBackupTimestamp;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isFullBackup() {
        return "FULL".equals(backupType);
    }

    public boolean isIncrementalBackup() {
        return "INCREMENTAL".equals(backupType);
    }

    @NonNull
    @Override
    public String toString() {
        return "BackupMetadata{" +
                "backupType='" + backupType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", version=" + version +
                ", transactionCount=" + transactionCount +
                '}';
    }


}
