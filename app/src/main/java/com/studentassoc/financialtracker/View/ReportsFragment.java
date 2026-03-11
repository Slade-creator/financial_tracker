package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.studentassoc.financialtracker.R;

public class ReportsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ReportsPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
            ) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        setupViewPager();
    }

    private void setupViewPager() {
        pagerAdapter = new ReportsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Weekly Summary");
                    tab.setIcon(R.drawable.ic_calendar_week);
                    break;
                case 1:
                    tab.setText("Term Summary");
                    tab.setIcon(R.drawable.ic_calendar_range);
                    break;
                case 2:
                    tab.setText("AI Report");
                    tab.setIcon(R.drawable.ic_launcher_foreground);
                    break;
            }
        }).attach();
    }

}
