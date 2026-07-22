package com.example.bpscnotes.presentation.rooms

import android.util.Log
import kotlinx.coroutines.delay
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
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
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
    tiersViewModel:   TierRoomsViewModel,
    sessionViewModel: StudySessionViewModel,
    adManager: com.example.bpscnotes.core.ads.AdManager? = null,
) {
    val state by tiersViewModel.uiState.collectAsState()
    val tiersState = state  // alias used in DemotionWarningBanner block
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("rooms_hub") }
    val sessionState by sessionViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track which locked room sheet to show (null = none)
    var showLockedSheetForTier by remember { mutableStateOf<RoomTierDto?>(null) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var claimResultMessage by remember { mutableStateOf("") }

    // Claim promotion dialog
    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            icon = { Text("🎉", fontSize = 36.sp) },
            title = { Text(str.roomsClaimPromotion, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(str.roomsMetRequirements, color = cs.onSurfaceVariant)
                    if (claimResultMessage.isNotEmpty()) {
                        Text(
                            claimResultMessage,
                            fontWeight = FontWeight.SemiBold,
                            color = BpscColors.Primary
                        )
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
                ) { Text(str.roomsClaimNow, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) { Text(str.roomsLater) }
            }
        )
    }

    // FIX: Only check session on resume, not full reload every tab switch.
    // Full reload (loadMyTier + loadAtRiskStatus) only when data isn't loaded yet.
    // This eliminates the flicker caused by repeated reloads on tab switches.
    LaunchedEffect(Unit) {
        sessionViewModel.checkForExistingSession()
        // Only load if not already loaded (ViewModel handles caching)
        if (state.myTierData == null) {
            tiersViewModel.loadMyTier()
            tiersViewModel.loadAtRiskStatus()
        }
    }

    // Re-check session on lifecycle resume + silently refresh the tier data
    // (stats tiles: Studied/Streak/Quizzes/Accuracy). refreshMyTierCounts()
    // does NOT flip the loading flag, so no skeleton flicker — but returning
    // from a study session / quiz now shows fresh numbers instead of the
    // stale first-load snapshot (QA 09-Jul issue 11 "not updating").
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sessionViewModel.checkForExistingSession()
            tiersViewModel.refreshMyTierCounts()
            tiersViewModel.loadAtRiskStatus()
        }
    }

    // BUG FIX: "X members · Y online now" on the hero card comes from
    // myTierData.currentTier (activeNow/memberCount), which socket events
    // (presenceUpdates/presenceSnapshot) do NOT update — those only patch
    // allTiers[]. Without this, the hero counts only ever reflect the
    // single load on first entry and never change while browsing.
    // Poll every 15s while the screen is visible, as a safety net
    // alongside the socket-driven updates to allTiers[].
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(15_000L)
                tiersViewModel.refreshMyTierCounts()
            }
        }
    }

    // Promotion overlay — full screen celebration
    state.pendingPromotion?.let { promotion ->
        val promoTier = state.allTiers.firstOrNull { it.tierKey == promotion.tierKey }
        if (promoTier != null) {
            TierPromotionOverlay(
                newTier = promoTier,
                onDismiss = {
                    tiersViewModel.clearPendingPromotion()
                    tiersViewModel.loadMyTier()
                }
            )
            return
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    var showRoomInfo by remember { mutableStateOf(false) }

    // Track previous status so we only auto-navigate to StudyFocus when session
    // transitions FROM a non-active state TO active (i.e. user just joined a room).
    // If status is already ACTIVE when RoomsHub loads, the user returned via PIP —
    // do NOT auto-push them back to StudyFocus. That's what caused the back-press loop.
    var prevStatus by remember { mutableStateOf(sessionState.status) }

    LaunchedEffect(sessionState.status) {
        val prev = prevStatus
        val curr = sessionState.status
        prevStatus = curr

        when (curr) {
            SessionStatus.ACTIVE, SessionStatus.AFK -> {
                // Only navigate to StudyFocus if this is a FRESH join (status just became active).
                // If prev was already ACTIVE/AFK, user returned from PIP — stay on RoomsHub.
                val justJoined = prev == SessionStatus.STARTING || prev == SessionStatus.IDLE || prev == SessionStatus.ERROR
                if (justJoined) {
                    val current = navController.currentDestination?.route
                    if (current != Screen.StudyFocus.route) {
                        navController.navigate(Screen.StudyFocus.route) { launchSingleTop = true }
                    }
                }
                // If prev was ACTIVE/AFK → user came back via PIP. Show PIP overlay, stay here.
            }

            SessionStatus.ERROR -> {
                snackbarHost.showSnackbar(sessionState.error ?: str.error)
                sessionViewModel.clearError()
            }

            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // containerColor  = Color(0xFF051D56)
    ) { padding ->

        // FIX: Pin header using Column + pinned header above scrollable LazyColumn
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Pinned — does not scroll
            RoomsHeroHeader(
                state = state,
                onBack = { navController.popBackStackSafe() },
                onRoomInfo = { showRoomInfo = true },
            )

            if (showRoomInfo) {
                RoomInfoSheet(tier = state.myTierData?.currentTier, onDismiss = { showRoomInfo = false })
            }

            // Scrollable content — pull down to force-refresh everything
            // (tier, stats, leaderboard, room insights): QA 09-Jul issue 11
            // explicitly asked for pull-to-refresh here.
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = state.isLoadingMyTier,
                onRefresh = {
                    tiersViewModel.loadMyTier()
                    tiersViewModel.loadAtRiskStatus()
                },
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 22.dp)
            ) {

                // ── 2. DEMOTION WARNING ───────────────────────────
                if (state.atRisk.isAtRisk && state.showDemotionBanner) {
                    item(key = "demotion") {
                        DemotionWarningBanner(
                            state = AtRiskState(
                                isAtRisk = state.atRisk.isAtRisk,
                                progress = state.atRisk.progress,
                                threshold = state.atRisk.threshold,
                                tierKey = state.atRisk.tierKey,
                                tierName = state.atRisk.tierName,
                                tierEmoji = state.atRisk.tierEmoji,
                            ),
                            onDismiss = { tiersViewModel.dismissDemotionBanner() },
                            onStudyNow = {
                                tiersViewModel.dismissDemotionBanner()
                                sessionViewModel.startSession(
                                    mode         = "study",
                                    tierName     = tiersState.myTierData?.currentTier?.name,
                                    tierEmoji    = tiersState.myTierData?.currentTier?.iconEmoji,
                                    tierColorHex = tiersState.myTierData?.currentTier?.colorHex
                                )
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
                            .background(cs.background)
                    ) {


                        if (state.myTierData?.currentTier != null) {
                            MyRoomProgressCard(state)
                        }

                        // Promotion ready banner
                        val allDone = state.myTierData?.progressItems?.all { it.done } == true
                                && state.myTierData?.nextTier != null
                        if (allDone) {
                            PromotionReadyBanner(
                                nextTierName = state.myTierData?.nextTier?.name ?: str.roomsTierSilver,
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
                            str.roomsChoose,
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                        Text(
                            str.roomsChooseHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = BpscColors.TextHint,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )


                        // ── Above Room list banner ad ──────────────────────────────
                        if (adManager != null) {
                            com.example.bpscnotes.core.ads.BannerAdView(
                                adUnitId = adManager.getBannerAdUnitId()
                            )
                        } else {
                            Spacer(Modifier.height(80.dp))
                        }

                        // ── ROOMS LIST ────────────────────────────
                        if (state.isLoadingTiers) {
                            repeat(4) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(cs.outline)
                                )
                            }
                        } else {
                            state.allTiers.forEach { tier ->
                                val myTierKey = state.myTierData?.currentTier?.tierKey
                                val myTierOrder = state.myTierData?.currentTier?.sortOrder ?: 1
                                val isMyRoom = tier.tierKey == myTierKey
                                val allDoneForNext =
                                    state.myTierData?.progressItems?.all { it.done } == true
                                            && state.myTierData?.nextTier != null
                                val isNextTier = tier.sortOrder == myTierOrder + 1
                                val isClaimReady = isNextTier && allDoneForNext
                                val isLowerTier =
                                    tier.sortOrder < myTierOrder  // e.g. Starter when user is Serious
                                //  val isLocked       = tier.sortOrder > myTierOrder && !isClaimReady

                                Log.e("TAG", "RoomsHubScreen1212: $myTierKey",)

                                val currentOrder =
                                    state.myTierData?.currentTier?.sortOrder ?: 0

                                val isLocked =
                                    tier.sortOrder > currentOrder &&
                                            !isClaimReady

                                Log.e(
                                    "ROOM_DEBUG",
                                    "tier=${tier.tierKey} tierOrder=${tier.sortOrder} currentOrder=$currentOrder locked=$isLocked"
                                )
                                val tierColor = try {
                                    Color(android.graphics.Color.parseColor(tier.colorHex))
                                } catch (e: Exception) {
                                    BpscColors.Primary
                                }
                                val isStarting =
                                    sessionState.status == SessionStatus.STARTING && isMyRoom

                                RoomCard(
                                    tier = tier,
                                    tierColor = tierColor,
                                    isMyRoom = isMyRoom,
                                    isLocked = isLocked,
                                    isClaimReady = isClaimReady,
                                    isLowerTier = isLowerTier,
                                    isStarting = isStarting,
                                    onClick = {
                                        when {
                                            isClaimReady -> showClaimDialog = true
                                            isLocked -> showLockedSheetForTier = tier
                                            // Lower tier OR own tier: always start session
                                            // Session backend uses user's DB tier for coins/XP.
                                            // Tapping Starter as a Serious user is fine — rewards
                                            // are determined server-side by current_tier_id.
                                            else -> sessionViewModel.startSession(
                                                mode         = "study",
                                                tierKey      = tier.tierKey,
                                                homeTierKey  = myTierKey,
                                                tierName     = tier.name,
                                                tierEmoji    = tier.iconEmoji,
                                                tierColorHex = tier.colorHex
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (state.myTierData?.currentTier != null) {
                            RoomChampionsCard(state.champions, Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(12.dp))

                        }

                        // Quick links — Leaderboard moved to top-right room
                        // actions (redesign section 2); no longer a 3rd
                        // cramped card here.
                        QuickActionRow(
                            onAchievements = { navController.navigate(Screen.Achievements.route) },
                            onChallenges = { navController.navigate(Screen.WeeklyChallenges.route) },
                        )

                        // ── Bottom banner ad ──────────────────────────────
                        if (adManager != null) {
                            com.example.bpscnotes.core.ads.BannerAdView(
                                adUnitId = adManager.getBannerAdUnitId()
                            )
                        } else {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
            } // PullToRefreshBox
        }

        // Locked room bottom sheet
        showLockedSheetForTier?.let { lockedTier ->
            LockedRoomSheet(
                tier = lockedTier,
                progressItems = state.myTierData?.progressItems ?: emptyList(),
                onDismiss = { showLockedSheetForTier = null }
            )
        }
    }
}
// ════════════════════════════════════════════════════════════
// HERO HEADER — kept deliberately compact (redesign section 1):
// title, tier, live status, online/studying counts only. Everything
// else (progress, personal stats, Room Insights, Champions, Activity
// Feed) now lives in normal scrollable cards in the LazyColumn below,
// not inside this gradient block.
// ════════════════════════════════════════════════════════════
@Composable
private fun RoomsHeroHeader(
    state: TierRoomsUiState,
    onBack: () -> Unit,
    onRoomInfo: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
    ) {
        Column(modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 46.dp, bottom = 16.dp)) {

            // Top bar: title + top-right room actions (redesign section 2)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(str.roomsGroupStudy,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Tri-state connection indicator (spec section 11) —
                    // reuses the same ChatConnectionStatus enum + localized
                    // strings ChatSheet already established for this socket.
                    val connectionStatus = when {
                        state.isSocketConnected  -> ChatConnectionStatus.LIVE
                        state.hasConnectedBefore -> ChatConnectionStatus.RECONNECTING
                        else                     -> ChatConnectionStatus.CONNECTING
                    }
                    val dotColor = when (connectionStatus) {
                        ChatConnectionStatus.LIVE         -> BpscColors.Success
                        ChatConnectionStatus.RECONNECTING -> Color(0xFFFFA726)
                        ChatConnectionStatus.CONNECTING   -> Color.Gray
                    }
                    Box(Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor))

                    RoomActionIcon(icon = Icons.Rounded.Info, contentDescription = "Room Info", onClick = onRoomInfo)
                    // Leaderboard moved inside the room itself
                    // (StudyFocusScreen's top bar) — it only makes sense
                    // once you've actually joined, per the redesign spec's
                    // "after joining a room" framing. Chat intentionally
                    // still not here either — only re-enable once
                    // reconnect/persistence/dedup/scroll are verified.
                }
            }

            Spacer(Modifier.height(14.dp))

            if (state.isLoadingMyTier) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.08f)))
                return@Column
            }

            // Compact room summary — the ONLY room content left in the
            // hero: name, tier emoji, live status (online/studying counts).
            if (myTier != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surface.copy(0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tierColor.copy(0.25f)),
                            contentAlignment = Alignment.Center
                        ) { Text(myTier.iconEmoji ?: "🏆", fontSize = 18.sp) }
                        Column {
                            Text(myTier.name ?: str.roomsTierSilver + " Room",
                                style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("👥 ${myTier.displayMembers}",
                                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f))
                                Text("🟢 ${myTier.displayActive} studying",
                                    style = MaterialTheme.typography.labelSmall, color = BpscColors.Success)
                                state.roomStats?.let {
                                    Text("📶 ${it.onlineMembers} online",
                                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA726))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BpscColors.CoinGold.copy(0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("🪙 ${coinsPerHrLabel(myTier.coinMultiplier)} coins/hr",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold,
                            fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    // Frosted-glass chip matching the hero's translucent surfaces — a tinted
    // vector glyph instead of an emoji so it renders identically on every OEM.
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.15f))
            .border(1.dp, Color.White.copy(0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

// Extracted from the old hero Card: coin multiplier progress + personal
// stats strip. Now a normal scrollable card (redesign section 1), not
// pinned inside the hero.
@Composable
private fun MyRoomProgressCard(state: TierRoomsUiState) {
    val myTier = state.myTierData?.currentTier ?: return
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Progress bar to next tier
            state.myTierData?.nextTier?.let { next ->
                val progress  = state.myTierData.nextTierProgress.toFloat()
                val animProg  by animateFloatAsState(progress, tween(900), label = "prog")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.1f))) {
                        Box(modifier = Modifier
                            .fillMaxWidth(animProg.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF64B5F6))),
                                RoundedCornerShape(3.dp)
                            ))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progress to ${next.iconEmoji ?: ""} ${next.name}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text("${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            // Stats strip
            state.myTierData?.userStats?.let { stats ->
                val str = LocalStrings.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPillLight("⏱️", "${stats.totalStudyHours.toInt()}h", str.roomsStudied)
                    StatPillLight("🔥", "${stats.streak}", "Streak")
                    StatPillLight("📝", "${stats.quizzesAttempted}", "Quizzes")
                    StatPillLight("🎯", "${stats.accuracy.toInt()}%", "Accuracy")
                }
            }
        }
    }
}

@Composable
private fun StatPillLight(icon: String, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 12.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 9.sp)
    }
}

