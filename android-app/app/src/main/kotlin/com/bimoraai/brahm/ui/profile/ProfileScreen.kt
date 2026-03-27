package com.bimoraai.brahm.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.ui.main.Route
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user by vm.user.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val savedLang = remember {
        context.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)
            .getString("language", null)
    }
    val currentLangLabel = when (savedLang) {
        "hi" -> "हिंदी (Hindi)"
        "en" -> "English"
        else -> "System Default"
    }

    val plan = user?.plan ?: "free"
    val isPaid    = plan != "free"
    val planColor = if (isPaid) BrahmGold else BrahmMutedForeground
    val planBg    = if (isPaid) BrahmGold.copy(alpha = 0.12f) else Color(0xFFE5E7EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmCard),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {

            // ── User card (top, like Grok) ──────────────────────────────────────
            Surface(color = BrahmCard) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BrahmGold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (user?.name?.firstOrNull() ?: "U").toString().uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.name ?: "User", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(user?.phone ?: user?.email ?: "—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                    // Plan badge
                    Surface(shape = RoundedCornerShape(20.dp), color = planBg) {
                        Text(
                            text = plan.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium.copy(color = planColor, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── ACCOUNT section ────────────────────────────────────────────────
            SettingsSectionLabel("Account")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF5C6BC0),
                    label = "Profile",
                    subtitle = "Name, birth details, gender",
                    onClick = { navController.navigate(Route.PROFILE_EDIT) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.CreditCard,
                    iconColor = BrahmGold,
                    label = "Billing",
                    subtitle = if (isPaid) "${plan.replaceFirstChar { it.uppercase() }} plan active" else "Free plan · Upgrade available",
                    onClick = { navController.navigate(Route.BILLING) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── PREFERENCES section ─────────────────────────────────────────────
            SettingsSectionLabel("Preferences")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFF43A047),
                    label = "Notifications",
                    subtitle = "Manage app notifications",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconColor = Color(0xFF7B1FA2),
                    label = "Language",
                    subtitle = currentLangLabel,
                    onClick = { showLanguageDialog = true },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    iconColor = Color(0xFF37474F),
                    label = "Dark Mode",
                    subtitle = "Coming soon — light theme active",
                    onClick = { /* Phase 2 */ },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF00ACC1),
                    label = "Share App",
                    subtitle = "Invite friends to Brahm AI",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Try Brahm AI — Vedic Astrology App")
                            putExtra(Intent.EXTRA_TEXT, "Check out Brahm AI — AI-powered Vedic astrology! 🌙✨\nhttps://brahmasmi.bimoraai.com")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Brahm AI"))
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── CHATS section ────────────────────────────────────────────────────
            SettingsSectionLabel("Chats")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Archive,
                    iconColor = Color(0xFF6D4C41),
                    label = "Archived Chats",
                    subtitle = "View your archived conversations",
                    onClick = { navController.navigate(com.bimoraai.brahm.ui.main.Route.ARCHIVED_CHATS) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── LEGAL section ───────────────────────────────────────────────────
            SettingsSectionLabel("Legal")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF546E7A),
                    label = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://brahmasmi.bimoraai.com/privacy"))
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign Out ────────────────────────────────────────────────────────
            Surface(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEBEE),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text("Sign Out", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        val languages = listOf(
            null   to "System Default",
            "en"   to "English",
            "hi"   to "हिंदी (Hindi)",
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) },
            text = {
                Column {
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    val prefs = context.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)
                                    if (code == null) {
                                        prefs.edit().remove("language").apply()
                                    } else {
                                        prefs.edit().putString("language", code).apply()
                                    }
                                    // Restart activity to apply new locale
                                    (context as? Activity)?.recreate()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = savedLang == code,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = BrahmGold),
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout { navController.navigate(Route.LOGIN) { popUpTo(0) } }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = BrahmMutedForeground,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        ),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrahmCard,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp))
        }
    }
}
