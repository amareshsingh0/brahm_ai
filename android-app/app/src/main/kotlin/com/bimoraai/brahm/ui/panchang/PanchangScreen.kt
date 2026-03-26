package com.bimoraai.brahm.ui.panchang

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun JsonObject.str(key: String): String {
    val el = this[key] ?: return "—"
    if (el !is JsonPrimitive) return "—"
    return el.contentOrNull?.takeIf { it.isNotBlank() && it != "null" } ?: "—"
}

// Choghadiya periods — static for day (calculated dynamically by backend; shown as placeholder if not returned)
private data class Choghadiya(val name: String, val type: String, val color: Color)
private val auspiciousNames = setOf("Amrit", "Shubh", "Labh", "Char")
private val staticChoghadiya = listOf(
    Choghadiya("Udveg",  "Inauspicious", Color(0xFFE53935)),
    Choghadiya("Char",   "Auspicious",   Color(0xFF43A047)),
    Choghadiya("Labh",   "Auspicious",   Color(0xFF43A047)),
    Choghadiya("Amrit",  "Auspicious",   Color(0xFFFFB300)),
    Choghadiya("Kaal",   "Inauspicious", Color(0xFF546E7A)),
    Choghadiya("Shubh",  "Auspicious",   Color(0xFF43A047)),
    Choghadiya("Rog",    "Inauspicious", Color(0xFFE53935)),
    Choghadiya("Udveg",  "Inauspicious", Color(0xFFE53935)),
)

@Composable
fun PanchangScreen(
    navController: NavController,
    vm: PanchangViewModel = hiltViewModel(),
) {
    val panchang  by vm.panchang.collectAsState()
    val festivals by vm.festivals.collectAsState()
    val grahan    by vm.grahan.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full Panchang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        when {
            isLoading -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize().padding(padding))
            error != null -> BrahmErrorView(message = error!!, onRetry = { vm.load() }, modifier = Modifier.padding(padding))
            else -> PanchangContent(panchang, festivals, grahan, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PanchangContent(
    panchang: JsonObject?,
    festivals: JsonObject?,
    grahan: JsonObject?,
    modifier: Modifier = Modifier,
) {
    val p = panchang ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Section 1: Today's Panchang ──────────────────────────────────────
        item {
            SectionHeader("Today's Panchang")
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PanchangStatCard("Tithi",    p.str("tithi"),          p.str("tithi_lord"),       Color(0xFFFF8F00), Modifier.weight(1f))
                        PanchangStatCard("Nakshatra",p.str("nakshatra"),      p.str("nakshatra_lord"),   Color(0xFF5C6BC0), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PanchangStatCard("Yoga",    p.str("yoga"),            "—",                       Color(0xFF43A047), Modifier.weight(1f))
                        PanchangStatCard("Karan",   p.str("karan"),           "—",                       Color(0xFF8E24AA), Modifier.weight(1f))
                    }
                    HorizontalDivider(color = BrahmBorder)
                    PanchangRow("Var (Day)",    p.str("var"))
                    PanchangRow("Moon Rashi",   p.str("chandra_rashi"))
                    PanchangRow("Sun Rashi",    p.str("surya_rashi"))
                    PanchangRow("Sunrise",      p.str("sunrise"))
                    PanchangRow("Sunset",       p.str("sunset"))
                }
            }
        }

        // ── Section 2: Rahu Kaal & Inauspicious Times ──────────────────────
        item { SectionHeader("Inauspicious Timings") }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InauspiciousRow("Rahu Kaal",   p.str("rahu_kaal"),   Color(0xFFE53935))
                    InauspiciousRow("Yamagandam",  p.str("yamagandam"),  Color(0xFF8E24AA))
                    InauspiciousRow("Gulika Kaal", p.str("gulika_kaal"), Color(0xFF546E7A))
                    InauspiciousRow("Dur Muhurta", p.str("dur_muhurta"), Color(0xFFFF6F00))
                }
            }
        }

        // ── Section 3: Choghadiya ────────────────────────────────────────────
        item { SectionHeader("Choghadiya (Day)") }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chogs = panchang?.get("choghadiya")?.let { el ->
                        try {
                            el.jsonArray.mapIndexed { i, item ->
                                val obj = item.jsonObject
                                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: staticChoghadiya[i % 8].name
                                val type = if (name in auspiciousNames) "Auspicious" else "Inauspicious"
                                val start = obj["start"]?.jsonPrimitive?.contentOrNull ?: "—"
                                val end   = obj["end"]?.jsonPrimitive?.contentOrNull ?: "—"
                                Triple(name, type, "$start – $end")
                            }
                        } catch (_: Exception) { null }
                    }

                    if (chogs != null) {
                        chogs.forEachIndexed { _, (name, type, time) ->
                            ChoghadiyaRow(name, type, time)
                        }
                    } else {
                        // Static fallback labels
                        staticChoghadiya.forEachIndexed { i, chog ->
                            ChoghadiyaRow(chog.name, chog.type, "Period ${i + 1}")
                        }
                        Text(
                            "Exact times vary by location. Enter your city in Profile to see accurate timings.",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp),
                        )
                    }
                }
            }
        }

        // ── Section 4: Festivals This Month ─────────────────────────────────
        item { SectionHeader("Festivals This Month") }
        item {
            val festList = festivals?.get("festivals")?.let { el ->
                try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
            }
            if (festList.isNullOrEmpty()) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text(
                        "No festivals data available. Check back later.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    festList.forEach { fest ->
                        FestivalCard(fest)
                    }
                }
            }
        }

        // ── Section 5: Grahan Calendar ───────────────────────────────────────
        item { SectionHeader("Eclipse Calendar") }
        item {
            val grahanList = grahan?.get("eclipses")?.let { el ->
                try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
            }
            if (grahanList.isNullOrEmpty()) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text(
                        "No upcoming eclipses in the near future.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grahanList.forEach { eclipse ->
                        GrahanCard(eclipse)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground),
    )
}

