package com.bimoraai.brahm.ui.kundali.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.components.SectionHeader
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.kundali.chart.KundaliChartView

// ─── Chart Tab ───────────────────────────────────────────────────────────────
@Composable
fun ChartTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val grahas = (data["grahas"] as? Map<String, List<String>>)
        ?.mapKeys { it.key.toIntOrNull() ?: 0 } ?: emptyMap()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        KundaliChartView(
            grahas = grahas,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        SectionHeader("Lagna")
        Text(data["lagna"]?.toString() ?: "—", style = MaterialTheme.typography.bodyLarge.copy(color = BrahmGold))
    }
}

// ─── Planets Tab ─────────────────────────────────────────────────────────────
@Composable
fun PlanetsTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val planets = data["planets"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Planetary Positions") }
        items(planets) { planet ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(planet["name"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(planet["rashi"]?.toString() ?: "—", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmGold))
                        Text("House ${planet["house"]?.toString() ?: "—"}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                }
            }
        }
    }
}

// ─── Dashas Tab ──────────────────────────────────────────────────────────────
@Composable
fun DashasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val dashas = data["dasha"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Vimshottari Dasha") }
        items(dashas) { dasha ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dasha["planet"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold))
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(dasha["start"]?.toString() ?: "—", style = MaterialTheme.typography.bodySmall)
                        Text("→ ${dasha["end"]?.toString() ?: "—"}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                }
            }
        }
    }
}

// ─── Yogas Tab ───────────────────────────────────────────────────────────────
@Composable
fun YogasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val yogas = data["yogas"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Yogas") }
        if (yogas.isEmpty()) {
            item { Text("No yogas found", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) }
        }
        items(yogas) { yoga ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(yoga["name"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(4.dp))
                    Text(yoga["description"]?.toString() ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
}

// ─── Alerts Tab ──────────────────────────────────────────────────────────────
@Composable
fun AlertsTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val alerts = data["alerts"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Alerts & Doshas") }
        if (alerts.isEmpty()) {
            item { Text("No alerts", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) }
        }
        items(alerts) { alert ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(alert["title"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    Text(alert["detail"]?.toString() ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
}

// ─── Shadbala Tab ────────────────────────────────────────────────────────────
@Composable
fun ShadbalaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val shadbala = data["shadbala"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Shadbala (Planetary Strength)") }
        items(shadbala) { s ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s["planet"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    Text("${s["total"]?.toString() ?: "—"} rupa", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmGold))
                }
            }
        }
    }
}

// ─── Navamsha Tab ────────────────────────────────────────────────────────────
@Composable
fun NavamshaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val navamsha = data["navamsha"] as? Map<String, Any?> ?: emptyMap()
    val grahas = (navamsha["grahas"] as? Map<String, List<String>>)
        ?.mapKeys { it.key.toIntOrNull() ?: 0 } ?: emptyMap()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Navamsha (D9) Chart")
        Spacer(Modifier.height(12.dp))
        KundaliChartView(grahas = grahas, modifier = Modifier.fillMaxWidth())
    }
}
