package com.bimoraai.brahm

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.bimoraai.brahm.core.theme.BrahmTheme
import com.bimoraai.brahm.ui.main.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(base: Context) {
        // Only override locale if user has explicitly selected one; otherwise use phone default
        val lang = base.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)
            .getString("language", null)
        if (lang != null) {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(base.createConfigurationContext(config))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // App uses a light background — force dark status bar icons so they're visible
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            BrahmTheme {
                AppNavHost()
            }
        }
    }
}
