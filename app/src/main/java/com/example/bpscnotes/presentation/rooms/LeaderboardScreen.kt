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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.LeaderboardEntryDto

@Composable
fun LeaderboardScreen(
    navController: NavHostController,
    viewModel: TierRoomsViewModel
) {
    val cs  = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state by viewModel.uiState.collectAsState()

    var selectedPeriod by remember { mutableStateOf("weekly") }
    val periods = listOf("weekly" to "📅 Weekly", "monthly" to "📆 Monthly", "alltime" to "🏆 All Time")

    // Single effect — fires on open AND on period tab switch
    LaunchedEffect(selectedPeriod) {
        com.example.bpscnotes.core.analytics.Event.screenView("leaderboard")
        // Use user's actual tier; "starter" is just the pre-load fallback
        val tierKey = state.myTierData?.currentTier?.tierKey ?: "starter"
        viewModel.loadLeaderboard(tierKey, selectedPeriod)
    }

    val leaderboard = state.leaderboard
    val myUserId    = state.myUserId

    Column(Modifier.fillMaxSize().background(cs.background)) {

        // ── Header ──────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(500f, 300f)
                    )
                )
        ) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.05f), 140.dp.toPx(), Offset(size.width + 10f, -40f))
            }
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStack() },
                        Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("🏆 Leaderboard", style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Top studiers in your tier", style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.65f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Period tabs
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.1f)).padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    periods.forEach { (key, label) ->
                        val sel = selectedPeriod == key
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (sel) Color.White else Color.Transparent)
                                .clickable { selectedPeriod = key }
                                .padding(vertical = 8.dp),
                            Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                color = if (sel) BpscColors.Primary else Color.White.copy(0.8f),
                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // ── Content ─────────────────────────────────────────────
        when {
            state.isLoadingLeaderboard -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            }
            state.leaderboardError != null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("😕", fontSize = 48.sp)
                        Text("Could not load leaderboard", style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text(state.leaderboardError ?: "", style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant)
                        Button(
                            onClick = { viewModel.loadLeaderboard(state.myTierData?.currentTier?.tierKey ?: "starter", selectedPeriod) },
                            colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Retry") }
                    }
                }
            }
            leaderboard.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📊", fontSize = 48.sp)
                        Text("No data yet", style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text("Study sessions will appear here", style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    // Top 3 podium
                    if (leaderboard.size >= 3) {
                        item { Podium(leaderboard.take(3), myUserId) }
                    }

                    // Rest of the list
                    val rest = if (leaderboard.size >= 3) leaderboard.drop(3) else leaderboard
                    itemsIndexed(rest) { index, entry ->
                        LeaderboardRow(
                            entry    = entry,
                            isMe     = entry.userId == myUserId,
                            position = if (leaderboard.size >= 3) index + 4 else index + 1
                        )
                        if (index < rest.lastIndex) HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp), color = cs.outline.copy(0.4f)
                        )
                    }
                }
            }
        }
    }
}

// ── Podium (top 3) ────────────────────────────────────────────
@Composable
private fun Podium(top3: List<LeaderboardEntryDto>, myUserId: String) {
    val cs = MaterialTheme.colorScheme
    // Reorder: 2nd, 1st, 3rd
    val ordered = listOf(top3.getOrNull(1), top3.getOrNull(0), top3.getOrNull(2))
    val heights = listOf(100.dp, 130.dp, 80.dp)
    val medals  = listOf("🥈", "🥇", "🥉")
    val colors  = listOf(Color(0xFFB0BEC5), Color(0xFFFFD700), Color(0xFFCD7F32))

    Box(
        Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1565C0).copy(0.08f), cs.background))
            )
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.Bottom) {
            ordered.forEachIndexed { i, entry ->
                val isMe = entry?.userId == myUserId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(100.dp)
                ) {
                    // Avatar circle
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(
                                if (isMe) BpscColors.Primary.copy(0.15f)
                                else cs.surface
                            )
                            .border(2.dp, colors[i], CircleShape),
                        Alignment.Center
                    ) {
                        Text(
                            entry?.userName?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors[i],
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(medals[i], fontSize = 18.sp)
                    Text(
                        entry?.userName ?: "-",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMe) BpscColors.Primary else cs.onSurface,
                        fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Podium block
                    Box(
                        Modifier.fillMaxWidth().height(heights[i])
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(listOf(colors[i].copy(0.7f), colors[i].copy(0.3f)))
                            ),
                        Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${entry?.studyMinutes ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("min", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ── Single row (rank 4+) ──────────────────────────────────────
@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto, isMe: Boolean, position: Int) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isMe) BpscColors.Primary.copy(0.06f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (isMe) BpscColors.Primary else cs.surface)
                .border(1.dp, if (isMe) BpscColors.Primary else cs.outline, CircleShape),
            Alignment.Center
        ) {
            Text(
                "#$position",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMe) Color.White else cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
        }

        // Avatar
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (isMe) BpscColors.Primary.copy(0.15f) else cs.surface)
                .border(1.dp, if (isMe) BpscColors.Primary else cs.outline, CircleShape),
            Alignment.Center
        ) {
            Text(
                entry.userName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isMe) BpscColors.Primary else cs.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Name + stats
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isMe) BpscColors.Primary else cs.onSurface,
                    fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (isMe) Text(
                    "YOU",
                    style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.Primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(BpscColors.Primary.copy(0.12f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⏱ ${entry.studyMinutes}m", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text("🪙 ${entry.coinsEarned}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                if (entry.streakDays > 0) Text("🔥 ${entry.streakDays}d", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6F00))
            }
        }

        // XP level badge
        Box(
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(BpscColors.Primary.copy(0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Lv.${entry.xpLevel}", style = MaterialTheme.typography.labelSmall,
                color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}