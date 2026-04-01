package com.bimoraai.brahm.ui.more

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

// ─── Tool data ─────────────────────────────────────────────────────────────────
enum class ToolCategory { Analysis, Daily, Knowledge }

data class ToolCard(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val route: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val category: ToolCategory,
)

private val tools = listOf(
    ToolCard(Icons.Default.Explore,                    "Gochar",         "Planetary transits",       Route.GOCHAR,        Color(0xFF6C63FF), Color(0xFF3B2FBF), ToolCategory.Analysis),
    ToolCard(Icons.Default.Favorite,                   "Compatibility",  "Kundali matching",         Route.COMPATIBILITY, Color(0xFFE8445A), Color(0xFFB0203A), ToolCategory.Analysis),
    ToolCard(Icons.Default.Schedule,                   "Muhurta",        "Auspicious timings",       Route.MUHURTA,       Color(0xFF20A090), Color(0xFF0D6B60), ToolCategory.Analysis),
    ToolCard(Icons.Default.Timelapse,                  "Sade Sati",      "Saturn transit",           Route.SADE_SATI,     Color(0xFF5C6BC0), Color(0xFF303F9F), ToolCategory.Analysis),
    ToolCard(Icons.Default.HealthAndSafety,            "Dosha Check",    "Manglik, Kaal Sarpa",      Route.DOSHA,         Color(0xFFE53935), Color(0xFFB71C1C), ToolCategory.Analysis),
    ToolCard(Icons.Default.Diamond,                    "Gemstones",      "Rashi ratna",              Route.GEMSTONE,      Color(0xFF00ACC1), Color(0xFF006978), ToolCategory.Analysis),
    ToolCard(Icons.Default.Science,                    "KP System",      "Krishnamurti Paddhati",    Route.KP,            Color(0xFF43A047), Color(0xFF1B5E20), ToolCategory.Analysis),
    ToolCard(Icons.AutoMirrored.Filled.LiveHelp,       "Prashna",        "Horary astrology",         Route.PRASHNA,       Color(0xFF8E24AA), Color(0xFF4A148C), ToolCategory.Analysis),
    ToolCard(Icons.Default.CalendarMonth,              "Varshphal",      "Solar return chart",       Route.VARSHPHAL,     Color(0xFFE67E22), Color(0xFFB7460C), ToolCategory.Analysis),
    ToolCard(Icons.Default.Analytics,                  "Rectification",  "Birth time correction",    Route.RECTIFICATION, Color(0xFF546E7A), Color(0xFF263238), ToolCategory.Analysis),
    ToolCard(Icons.Default.FrontHand,                  "Palmistry",      "AI palm reading",          Route.PALMISTRY,     Color(0xFFD4A017), Color(0xFF9A6E00), ToolCategory.Analysis),
    ToolCard(Icons.Default.CalendarViewDay,            "Panchang",       "Panchangam",               Route.PANCHANG,      Color(0xFF0288D1), Color(0xFF01579B), ToolCategory.Daily),
    ToolCard(Icons.Default.NightsStay,                 "Live Sky",       "Current planet positions", Route.SKY,           Color(0xFF1A237E), Color(0xFF0D1442), ToolCategory.Daily),
    ToolCard(Icons.Default.WbSunny,                    "Horoscope",      "Daily rashi forecast",     Route.HOROSCOPE,     Color(0xFFFF7043), Color(0xFFBF360C), ToolCategory.Daily),
    ToolCard(Icons.AutoMirrored.Filled.MenuBook,       "Vedic Stories",  "Mythology & wisdom",       Route.STORIES,       Color(0xFFAD1457), Color(0xFF6A0036), ToolCategory.Daily),
    ToolCard(Icons.Default.Brightness5,                "Rashi Explorer", "12 zodiac signs",          Route.RASHI,         Color(0xFFEF6C00), Color(0xFFBF360C), ToolCategory.Knowledge),
    ToolCard(Icons.Default.Stars,                      "Nakshatra",      "27 lunar mansions",        Route.NAKSHATRA,     Color(0xFF4527A0), Color(0xFF1A0072), ToolCategory.Knowledge),
    ToolCard(Icons.Default.AutoAwesome,                "Yogas",          "Astrological yogas",       Route.YOGAS,         Color(0xFF00838F), Color(0xFF004D56), ToolCategory.Knowledge),
    ToolCard(Icons.Default.Healing,                    "Remedies",       "Planet remedies",          Route.REMEDIES,      Color(0xFF2E7D32), Color(0xFF1B5E20), ToolCategory.Knowledge),
    ToolCard(Icons.Default.MusicNote,                  "Mantras",        "Sacred chants & prayers",  Route.MANTRA,        Color(0xFFC62828), Color(0xFF7F0000), ToolCategory.Knowledge),
    ToolCard(Icons.AutoMirrored.Filled.LibraryBooks,   "Vedic Library",  "Search scriptures",        Route.LIBRARY,       Color(0xFF558B2F), Color(0xFF33691E), ToolCategory.Knowledge),
    ToolCard(Icons.Default.AccountTree,                "Gotra Finder",   "Vedic lineage system",     Route.GOTRA,         Color(0xFF6D4C41), Color(0xFF3E2723), ToolCategory.Knowledge),
)