@Composable
private fun PanchangStatCard(label: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.SemiBold))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            if (sub != "—") Text(sub, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        }
    }
}

@Composable
private fun PanchangRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun InauspiciousRow(label: String, time: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
    }
}

@Composable
private fun ChoghadiyaRow(name: String, type: String, time: String) {
    val isAuspicious = name in auspiciousNames
    val color = if (isAuspicious) Color(0xFF43A047) else Color(0xFFE53935)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isAuspicious) "✓" else "✗", style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.Bold))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Text(type, style = MaterialTheme.typography.labelSmall.copy(color = color))
        }
        Text(time, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
    }
}

@Composable
private fun FestivalCard(fest: JsonObject) {
    val name = fest["name"]?.jsonPrimitive?.contentOrNull ?: "Festival"
    val date = fest["date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val tradition = fest["tradition"]?.jsonPrimitive?.contentOrNull ?: "Hindu"
    val tradColor = when (tradition) {
        "Hindu"   -> Color(0xFFFF6F00)
        "Jain"    -> Color(0xFF1565C0)
        "Buddhist"-> Color(0xFF43A047)
        "Sikh"    -> Color(0xFF8E24AA)
        else      -> BrahmGold
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(tradColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text("🎉", fontSize = 18.sp) }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(date, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
            Box(
                Modifier.clip(RoundedCornerShape(6.dp)).background(tradColor.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text(tradition, style = MaterialTheme.typography.labelSmall.copy(color = tradColor)) }
        }
    }
}

@Composable
private fun GrahanCard(eclipse: JsonObject) {
    val type    = eclipse["type"]?.jsonPrimitive?.contentOrNull ?: "Eclipse"
    val date    = eclipse["date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val time    = eclipse["time"]?.jsonPrimitive?.contentOrNull ?: "—"
    val sutak   = eclipse["sutak_start"]?.jsonPrimitive?.contentOrNull
    val visible = eclipse["visible_india"]?.jsonPrimitive?.contentOrNull
    val isSolar = type.contains("Solar", ignoreCase = true)
    val color   = if (isSolar) Color(0xFFFF6F00) else Color(0xFF1565C0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isSolar) "☀️" else "🌙", fontSize = 20.sp)
                Text(type, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) { Text(if (visible == "true") "Visible in India" else "Not in India", style = MaterialTheme.typography.labelSmall.copy(color = color)) }
            }
            PanchangRow("Date", date)
            PanchangRow("Time", time)
            if (sutak != null) PanchangRow("Sutak Starts", sutak)
        }
    }
}
