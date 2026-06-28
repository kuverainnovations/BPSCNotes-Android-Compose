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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.LeaderboardEntryDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ────────────────────────────────────────────────────────────────
// Tab definitions for MED-17
// ────────────────────────────────────────────────────────────────
private data class LeaderboardTab(val type: String, val label: String, val emoji: String)

private val TABS = listOf(
    LeaderboardTab("coins",         "All-time",  "🏆"),
    LeaderboardTab("weekly_coins",  "This Week", "📅"),
    LeaderboardTab("quiz_accuracy", "Accuracy",  "🎯"),
    LeaderboardTab("streak",        "Streak",    "🔥"),
)

@Composable
fun GlobalLeaderboardScreen(
    navController: NavHostController,
    vm: GlobalLeaderboardViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        // Header
        GlobalLeaderboardHeader(
            onBack    = { navController.popBackStackSafe() },
            onRefresh = { vm.load() },
        )

        // Type tab strip (MED-17)
        TypeTabStrip(selected = state.selectedType, onSelect = { vm.selectType(it) })

        // Body
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> AppLoader()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.data != null  -> LeaderboardBody(
                    data = state.data!!,
                    type = state.selectedType,
                )
            }
        }
    }
}

@Composable
private fun GlobalLeaderboardHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(
                listOf(Color(0xFF051D56), Color(0xFF1565C0)),
                start = Offset(0f, 0f), end = Offset(Float.MAX_VALUE, 0f),
            )
        )
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.04f), 180.dp.toPx(), Offset(size.width + 20f, -40f))
        }
        Column(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.12f)).clickable { onBack() },
                    Alignment.Center,
                ) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.12f)).clickable { onRefresh() },
                    Alignment.Center,
                ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text("🏆 Leaderboard", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("See how you rank against all users", fontSize = 13.sp, color = Color.White.copy(0.75f))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TypeTabStrip(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TABS, key = { it.type }) { tab ->
            val sel = selected == tab.type
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) BpscColors.brand else Color(0xFFEEEEF5))
                    .clickable { onSelect(tab.type) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(tab.emoji, fontSize = 14.sp)
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    color = if (sel) Color.White else Color(0xFF555577),
                )
            }
        }
    }
}

@Composable
private fun LeaderboardBody(data: com.example.bpscnotes.data.remote.api.LeaderboardData, type: String) {
    val top3    = data.leaderboard.take(3)
    val rest    = data.leaderboard.drop(3)
    val myRank  = data.myRank

    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Podium top-3 (LOW-08)
        if (top3.size == 3) {
            item(key = "podium") { PodiumRow(entries = top3, type = type) }
        } else {
            items(top3, key = { "top_${it.userId}" }) { entry ->
                LeaderboardRow(entry = entry, type = type, isMe = myRank?.userId == entry.userId)
            }
        }

        // Divider
        item(key = "divider") {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFE0E0EE))
        }

        // Ranks 4+
        itemsIndexed(rest, key = { _, e -> "r_${e.userId}" }) { _, entry ->
            LeaderboardRow(entry = entry, type = type, isMe = myRank?.userId == entry.userId)
        }

        // Pinned "my rank" footer (LOW-08) — shown when user is outside top list
        if (myRank != null && data.leaderboard.none { it.userId == myRank.userId }) {
            item(key = "myRankSticky") { MyRankStickyRow(entry = myRank, type = type) }
        }
    }
}

// ── Podium for top 3 (LOW-08) ───────────────────────────────────
@Composable
private fun PodiumRow(entries: List<LeaderboardEntryDto>, type: String) {
    val gold   = entries[0]
    val silver = entries[1]
    val bronze = entries[2]
    Box(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            // 2nd place
            PodiumColumn(entry = silver, place = 2, height = 80.dp, type = type)
            // 1st place — tallest
            PodiumColumn(entry = gold,   place = 1, height = 110.dp, type = type)
            // 3rd place
            PodiumColumn(entry = bronze, place = 3, height = 60.dp, type = type)
        }
    }
}

@Composable
private fun PodiumColumn(entry: LeaderboardEntryDto, place: Int, height: Dp, type: String) {
    val podiumColor = when (place) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    val medal = when (place) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Crown for #1
        if (place == 1) Text("👑", fontSize = 18.sp, modifier = Modifier.padding(bottom = 2.dp))
        // Avatar
        Box(
            Modifier.size(if (place == 1) 52.dp else 44.dp).clip(CircleShape).background(podiumColor.copy(0.2f)),
            Alignment.Center,
        ) {
            if (!entry.avatarUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = entry.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    entry.name.take(1).uppercase(),
                    fontSize = if (place == 1) 22.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = podiumColor,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            entry.name.split(" ").firstOrNull() ?: entry.name,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 70.dp), textAlign = TextAlign.Center,
        )
        Text(entryValue(entry, type), fontSize = 10.sp, color = Color(0xFF888899))
        Spacer(Modifier.height(4.dp))
        // Podium block
        Box(
            Modifier.width(72.dp).height(height).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(podiumColor.copy(0.85f)),
            Alignment.Center,
        ) { Text(medal, fontSize = 20.sp) }
    }
}

// ── Standard rank row ───────────────────────────────────────────
@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto, type: String, isMe: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (isMe) Modifier.background(BpscColors.brand.copy(0.06f))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rank + delta (LOW-08)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Text(
                "#${entry.rank}",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isMe) BpscColors.brand else Color(0xFF333355),
            )
            entry.prevRank?.let { prev ->
                val delta = prev - entry.rank
                if (delta != 0) {
                    val (arrow, color) = if (delta > 0)
                        ("▲$delta" to Color(0xFF2ECC71))
                    else
                        ("▼${-delta}" to Color(0xFFE74C3C))
                    Text(arrow, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Avatar
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(BpscColors.brand.copy(0.12f)),
            Alignment.Center,
        ) {
            if (!entry.avatarUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = entry.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(entry.name.take(1).uppercase(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand)
            }
        }

        // Name
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.name, fontSize = 14.sp,
                    fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isMe) BpscColors.brand else Color(0xFF1A1A2E),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isMe) {
                    Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.brand.copy(0.1f)).padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
        }

        // Value
        Text(entryValue(entry, type), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555566))
    }
}

// ── Pinned current-user row at bottom (LOW-08) ─────────────────
@Composable
private fun MyRankStickyRow(entry: LeaderboardEntryDto, type: String) {
    Box(
        Modifier.fillMaxWidth()
            .background(BpscColors.brand.copy(0.1f))
            .border(BorderStroke(1.dp, BpscColors.brand.copy(0.3f))),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (entry.rank > 0) "#${entry.rank}" else "—",
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand,
                modifier = Modifier.width(36.dp), textAlign = TextAlign.Center,
            )
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(BpscColors.brand.copy(0.2f)),
                Alignment.Center,
            ) {
                Text(entry.name.take(1).uppercase(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand)
            }
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BpscColors.brand,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.brand.copy(0.15f)).padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
            Text(entryValue(entry, type), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BpscColors.brand)
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────

private fun entryValue(e: LeaderboardEntryDto, type: String): String = when (type) {
    "weekly_coins"  -> "${e.weeklyCoins ?: 0} coins"
    "quiz_accuracy" -> "${String.format("%.1f", e.accuracy ?: 0.0)}%"
    "streak"        -> "${e.streak ?: 0}🔥"
    else            -> "${e.coins ?: 0} coins"
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("😕", fontSize = 48.sp)
            Text(message, textAlign = TextAlign.Center, color = Color(0xFF888899))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.brand)) {
                Text("Retry")
            }
        }
    }
}