// Shown in the "For You" horizontal row — popular / recommended
private val forYouRoutes = listOf(
    Route.GOCHAR, Route.PANCHANG, Route.COMPATIBILITY, Route.HOROSCOPE
)

// ─── Explore Screen ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoreScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }
    var longPressedTool by remember { mutableStateOf<ToolCard?>(null) }
    val haptic = LocalHapticFeedback.current

    // Voice search launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) searchQuery = spokenText
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Brahm AI tools...")
        }
        speechLauncher.launch(intent)
    }

    // Filtered tools
    val filteredTools = tools.filter { tool ->
        val matchesSearch = searchQuery.isBlank() ||
            tool.title.contains(searchQuery, ignoreCase = true) ||
            tool.subtitle.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || tool.category == selectedCategory
        matchesSearch && matchesCategory
    }

    val forYouTools = tools.filter { it.route in forYouRoutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Surface(color = BrahmCard, shadowElevation = 1.dp) {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                Text(
                    "Explore",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = BrahmGold,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "All 22 Jyotish tools",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
                Spacer(Modifier.height(12.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search tools, mantras, topics...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = BrahmMutedForeground, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(onClick = { launchVoiceSearch() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = BrahmGold, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrahmGold,
                        unfocusedBorderColor = BrahmBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                )

                Spacer(Modifier.height(10.dp))

                // Category chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryChip(
                        label = "All  ${tools.size}",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                    )
                    ToolCategory.entries.forEach { cat ->
                        val count = tools.count { it.category == cat }
                        CategoryChip(
                            label = "${cat.name}  $count",
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // "For You" row — only shown when no search/filter active
            if (searchQuery.isBlank() && selectedCategory == null) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column {
                        Text(
                            "For You",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = BrahmForeground,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(forYouTools) { tool ->
                                ForYouCard(
                                    tool = tool,
                                    onClick = { navController.navigate(tool.route) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Category section label
                        Text(
                            "All Tools",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = BrahmForeground,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Tool cards
            items(filteredTools) { tool ->
                ExploreToolCard(
                    tool = tool,
                    onClick = { navController.navigate(tool.route) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        longPressedTool = tool
                    },
                )
            }

            // Empty state
            if (filteredTools.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = BrahmMutedForeground,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No tools found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                        )
                    }
                }
            }

            // Bottom padding
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Long-press bottom sheet
    longPressedTool?.let { tool ->
        ModalBottomSheet(
            onDismissRequest = { longPressedTool = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
            ) {
                // Tool identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(tool.gradientStart, tool.gradientEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                }

                HorizontalDivider(color = BrahmBorder)
                Spacer(Modifier.height(8.dp))

                // Option: Open tool
                BottomSheetOption(
                    icon = Icons.Default.OpenInFull,
                    label = "Open ${tool.title}",
                    onClick = {
                        longPressedTool = null
                        navController.navigate(tool.route)
                    },
                )

                // Option: Ask AI — pop back to main and switch to chat tab
                BottomSheetOption(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "Ask Brahm AI about ${tool.title}",
                    onClick = {
                        longPressedTool = null
                        navController.popBackStack()
                    },
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── "For You" small horizontal card ──────────────────────────────────────────
@Composable
private fun ForYouCard(tool: ToolCard, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.width(90.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(tool.gradientStart, tool.gradientEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = tool.title, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                tool.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BrahmForeground,
                ),
                maxLines = 1,
            )
        }
    }
}

// ─── Explore tool card (main grid) ────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreToolCard(
    tool: ToolCard,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    BrahmCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(tool.gradientStart, tool.gradientEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = tool.title, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                tool.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                tool.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Category filter chip ──────────────────────────────────────────────────────
@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrahmGold,
            selectedLabelColor = Color.White,
            containerColor = Color(0xFFF5F5F5),
            labelColor = BrahmForeground,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BrahmBorder,
            selectedBorderColor = BrahmGold,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp,
        ),
    )
}

// ─── Bottom sheet option row ───────────────────────────────────────────────────
@Composable
private fun BottomSheetOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, tint = BrahmForeground, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
