package com.bimoraai.brahm.ui.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.kundali.chart.KundaliChartView
import com.bimoraai.brahm.ui.kundali.tabs.*

private val tabs = listOf("Chart", "Planets", "Dashas", "Yogas", "Alerts", "Shadbala", "Navamsha")

@Composable
fun KundaliScreen(
    navController: NavController,
    vm: KundaliViewModel = hiltViewModel(),
) {
    val kundali by vm.kundali.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val savedProfile by vm.savedProfile.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto-generate kundali from saved profile on first open
    LaunchedEffect(savedProfile) {
        val u = savedProfile
        if (kundali == null && !isLoading && u != null && u.date.isNotBlank() && u.place.isNotBlank()) {
            vm.setInputs(u.name, u.date, u.time, u.place, u.lat, u.lon, u.tz.toString())
            vm.generate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
    ) {
        // Header
        Surface(color = BrahmCard, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Kundali", style = MaterialTheme.typography.headlineSmall.copy(color = BrahmGold))
                Text("Vedic Birth Chart Analysis", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }

        if (isLoading) {
            BrahmLoadingSpinner()
        } else if (error != null) {
            BrahmErrorView(message = error!!, onRetry = { vm.generate() })
        } else if (kundali == null) {
            // Birth data input form
            KundaliInputForm(onSubmit = { name, dob, tob, pob, lat, lon ->
                vm.setInputs(name, dob, tob, pob, lat, lon)
                vm.generate()
            })
        } else {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = BrahmCard,
                contentColor = BrahmGold,
                edgePadding = 0.dp,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                        selectedContentColor = BrahmGold,
                        unselectedContentColor = BrahmMutedForeground,
                    )
                }
            }

            // Tab content
            val data = kundali!!
            when (selectedTab) {
                0 -> ChartTab(data)
                1 -> PlanetsTab(data)
                2 -> DashasTab(data)
                3 -> YogasTab(data)
                4 -> AlertsTab(data)
                5 -> ShadbalaTab(data)
                6 -> NavamshaTab(data)
            }
        }
    }
}
