package com.example.bpscnotes.presentation.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.profile.ProfileViewModel

// ════════════════════════════════════════════════════════════
// SettingsScreen — fully dynamic
// • User info from ProfileViewModel (already has live user data)
// • Toggles persist in SharedPreferences via SettingsViewModel
// • Storage sizes computed from real filesystem
// • Logout + Delete account call real APIs
// ════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    navController:    NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    profileViewModel:  ProfileViewModel  = hiltViewModel()
) {
    val settingsState by settingsViewModel.state.collectAsState()
    val profileState  by profileViewModel.uiState.collectAsState()
    val user = profileState.user

    val snackbarHost = remember { SnackbarHostState() }

    // Navigate to Login when logged out or account deleted
    LaunchedEffect(settingsState.loggedOut) {
        if (settingsState.loggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Show success/error snackbar
    LaunchedEffect(settingsState.successMessage) {
        settingsState.successMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            settingsViewModel.clearMessages()
        }
    }
    LaunchedEffect(settingsState.error) {
        settingsState.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            settingsViewModel.clearMessages()
        }
    }

    // Delete account confirmation dialog
    if (settingsState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = settingsViewModel::hideDeleteConfirm,
            icon  = { Text("⚠️", fontSize = 32.sp) },
            title = { Text("Delete Account?", fontWeight = FontWeight.ExtraBold) },
            text  = {
                Text(
                    "This will permanently delete your account, all progress, coins, " +
                    "and study data. This action cannot be undone.",
                    color = BpscColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = settingsViewModel::deleteAccount,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = settingsViewModel::hideDeleteConfirm) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BpscColors.Surface)
        ) {
            SettingsHeader(onBack = { navController.popBackStack() })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Account ────────────────────────────────────────
                SettingsSectionLabel("Account")
                AccountCard(
                    name   = user?.name,
                    email  = user?.email ?: user?.mobile,
                    coins  = user?.coins,
                    onEdit = { navController.navigate(Screen.EditProfile.route) }
                )

                // ── Appearance ────────────────────────────────────
                SettingsSectionLabel("Appearance")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    SettingsToggleRow(
                        icon     = Icons.Rounded.DarkMode,
                        iconBg   = Color(0xFF1A237E).copy(0.12f),
                        iconTint = Color(0xFF3949AB),
                        title    = "Dark Mode",
                        subtitle = "Switch to dark theme",
                        checked  = settingsState.darkMode,
                        onChange = settingsViewModel::setDarkMode
                    )
                }

                // ── Study Preferences ─────────────────────────────
                SettingsSectionLabel("Study Preferences")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon     = Icons.Rounded.Alarm,
                            iconBg   = Color(0xFFE3F2FD),
                            iconTint = Color(0xFF1565C0),
                            title    = "Daily Study Reminder",
                            subtitle = "Remind me to study every day",
                            checked  = settingsState.studyReminder,
                            onChange = settingsViewModel::setStudyReminder
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsToggleRow(
                            icon     = Icons.Rounded.PlayCircle,
                            iconBg   = Color(0xFFF3E5F5),
                            iconTint = Color(0xFF7B1FA2),
                            title    = "Auto-play Videos",
                            subtitle = "Play next video automatically",
                            checked  = settingsState.autoPlay,
                            onChange = settingsViewModel::setAutoPlay
                        )
                    }
                }

                // ── Sound & Haptics ───────────────────────────────
                SettingsSectionLabel("Sound & Haptics")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon     = Icons.Rounded.VolumeUp,
                            iconBg   = Color(0xFFE8F5E9),
                            iconTint = Color(0xFF2E7D32),
                            title    = "Sound Effects",
                            subtitle = "Play sounds for actions & alerts",
                            checked  = settingsState.sound,
                            onChange = settingsViewModel::setSound
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsToggleRow(
                            icon     = Icons.Rounded.Vibration,
                            iconBg   = Color(0xFFFFF0EA),
                            iconTint = Color(0xFFE67E22),
                            title    = "Haptic Feedback",
                            subtitle = "Vibrate on taps & interactions",
                            checked  = settingsState.haptics,
                            onChange = settingsViewModel::setHaptics
                        )
                    }
                }

                // ── Storage & Data ────────────────────────────────
                SettingsSectionLabel("Storage & Data")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        // Downloaded Content — real size from filesystem
                        SettingsActionRow(
                            icon         = Icons.Rounded.Download,
                            iconBg       = Color(0xFFEDE7F6),
                            iconTint     = Color(0xFF7E57C2),
                            title        = "Downloaded Content",
                            subtitle     = "Manage offline files",
                            trailingLabel = if (settingsState.isComputingStorage) "…"
                                           else "${"%.1f".format(settingsState.downloadedSizeMb)} MB",
                            onClick      = { navController.navigate(Screen.Downloads.route) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        // Clear Cache — real size + actual deletion
                        SettingsActionRow(
                            icon         = Icons.Rounded.CleaningServices,
                            iconBg       = Color(0xFFFFF3E0),
                            iconTint     = Color(0xFFFF8F00),
                            title        = "Clear Cache",
                            subtitle     = if (settingsState.isClearingCache) "Clearing…" else "Free up storage space",
                            trailingLabel = if (settingsState.isComputingStorage || settingsState.isClearingCache) "…"
                                           else "${"%.1f".format(settingsState.cacheSizeMb)} MB",
                            onClick      = { settingsViewModel.clearCache() }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon     = Icons.Rounded.WifiOff,
                            iconBg   = Color(0xFFE8F5E9),
                            iconTint = Color(0xFF2E7D32),
                            title    = "Offline Mode",
                            subtitle = "Access saved content offline",
                            onClick  = { navController.navigate(Screen.Downloads.route) }
                        )
                    }
                }

                // ── About ─────────────────────────────────────────
                SettingsSectionLabel("About")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Rounded.Info, iconBg = Color(0xFFE3F2FD), iconTint = Color(0xFF1565C0),
                            title = "App Version", subtitle = "BPSCNotes v1.0.0",
                            trailingLabel = "v1.0.0", showArrow = false, onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Star, iconBg = Color(0xFFFFF8E1), iconTint = Color(0xFFFF8F00),
                            title = "Rate the App", subtitle = "Love the app? Leave a review!", onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Share, iconBg = Color(0xFFE8F5E9), iconTint = Color(0xFF2E7D32),
                            title = "Share with Friends", subtitle = "Invite friends & earn 75 coins", onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.PrivacyTip, iconBg = Color(0xFFE8EAF6), iconTint = Color(0xFF3949AB),
                            title = "Privacy Policy", subtitle = "How we handle your data", onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Gavel, iconBg = Color(0xFFF5F5F5), iconTint = Color(0xFF616161),
                            title = "Terms of Service", subtitle = "Our terms & conditions", onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.HeadsetMic, iconBg = Color(0xFFF3E5F5), iconTint = Color(0xFF7B1FA2),
                            title = "Contact Support", subtitle = "Get help from our team", onClick = {}
                        )
                    }
                }

                // ── Account Actions ───────────────────────────────
                SettingsSectionLabel("Account Actions")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        // LOG OUT — calls POST /auth/logout + clears token
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !settingsState.isLoggingOut) { settingsViewModel.logOut() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFCE4EC)), contentAlignment = Alignment.Center) {
                                if (settingsState.isLoggingOut)
                                    CircularProgressIndicator(color = Color(0xFFC62828), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else
                                    Icon(Icons.Rounded.Logout, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (settingsState.isLoggingOut) "Logging out…" else "Log Out",
                                    style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                Text("Sign out of your account",
                                    style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                        // DELETE ACCOUNT — shows confirm dialog first
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !settingsState.isDeletingAccount) { settingsViewModel.showDeleteConfirm() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFCE4EC)), contentAlignment = Alignment.Center) {
                                if (settingsState.isDeletingAccount)
                                    CircularProgressIndicator(color = Color(0xFFC62828), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else
                                    Icon(Icons.Rounded.DeleteForever, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (settingsState.isDeletingAccount) "Deleting account…" else "Delete Account",
                                    style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                Text("Permanently delete all data",
                                    style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                            Icon(Icons.Rounded.KeyboardArrowRight, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────
@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()
        .background(Brush.linearGradient(
            colors = listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
            start = Offset(0f, 0f), end = Offset(600f, 400f)
        ))/*.statusBarsPadding()*/) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.12f))
                .clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.size(36.dp))
        }
    }
}

