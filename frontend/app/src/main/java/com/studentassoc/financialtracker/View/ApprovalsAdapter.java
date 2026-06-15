package com.studentassoc.financialtracker.View;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ApprovalsAdapter extends RecyclerView.Adapter<ApprovalsAdapter.ApprovalViewHolder> {

    public interface OnApprovalActionListener {
        void onApprove(Transaction transaction);
        void onReject(Transaction transaction);
        void onViewDetails(Transaction transaction);
    }

    private List<Transaction> transactions;
    private final OnApprovalActionListener listener;

    public ApprovalsAdapter(List<Transaction> transactions, OnApprovalActionListener listener) {
        this.listener = listener;
        this.transactions = transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = new ArrayList<>(transactions);
        notifyDataSetChanged();
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @NonNull
    @Override
    public ApprovalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_approval, parent, false);
        return new ApprovalViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ApprovalViewHolder holder, int position) {
        holder.bind(transactions.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
    static class ApprovalViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvApprovalType;
        private final TextView tvApprovalAmount;
        private final TextView tvApprovalCategory;
        private final TextView tvApprovalMember;
        private final TextView tvApprovalDate;
        private final TextView tvApprovalPayment;
        private final MaterialButton btnApprove;
        private final MaterialButton btnReject;

        ApprovalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApprovalType     = itemView.findViewById(R.id.tvApprovalType);
            tvApprovalAmount   = itemView.findViewById(R.id.tvApprovalAmount);
            tvApprovalCategory = itemView.findViewById(R.id.tvApprovalCategory);
            tvApprovalMember   = itemView.findViewById(R.id.tvApprovalMember);
            tvApprovalDate     = itemView.findViewById(R.id.tvApprovalDate);
            tvApprovalPayment  = itemView.findViewById(R.id.tvApprovalPayment);
            btnApprove         = itemView.findViewById(R.id.btnApprove);
            btnReject          = itemView.findViewById(R.id.btnReject);
        }

        void bind(Transaction transaction, OnApprovalActionListener listener) {
            tvApprovalType.setText(transaction.getTransactionType());
            tvApprovalAmount.setText(Utils.toKwacha(transaction.getAmount()));
            tvApprovalCategory.setText(transaction.getCategory());
            tvApprovalDate.setText(Utils.formatDateForDisplay(transaction.getTransactionDate()));
            tvApprovalPayment.setText(transaction.getPaymentMethod().replace("_", " "));

            String member = transaction.getMemberName();
            if (member != null && !member.isEmpty()) {
                tvApprovalMember.setText(member);
                tvApprovalMember.setVisibility(View.VISIBLE);
            } else {
                tvApprovalMember.setVisibility(View.GONE);
            }

            int color = transaction.getTransactionType().equals("INCOME")
                    ? ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark)
                    : ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
            tvApprovalType.setTextColor(color);
            tvApprovalAmount.setTextColor(color);

            btnApprove.setOnClickListener(v -> listener.onApprove(transaction));
            btnReject.setOnClickListener(v  -> listener.onReject(transaction));
            itemView.setOnClickListener(v   -> listener.onViewDetails(transaction));
        }
    }
}

