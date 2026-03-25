package com.bimoraai.brahm.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.BrahmOutlinedButton
import com.bimoraai.brahm.core.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

// Web OAuth client ID from Google Cloud Console (Firebase → Authentication → Google → Web client ID)
// TODO: replace with your actual Web Client ID from Firebase Console
private const val GOOGLE_WEB_CLIENT_ID = "121105525669-5km0oub8cmumovltdsi34279g8m0dv28.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showOtp by remember { mutableStateOf(false) }
    var storedPhone by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        when (state) {
            is AuthState.OtpSent -> {
                storedPhone = (state as AuthState.OtpSent).phone
                showOtp = true
            }
            is AuthState.LoggedIn -> onLoggedIn()
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))

        Text("ब्रह्म AI", style = MaterialTheme.typography.headlineLarge.copy(color = BrahmGold))
        Spacer(Modifier.height(8.dp))
        Text("Sign in to continue", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))

        Spacer(Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrahmCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrahmBorder),
        ) {
            Column(Modifier.padding(24.dp)) {

                if (!showOtp) {
                    // Phone input
                    Text("Phone Number", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10-digit mobile number", color = BrahmMutedForeground) },
                        prefix = { Text("+91  ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    BrahmButton(
                        text = "Send OTP",
                        onClick = { vm.sendOtp("+91$phone") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = phone.length == 10,
                        loading = state is AuthState.Loading,
                    )
                } else {
                    // OTP input
                    Text("Enter OTP", style = MaterialTheme.typography.titleMedium)
                    Text("Sent to $storedPhone", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("6-digit OTP", color = BrahmMutedForeground) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    BrahmButton(
                        text = "Verify OTP",
                        onClick = { vm.verifyOtp(storedPhone, otp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = otp.length == 6,
                        loading = state is AuthState.Loading,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showOtp = false; vm.resetState() }) {
                        Text("Change number", color = BrahmMutedForeground)
                    }
                }

                // Error
                if (state is AuthState.Error) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = (state as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Divider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = BrahmBorder)
            Text("  or  ", color = BrahmMutedForeground, style = MaterialTheme.typography.bodySmall)
            Divider(modifier = Modifier.weight(1f), color = BrahmBorder)
        }

        Spacer(Modifier.height(16.dp))

        // Google Sign-In
        BrahmOutlinedButton(
            text = "Continue with Google",
            onClick = {
                scope.launch {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    try {
                        val result = credentialManager.getCredential(
                            request = request,
                            context = context as Activity,
                        )
                        val idToken = GoogleIdTokenCredential
                            .createFrom(result.credential.data)
                            .idToken
                        vm.googleLogin(idToken)
                    } catch (e: GetCredentialException) {
                        vm.resetState()
                        // Error is shown via AuthState.Error if needed
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
