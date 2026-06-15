package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApprovalsFragment extends Fragment {

    private TransactionViewModel transactionViewModel;

    private RecyclerView recyclerViewPending;
    private TextView tvPendingCount;
    private View layoutEmptyApprovals;
    private MaterialButton btnApproveAll;

    private ApprovalsAdapter approvalsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeData();
    }


    private void initViews(View view) {
        recyclerViewPending = view.findViewById(R.id.recyclerViewPending);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        layoutEmptyApprovals = view.findViewById(R.id.layoutEmptyApprovals);
        btnApproveAll = view.findViewById(R.id.btnApproveAll);
    }

    private void setupRecyclerView() {
        approvalsAdapter = new ApprovalsAdapter(new ArrayList<>(),
                new ApprovalsAdapter.OnApprovalActionListener() {
                    @Override
                    public void onApprove(Transaction transaction) {
                        approveTransaction(transaction);
                    }

                    @Override
                    public void onReject(Transaction transaction) {
                        confirmReject(transaction);
                    }

                    @Override
                    public void onViewDetails(Transaction transaction) {
                        showDetails(transaction);
                    }
                });

        recyclerViewPending.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewPending.setAdapter(approvalsAdapter);
    }

    private void setupListeners() {
        btnApproveAll.setOnClickListener(v -> confirmApproveAll());
    }

    private void observeData() {
        transactionViewModel.getPendingTransaction().observe(getViewLifecycleOwner(),
                transactions -> {
                    if (transactions == null) return;

                    approvalsAdapter.setTransactions(transactions);

                    int count = transactions.size();
                    tvPendingCount.setText(String.format(Locale.ENGLISH,
                            "%d pending transaction%s", count, count == 1 ? "" :
                                    "s"));

                    boolean empty = transactions.isEmpty();
                    layoutEmptyApprovals.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerViewPending.setVisibility(empty ? View.GONE : View.VISIBLE);
                    btnApproveAll.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    private void approveTransaction(Transaction transaction) {
        Transaction updated = copyWith(transaction, 1);
        transactionViewModel.update(updated);
        Toast.makeText(requireContext(), "Transaction approved", Toast.LENGTH_SHORT).show();
    }

    private void confirmReject(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reject Transaction")
                .setMessage("Are you sure you want to delete this pending transaction?\n\n" +
                        "Amount: " + Utils.toKwacha(transaction.getAmount()) + "\n" +
                        "Category: " + transaction.getCategory() + "\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    transactionViewModel.delete(transaction);
                    Toast.makeText(requireContext(),
                            "Transaction rejected and deleted", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmApproveAll() {
        List<Transaction> pending = approvalsAdapter.getTransactions();
        if (pending.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Approve All")
                .setMessage(String.format(Locale.ENGLISH,
                        "Approve all %d pending transaction%s?",
                        pending.size(), pending.size() == 1 ? "" : "s"))
                .setPositiveButton("Approve All", (dialog, which) -> {
                    for (Transaction t : pending) {
                        transactionViewModel.update(copyWith(t, 1));
                    }
                    Toast.makeText(requireContext(),
                            "All transaction approved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDetails(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Transaction Details")
                .setMessage(
                        "Type: " + transaction.getTransactionType() + "\n" +
                                "Amount: " + Utils.toKwacha(transaction.getAmount()) + "\n" +
                                "Category: " + transaction.getCategory() + "\n" +
                                "Member: " + (transaction.getMemberName() != null ?
                                transaction.getMemberName() : "—") + "\n" +
                                "Payment: " + transaction.getPaymentMethod().replace("_", " ") + "\n" +
                                "Date: " + Utils.formatDateForDisplay(transaction.getTransactionDate()) + "\n" +
                                (transaction.getNotes() != null && !transaction.getNotes().isEmpty() ?
                                        "\nNotes: " + transaction.getNotes() : ""))
                .setPositiveButton("Approve", (d, w) -> approveTransaction(transaction))
                .setNegativeButton("Reject", (d, w) -> confirmReject(transaction))
                .setNeutralButton("Close", null)
                .show();
    }

    private Transaction copyWith(Transaction t, int approvalStatus) {
        return new Transaction(
                t.getId(), t.getTransactionType(), t.getAmount(),
                t.getMemberName(), t.getCategory(), t.getPaymentMethod(),
                approvalStatus, t.getTransactionDate(), t.getNotes(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

}
