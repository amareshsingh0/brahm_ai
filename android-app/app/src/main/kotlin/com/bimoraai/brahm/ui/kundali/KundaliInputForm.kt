package com.bimoraai.brahm.ui.kundali

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton

@Composable
fun KundaliInputForm(
    onSubmit: (name: String, dob: String, tob: String, pob: String, lat: Double, lon: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dob  by remember { mutableStateOf("") }
    var tob  by remember { mutableStateOf("") }
    var pob  by remember { mutableStateOf("") }
    var lat  by remember { mutableStateOf(0.0) }
    var lon  by remember { mutableStateOf(0.0) }
    var tz   by remember { mutableStateOf(5.5) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Enter Birth Details", style = MaterialTheme.typography.headlineSmall)

        BirthInputFields(
            name          = name,
            onNameChange  = { name = it },
            showName      = true,
            dob           = dob,
            onDobChange   = { dob = it },
            tob           = tob,
            onTobChange   = { tob = it },
            pob           = pob,
            onPobChange   = { pob = it },
            onCitySelected = { city ->
                pob = city.name
                lat = city.lat
                lon = city.lon
                tz  = city.tz
            },
            cityVmKey = "kundali_input",
        )

        BrahmButton(
            text    = "Generate Kundali",
            onClick = { onSubmit(name, dob, tob, pob, lat, lon) },
            modifier = Modifier.fillMaxWidth(),
            enabled  = name.isNotBlank() && dob.isNotBlank() && tob.isNotBlank() && lat != 0.0,
        )
    }
}
