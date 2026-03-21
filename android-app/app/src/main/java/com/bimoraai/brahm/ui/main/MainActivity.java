package com.bimoraai.brahm.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

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
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
