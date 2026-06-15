package com.studentassoc.financialtracker.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.studentassoc.financialtracker.Model.CategorySummary;
import com.studentassoc.financialtracker.Model.FilterCriteria;
import com.studentassoc.financialtracker.Model.TermSummary;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Model.WeeklySummary;
import com.studentassoc.financialtracker.Repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<Integer> totalIncome;
    private final LiveData<Integer> totalExpenses;
    private final LiveData<Integer> currentBalance;

    private final MutableLiveData<FilterCriteria> filterCriteria = new MutableLiveData<>();
    private final LiveData<List<Transaction>> filteredTransactions;
    private final LiveData<Integer> filteredTransactionCount;
    private final LiveData<Integer> filteredTotalIncome;
    private final LiveData<Integer> filteredTotalExpenses;
    private final LiveData<Integer> filteredCurrentBalance;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        allTransactions = repository.getAllTransactions();
        totalIncome = repository.getTotalIncome();
        totalExpenses = repository.getTotalExpenses();
        currentBalance = repository.getCurrentBalance();

        filterCriteria.setValue(new FilterCriteria());

        this.filteredTransactions = Transformations.switchMap(filterCriteria, criteria -> {
            if (criteria == null || criteria.isEmpty()) {
                return allTransactions;
            }
            return repository.getFilteredTransaction(criteria);
        });

        this.filteredTransactionCount = Transformations.switchMap(filterCriteria, repository::getFilteredTransactionCount
        );

        this.filteredTotalIncome = Transformations.switchMap(filterCriteria, repository::getFilteredTotalIncome
        );

        this.filteredTotalExpenses = Transformations.switchMap(filterCriteria, repository::getFilteredTotalExpenses
        );

        this.filteredCurrentBalance = Transformations.switchMap(filterCriteria, repository::getFilteredCurrentBalance
        );
    }

    public void deleteAllTransactions() {
        repository.deleteAllTransactions();
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
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
        return repository.getIncomeTransactions();
    }

    public LiveData<List<Transaction>> getExpenseTransactions() {
        return repository.getExpenseTransaction();
    }
    public void applyFilters(FilterCriteria criteria) {
        filterCriteria.setValue(criteria);
    }
    public void clearFilters() {
        filterCriteria.setValue(new FilterCriteria());
    }
    public LiveData<FilterCriteria> getFilterCriteria() {
        return filterCriteria;
    }

    public FilterCriteria getCurrentFilterCriteria() {
        FilterCriteria current = filterCriteria.getValue();
        return current != null ? current : new FilterCriteria();
    }

    public boolean hasActiveFilters() {
        FilterCriteria current = filterCriteria.getValue();
        return current != null && !current.isEmpty();
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public LiveData<Integer> getFilteredTransactionCount() {
        return filteredTransactionCount;
    }

    public LiveData<Integer> getFilteredTotalIncome() {
        return filteredTotalIncome;
    }

    public LiveData<Integer> getFilteredTotalExpenses() {
        return filteredTotalExpenses;
    }

    public LiveData<Integer> getFilteredCurrentBalance() {
        return filteredCurrentBalance;
    }

    public void filterByDateRange(String startDate, String endDate) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setStartDate(startDate);
        criteria.setEndDate(endDate);
        applyFilters(criteria);
    }

    public void filterByCategory(String category) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setCategory(category);
        applyFilters(criteria);
    }

    public void filterByPaymentMethod(String paymentMethod) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setPaymentMethod(paymentMethod);
        applyFilters(criteria);
    }

    public void filterByApprovalStatus(int approvalStatus) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setApprovalStatus(approvalStatus);
        applyFilters(criteria);
    }

    public void searchByMemberName(String searchQuery) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setMemberSearchQuery(searchQuery);
        applyFilters(criteria);
    }

    public LiveData<WeeklySummary> getWeeklySummary(String startDate, String endDate) {
        return repository.getWeeklySummary(startDate, endDate);
    }

    public LiveData<TermSummary> getTermSummary(String startDate, String endDate) {
        return repository.getTermSummary(startDate, endDate);
    }

    public LiveData<List<CategorySummary>> getCategoryBreakdown(String startDate, String endDate) {
        return repository.getCategoryBreakdown(startDate, endDate);
    }

    public LiveData<List<Transaction>> getTransactionsBetween(String startDate, String endDate) {
        return repository.getTransactionBetween(startDate, endDate);
    }

    public LiveData<String> getEarliestTransactionDate() {
        return repository.getEarliestTransactionDate();
    }

    public LiveData<List<Transaction>> getPendingTransaction() {
        return repository.getPendingTransactions();
    }
 }
