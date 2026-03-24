package com.bimoraai.brahm.ui.today

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

@Composable
fun TodayScreen(
    navController: NavController,
    vm: TodayViewModel = hiltViewModel(),
) {
    val panchang by vm.panchang.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Brahm AI", style = MaterialTheme.typography.headlineSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                    Text("Today's Panchang", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }

        // Loading / Error
        if (isLoading) {
            item { BrahmLoadingSpinner(modifier = Modifier.height(200.dp)) }
        } else if (error != null) {
            item { BrahmErrorView(message = error!!, onRetry = { vm.load() }) }
        } else if (panchang != null) {
            val p = panchang!!

            // Panchang summary card
            item {
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Today's Panchang")
                        PanchangRow("Tithi", p["tithi"]?.toString() ?: "—")
                        PanchangRow("Nakshatra", p["nakshatra"]?.toString() ?: "—")
                        PanchangRow("Yoga", p["yoga"]?.toString() ?: "—")
                        PanchangRow("Karan", p["karan"]?.toString() ?: "—")
                        PanchangRow("Var (Day)", p["var"]?.toString() ?: "—")
                    }
                }
            }

            // Rahu Kaal
            item {
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("Rahu Kaal")
                        Spacer(Modifier.height(8.dp))
                        Text(p["rahu_kaal"]?.toString() ?: "—", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // Quick access tools
        item { SectionHeader("Quick Tools") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val tools = listOf(
                    "Kundali" to Route.KUNDALI,
                    "Gochar" to Route.GOCHAR,
                    "Compatibility" to Route.COMPATIBILITY,
                    "Muhurta" to Route.MUHURTA,
                    "KP System" to Route.KP,
                    "Palmistry" to Route.PALMISTRY,
                )
                items(tools) { (label, route) ->
                    BrahmCard(onClick = { navController.navigate(route) }) {
                        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanchangRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}
