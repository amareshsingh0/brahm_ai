package com.bimoraai.brahm.ui.horoscope

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Matches website rashiData exactly: symbol, Sanskrit name, ruler
private data class RashiMeta(
    val symbol: String,
    val sanskritName: String,
    val ruler: String,
    val chipColor: Color,
)

private val rashiMeta: Map<String, RashiMeta> = mapOf(
    "Aries"       to RashiMeta("♈\uFE0E", "Mesha",     "Mars",    Color(0xFFE53935)),
    "Taurus"      to RashiMeta("♉\uFE0E", "Vrishabha", "Venus",   Color(0xFF43A047)),
    "Gemini"      to RashiMeta("♊\uFE0E", "Mithuna",   "Mercury", Color(0xFFFFB300)),
    "Cancer"      to RashiMeta("♋\uFE0E", "Karka",     "Moon",    Color(0xFF1565C0)),
    "Leo"         to RashiMeta("♌\uFE0E", "Simha",     "Sun",     Color(0xFFFF8F00)),
    "Virgo"       to RashiMeta("♍\uFE0E", "Kanya",     "Mercury", Color(0xFF2E7D32)),
    "Libra"       to RashiMeta("♎\uFE0E", "Tula",      "Venus",   Color(0xFF7B1FA2)),
    "Scorpio"     to RashiMeta("♏\uFE0E", "Vrischika", "Mars",    Color(0xFFB71C1C)),
    "Sagittarius" to RashiMeta("♐\uFE0E", "Dhanu",     "Jupiter", Color(0xFFE65100)),
    "Capricorn"   to RashiMeta("♑\uFE0E", "Makara",    "Saturn",  Color(0xFF37474F)),
    "Aquarius"    to RashiMeta("♒\uFE0E", "Kumbha",    "Saturn",  Color(0xFF0277BD)),
    "Pisces"      to RashiMeta("♓\uFE0E", "Meena",     "Jupiter", Color(0xFF6A1B9A)),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HoroscopeContent(
    result: JsonObject?,
    selectedRashi: String,
    isLoading: Boolean,
    userMoonRashi: String?,
    autoSelected: Boolean,
    modifier: Modifier = Modifier,
    onRashiSelected: (String) -> Unit,
) {
    val meta     = rashiMeta[selectedRashi]
    val today    = LocalDate.now()
    val dateStr  = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))

    val listState = rememberLazyListState()
    Box(modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Auto-selected Janma Rashi badge (matches website amber notice) ──
        if (autoSelected && userMoonRashi != null) {
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                    Text(
                        "Showing your Janma Rashi ($userMoonRashi) from your Kundali",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)),
                    )
                }
            }
        }

        // ── Rashi selector — horizontal scroll ──
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(rashiNames.size) { idx ->
                    val rashi    = rashiNames[idx]
                    val m        = rashiMeta[rashi]
                    val selected = rashi == selectedRashi
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (selected) (m?.chipColor ?: BrahmGold).copy(alpha = 0.18f)
                                else Color(0xFFF5F5F5)
                            )
                            .border(
                                1.dp,
                                if (selected) (m?.chipColor ?: BrahmGold) else Color(0xFFE0E0E0),
                                RoundedCornerShape(50.dp),
                            )
                            .clickable { onRashiSelected(rashi) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(m?.symbol ?: "", fontSize = 13.sp)
                            Text(
                                rashi,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) (m?.chipColor ?: BrahmGold) else BrahmMutedForeground,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ── Horoscope card (matches website cosmic-card layout) ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // ── Header: large symbol + name + Sanskrit · date ──
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(meta?.symbol ?: "♈\uFE0E", fontSize = 60.sp)
                        Text(
                            selectedRashi,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrahmForeground,
                            ),
                        )
                        Text(
                            "${meta?.sanskritName ?: ""} · $dateStr",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold),
                        )
                    }

                    // ── Today's Prediction section ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "TODAY'S PREDICTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrahmMutedForeground,
                                letterSpacing = 1.sp,
                            ),
                        )
                        if (isLoading) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(3) { idx ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth(if (idx == 2) 0.6f else if (idx == 1) 0.8f else 1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE0E0E0)),
                                    )
                                }
                            }
                        } else {
                            val prediction = result?.get("prediction")?.jsonPrimitive?.contentOrNull
                                ?: result?.get("daily_prediction")?.jsonPrimitive?.contentOrNull
                                ?: "Loading prediction…"
                            Text(
                                prediction,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BrahmForeground,
                                    lineHeight = 20.sp,
                                ),
                            )
                        }
                    }

                    // ── 2-col grid: Lucky | Sign Ruler (matches website exactly) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Lucky color + number
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "Lucky",
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                            )
                            if (!isLoading && result != null) {
                                val luckyColor  = result["lucky_color"]?.jsonPrimitive?.contentOrNull ?: "—"
                                val luckyNumber = result["lucky_number"]?.jsonPrimitive?.contentOrNull ?: "—"
                                Text(
                                    "$luckyColor · $luckyNumber",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrahmGold,
                                    ),
                                )
                            } else {
                                Text("—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }

                        // Sign Ruler
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "Sign Ruler",
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                            )
                            val ruler = if (!isLoading && result != null)
                                result["sign_ruler"]?.jsonPrimitive?.contentOrNull ?: meta?.ruler ?: "—"
                            else meta?.ruler ?: "—"
                            Text(
                                ruler,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrahmForeground,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // bottom padding
        item { Spacer(Modifier.height(16.dp)) }
    }
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp))
    } // Box
}
