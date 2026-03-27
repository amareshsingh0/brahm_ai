package com.bimoraai.brahm.ui.rectification

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
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

@Composable
fun RectificationScreen(navController: NavController, vm: RectificationScreenViewModel = hiltViewModel()) {
    val result      by vm.result.collectAsState()
    val isLoading   by vm.isLoading.collectAsState()
    val error       by vm.error.collectAsState()
    val hasData     by vm.hasData.collectAsState()
    val name        by vm.name.collectAsState()
    val dob         by vm.dob.collectAsState()
    val approxTob   by vm.approxTob.collectAsState()
    val pob         by vm.pob.collectAsState()
    val uncertainty by vm.uncertainty.collectAsState()
    val event1Type  by vm.event1Type.collectAsState()
    val event1Date  by vm.event1Date.collectAsState()
    val event2Type  by vm.event2Type.collectAsState()
    val event2Date  by vm.event2Date.collectAsState()

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Birth Time Rectification", fontWeight = FontWeight.Bold)
                        Text("Find your exact birth time", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize())
                error != null && !hasData -> BrahmErrorView(message = error!!, onRetry = { vm.load() })
                hasData && result != null -> RectificationContent(result!!)
                else -> RectificationInputForm(
                    name = name, dob = dob, approxTob = approxTob, pob = pob,
                    uncertainty = uncertainty,
                    event1Type = event1Type, event1Date = event1Date,
                    event2Type = event2Type, event2Date = event2Date,
                    error = error,
                    onNameChange        = { vm.name.value = it },
                    onDobChange         = { vm.dob.value = it },
                    onApproxTobChange   = { vm.approxTob.value = it },
                    onPobChange         = { vm.pob.value = it },
                    onCitySelected      = { city -> vm.pob.value = city.name; vm.lat.value = city.lat; vm.lon.value = city.lon; vm.tz.value = city.tz.toString() },
                    onUncertaintyChange = { vm.uncertainty.value = it },
                    onEvent1TypeChange  = { vm.event1Type.value = it },
                    onEvent1DateChange  = { vm.event1Date.value = it },
                    onEvent2TypeChange  = { vm.event2Type.value = it },
                    onEvent2DateChange  = { vm.event2Date.value = it },
                    onCalculate         = { vm.calculate() },
                )
            }
        }
    }
    } // SwipeBackLayout
}
