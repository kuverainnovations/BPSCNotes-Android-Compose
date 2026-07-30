package com.example.bpscnotes.presentation.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.bpscnotes.core.config.AppConfigData
import com.example.bpscnotes.core.ui.t.BpscColors

private fun mailSupport(context: Context, address: String) {
    val to = address.ifBlank { "admin@bpscnotes.in" }
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: ActivityNotFoundException) { /* no mail client installed */ }
}

/**
 * Root-level maintenance gate, rendered above the whole app alongside
 * [UpdateGateDialog]. Hard block with no escape — no back-press, no tap-outside
 * — for as long as the admin leaves the maintenance_mode switch on.
 *
 * Only fires once the config has actually been fetched ([AppConfigData.loaded]),
 * so a cold start or an offline launch never flashes this over the app.
 * Retry re-fetches rather than forcing a restart, so users get back in as soon
 * as the switch goes off.
 */
@Composable
fun MaintenanceGateDialog(config: AppConfigData, onRetry: () -> Unit) {
    val context = LocalContext.current

    if (!config.loaded || !config.maintenanceMode) return

    AlertDialog(
        onDismissRequest = { /* hard block — dismissal is not an option */ },
        icon = {
            Icon(
                Icons.Rounded.Build, null,
                tint = BpscColors.Primary, modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Under Maintenance",
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "BPSCNotes is briefly down for maintenance. We'll be back shortly — " +
                    "your progress and coins are safe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Retry") }
        },
        dismissButton = {
            TextButton(onClick = { mailSupport(context, config.supportEmail) }) {
                Text("Contact support", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
