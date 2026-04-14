package com.example.bpscnotes.presentation.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class NotifCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val bg: Color,
    val previewText: String,
    val defaultFrequency: String = "Daily",
)

data class NotifState(
    val enabled: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val frequency: String = "Daily",
    val time: String = "8:00 AM",
)

enum class FrequencyOption(val label: String) {
    Daily("Daily"), Weekly("Weekly"), Instant("Instant")
}

val notifCategories = listOf(
    NotifCategory("quiz",      "Daily Quiz Reminder",       "Remind to complete daily quiz",           Icons.Rounded.Quiz,          Color(0xFF9B59B6), Color(0xFFF3E8FD), "🎯 Today's quiz is ready! Polity — 10 questions. Attempt now!"),
    NotifCategory("affairs",   "Current Affairs Alert",     "New articles & daily current affairs",    Icons.Rounded.Newspaper,     Color(0xFF1565C0), Color(0xFFE8F0FD), "📰 Today's Current Affairs are live! 15 important news for BPSC 70th."),
    NotifCategory("streak",    "Streak Reminder",           "Don't break your study streak",           Icons.Rounded.Whatshot,      Color(0xFFE74C3C), Color(0xFFFEE8E8), "🔥 Your 7-day streak is at risk! Study for just 15 mins to keep it alive."),
    NotifCategory("jobs",      "Job Vacancy Alerts",        "New govt job vacancies posted",           Icons.Rounded.Work,          Color(0xFFE67E22), Color(0xFFFFF0EA), "💼 New! BPSC 71st CCE notification released. Apply before 30 April 2026."),
    NotifCategory("study",     "Group Study Room Alerts",   "When friends join your study room",       Icons.Rounded.Groups,        Color(0xFF1ABC9C), Color(0xFFE8FDF8), "👥 Priya Singh joined your study room 'Polity Masters'. Join now!"),
    NotifCategory("mock",      "Mock Test Reminders",       "Scheduled mock test alerts",              Icons.Rounded.Assignment,    Color(0xFF2980B9), Color(0xFFE8F4FD), "📝 Your Mock Test #5 starts in 30 minutes. Get ready!"),
    NotifCategory("coins",     "Coin & Reward Alerts",      "Coins earned, rewards & offers",          Icons.Rounded.MonetizationOn,Color(0xFFF39C12), Color(0xFFFFF8E1), "🪙 You earned 50 coins for 7-day streak! Balance: 192 coins."),
    NotifCategory("live",      "Live Class Reminders",      "Upcoming live class notifications",       Icons.Rounded.LiveTv,        Color(0xFFE74C3C), Color(0xFFFEE8E8), "🔴 Live Class starts in 15 min — Bihar GK with Rahul Sir. Tap to join!"),
)

