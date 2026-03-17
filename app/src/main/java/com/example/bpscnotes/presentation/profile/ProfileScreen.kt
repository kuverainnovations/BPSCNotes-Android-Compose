package com.example.bpscnotes.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.mock.MockData
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

data class SubjectProgress(
    val name: String,
    val emoji: String,
    val progress: Float,
    val color: Color,
    val bgColor: Color
)

data class BadgeItem(
    val emoji: String,
    val name: String,
    val earned: Boolean,
    val bgColor: Color
)

enum class DayStatus { DONE, TODAY, MISSED }

data class WeekDay(val label: String, val status: DayStatus)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(navController: NavHostController) {
    val user = MockData.currentUser

    // Sample data — replace with ViewModel state
    val subjects = listOf(
        SubjectProgress("Polity", "⚖️", 0.82f, Color(0xFF1565C0), Color(0xFFE3F2FD)),
        SubjectProgress("History", "🏛️", 0.65f, Color(0xFFFF8F00), Color(0xFFFFF3E0)),
        SubjectProgress("Geography", "🗺️", 0.48f, Color(0xFF2E7D32), Color(0xFFE8F5E9)),
        SubjectProgress("Economy", "📊", 0.35f, Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
        SubjectProgress("Bihar GK", "🏔️", 0.71f, Color(0xFF00838F), Color(0xFFE0F7FA)),
    )
    val badges = listOf(
        BadgeItem("🔥", "7-Day Streak", true, Color(0xFFFFF8E1)),
        BadgeItem("⚡", "Speed Reader", true, Color(0xFFE3F2FD)),
        BadgeItem("🎯", "Sharpshooter", true, Color(0xFFE8F5E9)),
        BadgeItem("👑", "Top Ranker", false, Color(0xFFF3E5F5)),
        BadgeItem("📚", "100 Topics", false, Color(0xFFFFF3E0)),
    )
    val weekDays = listOf(
        WeekDay("Mon", DayStatus.DONE),
        WeekDay("Tue", DayStatus.DONE),
        WeekDay("Wed", DayStatus.DONE),
        WeekDay("Thu", DayStatus.DONE),
        WeekDay("Fri", DayStatus.DONE),
        WeekDay("Sat", DayStatus.DONE),
        WeekDay("Sun", DayStatus.TODAY),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header (blue gradient hero)
            ProfileHeader(
                name = user.name,
                email = user.email ?: user.mobile,
                coins = user.coinBalance,
                rank = 3,
                rankTitle = "Gold Achiever",
                onEditClick = { navController.navigate(Screen.Settings.route) },
                onShareClick = { /* share profile */ }
            )

            // ── Content lifts over header with rounded top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(BpscColors.Surface)
                    .padding(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatsRow(
                    topicsCompleted = 2,
                    topicsTotal = 5,
                    rank = 3,
                    studyHours = "6.8h",
                    accuracy = 87
                )
                RankProgressCard(rank = 3, rankTitle = "Gold Achiever", points = 2340, nextPoints = 3000)
                WeeklyStreakCard(weekDays = weekDays, streakCount = 7)
                SubjectProgressCard(subjects = subjects, navController = navController)
                BadgesCard(badges = badges)
                ProfileSettingsRow(navController = navController)
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    coins: Int,
    rank: Int,
    rankTitle: String,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A2472),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0),
                        Color(0xFF1E88E5),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 600f)
                )
            )
    ) {
        // Decorative blobs — identical pattern to DashboardHeader
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 160.dp.toPx(),
                center = Offset(size.width + 30.dp.toPx(), -50.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = 90.dp.toPx(),
                center = Offset(size.width - 20.dp.toPx(), size.height * 0.6f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = 55.dp.toPx(),
                center = Offset(-20.dp.toPx(), size.height * 0.8f)
            )
            val dotSpacing = 28.dp.toPx()
            var x = dotSpacing
            while (x < size.width) {
                var y = dotSpacing
                while (y < size.height) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = 1.dp.toPx(),
                        center = Offset(x, y)
                    )
                    y += dotSpacing
                }
                x += dotSpacing
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
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: title + action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable(onClick = onShareClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable(onClick = onEditClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Avatar with gold gradient ring
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFD600), Color(0xFFFF8F00))
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF1A4080)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString(""),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )

            Spacer(Modifier.height(10.dp))

            // Coins + Rank pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🪙", fontSize = 13.sp)
                    Text(
                        "$coins Coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = BpscColors.CoinGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "#$rank · $rankTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STATS ROW  (mirrors the header stats strip in Dashboard)
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileStatsRow(
    topicsCompleted: Int,
    topicsTotal: Int,
    rank: Int,
    studyHours: String,
    accuracy: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("📚", "$topicsCompleted/$topicsTotal", "Topics"),
            Triple("🏆", "#$rank", "Rank"),
            Triple("⏱️", studyHours, "Study"),
            Triple("✅", "$accuracy%", "Accuracy"),
        ).forEach { (icon, value, label) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(icon, fontSize = 16.sp)
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RANK & PROGRESS CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun RankProgressCard(
    rank: Int,
    rankTitle: String,
    points: Int,
    nextPoints: Int
) {
    val progress = points.toFloat() / nextPoints
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200),
        label = "rankProg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Rank & Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = BpscColors.CoinGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        rankTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD600), Color(0xFFFF8F00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "#",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.8f),
                            fontSize = 10.sp
                        )
                        Text(
                            "$rank",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Top 5% this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                        Text(
                            "$points pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BpscColors.Primary,
                        trackColor = BpscColors.PrimaryLight
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${nextPoints - points} pts to #${rank - 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextHint
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WEEKLY STREAK CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeeklyStreakCard(weekDays: List<WeekDay>, streakCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Streak",
                    style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.AccentLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = BpscColors.Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "$streakCount streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Day circles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (day.status) {
                                        DayStatus.DONE -> BpscColors.Primary
                                        DayStatus.TODAY -> BpscColors.CoinGold
                                        DayStatus.MISSED -> BpscColors.Surface
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (day.status) {
                                    DayStatus.DONE -> "✓"
                                    DayStatus.TODAY -> "→"
                                    DayStatus.MISSED -> "✕"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (day.status) {
                                    DayStatus.DONE, DayStatus.TODAY -> Color.White
                                    DayStatus.MISSED -> BpscColors.TextHint
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            day.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Streak info strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = BpscColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$streakCount",
                    style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.Accent,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "day streak — keep it up!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF795548)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SUBJECT PROGRESS CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun SubjectProgressCard(
    subjects: List<SubjectProgress>,
    navController: NavHostController
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleSubjects = if (expanded) subjects else subjects.take(3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Subject Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { navController.navigate(Screen.MyLearning.route) }) {
                    Text(
                        "See all",
                        color = BpscColors.Primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            visibleSubjects.forEachIndexed { index, subject ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                SubjectProgressItem(subject = subject)
            }

            if (subjects.size > 3) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .background(BpscColors.PrimaryLight)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (expanded) "Show Less" else "+ ${subjects.size - 3} more subjects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressItem(subject: SubjectProgress) {
    val animProgress by animateFloatAsState(
        targetValue = subject.progress,
        animationSpec = tween(1000),
        label = "subjectProg_${subject.name}"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(subject.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(subject.emoji, fontSize = 16.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    subject.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${(subject.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = subject.color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = subject.color,
                trackColor = subject.bgColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BADGES CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun BadgesCard(badges: List<BadgeItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Badges",
                    style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.PrimaryLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = BpscColors.Primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "${badges.count { it.earned }} earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { badge ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .width(64.dp)
                            .alpha(if (badge.earned) 1f else 0.4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (badge.earned) badge.bgColor else BpscColors.Surface)
                                .border(
                                    1.dp,
                                    if (badge.earned) BpscColors.Divider else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(badge.emoji, fontSize = 24.sp)
                        }
                        Text(
                            badge.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (badge.earned) BpscColors.TextPrimary else BpscColors.TextHint,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SETTINGS ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileSettingsRow(navController: NavHostController) {
    val items = listOf(
        Triple(Icons.Rounded.MenuBook, "My Courses", Screen.MyLearning.route),
        Triple(Icons.Rounded.EmojiEvents, "Achievements", Screen.Dashboard.route),
        Triple(Icons.Rounded.Settings, "Settings", Screen.Settings.route),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEachIndexed { index, (icon, label, route) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(route) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = BpscColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BpscColors.TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = BpscColors.Divider,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}