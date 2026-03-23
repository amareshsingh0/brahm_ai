package com.bimoraai.brahm.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.databinding.ActivityMainBinding;
import com.bimoraai.brahm.ui.secondary.CompatibilityActivity;
import com.bimoraai.brahm.ui.secondary.DoshaActivity;
import com.bimoraai.brahm.ui.secondary.GemstoneActivity;
import com.bimoraai.brahm.ui.secondary.GocharActivity;
import com.bimoraai.brahm.ui.secondary.HoroscopeActivity;
import com.bimoraai.brahm.ui.secondary.KPActivity;
import com.bimoraai.brahm.ui.secondary.MuhurtaActivity;
import com.bimoraai.brahm.ui.secondary.PalmistryActivity;
import com.bimoraai.brahm.ui.secondary.PrashnaActivity;
import com.bimoraai.brahm.ui.secondary.RectificationActivity;
import com.bimoraai.brahm.ui.secondary.SadeSatiActivity;
import com.bimoraai.brahm.ui.secondary.VarshpalActivity;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding b;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Setup Navigation Component with Bottom Nav
        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(b.bottomNavView, navController);
        }

        // Setup Navigation Drawer
        b.navigationView.setNavigationItemSelectedListener(this);

        // Highlight "Home" as default selected in drawer
        b.navigationView.setCheckedItem(R.id.nav_home);

        // Floating AI button → navigate to Chat
        b.fabAiContainer.setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.chatFragment);
        });

        // Hide FAB when on Chat screen (already there)
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, args) -> {
                boolean onChat = destination.getId() == R.id.chatFragment;
                b.fabAiContainer.setVisibility(onChat ? android.view.View.GONE : android.view.View.VISIBLE);
            });
        }

        // Close drawer when tapping the scrim
        b.drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Optional: reset any state when drawer closes
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Bottom nav tabs — use NavController
        if (id == R.id.nav_home) {
            navController.navigate(R.id.homeFragment);
        } else if (id == R.id.nav_chat) {
            navController.navigate(R.id.chatFragment);
        } else if (id == R.id.nav_panchang) {
            navController.navigate(R.id.todayFragment);
        } else if (id == R.id.nav_kundali) {
            navController.navigate(R.id.kundaliFragment);
        } else if (id == R.id.nav_profile) {
            navController.navigate(R.id.profileFragment);
        }
        // Secondary screens — open as Activities
        else if (id == R.id.nav_gochar) {
            startActivity(new Intent(this, GocharActivity.class));
        } else if (id == R.id.nav_compatibility) {
            startActivity(new Intent(this, CompatibilityActivity.class));
        } else if (id == R.id.nav_horoscope) {
            startActivity(new Intent(this, HoroscopeActivity.class));
        } else if (id == R.id.nav_muhurta) {
            startActivity(new Intent(this, MuhurtaActivity.class));
        } else if (id == R.id.nav_sade_sati) {
            startActivity(new Intent(this, SadeSatiActivity.class));
        } else if (id == R.id.nav_dosha) {
            startActivity(new Intent(this, DoshaActivity.class));
        } else if (id == R.id.nav_kp) {
            startActivity(new Intent(this, KPActivity.class));
        } else if (id == R.id.nav_prashna) {
            startActivity(new Intent(this, PrashnaActivity.class));
        } else if (id == R.id.nav_varshphal) {
            startActivity(new Intent(this, VarshpalActivity.class));
        } else if (id == R.id.nav_rectification) {
            startActivity(new Intent(this, RectificationActivity.class));
        } else if (id == R.id.nav_palmistry) {
            startActivity(new Intent(this, PalmistryActivity.class));
        } else if (id == R.id.nav_gemstone) {
            startActivity(new Intent(this, GemstoneActivity.class));
        }

        b.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /** Called by fragments to open the navigation drawer. */
    public void openDrawer() {
        b.drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && (navController.navigateUp() || super.onSupportNavigateUp());
    }
}
