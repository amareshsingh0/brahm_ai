package com.bimoraai.brahm.ui.horoscope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private data class RashiInfo(val emoji: String, val symbol: String, val color: Color)
private val rashiList = listOf(
    "Aries" to RashiInfo("♈", "Mesh", Color(0xFFE53935)),
    "Taurus" to RashiInfo("♉", "Vrishabh", Color(0xFF43A047)),
    "Gemini" to RashiInfo("♊", "Mithun", Color(0xFFFFB300)),
    "Cancer" to RashiInfo("♋", "Kark", Color(0xFF1565C0)),
    "Leo" to RashiInfo("♌", "Simha", Color(0xFFFF8F00)),
    "Virgo" to RashiInfo("♍", "Kanya", Color(0xFF2E7D32)),
    "Libra" to RashiInfo("⚖", "Tula", Color(0xFF7B1FA2)),
    "Scorpio" to RashiInfo("♏", "Vrishchik", Color(0xFFB71C1C)),
    "Sagittarius" to RashiInfo("♐", "Dhanu", Color(0xFFE65100)),
    "Capricorn" to RashiInfo("♑", "Makar", Color(0xFF37474F)),
    "Aquarius" to RashiInfo("♒", "Kumbh", Color(0xFF0277BD)),
    "Pisces" to RashiInfo("♓", "Meen", Color(0xFF6A1B9A)),
)

@Composable
fun HoroscopeContent(
    result: JsonObject?,
    selectedRashi: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onRashiSelected: (String) -> Unit,
) {
    val rashiInfo = rashiList.find { it.first == selectedRashi }?.second

    Column(modifier = modifier.fillMaxSize().background(BrahmBackground)) {
        // ── Rashi Selector ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rashiList) { (rashiName, info) ->
                val selected = rashiName == selectedRashi
                FilterChip(
                    selected = selected,
                    onClick = { onRashiSelected(rashiName) },
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(info.emoji, fontSize = 14.sp)
                            Text(rashiName, fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = rashiInfo?.color ?: BrahmGold,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrahmGold)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Rashi Header ──
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = rashiInfo?.color?.copy(alpha = 0.1f) ?: Color(0xFFFFF8E7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (rashiInfo?.color ?: BrahmGold).copy(alpha = 0.3f)),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background((rashiInfo?.color ?: BrahmGold).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(rashiInfo?.emoji ?: "♈", fontSize = 28.sp)
                        }
                        Column {
                            Text(selectedRashi, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(rashiInfo?.symbol ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                }
            }

            if (result == null) {
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                        Text("Horoscope data not available.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                    }
                }
            } else {
                val prediction   = result["prediction"]?.jsonPrimitive?.contentOrNull ?: result["daily_prediction"]?.jsonPrimitive?.contentOrNull ?: "—"
                val luckyColor   = result["lucky_color"]?.jsonPrimitive?.contentOrNull ?: "—"
                val luckyNumber  = result["lucky_number"]?.jsonPrimitive?.contentOrNull ?: "—"
                val rulingPlanet = result["sign_ruler"]?.jsonPrimitive?.contentOrNull ?: "—"

                // ── Daily Prediction ──
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Today's Prediction", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(prediction, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp))
                        }
                    }
                }

                // ── Lucky Info ──
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🎨", fontSize = 20.sp)
                                Text("Lucky Color", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(luckyColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            VerticalDivider(modifier = Modifier.height(48.dp), color = BrahmBorder)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔢", fontSize = 20.sp)
                                Text("Lucky Number", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(luckyNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                            }
                            VerticalDivider(modifier = Modifier.height(48.dp), color = BrahmBorder)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🪐", fontSize = 20.sp)
                                Text("Sign Ruler", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(rulingPlanet, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

            }
        }
    }
}
