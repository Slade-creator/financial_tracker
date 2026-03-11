package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.Model.WeeklySummary;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.AIReportViewModel;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WeeklySummaryFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private AIReportViewModel aiReportViewModel;

    private TextView tvWeekLabel, tvWeekDates;
    private TextView tvWeeklyIncome, tvWeeklyExpenses, tvWeeklyBalance;
    private TextView tvTransactionCount, tvSavingsRate;
    private ProgressBar progressIncome, progressExpenses;
    private Button btnPreviousWeek, btnNextWeek, btnCurrentWeek;
    private View layoutNoData;
    private View cardAiReport;
    private MaterialButton btnAiReport;
    private TextView tvAiReportHint;

    private Calendar currentWeekStart;
    private WeeklySummary currentSummary;

    private List<Transaction> weekTransactions;
    private String currentStartDateISO;
    private String currentEndDateISO;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        aiReportViewModel = new ViewModelProvider(requireActivity()).get(AIReportViewModel.class);

        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weekly_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupListeners();
        loadWeeklySummary();
    }

    private void initializeViews(View view) {
        tvWeekLabel = view.findViewById(R.id.tvWeekLabel);
        tvWeekDates = view.findViewById(R.id.tvWeekDates);
        tvWeeklyIncome = view.findViewById(R.id.tvWeeklyIncome);
        tvWeeklyExpenses = view.findViewById(R.id.tvWeeklyExpenses);
        tvWeeklyBalance = view.findViewById(R.id.tvWeeklyBalance);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        tvSavingsRate = view.findViewById(R.id.tvSavingsRate);
        progressIncome = view.findViewById(R.id.progressIncome);
        progressExpenses = view.findViewById(R.id.progressExpenses);
        btnPreviousWeek = view.findViewById(R.id.btnPreviousWeek);
        btnNextWeek = view.findViewById(R.id.btnNextWeek);
        btnCurrentWeek = view.findViewById(R.id.btnCurrentWeek);
        layoutNoData = view.findViewById(R.id.layoutNoData);
        cardAiReport       = view.findViewById(R.id.cardAiReport);
        btnAiReport        = view.findViewById(R.id.btnAiReport);
        tvAiReportHint     = view.findViewById(R.id.tvAiReportHint);
    }

    private void setupListeners() {
        btnPreviousWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            loadWeeklySummary();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            loadWeeklySummary();
        });

        btnCurrentWeek.setOnClickListener(v -> {
            currentWeekStart = Calendar.getInstance();
            currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
            currentWeekStart.set(Calendar.MINUTE, 0);
            currentWeekStart.set(Calendar.SECOND, 0);
            currentWeekStart.set(Calendar.MILLISECOND, 0);
            loadWeeklySummary();
        });

        btnAiReport.setOnClickListener(v -> {
            Log.d("AI_debug", "the button is workings");
            navigateToAIInsights();
        });
    }

    private void loadWeeklySummary() {
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);

        currentStartDateISO = Utils.toISO8601(currentWeekStart.getTime());
        currentEndDateISO = Utils.toISO8601(weekEnd.getTime());

        updateWeekLabel();

        transactionViewModel.getWeeklySummary(currentStartDateISO, currentEndDateISO)
                .observe(getViewLifecycleOwner(), summary -> {
                    if (summary != null) {
                        currentSummary = summary;
                        displayWeeklySummary(summary);
                    }
                });

        transactionViewModel.getTransactionsBetween(currentStartDateISO, currentEndDateISO)
                .observe(getViewLifecycleOwner(), transactions -> {
                    weekTransactions = transactions;
                    updateAiButtonVisibility(transactions);
                });
    }

    private void updateAiButtonVisibility(List<Transaction> transactions) {
        if (transactions != null && !transactions.isEmpty()) {
            cardAiReport.setVisibility(View.VISIBLE);
            tvAiReportHint.setText(String.format(Locale.ENGLISH,
                    "Analyse %d transaction%s with AI",
                    transactions.size(),
                    transactions.size() == 1 ? "" : "s"));
        } else {
            cardAiReport.setVisibility(View.GONE);
        }
    }

    private void navigateToAIInsights() {
        Log.d("AI_debug", "is workings");
        if (weekTransactions == null || weekTransactions.isEmpty()) return;

        // Switch to the AI Report tab (tab index 2)
        ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
        if (viewPager != null) {
            Log.d("AI_debug", "Total tabs: " + Objects.requireNonNull(viewPager.getAdapter()).getItemCount());
            Log.d("AI_debug", "Current tab: " + viewPager.getCurrentItem());
            viewPager.setCurrentItem(2, true);
        }

        // Post the request to the shared ViewModel
        assert viewPager != null;
        viewPager.post(() -> {
            aiReportViewModel.requestReport(
                    weekTransactions,
                    currentStartDateISO,
                    currentEndDateISO,
                    "weekly"
            );
        });
    }

    private void updateWeekLabel() {
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);

        String weekLabel = "Week of " + labelFormat.format(currentWeekStart.getTime()) +
                " - " + labelFormat.format(weekEnd.getTime());
        tvWeekLabel.setText(weekLabel);

        String dateRange = fullFormat.format(currentWeekStart.getTime()) +
                " to " + fullFormat.format(weekEnd.getTime());
        tvWeekDates.setText(dateRange);

        Calendar now = Calendar.getInstance();
        btnNextWeek.setEnabled(weekEnd.before(now));
    }

    private void displayWeeklySummary(WeeklySummary summary) {
        if (summary.getTransactionCount() == 0) {
            // Show no data message
            layoutNoData.setVisibility(View.VISIBLE);
            hideDataViews();
            return;
        }

        layoutNoData.setVisibility(View.GONE);
        showDataViews();

        tvWeeklyIncome.setText(Utils.toKwacha(summary.getTotalIncome()));
        tvWeeklyExpenses.setText(Utils.toKwacha(summary.getTotalExpenses()));
        tvWeeklyBalance.setText(Utils.toKwacha(summary.getNetBalance()));

        if (summary.getNetBalance() >= 0) {
            tvWeeklyBalance.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            tvWeeklyBalance.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        tvTransactionCount.setText(String.format(Locale.ENGLISH,
                "%d transactions this week", summary.getTransactionCount()));

        double savingsRate = summary.getSavingsRatePercentage();
        tvSavingsRate.setText(String.format(Locale.ENGLISH, "Savings Rate: %.1f%%", savingsRate));
        if (savingsRate >= 0) {
            tvSavingsRate.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            tvSavingsRate.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        updateProgressBars(summary);
    }

    private void updateProgressBars(WeeklySummary summary) {
        int maxAmount = Math.max(summary.getTotalIncome(), summary.getTotalExpenses());

        if (maxAmount == 0) {
            progressIncome.setProgress(0);
            progressExpenses.setProgress(0);
        }

        int incomePercent = (int) ((summary.getTotalIncome() * 100.0) / maxAmount);
        int expensePercent = (int) ((summary.getTotalExpenses() * 100.0) / maxAmount);

        progressIncome.setProgress(incomePercent);
        progressExpenses.setProgress(expensePercent);
    }

    private void showDataViews() {
        tvWeeklyIncome.setVisibility(View.VISIBLE);
        tvWeeklyExpenses.setVisibility(View.VISIBLE);
        tvWeeklyBalance.setVisibility(View.VISIBLE);
        tvTransactionCount.setVisibility(View.VISIBLE);
        tvSavingsRate.setVisibility(View.VISIBLE);
        progressIncome.setVisibility(View.VISIBLE);
        progressExpenses.setVisibility(View.VISIBLE);
    }

    private void hideDataViews() {
        tvWeeklyIncome.setVisibility(View.GONE);
        tvWeeklyExpenses.setVisibility(View.GONE);
        tvWeeklyBalance.setVisibility(View.GONE);
        tvTransactionCount.setVisibility(View.GONE);
        tvSavingsRate.setVisibility(View.GONE);
        progressIncome.setVisibility(View.GONE);
        progressExpenses.setVisibility(View.GONE);
    }
}

