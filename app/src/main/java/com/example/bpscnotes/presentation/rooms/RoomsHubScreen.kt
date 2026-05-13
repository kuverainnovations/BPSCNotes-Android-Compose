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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/RoomsHubScreen.kt
//
// Entry screen for the Group Study feature.
// Shows:
//   - User's current tier card (with progress bar to next tier)
//   - All 4 tier rooms as a horizontal map
//   - [Enter Room] button → StudyFocusScreen
//   - Leaderboard tab
//   - Members tab
// ════════════════════════════════════════════════════════════

@Composable
fun RoomsHubScreen(
    navController: NavHostController,
    tiersViewModel: TierRoomsViewModel    = hiltViewModel(),
    sessionViewModel: StudySessionViewModel = hiltViewModel()
) {
    val state         by tiersViewModel.uiState.collectAsState()
    val sessionState  by sessionViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check for existing session on resume (app killed and restarted)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sessionViewModel.checkForExistingSession()
            tiersViewModel.loadMyTier()
        }
    }

    // If an active session exists → go straight to focus screen
    LaunchedEffect(sessionState.status) {
        if (sessionState.status == SessionStatus.ACTIVE || sessionState.status == SessionStatus.AFK) {
            navController.navigate(Screen.StudyFocus.route) {
                launchSingleTop = true
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs         = listOf("Leaderboard", "Members")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            RoomsHubHeader(
                myTierData    = state.myTierData,
                isLoading     = state.isLoadingMyTier,
                onBack        = { navController.popBackStack() }
            )

            // ── Tier Map ──────────────────────────────────────
            if (state.isLoadingTiers) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(28.dp))
                }
            } else {
                TierMapRow(
                    tiers             = state.allTiers,
                    userTierKey       = state.myTierData?.currentTier?.tierKey,
                    selectedTierKey   = state.selectedTierKey,
                    onSelectTier      = { tiersViewModel.selectTier(it) }
                )
            }

            // ── Enter Room Button ─────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = {
                        val tierKey = state.selectedTierKey ?: state.myTierData?.currentTier?.tierKey
                        sessionViewModel.startSession(roomId = null, mode = "study")
                        navController.navigate(Screen.StudyFocus.route)
                    },
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    enabled   = state.myTierData != null && sessionState.status == SessionStatus.IDLE
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = when (sessionState.status) {
                            SessionStatus.STARTING -> "Starting…"
                            else                   -> "Start Studying in ${state.selectedTierKey?.replaceFirstChar { it.uppercase() } ?: "Silver"} Room"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ── Tabs: Leaderboard | Members ───────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.White,
                contentColor     = BpscColors.Primary,
               // dividerColor     = BpscColors.Divider,
            ) {
                tabs.forEachIndexed { i, tab ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(tab, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // ── Tab Content ───────────────────────────────────
            when (selectedTab) {
                0 -> LeaderboardTab(
                    entries     = state.leaderboard,
                    isLoading   = state.isLoadingLeaderboard,
                    myUserId    = "",   // from TokenStore — pass if needed for highlight
                    period      = state.leaderboardPeriod,
                    onChangePeriod = { p ->
                        val key = state.selectedTierKey ?: return@LeaderboardTab
                        tiersViewModel.loadLeaderboard(key, p)
                    }
                )
                1 -> MembersTab(
                    members   = state.members,
                    isLoading = state.isLoadingMembers
                )
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────

@Composable
private fun RoomsHubHeader(
    myTierData: MyTierResponseData?,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                Offset(0f, 0f), Offset(400f, 300f)
            ))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier         = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f)).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Group Study", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Study together, grow faster", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // My Tier Progress Card
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(0.1f)))
            } else if (myTierData != null) {
                MyTierProgressCard(myTierData)
            }
        }
    }
}

