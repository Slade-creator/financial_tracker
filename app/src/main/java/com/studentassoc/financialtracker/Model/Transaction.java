package com.studentassoc.financialtracker.Model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "transactions")
public class Transaction implements Serializable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @NonNull
    @ColumnInfo(name = "transaction_type")
    private String transactionType;


    @ColumnInfo(name = "amount")
    private int amount;

    @ColumnInfo(name = "member_name")
    private String memberName;

    @NonNull
    @ColumnInfo(name = "category")
    private String category;

    @NonNull
    @ColumnInfo(name = "payment_method")
    private String paymentMethod;

    @ColumnInfo(name = "is_approved")
    private int isApproved; // 0 = not approved, 1 = approved

    @NonNull
    @ColumnInfo(name = "transaction_date")
    private String transactionDate;

    @ColumnInfo(name = "notes")
    private String notes;

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @NonNull
    @ColumnInfo(name = "updated_at")
    private String updatedAt;


    public Transaction(@NonNull String id, @NonNull String transactionType,
                       int amount, String memberName, @NonNull String category,
                       @NonNull String paymentMethod, int isApproved,
                       @NonNull String transactionDate, String notes,
                       @NonNull String createdAt, @NonNull String updatedAt) {
        this.id = id;
        this.transactionType = transactionType;
        this.amount = amount;
        this.memberName = memberName;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.isApproved = isApproved;
        this.transactionDate = transactionDate;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(@NonNull String transactionType) {
        this.transactionType = transactionType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    @NonNull
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(@NonNull String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(int isApproved) {
        this.isApproved = isApproved;
    }

    @NonNull
    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(@NonNull String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @NonNull
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull String createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