// ℹ️ Room Info top-right action (redesign section 2) — tier description
// + perks, reusing fields RoomTierDto already carries rather than a new
// endpoint.
@OptIn(ExperimentalMaterial3Api::class)
/** Converts a tier coin multiplier to a user-readable "X coins/hr" string.
 *  BASE_COINS_PER_HOUR = 6 (matches the backend constant).
 *  Formats as integer when exact (e.g. 1.0 → "6"), one decimal otherwise (1.75 → "10.5").
 *  Internal so TierPromotionOverlay (same package) can reuse it. */
internal fun coinsPerHrLabel(multiplier: Double): String {
    val rate = 6.0 * multiplier
    return if (rate == kotlin.math.floor(rate)) rate.toLong().toString()
    else String.format("%.1f", rate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomInfoSheet(tier: RoomTierDto?, onDismiss: () -> Unit) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (tier == null) {
                Text(str.rhRoomInfoUnavail, style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tier.iconEmoji ?: "🏆", fontSize = 28.sp)
                    Column {
                        Text(tier.name ?: "Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("${tier.displayMembers} members", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!tier.description.isNullOrBlank()) {
                    Text(tier.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tier.perks.isNotEmpty()) {
                    Text(str.rhPerks, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    tier.perks.forEach { perk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("✓", color = BpscColors.Success, fontWeight = FontWeight.Bold)
                            Text(perk, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BpscColors.CoinGold.copy(0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("🪙 Earn ${coinsPerHrLabel(tier.coinMultiplier)} coins per hour (${tier.coinMultiplier}× base rate)",
                        style = MaterialTheme.typography.labelMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RoomInsightsCard(stats: RoomStatsResponseData?, modifier: Modifier = Modifier) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    if (stats == null) return
    val cs = MaterialTheme.colorScheme
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(0.4f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(str.rhRoomInsights, style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatPillLight("👥", "${stats.totalMembers}", "Members")
                StatPillLight("🟢", "${stats.studyingNow}", "Studying")
                StatPillLight("📶", "${stats.onlineMembers}", "Online")
                StatPillLight("🔥", "${stats.highestStreak}", "Top streak")
            }
            HorizontalDivider(color = cs.onSurface.copy(0.08f), thickness = 0.5.dp)
            val todayH = stats.minutesToday / 60.0
            val weekH  = stats.minutesThisWeek / 60.0
            val monthH = stats.minutesThisMonth / 60.0
            Text("Today: ${"%.1f".format(todayH)}h · Week: ${"%.1f".format(weekH)}h · Month: ${"%.1f".format(monthH)}h",
                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            stats.topPerformer?.let { top ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏆", fontSize = 13.sp)
                    Text("Top this week: ${top.userName} (${top.minutes / 60}h ${top.minutes % 60}m)",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Room Champions (redesign section 5) — Today/Weekly/Monthly champion +
// Most Improved, shown prominently per the spec ("Show these prominently").
@Composable
fun RoomChampionsCard(champions: RoomChampionsResponseData?, modifier: Modifier = Modifier) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    if (champions == null) return
    val cs = MaterialTheme.colorScheme
    val entries = listOfNotNull(
        champions.todayChampion?.let { Triple("🏆", "Today's Champion", it) },
        champions.weeklyChampion?.let { Triple("🔥", "Weekly Champion", it) },
        champions.monthlyChampion?.let { Triple("👑", "Monthly Champion", it) },
        champions.mostImproved?.let { Triple("⚡", "Most Improved", it) },
    )
    if (entries.isEmpty()) return
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(0.4f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(str.rhRoomChampions, style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            entries.forEach { (emoji, label, champion) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 16.sp)
                        Column {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            Text(champion.userName, style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                    val mins = champion.deltaMinutes ?: champion.minutes
                    val prefix = if (champion.deltaMinutes != null) "+" else ""
                    Text("$prefix${mins / 60}h ${mins % 60}m",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    // Dark-themed cards that match the navy/dark room background
    val bgColor = when {
        isClaimReady -> Color(0xFF1E4D35)            // soft green
        isMyRoom     -> Color(0xFF1A3460)            // medium navy highlight
        isLocked     -> Color(0xFF1C2540)            // muted navy
        isLowerTier  -> Color(0xFF151C30)            // darker muted — disabled past tier
        else         -> Color(0xFF1E2D52)            // clean navy card
    }
    val borderColor = when {
        isClaimReady -> BpscColors.Success
        isMyRoom     -> tierColor
        isLocked     -> Color.White.copy(0.08f)
        isLowerTier  -> Color.White.copy(0.05f)     // nearly invisible border
        else         -> Color.White.copy(0.12f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(enabled = !isLowerTier, onClick = onClick)
            .alpha(if (isLowerTier) 0.45f else 1f),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(if (isMyRoom) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(if (isLocked) Color.White.copy(0.06f) else tierColor.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isStarting   -> CircularProgressIndicator(color = tierColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    isClaimReady -> Text("🎉", fontSize = 24.sp)
                    isLocked     -> Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(22.dp))
                    isLowerTier  -> Text(tier.iconEmoji ?: "🏆", fontSize = 24.sp)
                    else         -> Text(tier.iconEmoji ?: "🏆", fontSize = 24.sp)
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tier.name ?: tier.tierKey.replaceFirstChar { it.uppercase() },
                        style      = MaterialTheme.typography.titleMedium,
                        color      = if (isLocked) Color.White.copy(0.4f) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    when {
                        isClaimReady -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.Success.copy(0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("🎉 " + str.roomsClaimNow, style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                        isMyRoom -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.Success.copy(0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(str.roomsYourRoom, style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                        isLocked -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(str.roomsLocked, style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFAAAAAA), fontSize = 9.sp)
                        }
                        isLowerTier -> Box(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.07f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(str.rhCompleted, style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.35f), fontSize = 9.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🪙 ${coinsPerHrLabel(tier.coinMultiplier)}/hr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLocked) Color(0xFFCCCCCC) else tierColor,
                        fontWeight = FontWeight.SemiBold)
                    Text("👥 ${tier.displayMembers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLocked) Color(0xFFCCCCCC) else BpscColors.TextHint)
                    // BUG FIX: Only show active count when > 0 and not locked.
                    // displayActive comes from activeSessions field which is updated via WebSocket.
                    // If it shows 1 just by opening the page, it means the server is counting
                    // socket connections (not sessions). We hide counts of 0 to avoid confusion.
                    if (tier.displayActive > 0 && !isLocked) {
                        Text("🟢 ${tier.displayActive} studying",
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                .background(cs.outline))

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
                    color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                Text(str.roomsChooseHint,
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            }

            // Requirements list
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(str.roomsRequirements, style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.Bold)
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
                                Text(perk, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
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
            ) { Text(str.roomsKeepStudying, fontWeight = FontWeight.Bold) }
        }
    }
}

// ════════════════════════════════════════════════════════════
// PROMOTION READY BANNER
// ════════════════════════════════════════════════════════════
@Composable
private fun PromotionReadyBanner(nextTierName: String, nextTierEmoji: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                Text("${str.roomsReadyForNext} $nextTierEmoji $nextTierName!", style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                Text(str.roomsPromotedMidnight,
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// PROGRESS BREAKDOWN CARD
// ════════════════════════════════════════════════════════════
@Composable
private fun ProgressBreakdownCard(tierData: MyTierResponseData) {
    val cs = MaterialTheme.colorScheme
    val next = tierData.nextTier ?: return
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Progress to ${next.iconEmoji ?: ""} ${next.name}",
                style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            tierData.progressItems.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (item.done) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(13.dp))
                            else Box(Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, cs.outline, CircleShape))
                            Text(item.label, style = MaterialTheme.typography.bodySmall, color = cs.onSurface)
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
                        .background(cs.outline)) {
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
    val cs = MaterialTheme.colorScheme
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
    val cs = MaterialTheme.colorScheme
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        border = BorderStroke(1.dp, color.copy(0.15f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}