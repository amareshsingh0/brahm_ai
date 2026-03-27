package com.bimoraai.brahm.ui.horoscope

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
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

@Composable
fun HoroscopeScreen(navController: NavController, vm: HoroscopeScreenViewModel = hiltViewModel()) {
    val result        by vm.result.collectAsState()
    val isLoading     by vm.isLoading.collectAsState()
    val error         by vm.error.collectAsState()
    val selectedRashi by vm.selectedRashi.collectAsState()
    val userMoonRashi by vm.userMoonRashi.collectAsState()
    val autoSelected  by vm.autoSelected.collectAsState()

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Daily Horoscope", fontWeight = FontWeight.Bold)
                            Text(
                                "Select your Rashi",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
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
            if (error != null && result == null) {
                BrahmErrorView(
                    message = error!!,
                    onRetry = { vm.load() },
                    modifier = Modifier.padding(padding),
                )
            } else {
                HoroscopeContent(
                    result        = result,
                    selectedRashi = selectedRashi,
                    isLoading     = isLoading,
                    userMoonRashi = userMoonRashi,
                    autoSelected  = autoSelected,
                    modifier      = Modifier.padding(padding),
                    onRashiSelected = { vm.selectRashi(it) },
                )
            }
        }
    }
}
