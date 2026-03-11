package com.studentassoc.financialtracker.Model;

import androidx.room.Ignore;

public class WeeklySummary {
    @Ignore
    private String weekLabel;
    private String weekIdentifier;
    private int totalIncome;
    private int totalExpenses;
    private int netBalance;
    private int transactionCount;
    @Ignore
    private String startDate;
    @Ignore
    private String endDate;

    public WeeklySummary() {
    }

    @Ignore
    public WeeklySummary(String weekLabel, String weekIdentifier, int totalIncome,
                         int totalExpenses, int netBalance, int transactionCount,
                         String startDate, String endDate) {
        this.weekLabel = weekLabel;
        this.weekIdentifier = weekIdentifier;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netBalance = netBalance;
        this.transactionCount = transactionCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getWeekLabel() {
        return weekLabel;
    }

    public void setWeekLabel(String weekLabel) {
        this.weekLabel = weekLabel;
    }

    public String getWeekIdentifier() {
        return weekIdentifier;
    }

    public void setWeekIdentifier(String weekIdentifier) {
        this.weekIdentifier = weekIdentifier;
    }

    public int getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(int totalIncome) {
        this.totalIncome = totalIncome;
    }

    public int getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(int totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public int getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(int netBalance) {
        this.netBalance = netBalance;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public double getSavingsRatePercentage() {
        if (totalIncome == 0) return 0.0;
        return ((double) (totalIncome - totalExpenses) / totalIncome) * 100.0;
    }

    public boolean isProfitable() {
        return netBalance >= 0;
    }
}