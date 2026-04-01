package com.bimoraai.brahm.ui.about

import android.webkit.WebView
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    var legalDoc by remember { mutableStateOf<String?>(null) }

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
                onClick = { legalDoc = "privacy" },
            )
            AboutItem(
                icon = Icons.Default.Description,
                iconColor = Color(0xFF5C6BC0),
                label = "Terms of Use",
                onClick = { legalDoc = "terms" },
            )
            AboutItem(
                icon = Icons.Default.Cancel,
                iconColor = Color(0xFFE53935),
                label = "Cancellation Policy",
                onClick = { legalDoc = "cancellation" },
            )

            Spacer(Modifier.weight(1f))

            // App version at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = BrahmGold, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Brahm AI", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = BrahmForeground))
                    Text("Version $versionName", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }

    // Legal doc bottom sheet
    legalDoc?.let { doc ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val title = when (doc) {
            "privacy" -> "Privacy Policy"; "terms" -> "Terms of Use"; else -> "Cancellation Policy"
        }
        val assetFile = when (doc) {
            "privacy" -> "privacy_policy.html"; "terms" -> "terms_of_use.html"; else -> "cancellation_policy.html"
        }
        ModalBottomSheet(
            onDismissRequest = { legalDoc = null },
            sheetState = sheetState,
            containerColor = BrahmCard,
            dragHandle = {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .background(BrahmMutedForeground.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                }
            },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                HorizontalDivider(color = BrahmBorder)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            isVerticalScrollBarEnabled = true
                            loadUrl("file:///android_asset/$assetFile")
                        }
                    },
                    update = { webView ->
                        // Reload when switching between docs (privacy → terms → cancellation)
                        val current = webView.url ?: ""
                        if (!current.endsWith(assetFile)) {
                            webView.loadUrl("file:///android_asset/$assetFile")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                // Shrinks WebView by nav bar height so its bottom sits above the nav bar.
                // Without this, WebView extends behind the nav bar and last lines are hidden.
                Spacer(Modifier.navigationBarsPadding())
            }
        }
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        Modifier
                            .padding(0.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
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
