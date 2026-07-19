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
import com.example.bpscnotes.data.remote.api.TierMemberDto
import com.example.bpscnotes.data.remote.api.MyRankResponseData
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ════════════════════════════════════════════════════════════
// LEADERBOARD — redesigned per the room redesign brief, section 3-5:
// shows ALL room members (not just top 3) with status tabs
// (Studying / Online / Offline / All), a personal ranking section,
// and Room Champions. Primary data source is now state.members (the
// same rich roster StudyFocusScreen's Active/Inactive tabs use) rather
// than the old period-snapshot state.leaderboard, so rank/status/badges
// are consistent everywhere they're shown.
// ════════════════════════════════════════════════════════════
@Composable
fun LeaderboardScreen(
    navController: NavHostController,
    viewModel: TierRoomsViewModel
) {
    val cs    = MaterialTheme.colorScheme
    val str   = LocalStrings.current
    val state by viewModel.uiState.collectAsState()

    val tierKey  = state.myTierData?.currentTier?.tierKey ?: state.selectedTierKey
    val tierName = state.myTierData?.currentTier?.name
        ?: tierKey.replaceFirstChar { it.uppercase() }

    // "studying" | "online" | "offline" | "all"
    var selectedTab by remember { mutableStateOf("all") }

    LaunchedEffect(tierKey) {
        viewModel.loadMembers(tierKey)
        viewModel.loadChampions(tierKey)
        viewModel.loadMyRank(tierKey)
    }

    fun refresh() {
        viewModel.loadMembers(tierKey)
        viewModel.loadChampions(tierKey)
        viewModel.loadMyRank(tierKey)
    }

    val myUserId    = state.myUserId
    val allMembers  = state.members
    val filteredMembers = remember(allMembers, selectedTab) {
        val base = when (selectedTab) {
            "studying" -> allMembers.filter { it.status == "studying" }
            "online"   -> allMembers.filter { it.status == "online" }
            "offline"  -> allMembers.filter { it.status == "offline" }
            else       -> allMembers
        }
        base.sortedBy { it.rankPosition }
    }

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
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.04f), 200.dp.toPx(), Offset(size.width + 40f, -60f))
                drawCircle(Color.White.copy(0.03f), 120.dp.toPx(), Offset(size.width - 60f, 30f))
            }

            Column(
                Modifier.statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
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
                        Text(str.leaderboardTitleEmoji, style = MaterialTheme.typography.titleLarge,
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
                            .clickable { refresh() },
                        Alignment.Center
                    ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
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
                state.isLoadingMembers && allMembers.isEmpty() -> AppLoader()
                state.membersError != null && allMembers.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("😕", fontSize = 48.sp)
                            Text(str.lbCouldNotLoad, style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold)
                            Text(state.membersError ?: "", style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                            Button(
                                onClick = { refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(str.retry) }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── Personal ranking (redesign section 4) ──────
                        item(key = "myRank") {
                            PersonalRankCard(state.myRank)
                        }

                        // ── Room Champions (redesign section 5) ────────
                        if (state.champions != null) {
                            item(key = "champions") {
                                RoomChampionsCard(state.champions)
                            }
                        }

                        // ── Status tabs (redesign section 3) ───────────
                        item(key = "tabs") {
                            LeaderboardTabs(
                                selected = selectedTab,
                                studyingCount = allMembers.count { it.status == "studying" },
                                onlineCount   = allMembers.count { it.status == "online" },
                                offlineCount  = allMembers.count { it.status == "offline" },
                                allCount      = allMembers.size,
                                onSelect = { selectedTab = it }
                            )
                        }

                        if (filteredMembers.isEmpty()) {
                            item(key = "empty") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("📊", fontSize = 40.sp)
                                        Text(
                                            when (selectedTab) {
                                                "studying" -> "No one is studying right now"
                                                "online"   -> "No one else is online"
                                                "offline"  -> "Everyone's been active recently"
                                                else       -> "No members yet"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = cs.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(filteredMembers, key = { _, m -> m.id }) { _, member ->
                                MemberLeaderboardRow(member = member, isMe = member.id == myUserId)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Personal ranking card (redesign section 4) ─────────────────
@Composable
private fun PersonalRankCard(myRank: MyRankResponseData?) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = BpscColors.Primary.copy(0.08f)),
        border    = BorderStroke(1.dp, BpscColors.Primary.copy(0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(str.lbYourRanking, style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                val rankText = myRank?.rankPosition?.let { rankLabel(it) } ?: "—"
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BpscColors.Primary.copy(0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(rankText, style = MaterialTheme.typography.labelMedium,
                        color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (myRank == null) {
                Text(str.lbJoinToSee, style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PersonalStatPill("Today", minutesLabel(myRank.todayMinutes))
                    PersonalStatPill("Week", minutesLabel(myRank.weekMinutes))
                    PersonalStatPill("Month", minutesLabel(myRank.monthMinutes))
                    PersonalStatPill("Streak", "🔥${myRank.streak}d")
                }
                if (myRank.minutesToNextRank != null && myRank.nextRankPosition != null) {
                    HorizontalDivider(color = cs.onSurface.copy(0.08f), thickness = 0.5.dp)
                    Text(
                        "You need ${minutesLabel(myRank.minutesToNextRank)} more to reach Rank #${myRank.nextRankPosition}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BpscColors.CoinGold,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (myRank.rankPosition == 1) {
                    Text(str.lbYou1, style = MaterialTheme.typography.labelMedium,
                        color = BpscColors.CoinGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PersonalStatPill(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp)
    }
}

private fun rankLabel(rank: Int): String = when (rank) {
    1 -> "🥇 #1"
    2 -> "🥈 #2"
    3 -> "🥉 #3"
    else -> "#$rank"
}

private fun minutesLabel(mins: Int): String =
    if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"

// ── Status tabs (redesign section 3) ────────────────────────────
@Composable
private fun LeaderboardTabs(
    selected: String,
    studyingCount: Int,
    onlineCount: Int,
    offlineCount: Int,
    allCount: Int,
    onSelect: (String) -> Unit
) {
    val tabs = listOf(
        Triple("studying", "🟢 Studying", studyingCount),
        Triple("online",   "🟡 Online",   onlineCount),
        Triple("offline",  "⚫ Offline",  offlineCount),
        Triple("all",      "All",         allCount),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs, key = { it.first }) { (key, label, count) ->
            val sel = selected == key
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) BpscColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Medium)
                Text("$count", style = MaterialTheme.typography.labelSmall,
                    color = if (sel) Color.White.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
            }
        }
    }
}

// ── Member row (redesign section 3) — rank, photo, name, status,
//    today/week/month time, current+longest streak, last active, badges
@Composable
private fun MemberLeaderboardRow(member: TierMemberDto, isMe: Boolean) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isMe) Modifier.background(BpscColors.Primary.copy(0.08f))
                    .border(1.dp, BpscColors.Primary.copy(0.25f), RoundedCornerShape(14.dp))
                else Modifier.background(cs.surfaceVariant.copy(0.25f))
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rank
        Text(
            if (member.rankPosition in 1..3) rankLabel(member.rankPosition) else "#${member.rankPosition}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isMe) BpscColors.Primary else cs.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(40.dp).padding(top = 6.dp),
            textAlign = TextAlign.Center
        )

        // Avatar + status dot
        Box {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isMe) BpscColors.Primary else BpscColors.PrimaryLight),
                Alignment.Center
            ) {
                if (!member.avatarUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        member.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isMe) Color.White else BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            val statusColor = when (member.status) {
                "studying" -> BpscColors.Success
                "online"   -> Color(0xFFFFA726)
                else       -> Color.Gray
            }
            Box(
                Modifier
                    .size(11.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }

        // Name + stats
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) BpscColors.Primary else cs.onSurface,
                    fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isMe) Text(str.focusYou,
                    style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(BpscColors.Primary.copy(0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp))
                if (member.badgeCount > 0) {
                    Text("🎖${member.badgeCount}",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                }
            }

            // Today / Week / Month — all three shown together (redesign
            // section 3), not gated behind a period selector.
            Text(
                "📍 ${minutesLabel(member.todayMinutes)} today · ${minutesLabel(member.weekMinutes)} wk · ${minutesLabel(member.monthMinutes)} mo",
                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 11.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (member.streak > 0) {
                    Text("🔥 ${member.streak}d", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6F00))
                }
                if (member.longestStreak > member.streak) {
                    Text("best ${member.longestStreak}d", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(0.6f))
                }
                val lastActiveLabel = when (member.status) {
                    "studying" -> "Studying now"
                    else -> member.lastActiveAt?.let { relativeTimeAgo(it) } ?: "Not active yet"
                }
                Text(lastActiveLabel, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(0.6f))
            }
        }
    }
}
