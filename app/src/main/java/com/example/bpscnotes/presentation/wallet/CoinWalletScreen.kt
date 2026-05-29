package com.example.bpscnotes.presentation.wallet

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.mutableStateOf
import com.example.bpscnotes.data.remote.api.CheckInDayDto
import com.example.bpscnotes.data.remote.api.CoinTransactionDto
import com.example.bpscnotes.data.remote.api.EarnTaskDto
import com.example.bpscnotes.data.remote.api.mapIcon
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ads.WatchAdForCoinsCard
import com.example.bpscnotes.presentation.wallet.CoinWalletViewModel
import android.app.Activity
import android.util.Log

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

enum class TransactionType { EARNED, SPENT }
enum class CheckInStatus    { DONE, BONUS, TODAY, LOCKED }

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun CoinWalletScreen(
    navController: NavHostController,
    adManager:     AdManager,
    viewModel:     CoinWalletViewModel = hiltViewModel()
) {
    val context      = androidx.compose.ui.platform.LocalContext.current
    val activity     = context as? Activity
    val state        by viewModel.uiState.collectAsState()
    val adLoopActive: Boolean by adManager.adLoopActive.collectAsState(initial = false)

    // Block back button while ad loop is running — user must watch all ads
    BackHandler(enabled = adLoopActive) { /* swallow back press during ad loop */ }
    val str = LocalStrings.current
    val rewardedAdReady by adManager.rewardedReady.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Coins are now awarded server-side when actions complete (upload approved, quiz passed).
    // No client-side claim needed — getEarnTasks() already reflects the updated state.
    val tabs = listOf(str.walletEarnCoins, str.walletHistory)

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    if (state.isLoading && state.balance == 0) {
        Box(Modifier
            .fillMaxSize()
            .background(BpscColors.Surface), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.CoinGold)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        // FIX: Header and tabs pinned — only content area scrolls
        Column(Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)) {
            // Pinned header — does not scroll
            CoinHeroHeader(coins = state.balance, onBack = { navController.popBackStack() })
            // Pinned tab row — does not scroll
            CoinTabRow(selectedTab = selectedTab, tabs = tabs, onSelect = { selectedTab = it })

            // Scrollable content area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BpscColors.Surface),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                when (selectedTab) {

                    // ─── EARN TAB ─────────────────────────────────────
                    0 -> {
                        // Daily streak layout removed per client request
                        // ── Watch Ad to earn coins ─────────────────────
                        item(key = "watch_ad_card") {
                            WatchAdForCoinsCard(
                                adManager        = adManager,
                                coinsPerAd       = state.adCoinsPerAd,
                                minAdsPerSession = state.minAdsPerSession,
                                isAdReady        = rewardedAdReady,
                                watchedCount     = state.adsWatchedToday,
                                onWatchAd        = {
                                    activity?.let { act ->
                                        adManager.showRewardedAdLoop(
                                            activity      = act,
                                            count         = state.minAdsPerSession,
                                            coinsPerAd    = state.adCoinsPerAd,
                                            // Called ONCE after ALL ads complete — not per-ad
                                            onAllComplete = { totalCoins ->
                                                viewModel.onAdRewardEarned(totalCoins)
                                            },
                                            onFailed      = { reason ->
                                                viewModel.showMessage(reason)
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item { SectionHeader(str.walletEarnCoins, str.walletEarnCoins) }
                        state.earnTasks.forEachIndexed { idx, task ->
                            item(key = task.id) {
                                Log.e("TASK_ACTION", task.action)
                                EarnTaskRow(
                                    index    = idx + 1,
                                    task     = task,
                                    isClaiming = state.claimingTaskId == task.id,
                                    onClick  = {
                                        if (task.isCompleted || state.claimingTaskId != null) return@EarnTaskRow
                                        // FIX: Navigate to the actual task instead of claiming coins directly.
                                        // Coins are awarded only after completing the real task.
                                        when (task.action.lowercase().trim()) {

                                            // Daily Quiz
                                            "quiz_attempt" -> {
                                                val today = java.text.SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    java.util.Locale.getDefault()
                                                ).format(java.util.Date())

                                                navController.navigate(
                                                    Screen.DailyQuiz.createRoute(today)
                                                )
                                            }

                                            // Study Session
                                            "study_session" -> {
                                                navController.navigate(
                                                    Screen.StudyMaterials.route
                                                )
                                            }

                                            // Upload Notes
                                            "material_upload" -> {
                                                navController.navigate(
                                                    Screen.StudyMaterials.route
                                                )
                                            }

                                            // Referral
                                            "referral" -> {
                                                val code = viewModel.getReferralCode()

                                                val msg = """
            Join BPSCNotes and ace your BPSC exam!
            Use my referral code: $code
            
            https://play.google.com/store/apps/details?id=com.example.bpscnotes
        """.trimIndent()

                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_SEND
                                                ).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, msg)
                                                }

                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        intent,
                                                        str.walletInviteFriend
                                                    )
                                                )
                                            }

                                            // Watch Ad
                                            "ad_watch" -> {
                                                viewModel.showMessage(
                                                    str.walletWatchAd
                                                )
                                            }

                                            else -> {
                                                Log.e("UNKNOWN_ACTION", task.action)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (state.earnTasks.isEmpty() && !state.isLoading) {
                            item {
                                Box(Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp), Alignment.Center) {
                                    Text(str.walletNoTasks, color = BpscColors.TextSecondary)
                                }
                            }
                        }
                    }

                    // ─── HISTORY TAB ──────────────────────────────────
                    1 -> {
                        item {
                            HistorySummaryRow(
                                totalEarned = state.totalEarned,
                                totalSpent = state.totalSpent
                            )
                        }
                        item {
                            SectionHeader(
                                str.walletHistory,
                                "${state.transactions.size} transactions"
                            )
                        }
                        items(state.transactions, key = { it.id }) { txn ->
                            TransactionRow(
                                transaction = txn
                            )
                        }
                        if (state.transactions.isEmpty() && !state.isLoading) {
                            item {
                                EmptyHistoryState()
                            }
                        }
                        if (state.hasMoreTransactions && state.transactions.isNotEmpty()) {
                            item(key = "load_more_txn") {
                                LaunchedEffect(Unit) { viewModel.loadMoreTransactions() }
                                Box(Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), Alignment.Center) {
                                    if (state.isLoadingTransactions) {
                                        CircularProgressIndicator(
                                            color = BpscColors.CoinGold,
                                            modifier = Modifier.size(24.dp)
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
}

// ─────────────────────────────────────────────────────────────
// HERO HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun CoinHeroHeader(coins: Int, onBack: () -> Unit) {
    val str = LocalStrings.current
    val animCoins by animateFloatAsState(targetValue = coins.toFloat(), animationSpec = tween(1400), label = "coinAnim")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFAC84A),
                        Color(0xFFF0A500),
                        Color(0xFFE59400)
                    )
                )
            )
        //  .statusBarsPadding()
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.12f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
            drawCircle(Color.White.copy(0.08f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.75f))
            drawCircle(Color.White.copy(0.07f), 100.dp.toPx(), Offset(size.width/2, size.height*0.52f), style = Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(0.05f), 130.dp.toPx(), Offset(size.width/2, size.height*0.52f), style = Stroke(1.dp.toPx()))
            val dotSpacing = 28.dp.toPx()
            var x = dotSpacing
            while (x < size.width) { var y = dotSpacing; while (y < size.height) { drawCircle(Color.White.copy(0.07f), 1.dp.toPx(), Offset(x, y)); y += dotSpacing }; x += dotSpacing }
        }

        // Back button
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 46.dp, bottom = 16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.2f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        // Coin icon + balance
        Column(
            modifier            = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(str.walletTitle, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(0.85f))
            // Coin medallion
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.25f))
                    .border(2.dp, Color.White.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🏛️", fontSize = 32.sp)
            }
            Text(
                "${animCoins.toInt()} Coins",
                style      = MaterialTheme.typography.headlineLarge,
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun CoinTabRow(selectedTab: Int, tabs: List<String>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) BpscColors.CoinGold else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = if (selected) Color.White else BpscColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────
// DAILY CHECK-IN CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun DailyCheckInCard(
    days: List<CheckInDayDto>,
    onCheckIn: () -> Unit,
    isLoading: Boolean,
    doneToday: Boolean,
    streak: Int = 0
) {
    val str = LocalStrings.current
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(str.walletDailyStreak, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                if (streak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔥", fontSize = 14.sp)
                        Text("$streak days", style = MaterialTheme.typography.titleSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Day circles
            if (days.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    days.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!day.bonusLabel.isNullOrEmpty()) {
                                Text(day.bonusLabel ?: "", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 7.sp)
                            } else {
                                Spacer(Modifier.height(12.dp))
                            }
                            val status = when {
                                day.isToday -> CheckInStatus.TODAY
                                day.isDone  -> CheckInStatus.DONE
                                day.isBonus -> CheckInStatus.BONUS
                                else        -> CheckInStatus.LOCKED
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            CheckInStatus.DONE, CheckInStatus.BONUS -> BpscColors.CoinGold
                                            else -> Color(0xFFF5F5F5)
                                        }
                                    )
                                    .border(
                                        width = if (status == CheckInStatus.TODAY) 2.dp else 0.dp,
                                        color = if (status == CheckInStatus.TODAY) BpscColors.CoinGold else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (status) {
                                    CheckInStatus.DONE, CheckInStatus.BONUS ->
                                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    CheckInStatus.TODAY ->
                                        Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = BpscColors.CoinGold, modifier = Modifier.size(20.dp))
                                    CheckInStatus.LOCKED ->
                                        Icon(Icons.Rounded.Lock, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                day.label.orEmpty(),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = when (status) {
                                    CheckInStatus.DONE, CheckInStatus.BONUS -> BpscColors.TextPrimary
                                    CheckInStatus.TODAY  -> BpscColors.CoinGold
                                    CheckInStatus.LOCKED -> Color(0xFFBDBDBD)
                                },
                                fontWeight = if (status == CheckInStatus.TODAY) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 9.sp
                            )
                        }
                    }
                }
            }

            // Check-In button
            Button(
                onClick  = onCheckIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                enabled  = !doneToday && !isLoading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = BpscColors.CoinGold,
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor   = Color(0xFF9E9E9E)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(str.walletCheckingIn, style = MaterialTheme.typography.titleMedium)
                } else if (doneToday) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.walletCheckedIn, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.walletCheckIn, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EARN TASK ROW — redesigned action button (not a blue blob)
// ─────────────────────────────────────────────────────────────

@Composable
private fun EarnTaskRow(
    index: Int,
    task: EarnTaskDto,
    isClaiming: Boolean,
    onClick: () -> Unit
) {
    val str = LocalStrings.current
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color(0xFFF9FFF9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Task number badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) Color(0xFF4CAF50).copy(0.15f)
                        else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = if (task.isCompleted) Color(0xFF4CAF50) else BpscColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 11.sp
                )
            }

            // Icon circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (task.isCompleted) Color(0xFF4CAF50).copy(0.12f) else task.iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                } else {
                    Icon(mapIcon(task.icon), null, tint = task.iconTint, modifier = Modifier.size(22.dp))
                }
            }

            // Text column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        task.title.orEmpty(),
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = if (task.isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    if (task.isAd) {
                        Text(
                            "AD",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFF1565C0),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE3F2FD))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(task.subtitle.orEmpty(), style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, maxLines = 1)
                // Coin reward pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.CoinGold.copy(0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("🪙", fontSize = 10.sp)
                    Log.e("TAG", "EarnTaskRow:${task.coinsReward} ", )
                    Text(
                        "+${task.coinsReward} coins",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = BpscColors.CoinGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 10.sp
                    )
                }
            }

            // ── Action button — redesigned ────────────────────
            // Was: a big filled Circle (blue blob) — totally unclear
            // Now: clear pill button with label text OR "Done ✓" when complete
            if (task.isCompleted) {
                // Completed state — green chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF4CAF50).copy(0.12f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Text(str.done, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold)
                }
            } else {
                // Active state — solid colored pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isClaiming) task.actionBg.copy(alpha = 0.5f)
                            else task.actionBg
                        )
                        .clickable(enabled = !isClaiming, onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            color        = task.actionTextColor,
                            modifier     = Modifier.size(14.dp),
                            strokeWidth  = 2.dp
                        )
                    } else {
                        Text(
                            task.actionLabel.orEmpty(),
                            style      = MaterialTheme.typography.labelMedium,
                            color      = task.actionTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HISTORY SUMMARY ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun HistorySummaryRow(totalEarned: Int, totalSpent: Int) {
    val str = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFE8FDF4)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.ArrowUpward, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Text(str.walletEarned, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Text("+$totalEarned", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)
                Text(str.coins, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.ArrowDownward, null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                    Text(str.walletSpent, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Text("-$totalSpent", style = MaterialTheme.typography.titleLarge, color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
                Text(str.coins, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EMPTY HISTORY STATE
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState() {
    val str = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BpscColors.CoinGold.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🪙", fontSize = 36.sp)
        }
        Text(
            str.walletNoTransactions,
            style      = MaterialTheme.typography.titleMedium,
            color      = BpscColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Complete check-in, quizzes or study\nsessions to start earning coins!",
            style     = MaterialTheme.typography.bodyMedium,
            color     = BpscColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun TransactionRow(transaction: CoinTransactionDto) {
    val isEarned = transaction.type == "earned"
    val iconBg   = if (isEarned) Color(0xFFE8FDF4) else Color(0xFFFCE4EC)
    val iconTint = if (isEarned) Color(0xFF2E7D32) else Color(0xFFC62828)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(mapIcon(transaction.icon.ifBlank { transaction.action }), null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(transaction.title.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(transaction.subtitle.orEmpty(), style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatDate(transaction.displayDate.orEmpty()), style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🪙", fontSize = 11.sp)
                Text(
                    if (isEarned) "+${transaction.displayAmount}" else "-${kotlin.math.abs(transaction.displayAmount)}",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = if (isEarned) Color(0xFF2E7D32) else Color(0xFFE53935),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp
                )
            }
            Text("coins", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BpscColors.Divider, thickness = 0.5.dp)
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────

private fun formatDate(iso: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(iso) ?: return iso
        java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(date)
    } catch (e: Exception) { iso.take(10) }
}

private fun Int.formatWithComma(): String = "%,d".format(this)