// ── Account Card ──────────────────────────────────────────────
@Composable
private fun AccountCard(name: String?, email: String?, coins: Int?, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(54.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))),
                contentAlignment = Alignment.Center) {
                Text(
                    name?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "",
                    style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name ?: "Loading…", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                Text(email ?: "", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF8E1))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🪙", fontSize = 11.sp)
                    Text("${coins ?: 0} Coins", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                }
            }
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(BpscColors.PrimaryLight)
                .clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Edit, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────
@Composable
private fun SettingsSectionLabel(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextSecondary,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
}

// ── Toggle row ────────────────────────────────────────────────
@Composable
private fun SettingsToggleRow(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White, checkedTrackColor   = iconTint,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = BpscColors.TextHint.copy(0.3f)
            ))
    }
}

// ── Action row ────────────────────────────────────────────────
@Composable
private fun SettingsActionRow(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    title: String, subtitle: String,
    trailingLabel: String = "", showArrow: Boolean = true,
    isDanger: Boolean = false, onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (isDanger) Color(0xFFC62828) else BpscColors.TextPrimary,
                fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }
        if (trailingLabel.isNotEmpty())
            Text(trailingLabel, style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextHint, fontWeight = FontWeight.SemiBold)
        else if (showArrow)
            Icon(Icons.Rounded.KeyboardArrowRight, null,
                tint = if (isDanger) Color(0xFFC62828) else BpscColors.TextHint,
                modifier = Modifier.size(18.dp))
    }
}
