package com.studentassoc.financialtracker.View;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.studentassoc.financialtracker.Model.FilterCriteria;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Locale;

public class DashboardFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private TransactionViewModel transactionViewModel;

    private TextView tvCurrentBalance, tvTotalIncome, tvTotalExpenses;
    private TextView tvTransactionCount, tvFilteredLabel;
    private ChipGroup chipGroupActiveFilters;
    private RecyclerView recyclerViewRecentTransactions;

    private FloatingActionButton fabAddTransaction, fabFilter;
    private boolean isFilterVisible = false;

    private TransactionAdapter transactionAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeView(view);
        setupRecyclerView();
        setupSwipeToDelete();
        setupListeners();
        observeData();
    }

    private void initializeView(View view) {
        // Summary cards
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);

        // Filter display
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        tvFilteredLabel = view.findViewById(R.id.tvFilteredLabel);
        chipGroupActiveFilters = view.findViewById(R.id.chipGroupActiveFilters);

        // Recent transactions list
        recyclerViewRecentTransactions = view.findViewById(R.id.recyclerViewRecentTransactions);

        // FABs
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        fabFilter = view.findViewById(R.id.fabFilter);

        fabFilter.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(new ArrayList<>(), this);
        recyclerViewRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewRecentTransactions.setAdapter(transactionAdapter);
    }

    private void setupListeners() {
        fabAddTransaction.setOnClickListener(v -> {
            TransactionFormBottomSheet bottomSheet = new TransactionFormBottomSheet();
            bottomSheet.show(getParentFragmentManager(), "TransactionFormBottomSheet");
        });

        fabFilter.setOnClickListener(v -> {
            FilterDialogFragment filterDialog = new FilterDialogFragment();
            filterDialog.show(getParentFragmentManager(), "FilterDialogFragment");
        });

        fabAddTransaction.setOnLongClickListener(v -> {
            toggleAnchoredFabs();
            return true;
        });


        assert getView() != null;
        View tvViewAll = getView().findViewById(R.id.tvViewAllTransactions);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Showing all transactions", Toast.LENGTH_SHORT).show());
        }
    }

    private void toggleAnchoredFabs() {
        if (isFilterVisible) {
            fabFilter.setVisibility(View.GONE);
            fabAddTransaction.animate().rotation(0f).setDuration(200).start();
        } else {
            fabFilter.setVisibility(View.VISIBLE);
            fabAddTransaction.animate().rotation(45f).setDuration(200).start();
        }

        isFilterVisible = !isFilterVisible;
    }


    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();

                Transaction deletedTransaction = transactionAdapter.getTransactionAt(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to delete this transaction?\n\n" +
                                "Type: " + deletedTransaction.getTransactionType() + "\n" +
                                "Amount: " + Utils.toKwacha(deletedTransaction.getAmount()) + "\n" +
                                "Category: " + deletedTransaction.getCategory())
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // BR-07 & BR-08: Hard delete within database transaction
                            transactionViewModel.delete(deletedTransaction);

                            // Show undo option
                            Snackbar.make(recyclerViewRecentTransactions, "Transaction deleted", Snackbar.LENGTH_LONG)
                                    .setAction("UNDO", v -> {
                                        transactionViewModel.insert(deletedTransaction);
                                    }).show();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // Restore the item if cancelled
                            transactionAdapter.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> {
                            // Restore the item if dialog is dismissed
                            transactionAdapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewRecentTransactions);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        showTransactionDetailsDialog(transaction);
    }

    @Override
    public void onTransactionLongClick(Transaction transaction) {
        TransactionFormBottomSheet bottomSheet = TransactionFormBottomSheet.newInstance(transaction);
        bottomSheet.show(getParentFragmentManager(), "TransactionFormBottomSheet");
    }

    private void showTransactionDetailsDialog(Transaction transaction) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);

        TextView tvDetailType = dialogView.findViewById(R.id.tvDetailType);
        TextView tvDetailAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDetailCategory = dialogView.findViewById(R.id.tvDetailCategory);
        TextView tvDetailMember = dialogView.findViewById(R.id.tvDetailMember);
        TextView tvDetailPayment = dialogView.findViewById(R.id.tvDetailPayment);
        TextView tvDetailDate = dialogView.findViewById(R.id.tvDetailDate);
        TextView tvDetailNotes = dialogView.findViewById(R.id.tvDetailNotes);
        TextView tvDetailStatus = dialogView.findViewById(R.id.tvDetailStatus);
        TextView tvDetailCreated = dialogView.findViewById(R.id.tvDetailCreated);
        TextView tvDetailUpdated = dialogView.findViewById(R.id.tvDetailUpdated);
        View notesSection = dialogView.findViewById(R.id.notesSection);
        View memberSection = dialogView.findViewById(R.id.memberSection);

        tvDetailType.setText(transaction.getTransactionType());
        tvDetailAmount.setText(Utils.toKwacha(transaction.getAmount()));
        tvDetailCategory.setText(transaction.getCategory());
        tvDetailPayment.setText(transaction.getPaymentMethod().replace("_", " "));
        tvDetailDate.setText(Utils.formatDateForDisplay(transaction.getTransactionDate()));
        tvDetailStatus.setText(transaction.getIsApproved() == 1 ? "Approved" : "Pending");
        tvDetailCreated.setText(Utils.formatDateForDisplay(transaction.getCreatedAt()));
        tvDetailUpdated.setText(Utils.formatDateForDisplay(transaction.getUpdatedAt()));

        int color = transaction.getTransactionType().equals("INCOME") ?
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark) :
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
        tvDetailType.setTextColor(color);
        tvDetailAmount.setTextColor(color);

        if (transaction.getMemberName() != null && !transaction.getMemberName().isEmpty()) {
            memberSection.setVisibility(View.VISIBLE);
            tvDetailMember.setText(transaction.getMemberName());
        } else {
            memberSection.setVisibility(View.GONE);
        }

        if (transaction.getNotes() != null && !transaction.getNotes().isEmpty()) {
            notesSection.setVisibility(View.VISIBLE);
            tvDetailNotes.setText(transaction.getNotes());
        } else {
            notesSection.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Transaction Details")
                .setView(dialogView)
                .setPositiveButton("Edit", (d, which) -> {
                    TransactionFormBottomSheet bottomSheet = TransactionFormBottomSheet.newInstance(transaction);
                    bottomSheet.show(getParentFragmentManager(), "TransactionFormBottomSheet");
                })
                .setNegativeButton("Delete", (d, which) -> confirmDeleteTransaction(transaction))
                .setNeutralButton("Close", null)
                .create();

        dialog.show();

        Button editButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button deleteButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        deleteButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
    }

    private void confirmDeleteTransaction(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to permanently delete this transaction?\n\n" +
                        "Type: " + transaction.getTransactionType() + "\n" +
                        "Amount: " + Utils.toKwacha(transaction.getAmount()) + "\n" +
                        "Category: " + transaction.getCategory() + "\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    transactionViewModel.delete(transaction);
                    Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void observeData() {

        transactionViewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                // Show only recent 10 transactions on dashboard
                int displayCount = Math.min(10, transactions.size());
                transactionAdapter.setTransactions(
                        transactions.subList(0, displayCount)
                );
            }
        });

        transactionViewModel.getFilteredTotalIncome().observe(getViewLifecycleOwner(), income -> {
            if (income != null) {
                tvTotalIncome.setText(Utils.toKwacha(income));
            }
        });

        transactionViewModel.getFilteredTotalExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                tvTotalExpenses.setText(Utils.toKwacha(expenses));
            }
        });

        transactionViewModel.getFilteredCurrentBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                tvCurrentBalance.setText(Utils.toKwacha(balance));

                // Update color based on balance
                if (balance >= 0) {
                    tvCurrentBalance.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                } else {
                    tvCurrentBalance.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                }
            }
        });

        transactionViewModel.getFilteredTransactionCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && tvTransactionCount != null) {
                tvTransactionCount.setText(String.format(Locale.ENGLISH, "%d transactions", count));
            }
        });

        transactionViewModel.getFilterCriteria().observe(getViewLifecycleOwner(), criteria -> {
            if (criteria != null) {
                updateFilterChips(criteria);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateFilterChips(FilterCriteria criteria) {
        if (chipGroupActiveFilters == null) return;

        chipGroupActiveFilters.removeAllViews();

        if (criteria.isEmpty()) {
            chipGroupActiveFilters.setVisibility(View.GONE);
            if (tvFilteredLabel != null) {
                tvFilteredLabel.setVisibility(View.GONE);
            }
        }

        chipGroupActiveFilters.setVisibility(View.VISIBLE);
        if (tvFilteredLabel != null) {
            tvFilteredLabel.setVisibility(View.VISIBLE);
        }

        if (criteria.getStartDate() != null || criteria.getEndDate() != null) {
            Chip chip = new Chip(requireContext());
            String chipText = "Date: ";
            if (criteria.getStartDate() != null) {
                chipText += Utils.formatDateForDisplay(criteria.getStartDate());
            }
            chipText += " - ";
            if (criteria.getEndDate() != null) {
                chipText += Utils.formatDateForDisplay(criteria.getEndDate());
            }
            chip.setText(chipText);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                FilterCriteria updated = transactionViewModel.getCurrentFilterCriteria().copy();
                updated.setStartDate(null);
                updated.setEndDate(null);
                transactionViewModel.applyFilters(updated);
            });
            chipGroupActiveFilters.addView(chip);
        }

        if (criteria.getCategory() != null) {
            Chip chip = new Chip(requireContext());
            chip.setText("Category: " + criteria.getCategory());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                FilterCriteria updated = transactionViewModel.getCurrentFilterCriteria().copy();
                updated.setCategory(null);
                transactionViewModel.applyFilters(updated);
            });
            chipGroupActiveFilters.addView(chip);
        }

        if (criteria.getPaymentMethod() != null) {
            Chip chip = new Chip(requireContext());
            chip.setText("Payment: " + criteria.getPaymentMethod().replace("_", " "));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                FilterCriteria updated = transactionViewModel.getCurrentFilterCriteria().copy();
                updated.setPaymentMethod(null);
                transactionViewModel.applyFilters(updated);
            });
            chipGroupActiveFilters.addView(chip);
        }

        if (criteria.getApprovalStatus() != null) {
            Chip chip = new Chip(requireContext());
            chip.setText(criteria.getApprovalStatus() == 1 ? "Approved" : "Pending");
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                FilterCriteria updated = transactionViewModel.getCurrentFilterCriteria().copy();
                updated.setApprovalStatus(null);
                transactionViewModel.applyFilters(updated);
            });
            chipGroupActiveFilters.addView(chip);
        }

        if (criteria.getMemberSearchQuery() != null && !criteria.getMemberSearchQuery().isEmpty()) {
            Chip chip = new Chip(requireContext());
            chip.setText("Member: " + criteria.getMemberSearchQuery());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                FilterCriteria updated = transactionViewModel.getCurrentFilterCriteria().copy();
                updated.setMemberSearchQuery(null);
                transactionViewModel.applyFilters(updated);
            });
            chipGroupActiveFilters.addView(chip);
        }
    }

}
