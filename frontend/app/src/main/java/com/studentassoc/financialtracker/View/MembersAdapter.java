package com.studentassoc.financialtracker.View;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;

import java.util.List;
import java.util.Locale;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {

    public interface onMemberClickListener {
        void onMemberClick(String member);
    }

    private List<Transaction> items;
    private final onMemberClickListener listener;

    public MembersAdapter(List<Transaction> items, onMemberClickListener listener)  {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Transaction> items) {
        this.items = items;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Transaction t = items.get(position);
        String name = t.getMemberName();

        h.tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase(Locale.ROOT));
        h.tvName.setText(name);

        h.tvDate.setText(Utils.formatDateForDisplay(t.getTransactionDate()));
        h.tvPayment.setText(t.getPaymentMethod().replace("_", " "));


        if (t.getNotes() != null && !t.getNotes().isEmpty()) {
            h.tvNotes.setText(t.getNotes());
            h.tvNotes.setVisibility(View.VISIBLE);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onMemberClick(name));

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvDate, tvPayment, tvNotes;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvMemberInitial);
            tvName    = itemView.findViewById(R.id.tvMemberName);
            tvDate    = itemView.findViewById(R.id.tvMemberDate);
            tvPayment = itemView.findViewById(R.id.tvMemberPayment);
            tvNotes   = itemView.findViewById(R.id.tvMemberNotes);
        }
    }
}
