package com.studentassoc.financialtracker.services;

import com.studentassoc.financialtracker.Model.Transaction;

import java.util.List;

public class ReportRequest {

    private String period;
    private String reportType;
    private List<Transaction> transactions;

    public ReportRequest(String startDate, String endDate, List<Transaction> transactions, String reportType) {
        this.period = startDate + " to " + endDate;
        this.reportType = reportType;
        this.transactions = transactions;
    }

    public ReportRequest(String startDate, String endDate, List<Transaction> transactions) {
        this(startDate, endDate, transactions, "monthly");
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}
