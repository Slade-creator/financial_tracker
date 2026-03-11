package com.studentassoc.financialtracker.View;

import static com.studentassoc.financialtracker.services.ReportApiService.generateAIReport;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.CsvExporter;
import com.studentassoc.financialtracker.Utils.PdfExporter;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;
import com.studentassoc.financialtracker.services.ReportApiService;
import com.studentassoc.financialtracker.services.ReportResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportBottomSheet extends BottomSheetDialogFragment {

    private TransactionViewModel viewModel;
    private RadioGroup rgExportFormat;
    private RadioGroup rgExportOptions;

    private RadioButton rbAiReport;
    private MaterialButton btnExport;
    private TextView tvExportInfo;

    private List<Transaction> transactions;
    private int totalIncome;
    private int totalExpenses;
    private int balance;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AlertDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_export, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        initializeViews(view);
        setupListeners();
        loadData();
    }

    private void initializeViews(View view) {
        rgExportFormat = view.findViewById(R.id.rgExportFormat);
        rgExportOptions = view.findViewById(R.id.rgExportOptions);
        btnExport = view.findViewById(R.id.btnExport);
        rbAiReport = view.findViewById(R.id.rbAiReport);
        tvExportInfo = view.findViewById(R.id.tvExportInfo);

        // Set default selections
        rgExportFormat.check(R.id.rbCsv);
        rgExportOptions.check(R.id.rbWithSummary);

        ProgressBar progressBar = new ProgressBar(requireContext());
        progressBar.setPadding(0, 40, 0, 40);

        progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Processing")
                .setMessage("Please wait...")
                .setView(progressBar)
                .setCancelable(false)
                .create();

        updateExportInfo();
    }

    private void setupListeners() {

        rgExportFormat.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.rbAiReport) {
                rgExportOptions.setVisibility(View.GONE);
            } else {
                rgExportOptions.setVisibility(View.VISIBLE);
            }
        });

        rgExportOptions.setOnCheckedChangeListener((group, checkedId) -> updateExportInfo());

        btnExport.setOnClickListener(v -> handleExport());
    }

    private void loadData() {
        // Observe filtered transactions
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactionList -> {
            this.transactions = transactionList;
            updateExportInfo();
        });

        // Observe totals
        viewModel.getFilteredTotalIncome().observe(getViewLifecycleOwner(), income -> {
            this.totalIncome = income != null ? income : 0;
        });

        viewModel.getFilteredTotalExpenses().observe(getViewLifecycleOwner(), expenses -> {
            this.totalExpenses = expenses != null ? expenses : 0;
        });

        viewModel.getFilteredCurrentBalance().observe(getViewLifecycleOwner(), bal -> {
            this.balance = bal != null ? bal : 0;
        });
    }

    @SuppressLint("DefaultLocale")
    private void updateExportInfo() {
        if (transactions == null) return;

        int checkedFormatId = rgExportFormat.getCheckedRadioButtonId();
        boolean includeSummary = rgExportOptions.getCheckedRadioButtonId() == R.id.rbWithSummary;

        String format = checkedFormatId == R.id.rbCsv ? "CSV" : "PDF";
        String summary = includeSummary ? "with summary statistics" : "transactions only";

        String info;
        if (checkedFormatId == R.id.rbAiReport) {
            info = String.format("Generate AI-powered report for %d transactions\n" +
                            "Includes insights, recommendations, and professional PDF",
                    transactions.size());
        } else {
             includeSummary = rgExportOptions.getCheckedRadioButtonId() == R.id.rbWithSummary;
             format = checkedFormatId == R.id.rbCsv ? "CSV" : "PDF";
             summary = includeSummary ? "with summary statistics" : "transactions only";
            info = String.format("Export %d transactions as %s (%s)",
                    transactions.size(), format, summary);
        }

        tvExportInfo.setText(info);
    }

    private void handleExport() {
        if (transactions == null || transactions.isEmpty()) {
            Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show();
        }

        int checkedFormatId = rgExportFormat.getCheckedRadioButtonId();

        // Handle AI report separately
        if (checkedFormatId == R.id.rbAiReport) {
            generateAIReport();
            return;
        }

        ProgressBar progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(60, 40, 60, 0);

        progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Exporting")
                .setMessage("Generating export file...")
                .setView(progressBar)
                .setCancelable(false)
                .create();

        progressDialog.show();

        boolean includeSummary = rgExportOptions.getCheckedRadioButtonId() == R.id.rbWithSummary;

        executorService.execute(() -> {
            Uri fileUri;

            if (checkedFormatId == R.id.rbCsv) {
                fileUri = exportCsv(includeSummary);
            } else {
                fileUri = exportPdf();
            }

            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();

                if (fileUri != null) {
                    shareFile(fileUri, checkedFormatId == R.id.rbCsv ? "CSV" : "PDF");
                } else {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void generateAIReport() {

        progressDialog.setMessage("Analyzing data with AI...\nThis may take 5-10 seconds");
        progressDialog.show();

        String startDate = getStartDate();
        String endDate = getEndDate();

        ReportApiService.generateAIReport(
                transactions,
                startDate,
                endDate,
                new ReportApiService.ReportCallback() {
                    @Override
                    public void onSuccess(ReportResponse response) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showAISummaryDialog(response);
                        });
                    }
                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showErrorDialog(error);
                        });
                    }
                }
        );
    }

    private void showAISummaryDialog(ReportResponse response) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("📊 AI Report Generated");

        StringBuilder message = new StringBuilder();
        message.append("✨ ").append(response.getSummary()).append("\n\n");

        message.append("🔍 Key Insights:\n");
        int insightNum = 1;
        for (String insight : response.getInsights().getInsights()) {
            message.append(insightNum++).append(". ").append(insight).append("\n");
        }

        message.append("\n💡 Recommendations:\n");
        int recNum = 1;
        for (String rec : response.getInsights().getRecommendations()) {
            message.append(recNum++).append(". ").append(rec).append("\n");
        }

        String concerns = response.getInsights().getConcerns();
        if (concerns != null && !concerns.equalsIgnoreCase("none")
                && !concerns.equalsIgnoreCase("none identified")) {
            message.append("\n⚠️ Concerns:\n").append(concerns);
        }

        builder.setMessage(message.toString());

        builder.setPositiveButton("View PDF", (dialog, which) -> {
            openPDF(response.getReportUrl());
        });

        // Button to share
        builder.setNegativeButton("Share", (dialog, which) -> {
            sharePDF(response.getReportUrl());
        });

        // Close button
        builder.setNeutralButton("Close", (dialog, which) -> {
            dialog.dismiss();
            dismiss(); // Close the bottom sheet
        });

        builder.show();
    }

    private void openPDF(String reportPath) {
        String fullUrl = ReportApiService.getFullReportUrl(reportPath);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(fullUrl));

        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "No app found to open PDF. URL copied to clipboard.",
                    Toast.LENGTH_LONG).show();

            // Copy URL to clipboard as fallback
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) requireContext()
                            .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("Report URL", fullUrl);
            clipboard.setPrimaryClip(clip);
        }
    }

    private void sharePDF(String reportPath) {
        String fullUrl = ReportApiService.getFullReportUrl(reportPath);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AI-Powered Financial Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "📊 Financial Report with AI Insights\n\n" +
                        "View the full report here: " + fullUrl + "\n\n" +
                        "Generated by Financial Tracker");

        startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }

    private void showErrorDialog(String error) {
        new AlertDialog.Builder(requireContext())
                .setTitle("❌ Error Generating Report")
                .setMessage("Failed to generate AI report:\n\n" + error +
                        "\n\nMake sure the backend server is running.")
                .setPositiveButton("Retry", (dialog, which) -> generateAIReport())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getStartDate() {
        if (transactions == null || transactions.isEmpty()) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        // Get earliest transaction date
        String earliest = transactions.get(0).getTransactionDate();
        for (Transaction tx : transactions) {
            if (tx.getTransactionDate().compareTo(earliest) < 0) {
                earliest = tx.getTransactionDate();
            }
        }

        // Convert to simple date format
        try {
            return earliest.substring(0, 10); // Extract YYYY-MM-DD
        } catch (Exception e) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }
    }

    private String getEndDate() {
        if (transactions == null || transactions.isEmpty()) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        // Get latest transaction date
        String latest = transactions.get(0).getTransactionDate();
        for (Transaction tx : transactions) {
            if (tx.getTransactionDate().compareTo(latest) > 0) {
                latest = tx.getTransactionDate();
            }
        }

        // Convert to simple date format
        try {
            return latest.substring(0, 10); // Extract YYYY-MM-DD
        } catch (Exception e) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }
    }



    private Uri exportCsv(boolean includeSummary) {
        if (includeSummary) {
            return CsvExporter.exportWithSummary(
                    requireContext(),
                    transactions,
                    totalIncome,
                    totalExpenses,
                    balance
            );
        } else {
            return CsvExporter.exportToCSV(requireContext(), transactions, null);
        }
    }

    private Uri exportPdf() {
        String reportTitle = "Financial Report";

        // Add date range if filters are active
        if (viewModel.hasActiveFilters()) {
            reportTitle = "Filtered Financial Report";
        }

        return PdfExporter.generateReport(
                requireContext(),
                transactions,
                totalIncome,
                totalExpenses,
                balance,
                reportTitle
        );
    }

  private void shareFile(Uri fileUri, String fileType) {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType(fileType.equals("CSV") ? "text/csv" : "application/pdf");
      shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      String subject = "Financial Tracker Export - " + fileType;
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

      @SuppressLint("DefaultLocale") String text = String.format("Financial report containing %d transactions.\n\nTotal Income: %s\nTotal Expenses: %s\nNet Balance: %s",
              transactions.size(),
              Utils.toKwacha(totalIncome),
              Utils.toKwacha(totalExpenses),
              Utils.toKwacha(balance));
      shareIntent.putExtra(Intent.EXTRA_TEXT, text);

      startActivity(Intent.createChooser(shareIntent, "Share " + fileType + " Report"));

      Toast.makeText(requireContext(), fileType + " export successful!", Toast.LENGTH_SHORT).show();
      dismiss();
  }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        executorService.shutdown();
    }
}
