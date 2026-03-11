package com.studentassoc.financialtracker.Model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class CategorySummary {
    private String category;
    @ColumnInfo(name = "transaction_type")
    private String transactionType;
    private int totalAmount;
    private int transactionCount;
    private double percentageOfTotal;

    public CategorySummary() {
    }

    @Ignore
    public CategorySummary(String category, String transactionType, int totalAmount, int transactionCount) {
        this.category = category;
        this.transactionType = transactionType;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getPercentageOfTotal() {
        return percentageOfTotal;
    }

    public void setPercentageOfTotal(double percentageOfTotal) {
        this.percentageOfTotal = percentageOfTotal;
    }

    public int getAverageAmount() {
        if (transactionCount == 0) return 0;
        return totalAmount / transactionCount;
    }
}