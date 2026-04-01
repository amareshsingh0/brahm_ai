package com.bimoraai.brahm.ui.marriage

import androidx.compose.foundation.background
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
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

@Composable
fun MarriageScreen(
    navController: NavController,
    vm: MarriageScreenViewModel = hiltViewModel(),
) {
    val result    by vm.result.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val hasData   by vm.hasData.collectAsState()
    val name      by vm.name.collectAsState()
    val dob       by vm.dob.collectAsState()
    val tob       by vm.tob.collectAsState()
    val pob       by vm.pob.collectAsState()
    val gender    by vm.gender.collectAsState()

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Marriage Analysis", fontWeight = FontWeight.Bold, color = BrahmGold)
                            Text("Vivah Jyotish", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (hasData) {
                            IconButton(onClick = { vm.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrahmMutedForeground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    hasData && result != null -> MarriageContent(result!!)
                    isLoading -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize())
                    error != null && !hasData -> BrahmErrorView(message = error!!, onRetry = { vm.load() })
                    else -> MarriageInputForm(
                        name = name, dob = dob, tob = tob, pob = pob, gender = gender,
                        error = error,
                        onNameChange   = { vm.name.value = it },
                        onDobChange    = { vm.dob.value = it },
                        onTobChange    = { vm.tob.value = it },
                        onPobChange    = { vm.pob.value = it },
                        onGenderChange = { vm.gender.value = it },
                        onCitySelected = { city ->
                            vm.pob.value = city.name
                            vm.lat.value = city.lat
                            vm.lon.value = city.lon
                            vm.tz.value  = city.tz.toString()
                        },
                        onCalculate = { vm.calculate() },
                    )
                }
            }
        }
    }
}
