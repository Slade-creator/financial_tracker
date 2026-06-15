package com.studentassoc.financialtracker.View;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.AIReportViewModel;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;
import com.studentassoc.financialtracker.services.ReportApiService;
import com.studentassoc.financialtracker.services.ReportResponse;

import java.util.List;
import java.util.Locale;

public class AIInsightsFragment extends Fragment {

    private View layoutIdle, layoutLoading, layoutResults, layoutError;
    private TextView tvTransactionCount, tvLoadingStatus;
    private TextView tvExecutiveSummary, tvGeneratedAt;
    private LinearLayout layoutInsightItems, layoutRecommendationItems;
    private View cardConcerns;
    private TextView tvConcerns, tvErrorMessage;
    private MaterialButton btnGenerateInsights, btnViewPdf, btnShareReport, btnRegenerate, btnRetry;

    private TransactionViewModel transactionViewModel;
    private AIReportViewModel aiReportViewModel;

    private List<Transaction> currentTransactions;
    private String pendingStartDate;
    private String pendingEndDate;
    private String pendingReportType = "monthly";
    private String lastReportUrl;
    private boolean isHandlingRequest = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_insights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        aiReportViewModel    = new ViewModelProvider(requireActivity()).get(AIReportViewModel.class);
        bindViews(view);
        setupListeners();
        observeFilteredTransactions();
        observeIncomingRequest();
    }

    private void bindViews(View v) {
        layoutIdle    = v.findViewById(R.id.layoutIdle);
        layoutLoading = v.findViewById(R.id.layoutLoading);
        layoutResults = v.findViewById(R.id.layoutResults);
        layoutError   = v.findViewById(R.id.layoutError);
        tvTransactionCount        = v.findViewById(R.id.tvTransactionCount);
        tvLoadingStatus           = v.findViewById(R.id.tvLoadingStatus);
        tvExecutiveSummary        = v.findViewById(R.id.tvExecutiveSummary);
        tvGeneratedAt             = v.findViewById(R.id.tvGeneratedAt);
        layoutInsightItems        = v.findViewById(R.id.layoutInsightItems);
        layoutRecommendationItems = v.findViewById(R.id.layoutRecommendationItems);
        cardConcerns              = v.findViewById(R.id.cardConcerns);
        tvConcerns                = v.findViewById(R.id.tvConcerns);
        tvErrorMessage            = v.findViewById(R.id.tvErrorMessage);
        btnGenerateInsights = v.findViewById(R.id.btnGenerateInsights);
        btnViewPdf          = v.findViewById(R.id.btnViewPdf);
        btnShareReport      = v.findViewById(R.id.btnShareReport);
        btnRegenerate       = v.findViewById(R.id.btnRegenerate);
        btnRetry            = v.findViewById(R.id.btnRetry);
    }

    private void setupListeners() {
        btnGenerateInsights.setOnClickListener(v -> startGeneration());
        btnRegenerate.setOnClickListener(v -> { showState(State.IDLE); startGeneration(); });
        btnRetry.setOnClickListener(v -> startGeneration());
        btnViewPdf.setOnClickListener(v -> openPdf(lastReportUrl));
        btnShareReport.setOnClickListener(v -> sharePdf(lastReportUrl));
    }

    private void observeFilteredTransactions() {
        transactionViewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (!isHandlingRequest) {
                currentTransactions = transactions;
                int count = transactions != null ? transactions.size() : 0;
                tvTransactionCount.setText(String.format(Locale.ENGLISH,
                        "Ready to analyse %d transaction%s", count, count == 1 ? "" : "s"));
            }
        });
    }

    private void observeIncomingRequest() {
        aiReportViewModel.getPendingRequest().observe(getViewLifecycleOwner(), request -> {
            if (request == null) return;
            isHandlingRequest = true;
            currentTransactions = request.transactions;
            pendingStartDate    = request.startDate;
            pendingEndDate      = request.endDate;
            pendingReportType   = request.reportType;
            tvTransactionCount.setText(String.format(Locale.ENGLISH,
                    "Analysing %d transaction%s (%s)",
                    currentTransactions.size(),
                    currentTransactions.size() == 1 ? "" : "s",
                    request.reportType));
            aiReportViewModel.clearRequest();
            requireView().post(()-> {
                isHandlingRequest = false;
                startGeneration();
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void startGeneration() {
        if (currentTransactions == null || currentTransactions.isEmpty()) {
            showError("No transactions found. Add some transactions first.");
            return;
        }
        showState(State.LOADING);
        tvLoadingStatus.setText("Analyzing your financial data...");

        String start = pendingStartDate != null ? pendingStartDate : getEarliestDate(currentTransactions);
        String end   = pendingEndDate   != null ? pendingEndDate   : getLatestDate(currentTransactions);
        String type  = pendingReportType != null ? pendingReportType : "monthly";
        pendingStartDate  = null;
        pendingEndDate    = null;
        pendingReportType = "monthly";

        ReportApiService.generateAIReport(currentTransactions, start, end, type,
                new ReportApiService.ReportCallback() {
                    @Override public void onSuccess(ReportResponse response) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> displayResults(response));
                    }
                    @Override public void onError(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> showError(error));
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void displayResults(ReportResponse response) {
        lastReportUrl = response.getReportUrl();
        tvExecutiveSummary.setText(response.getInsights().getExecutiveSummary());
        tvGeneratedAt.setText("Generated " + formatTimestamp(response.getGeneratedAt()));

        layoutInsightItems.removeAllViews();
        if (response.getInsights().getInsights() != null)
            for (String s : response.getInsights().getInsights())
                layoutInsightItems.addView(buildBulletItem(s, "•", "#1565C0"));

        layoutRecommendationItems.removeAllViews();
        if (response.getInsights().getRecommendations() != null) {
            List<String> recs = response.getInsights().getRecommendations();
            for (int i = 0; i < recs.size(); i++)
                layoutRecommendationItems.addView(buildBulletItem(recs.get(i), (i + 1) + ".", "#2E7D32"));
        }

        String concerns = response.getInsights().getConcerns();
        if (concerns != null && !concerns.trim().isEmpty()
                && !concerns.equalsIgnoreCase("none")
                && !concerns.equalsIgnoreCase("none identified")
                && !concerns.toLowerCase().contains("no significant")) {
            tvConcerns.setText(concerns);
            cardConcerns.setVisibility(View.VISIBLE);
        } else {
            cardConcerns.setVisibility(View.GONE);
        }
        showState(State.RESULTS);
    }

    private View buildBulletItem(String text, String bullet, String colorHex) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dpToPx(10));
        row.setLayoutParams(rp);

        TextView tb = new TextView(requireContext());
        tb.setText(bullet); tb.setTextSize(14f);
        tb.setTextColor(android.graphics.Color.parseColor(colorHex));
        tb.setTypeface(null, android.graphics.Typeface.BOLD);
        tb.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tc = new TextView(requireContext());
        tc.setText(text); tc.setTextSize(13f);
        tc.setTextColor(android.graphics.Color.parseColor("#424242"));
        tc.setLineSpacing(0, 1.4f);
        tc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(tb); row.addView(tc);
        return row;
    }

    private void showError(String msg) { tvErrorMessage.setText(msg); showState(State.ERROR); }

    private void showState(State s) {
        layoutIdle.setVisibility(s    == State.IDLE    ? View.VISIBLE : View.GONE);
        layoutLoading.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        layoutResults.setVisibility(s == State.RESULTS ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(s   == State.ERROR   ? View.VISIBLE : View.GONE);
    }

    private void openPdf(String path) {
        if (path == null) return;
        String url = ReportApiService.getFullReportUrl(path);
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { copyToClipboard(url); }
    }

    private void sharePdf(String path) {
        if (path == null) return;
        String url = ReportApiService.getFullReportUrl(path);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "ICTAZ MU Chapter — AI Financial Report");
        i.putExtra(Intent.EXTRA_TEXT, "AI Financial Report\n\nView: " + url);
        startActivity(Intent.createChooser(i, "Share Report"));
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager c = (android.content.ClipboardManager)
                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        c.setPrimaryClip(android.content.ClipData.newPlainText("Report URL", text));
    }

    private String getEarliestDate(List<Transaction> txns) {
        String d = txns.get(0).getTransactionDate();
        for (Transaction t : txns) if (t.getTransactionDate().compareTo(d) < 0) d = t.getTransactionDate();
        return d.substring(0, 10);
    }

    private String getLatestDate(List<Transaction> txns) {
        String d = txns.get(0).getTransactionDate();
        for (Transaction t : txns) if (t.getTransactionDate().compareTo(d) > 0) d = t.getTransactionDate();
        return d.substring(0, 10);
    }

    private String formatTimestamp(String iso) {
        if (iso == null) return "";
        try {
            String[] p = iso.split("T"); String[] dp = p[0].split("-");
            String[] m = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            return dp[2] + " " + m[Integer.parseInt(dp[1])-1] + " " + dp[0] + ", " + p[1].substring(0,5);
        } catch (Exception e) { return iso; }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private enum State { IDLE, LOADING, RESULTS, ERROR }
}
