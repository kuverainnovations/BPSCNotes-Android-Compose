package com.example.bpscnotes.presentation.rooms

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.LeaderboardEntryDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@Composable
fun LeaderboardScreen(
    navController: NavHostController,
    viewModel: TierRoomsViewModel
) {
    val cs    = MaterialTheme.colorScheme
    val str   = LocalStrings.current
    val state by viewModel.uiState.collectAsState()

    var selectedPeriod by remember { mutableStateOf("weekly") }
    val periods = listOf("weekly" to "Weekly", "monthly" to "Monthly", "alltime" to "All time")

    val tierKey = state.myTierData?.currentTier?.tierKey ?: "silver"
    val tierName = state.myTierData?.currentTier?.name
        ?: tierKey.replaceFirstChar { it.uppercase() }

    LaunchedEffect(selectedPeriod, tierKey) {
        viewModel.loadLeaderboard(tierKey, selectedPeriod)
    }

    val leaderboard = state.leaderboard
    val myUserId    = state.myUserId

    Column(Modifier.fillMaxSize().background(cs.background)) {

        // ── Gradient header ──────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(500f, 350f)
                    )
                )
        ) {
            // Decorative circles
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.04f), 200.dp.toPx(), Offset(size.width + 40f, -60f))
                drawCircle(Color.White.copy(0.03f), 120.dp.toPx(), Offset(size.width - 60f, 30f))
            }

            Column(
                Modifier.statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)
            ) {
                // Top row: back + title + tier badge + refresh
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .clickable { navController.popBackStackSafe() },
                        Alignment.Center
                    ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }

                    Column(Modifier.weight(1f)) {
                        Text("🏆 Leaderboard", style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(tierName, style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.85f))
                        }
                    }

                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .clickable { viewModel.loadLeaderboard(tierKey, selectedPeriod) },
                        Alignment.Center
                    ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }

                Spacer(Modifier.height(16.dp))

                // Period tabs
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.1f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    periods.forEach { (key, label) ->
                        val sel = selectedPeriod == key
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Color.White else Color.Transparent)
                                .clickable { selectedPeriod = key }
                                .padding(vertical = 8.dp),
                            Alignment.Center
                        ) {
                            Text(label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) BpscColors.Primary else Color.White.copy(0.75f),
                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal)
                        }
                    }
                }

                // Podium (top 3) — shown inside header gradient.
                // FIX: only render when there are 3+ real entries. For 1-2
                // users this previously rendered 1-2 "ghost" placeholder
                // slots ("-" name, "?" avatar, 0m) AND duplicated those
                // same users again in the RANKINGS list below — looked
                // like empty/duplicated blocks on small (staging) datasets.
                if (!state.isLoadingLeaderboard && leaderboard.size >= 3) {
                    Spacer(Modifier.height(20.dp))
                    Podium(leaderboard.take(3), myUserId)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Content area ─────────────────────────────────────────
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(cs.background)
        ) {
            when {
                state.isLoadingLeaderboard -> {
                    AppLoader()
                }
                state.leaderboardError != null -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("😕", fontSize = 48.sp)
                            Text("Could not load", style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold)
                            Text(state.leaderboardError ?: "", style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.loadLeaderboard(tierKey, selectedPeriod) },
                                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Retry") }
                        }
                    }
                }
                leaderboard.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📊", fontSize = 48.sp)
                            Text("No data yet", style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold)
                            Text("Study sessions will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    val rest = if (leaderboard.size >= 3) leaderboard.drop(3) else leaderboard
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (rest.isNotEmpty()) {
                            item {
                                Text(
                                    "RANKINGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            itemsIndexed(rest) { index, entry ->
                                val isMe = entry.userId == myUserId
                                val position = if (leaderboard.size >= 3) index + 4 else index + 1
                                LeaderboardRow(entry = entry, isMe = isMe, position = position)
                                if (index < rest.lastIndex && !isMe) {
                                    HorizontalDivider(
                                        Modifier.padding(start = 72.dp),
                                        color = cs.outline.copy(0.3f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Podium (top 3) ────────────────────────────────────────────
@Composable
private fun Podium(top3: List<LeaderboardEntryDto>, myUserId: String) {
    // Order: 2nd, 1st, 3rd
    val ordered  = listOf(top3.getOrNull(1), top3.getOrNull(0), top3.getOrNull(2))
    val heights  = listOf(70.dp, 92.dp, 55.dp)
    val medals   = listOf("🥈", "🥇", "🥉")
    val colors   = listOf(Color(0xFFC0C0C0), Color(0xFFFFD700), Color(0xFFCD7F32))
    val alphas   = listOf(0.18f, 0.22f, 0.15f)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEachIndexed { i, entry ->
            val isMe = entry?.userId == myUserId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(100.dp)
            ) {
                // Avatar
                Box(
                    Modifier.size(if (i == 1) 56.dp else 48.dp)
                        .clip(CircleShape)
                        .background(colors[i].copy(alphas[i]))
                        .border(2.dp, colors[i], CircleShape),
                    Alignment.Center
                ) {
                    Text(
                        entry?.userName?.take(1)?.uppercase() ?: "?",
                        style = if (i == 1) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.titleMedium,
                        color = colors[i],
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(medals[i], fontSize = if (i == 1) 20.sp else 16.sp)
                Text(
                    entry?.userName ?: "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMe) Color(0xFFFFD700) else Color.White.copy(0.9f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val mins = entry?.studyMinutes ?: 0
                val timeStr = if (mins >= 60) "${mins/60}h ${mins%60}m" else "${mins}m"
                Text(
                    "⏱ $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.7f)
                )
                // Podium block
                Box(
                    Modifier.fillMaxWidth().height(heights[i])
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(colors[i].copy(alphas[i])),
                    Alignment.Center
                ) {
                    // positions: left=2nd(i=0), center=1st(i=1), right=3rd(i=2)
                    val pos = listOf(2, 1, 3)[i]
                    val suffix = listOf("nd","st","rd")[i]
                    Text(
                        "$pos$suffix",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors[i],
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ── Rank row (4th+) ───────────────────────────────────────────
@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntryDto,
    isMe: Boolean,
    position: Int
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isMe) Modifier.background(BpscColors.Primary.copy(0.10f))
                    .border(1.dp, BpscColors.Primary.copy(0.3f), RoundedCornerShape(12.dp))
                else Modifier.background(cs.surface)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rank badge
        Text(
            "#$position",
            style = MaterialTheme.typography.labelMedium,
            color = if (isMe) BpscColors.Primary else cs.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        // Avatar
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (isMe) BpscColors.Primary else BpscColors.PrimaryLight),
            Alignment.Center
        ) {
            Text(
                entry.userName.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isMe) Color.White else BpscColors.Primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Name + stats
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) BpscColors.Primary else cs.onSurface,
                    fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (isMe) Text("YOU",
                    style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(BpscColors.Primary.copy(0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp))
            }
            // Only show non-zero stats to avoid clutter
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val mins = entry.studyMinutes
                val timeStr = if (mins >= 60) "${mins/60}h ${mins%60}m" else "${mins}m"
                Text("⏱ $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mins > 0) cs.onSurfaceVariant else cs.onSurfaceVariant.copy(0.4f))
                if (entry.coinsEarned > 0)
                    Text("🪙 ${entry.coinsEarned}",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF8F00))
                if (entry.streakDays > 0)
                    Text("🔥 ${entry.streakDays}d",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6F00))
            }
        }
    }
}