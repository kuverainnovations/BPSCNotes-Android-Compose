package com.example.bpscnotes.presentation.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bpscnotes.app.BuildConfig
import com.example.bpscnotes.core.config.AppConfigData
import com.example.bpscnotes.core.ui.t.BpscColors

/** Dotted version compare ("1.2.10" > "1.2.9"); missing segments treated as 0. */
private fun isVersionLower(current: String, target: String): Boolean {
    if (target.isBlank()) return false
    val c = current.split(".").map { it.toIntOrNull() ?: 0 }
    val t = target.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(c.size, t.size)) {
        val cv = c.getOrElse(i) { 0 }
        val tv = t.getOrElse(i) { 0 }
        if (cv != tv) return cv < tv
    }
    return false
}

private fun openPlayStore(context: Context, storeUrl: String) {
    val url = storeUrl.ifBlank { "https://play.google.com/store/apps/details?id=${context.packageName}" }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: ActivityNotFoundException) { /* no browser/Play Store available on device */ }
}

/**
 * Root-level update gate, rendered above the whole app (NavHost, splash, everything).
 * - Hard block (no dismiss, no back-press escape) when the installed version is below
 *   min_app_version, or the admin has flipped the force_update kill-switch.
 * - Otherwise a dismissible "new version available" nudge, once per app open, when
 *   a newer app_version exists.
 */
@Composable
fun UpdateGateDialog(config: AppConfigData) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME

    val mustUpdate = config.forceUpdate || isVersionLower(currentVersion, config.minAppVersion)
    var softNudgeDismissed by rememberSaveable { mutableStateOf(false) }
    val showSoftNudge = !mustUpdate &&
        !softNudgeDismissed &&
        isVersionLower(currentVersion, config.latestAppVersion)

    if (!mustUpdate && !showSoftNudge) return

    AlertDialog(
        onDismissRequest = { if (!mustUpdate) softNudgeDismissed = true },
        icon = {
            Icon(
                Icons.Rounded.SystemUpdate, null,
                tint = BpscColors.Primary, modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                if (mustUpdate) "Update Required" else "New Version Available",
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                if (mustUpdate)
                    "Please update BPSCNotes to continue. This version is no longer supported."
                else
                    "A new version of BPSCNotes is available with improvements and fixes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = { openPlayStore(context, config.androidStoreUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) { Text(str.updateNow) }
        },
        dismissButton = if (mustUpdate) null else {
            @Composable {
                TextButton(onClick = { softNudgeDismissed = true }) {
                    Text(str.roomsLater, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !mustUpdate,
            dismissOnClickOutside = !mustUpdate
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
