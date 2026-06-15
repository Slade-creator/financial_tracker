package com.studentassoc.financialtracker.View;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studentassoc.financialtracker.Model.CategorySummary;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;

import java.util.List;
import java.util.Locale;

public class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder> {

    private List<CategorySummary> categories;

    public CategoryBreakdownAdapter(List<CategorySummary> categories) {
        this.categories = categories;
    }

    public void setCategories(List<CategorySummary> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_breakdown, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySummary category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvCategoryAmount, tvCategoryPercentage, tvTransactionCount;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercentage = itemView.findViewById(R.id.tvCategoryPercentage);
            tvTransactionCount = itemView.findViewById(R.id.tvTransactionCount);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        void bind(CategorySummary category) {
            tvCategoryName.setText(category.getCategory());
            tvCategoryAmount.setText(Utils.toKwacha(category.getTotalAmount()));
            tvCategoryPercentage.setText(String.format(Locale.ENGLISH,
                    "%.1f%%", category.getPercentageOfTotal()));
            tvTransactionCount.setText(String.format(Locale.ENGLISH,
                    "%d transactions", category.getTransactionCount()));
            progressBar.setProgress((int) category.getPercentageOfTotal());
        }
    }
}
