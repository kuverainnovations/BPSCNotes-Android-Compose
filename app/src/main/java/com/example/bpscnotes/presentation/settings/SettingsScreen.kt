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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bpscnotes.core.language.AppLanguage
import com.example.bpscnotes.core.language.LanguageManager
import com.example.bpscnotes.core.language.LanguagePickerRow
import com.example.bpscnotes.core.language.LocalStrings
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.profile.ProfileViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color


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
    val cs = MaterialTheme.colorScheme

    val settingsState by settingsViewModel.state.collectAsState()
    val profileState  by profileViewModel.uiState.collectAsState()
    val str = LocalStrings.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("settings") }
    val user = profileState.user

    val snackbarHost = remember { SnackbarHostState() }

    // Full restart on logout — kills all ViewModels and clears nav stack
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(settingsState.loggedOut) {
        if (settingsState.loggedOut) {
            val activity = context as? android.app.Activity ?: return@LaunchedEffect
            // Navigate to Login and clear entire back stack
            // FLAG_ACTIVITY_CLEAR_TASK ensures the Activity is re-created fresh
            // so ALL Hilt ViewModels are destroyed (they're tied to NavBackStackEntries)
            val intent = android.content.Intent(activity, activity::class.java).apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)  // no animation — instant switch
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
            title = { Text(str.settingsDeleteConfirmTitle, fontWeight = FontWeight.ExtraBold) },
            text  = {
                Text(
                    str.settingsDeleteConfirmBody,
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = settingsViewModel::deleteAccount,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text(str.settingsDeleteForever) }
            },
            dismissButton = {
                TextButton(onClick = settingsViewModel::hideDeleteConfirm) {
                    Text(str.cancel)
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(cs.background)
        ) {
            SettingsHeader(onBack = { navController.popBackStackSafe() })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Account ────────────────────────────────────────
                SettingsSectionLabel(str.settingsAccount)
                AccountCard(
                    name   = user?.name,
                    email  = user?.email ?: user?.mobile,
                    coins  = user?.coins,
                    onEdit = { navController.navigate(Screen.EditProfile.route) }
                )

                // ── Security ──────────────────────────────────────
                SettingsSectionLabel("Security")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = cs.surface)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        // Change MPIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.ChangeMpin.route) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = BpscColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Change MPIN", style = MaterialTheme.typography.titleSmall,
                                    color = cs.onSurface)
                                Text("Update your 6-digit login MPIN",
                                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)

                        // Biometric toggle
                        // LocalContext.current must be called at composable scope, not inside remember {}
                        val biometricContext = androidx.compose.ui.platform.LocalContext.current
                        val biometricAvailable = remember(biometricContext) {
                            com.example.bpscnotes.core.auth.AppBiometricManager.isAvailable(biometricContext)
                        }
                        if (biometricAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = null,
                                    tint = BpscColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Biometric Login", style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface)
                                    Text("Use fingerprint or face to login",
                                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settingsState.biometricEnabled,
                                    onCheckedChange = { settingsViewModel.setBiometricEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor  = Color.White,
                                        checkedTrackColor  = BpscColors.Primary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Appearance ────────────────────────────────────
                SettingsSectionLabel(str.settingsAppearance)
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon     = Icons.Rounded.DarkMode,
                            iconBg   = Color(0xFF1A237E).copy(0.12f),
                            iconTint = Color(0xFF3949AB).copy(0.4f),
                            title    = str.settingsDarkMode,
                            subtitle = "🚧 Coming soon — not implemented yet",
                            checked  = false,
                            onChange = { /* not yet implemented */ }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        // Language picker
                        LanguagePickerRow()
                    }
                }

                // ── Study Preferences ─────────────────────────────
                SettingsSectionLabel(str.settingsStudyPrefs)
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon     = Icons.Rounded.Alarm,
                            iconBg   = Color(0xFFE3F2FD),
                            iconTint = Color(0xFF1565C0),
                            title    = str.settingsReminder,
                            subtitle = str.settingsReminderSubtitle,
                            checked  = settingsState.studyReminder,
                            onChange = settingsViewModel::setStudyReminder
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsToggleRow(
                            icon     = Icons.Rounded.PlayCircle,
                            iconBg   = Color(0xFFF3E5F5),
                            iconTint = Color(0xFF7B1FA2),
                            title    = str.settingsAutoPlay,
                            subtitle = str.settingsAutoPlaySubtitle,
                            checked  = settingsState.autoPlay,
                            onChange = settingsViewModel::setAutoPlay
                        )
                    }
                }

                // ── Sound & Haptics ───────────────────────────────
                SettingsSectionLabel(str.settingsSound + " & Haptics")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsToggleRow(
                            icon     = Icons.Rounded.VolumeUp,
                            iconBg   = Color(0xFFE8F5E9),
                            iconTint = Color(0xFF2E7D32),
                            title    = str.settingsSound,
                            subtitle = str.settingsSoundSubtitle,
                            checked  = settingsState.sound,
                            onChange = settingsViewModel::setSound
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsToggleRow(
                            icon     = Icons.Rounded.Vibration,
                            iconBg   = Color(0xFFFFF0EA),
                            iconTint = Color(0xFFE67E22),
                            title    = str.settingsHaptics,
                            subtitle = str.settingsHapticsSubtitle,
                            checked  = settingsState.haptics,
                            onChange = settingsViewModel::setHaptics
                        )
                    }
                }

                // ── Storage & Data ────────────────────────────────
                SettingsSectionLabel(str.settingsStorage)
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        // Downloaded Content — real size from filesystem
                        SettingsActionRow(
                            icon         = Icons.Rounded.Download,
                            iconBg       = Color(0xFFEDE7F6),
                            iconTint     = Color(0xFF7E57C2),
                            title        = str.materialsDownloaded + " " + str.settingsTitle,
                            subtitle     = str.settingsManageOffline,
                            trailingLabel = if (settingsState.isComputingStorage) "…"
                            else "${"%.1f".format(settingsState.downloadedSizeMb)} MB",
                            onClick      = { navController.navigate(Screen.Downloads.route) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        // Clear Cache — real size + actual deletion
                        SettingsActionRow(
                            icon         = Icons.Rounded.CleaningServices,
                            iconBg       = Color(0xFFFFF3E0),
                            iconTint     = Color(0xFFFF8F00),
                            title        = str.settingsClearCache,
                            subtitle     = if (settingsState.isClearingCache) str.settingsClearing else str.settingsClearCacheSubtitle,
                            trailingLabel = if (settingsState.isComputingStorage || settingsState.isClearingCache) "…"
                            else "${"%.1f".format(settingsState.cacheSizeMb)} MB",
                            onClick      = { settingsViewModel.clearCache() }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon     = Icons.Rounded.WifiOff,
                            iconBg   = Color(0xFFE8F5E9),
                            iconTint = Color(0xFF2E7D32),
                            title    = str.settingsOffline,
                            subtitle = str.settingsOfflineSubtitle,
                            onClick  = { navController.navigate(Screen.Downloads.route) }
                        )
                    }
                }

                // ── About ─────────────────────────────────────────
                val context = androidx.compose.ui.platform.LocalContext.current
                val packageName = context.packageName

                SettingsSectionLabel(str.settingsAbout)
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Rounded.Info, iconBg = Color(0xFFE3F2FD), iconTint = Color(0xFF1565C0),
                            title = str.settingsVersion, subtitle = str.settingsVersionSubtitle,
                            trailingLabel = str.version, showArrow = false, onClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Star, iconBg = Color(0xFFFFF8E1), iconTint = Color(0xFFFF8F00),
                            title = str.settingsRate, subtitle = str.settingsRateSubtitle,
                            onClick = {
                                try {
                                    // Try opening Play Store app first
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("market://details?id=$packageName"))
                                    )
                                } catch (e: android.content.ActivityNotFoundException) {
                                    // Fallback to browser
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                    )
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Share, iconBg = Color(0xFFE8F5E9), iconTint = Color(0xFF2E7D32),
                            title = str.settingsShare, subtitle = str.settingsShareSubtitle,
                            onClick = {
                                val shareText = "🎯 I'm preparing for BPSC on BPSCNotes!\n\n" +
                                        "Best app for BPSC exam prep — Daily quizzes, Current Affairs, Mock Tests & more.\n\n" +
                                        "Download now: https://play.google.com/store/apps/details?id=$packageName"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share BPSCNotes"))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.PrivacyTip, iconBg = Color(0xFFE8EAF6), iconTint = Color(0xFF3949AB),
                            title = str.settingsPrivacy, subtitle = str.settingsPrivacySubtitle,
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://bpscnotes.in/privacy-policy"))
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.Gavel, iconBg = Color(0xFFF5F5F5), iconTint = Color(0xFF616161),
                            title = str.settingsTerms, subtitle = str.settingsTermsSubtitle,
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://bpscnotes.in/terms-of-service"))
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
                        SettingsActionRow(
                            icon = Icons.Rounded.HeadsetMic, iconBg = Color(0xFFF3E5F5), iconTint = Color(0xFF7B1FA2),
                            title = str.settingsSupport, subtitle = str.settingsSupportSubtitle,
                            onClick = {
                                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:support@bpscnotes.in")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "BPSCNotes App Support")
                                    putExtra(android.content.Intent.EXTRA_TEXT,
                                        "App Version: ${str.version}\nDevice: ${android.os.Build.MODEL}\n\nDescribe your issue:\n")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    // Fallback — open WhatsApp or browser support page
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://bpscnotes.in/support"))
                                    )
                                }
                            }
                        )
                    }
                }

                // ── Account Actions ───────────────────────────────
                SettingsSectionLabel("⚠️  " + str.settingsAccount + " Actions")
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
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
                                Text(if (settingsState.isLoggingOut) str.loading else str.settingsLogout,
                                    style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                Text(str.settingsLogoutSubtitle,
                                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
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
                                Text(if (settingsState.isDeletingAccount) str.loading else str.settingsDeleteAccount,
                                    style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                Text(str.settingsDeleteSubtitle,
                                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
            Text(str.settingsTitle, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.size(36.dp))
        }
    }
}

// ── Account Card ──────────────────────────────────────────────
@Composable
private fun AccountCard(name: String?, email: String?, coins: Int?, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surface),
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
                Text(name ?: str.loading, style = MaterialTheme.typography.titleMedium, color = cs.onBackground, fontWeight = FontWeight.ExtraBold)
                Text(email ?: "", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
}

// ── Toggle row ────────────────────────────────────────────────
@Composable
private fun SettingsToggleRow(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = cs.onBackground, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
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
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
        if (trailingLabel.isNotEmpty())
            Text(trailingLabel, style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        else if (showArrow)
            Icon(Icons.Rounded.KeyboardArrowRight, null,
                tint = if (isDanger) Color(0xFFC62828) else BpscColors.TextHint,
                modifier = Modifier.size(18.dp))
    }
}