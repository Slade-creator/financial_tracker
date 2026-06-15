package com.studentassoc.financialtracker.View;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.studentassoc.financialtracker.Model.TermSummary;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.AIReportViewModel;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TermsSummaryFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private AIReportViewModel aiReportViewModel;

    private TextView tvDateRange, tvSelectedPeriod;
    private TextView tvTermIncome, tvTermExpenses, tvTermBalance, tvTermTransactionCount;
    private ChipGroup chipGroupTermPresets;
    private Button btnSelectStartDate, btnSelectEndDate, btnGenerateReport;
    private RecyclerView recyclerViewCategoryBreakdown;
    private View layoutCategoryBreakdown, layoutNoData;
    private View cardAiReport;
    private MaterialButton btnAiReport;
    private TextView tvAiReportHint;

    private Calendar startDate, endDate;
    private CategoryBreakdownAdapter categoryAdapter;
    private List<Transaction> periodTransactions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        aiReportViewModel = new ViewModelProvider(requireActivity())
                .get(AIReportViewModel.class);
        startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        endDate = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_term_summary, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupPresetChips();
        setupListeners();
        setupRecyclerView();
        updateDateRangeDisplay();
    }

    private void initializeViews(View view) {
        tvDateRange = view.findViewById(R.id.tvDateRange);
        tvSelectedPeriod = view.findViewById(R.id.tvSelectedPeriod);
        tvTermIncome = view.findViewById(R.id.tvTermIncome);
        tvTermExpenses = view.findViewById(R.id.tvTermExpenses);
        tvTermBalance = view.findViewById(R.id.tvTermBalance);
        tvTermTransactionCount = view.findViewById(R.id.tvTermTransactionCount);
        chipGroupTermPresets = view.findViewById(R.id.chipGroupTermPresets);
        btnSelectStartDate = view.findViewById(R.id.btnSelectStartDate);
        btnSelectEndDate = view.findViewById(R.id.btnSelectEndDate);
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        recyclerViewCategoryBreakdown = view.findViewById(R.id.recyclerViewCategoryBreakdown);
        layoutCategoryBreakdown = view.findViewById(R.id.layoutCategoryBreakdown);
        layoutNoData = view.findViewById(R.id.layoutNoData);
        cardAiReport              = view.findViewById(R.id.cardAiReport);
        btnAiReport               = view.findViewById(R.id.btnAiReport);
        tvAiReportHint            = view.findViewById(R.id.tvAiReportHint);
    }

    private void setupPresetChips() {
        String[][] presets = {
                {"Current Month", "currentMonth"},
                {"Last Month",    "lastMonth"},
                {"This Quarter",  "currentQuarter"},
                {"This Year",     "currentYear"},
                {"Academic Year", "academicYear"},
                {"All Time",      "allTime"},
        };

        for (String[] preset : presets) {
            Chip chip = new Chip(requireContext());
            chip.setText(preset[0]);
            chip.setCheckable(true);
            chip.setTag(preset[1]);
            chip.setOnClickListener(v -> applyPreset((String) v.getTag()));
            chipGroupTermPresets.addView(chip);
        }
    }

    private void applyPreset(String key) {
        switch (key) {
            case "currentMonth":  setCurrentMonth();   break;
            case "lastMonth":     setLastMonth();      break;
            case "currentQuarter":setCurrentQuarter(); break;
            case "currentYear":   setCurrentYear();    break;
            case "academicYear":  setAcademicYear();   break;
            case "allTime":       setAllTime();         break;
        }
    }

    private void setupListeners() {
        btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));
        btnGenerateReport.setOnClickListener(v -> generateReport());
        btnAiReport.setOnClickListener(v -> navigateToAIInsights());
    }


    private void setupRecyclerView() {
        categoryAdapter = new CategoryBreakdownAdapter(new ArrayList<>());
        recyclerViewCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewCategoryBreakdown.setAdapter(categoryAdapter);
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    if (isStartDate) {
                        startDate.set(year, month, dayOfMonth, 0, 0, 0);
                    } else {
                        endDate.set(year, month, dayOfMonth, 23, 59, 59);
                    }
                    updateDateRangeDisplay();
                    chipGroupTermPresets.clearCheck(); // Clear preset selection
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateRangeDisplay() {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        String rangeText = format.format(startDate.getTime()) + " - " + format.format(endDate.getTime());
        tvDateRange.setText(rangeText);

        btnSelectStartDate.setText(format.format(startDate.getTime()));
        btnSelectEndDate.setText(format.format(endDate.getTime()));
    }

    private void generateReport() {
        if (startDate.after(endDate)) {
            Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show();
            return;
        }

        String startDateISO = Utils.toISO8601(startDate.getTime());
        String endDateISO = Utils.toISO8601(endDate.getTime());

        transactionViewModel.getTermSummary(startDateISO, endDateISO)
                .observe(getViewLifecycleOwner(), summary -> {
                    if (summary != null && summary.getTransactionCount() > 0) {
                        displayTermSummary(summary);
                    } else {
                        showNoData();
                    }
                });

        transactionViewModel.getCategoryBreakdown(startDateISO, endDateISO)
                .observe(getViewLifecycleOwner(), categories -> {
                    if (categories != null && !categories.isEmpty()) {
                        categoryAdapter.setCategories(categories);
                        layoutCategoryBreakdown.setVisibility(View.VISIBLE);
                    }
                });

        transactionViewModel.getTransactionsBetween(startDateISO, endDateISO)
                .observe(getViewLifecycleOwner(), transactions -> {
                    periodTransactions = transactions;
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
        if (periodTransactions == null || periodTransactions.isEmpty()) return;

        String startISO = Utils.toISO8601(startDate.getTime());
        String endISO   = Utils.toISO8601(endDate.getTime());

        aiReportViewModel.requestReport(
                periodTransactions,
                startISO,
                endISO,
                "termly"
        );

        // Switch to AI Report tab (index 2)
        ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPagerReports);
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
        }
    }

    private void displayTermSummary(TermSummary summary) {
        layoutNoData.setVisibility(View.GONE);

        tvTermIncome.setText(Utils.toKwacha(summary.getTotalIncome()));
        tvTermExpenses.setText(Utils.toKwacha(summary.getTotalExpenses()));
        tvTermBalance.setText(Utils.toKwacha(summary.getNetBalance()));

        if (summary.getNetBalance() >= 0) {
            tvTermBalance.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            tvTermBalance.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        tvTermTransactionCount.setText(String.format(Locale.ENGLISH,
                "%d transactions", summary.getTransactionCount()));

        long days = (endDate.getTimeInMillis() - startDate.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        tvSelectedPeriod.setText(String.format(Locale.ENGLISH, "%d days", days + 1));
    }

    private void showNoData() {
        layoutNoData.setVisibility(View.VISIBLE);
        layoutCategoryBreakdown.setVisibility(View.GONE);
    }

    private void setCurrentMonth() {
        startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);

        endDate = Calendar.getInstance();
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);

        updateDateRangeDisplay();
        generateReport();
    }

    private void setLastMonth() {
        startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, -1);
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);

        endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, -1);
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);

        updateDateRangeDisplay();
        generateReport();
    }

    private void setCurrentQuarter() {
        startDate = Calendar.getInstance();
        int month = startDate.get(Calendar.MONTH);
        int quarterStartMonth = (month / 3) * 3; // 0, 3, 6, or 9
        startDate.set(Calendar.MONTH, quarterStartMonth);
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);

        endDate = Calendar.getInstance();

        updateDateRangeDisplay();
        generateReport();
    }

    private void setCurrentYear() {
        startDate = Calendar.getInstance();
        startDate.set(Calendar.MONTH, Calendar.JANUARY);
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);

        endDate = Calendar.getInstance();

        updateDateRangeDisplay();
        generateReport();
    }

    private void setAcademicYear() {
        startDate = Calendar.getInstance();
        int currentMonth = startDate.get(Calendar.MONTH);

        if (currentMonth >= Calendar.SEPTEMBER) {
            // Current academic year started in September this year
            startDate.set(Calendar.MONTH, Calendar.SEPTEMBER);
        } else {
            // Current academic year started in September last year
            startDate.add(Calendar.YEAR, -1);
            startDate.set(Calendar.MONTH, Calendar.SEPTEMBER);
        }
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);

        endDate = Calendar.getInstance();

        updateDateRangeDisplay();
        generateReport();
    }

    private void setAllTime() {
        // Query database for earliest transaction date
        transactionViewModel.getEarliestTransactionDate().observe(getViewLifecycleOwner(), earliestDate -> {
            if (earliestDate != null) {
                startDate.setTime(Utils.fromISO8601(earliestDate));
            } else {
                // Default to 1 year ago if no transactions
                startDate = Calendar.getInstance();
                startDate.add(Calendar.YEAR, -1);
            }

            endDate = Calendar.getInstance();
            updateDateRangeDisplay();
            generateReport();
        });
    }
}
