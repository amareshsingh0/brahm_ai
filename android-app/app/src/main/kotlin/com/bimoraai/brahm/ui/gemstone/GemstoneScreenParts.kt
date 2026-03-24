package com.bimoraai.brahm.ui.gemstone

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.theme.*

@Composable
fun GemstoneScreenContent(data: Map<String, Any?>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        data.forEach { (key, value) ->
            if (value != null) {
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(key.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium.copy(color = BrahmMutedForeground))
                        Spacer(Modifier.height(4.dp))
                        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun GemstoneScreenInputForm(onSubmit: (Map<String, Any?>) -> Unit) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Enter Details", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Birth details or query") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
        )
        BrahmButton(
            text = "Calculate",
            onClick = { onSubmit(mapOf("query" to inputText)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotBlank(),
        )
    }
}
