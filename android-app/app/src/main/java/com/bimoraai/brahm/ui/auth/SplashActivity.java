package com.bimoraai.brahm.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.bimoraai.brahm.ui.main.MainActivity;
import com.bimoraai.brahm.utils.PrefsHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Keep splash on screen briefly (can use animate if needed)
        splashScreen.setKeepOnScreenCondition(() -> false);

        // Route: logged in → MainActivity, else → LoginActivity
        PrefsHelper prefs = new PrefsHelper(this);
        if (prefs.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
