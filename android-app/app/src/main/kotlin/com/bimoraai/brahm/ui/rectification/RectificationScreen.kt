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
    val lat         by vm.lat.collectAsState()
    val lon         by vm.lon.collectAsState()
    val uncertainty by vm.uncertainty.collectAsState()
    val events      by vm.events.collectAsState()

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
                hasData && result != null -> RectificationContent(result!!, onReset = { vm.reset() })
                else -> RectificationInputForm(
                    name = name, dob = dob, approxTob = approxTob, pob = pob,
                    lat = lat, lon = lon,
                    uncertainty = uncertainty,
                    events = events,
                    error = error,
                    onNameChange        = { vm.name.value = it },
                    onDobChange         = { vm.dob.value = it },
                    onApproxTobChange   = { vm.approxTob.value = it },
                    onPobChange         = { vm.pob.value = it },
                    onCitySelected      = { city -> vm.pob.value = city.name; vm.lat.value = city.lat; vm.lon.value = city.lon; vm.tz.value = city.tz.toString() },
                    onUncertaintyChange = { vm.uncertainty.value = it },
                    onAddEvent          = { vm.addEvent() },
                    onRemoveEvent       = { vm.removeEvent(it) },
                    onUpdateEventDate   = { i, d -> vm.updateEventDate(i, d) },
                    onUpdateEventType   = { i, t -> vm.updateEventType(i, t) },
                    onCalculate         = { vm.calculate() },
                )
            }
        }
    }
    } // SwipeBackLayout
}
