package com.studentassoc.financialtracker.Model;

public class FilterCriteria {
    private String startDate;
    private String endDate;
    private String category;
    private String paymentMethod;
    private Integer approvalStatus;
    private String memberSearchQuery;

    public FilterCriteria() {}

    public FilterCriteria(String startDate, String endDate, String category,
                          String paymentMethod, Integer approvalStatus,
                          String memberSearchQuery) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.approvalStatus = approvalStatus;
        this.memberSearchQuery = memberSearchQuery;
    }

    public boolean isEmpty() {
        return startDate == null && endDate == null &&
                category == null && paymentMethod == null &&
                approvalStatus == null &&
                (memberSearchQuery == null || memberSearchQuery.trim().isEmpty());
    }

    public int getActiveFilterCount() {
        int count = 0;
        if (startDate != null || endDate != null) count++; // Date range counts as one filter
        if (category != null) count++;
        if (paymentMethod != null) count++;
        if (approvalStatus != null) count++;
        if (memberSearchQuery != null && !memberSearchQuery.trim().isEmpty()) count++;
        return count;
    }

    public String getFilterDescription() {
        StringBuilder description = new StringBuilder();

        if (startDate != null || endDate != null) {
            description.append("Date Range");
        }

        if (category != null) {
            if (description.length() > 0) description.append(", ");
            description.append("Category: ").append(category);
        }

        if (paymentMethod != null) {
            if (description.length() > 0) description.append(", ");
            description.append("Payment: ").append(paymentMethod.replace("_", " "));
        }

        if (approvalStatus != null) {
            if (description.length() > 0) description.append(", ");
            description.append(approvalStatus == 1 ? "Approved" : "Pending");
        }

        if (memberSearchQuery != null && !memberSearchQuery.trim().isEmpty()) {
            if (description.length() > 0) description.append(", ");
            description.append("Member: ").append(memberSearchQuery);
        }

        return description.toString();
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
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public Integer getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Integer approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    public String getMemberSearchQuery() {
        return memberSearchQuery;
    }

    public void setMemberSearchQuery(String memberSearchQuery) {
        this.memberSearchQuery = memberSearchQuery;
    }
    public FilterCriteria copy() {
        return new FilterCriteria(startDate, endDate, category,
                paymentMethod, approvalStatus, memberSearchQuery);
    }

    public void clear() {
        this.startDate = null;
        this.endDate = null;
        this.category = null;
        this.paymentMethod = null;
        this.approvalStatus = null;
        this.memberSearchQuery = null;
    }
}
