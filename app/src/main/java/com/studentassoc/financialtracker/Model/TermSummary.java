package com.studentassoc.financialtracker.Model;

public class TermSummary {
    private int totalIncome;
    private int totalExpenses;
    private int netBalance;
    private int transactionCount;
    private String startDate;
    private String endDate;

    public TermSummary(
            int totalIncome,
            int totalExpenses,
            int netBalance,
            int transactionCount,
            String startDate,
            String endDate
    ) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netBalance = netBalance;
        this.transactionCount = transactionCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setTotalIncome(int totalIncome) {
        this.totalIncome = totalIncome;
    }

    public int getTotalIncome() { return totalIncome; }

    public void setTotalExpenses(int totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public int getTotalExpenses() { return totalExpenses; }

    public void setNetBalance(int netBalance) {
        this.netBalance = netBalance;
    }

    public int getNetBalance() { return netBalance; }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndDate() {
        return endDate;
    }
}
