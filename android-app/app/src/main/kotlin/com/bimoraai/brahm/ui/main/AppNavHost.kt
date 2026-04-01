package com.bimoraai.brahm.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bimoraai.brahm.core.components.WithAiFab
import com.bimoraai.brahm.core.theme.BrahmBackground
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.ui.auth.LoginScreen
import com.bimoraai.brahm.ui.auth.OnboardingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.runtime.collectAsState

// ─── Route constants ──────────────────────────────────────────────────────────
object Route {
    const val ONBOARDING   = "onboarding"
    const val LOGIN        = "login"
    const val MAIN         = "main"
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
    const val CALENDAR           = "calendar"
    const val ARCHIVED_CHATS     = "archived_chats"
    const val ABOUT              = "about"
}

// Routes that are accessible without a valid token (auth screens)
private val authRoutes = setOf(Route.ONBOARDING, Route.LOGIN)

@Composable
fun AppNavHost(tokenDataStore: TokenDataStore = androidx.hilt.navigation.compose.hiltViewModel<MainViewModel>().tokenDataStore) {
    val navController = rememberNavController()

    // ── Instant start destination (no blank-screen flash) ─────────────────────
    // YouTube / ChatGPT / Grok pattern:
    //   1. Read sync SharedPreferences flag → route decided in first frame, no coroutine.
    //   2. Token refresh runs silently in background AFTER the UI is already visible.
    //   3. If refresh fails with 401 → auth guard redirects to LOGIN.
    val startDestination = remember {
        if (tokenDataStore.hasTokenSync()) Route.MAIN else Route.ONBOARDING
    }

    // ── Global auth guard ──────────────────────────────────────────────────────
    // Token refresh is handled EXCLUSIVELY by ApiClient's mutex-protected OkHttp
    // authenticator. Having a second independent refresh path here caused a race:
    // both callers would use the refresh token simultaneously → server rotates it →
    // the slower caller got 401 on the refresh → called clear() → random logout.
    //
    // Now: only one path refreshes tokens. This guard just watches for explicit
    // clear() (real logout / genuinely expired refresh token confirmed by server).
    val accessToken by tokenDataStore.accessToken.collectAsState(initial = "loading")
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    var guardActive by remember { mutableStateOf(false) }

    // Activate only after DataStore emits a real value — "loading" sentinel prevents
    // false triggers while DataStore initialises on first app open.
    LaunchedEffect(accessToken) {
        if (accessToken != "loading") guardActive = true
    }

    LaunchedEffect(accessToken) {
        if (!guardActive) return@LaunchedEffect
        if (accessToken == null && currentRoute != null && currentRoute !in authRoutes) {
            // Small stabilisation delay: DataStore writes are async, and saveTokens()
            // clears then re-saves — avoid firing on the brief null window mid-write.
            delay(300)
            val confirmedNull = tokenDataStore.accessToken.firstOrNull()
            if (confirmedNull == null) {
                navController.navigate(Route.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        enterTransition    = { fadeIn(tween(180)) },
        exitTransition     = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition  = { fadeOut(tween(120)) },
    ) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Route.LOGIN) {
                    popUpTo(Route.ONBOARDING) { inclusive = true }
                }}
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                onLoggedIn = { _ ->
                    navController.navigate(Route.MAIN) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.MAIN) {
            MainScreen(navController = navController)
        }
        composable(Route.GOCHAR)         { WithAiFab("gochar")         { com.bimoraai.brahm.ui.gochar.GocharScreen(navController) } }
        composable(Route.COMPATIBILITY)  { WithAiFab("compatibility")  { com.bimoraai.brahm.ui.compatibility.CompatibilityScreen(navController) } }
        composable(Route.MUHURTA)        { WithAiFab("muhurta")        { com.bimoraai.brahm.ui.muhurta.MuhurtaScreen(navController) } }
        composable(Route.SADE_SATI)      { WithAiFab("sade_sati")      { com.bimoraai.brahm.ui.sadesati.SadeSatiScreen(navController) } }
        composable(Route.DOSHA)          { WithAiFab("dosha")          { com.bimoraai.brahm.ui.dosha.DoshaScreen(navController) } }
        composable(Route.GEMSTONE)       { WithAiFab("gemstone")       { com.bimoraai.brahm.ui.gemstone.GemstoneScreen(navController) } }
        composable(Route.KP)             { WithAiFab("kp")             { com.bimoraai.brahm.ui.kp.KPScreen(navController) } }
        composable(Route.PRASHNA)        { WithAiFab("prashna")        { com.bimoraai.brahm.ui.prashna.PrashnaScreen(navController) } }
        composable(Route.VARSHPHAL)      { WithAiFab("varshphal")      { com.bimoraai.brahm.ui.varshphal.VarshpalScreen(navController) } }
        composable(Route.RECTIFICATION)  { WithAiFab("rectification")  { com.bimoraai.brahm.ui.rectification.RectificationScreen(navController) } }
        composable(Route.PALMISTRY)      { WithAiFab("palmistry")      { com.bimoraai.brahm.ui.palmistry.PalmistryScreen(navController) } }
        composable(Route.HOROSCOPE)      { WithAiFab("horoscope")      { com.bimoraai.brahm.ui.horoscope.HoroscopeScreen(navController) } }
        composable(Route.PROFILE)        { com.bimoraai.brahm.ui.profile.ProfileScreen(navController) }
        composable(Route.BILLING)        { com.bimoraai.brahm.ui.profile.BillingScreen(navController) }
        composable(Route.PANCHANG)       { com.bimoraai.brahm.ui.panchang.PanchangScreen(navController) }
        composable(Route.RASHI)          { WithAiFab("rashi")          { com.bimoraai.brahm.ui.rashi.RashiScreen(navController) } }
        composable(Route.NAKSHATRA)      { WithAiFab("nakshatra")      { com.bimoraai.brahm.ui.nakshatra.NakshatraScreen(navController) } }
        composable(Route.YOGAS)          { WithAiFab("yogas")          { com.bimoraai.brahm.ui.yogas.YogasScreen(navController) } }
        composable(Route.REMEDIES)       { WithAiFab("remedies")       { com.bimoraai.brahm.ui.remedies.RemediesScreen(navController) } }
        composable(Route.MANTRA)         { WithAiFab("mantra")         { com.bimoraai.brahm.ui.mantra.MantraScreen(navController) } }
        composable(Route.GOTRA)          { WithAiFab("gotra")          { com.bimoraai.brahm.ui.gotra.GotraScreen(navController) } }
        composable(Route.STORIES)        { WithAiFab("stories")        { com.bimoraai.brahm.ui.stories.StoriesScreen(navController) } }
        composable(Route.SKY)            { WithAiFab("sky")            { com.bimoraai.brahm.ui.sky.SkyScreen(navController) } }
        composable(Route.LIBRARY)        { WithAiFab("library")        { com.bimoraai.brahm.ui.library.LibraryScreen(navController) } }
        composable(Route.CALENDAR)       { WithAiFab("calendar")       { com.bimoraai.brahm.ui.calendar.CalendarScreen(navController) } }
        composable(Route.ARCHIVED_CHATS) { com.bimoraai.brahm.ui.chat.ArchivedChatsScreen(navController) }
        composable(Route.ABOUT)          { com.bimoraai.brahm.ui.about.AboutScreen(navController) }
        composable("legal/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
            com.bimoraai.brahm.ui.about.LegalDocScreen(navController = navController, docIndex = index)
        }
    }
}
