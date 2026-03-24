package com.bimoraai.brahm.ui.palmistry

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalmistryScreen(
    navController: NavController,
    vm: PalmistryViewModel = hiltViewModel(),
) {
    val result by vm.result.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Palmistry AI", style = MaterialTheme.typography.titleLarge.copy(color = BrahmGold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmCard),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Image picker area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrahmMuted)
                    .border(1.dp, BrahmBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Palm image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🖐️", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Upload your palm photo", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                    }
                }
            }

            // Pick image button
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrahmOutlinedButton(
                    text = "Gallery",
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image,
                )
                BrahmButton(
                    text = "Analyze",
                    onClick = { selectedUri?.let { vm.analyzePalm(it) } },
                    modifier = Modifier.weight(1f),
                    enabled = selectedUri != null && !isLoading,
                    loading = isLoading,
                    icon = Icons.Default.CameraAlt,
                )
            }

            // Tips
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    SectionHeader("Tips for best results")
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "📷 Good lighting, no shadows",
                        "🖐️ Flat open palm, all lines visible",
                        "📱 Hold camera steady, close up",
                    ).forEach { tip ->
                        Text(tip, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            // Error
            if (error != null) {
                BrahmErrorView(message = error!!, onRetry = { selectedUri?.let { vm.analyzePalm(it) } })
            }

            // Result
            if (result != null) {
                SectionHeader("Palm Reading")
                result!!.forEach { (key, value) ->
                    if (value != null) {
                        BrahmCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium.copy(color = BrahmGold),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
