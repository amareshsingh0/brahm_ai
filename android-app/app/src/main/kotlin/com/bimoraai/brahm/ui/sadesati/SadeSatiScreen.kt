package com.bimoraai.brahm.ui.sadesati

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

@Composable
fun SadeSatiScreen(navController: NavController, vm: SadeSatiScreenViewModel = hiltViewModel()) {
    val shaniRashi      by vm.shaniRashi.collectAsState()
    val shaniDegree     by vm.shaniDegree.collectAsState()
    val lagnaRashi      by vm.lagnaRashi.collectAsState()
    val isLoading       by vm.isLoading.collectAsState()
    val saturnError     by vm.saturnError.collectAsState()
    val selectedMoon    by vm.selectedMoonRashi.collectAsState()

    val sadeSatiPageData = remember(shaniRashi, shaniDegree, lagnaRashi, selectedMoon) {
        "{\"shani_rashi\":\"$shaniRashi\",\"shani_degree\":$shaniDegree,\"lagna_rashi\":\"$lagnaRashi\",\"moon_rashi\":\"$selectedMoon\",\"page\":\"sadesati\"}"
    }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sade Sati", fontWeight = FontWeight.Bold, color = BrahmGold)
                        Text("Saturn 7½ Year Transit", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshSaturn() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrahmMutedForeground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            SadeSatiContent(
                shaniRashi       = shaniRashi,
                shaniDegree      = shaniDegree,
                lagnaRashi       = lagnaRashi,
                isLoading        = isLoading,
                saturnError      = saturnError,
                selectedMoonRashi = selectedMoon,
                onMoonRashiSelected = { vm.selectedMoonRashi.value = it },
            )
        }
    }
    } // SwipeBackLayout
}
