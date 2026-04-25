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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.mock.MockData
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

data class SettingsToggle(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val default: Boolean = true
)

data class SettingsAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val trailingLabel: String = "",
    val showArrow: Boolean = true,
    val isDanger: Boolean = false
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(navController: NavHostController) {
    val user = MockData.currentUser

    // Toggle states
    val toggleStates = remember {
        mutableStateMapOf(
            "dark_mode"         to false,
            "offline_mode"      to false,
            "auto_play"         to true,
            "sound"             to true,
            "haptics"           to true,
            "study_reminder"    to true,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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

            // ── Account section
            SettingsSectionLabel("Account")
            AccountCard(
                name   = user.name,
                email  = user.email ?: user.mobile,
                coins  = user.coins,
                onEdit = { navController.navigate(Screen.Profile.route) }
            )

            // ── Appearance section
            SettingsSectionLabel("Appearance")
            SettingsToggleGroup(
                toggles = listOf(
                    SettingsToggle("dark_mode",  "Dark Mode",      "Switch to dark theme",              Icons.Rounded.DarkMode,    Color(0xFF1A237E).copy(0.12f), Color(0xFF3949AB)),
                ),
                toggleStates = toggleStates
            )

            // ── Study Preferences
            SettingsSectionLabel("Study Preferences")
            SettingsToggleGroup(
                toggles = listOf(
                    SettingsToggle("study_reminder", "Daily Study Reminder", "Remind me to study every day",     Icons.Rounded.Alarm,       Color(0xFFE3F2FD), Color(0xFF1565C0)),
                    SettingsToggle("auto_play",      "Auto-play Videos",     "Play next video automatically",    Icons.Rounded.PlayCircle,  Color(0xFFF3E5F5), Color(0xFF7B1FA2)),
                ),
                toggleStates = toggleStates
            )

            // ── Sound & Haptics
            SettingsSectionLabel("Sound & Haptics")
            SettingsToggleGroup(
                toggles = listOf(
                    SettingsToggle("sound",   "Sound Effects",  "Play sounds for actions & alerts", Icons.Rounded.VolumeUp,    Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                    SettingsToggle("haptics", "Haptic Feedback","Vibrate on taps & interactions",   Icons.Rounded.Vibration,   Color(0xFFFFF0EA), Color(0xFFE67E22)),
                ),
                toggleStates = toggleStates
            )

            // ── Storage & Data
            SettingsSectionLabel("Storage & Data")
            SettingsActionGroup(
                actions = listOf(
                    SettingsAction("Downloaded Content",  "Manage offline files",           Icons.Rounded.Download,      Color(0xFFEDE7F6), Color(0xFF7E57C2), trailingLabel = "128 MB"),
                    SettingsAction("Clear Cache",         "Free up storage space",          Icons.Rounded.CleaningServices, Color(0xFFFFF3E0), Color(0xFFFF8F00), trailingLabel = "24 MB"),
                    SettingsAction("Offline Mode",        "Access saved content offline",   Icons.Rounded.WifiOff,       Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                ),
                onAction = { /* handle */ }
            )

            // ── About
            SettingsSectionLabel("About")
            SettingsActionGroup(
                actions = listOf(
                    SettingsAction("App Version",         "BPSCNotes v1.0.0",               Icons.Rounded.Info,          Color(0xFFE3F2FD), Color(0xFF1565C0), trailingLabel = "v1.0.0", showArrow = false),
                    SettingsAction("Rate the App",        "Love the app? Leave a review!",  Icons.Rounded.Star,          Color(0xFFFFF8E1), Color(0xFFFF8F00)),
                    SettingsAction("Share with Friends",  "Invite friends & earn 75 coins", Icons.Rounded.Share,         Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                    SettingsAction("Privacy Policy",      "How we handle your data",        Icons.Rounded.PrivacyTip,    Color(0xFFE8EAF6), Color(0xFF3949AB)),
                    SettingsAction("Terms of Service",    "Our terms & conditions",         Icons.Rounded.Gavel,         Color(0xFFF5F5F5), Color(0xFF616161)),
                    SettingsAction("Contact Support",     "Get help from our team",         Icons.Rounded.HeadsetMic,    Color(0xFFF3E5F5), Color(0xFF7B1FA2)),
                ),
                onAction = { /* handle */ }
            )

            // ── Danger zone
            SettingsSectionLabel("Account Actions")
            SettingsActionGroup(
                actions = listOf(
                    SettingsAction("Log Out",             "Sign out of your account",       Icons.Rounded.Logout,        Color(0xFFFCE4EC), Color(0xFFC62828), isDanger = true),
                    SettingsAction("Delete Account",      "Permanently delete all data",    Icons.Rounded.DeleteForever, Color(0xFFFCE4EC), Color(0xFFC62828), isDanger = true),
                ),
                onAction = { title ->
                    if (title == "Log Out") {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF051D56),
                        Color(0xFF0A2472),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0),
                        Color(0xFF1976D2),
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(600f, 400f)
                )
            )
            .statusBarsPadding()
    ) {
        // Decorative blobs
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 30.dp.toPx(), -50.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.75f))
            val sp = 28.dp.toPx()
            var x = sp
            while (x < size.width) {
                var y = sp
                while (y < size.height) {
                    drawCircle(Color.White.copy(0.05f), 1.dp.toPx(), Offset(x, y))
                    y += sp
                }
                x += sp
            }
        }

        // Shiny top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(0.3f),
                            Color.White.copy(0.7f),
                            Color.White.copy(0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Back button — CircleShape matching reference
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.12f))
                    .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Text(
                "Settings",
                style      = MaterialTheme.typography.titleLarge,
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            // Balance spacer
            Spacer(Modifier.size(36.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACCOUNT CARD  — profile summary at top
// ─────────────────────────────────────────────────────────────

@Composable
private fun AccountCard(
    name: String,
    email: String?,
    coins: Int,
    onEdit: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name,  style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary,   fontWeight = FontWeight.ExtraBold)
                Text(email?:"", style = MaterialTheme.typography.bodyMedium,  color = BpscColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🪙", fontSize = 11.sp)
                    Text(
                        "$coins Coins",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = BpscColors.CoinGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Edit profile button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BpscColors.PrimaryLight)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Edit, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION LABEL
// ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        color      = BpscColors.TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// TOGGLE GROUP
// ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleGroup(
    toggles: List<SettingsToggle>,
    toggleStates: MutableMap<String, Boolean>
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            toggles.forEachIndexed { index, toggle ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(toggle.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(toggle.icon, null, tint = toggle.iconTint, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(toggle.title,    style = MaterialTheme.typography.bodyLarge,  color = BpscColors.TextPrimary,   fontWeight = FontWeight.SemiBold)
                        Text(toggle.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                    Switch(
                        checked         = toggleStates[toggle.key] ?: toggle.default,
                        onCheckedChange = { toggleStates[toggle.key] = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = toggle.iconTint,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BpscColors.TextHint.copy(0.3f)
                        )
                    )
                }
                if (index < toggles.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACTION GROUP
// ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsActionGroup(
    actions: List<SettingsAction>,
    onAction: (String) -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            actions.forEachIndexed { index, action ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(action.title) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(action.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.icon, null, tint = action.iconTint, modifier = Modifier.size(20.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.title,
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = if (action.isDanger) Color(0xFFC62828) else BpscColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            action.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }

                    // Trailing — either a label or a chevron
                    if (action.trailingLabel.isNotEmpty()) {
                        Text(
                            action.trailingLabel,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = BpscColors.TextHint,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (action.showArrow) {
                        Icon(
                            Icons.Rounded.KeyboardArrowRight,
                            null,
                            tint     = if (action.isDanger) Color(0xFFC62828) else BpscColors.TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (index < actions.size - 1) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        color     = BpscColors.Divider,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}