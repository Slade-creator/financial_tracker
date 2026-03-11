package com.studentassoc.financialtracker.services;

import java.util.List;

public class AIInsights {

    private String executiveSummary;
    private List<String> insights;
    private List<String> recommendations;
    private String concerns;

    // Constructors
    public AIInsights() {}

    public AIInsights(String executiveSummary, List<String> insights,
                      List<String> recommendations, String concerns) {
        this.executiveSummary = executiveSummary;
        this.insights = insights;
        this.recommendations = recommendations;
        this.concerns = concerns;
    }

    // Getters and setters
    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getConcerns() {
        return concerns;
    }

    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    @Override
    public String toString() {
        return "AIInsights{" +
                "executiveSummary='" + executiveSummary + '\'' +
                ", insights=" + insights +
                ", recommendations=" + recommendations +
                ", concerns='" + concerns + '\'' +
                '}';
    }
}