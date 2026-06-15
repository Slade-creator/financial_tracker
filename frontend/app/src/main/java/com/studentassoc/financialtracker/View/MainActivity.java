package com.studentassoc.financialtracker.View;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studentassoc.financialtracker.R;

public class MainActivity extends BaseActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();

        // Wire bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int navBarHeight = insets
                    .getInsets(WindowInsetsCompat.Type.navigationBars())
                    .bottom;
            v.setPadding(0, 0, 0, navBarHeight);
            return insets;
        });
        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}