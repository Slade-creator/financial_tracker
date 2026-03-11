package com.studentassoc.financialtracker.ViewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.studentassoc.financialtracker.Model.Transaction;

import java.util.List;

public class AIReportViewModel extends ViewModel {

    private final MutableLiveData<AIReportRequest> pendingRequest = new MutableLiveData<>();

    public void requestReport(List<Transaction> transactions,
                              String startDate,
                              String endDate,
                              String reportType) {
        pendingRequest.setValue(
                new AIReportRequest(transactions, startDate, endDate, reportType));
    }

    public MutableLiveData<AIReportRequest> getPendingRequest() {
        return pendingRequest;
    }

    public void clearRequest() {
        pendingRequest.setValue(null);
    }

    public static class AIReportRequest {
        public final List<Transaction> transactions;
        public final String startDate;
        public final String endDate;
        public final String reportType;  // "weekly" | "termly" | "monthly"

        public AIReportRequest(List<Transaction> transactions,
                               String startDate, String endDate, String reportType) {
            this.transactions = transactions;
            this.startDate    = startDate;
            this.endDate      = endDate;
            this.reportType   = reportType;
        }
    }
}
