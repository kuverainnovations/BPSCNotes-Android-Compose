package com.example.bpscnotes.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ui.t.LocalDarkMode
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import java.time.LocalDate

data class SubjectProgress(
    val name: String, val emoji: String,
    val progress: Float, val color: Color, val bgColor: Color
)
data class BadgeItem(
    val emoji: String, val name: String,
    val description: String = "", val earned: Boolean,
    val bgColor: Color, val earnedDate: String? = null
)
enum class DayStatus { DONE, TODAY, MISSED }
data class WeekDay(val label: String, val status: DayStatus)

// ── Rank tier from coins ──────────────────────────────────────
private fun rankTier(coins: Int): Triple<String, Int, Int> {
    // Triple(title, currentTierMin, nextTierMin)
    return when {
        coins < 500    -> Triple("Beginner",   0,    500)
        coins < 1500   -> Triple("Explorer",   500,  1500)
        coins < 3000   -> Triple("Achiever",   1500, 3000)
        coins < 6000   -> Triple("Expert",     3000, 6000)
        coins < 12000  -> Triple("Champion",   6000, 12000)
        else           -> Triple("Legend",     12000, 20000)
    }
}

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsState()
    val str     = LocalStrings.current
    val isDark  = LocalDarkMode.current
    val cs      = MaterialTheme.colorScheme
    val user    = state.user

    val uiSubjects = state.subjects.map {
        SubjectProgress(it.name, it.emoji, it.progress, it.color, it.bgColor)
    }
    val uiBadges = state.badges.map {
        BadgeItem(it.emoji, it.name, it.description, it.earned, it.bgColor, it.earnedDate)
    }
    val uiWeekDays = state.weekDays.map { WeekDay(it.label, it.status) }

    if (state.isLoading && user == null) {
        com.example.bpscnotes.core.ui.AppLoader(message = "Loading profile…")
        return
    }
    state.error?.let { err ->
        if (user == null) {
            com.example.bpscnotes.core.ui.AppErrorState(message = err, onRetry = { viewModel.load() })
            return
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            ProfileHeader(
                name        = user?.name ?: "",
                email       = user?.email ?: user?.mobile,
                coins       = user?.coins ?: 0,
                rank        = user?.rank ?: 0,
                isDark      = isDark,
                onEditClick = { navController.navigate(Screen.EditProfile.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onShareClick = {
                    val shareText = "I'm preparing for BPSC on BPSCNotes! 🎯\nhttps://bpscnotes.in"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, str.profileShare))
                }
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(cs.background)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatsRow(
                    rank        = user?.rank ?: 0,
                    studyHours  = "${(user?.totalStudyMinutes ?: 0) / 60}h",
                    accuracy    = user?.accuracy?.toDoubleOrNull()?.toInt() ?: 0,
                    quizzes     = user?.quizzesAttempted ?: 0,
                    isDark      = isDark
                )

                val (rankTitle, tierMin, tierMax) = rankTier(user?.coins ?: 0)
                RankProgressCard(
                    rank          = user?.rank ?: 0,
                    rankTitle     = rankTitle,
                    tierMin       = tierMin,
                    coins         = user?.coins ?: 0,
                    tierMax       = tierMax,
                    isDark        = isDark
                )

                WeeklyStreakCard(weekDays = uiWeekDays, streakCount = user?.streak ?: 0, isDark = isDark)

                StudyHeatmapCard(studyDays = state.studyHeatmap, isDark = isDark)

                SubjectProgressCard(subjects = uiSubjects, navController = navController, isDark = isDark)

                BadgesCard(badges = uiBadges, isDark = isDark)

                CoinWalletSection(
                    balance      = user?.coins ?: 0,
                    transactions = state.recentTransactions,
                    onViewAll    = { navController.navigate(Screen.CoinWallet.route) }
                )

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ── HEADER ─────────────────────────────────────────────────────
@Composable
private fun ProfileHeader(
    name: String, email: String?,
    coins: Int, rank: Int, isDark: Boolean,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val str = LocalStrings.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("profile") }
    val (rankTitle, _, _) = rankTier(coins)

    Box(modifier = Modifier.fillMaxWidth().height(280.dp)
        .background(Brush.linearGradient(
            listOf(Color(0xFF0A2472), Color(0xFF0D47A1), Color(0xFF1565C0)),
            Offset(0f, 0f), Offset(400f, 500f)
        ))) {
        // Dot grid
        Canvas(Modifier.matchParentSize()) {
            val dot = 22.dp.toPx()
            var x = dot
            while (x < size.width) {
                var y = dot
                while (y < size.height) {
                    drawCircle(Color.White.copy(0.05f), 1.2.dp.toPx(), Offset(x, y))
                    y += dot
                }; x += dot
            }
            drawCircle(Color.White.copy(0.06f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -30.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.75f))
        }

        Column(modifier = Modifier.fillMaxSize()
            .padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            // Top row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(str.profileTitle, style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Icons.Rounded.Share to onShareClick,
                        Icons.Rounded.Edit to onEditClick,
                        Icons.Rounded.Settings to onSettingsClick,
                    ).forEach { (icon, action) ->
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable(onClick = action),
                            contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Avatar
            Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF8F00))))
                .padding(3.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .background(Color(0xFF1A4080)),
                    contentAlignment = Alignment.Center) {
                    Text(
                        name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            if (!email.isNullOrBlank())
                Text(email, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.65f))

            Spacer(Modifier.height(10.dp))

            // Pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("🪙 $coins ${str.coins}", BpscColors.CoinGold)
                if (rank > 0) Pill("🏆 #$rank · $rankTitle", Color.White.copy(0.85f))
            }
        }
    }
}

