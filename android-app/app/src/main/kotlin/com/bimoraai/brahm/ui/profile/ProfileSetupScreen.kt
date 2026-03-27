package com.bimoraai.brahm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

private data class Benefit(val icon: ImageVector, val title: String, val desc: String)

private val BENEFITS = listOf(
    Benefit(Icons.Default.AutoGraph,    "Personalized Kundali",  "Exact birth chart — Lagna, Dasha, Yogas instantly."),
    Benefit(Icons.Default.Psychology,   "AI That Knows You",     "Every answer specific to YOUR planets, not generic."),
    Benefit(Icons.Default.Favorite,     "Love & Career Timing",  "Know exact periods for marriage, job, travel."),
    Benefit(Icons.Default.CloudDone,    "Saved Across Devices",  "Profile synced — never enter details again."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    vm: ProfileSetupViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    var name   by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob    by remember { mutableStateOf("") }
    var tob    by remember { mutableStateOf("") }
    var pob    by remember { mutableStateOf("") }
    var city   by remember { mutableStateOf<City?>(null) }

    val genders = listOf("Male", "Female", "Other", "Prefer not to say")
    var genderExpanded by remember { mutableStateOf(false) }

    // Navigate to MAIN after success
    LaunchedEffect(state) {
        if (state is ProfileSetupState.Success || state is ProfileSetupState.Skipped) {
            navController.navigate(Route.MAIN) {
                popUpTo(Route.PROFILE_SETUP) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Text(
            "ONE-TIME SETUP",
            style = MaterialTheme.typography.labelSmall.copy(
                color = BrahmGold,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Complete Your Birth Profile",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BrahmForeground,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Save once — unlock personalized Vedic insights across every feature.",
            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
        )

        Spacer(Modifier.height(20.dp))

        // Benefits grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BENEFITS.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { benefit ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0A0)),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(benefit.icon, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(18.dp))
                                Text(benefit.title, style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold, color = Color(0xFF5C3D00),
                                ))
                                Text(benefit.desc, style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF8A6020), lineHeight = 16.sp,
                                ))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Form card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = BrahmCard),
            border   = androidx.compose.foundation.BorderStroke(1.dp, BrahmBorder),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it },
                ) {
                    OutlinedTextField(
                        value            = gender.ifBlank { "Select Gender" },
                        onValueChange    = {},
                        readOnly         = true,
                        modifier         = Modifier.fillMaxWidth().menuAnchor(),
                        label            = { Text("Gender") },
                        leadingIcon      = { Icon(Icons.Default.Person, contentDescription = null, tint = BrahmGold) },
                        trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        shape            = RoundedCornerShape(10.dp),
                        colors           = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        genders.forEach { g ->
                            DropdownMenuItem(
                                text    = { Text(g) },
                                onClick = { gender = g; genderExpanded = false },
                            )
                        }
                    }
                }

                // Birth fields (Name, DOB, TOB, Place)
                BirthInputFields(
                    name          = name,
                    onNameChange  = { name = it },
                    dob           = dob,
                    onDobChange   = { dob = it },
                    tob           = tob,
                    onTobChange   = { tob = it },
                    pob           = pob,
                    onPobChange   = { pob = it },
                    onCitySelected = { c ->
                        city = c
                        pob  = c.name
                    },
                )

                if (state is ProfileSetupState.Error) {
                    Text(
                        (state as ProfileSetupState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Save button
        Button(
            onClick  = {
                if (city != null && dob.isNotBlank()) {
                    vm.saveProfile(
                        name   = name,
                        gender = gender,
                        date   = dob,
                        time   = tob,
                        place  = city!!.name,
                        lat    = city!!.lat,
                        lon    = city!!.lon,
                        tz     = city!!.tz,
                    )
                }
            },
            enabled  = city != null && dob.isNotBlank() && state !is ProfileSetupState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4540A)),
        ) {
            if (state is ProfileSetupState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Save Profile", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Skip button
        TextButton(
            onClick  = { vm.skip() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Skip for now",
                color = BrahmMutedForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "You can always update this later in Profile settings.",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
        )

        Spacer(Modifier.height(24.dp))
    }
}
