package com.bimoraai.brahm.ui.kundali

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.theme.*

@Composable
fun KundaliInputForm(
    onSubmit: (name: String, dob: String, tob: String, pob: String, lat: Double, lon: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dob  by remember { mutableStateOf("") }   // YYYY-MM-DD
    var tob  by remember { mutableStateOf("") }   // HH:MM
    var pob  by remember { mutableStateOf("") }
    var lat  by remember { mutableStateOf("") }
    var lon  by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Enter Birth Details", style = MaterialTheme.typography.headlineSmall)

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrahmGold,
            unfocusedBorderColor = BrahmBorder,
        )
        val shape = RoundedCornerShape(8.dp)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = shape, colors = fieldColors)
        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), shape = shape, colors = fieldColors, placeholder = { Text("1990-06-15") })
        OutlinedTextField(value = tob, onValueChange = { tob = it }, label = { Text("Time of Birth (HH:MM)") }, modifier = Modifier.fillMaxWidth(), shape = shape, colors = fieldColors, placeholder = { Text("14:30") })
        OutlinedTextField(value = pob, onValueChange = { pob = it }, label = { Text("Place of Birth") }, modifier = Modifier.fillMaxWidth(), shape = shape, colors = fieldColors, placeholder = { Text("New Delhi, India") })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), shape = shape, colors = fieldColors, placeholder = { Text("28.6139") })
            OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), shape = shape, colors = fieldColors, placeholder = { Text("77.2090") })
        }

        BrahmButton(
            text = "Generate Kundali",
            onClick = {
                onSubmit(name, dob, tob, pob, lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && dob.isNotBlank() && tob.isNotBlank(),
        )
    }
}
