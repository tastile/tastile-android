package com.tastile.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ProfileScreen — Settings profile screen (v1/15 §4).
 *
 * Displays and edits the user's global profile.
 * Phase A: skeleton. Full implementation in Phase X.
 */
@Composable
fun ProfileScreen() {
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var accentColor by remember { mutableStateOf("#3366ff") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Color picker for accentColor
        // TODO: AvatarUpload component
    }
}
