package com.example.bpscnotes.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun SettingsScreen(navController: NavHostController) {
    var selectedLanguage by remember { mutableStateOf("English") }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    var notifQuiz by remember { mutableStateOf(true) }
    var notifCurrentAffairs by remember { mutableStateOf(true) }
    var notifStreak by remember { mutableStateOf(true) }
    var notifJobs by remember { mutableStateOf(false) }
    var notifGroupStudy by remember { mutableStateOf(true) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BpscColors.Surface)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0A2472),
                                Color(0xFF1565C0),
                                Color(0xFF1E88E5)
                            ), Offset(0f, 0f), Offset(400f, 400f)
                        )
                    )
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        Color.White.copy(0.05f),
                        160.dp.toPx(),
                        Offset(size.width + 20.dp.toPx(), -50.dp.toPx())
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Customize your experience",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Language ─────────────────────────────────────
            SettingsSectionTitle("🌐 Language & Region")
            SettingsCard {
                SettingsClickRow(
                    icon = Icons.Rounded.Language,
                    color = Color(0xFF1565C0),
                    title = "App Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageSheet = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Notifications ────────────────────────────────
            SettingsSectionTitle("🔔 Notification Preferences")
            SettingsCard {
                SettingsToggleRow(
                    Icons.Rounded.Quiz,
                    Color(0xFF9B59B6),
                    "Daily Quiz Reminder",
                    "Get reminded to complete your daily quiz",
                    notifQuiz
                ) { notifQuiz = it }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsToggleRow(
                    Icons.Rounded.Newspaper,
                    Color(0xFF1565C0),
                    "Current Affairs Alert",
                    "New articles & daily current affairs",
                    notifCurrentAffairs
                ) { notifCurrentAffairs = it }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsToggleRow(
                    Icons.Rounded.Whatshot,
                    Color(0xFFE74C3C),
                    "Streak Reminder",
                    "Don't break your study streak!",
                    notifStreak
                ) { notifStreak = it }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsToggleRow(
                    Icons.Rounded.Work,
                    Color(0xFFE67E22),
                    "Job Alert Notifications",
                    "New govt job vacancies",
                    notifJobs
                ) { notifJobs = it }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsToggleRow(
                    Icons.Rounded.Groups,
                    Color(0xFF1ABC9C),
                    "Group Study Alerts",
                    "When friends join your study room",
                    notifGroupStudy
                ) { notifGroupStudy = it }
            }

            Spacer(Modifier.height(12.dp))

            // ── Storage ──────────────────────────────────────
            SettingsSectionTitle("💾 Storage")
            SettingsCard {
                SettingsClickRow(
                    icon = Icons.Rounded.CleaningServices,
                    color = Color(0xFF1ABC9C),
                    title = "Clear Cache",
                    subtitle = if (cacheCleared) "Cache cleared successfully ✅" else "Free up space — approx 45 MB",
                    onClick = { showClearCacheDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsClickRow(
                    icon = Icons.Rounded.Download,
                    color = Color(0xFF9B59B6),
                    title = "Manage Downloads",
                    subtitle = "View and delete downloaded files",
                    onClick = { navController.navigate(Screen.Downloads.route) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── App Info ─────────────────────────────────────
            SettingsSectionTitle("ℹ️ App Info")
            SettingsCard {
                SettingsClickRow(
                    Icons.Rounded.Info,
                    Color(0xFF1565C0),
                    "App Version",
                    "1.0.0 (Build 100)",
                    onClick = {})
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsClickRow(
                    Icons.Rounded.Policy,
                    Color(0xFF9B59B6),
                    "Privacy Policy",
                    "View our privacy policy",
                    onClick = {})
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsClickRow(
                    Icons.Rounded.Description,
                    Color(0xFFE67E22),
                    "Terms of Service",
                    "Read terms & conditions",
                    onClick = {})
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsClickRow(
                    Icons.Rounded.Star,
                    Color(0xFFFFD700),
                    "Rate the App",
                    "Love BPSCNotes? Rate us ⭐",
                    onClick = {})
            }

            Spacer(Modifier.height(12.dp))

            // ── Danger zone ───────────────────────────────────
            SettingsSectionTitle("⚠️ Danger Zone")
            SettingsCard {
                SettingsClickRow(
                    icon = Icons.Rounded.Logout,
                    color = Color(0xFFE74C3C),
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    titleColor = Color(0xFFE74C3C),
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BpscColors.Divider,
                    thickness = 0.5.dp
                )
                SettingsClickRow(
                    icon = Icons.Rounded.DeleteForever,
                    color = Color(0xFFB71C1C),
                    title = "Delete Account",
                    subtitle = "Permanently delete your account and all data",
                    titleColor = Color(0xFFB71C1C),
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "BPSCNotes · Only What Matters · v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }

        // Dialogs + Sheets
        if (showLanguageSheet) LanguageSheet(
            selected = selectedLanguage,
            onSelect = { selectedLanguage = it; showLanguageSheet = false },
            onDismiss = { showLanguageSheet = false })

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                containerColor = Color.White, shape = RoundedCornerShape(20.dp),
                title = { Text("Clear Cache?", fontWeight = FontWeight.Bold) },
                text = { Text("This will free up approximately 45 MB. Your downloads and account data won't be affected.") },
                confirmButton = {
                    Button(
                        onClick = { cacheCleared = true; showClearCacheDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Clear") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showClearCacheDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Cancel") }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White, shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        "Delete Account?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "This action is permanent and cannot be undone. The following will be deleted:",
                            color = BpscColors.TextPrimary
                        )
                        listOf(
                            "Your profile and account data",
                            "All progress and achievements",
                            "Purchased subscriptions",
                            "Earned coins and rewards"
                        ).forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("•", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BpscColors.TextSecondary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false; navController.navigate(Screen.Login.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Delete Forever") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Cancel") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LANGUAGE SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val languages = listOf(
        Triple("English", "English", "🇬🇧"),
        Triple("Hindi", "हिंदी", "🇮🇳"),
        Triple("Bhojpuri", "भोजपुरी", "🎭"),
        Triple("Maithili", "मैथिली", "📜"),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Select Language",
                style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            HorizontalDivider(color = BpscColors.Divider)
            languages.forEach { (key, display, flag) ->
                val isSel = selected == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) BpscColors.PrimaryLight else BpscColors.Surface)
                        .border(
                            1.dp,
                            if (isSel) BpscColors.Primary else BpscColors.Divider,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelect(key) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(flag, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            key,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSel) BpscColors.Primary else BpscColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            display,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                    if (isSel) Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = BpscColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED COMPOSABLES
// ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = BpscColors.TextSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector, color: Color, title: String, subtitle: String,
    titleColor: Color = BpscColors.TextPrimary, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(0.1f)), contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = BpscColors.TextHint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector, color: Color, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(0.1f)), contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = color
            )
        )
    }
}