package com.studentassoc.financialtracker.services;

import androidx.annotation.NonNull;

public class ReportResponse {

    private boolean success;
    private String reportUrl;
    private String summary;
    private AIInsights insights;
    private String generatedAt;

    public ReportResponse() {}

    public ReportResponse(boolean success, String reportUrl, String summary,
                          AIInsights insights, String generatedAt) {
        this.success = success;
        this.reportUrl = reportUrl;
        this.summary = summary;
        this.insights = insights;
        this.generatedAt = generatedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public AIInsights getInsights() {
        return insights;
    }

    public void setInsights(AIInsights insights) {
        this.insights = insights;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "ReportResponse{" +
                "success=" + success +
                ", reportUrl='" + reportUrl + '\'' +
                ", summary='" + summary + '\'' +
                ", insights=" + insights +
                ", generatedAt='" + generatedAt + '\'' +
                '}';
    }

}
