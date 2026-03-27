package com.bimoraai.brahm.ui.gotra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

data class GotraInfo(
    val name: String,
    val rishi: String,
    val pravara: List<String>,
    val deity: String,
    val veda: String,
    val descendants: String,
    val rule: String,
)

private val gotras = listOf(
    GotraInfo("Bharadwaj", "Rishi Bharadwaj", listOf("Angirasa", "Barhaspatya", "Bharadwaja"), "Brahaspati", "Rig Veda", "Bharadwajas, many Brahmin families", "Avoid marriage within same gotra + 7 generations of Sapinda"),
    GotraInfo("Kashyap", "Rishi Kashyap", listOf("Kashyapa", "Avatsara", "Naidhruva"), "Sun/Prajapati", "Atharva Veda", "Kashyapas — ancestors of Devas, Asuras, humans", "Same rule — no same-gotra marriage"),
    GotraInfo("Gautam", "Rishi Gautam", listOf("Angirasa", "Ayasya", "Gautama"), "Indra/Varuna", "Sama Veda", "Gautamas, Ahirs (Yadavas)", "Sapinda rule applies"),
    GotraInfo("Agastya", "Rishi Agastya", listOf("Agastya", "Mahendra", "Mayobhuva"), "Varuna", "Rig Veda", "South Indian Brahmins, Tamil Brahmins", "No same-gotra marriage"),
    GotraInfo("Vasishtha", "Rishi Vasishtha", listOf("Vasishtha", "Shakti", "Parasara"), "Mitra-Varuna", "Rig Veda", "Vashisthas, Rajputs, many North Indian families", "Same rule"),
    GotraInfo("Vishwamitra", "Rishi Vishwamitra", listOf("Vishwamitra", "Devarata", "Audala"), "Indra", "Rig Veda", "Vishwamitras, Kaushikas", "Sapinda + gotra rule"),
    GotraInfo("Atri", "Rishi Atri", listOf("Atreya", "Archananasa", "Shyavashwa"), "Moon/Soma", "Rig Veda", "Atris, some Brahmin clans", "Same rule"),
    GotraInfo("Jamadagni", "Rishi Jamadagni", listOf("Bhargava", "Chyavana", "Apnavana", "Jamadagni"), "Vishnu", "Yajur Veda", "Parasurama lineage, Bhargavas", "Sapinda rule"),
    GotraInfo("Sandilya", "Rishi Sandilya", listOf("Kashyapa", "Asita", "Devala", "Sandilya"), "Agni", "Atharva Veda", "Sandilyayana Brahmins, Bengal Brahmins", "Same rule"),
    GotraInfo("Vatsya", "Rishi Vatsa", listOf("Bhargava", "Vatsya"), "Varuna", "Yajur Veda", "Vatsyayanas, some Brahmin clans", "No same-gotra marriage"),
)

@Composable
fun GotraScreen(navController: NavController) {
    var expandedGotra by remember { mutableStateOf<String?>(null) }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gotra Finder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
    ) { padding ->
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                // Info banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D59A)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About Gotra System", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        Text(
                            "Gotra refers to the lineage or clan of a person, tracing descent from a Vedic rishi (sage). Hindu marriage rules forbid marriage within the same gotra to prevent consanguinity. The Pravara is a list of distinguished ancestors used during Vedic rituals.",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground),
                        )
                        Text(
                            "Marriage Rule: Same Gotra = Forbidden · Sapinda (7 generations paternal, 5 maternal) = Forbidden",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935), fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
            items(gotras) { gotra ->
                GotraCard(
                    gotra = gotra,
                    expanded = expandedGotra == gotra.name,
                    onClick = { expandedGotra = if (expandedGotra == gotra.name) null else gotra.name },
                )
            }
        }
        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
        } // Box
    }
    } // SwipeBackLayout
}

@Composable
private fun GotraCard(gotra: GotraInfo, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.5.dp, BrahmGold) else null,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(BrahmGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🕉", style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text(gotra.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(gotra.rishi, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1565C0).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(gotra.veda, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF1565C0)))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = BrahmBorder)
                    DetailRow("Deity", gotra.deity)
                    DetailRow("Veda", gotra.veda)
                    DetailRow("Descendants", gotra.descendants)
                    Text("Pravara (ancestor list):", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold))
                    Text(gotra.pravara.joinToString(" → "), style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    ) {
                        Text(
                            "⚠ Marriage Rule: ${gotra.rule}",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
