package com.bimoraai.brahm.ui.about

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.bimoraai.brahm.core.theme.*

// ── Legal doc index → (asset file, screen title) ─────────────────────────────
private val legalDocs = listOf(
    "privacy_policy.html"    to "Privacy Policy",
    "terms_of_use.html"      to "Terms of Use",
    "cancellation_policy.html" to "Cancellation Policy",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0" }
        catch (_: Exception) { "1.0" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrahmBackground,
                    titleContentColor = BrahmForeground,
                    navigationIconContentColor = BrahmForeground,
                ),
            )
        },
        containerColor = BrahmBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AboutItem(
                icon = Icons.Default.PrivacyTip,
                iconColor = Color(0xFF546E7A),
                label = "Privacy Policy",
                onClick = { navController.navigate("legal/0") },
            )
            AboutItem(
                icon = Icons.Default.Description,
                iconColor = Color(0xFF5C6BC0),
                label = "Terms of Use",
                onClick = { navController.navigate("legal/1") },
            )
            AboutItem(
                icon = Icons.Default.Cancel,
                iconColor = Color(0xFFE53935),
                label = "Cancellation Policy",
                onClick = { navController.navigate("legal/2") },
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = BrahmGold, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Brahm AI", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = BrahmForeground))
                    Text("Version $versionName", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
}

// ── Full-screen legal document viewer ────────────────────────────────────────
// One dedicated Scaffold per doc — WebView fills the entire safe area with
// padding(innerPadding). No ModalBottomSheet, no inset juggling.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocScreen(navController: NavController, docIndex: Int) {
    val (assetFile, title) = legalDocs.getOrElse(docIndex) { legalDocs[0] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrahmBackground,
                    titleContentColor = BrahmForeground,
                    navigationIconContentColor = BrahmForeground,
                ),
            )
        },
        containerColor = BrahmBackground,
    ) { innerPadding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    isVerticalScrollBarEnabled = true
                    loadUrl("file:///android_asset/$assetFile")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = BrahmCard,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = BrahmForeground),
            )
            Icon(Icons.Default.ChevronRight, null, tint = BrahmMutedForeground, modifier = Modifier.size(20.dp))
        }
    }
}
