package com.example.bpscnotes.presentation.rooms

import android.util.Log
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
// RoomsHubScreen — Lobby
// Flow:
//   1. Shows user's current tier hero card (read-only)
//   2. Shows 4 tier rooms as a vertical list
//      - User's room: tappable → enters room (starts session) → StudyFocusScreen
//      - Locked rooms: tappable → shows "Requirements not met" bottom sheet, NOT entered
//   3. Promotion: when all conditions met → banner → user taps → auto-promote
//   4. No "Start Studying" button — tap the room card directly
// ════════════════════════════════════════════════════════════

@Composable
fun RoomsHubScreen(
    navController: NavHostController,
    tiersViewModel:   TierRoomsViewModel    = hiltViewModel(),
    sessionViewModel: StudySessionViewModel = hiltViewModel()
) {
    val state        by tiersViewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track which locked room sheet to show (null = none)
    var showLockedSheetForTier  by remember { mutableStateOf<RoomTierDto?>(null) }
    var showClaimDialog         by remember { mutableStateOf(false) }
    var claimResultMessage      by remember { mutableStateOf("") }

    // Claim promotion dialog
    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            icon   = { Text("🎉", fontSize = 36.sp) },
            title  = { Text("Claim Promotion!", fontWeight = FontWeight.ExtraBold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("You've met all requirements!", color = BpscColors.TextSecondary)
                    if (claimResultMessage.isNotEmpty()) {
                        Text(claimResultMessage, fontWeight = FontWeight.SemiBold, color = BpscColors.Primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClaimDialog = false
                        tiersViewModel.claimPromotion(
                            onSuccess = { emoji, name ->
                                claimResultMessage = ""
                                // TierPromotionOverlay will show via pendingPromotion flow
                            },
                            onFail = { reason ->
                                claimResultMessage = reason
                                showClaimDialog = true
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Success)
                ) { Text("Claim Now 🚀", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) { Text("Later") }
            }
        )
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sessionViewModel.checkForExistingSession()
            tiersViewModel.loadMyTier()
            tiersViewModel.loadAtRiskStatus()
        }
    }

    // Promotion overlay — full screen celebration
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

    // When session becomes ACTIVE (after entering a room) → navigate to StudyFocusScreen
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(sessionState.status) {
        when (sessionState.status) {
            SessionStatus.ACTIVE, SessionStatus.AFK -> {
                val current = navController.currentDestination?.route
                if (current != Screen.StudyFocus.route) {
                    navController.navigate(Screen.StudyFocus.route) { launchSingleTop = true }
                }
            }
            SessionStatus.ERROR -> {
                snackbarHost.showSnackbar(sessionState.error ?: "Failed to start session")
                sessionViewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
       // containerColor  = Color(0xFF051D56)
    ) { padding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(bottom = 32.dp),
        ) {

            // ── 1. HERO HEADER ────────────────────────────────
            item(key = "header") {
                RoomsHeroHeader(
                    state  = state,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── 2. DEMOTION WARNING ───────────────────────────
            if (state.atRisk.isAtRisk && state.showDemotionBanner) {
                item(key = "demotion") {
                    DemotionWarningBanner(
                        state      = AtRiskState(
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
                        }
                    )
                }
            }

            // ── 3. WHITE CONTENT CARD ─────────────────────────
            item(key = "content") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                      //  .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(BpscColors.Surface)
                        .padding(top = 8.dp)
                ) {

                    // Promotion ready banner
                    val allDone = state.myTierData?.progressItems?.all { it.done } == true
                        && state.myTierData?.nextTier != null
                    if (allDone) {
                        PromotionReadyBanner(
                            nextTierName = state.myTierData?.nextTier?.name ?: "Gold",
                            nextTierEmoji = state.myTierData?.nextTier?.iconEmoji ?: "🥇"
                        )
                    }

                    // Progress breakdown
                    state.myTierData?.let { tierData ->
                        if (tierData.progressItems.isNotEmpty() && tierData.nextTier != null) {
                            ProgressBreakdownCard(tierData = tierData)
                        }
                    }

                    // Section title
                    Text(
                        "Choose Your Room",
                        style     = MaterialTheme.typography.titleMedium,
                        color     = BpscColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier  = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                    Text(
                        "Tap your room to start studying. Locked rooms unlock as you progress.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = BpscColors.TextHint,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    // ── ROOMS LIST ────────────────────────────
                    if (state.isLoadingTiers) {
                        repeat(4) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BpscColors.Divider)
                            )
                        }
                    } else {
                        state.allTiers.forEach { tier ->
                            val myTierKey      = state.myTierData?.currentTier?.tierKey
                            val myTierOrder    = state.myTierData?.currentTier?.sortOrder ?: 1
                            val isMyRoom       = tier.tierKey == myTierKey
                            val allDoneForNext = state.myTierData?.progressItems?.all { it.done } == true
                                && state.myTierData?.nextTier != null
                            val isNextTier     = tier.sortOrder == myTierOrder + 1
                            val isClaimReady   = isNextTier && allDoneForNext
                            val isLowerTier    = tier.sortOrder < myTierOrder  // e.g. Silver when user is Gold
                          //  val isLocked       = tier.sortOrder > myTierOrder && !isClaimReady

                            Log.e("TAG", "RoomsHubScreen1212: $myTierKey", )

                            val currentOrder =
                                state.myTierData?.currentTier?.sortOrder ?: 0

                            val isLocked =
                                tier.sortOrder > currentOrder &&
                                        !isClaimReady

                            Log.e(
                                "ROOM_DEBUG",
                                "tier=${tier.tierKey} tierOrder=${tier.sortOrder} currentOrder=$currentOrder locked=$isLocked"
                            )
                            val tierColor    = try {
                                Color(android.graphics.Color.parseColor(tier.colorHex))
                            } catch (e: Exception) { BpscColors.Primary }
                            val isStarting   = sessionState.status == SessionStatus.STARTING && isMyRoom

                            RoomCard(
                                tier         = tier,
                                tierColor    = tierColor,
                                isMyRoom     = isMyRoom,
                                isLocked     = isLocked,
                                isClaimReady = isClaimReady,
                                isLowerTier  = isLowerTier,
                                isStarting   = isStarting,
                                onClick      = {
                                    when {
                                        isClaimReady -> showClaimDialog = true
                                        isLocked     -> showLockedSheetForTier = tier
                                        // Lower tier OR own tier: always start session
                                        // Session backend uses user's DB tier for coins/XP.
                                        // Tapping Silver as a Gold user is fine — rewards
                                        // are determined server-side by current_tier_id.
                                        else         -> sessionViewModel.startSession(mode = "study")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick links
                    QuickActionRow(
                        onAchievements = { navController.navigate(Screen.Achievements.route) },
                        onChallenges   = { navController.navigate(Screen.WeeklyChallenges.route) }
                    )

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // Locked room bottom sheet
    showLockedSheetForTier?.let { lockedTier ->
        LockedRoomSheet(
            tier        = lockedTier,
            progressItems = state.myTierData?.progressItems ?: emptyList(),
            onDismiss   = { showLockedSheetForTier = null }
        )
    }
}

// ════════════════════════════════════════════════════════════
// HERO HEADER
// ════════════════════════════════════════════════════════════
@Composable
private fun RoomsHeroHeader(state: TierRoomsUiState, onBack: () -> Unit) {
    val myTier    = state.myTierData?.currentTier
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
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
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
                        Text("Tap your room to start", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                    }
                }
                if (state.isSocketConnected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BpscColors.Success.copy(0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BpscColors.Success))
                        Text("Live", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (state.isLoadingMyTier) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.08f)))
                return@Column
            }

            if (myTier != null) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(tierColor.copy(0.25f)),
                                    contentAlignment = Alignment.Center
                                ) { Text(myTier.iconEmoji ?: "🏆", fontSize = 26.sp) }
                                Column {
                                    Text(myTier.name ?: "Silver Room",
                                        style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text("${myTier.displayMembers} members · ${myTier.displayActive} online now",
                                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.65f))
                                }
                            }
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BpscColors.CoinGold.copy(0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("🪙 ${myTier.coinMultiplier}×/hr",
                                    style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Progress bar to next tier
                        state.myTierData?.nextTier?.let { next ->
                            val progress  = state.myTierData.nextTierProgress.toFloat()
                            val animProg  by animateFloatAsState(progress, tween(900), label = "prog")
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(0.15f))) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth(animProg.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF64B5F6),
                                                    Color.White
                                                )
                                            ), RoundedCornerShape(3.dp)
                                        ))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Progress to ${next.iconEmoji ?: ""} ${next.name}",
                                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 10.sp)
                                    Text("${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }

                        // Stats strip
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
// ROOM CARD — tap to enter (own room) or see requirements (locked)
// ════════════════════════════════════════════════════════════
@Composable
private fun RoomCard(
    tier:         RoomTierDto,
    tierColor:    Color,
    isMyRoom:     Boolean,
    isLocked:     Boolean,
    isClaimReady: Boolean = false,
    isLowerTier:  Boolean = false,
    isStarting:   Boolean,
    onClick:      () -> Unit
) {
    val bgColor = when {
        isClaimReady -> BpscColors.Success.copy(0.05f)
        isMyRoom     -> Color.White
        isLocked     -> Color(0xFFF7F7F7)
        else         -> Color.White
    }

    val borderColor = when {
        isClaimReady -> BpscColors.Success
        isMyRoom     -> tierColor.copy(alpha = 0.45f)
        isLocked     -> Color(0xFFE2E2E2)
        else         -> BpscColors.Divider
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (isMyRoom) 1.5.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLocked) Color(0xFFE8E8E8) else tierColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isStarting   -> CircularProgressIndicator(color = tierColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    isClaimReady -> Text("🎉", fontSize = 24.sp)
                    isLocked     -> Icon(Icons.Rounded.Lock, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(22.dp))
                    else         -> Text(tier.iconEmoji ?: "🏆", fontSize = 24.sp)
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tier.name ?: tier.tierKey.replaceFirstChar { it.uppercase() },
                        style      = MaterialTheme.typography.titleMedium,
                        color      = if (isLocked) Color(0xFFAAAAAA) else BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    when {
                        isClaimReady -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.Success.copy(0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("🎉 Claim Now!", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                        isMyRoom -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.Success.copy(0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("Your Room", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                        isLocked -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("Locked", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFAAAAAA), fontSize = 9.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🪙 ${tier.coinMultiplier}×/hr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLocked) Color(0xFFCCCCCC) else tierColor,
                        fontWeight = FontWeight.SemiBold)
                    Text("👥 ${tier.displayMembers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLocked) Color(0xFFCCCCCC) else BpscColors.TextHint)
                    if (tier.displayActive > 0 && !isLocked) {
                        Text("🟢 ${tier.displayActive} live",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.Success)
                    }
                }
                // Perks teaser for locked rooms
                if (isLocked && tier.perks.isNotEmpty()) {
                    Text(tier.perks.firstOrNull() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBBBBBB), fontSize = 10.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }

            // Right arrow for own room; chevron-down for locked
            when {
                isClaimReady -> Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BpscColors.Success.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.ArrowForward, null, tint = BpscColors.Success, modifier = Modifier.size(18.dp)) }
                isMyRoom -> Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.PlayArrow, null, tint = tierColor, modifier = Modifier.size(18.dp)) }
                isLocked -> Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// LOCKED ROOM SHEET — shows requirements when tapping locked room
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockedRoomSheet(
    tier:          RoomTierDto,
    progressItems: List<TierProgressItemDto>,
    onDismiss:     () -> Unit
) {
    val tierColor = try { Color(android.graphics.Color.parseColor(tier.colorHex)) } catch (e: Exception) { BpscColors.Primary }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = BpscColors.Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {
            // Handle
            Box(Modifier
                .size(40.dp, 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BpscColors.Divider))

            // Tier badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(tierColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Text(tier.iconEmoji ?: "🔒", fontSize = 36.sp) }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${tier.name} is Locked", style = MaterialTheme.typography.headlineSmall,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                Text("Complete these requirements in Silver Room first",
                    style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
            }

            // Requirements list
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Requirements", style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    progressItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.done) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
                                else Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp))
                                Text(item.label, style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.done) BpscColors.TextPrimary else BpscColors.TextSecondary)
                            }
                            Text("${item.current.toInt()}/${item.required.toInt()} ${item.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.done) BpscColors.Success else BpscColors.TextHint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Perks preview
            if (tier.perks.isNotEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tierColor.copy(0.06f)),
                    border = BorderStroke(1.dp, tierColor.copy(0.2f))) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${tier.iconEmoji ?: ""} Perks you'll unlock:", style = MaterialTheme.typography.labelMedium,
                            color = tierColor, fontWeight = FontWeight.Bold)
                        tier.perks.take(3).forEach { perk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("•", color = tierColor)
                                Text(perk, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                            }
                        }
                    }
                }
            }

            Button(
                onClick  = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) { Text("Got it, I'll keep studying!", fontWeight = FontWeight.Bold) }
        }
    }
}