@Composable
private fun MyTierProgressCard(data: MyTierResponseData) {
    val tier     = data.currentTier
    val next     = data.nextTier
    val progress = data.nextTierProgress.toFloat()
    val animProg by animateFloatAsState(progress, tween(800), label = "tier_prog")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tier.iconEmoji ?: "🏆", fontSize = 22.sp)
                    Column {
                        Text(tier.name?: "Unknown Tier", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${tier.totalMembers} members · ${tier.activeSessions} studying now",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
                if (next != null) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(next.iconEmoji ?: "🏆", fontSize = 12.sp)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (next != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                    }
                    Text("Progress to ${next.name}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f), fontSize = 10.sp)
                }
            } else {
                Text("🏆 Maximum tier reached — Diamond Elite", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700))
            }
        }
    }
}

// ── Tier Map Row ───────────────────────────────────────────────

@Composable
private fun TierMapRow(
    tiers: List<RoomTierDto>,
    userTierKey: String?,
    selectedTierKey: String?,
    onSelectTier: (String) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tiers, key = { it.id }) { tier ->
            val isUser = tier.tierKey != null && tier.tierKey == userTierKey
            val isSelected = tier.tierKey != null && tier.tierKey == selectedTierKey
            val tierColor  = Color(android.graphics.Color.parseColor(tier.colorHex))

            Card(
                modifier = Modifier.width(130.dp).clickable {
                    tier.tierKey?.let {
                        onSelectTier(it)
                    }
                }
                    .then(if (isSelected) Modifier.border(2.dp, tierColor, RoundedCornerShape(16.dp)) else Modifier),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = if (isSelected) tierColor.copy(0.12f) else Color.White),
                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tier.iconEmoji ?: "🏆", fontSize = 22.sp)
                        if (isUser) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Success))
                        }
                    }
                    Text(tier.name?: "Unknown Tier", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${tier.totalMembers} members", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    Text("${tier.coinMultiplier}x coins/hr", style = MaterialTheme.typography.labelSmall, color = tierColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    if (tier.activeSessions > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(BpscColors.Success))
                            Text("${tier.activeSessions} live", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Leaderboard Tab ────────────────────────────────────────────

@Composable
private fun LeaderboardTab(
    entries: List<LeaderboardEntryDto>,
    isLoading: Boolean,
    myUserId: String,
    period: String,
    onChangePeriod: (String) -> Unit
) {
    val periods = listOf("weekly" to "This Week", "monthly" to "This Month", "alltime" to "All Time")

    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        // Period selector
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            periods.forEach { (key, label) ->
                val sel = period == key
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White).border(if (sel) 0.dp else 1.dp, BpscColors.Divider, RoundedCornerShape(20.dp)).clickable { onChangePeriod(key) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        } else if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆", fontSize = 40.sp)
                    Text("No data yet for this period", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Start studying to appear on the leaderboard!", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(entries) { idx, entry ->
                    LeaderboardEntryRow(entry = entry, isMe = entry.userId == myUserId)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryRow(entry: LeaderboardEntryDto, isMe: Boolean) {
    val bgColor = when (entry.rankPosition) {
        1 -> Color(0xFFFFF8E1); 2 -> Color(0xFFF5F5F5); 3 -> Color(0xFFFFF3E0)
        else -> if (isMe) BpscColors.PrimaryLight else Color.White
    }
    val rankEmoji = when (entry.rankPosition) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${entry.rankPosition}" }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(if (isMe) 3.dp else 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rankEmoji, fontSize = if (entry.rankPosition <= 3) 20.sp else 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.userName ?: "Unknown"}${if (isMe) " (You)" else ""}", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lv.${entry.xpLevel}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text("🔥${entry.streakDays}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.studyMinutes}m", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                Text("🪙${entry.coinsEarned}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
            }
        }
    }
}

// ── Members Tab ────────────────────────────────────────────────

@Composable
private fun MembersTab(members: List<TierMemberDto>, isLoading: Boolean) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(members, key = { it.id }) { member ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                        Text("Lv${member.xpLevel}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(member.name?: "Unknown User", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (member.isStudyingNow) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BpscColors.Success.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(BpscColors.Success))
                                    Text("live", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🔥${member.streak}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 11.sp)
                            Text("🎯${member.accuracy.toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 11.sp)
                            Text("${member.totalStudyMinutes/60}h", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 11.sp)
                        }
                    }
                    Text("🪙${member.coins}", style = MaterialTheme.typography.titleMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
