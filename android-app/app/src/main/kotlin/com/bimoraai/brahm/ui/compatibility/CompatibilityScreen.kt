package com.bimoraai.brahm.ui.compatibility

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
fun CompatibilityScreen(navController: NavController, vm: CompatibilityScreenViewModel = hiltViewModel()) {
    val result    by vm.result.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val hasData   by vm.hasData.collectAsState()
    val name1     by vm.name1.collectAsState()
    val dob1      by vm.dob1.collectAsState()
    val tob1      by vm.tob1.collectAsState()
    val pob1      by vm.pob1.collectAsState()
    val name2     by vm.name2.collectAsState()
    val dob2      by vm.dob2.collectAsState()
    val tob2      by vm.tob2.collectAsState()
    val pob2      by vm.pob2.collectAsState()

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kundali Matching", fontWeight = FontWeight.Bold)
                        Text("Ashta-Koot Compatibility", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
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
                hasData && result != null -> CompatibilityContent(result!!)
                else -> CompatibilityInputForm(
                    name1 = name1, dob1 = dob1, tob1 = tob1, pob1 = pob1,
                    name2 = name2, dob2 = dob2, tob2 = tob2, pob2 = pob2,
                    error = error,
                    onName1Change    = { vm.name1.value = it },
                    onDob1Change     = { vm.dob1.value = it },
                    onTob1Change     = { vm.tob1.value = it },
                    onPob1Change     = { vm.pob1.value = it },
                    onCityASelected  = { city -> vm.pob1.value = city.name; vm.lat1.value = city.lat; vm.lon1.value = city.lon; vm.tz1.value = city.tz.toString() },
                    onName2Change    = { vm.name2.value = it },
                    onDob2Change     = { vm.dob2.value = it },
                    onTob2Change     = { vm.tob2.value = it },
                    onPob2Change     = { vm.pob2.value = it },
                    onCityBSelected  = { city -> vm.pob2.value = city.name; vm.lat2.value = city.lat; vm.lon2.value = city.lon; vm.tz2.value = city.tz.toString() },
                    onCalculate      = { vm.calculate() },
                )
            }
        }
    }
    } // SwipeBackLayout
}
