package com.example.bpscnotes.core.permissions

import com.example.bpscnotes.core.language.LocalStrings
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// Persistence keys — stored in SharedPreferences so state
// survives process death, cold starts, and back-navigation
// ─────────────────────────────────────────────────────────────

private const val PREFS_NAME         = "bpsc_permissions"
private const val KEY_NOTIF_ASKED    = "notif_rationale_shown"   // shown rationale at least once
private const val KEY_NOTIF_DENIED   = "notif_permanently_denied" // user tapped "Don't ask again"

private fun Context.permPrefs() =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

// ─────────────────────────────────────────────────────────────
// Permission check helpers
// ─────────────────────────────────────────────────────────────

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.areNotificationsGranted(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    else
        true   // Android < 13: notifications always on, no runtime permission

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data  = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

// ─────────────────────────────────────────────────────────────
// NotificationPermissionEffect
//
// Drop into MainShell (or any @Composable that should trigger
// the notification permission request). Safe to call every
// recomposition — all state is persisted or derived from OS.
//
// Decision tree:
//   Android < 13        → skip (no runtime permission needed)
//   Already granted     → skip
//   Perm denied forever → show "Go to Settings" prompt once
//   Rationale shown 1x  → go straight to system prompt
//   First time          → show custom rationale → system prompt
// ─────────────────────────────────────────────────────────────

@Composable
fun NotificationPermissionEffect() {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    if (context.areNotificationsGranted()) return

    val prefs = remember { context.permPrefs() }

    // ── Derive initial state from persisted prefs (survives cold start) ──
    var showRationale    by remember {
        mutableStateOf(
            // Show rationale only if we have NOT already asked before
            !prefs.getBoolean(KEY_NOTIF_ASKED, false) &&
            !prefs.getBoolean(KEY_NOTIF_DENIED, false)
        )
    }
    var showGoToSettings by remember {
        mutableStateOf(
            // Show settings prompt only if user permanently denied before
            prefs.getBoolean(KEY_NOTIF_DENIED, false) &&
            !context.areNotificationsGranted()
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult

        val activity = context as? Activity
        val canStillAsk = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS
        ) == true

        if (!canStillAsk) {
            // User tapped "Don't ask again" — persist this so we don't spam on next open
            prefs.edit().putBoolean(KEY_NOTIF_DENIED, true).apply()
            showGoToSettings = true
        }
    }

    // Show rationale after screen settles (1.2s delay — only fires once per
    // this composition lifecycle, but state is persisted so it won't re-show
    // if already asked, even after back-navigation)
    LaunchedEffect(showRationale) {
        if (!showRationale) return@LaunchedEffect
        kotlinx.coroutines.delay(1_200)
        // Final safety check: could have been granted while we were waiting
        if (!context.areNotificationsGranted()) {
            showRationale = true  // trigger recompose to show dialog
        }
    }

    // ── Rationale dialog ────────────────────────────────────
    if (showRationale) {
        PermissionRationaleDialog(
            onAllow = {
                showRationale = false
                // Persist that we've shown the rationale — won't show again
                prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDeny = {
                showRationale = false
                // Persist — don't ask again this install
                prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
            }
        )
    }

    // ── "Go to Settings" prompt ─────────────────────────────
    if (showGoToSettings) {
        NotificationSettingsPrompt(
            onGoToSettings = {
                showGoToSettings = false
                // Clear the denied flag so we check again when they return
                prefs.edit().putBoolean(KEY_NOTIF_DENIED, false).apply()
                context.openAppSettings()
            },
            onDismiss = { showGoToSettings = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Rationale dialog — shown BEFORE the system permission prompt
// ─────────────────────────────────────────────────────────────

@Composable
private fun PermissionRationaleDialog(
    onAllow: () -> Unit,
    onDeny:  () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Dialog(
        onDismissRequest = onDeny,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Card(
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(24.dp),
            ) {
                Column {
                    // Gradient header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)))
                            )
                            .padding(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Pulsing icon ring
                            val pulse = rememberInfiniteTransition(label = "pulse")
                            val ringAlpha by pulse.animateFloat(
                                initialValue  = 0.15f,
                                targetValue   = 0.30f,
                                animationSpec = infiniteRepeatable(
                                    tween(900, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse,
                                ),
                                label = "ring",
                            )
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = ringAlpha))
                                )
                                Box(
                                    modifier         = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications, null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Text(
                                str.permStayUpdated,
                                style      = MaterialTheme.typography.titleLarge,
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign  = TextAlign.Center,
                            )
                        }
                    }

                    // Body
                    Column(
                        modifier            = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "BPSCNotes will send you:\n\n" +
                            "📅  Daily quiz reminders\n" +
                            str.permStreakWarn + "\n" +
                            "🎯  Study target nudges\n" +
                            "📢  New current affairs & exam alerts\n" +
                            "🏆  Rank & leaderboard updates",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = BpscColors.TextPrimary,
                            lineHeight = 22.sp,
                        )

                        // Benefit callout
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0F4FF))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.Top,
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Text(
                                str.permStudentsNote,
                                style      = MaterialTheme.typography.bodySmall,
                                color      = Color(0xFF1565C0),
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick  = onAllow,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = BpscColors.Primary,
                                ),
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive, null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    str.permEnableBtn,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            TextButton(
                                onClick  = onDeny,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    str.permMaybeLater,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// str.permBlocked dialog — shown after permanent denial
// ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationSettingsPrompt(
    onGoToSettings: () -> Unit,
    onDismiss:      () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(16.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier         = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsOff, null,
                            tint     = Color(0xFFF57C00),
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Text(
                        str.permBlocked,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = BpscColors.TextPrimary,
                        textAlign  = TextAlign.Center,
                    )
                    Text(
                        "You've blocked notifications for BPSCNotes. " +
                        "To receive study reminders and exam alerts, " +
                        "enable them in your device settings.",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = BpscColors.TextSecondary,
                        textAlign  = TextAlign.Center,
                        lineHeight = 20.sp,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick  = onGoToSettings,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF57C00),
                            ),
                        ) {
                            Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(str.permOpenSettings, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Not now", color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
