package com.studentassoc.financialtracker.DTO;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.studentassoc.financialtracker.Model.CategorySummary;
import com.studentassoc.financialtracker.Model.TermSummary;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Model.WeeklySummary;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    @Insert
    void insertAll(Transaction... transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    Transaction getTransactionById(String transactionId);

    @Query("SELECT * FROM transactions WHERE transaction_type = 'INCOME' ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getIncomeTransactions();

    @Query("SELECT * FROM transactions WHERE transaction_type = 'EXPENSE' ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getExpenseTransactions();

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsCategory(String category);

    @Query("SELECT * FROM transactions WHERE payment_method = :paymentMethod ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByPaymentMethod(String paymentMethod);

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(String startDate, String endDate);

    @Query("SELECT * FROM transactions WHERE transaction_type = 'INCOME' AND member_name = :memberName ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getIncomeByMember(String memberName);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE transaction_type = 'INCOME'")
    LiveData<Integer> getTotalIncome();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE transaction_type = 'EXPENSE'")
    LiveData<Integer> getTotalExpenses();

    @Query("SELECT COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE -amount END), 0) FROM transactions")
    LiveData<Integer> getCurrentBalance();

    @Query("SELECT * FROM transactions WHERE is_approved = :isApproved ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getTransactionByApprovalStatus(int isApproved);

    @Query("SELECT * FROM transactions WHERE member_name LIKE '%' || :searchQuery || '%' ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> searchByMemberName(String searchQuery);

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date ASC")
    LiveData<List<Transaction>> getTransactionsBetween(String startDate, String endDate);

    @Query("DELETE FROM transactions")
    void deleteAllTransactions();

    @Query("SELECT * FROM transactions WHERE " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) " +
            "ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getFilteredTransaction(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus
    );

    @Query("SELECT * FROM transactions WHERE " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) AND " +
            "(:memberSearch IS NULL OR member_name LIKE '%' || :memberSearch || '%') " +
            "ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getFilteredTransactionsWithMemberSearch(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus,
            String memberSearch
    );

    @Query("SELECT COUNT(*) FROM transactions WHERE " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) AND " +
            "(:memberSearch IS NULL OR member_name LIKE '%' || :memberSearch || '%')")
    LiveData<Integer> getFilteredTransactionCount(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus,
            String memberSearch
    );

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE " +
            "transaction_type = 'INCOME' AND " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) AND " +
            "(:memberSearch IS NULL OR member_name LIKE '%' || :memberSearch || '%')")
    LiveData<Integer> getFilteredTotalIncome(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus,
            String memberSearch
    );

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE " +
            "transaction_type = 'EXPENSE' AND " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) AND " +
            "(:memberSearch IS NULL OR member_name LIKE '%' || :memberSearch || '%')")
    LiveData<Integer> getFilteredTotalExpenses(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus,
            String memberSearch
    );

    @Query("SELECT COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE -amount END), 0) " +
            "FROM transactions WHERE " +
            "(:startDate IS NULL OR transaction_date >= :startDate) AND " +
            "(:endDate IS NULL OR transaction_date <= :endDate) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:paymentMethod IS NULL OR payment_method = :paymentMethod) AND " +
            "(:approvalStatus IS NULL OR is_approved = :approvalStatus) AND " +
            "(:memberSearch IS NULL OR member_name LIKE '%' || :memberSearch || '%')")
    LiveData<Integer> getFilteredCurrentBalance(
            String startDate,
            String endDate,
            String category,
            String paymentMethod,
            Integer approvalStatus,
            String memberSearch
    );

    @Query("SELECT " +
            "SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END) as totalIncome, " +
            "SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpenses, " +
            "SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE -amount END) as netBalance, " +
            "COUNT(*) as transactionCount " +
            "FROM transactions " +
            "WHERE transaction_date BETWEEN :startDate AND :endDate")
    LiveData<WeeklySummary> getWeeklySummaryData(String startDate, String endDate);

    @Query("SELECT " +
            "SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END) as totalIncome, " +
            "SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpenses, " +
            "SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE -amount END) as netBalance, " +
            "COUNT(*) as transactionCount " +
            "FROM transactions " +
            "WHERE transaction_date BETWEEN :startDate AND :endDate")
    LiveData<TermSummary> getTermSummaryData(String startDate, String endDate);

    @Query("SELECT category, transaction_type, " +
            "SUM(amount) as totalAmount, " +
            "COUNT(*) as transactionCount, " +
            "(SUM(amount) * 100.0 / (SELECT SUM(amount) FROM transactions WHERE transaction_date BETWEEN :startDate AND :endDate)) as percentageOfTotal " +
            "FROM transactions " +
            "WHERE transaction_date BETWEEN :startDate AND :endDate " +
            "GROUP BY category, transaction_type " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CategorySummary>> getCategoryBreakdownData(String startDate, String endDate);

    @Query("SELECT COALESCE(MIN(transaction_date), DATE('now')) FROM transactions")
    LiveData<String> getEarliestTransactionDate();

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT * FROM transactions WHERE is_approved = 0 ORDER BY transaction_date DESC")
    LiveData<List<Transaction>> getPendingTransactions();
}
