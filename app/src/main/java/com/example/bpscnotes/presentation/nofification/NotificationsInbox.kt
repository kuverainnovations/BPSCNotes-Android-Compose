package com.example.bpscnotes.presentation.nofification

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val type: NotifType,
    val isRead: Boolean
)

enum class NotifType {
    DAILY_TARGET, QUIZ, CURRENT_AFFAIRS, JOB_ALERT,
    STREAK, RANK, COINS, LIVE_CLASS, SYSTEM
}


private fun mockNotifications() = listOf(
    NotificationItem("n1",  "🔴 LIVE Class Starting Now",
        "General Science with Prof. Sharma — Join now before it's too late!",
        "Just now",    NotifType.LIVE_CLASS,      false),
    NotificationItem("n2",  "Daily Target Reminder 📚",
        "You've completed 2/5 topics today. 3 more to hit your goal!",
        "10 min ago",  NotifType.DAILY_TARGET,    false),
    NotificationItem("n3",  "🔥 7-Day Streak! Keep it up",
        "Amazing work Rahul! You've studied 7 days in a row. Don't break it!",
        "1 hr ago",    NotifType.STREAK,           false),
    NotificationItem("n4",  "New Current Affairs Added",
        "Today's Bihar & National current affairs are now available. Stay updated!",
        "2 hrs ago",   NotifType.CURRENT_AFFAIRS, false),
    NotificationItem("n5",  "Quiz Result: Polity",
        "You scored 9/10 on Fundamental Rights quiz. You earned 20 coins! 🪙",
        "3 hrs ago",   NotifType.QUIZ,             true),
    NotificationItem("n6",  "🏆 You moved to Rank #3!",
        "You've entered the top 10 on this week's leaderboard. Keep studying!",
        "5 hrs ago",   NotifType.RANK,             true),
    NotificationItem("n7",  "New Job Alert: BPSC 70th CCE",
        "1929 vacancies open. Last date: 28 Feb 2026. Apply before it's too late!",
        "Yesterday",   NotifType.JOB_ALERT,        true),
    NotificationItem("n8",  "🪙 Coins Credited: +75",
        "Referral bonus received! Priya S. joined using your link.",
        "Yesterday",   NotifType.COINS,            true),
    NotificationItem("n9",  "Daily Quiz Available",
        "Today's Geography quiz is live. Answer 10 questions and earn 20 coins!",
        "2 days ago",  NotifType.QUIZ,             true),
    NotificationItem("n10", "App Update Available",
        "BPSCNotes v1.1.0 is ready. New features: Active Recall, Dark Mode.",
        "3 days ago",  NotifType.SYSTEM,           true),
)

private fun notifMeta(type: NotifType): Triple<ImageVector, Color, Color> = when (type) {
    NotifType.DAILY_TARGET    -> Triple(Icons.Rounded.TrackChanges,    Color(0xFFE3F2FD), Color(0xFF1565C0))
    NotifType.QUIZ            -> Triple(Icons.Rounded.Quiz,             Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    NotifType.CURRENT_AFFAIRS -> Triple(Icons.Rounded.Newspaper,       Color(0xFFE8F5E9), Color(0xFF2E7D32))
    NotifType.JOB_ALERT       -> Triple(Icons.Rounded.Work,            Color(0xFFFFF0EA), Color(0xFFE67E22))
    NotifType.STREAK          -> Triple(Icons.Rounded.Whatshot,        Color(0xFFFFF3E0), Color(0xFFFF8F00))
    NotifType.RANK            -> Triple(Icons.Rounded.EmojiEvents,     Color(0xFFFFF8E1), Color(0xFFFF8F00))
    NotifType.COINS           -> Triple(Icons.Rounded.MonetizationOn,  Color(0xFFFFF8E1), Color(0xFFFF8F00))
    NotifType.LIVE_CLASS      -> Triple(Icons.Rounded.PlayCircle,      Color(0xFFFCE4EC), Color(0xFFC62828))
    NotifType.SYSTEM          -> Triple(Icons.Rounded.Settings,        Color(0xFFF5F5F5), Color(0xFF616161))
}

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun NotificationSettingsScreen(navController: NavHostController) {
    val notifications = remember { mockNotifications().toMutableStateList() }
    val unreadCount   = notifications.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
    ) {
        NotifHeader(
            unreadCount = unreadCount,
            onBack      = { navController.popBackStack() },
            onMarkAll   = { notifications.replaceAll { it.copy(isRead = true) } }
        )

        NotifInbox(
            notifications = notifications,
            onRead        = { id ->
                val i = notifications.indexOfFirst { it.id == id }
                if (i >= 0) notifications[i] = notifications[i].copy(isRead = true)
            },
            onDelete      = { id -> notifications.removeAll { it.id == id } }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun NotifHeader(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAll: () -> Unit
) {
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
        // Decorative blobs — same pattern as all other headers
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 18.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Back button
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

                // Title + unread badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Notifications",
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE74C3C))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$unreadCount new",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 10.sp
                            )
                        }
                    }
                }

                // Mark all read button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.12f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(10.dp))
                        .clickable(enabled = unreadCount > 0, onClick = onMarkAll)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .alpha(if (unreadCount > 0) 1f else 0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.DoneAll, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            "Mark all",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// INBOX
// ─────────────────────────────────────────────────────────────

@Composable
private fun NotifInbox(
    notifications: List<NotificationItem>,
    onRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔔", fontSize = 52.sp)
                Text(
                    "All caught up!",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "No new notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary
                )
            }
        }
        return
    }

    val unread = notifications.filter { !it.isRead }
    val read   = notifications.filter {  it.isRead }

    LazyColumn(
        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (unread.isNotEmpty()) {
            item {
                NotifSectionLabel(title = "New", count = unread.size, color = Color(0xFFE74C3C))
            }
            items(unread, key = { it.id }) { notif ->
                NotifCard(notif = notif, onRead = onRead, onDelete = onDelete)
            }
        }

        if (read.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                NotifSectionLabel(title = "Earlier", count = null, color = BpscColors.TextSecondary)
            }
            items(read, key = { it.id }) { notif ->
                NotifCard(notif = notif, onRead = onRead, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun NotifSectionLabel(title: String, count: Int?, color: Color) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            title,
            style      = MaterialTheme.typography.titleMedium,
            color      = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = color,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun NotifCard(
    notif: NotificationItem,
    onRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val (icon, iconBg, iconTint) = notifMeta(notif.type)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { if (!notif.isRead) onRead(notif.id) },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (!notif.isRead) Color.White else BpscColors.Surface
        ),
        elevation = CardDefaults.cardElevation(if (!notif.isRead) 3.dp else 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Unread left accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (!notif.isRead) BpscColors.Primary else Color.Transparent,
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Text(
                            notif.title,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = BpscColors.TextPrimary,
                            fontWeight = if (!notif.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Delete ✕ button
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(BpscColors.Surface)
                                .clickable { onDelete(notif.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        notif.body,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = BpscColors.TextSecondary,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            notif.timeLabel,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = BpscColors.TextHint,
                            fontSize = 10.sp
                        )
                        if (!notif.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(BpscColors.Primary)
                            )
                            Text(
                                "Tap to mark read",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = BpscColors.Primary,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}