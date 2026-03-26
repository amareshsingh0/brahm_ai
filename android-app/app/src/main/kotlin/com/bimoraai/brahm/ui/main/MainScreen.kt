package com.bimoraai.brahm.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.LiveHelp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.chat.ChatScreen
import com.bimoraai.brahm.ui.kundali.KundaliScreen
import com.bimoraai.brahm.ui.more.MoreScreen
import com.bimoraai.brahm.ui.profile.ProfileViewModel
import com.bimoraai.brahm.ui.profile.ProfileScreen
import com.bimoraai.brahm.ui.today.TodayScreen
import kotlinx.coroutines.launch

// ─── Drawer nav item ──────────────────────────────────────────────────────────
data class DrawerItem(val route: String, val label: String, val icon: ImageVector)

private val mainItems = listOf(
    DrawerItem("tab_today",      "Dashboard",       Icons.Default.Dashboard),
    DrawerItem("tab_chat",       "Brahm AI Chat",   Icons.AutoMirrored.Filled.Chat),
    DrawerItem("tab_kundali",    "My Kundli",       Icons.Default.Stars),
    DrawerItem(Route.HOROSCOPE,  "Daily Horoscope", Icons.Default.WbSunny),
    DrawerItem(Route.PANCHANG,   "Full Panchang",   Icons.Default.CalendarViewDay),
    DrawerItem(Route.SKY,        "Live Sky",        Icons.Default.NightsStay),
    DrawerItem(Route.STORIES,    "Vedic Stories",   Icons.AutoMirrored.Filled.MenuBook),
)

private val exploreItems = listOf(
    DrawerItem(Route.GOCHAR,         "Gochar (Transits)",  Icons.Default.Explore),
    DrawerItem(Route.COMPATIBILITY,  "Compatibility",      Icons.Default.Favorite),
    DrawerItem(Route.PALMISTRY,      "Palmistry",          Icons.Default.FrontHand),
    DrawerItem(Route.SADE_SATI,      "Sade Sati",          Icons.Default.Timelapse),
    DrawerItem(Route.DOSHA,          "Dosha Check",        Icons.Default.HealthAndSafety),
    DrawerItem(Route.GEMSTONE,       "Gemstone Guide",     Icons.Default.Diamond),
    DrawerItem(Route.MUHURTA,        "Muhurta",            Icons.Default.Schedule),
    DrawerItem(Route.KP,             "KP System",          Icons.Default.Science),
    DrawerItem(Route.PRASHNA,        "Prashna",            Icons.AutoMirrored.Filled.LiveHelp),
    DrawerItem(Route.VARSHPHAL,      "Varshphal",          Icons.Default.CalendarMonth),
    DrawerItem(Route.RECTIFICATION,  "Rectification",      Icons.Default.Analytics),
    DrawerItem(Route.RASHI,          "Rashi Explorer",     Icons.Default.Brightness5),
    DrawerItem(Route.NAKSHATRA,      "Nakshatra",          Icons.Default.Stars),
    DrawerItem(Route.YOGAS,          "Yogas",              Icons.Default.AutoAwesome),
    DrawerItem(Route.REMEDIES,       "Remedies",           Icons.Default.Healing),
    DrawerItem(Route.MANTRA,         "Mantras",            Icons.Default.MusicNote),
    DrawerItem(Route.LIBRARY,        "Vedic Library",      Icons.AutoMirrored.Filled.LibraryBooks),
    DrawerItem(Route.GOTRA,          "Gotra Finder",       Icons.Default.AccountTree),
)

// ─── Main Screen with Drawer nav ──────────────────────────────────────────────
@Composable
fun MainScreen(navController: NavController, tokenDataStore: TokenDataStore? = null) {
    val profileVm: ProfileViewModel = hiltViewModel()
    val user by profileVm.user.collectAsState()

    val tabNavController = rememberNavController()
    val currentEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun openDrawer() { scope.launch { drawerState.open() } }
    fun closeDrawer() { scope.launch { drawerState.close() } }

    fun navigate(route: String) {
        closeDrawer()
        // Tab routes stay inside the inner NavHost
        if (route.startsWith("tab_")) {
            tabNavController.navigate(route) {
                popUpTo("tab_today") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            // Full-screen routes go to outer NavController
            navController.navigate(route)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color(0xFFF0F0F2),
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
            ) {
                DrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { navigate(it) },
                    onClose = { closeDrawer() },
                    onProfileClick = { navigate(Route.PROFILE) },
                    userName = user?.name?.takeIf { it.isNotBlank() } ?: user?.phone ?: "User",
                    userPlan = "Free",
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(18.dp))
                            Text(
                                "Brahm AI",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = BrahmGold,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = BrahmForeground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White,
                    ),
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = tabNavController,
                startDestination = "tab_today",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("tab_today")   { TodayScreen(navController, tabNavController) }
                composable("tab_kundali") { KundaliScreen(navController) }
                composable("tab_chat")    { ChatScreen() }
                composable("tab_more")    { MoreScreen(navController) }
                composable("tab_profile") { ProfileScreen(navController) }
            }
        }
    }
}

// ─── Drawer Content ───────────────────────────────────────────────────────────
@Composable
private fun DrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onProfileClick: () -> Unit,
    userName: String = "User",
    userPlan: String = "Free",
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Nightlight, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(22.dp))
                Text(
                    "BRAHM AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = BrahmGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Menu, contentDescription = "Close", tint = BrahmMutedForeground)
            }
        }

        Spacer(Modifier.height(4.dp))

        // MAIN section
        DrawerSectionLabel("MAIN")
        mainItems.forEach { item ->
            DrawerNavItem(
                item = item,
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
            )
        }

        Spacer(Modifier.height(16.dp))

        // EXPLORE section
        DrawerSectionLabel("EXPLORE")
        exploreItems.forEach { item ->
            DrawerNavItem(
                item = item,
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
            )
        }

        Spacer(Modifier.weight(1f))

        // User card at bottom
        HorizontalDivider(color = BrahmBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrahmGold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(2).uppercase(),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    )
                }
                Column {
                    Text(userName, style = MaterialTheme.typography.titleSmall)
                    Text(userPlan, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = BrahmMutedForeground)
        }
    }
}

@Composable
private fun DrawerSectionLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = BrahmMutedForeground,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
    )
}

@Composable
private fun DrawerNavItem(item: DrawerItem, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Color(0xFFE8DFC8) else Color.Transparent
    val contentColor = if (selected) BrahmGold else BrahmForeground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Gold selection indicator bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrahmGold else Color.Transparent),
        )
        Spacer(Modifier.width(4.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(item.icon, contentDescription = item.label, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(item.label, style = MaterialTheme.typography.bodyMedium.copy(color = contentColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
        }
    }
}
