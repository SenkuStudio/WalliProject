package com.walli.wallpaper.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.walli.wallpaper.domain.model.Wallpaper

@Composable
fun UnlockPremiumDialog(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Premium Wallpaper") },
        text = { Text("Watch a short video to unlock this premium wallpaper and support our creators.") },
        confirmButton = {
            Button(onClick = onUnlock) {
                Text("Watch Ad & Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
