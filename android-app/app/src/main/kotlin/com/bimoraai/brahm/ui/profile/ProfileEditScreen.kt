package com.bimoraai.brahm.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user by vm.user.collectAsState()
    val saveState by vm.saveState.collectAsState()

    // Pre-fill from loaded profile; update when user loads
    var fullName   by remember(user) { mutableStateOf(user?.name ?: "") }
    var dob        by remember(user) { mutableStateOf(user?.date ?: "") }
    var birthTime  by remember(user) { mutableStateOf(user?.time ?: "") }
    var birthPlace by remember(user) { mutableStateOf(user?.place ?: "") }
    var gender     by remember(user) { mutableStateOf(user?.gender ?: "") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    var genderExpanded by remember { mutableStateOf(false) }

    // Handle save result
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            vm.resetSaveState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (saveState is SaveState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            color = BrahmGold,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = {
                            vm.saveProfile(
                                name   = fullName,
                                date   = dob,
                                time   = birthTime,
                                place  = birthPlace,
                                gender = gender,
                            )
                        }) {
                            Text("Save", color = BrahmGold, fontWeight = FontWeight.SemiBold)
                        }
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

            // Error banner
            if (saveState is SaveState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        (saveState as SaveState.Error).msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer),
                    )
                }
            }

            // ── Personal Info ───────────────────────────────────────────────────
            ProfileEditSection("Personal Information") {
                BrahmTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                )
                BrahmTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = "Date of Birth",
                    placeholder = "YYYY-MM-DD",
                    leadingIcon = Icons.Default.CalendarToday,
                    keyboardType = KeyboardType.Number,
                )
                BrahmTextField(
                    value = birthTime,
                    onValueChange = { birthTime = it },
                    label = "Birth Time",
                    placeholder = "HH:MM  (e.g. 14:30)",
                    leadingIcon = Icons.Default.Schedule,
                )
                BrahmTextField(
                    value = birthPlace,
                    onValueChange = { birthPlace = it },
                    label = "Birth Place",
                    placeholder = "City, State, Country",
                    leadingIcon = Icons.Default.LocationOn,
                )

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it },
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = BrahmGold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { gender = option; genderExpanded = false },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Delete Account ──────────────────────────────────────────────────
            Surface(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEBEE),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Column {
                        Text("Delete Account", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium))
                        Text("Permanently delete your account and all data", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)))
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently delete your account, Kundali data, and chat history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false /* TODO: call delete API */ },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfileEditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BrahmCard,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun BrahmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = BrahmMutedForeground) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = BrahmGold) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrahmGold,
            unfocusedBorderColor = BrahmBorder,
        ),
    )
}
