package com.studentassoc.financialtracker.View;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;


import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private final OnTransactionClickListener listener;


    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onTransactionLongClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {

        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction, listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public Transaction getTransactionAt(int position) {
        return transactions.get(position);
    }

    public void setTransactions(List<Transaction> newTransactions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return transactions.size();
            }

            @Override
            public int getNewListSize() {
                return newTransactions.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return transactions.get(oldItemPosition).getId()
                        .equals(newTransactions.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Transaction oldTransaction = transactions.get(oldItemPosition);
                Transaction newTransaction = newTransactions.get(newItemPosition);

                return oldTransaction.getId().equals(newTransaction.getId()) &&
                        oldTransaction.getTransactionType().equals(newTransaction.getTransactionType()) &&
                        oldTransaction.getAmount() == newTransaction.getAmount() &&
                        oldTransaction.getCategory().equals(newTransaction.getCategory()) &&
                        oldTransaction.getIsApproved() == newTransaction.getIsApproved() &&
                        oldTransaction.getTransactionDate().equals(newTransaction.getTransactionDate()) &&
                        oldTransaction.getUpdatedAt().equals(newTransaction.getUpdatedAt());
            }
        });

        this.transactions = newTransactions;
        diffResult.dispatchUpdatesTo(this);
    }


   public static class TransactionViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTransactionType;
        private final TextView tvAmount;
        private final TextView tvCategory;
        private final TextView tvMemberName;
        private final TextView tvDate;
        private final TextView tvPaymentMethod;
        private final View statusIndicator;
       private final int COLOR_INCOME  = Color.parseColor("#00FFD1");
       private final int COLOR_EXPENSE = Color.parseColor("#FF3B30");
       private final int COLOR_LABEL   = Color.parseColor("#4DFFFFFF");

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }

        public void bind(Transaction transaction, OnTransactionClickListener listener) {
            tvTransactionType.setText(transaction.getTransactionType());

            String amountText = Utils.toKwacha(transaction.getAmount());
            tvAmount.setText(amountText);

            if ("INCOME".equals(transaction.getTransactionType())) {
                tvAmount.setTextColor(
                        COLOR_INCOME
                );
                tvTransactionType.setTextColor(
                       COLOR_INCOME
                );
            } else {
                tvAmount.setTextColor(
                        COLOR_EXPENSE
                );
                tvTransactionType.setTextColor(
                      COLOR_EXPENSE
                );
            }

            tvCategory.setText(transaction.getCategory());

            if (transaction.getMemberName() != null && !transaction.getMemberName().isEmpty()) {
                tvMemberName.setVisibility(View.VISIBLE);
                tvMemberName.setText(
                        itemView.getContext().getString(R.string.member_label,
                                transaction.getMemberName())
                );
            } else {
                tvMemberName.setVisibility(View.GONE);
            }

            tvDate.setText(Utils.formatDateForDisplay(transaction.getTransactionDate()));

            tvPaymentMethod.setText(transaction.getPaymentMethod().replace("_", " "));

            if (transaction.getIsApproved() == 1) {
                statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_green_light));
            } else {
                statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_orange_light));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionLongClick(transaction);
                    return true;
                }
                return false;
            });
        }
    }
}


