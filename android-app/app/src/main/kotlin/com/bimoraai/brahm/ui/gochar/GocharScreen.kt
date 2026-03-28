package com.bimoraai.brahm.ui.gochar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.PageBotFab
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

@Composable
fun GocharScreen(navController: NavController, vm: GocharScreenViewModel = hiltViewModel<GocharScreenViewModel>()) {
    val gocharData  by vm.gocharData.collectAsState()
    val analyzeData by vm.analyzeData.collectAsState()
    val isLoading   by vm.isLoading.collectAsState()
    val error       by vm.error.collectAsState()
    val hasData     by vm.hasData.collectAsState()
    val name        by vm.name.collectAsState()
    val dob         by vm.dob.collectAsState()
    val tob         by vm.tob.collectAsState()
    val pob         by vm.pob.collectAsState()

    // Build page_data JSON from current screen data for AI analysis
    val gocharPageData = remember(gocharData, analyzeData) {
        buildString {
            append("{")
            if (gocharData != null) append("\"positions\":${gocharData},")
            if (analyzeData != null) append("\"analysis\":${analyzeData},")
            append("\"page\":\"gochar\"}")
        }
    }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Planetary Transits", fontWeight = FontWeight.Bold)
                        Text("Gochar Analysis", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
        floatingActionButton = {
            if (hasData) {
                PageBotFab(pageContext = "gochar", pageData = gocharPageData)
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                // Show content immediately once sky positions load — don't block full screen
                // while personal analysis is still loading in background
                hasData -> GocharContent(gocharData, analyzeData, isLoading)
                isLoading -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize())
                error != null && !hasData -> BrahmErrorView(message = error!!, onRetry = { vm.load() })
                else -> GocharInputForm(
                    name = name, dob = dob, tob = tob, pob = pob,
                    error = error,
                    onNameChange   = { vm.name.value = it },
                    onDobChange    = { vm.dob.value = it },
                    onTobChange    = { vm.tob.value = it },
                    onPobChange    = { vm.pob.value = it },
                    onCitySelected = { city -> vm.pob.value = city.name; vm.lat.value = city.lat; vm.lon.value = city.lon; vm.tz.value = city.tz.toString() },
                    onCalculate    = { vm.calculate() },
                )
            }
        }
    }
    } // SwipeBackLayout
}