val subjects = listOf("Polity", "History", "Geography", "Economy", "Bihar GK", "Science", "Current Affairs")

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun NotificationSettingsScreen(navController: NavHostController) {

    // Master toggle
    var masterEnabled       by remember { mutableStateOf(true) }
    var masterSound         by remember { mutableStateOf(true) }
    var masterVibration     by remember { mutableStateOf(true) }

    // Per-category states
    val notifStates = remember {
        mutableStateMapOf<String, NotifState>().apply {
            notifCategories.forEach { cat -> put(cat.id, NotifState()) }
        }
    }

    // Quiet hours
    var quietHoursEnabled   by remember { mutableStateOf(true) }
    var quietStart          by remember { mutableStateOf("10:00 PM") }
    var quietEnd            by remember { mutableStateOf("6:00 AM") }

    // Per-subject alerts
    val subjectAlerts       = remember { mutableStateMapOf<String, Boolean>().apply { subjects.forEach { put(it, true) } } }

    // Preview + time picker sheets
    var previewCategory     by remember { mutableStateOf<NotifCategory?>(null) }
    var timePickerCategory  by remember { mutableStateOf<String?>(null) }
    var showQuietSheet      by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Header ───────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            Offset(0f, 0f), Offset(400f, 400f)
                        ))
                        .statusBarsPadding()
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                        drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                    }
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Notification Settings", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Control how BPSCNotes notifies you", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }

                        // Master toggle card
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)), elevation = CardDefaults.cardElevation(0.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (masterEnabled) Color(0xFFFFD700) else Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(if (masterEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, null,
                                        tint = if (masterEnabled) Color(0xFF1A1A00) else Color.White, modifier = Modifier.size(22.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("All Notifications", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text(if (masterEnabled) "Notifications are enabled" else "All notifications are muted", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                                }
                                Switch(
                                    checked = masterEnabled,
                                    onCheckedChange = {
                                        masterEnabled = it
                                        if (!it) notifStates.keys.forEach { key -> notifStates[key] = notifStates[key]!!.copy(enabled = false) }
                                        else notifStates.keys.forEach { key -> notifStates[key] = notifStates[key]!!.copy(enabled = true) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1A1A00), checkedTrackColor = Color(0xFFFFD700), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.White.copy(0.3f))
                                )
                            }
                        }

                        // Global sound + vibration
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlobalToggleChip("🔔", "Sound", masterSound, modifier = Modifier.weight(1f)) { masterSound = it; notifStates.keys.forEach { key -> notifStates[key] = notifStates[key]!!.copy(sound = it) } }
                            GlobalToggleChip("📳", "Vibration", masterVibration, modifier = Modifier.weight(1f)) { masterVibration = it; notifStates.keys.forEach { key -> notifStates[key] = notifStates[key]!!.copy(vibration = it) } }
                        }
                    }
                }
            }

            // ── Quiet hours ───────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                NotifSectionTitle("🌙 Do Not Disturb")
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A237E).copy(0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DarkMode, null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quiet Hours", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("No notifications during this time", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                            Switch(checked = quietHoursEnabled, onCheckedChange = { quietHoursEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1A237E)))
                        }
                        AnimatedVisibility(visible = quietHoursEnabled) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(color = BpscColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // From time
                                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(BpscColors.Surface)
                                        .border(1.dp, BpscColors.Divider, RoundedCornerShape(12.dp)).clickable { showQuietSheet = true }.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("FROM", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(quietStart, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                        Text("Quiet starts", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
                                    }
                                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Icon(Icons.Rounded.ArrowForward, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                                    }
                                    // To time
                                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(BpscColors.Surface)
                                        .border(1.dp, BpscColors.Divider, RoundedCornerShape(12.dp)).clickable { showQuietSheet = true }.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("TO", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(quietEnd, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                        Text("Quiet ends", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                    .clip(RoundedCornerShape(10.dp)).background(Color(0xFFE8EAF6)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Info, null, tint = Color(0xFF1A237E), modifier = Modifier.size(14.dp))
                                    Text("Notifications will be paused from $quietStart to $quietEnd",
                                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1A237E))
                                }
                            }
                        }
                    }
                }
            }

            // ── Notification categories ───────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                NotifSectionTitle("🔔 Notification Types")
                Spacer(Modifier.height(8.dp))
            }

            items(notifCategories) { category ->
                val state = notifStates[category.id] ?: NotifState()
                val isEnabled = masterEnabled && state.enabled

                NotifCategoryCard(
                    category    = category,
                    state       = state,
                    masterEnabled = masterEnabled,
                    onToggle    = { notifStates[category.id] = state.copy(enabled = it) },
                    onSoundToggle = { notifStates[category.id] = state.copy(sound = it) },
                    onVibToggle = { notifStates[category.id] = state.copy(vibration = it) },
                    onFrequency = { notifStates[category.id] = state.copy(frequency = it) },
                    onTimePick  = { timePickerCategory = category.id },
                    onPreview   = { previewCategory = category },
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Per-subject alerts ────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                NotifSectionTitle("📚 Per-Subject Alerts")
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Get notified for new content in specific subjects", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
                        subjects.forEachIndexed { index, subject ->
                            val isOn = subjectAlerts[subject] ?: true
                            val subColor = subjectColor(subject)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(subColor.copy(0.1f)), contentAlignment = Alignment.Center) {
                                    Text(subjectEmoji(subject), fontSize = 16.sp)
                                }
                                Text(subject, style = MaterialTheme.typography.bodyLarge, color = if (isOn) BpscColors.TextPrimary else BpscColors.TextHint, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Switch(checked = isOn, onCheckedChange = { subjectAlerts[subject] = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = subColor))
                            }
                            if (index < subjects.size - 1) HorizontalDivider(color = BpscColors.Divider, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // ── System settings shortcut ──────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Settings, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Notification Settings", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Open Android notification settings for BPSCNotes", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                        Icon(Icons.Rounded.OpenInNew, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // ── Notification Preview Sheet ────────────────────────
        previewCategory?.let { cat ->
            NotifPreviewSheet(category = cat, onDismiss = { previewCategory = null })
        }

        // ── Time Picker Sheet ─────────────────────────────────
        timePickerCategory?.let { catId ->
            val category = notifCategories.find { it.id == catId }
            val state    = notifStates[catId] ?: NotifState()
            if (category != null) {
                TimePickerSheet(
                    category  = category,
                    currentTime = state.time,
                    onConfirm = { time -> notifStates[catId] = state.copy(time = time); timePickerCategory = null },
                    onDismiss = { timePickerCategory = null }
                )
            }
        }

        // ── Quiet Hours Sheet ─────────────────────────────────
        if (showQuietSheet) {
            QuietHoursSheet(
                quietStart = quietStart,
                quietEnd   = quietEnd,
                onConfirm  = { start, end -> quietStart = start; quietEnd = end; showQuietSheet = false },
                onDismiss  = { showQuietSheet = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NOTIFICATION CATEGORY CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun NotifCategoryCard(
    category: NotifCategory,
    state: NotifState,
    masterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibToggle: (Boolean) -> Unit,
    onFrequency: (String) -> Unit,
    onTimePick: () -> Unit,
    onPreview: () -> Unit,
) {
    val isActive = masterEnabled && state.enabled

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main row
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) category.bg else BpscColors.Surface)
                    .alpha(if (isActive) 1f else 0.5f), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = if (isActive) category.color else BpscColors.TextHint, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.bodyLarge, color = if (isActive) BpscColors.TextPrimary else BpscColors.TextHint, fontWeight = FontWeight.Bold)
                    Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Switch(checked = state.enabled, onCheckedChange = onToggle, enabled = masterEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = category.color, disabledCheckedTrackColor = BpscColors.Divider))
            }

            // Expanded controls when enabled
            AnimatedVisibility(visible = isActive) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = BpscColors.Divider, modifier = Modifier.padding(horizontal = 14.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Sound + Vibration row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniToggle("🔔", "Sound", state.sound, category.color, Modifier.weight(1f)) { onSoundToggle(it) }
                            MiniToggle("📳", "Vibration", state.vibration, category.color, Modifier.weight(1f)) { onVibToggle(it) }
                        }

                        // Frequency selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Frequency", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FrequencyOption.values().forEach { freq ->
                                    val isSel = state.frequency == freq.label
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) category.color else BpscColors.Surface)
                                        .border(1.dp, if (isSel) category.color else BpscColors.Divider, RoundedCornerShape(10.dp))
                                        .clickable { onFrequency(freq.label) }.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text(freq.label, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else BpscColors.TextSecondary, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        // Time + Preview row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Time picker trigger
                            Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(BpscColors.Surface).border(1.dp, BpscColors.Divider, RoundedCornerShape(10.dp))
                                .clickable(onClick = onTimePick).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Schedule, null, tint = category.color, modifier = Modifier.size(14.dp))
                                Text(state.time, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.Edit, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                            }
                            // Preview button
                            Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(category.bg).border(1.dp, category.color.copy(0.3f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onPreview).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Rounded.Visibility, null, tint = category.color, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Preview", style = MaterialTheme.typography.bodyMedium, color = category.color, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NOTIFICATION PREVIEW SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifPreviewSheet(category: NotifCategory, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Notification Preview", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text("This is how the notification will appear on your device", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
            HorizontalDivider(color = BpscColors.Divider)

            // Phone mockup notification
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1A2E)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Status bar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("9:41", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.SignalCellularAlt, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Icon(Icons.Rounded.Wifi, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Icon(Icons.Rounded.BatteryFull, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Notification card
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF2C2C3E)).padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(category.bg), contentAlignment = Alignment.Center) {
                                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("BPSCNotes", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 10.sp)
                                    Text("now", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f), fontSize = 10.sp)
                                }
                                Text(category.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(category.previewText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            // Category info
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(category.bg).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Got it!", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TIME PICKER SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(
    category: NotifCategory,
    currentTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val times = listOf("6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM")
    var selectedTime by remember { mutableStateOf(currentTime) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(category.bg), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = category.color, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Set Reminder Time", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Text(category.title, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
            }
            HorizontalDivider(color = BpscColors.Divider)
            // Time grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                times.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { time ->
                            val isSel = selectedTime == time
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) category.color else BpscColors.Surface)
                                .border(1.dp, if (isSel) category.color else BpscColors.Divider, RoundedCornerShape(10.dp))
                                .clickable { selectedTime = time }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center) {
                                Text(time, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else BpscColors.TextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Button(onClick = { onConfirm(selectedTime) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = category.color)) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Set reminder at $selectedTime", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIET HOURS SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietHoursSheet(
    quietStart: String,
    quietEnd: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val times = listOf("6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM", "10:00 PM", "11:00 PM", "12:00 AM", "1:00 AM", "2:00 AM", "3:00 AM", "4:00 AM", "5:00 AM", "6:00 AM")
    val nightTimes = listOf("8:00 PM", "9:00 PM", "10:00 PM", "11:00 PM", "12:00 AM")
    val mornTimes  = listOf("4:00 AM", "5:00 AM", "6:00 AM", "7:00 AM", "8:00 AM")
    var selStart by remember { mutableStateOf(quietStart) }
    var selEnd   by remember { mutableStateOf(quietEnd) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("⏰ Set Quiet Hours", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text("No notifications will be sent during quiet hours", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
            HorizontalDivider(color = BpscColors.Divider)

            Text("Quiet starts at:", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nightTimes.forEach { time ->
                    val isSel = selStart == time
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) Color(0xFF1A237E) else BpscColors.Surface)
                        .border(1.dp, if (isSel) Color(0xFF1A237E) else BpscColors.Divider, RoundedCornerShape(10.dp))
                        .clickable { selStart = time }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(time, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else BpscColors.TextSecondary, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp)
                    }
                }
            }

            Text("Quiet ends at:", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mornTimes.forEach { time ->
                    val isSel = selEnd == time
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) BpscColors.Primary else BpscColors.Surface)
                        .border(1.dp, if (isSel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(10.dp))
                        .clickable { selEnd = time }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(time, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else BpscColors.TextSecondary, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8EAF6)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🌙", fontSize = 16.sp)
                Text("Quiet from $selStart to $selEnd", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1A237E), fontWeight = FontWeight.SemiBold)
            }

            Button(onClick = { onConfirm(selStart, selEnd) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) {
                Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Quiet Hours", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun NotifSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun GlobalToggleChip(emoji: String, label: String, checked: Boolean, modifier: Modifier, onToggle: (Boolean) -> Unit) {
    Row(modifier = modifier.clip(RoundedCornerShape(12.dp))
        .background(if (checked) Color.White else Color.White.copy(0.15f))
        .clickable { onToggle(!checked) }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, fontSize = 16.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (checked) BpscColors.Primary else Color.White, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (checked) Icon(Icons.Rounded.Check, null, tint = BpscColors.Primary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun MiniToggle(emoji: String, label: String, checked: Boolean, color: Color, modifier: Modifier, onToggle: (Boolean) -> Unit) {
    Row(modifier = modifier.clip(RoundedCornerShape(10.dp))
        .background(if (checked) color.copy(0.08f) else BpscColors.Surface)
        .border(1.dp, if (checked) color.copy(0.3f) else BpscColors.Divider, RoundedCornerShape(10.dp))
        .clickable { onToggle(!checked) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(emoji, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (checked) color else BpscColors.TextHint, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (checked) color else BpscColors.Surface).border(1.dp, if (checked) color else BpscColors.Divider, CircleShape), contentAlignment = Alignment.Center) {
            if (checked) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
    }
}

private fun subjectColor(subject: String) = when (subject) {
    "Polity"          -> Color(0xFF9B59B6)
    "History"         -> Color(0xFFE74C3C)
    "Geography"       -> Color(0xFF1ABC9C)
    "Economy"         -> Color(0xFFE67E22)
    "Bihar GK"        -> Color(0xFFF39C12)
    "Science"         -> Color(0xFF2ECC71)
    "Current Affairs" -> Color(0xFF1565C0)
    else              -> BpscColors.Primary
}

private fun subjectEmoji(subject: String) = when (subject) {
    "Polity"          -> "⚖️"
    "History"         -> "🏛️"
    "Geography"       -> "🗺️"
    "Economy"         -> "💰"
    "Bihar GK"        -> "🏔️"
    "Science"         -> "🔬"
    "Current Affairs" -> "📰"
    else              -> "📚"
}