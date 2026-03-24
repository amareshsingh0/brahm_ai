package com.bimoraai.brahm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.theme.*

@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user by vm.user.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
            .padding(16.dp),
    ) {
        // Avatar + name
        BrahmCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrahmGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (user?.name?.firstOrNull() ?: "U").toString().uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(color = BrahmGold),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(user?.name ?: "User", style = MaterialTheme.typography.titleLarge)
                    Text(user?.phone ?: user?.email ?: "—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (user?.plan == "premium") BrahmGold.copy(alpha = 0.15f) else BrahmMuted,
                    ) {
                        Text(
                            text = (user?.plan ?: "free").uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(color = if (user?.plan == "premium") BrahmGold else BrahmMutedForeground),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Menu items
        listOf(
            Triple(Icons.Default.Person, "Edit Profile", {}),
            Triple(Icons.Default.Star, "Upgrade to Premium", {}),
            Triple(Icons.Default.Notifications, "Notifications", {}),
            Triple(Icons.Default.Language, "Language", {}),
            Triple(Icons.Default.Share, "Share App", {}),
        ).forEach { (icon, label, action) ->
            ProfileMenuItem(icon = icon, label = label, onClick = action)
        }

        Spacer(Modifier.weight(1f))

        // Logout
        OutlinedButton(
            onClick = { vm.logout { navController.navigate("login") { popUpTo(0) } } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = BrahmCard,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(20.dp))
        }
    }
}
