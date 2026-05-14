package com.example.bpscnotes.presentation.rooms

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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

@Composable
fun RoomsHubScreen(
    navController: NavHostController,
    tiersViewModel: TierRoomsViewModel      = hiltViewModel(),
    sessionViewModel: StudySessionViewModel = hiltViewModel()
) {
    val state        by tiersViewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sessionViewModel.checkForExistingSession()
            tiersViewModel.loadMyTier()
            tiersViewModel.loadAtRiskStatus()
        }
    }

    // Promotion overlay — full screen
    state.pendingPromotion?.let { promotion ->
        val promoTier = state.allTiers.firstOrNull { it.tierKey == promotion.tierKey }
        if (promoTier != null) {
            TierPromotionOverlay(
                newTier   = promoTier,
                onDismiss = {
                    tiersViewModel.clearPendingPromotion()
                    tiersViewModel.loadMyTier()
                }
            )
            return
        }
    }

    // Navigate to StudyFocus when session becomes ACTIVE.
    // Use a var+key pattern so rotation doesn't re-trigger navigation.
    // LaunchedEffect(Unit) runs once per composition entry (survives rotation
    // because ViewModel is retained, but NavBackStack pops properly).
    LaunchedEffect(sessionState.status) {
        Log.d("ROOM_NAV", "status=${sessionState.status}")

        when (sessionState.status) {
            SessionStatus.ACTIVE, SessionStatus.AFK -> {
                // Only navigate if we are not already on StudyFocus
                val currentDest = navController.currentDestination?.route
                Log.d("ROOM_NAV", "navigate route1=${Screen.StudyFocus.route}")
                if (currentDest != Screen.StudyFocus.route) {
                    Log.d("ROOM_NAV", "navigate route2=${Screen.StudyFocus.route}")
                    navController.navigate(Screen.StudyFocus.route) {
                        Log.d("ROOM_NAV", "navigate route3=${Screen.StudyFocus.route}")
                        launchSingleTop = true
                    }
                }
            }
            SessionStatus.ERROR -> {
                // Reset to IDLE so button becomes enabled again
                sessionViewModel.clearError()
            }
            else -> { /* idle / ending / ended — stay on screen */ }
        }
    }

    // Error snackbar for session start failure
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(sessionState.error) {
        sessionState.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            sessionViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color(0xFF051D56)
    ) { scaffoldPadding ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)) {

            // ── Scrollable full screen ────────────────────────
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── 1. HERO HEADER ────────────────────────────
                item(key = "header") {
                    HeroHeader(
                        state        = state,
                        sessionState = sessionState,
                        onBack       = { navController.popBackStack() },
                        onStart      = {
                            // startSession() → status becomes STARTING → ACTIVE
                            // The LaunchedEffect(sessionState.status) below handles navigation
                            // once ACTIVE is confirmed. Do NOT navigate here directly.
                            sessionViewModel.startSession(mode = "study")
                        }
                    )
                }

                // ── 2. DEMOTION WARNING ───────────────────────
                if (state.atRisk.isAtRisk && state.showDemotionBanner) {
                    item(key = "demotion") {
                        DemotionWarningBanner(
                            state     = AtRiskState(
                                isAtRisk  = state.atRisk.isAtRisk,
                                progress  = state.atRisk.progress,
                                threshold = state.atRisk.threshold,
                                tierKey   = state.atRisk.tierKey,
                                tierName  = state.atRisk.tierName,
                                tierEmoji = state.atRisk.tierEmoji,
                            ),
                            onDismiss  = { tiersViewModel.dismissDemotionBanner() },
                            onStudyNow = {
                                tiersViewModel.dismissDemotionBanner()
                                sessionViewModel.startSession(mode = "study")
                                navController.navigate(Screen.StudyFocus.route)
                            }
                        )
                    }
                }

                // ── 3. CONTENT AREA (white bg) ────────────────
                item(key = "content_area") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(BpscColors.Surface)
                            .padding(top = 8.dp)
                    ) {

                        // ── 3a. Progress breakdown ────────────
                        state.myTierData?.let { tierData ->
                            if (tierData.progressItems.isNotEmpty() && tierData.nextTier != null) {
                                ProgressBreakdownCard(tierData = tierData)
                            }
                        }

                        // ── 3b. All 4 tier rooms ──────────────
                        Spacer(Modifier.height(12.dp))

                        val myTier = state.myTierData

                        if (myTier == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            return@Column
                        }
                        TierRoomsGrid(
                            tiers         = state.allTiers,
                            userTierKey   = state.myTierData?.currentTier?.tier_Key,
                            selectedKey   = state.selectedTierKey,
                            isLoading     = state.isLoadingTiers,
                            onSelect      = { tiersViewModel.selectTier(it) }
                        )

                        // ── 3b.5 Browsing banner ─────────────
                        val selectedKey = state.selectedTierKey
                        val myTierKey   = state.myTierData?.currentTier?.tier_Key
                        if (selectedKey != null && myTierKey != null && selectedKey != myTierKey) {
                            val selectedTier = state.allTiers.firstOrNull { it.tierKey == selectedKey }
                            if (selectedTier != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Info, null, tint = BpscColors.Primary, modifier = Modifier.size(14.dp))
                                        Text(
                                            "Viewing ${selectedTier.name} leaderboard. Keep studying in ${state.myTierData?.currentTier?.name ?: "Silver"} to unlock this room.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BpscColors.Primary, fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ── 3c. Quick action row ──────────────
                        QuickActionRow(
                            onAchievements = { navController.navigate(Screen.Achievements.route) },
                            onChallenges   = { navController.navigate(Screen.WeeklyChallenges.route) }
                        )

                        // ── 3d. Leaderboard ───────────────────
                        LeaderboardSection(
                            entries   = state.leaderboard,
                            isLoading = state.isLoadingLeaderboard,
                            period    = state.leaderboardPeriod,
                            selectedTierKey = state.selectedTierKey,
                            onChangePeriod = { p ->
                                val key = state.selectedTierKey ?: return@LeaderboardSection
                                tiersViewModel.loadLeaderboard(key, p)
                            }
                        )

                        // ── 3e. Members online ────────────────
                        if (state.members.isNotEmpty() || state.isLoadingMembers) {
                            MembersSection(
                                members   = state.members,
                                isLoading = state.isLoadingMembers
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// HERO HEADER
// ════════════════════════════════════════════════════════════
@Composable
private fun HeroHeader(
    state:        TierRoomsUiState,
    sessionState: StudySessionUiState,
    onBack:       () -> Unit,
    onStart:      () -> Unit
) {
    val myTier = state.myTierData?.currentTier
    val tierColor = try {
        Color(android.graphics.Color.parseColor(myTier?.colorHex ?: "#9E9E9E"))
    } catch (e: Exception) { BpscColors.CoinGold }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(500f, 500f)
                )
            )
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)) {

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Group Study", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Study together, grow faster", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.65f))
                    }
                }
                // Socket status indicator
                if (state.isSocketConnected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BpscColors.Success.copy(0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BpscColors.Success))
                        Text("Live", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (state.isLoadingMyTier) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.08f)))
            } else if (myTier != null) {
                // ── My Tier Hero Card ─────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Tier badge
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(tierColor.copy(0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(myTier.iconEmoji ?: "🏆", fontSize = 26.sp)
                                }
                                Column {
                                    Text(myTier.name ?: "Silver Room",
                                        style = MaterialTheme.typography.titleLarge, color = Color.White,
                                        fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        "${myTier.displayMembers} members · ${myTier.displayActive} studying now",
                                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.65f)
                                    )
                                }
                            }
                            // Coin multiplier badge
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BpscColors.CoinGold.copy(0.2f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("🪙 ${myTier.coinMultiplier}×/hr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                                }
                                val nextTier = state.myTierData?.nextTier
                                if (nextTier != null) {
                                    val pct = ((state.myTierData?.nextTierProgress ?: 0.0) * 100).toInt()
                                    Spacer(Modifier.height(4.dp))
                                    Text("→ ${nextTier.iconEmoji ?: ""} $pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(0.6f), fontSize = 10.sp)
                                }
                            }
                        }

                        // Progress bar to next tier
                        state.myTierData?.nextTier?.let { next ->
                            val progress = state.myTierData.nextTierProgress.toFloat()
                            val animProg by animateFloatAsState(progress, tween(900), label = "prog")
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animProg.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF64B5F6),
                                                        Color.White
                                                    )
                                                ),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                Text(
                                    "Progress to ${next.iconEmoji ?: ""} ${next.name} — ${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.6f), fontSize = 10.sp
                                )
                            }
                        } ?: run {
                            Text("💎 Maximum tier — Diamond Elite",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                        }

                        // User stats strip
                        state.myTierData?.userStats?.let { stats ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatPill("⏱️", "${stats.totalStudyHours.toInt()}h", "Studied")
                                StatPill("🔥", "${stats.streak}", "Streak")
                                StatPill("📝", "${stats.quizzesAttempted}", "Quizzes")
                                StatPill("🎯", "${stats.accuracy.toInt()}%", "Accuracy")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── START STUDYING BUTTON ─────────────────────────
            Button(
                onClick  = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                // Enabled: idle OR error (retry). Disabled during STARTING/ENDING/ACTIVE.
                enabled  = state.myTierData != null &&
                        (sessionState.status == SessionStatus.IDLE ||
                                sessionState.status == SessionStatus.ERROR),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color(0xFF051D56),
                    disabledContainerColor = Color.White.copy(0.3f),
                    disabledContentColor   = Color.White.copy(0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                when (sessionState.status) {
                    SessionStatus.STARTING -> {
                        CircularProgressIndicator(
                            color = Color(0xFF051D56),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Starting session…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    else -> {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        // Always start in user's OWN tier — not the browsed tier
                        val myTierName = state.myTierData?.currentTier?.name ?: "Silver"
                        val browsingOther = state.selectedTierKey != state.myTierData?.currentTier?.tier_Key
                        Text(
                            if (browsingOther) "Start Studying · $myTierName ↑"
                            else "Start Studying · $myTierName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 12.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f), fontSize = 9.sp)
    }
}

// ════════════════════════════════════════════════════════════
// PROGRESS BREAKDOWN CARD
// ════════════════════════════════════════════════════════════
@Composable
private fun ProgressBreakdownCard(tierData: MyTierResponseData) {
    val next = tierData.nextTier ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress to ${next.iconEmoji ?: ""} ${next.name}",
                    style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold)
                val allDone = tierData.progressItems.all { it.done }
                if (allDone) {
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.Success.copy(0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("✅ Ready to promote!", style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }
            }
            tierData.progressItems.forEach { item ->
                ProgressConditionRow(item = item)
            }
            if (tierData.progressItems.isEmpty()) {
                Text("Study, complete quizzes and set daily goals to unlock ${next.name}.",
                    style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ProgressConditionRow(item: TierProgressItemDto) {
    val pct = (item.current / item.required.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
    val animPct by animateFloatAsState(pct, tween(700), label = "cond_${item.label}")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.done) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                else Box(Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, BpscColors.Divider, CircleShape))
                Text(item.label, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            Text("${item.current.toInt()} / ${item.required.toInt()} ${item.unit}",
                style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(BpscColors.Divider)) {
            Box(modifier = Modifier
                .fillMaxWidth(animPct)
                .fillMaxHeight()
                .background(
                    if (item.done) BpscColors.Success else BpscColors.Primary,
                    RoundedCornerShape(3.dp)
                ))
        }
    }
}

// ════════════════════════════════════════════════════════════
// TIER ROOMS GRID
// ════════════════════════════════════════════════════════════
@Composable
private fun TierRoomsGrid(
    tiers:       List<RoomTierDto>,
    userTierKey: String?,
    selectedKey: String?,
    isLoading:   Boolean,
    onSelect:    (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Text("All Rooms", style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 10.dp))

        if (isLoading) {
            repeat(4) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BpscColors.Divider))
            }
            return@Column
        }

        tiers.forEach { tier ->
            val isUser     = tier.tierKey == userTierKey

            Log.e("TAG", "TierRoomsGrid $isUser  ${tier.tierKey}   $userTierKey: ", )
            val tierColor  = try { Color(android.graphics.Color.parseColor(tier.colorHex)) } catch (e: Exception) { BpscColors.Primary }
            val currentLevel = when (userTierKey?.lowercase()) {
                "silver"  -> 1
                "gold"    -> 2
                "premium" -> 3
                "diamond" -> 4
                else      -> 0
            }

            val tierLevel = when (tier.tierKey.lowercase()) {
                "silver"  -> 1
                "gold"    -> 2
                "premium" -> 3
                "diamond" -> 4
                else      -> Int.MAX_VALUE
            }

            val locked = tierLevel > currentLevel
            val isSelected = tier.tierKey == selectedKey// && !locked

            val isHigherTier = locked && !isUser

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(enabled = !locked) {
                        onSelect(tier.tierKey)
                    }
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            tierColor,
                            RoundedCornerShape(14.dp)
                        ) else Modifier
                    ),
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        when {
                            locked -> Color(0xFFF5F5F5)
                           // isSelected -> tierColor.copy(0.07f)
                            else -> Color.White
                        }
                ),
                elevation = CardDefaults.cardElevation(/*if (isSelected) 3.dp else */1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isHigherTier) BpscColors.Divider else tierColor.copy(
                                    0.12f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isHigherTier) {
                            Icon(Icons.Rounded.Lock, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                        } else {
                            Text(tier.iconEmoji ?: "🏆", fontSize = 20.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tier.name ?: tier.tierKey.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isHigherTier) BpscColors.TextHint else BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold)
                            when {
                                isUser -> Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.Success.copy(0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("Your Room", style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                                isHigherTier -> Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.TextHint.copy(0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("Browse only", style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.TextHint, fontSize = 9.sp)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🪙 ${tier.coinMultiplier}×/hr",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHigherTier) BpscColors.TextHint else tierColor,
                                fontWeight = FontWeight.SemiBold)
                            Text("👥 ${tier.displayMembers}",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                            if (tier.displayActive > 0) {
                                Text("🟢 ${tier.displayActive} live",
                                    style = MaterialTheme.typography.labelSmall, color = BpscColors.Success)
                            }

                            Log.e("TAG", "TierRoomsGrid: $locked  ${tier.tierKey}", )

                            Log.e(
                                "ROOM_LOCK",
                                "userTier=$userTierKey tier=${tier.tierKey} locked=$locked"
                            )
                            /* when {
                                 locked -> {
                                     Text(
                                         "🔒 Locked",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = BpscColors.TextHint
                                     )
                                 }


                                 tier.displayActive > 0 -> {
                                     Text(
                                         "🟢 ${tier.displayActive} studying",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = BpscColors.Success
                                     )
                                 }

                                 else -> {
                                     Text(
                                         "No active sessions",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = BpscColors.TextHint
                                     )
                                 }
                             }*/
                        }
                    }
                    if (isSelected) {
                      //  Icon(Icons.Rounded.KeyboardArrowRight, null, tint = tierColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// QUICK ACTION ROW
// ════════════════════════════════════════════════════════════
@Composable
private fun QuickActionRow(onAchievements: () -> Unit, onChallenges: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionCard("🏅", "Achievements", "Unlock badges", BpscColors.CoinGold, Modifier.weight(1f), onAchievements)
        QuickActionCard("⚡", "Challenges", "Weekly goals", BpscColors.Primary, Modifier.weight(1f), onChallenges)
    }
}

@Composable
private fun QuickActionCard(icon: String, title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        border   = BorderStroke(1.dp, color.copy(0.15f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// LEADERBOARD SECTION
// ════════════════════════════════════════════════════════════
@Composable
private fun LeaderboardSection(
    entries:        List<LeaderboardEntryDto>,
    isLoading:      Boolean,
    period:         String,
    selectedTierKey: String?,
    onChangePeriod: (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 10.dp))

        // Period tabs
        val periods = listOf("weekly" to "This Week", "monthly" to "Month", "alltime" to "All Time")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            periods.forEach { (key, label) ->
                val sel = period == key
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) BpscColors.Primary else Color.White)
                    .border(if (sel) 0.dp else 1.dp, BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onChangePeriod(key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        when {
            isLoading -> Box(Modifier
                .fillMaxWidth()
                .height(80.dp), Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(24.dp))
            }
            entries.isEmpty() -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏆", fontSize = 36.sp)
                    Text("No rankings yet", style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Start a study session to appear here.\nRankings update every Sunday night.",
                        style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary,
                        textAlign = TextAlign.Center)
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.take(10).forEachIndexed { _, entry ->
                    LeaderboardRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto) {
    val bgColor = when (entry.rankPosition) {
        1 -> Color(0xFFFFF8E1); 2 -> Color(0xFFF5F5F5); 3 -> Color(0xFFFFF3E0)
        else -> Color.White
    }
    val medal = when (entry.rankPosition) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${entry.rankPosition}" }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(medal, fontSize = if (entry.rankPosition <= 3) 18.sp else 12.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.userName ?: "Unknown", style = MaterialTheme.typography.titleSmall,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lv.${entry.xpLevel}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    Text("🔥${entry.streakDays}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.studyMinutes}m", style = MaterialTheme.typography.titleSmall,
                    color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                Text("🪙${entry.coinsEarned}", style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.CoinGold, fontSize = 10.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// MEMBERS SECTION
// ════════════════════════════════════════════════════════════
@Composable
private fun MembersSection(members: List<TierMemberDto>, isLoading: Boolean) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Members Online", style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 10.dp))
        if (isLoading) {
            Box(Modifier
                .fillMaxWidth()
                .height(60.dp), Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(24.dp))
            }
            return@Column
        }
        members.take(5).forEach { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BpscColors.PrimaryLight),
                    contentAlignment = Alignment.Center) {
                    Text("Lv${member.xpLevel}", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(member.name, style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                        if (member.isStudyingNow) {
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BpscColors.Success.copy(0.1f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text("live", style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                    Text("🔥${member.streak} streak · ${member.totalStudyMinutes/60}h total",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                Text("🪙${member.coins}", style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}
