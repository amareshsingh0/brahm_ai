package com.bimoraai.brahm.ui.varshphal

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
import com.bimoraai.brahm.core.theme.*

@Composable
fun VarshpalScreen(navController: NavController, vm: VarshpalScreenViewModel = hiltViewModel()) {
    val result     by vm.result.collectAsState()
    val isLoading  by vm.isLoading.collectAsState()
    val error      by vm.error.collectAsState()
    val hasData    by vm.hasData.collectAsState()
    val name       by vm.name.collectAsState()
    val dob        by vm.dob.collectAsState()
    val tob        by vm.tob.collectAsState()
    val pob        by vm.pob.collectAsState()
    val targetYear by vm.targetYear.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Varshphal", fontWeight = FontWeight.Bold)
                        Text("Solar Return Chart", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
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
                hasData && result != null -> VarshpalContent(result!!)
                else -> VarshpalInputForm(
                    name = name, dob = dob, tob = tob, pob = pob, targetYear = targetYear,
                    error = error,
                    onNameChange       = { vm.name.value = it },
                    onDobChange        = { vm.dob.value = it },
                    onTobChange        = { vm.tob.value = it },
                    onPobChange        = { vm.pob.value = it },
                    onTargetYearChange = { vm.targetYear.value = it },
                    onCalculate        = { vm.calculate() },
                )
            }
        }
    }
}
