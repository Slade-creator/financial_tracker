package com.studentassoc.financialtracker.Repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.studentassoc.financialtracker.DTO.TransactionDao;
import com.studentassoc.financialtracker.Model.CategorySummary;
import com.studentassoc.financialtracker.Model.FilterCriteria;
import com.studentassoc.financialtracker.Model.TermSummary;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Model.WeeklySummary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<Integer> totalIncome;
    private final LiveData<Integer> totalExpenses;
    private final LiveData<Integer> currentBalance;
    private final ExecutorService executorService;

    public TransactionRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        transactionDao = database.transactionDao();
        allTransactions = transactionDao.getAllTransactions();
        totalIncome = transactionDao.getTotalIncome();
        totalExpenses = transactionDao.getTotalExpenses();
        currentBalance = transactionDao.getCurrentBalance();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.insert(transaction);
        });
    }

    public void update(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.update(transaction);
        });
    }

    public void delete(Transaction transaction) {
        executorService.execute(() -> {
            transactionDao.delete(transaction);
        });
    }

    public void deleteAllTransactions() {
        executorService.execute(transactionDao::deleteAllTransactions);
    }

    public void replaceAllTransactions(List<Transaction> transactions) {
        executorService.execute(() -> {
            transactionDao.deleteAllTransactions();
            if (!transactions.isEmpty()) {
                transactionDao.insertAll(transactions.toArray(new Transaction[0]));
            }
        });
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<Integer> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Integer> getTotalExpenses() {
        return totalExpenses;
    }

    public LiveData<Integer> getCurrentBalance() {
        return currentBalance;
    }

    public LiveData<List<Transaction>> getIncomeTransactions() {
        return transactionDao.getIncomeTransactions();
    }
    public LiveData<List<Transaction>> getExpenseTransaction() {
        return transactionDao.getExpenseTransactions();
    }

    public LiveData<List<Transaction>> getFilteredTransaction(FilterCriteria criteria) {
        if (criteria.isEmpty()) {
            return allTransactions;
        }

        return transactionDao.getFilteredTransactionsWithMemberSearch(
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategory(),
                criteria.getPaymentMethod(),
                criteria.getApprovalStatus(),
                criteria.getMemberSearchQuery()
        );
    }

    public LiveData<Integer> getFilteredTransactionCount(FilterCriteria criteria) {
        if (criteria.isEmpty()) {
            return transactionDao.getFilteredTransactionCount(null, null, null, null, null, null);
        }

        return transactionDao.getFilteredTransactionCount(
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategory(),
                criteria.getPaymentMethod(),
                criteria.getApprovalStatus(),
                criteria.getMemberSearchQuery()
        );
    }

    public LiveData<Integer> getFilteredTotalIncome(FilterCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return totalIncome;
        }

        return transactionDao.getFilteredTotalIncome(
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategory(),
                criteria.getPaymentMethod(),
                criteria.getApprovalStatus(),
                criteria.getMemberSearchQuery()
        );
    }

    public LiveData<Integer> getFilteredTotalExpenses(FilterCriteria criteria) {
        if (criteria.isEmpty()) {
            return totalExpenses;
        }

        return transactionDao.getFilteredTotalExpenses(
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategory(),
                criteria.getPaymentMethod(),
                criteria.getApprovalStatus(),
                criteria.getMemberSearchQuery()
        );
    }

    public LiveData<Integer> getFilteredCurrentBalance(FilterCriteria criteria) {
        if (criteria.isEmpty()) {
            return currentBalance;
        }

        return transactionDao.getFilteredCurrentBalance(
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategory(),
                criteria.getPaymentMethod(),
                criteria.getApprovalStatus(),
                criteria.getMemberSearchQuery()
        );
    }

    public LiveData<WeeklySummary> getWeeklySummary(String startDate, String endDate) {
        LiveData<WeeklySummary> data = transactionDao.getWeeklySummaryData(startDate, endDate);
        return Transformations.map(data, summary -> {
            if (summary == null) {
                return new WeeklySummary("", "", 0, 0, 0, 0, startDate, endDate);
            }
            return new WeeklySummary(
                    "", // weekLabel - set in fragment
                    "", // weekIdentifier - set in fragment
                    summary.getTotalIncome(),
                    summary.getTotalExpenses(),
                    summary.getNetBalance(),
                    summary.getTransactionCount(),
                    startDate,
                    endDate
            );
        });
    }

    public LiveData<TermSummary> getTermSummary(String startDate, String endDate) {
        LiveData<TermSummary> data = transactionDao.getTermSummaryData(startDate, endDate);

        return Transformations.map(data, summary -> {
            if (summary == null) {
                // Return an empty/default summary so the UI doesn't crash
                return new TermSummary(0, 0, 0, 0, startDate, endDate);
            }
            return new TermSummary(
                    summary.getTotalIncome(),
                    summary.getTotalExpenses(),
                    summary.getNetBalance(),
                    summary.getTransactionCount(),
                    startDate,
                    endDate
            );
        });
    }

    public LiveData<List<CategorySummary>> getCategoryBreakdown(String startDate, String endDate) {
        return Transformations.map(transactionDao.getCategoryBreakdownData(startDate, endDate), list -> {
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }

            return list;
        });
    }

    public LiveData<List<Transaction>> getTransactionBetween(String startDate, String endDate) {
        return transactionDao.getTransactionsBetween(startDate, endDate);
    }

    public LiveData<String> getEarliestTransactionDate() {
        return transactionDao.getEarliestTransactionDate();
    }

    public  LiveData<List<Transaction>> getPendingTransactions() {
        return transactionDao.getPendingTransactions();
    }
    public List<Transaction> getAllTransactionSync() {
        return transactionDao.getAllTransactionsSync();
    }

}