@Composable
private fun Pill(text: String, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(0.12f))
        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(20.dp))
        .padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
    }
}

// ── STATS ROW ──────────────────────────────────────────────────
@Composable
private fun ProfileStatsRow(rank: Int, studyHours: String, accuracy: Int, quizzes: Int, isDark: Boolean) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("🏆", if (rank > 0) "#$rank" else "—", "Rank"),
            Triple("⏱️", studyHours,              str.profileStudy),
            Triple("✅", "$accuracy%",            "Accuracy"),
            Triple("📝", "$quizzes",              "Quizzes"),
        ).forEach { (icon, value, label) ->
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(icon, fontSize = 15.sp)
                    Text(value, style = MaterialTheme.typography.titleSmall,
                        color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ── RANK PROGRESS CARD ─────────────────────────────────────────
@Composable
private fun RankProgressCard(
    rank: Int, rankTitle: String, tierMin: Int,
    coins: Int, tierMax: Int, isDark: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val str          = LocalStrings.current
    val progress     = ((coins - tierMin).toFloat() / (tierMax - tierMin)).coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(progress, tween(1200), label = "rankProg")
    val ptsToNext    = (tierMax - coins).coerceAtLeast(0)

    // Next tier name
    val (_, nextTierMin, _) = rankTier(tierMax)
    val nextTierName         = rankTier(tierMax).first

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(str.profileRank, style = MaterialTheme.typography.titleMedium,
                    color = cs.onBackground, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = BpscColors.CoinGold, modifier = Modifier.size(14.dp))
                    Text(rankTitle, style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Rank badge
                Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF8F00)))),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("#", style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.7f), fontSize = 10.sp)
                        Text("${if (rank > 0) rank else "—"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("$coins ${str.coins}", style = MaterialTheme.typography.bodyMedium,
                            color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                        Text("→ $nextTierName", style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Progress bar with tier labels
                    Box {
                        LinearProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color    = BpscColors.Primary,
                            trackColor = cs.surfaceVariant
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(rankTitle, style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        if (ptsToNext > 0)
                            Text("$ptsToNext 🪙 to $nextTierName",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant, fontSize = 9.sp)
                        else
                            Text(str.profileMaxTier, style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── WEEKLY STREAK ──────────────────────────────────────────────
@Composable
private fun WeeklyStreakCard(weekDays: List<WeekDay>, streakCount: Int, isDark: Boolean) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(str.profileWeeklyStreak, style = MaterialTheme.typography.titleMedium,
                    color = cs.onBackground, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(BpscColors.AccentLight)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Whatshot, null, tint = BpscColors.Accent, modifier = Modifier.size(14.dp))
                    Text("$streakCount ${str.profileStreak}", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Accent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                weekDays.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(when (day.status) {
                                DayStatus.DONE   -> BpscColors.Primary
                                DayStatus.TODAY  -> BpscColors.CoinGold
                                DayStatus.MISSED -> cs.surfaceVariant
                            }), contentAlignment = Alignment.Center) {
                            Text(
                                when (day.status) {
                                    DayStatus.DONE   -> "✓"
                                    DayStatus.TODAY  -> "→"
                                    DayStatus.MISSED -> "·"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (day.status) {
                                    DayStatus.DONE, DayStatus.TODAY -> Color.White
                                    DayStatus.MISSED -> cs.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                        Text(day.label, style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF8E1)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = BpscColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("$streakCount", style = MaterialTheme.typography.titleLarge,
                    color = BpscColors.Accent, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(6.dp))
                Text(str.profileDayStreak, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF795548))
            }
        }
    }
}

// ── HEATMAP — 1 month (4 weeks × 7 days) with large clear cells ──
@Composable
private fun StudyHeatmapCard(studyDays: List<Int>, isDark: Boolean) {
    val cs = MaterialTheme.colorScheme
    val str       = LocalStrings.current
    // 28 days = 4 weeks
    val totalDays = 28
    val days = if (studyDays.size >= totalDays)
        studyDays.takeLast(totalDays)
    else
        List(totalDays - studyDays.size) { 0 } + studyDays
    val maxMins = days.maxOrNull()?.coerceAtLeast(1) ?: 1

    var selectedIdx by remember { mutableStateOf(-1) }
    val dayNames = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 16.sp)
                    Text(str.profileHeatmap, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
                }
                Text(str.profileLast28, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }

            // Tap tooltip
            if (selectedIdx >= 0) {
                val mins    = days.getOrElse(selectedIdx) { 0 }
                val daysAgo = totalDays - 1 - selectedIdx
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (mins > 0) BpscColors.Primary.copy(0.08f) else cs.outline.copy(0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (mins == 0) "😴" else "🔥", fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (daysAgo) { 0 -> "Today"; 1 -> "Yesterday"; else -> "$daysAgo days ago" },
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (mins == 0) "No study session" else "${mins / 60}h ${mins % 60}m studied",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onBackground, fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { selectedIdx = -1 },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("✕", color = cs.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            // Day-of-week header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                dayNames.forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                        fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            // 4 rows × 7 days — large cells, clear colors
            repeat(4) { row ->
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    repeat(7) { col ->
                        val dayIdx    = row * 7 + col
                        val mins      = days.getOrElse(dayIdx) { 0 }
                        val isSelected = selectedIdx == dayIdx
                        val isToday   = dayIdx == totalDays - 1
                        val cellColor = when {
                            isSelected -> BpscColors.Accent
                            mins == 0  -> if (isDark) cs.surfaceVariant else Color(0xFFF0F4FF)
                            else       -> {
                                val ratio = (mins.toFloat() / maxMins).coerceIn(0f, 1f)
                                when {
                                    ratio > 0.7f -> Color(0xFF0D47A1)
                                    ratio > 0.4f -> Color(0xFF1565C0)
                                    else         -> Color(0xFF90CAF9)
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cellColor)
                            .border(
                                if (isToday && !isSelected) 2.dp else if (isSelected) 0.dp else 0.dp,
                                if (isToday) BpscColors.Accent else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedIdx = if (isSelected) -1 else dayIdx }
                        ) {
                            if (mins > 0 && !isSelected) {
                                // Small dot to indicate study — visible at a glance
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.5f))
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp))
                            }
                        }
                    }
                }
            }

            // Summary + legend row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                val activeDays = days.count { it > 0 }
                Text(
                    "$activeDays / 28 days active",
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant, fontSize = 9.sp)
                    Spacer(Modifier.width(3.dp))
                    listOf(Color(0xFFF0F4FF), Color(0xFF90CAF9), Color(0xFF1565C0), Color(0xFF0D47A1)).forEach { col ->
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(col))
                        Spacer(Modifier.width(2.dp))
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        }
    }
}