// ════════════════════════════════════════════════════════════
// PROMOTION READY BANNER
// ════════════════════════════════════════════════════════════
@Composable
private fun PromotionReadyBanner(nextTierName: String, nextTierEmoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(0.3f, 0.7f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = BpscColors.Success.copy(0.12f)),
        border   = BorderStroke(1.5.dp, BpscColors.Success.copy(glowAlpha))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎉", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Ready for $nextTierEmoji $nextTierName!", style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                Text("All requirements met! You'll be promoted at midnight.",
                    style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// PROGRESS BREAKDOWN CARD
// ════════════════════════════════════════════════════════════
@Composable
private fun ProgressBreakdownCard(tierData: MyTierResponseData) {
    val next = tierData.nextTier ?: return
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Progress to ${next.iconEmoji ?: ""} ${next.name}",
                style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            tierData.progressItems.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (item.done) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(13.dp))
                            else Box(Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, BpscColors.Divider, CircleShape))
                            Text(item.label, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextPrimary)
                        }
                        Text("${item.current.toInt()}/${item.required.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    }
                    val pct by animateFloatAsState(
                        (item.current / item.required.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f),
                        tween(700), label = "p_${item.label}")
                    Box(Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BpscColors.Divider)) {
                        Box(Modifier
                            .fillMaxWidth(pct)
                            .fillMaxHeight()
                            .background(
                                if (item.done) BpscColors.Success else BpscColors.Primary,
                                RoundedCornerShape(3.dp)
                            ))
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
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickActionCard("🏅", "Achievements", "Unlock badges", BpscColors.CoinGold, Modifier.weight(1f), onAchievements)
        QuickActionCard("⚡", "Challenges", "Weekly goals", BpscColors.Primary, Modifier.weight(1f), onChallenges)
    }
}

@Composable
private fun QuickActionCard(icon: String, title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        border = BorderStroke(1.dp, color.copy(0.15f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }
    }
}
