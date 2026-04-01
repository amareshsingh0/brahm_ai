package com.bimoraai.brahm.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.chat.ChatScreen
import com.bimoraai.brahm.ui.kundali.KundaliScreen
import com.bimoraai.brahm.ui.more.MoreScreen
import com.bimoraai.brahm.ui.profile.ProfileScreen
import com.bimoraai.brahm.ui.today.TodayScreen

// ─── Bottom nav tab definition ────────────────────────────────────────────────
private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val bottomTabs = listOf(
    BottomTab("tab_today",   "Today",   Icons.Default.Home),
    BottomTab("tab_kundali", "Kundali", Icons.Default.Brightness3),
    BottomTab("tab_chat",    "Chat",    Icons.AutoMirrored.Filled.Chat),
    BottomTab("tab_more",    "Explore", Icons.Default.Apps),
    BottomTab("tab_profile", "Profile", Icons.Default.Person),
)

// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun MainScreen(navController: NavController, tokenDataStore: TokenDataStore? = null) {
    val tabNavController = rememberNavController()
    val currentEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val haptic = LocalHapticFeedback.current

    fun navigateTab(route: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        tabNavController.navigate(route) {
            popUpTo("tab_today") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Hide bottom nav when keyboard is open so chat input sits flush to keyboard
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        containerColor = BrahmBackground,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!imeVisible) {
                val userRepo = hiltViewModel<MainViewModel>().userRepository
                val user by userRepo.user.collectAsState()
                BrahmBottomNav(
                    currentRoute = currentRoute,
                    onTabSelected = { navigateTab(it) },
                    avatarUrl = user?.avatar_url,
                    userName = user?.name,
                )
            }
        },
        // Each feature screen has its own PageBotFab with page-specific data & session
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = "tab_today",
            modifier = Modifier
                .fillMaxSize()
                .background(BrahmBackground)
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition    = { fadeIn(tween(180)) + slideInVertically { it / 25 } },
            exitTransition     = { fadeOut(tween(130)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition  = { fadeOut(tween(130)) + slideOutVertically { it / 25 } },
        ) {
            composable("tab_today")   { TodayScreen(navController, onNavigateTab = { navigateTab(it) }) }
            composable("tab_kundali") { KundaliScreen(navController) }
            composable("tab_chat")    { ChatScreen() }
            composable("tab_more")    { MoreScreen(navController) }
            composable("tab_profile") { ProfileScreen(navController) }
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────
@Composable
private fun BrahmBottomNav(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    avatarUrl: String? = null,
    userName: String? = null,
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Top divider — thin 1dp line separating content from nav
            HorizontalDivider(
                thickness = 1.dp,
                color = BrahmBorder,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(62.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    BrahmNavItem(
                        tab = tab,
                        selected = selected,
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier.weight(1f),
                        avatarUrl = if (tab.route == "tab_profile") avatarUrl else null,
                        userName = if (tab.route == "tab_profile") userName else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrahmNavItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    userName: String? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "tabScale",
    )

    val iconColor = if (selected) BrahmGold else Color(0xFF9E9E9E)
    val labelColor = if (selected) BrahmGold else Color(0xFF9E9E9E)

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .clickable(indication = null, interactionSource = interactionSource, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Gold indicator line at top of selected tab
        Box(
            modifier = Modifier
                .width(if (selected) 28.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) BrahmGold else Color.Transparent),
        )
        Spacer(Modifier.height(6.dp))
        if (avatarUrl != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .then(
                        if (selected) Modifier.background(BrahmGold)
                        else Modifier.background(Color(0xFF9E9E9E).copy(alpha = 0.3f))
                    )
                    .padding(1.5.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }
        } else if (tab.route == "tab_profile" && !userName.isNullOrBlank()) {
            // Initials fallback
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (selected) BrahmGold else Color(0xFF9E9E9E)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userName.first().uppercaseChar().toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        } else {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
    }
}

