package com.bimoraai.brahm.ui.main

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.ui.auth.LoginScreen
import com.bimoraai.brahm.ui.auth.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// ─── Route constants ──────────────────────────────────────────────────────────
object Route {
    const val ONBOARDING = "onboarding"
    const val LOGIN      = "login"
    const val MAIN       = "main"
    const val KUNDALI    = "kundali"
    const val CHAT       = "chat"
    const val GOCHAR     = "gochar"
    const val COMPATIBILITY = "compatibility"
    const val MUHURTA    = "muhurta"
    const val SADE_SATI  = "sade_sati"
    const val DOSHA      = "dosha"
    const val GEMSTONE   = "gemstone"
    const val KP         = "kp"
    const val PRASHNA    = "prashna"
    const val VARSHPHAL  = "varshphal"
    const val RECTIFICATION = "rectification"
    const val PALMISTRY  = "palmistry"
    const val HOROSCOPE  = "horoscope"
    const val PROFILE         = "profile"
    const val PROFILE_EDIT    = "profile_edit"
    const val BILLING         = "billing"
    // New screens
    const val PANCHANG        = "panchang"
    const val RASHI           = "rashi"
    const val NAKSHATRA       = "nakshatra"
    const val YOGAS           = "yogas"
    const val REMEDIES        = "remedies"
    const val MANTRA          = "mantra"
    const val GOTRA           = "gotra"
    const val STORIES         = "stories"
    const val SKY             = "sky"
    const val LIBRARY         = "library"
}

@Composable
fun AppNavHost(tokenDataStore: TokenDataStore = androidx.hilt.navigation.compose.hiltViewModel<MainViewModel>().tokenDataStore) {
    val navController = rememberNavController()

    // Determine start destination based on stored token
    val hasToken = remember {
        runBlocking { tokenDataStore.accessToken.firstOrNull() } != null
    }
    val startDestination = if (hasToken) Route.MAIN else Route.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Route.LOGIN) {
                    popUpTo(Route.ONBOARDING) { inclusive = true }
                }}
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.navigate(Route.MAIN) {
                    popUpTo(Route.LOGIN) { inclusive = true }
                }}
            )
        }
        composable(Route.MAIN) {
            MainScreen(navController = navController)
        }
        composable(Route.GOCHAR)         { com.bimoraai.brahm.ui.gochar.GocharScreen(navController) }
        composable(Route.COMPATIBILITY)  { com.bimoraai.brahm.ui.compatibility.CompatibilityScreen(navController) }
        composable(Route.MUHURTA)        { com.bimoraai.brahm.ui.muhurta.MuhurtaScreen(navController) }
        composable(Route.SADE_SATI)      { com.bimoraai.brahm.ui.sadesati.SadeSatiScreen(navController) }
        composable(Route.DOSHA)          { com.bimoraai.brahm.ui.dosha.DoshaScreen(navController) }
        composable(Route.GEMSTONE)       { com.bimoraai.brahm.ui.gemstone.GemstoneScreen(navController) }
        composable(Route.KP)             { com.bimoraai.brahm.ui.kp.KPScreen(navController) }
        composable(Route.PRASHNA)        { com.bimoraai.brahm.ui.prashna.PrashnaScreen(navController) }
        composable(Route.VARSHPHAL)      { com.bimoraai.brahm.ui.varshphal.VarshpalScreen(navController) }
        composable(Route.RECTIFICATION)  { com.bimoraai.brahm.ui.rectification.RectificationScreen(navController) }
        composable(Route.PALMISTRY)      { com.bimoraai.brahm.ui.palmistry.PalmistryScreen(navController) }
        composable(Route.HOROSCOPE)      { com.bimoraai.brahm.ui.horoscope.HoroscopeScreen(navController) }
        composable(Route.PROFILE)        { com.bimoraai.brahm.ui.profile.ProfileScreen(navController) }
        composable(Route.PROFILE_EDIT)   { com.bimoraai.brahm.ui.profile.ProfileEditScreen(navController) }
        composable(Route.BILLING)        { com.bimoraai.brahm.ui.profile.BillingScreen(navController) }
        composable(Route.PANCHANG)       { com.bimoraai.brahm.ui.panchang.PanchangScreen(navController) }
        composable(Route.RASHI)          { com.bimoraai.brahm.ui.rashi.RashiScreen(navController) }
        composable(Route.NAKSHATRA)      { com.bimoraai.brahm.ui.nakshatra.NakshatraScreen(navController) }
        composable(Route.YOGAS)          { com.bimoraai.brahm.ui.yogas.YogasScreen(navController) }
        composable(Route.REMEDIES)       { com.bimoraai.brahm.ui.remedies.RemediesScreen(navController) }
        composable(Route.MANTRA)         { com.bimoraai.brahm.ui.mantra.MantraScreen(navController) }
        composable(Route.GOTRA)          { com.bimoraai.brahm.ui.gotra.GotraScreen(navController) }
        composable(Route.STORIES)        { com.bimoraai.brahm.ui.stories.StoriesScreen(navController) }
        composable(Route.SKY)            { com.bimoraai.brahm.ui.sky.SkyScreen(navController) }
        composable(Route.LIBRARY)        { com.bimoraai.brahm.ui.library.LibraryScreen(navController) }
    }
}