// ── SUBJECT PROGRESS ───────────────────────────────────────────
@Composable
private fun SubjectProgressCard(
    subjects: List<SubjectProgress>,
    navController: NavHostController,
    isDark: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val str      = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val visible  = if (expanded) subjects else subjects.take(3)

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(str.profileSubjectProgress, style = MaterialTheme.typography.titleMedium,
                    color = cs.onBackground, fontWeight = FontWeight.Bold)
                TextButton(onClick = { navController.navigate(Screen.MyLearning.route) }) {
                    Text(str.seeAll, color = BpscColors.Primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            visible.forEachIndexed { i, subj ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                val animProg by animateFloatAsState(subj.progress, tween(900), label = "sp$i")
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(subj.bgColor),
                        contentAlignment = Alignment.Center) {
                        Text(subj.emoji, fontSize = 15.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(subj.name, style = MaterialTheme.typography.bodyMedium,
                                color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                            Text("${(subj.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = subj.color, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = { animProg },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = subj.color, trackColor = subj.bgColor)
                    }
                }
            }
            if (subjects.size > 3) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .background(cs.surfaceVariant).padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(
                        if (expanded) str.profileShowLess else "+ ${subjects.size - 3} more",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.Primary, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── BADGES CARD — tap badge to see details ─────────────────────
@Composable
private fun BadgesCard(badges: List<BadgeItem>, isDark: Boolean) {
    val cs = MaterialTheme.colorScheme
    val str             = LocalStrings.current
    var selectedBadge   by remember { mutableStateOf<BadgeItem?>(null) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(str.profileBadges, style = MaterialTheme.typography.titleMedium,
                    color = cs.onBackground, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Star, null, tint = BpscColors.Primary, modifier = Modifier.size(12.dp))
                    Text("${badges.count { it.earned }} earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(badges) { badge ->
                    Column(modifier = Modifier.width(62.dp).alpha(if (badge.earned) 1f else 0.35f)
                        .clickable { selectedBadge = badge },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (badge.earned) badge.bgColor else cs.surfaceVariant)
                            .border(if (badge.earned) 1.dp else 0.dp,
                                if (badge.earned) badge.bgColor.copy(0.5f) else Color.Transparent,
                                RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(badge.emoji, fontSize = 22.sp)
                        }
                        Text(badge.name, style = MaterialTheme.typography.labelSmall,
                            color = if (badge.earned) cs.onBackground else cs.onSurfaceVariant,
                            textAlign = TextAlign.Center, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, lineHeight = 11.sp, fontSize = 9.sp)
                    }
                }
            }
        }
    }

    // Badge detail dialog
    selectedBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = cs.surface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (badge.earned) badge.bgColor else cs.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Text(badge.emoji, fontSize = 36.sp)
                    }
                    Text(badge.name, fontWeight = FontWeight.ExtraBold, color = cs.onBackground,
                        textAlign = TextAlign.Center)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (badge.earned) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2E7D32).copy(0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("✅", fontSize = 14.sp)
                            Column {
                                Text(str.profileBadgeEarned, style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                if (!badge.earnedDate.isNullOrBlank())
                                    Text(badge.earnedDate, style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant)
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.Primary.copy(0.07f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 14.sp)
                            Text(str.profileNotEarned, style = MaterialTheme.typography.labelMedium,
                                color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (badge.description.isNotBlank())
                        Text(badge.description, style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text(str.closeLabel, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ── COIN WALLET SECTION ────────────────────────────────────────
@Composable
private fun CoinWalletSection(
    balance: Int,
    transactions: List<com.example.bpscnotes.data.remote.api.CoinTransactionDto>,
    onViewAll: () -> Unit
) {
    val str = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Box(Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))),
            RoundedCornerShape(20.dp))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 16.sp)
                        Text(str.profileCoinWallet, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = onViewAll) {
                        Text(str.profileViewAll, color = BpscColors.CoinGold,
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                    Column {
                        Text(str.profileCurrentBalance, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 22.sp)
                            Text("$balance coins", style = MaterialTheme.typography.headlineMedium,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("≈ ₹${balance / 10} discount value",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(str.profileHowToEarn, style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                        Text(str.profileEarnQuiz, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                        Text(str.profileEarnStreak, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
                transactions.take(3).forEach { tx ->
                    val earned = tx.type == "earned"
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.08f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (earned) "🎯" else "🛍️", fontSize = 14.sp)
                            Text(tx.title.ifBlank { tx.action.ifBlank { if (earned) "Earned" else "Spent" } },
                                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp))
                        }
                        Text("${if (earned) "+" else "-"}${tx.coins} 🪙",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (earned) Color(0xFF4CAF50) else Color(0xFFE57373),
                            fontWeight = FontWeight.Bold)
                    }
                }
                if (transactions.isEmpty())
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.07f)).padding(12.dp), Alignment.Center) {
                        Text(str.profileNoTransactions, style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
                    }
            }
        }
    }
}