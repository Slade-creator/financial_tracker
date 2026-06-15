package com.studentassoc.financialtracker.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ReportsPagerAdapter extends FragmentStateAdapter {

    public ReportsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new WeeklySummaryFragment();
            case 1: return new TermsSummaryFragment();
            case 2: return new AIInsightsFragment();
            default: throw new IllegalStateException("Unexpected tab position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
