package com.bimoraai.brahm.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bimoraai.brahm.core.theme.BrahmBorder
import com.bimoraai.brahm.core.theme.BrahmGold
import com.bimoraai.brahm.core.theme.BrahmMutedForeground
import com.bimoraai.brahm.ui.chat.ChatScreen
import com.bimoraai.brahm.ui.kundali.KundaliScreen
import com.bimoraai.brahm.ui.more.MoreScreen
import com.bimoraai.brahm.ui.profile.ProfileScreen
import com.bimoraai.brahm.ui.today.TodayScreen

// ─── Bottom nav tabs ──────────────────────────────────────────────────────────
sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    object Today   : BottomTab("tab_today",   "Today",   Icons.Default.WbSunny)
    object Kundali : BottomTab("tab_kundali", "Kundali", Icons.Default.AutoAwesome)
    object Chat    : BottomTab("tab_chat",    "Ask AI",  Icons.Default.AutoAwesomeMosaic)
    object More    : BottomTab("tab_more",    "More",    Icons.Default.GridView)
    object Profile : BottomTab("tab_profile", "Profile", Icons.Default.Person)
}

private val bottomTabs = listOf(
    BottomTab.Today,
    BottomTab.Kundali,
    BottomTab.Chat,
    BottomTab.More,
    BottomTab.Profile,
)

@Composable
fun MainScreen(navController: NavController) {
    val tabNavController = rememberNavController()
    val currentEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White,
                tonalElevation = 0.dp,
            ) {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrahmGold,
                            selectedTextColor = BrahmGold,
                            unselectedIconColor = BrahmMutedForeground,
                            unselectedTextColor = BrahmMutedForeground,
                            indicatorColor = androidx.compose.ui.graphics.Color(0xFFFFF8E7),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = BottomTab.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomTab.Today.route)   { TodayScreen(navController) }
            composable(BottomTab.Kundali.route) { KundaliScreen(navController) }
            composable(BottomTab.Chat.route)    { ChatScreen() }
            composable(BottomTab.More.route)    { MoreScreen(navController) }
            composable(BottomTab.Profile.route) { ProfileScreen(navController) }
        }
    }
}
