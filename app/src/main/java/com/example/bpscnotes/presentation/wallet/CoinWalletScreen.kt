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
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
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
import androidx.compose.ui.platform.LocalContext

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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context      = androidx.compose.ui.platform.LocalContext.current
    val activity     = context as? Activity
    val state        by viewModel.uiState.collectAsState()
    val adLoopActive: Boolean by adManager.adLoopActive.collectAsState(initial = false)

    // Block back button while ad loop is running — user must watch all ads
    BackHandler(enabled = adLoopActive) { /* swallow back press during ad loop */ }
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("coin_wallet") }
    val rewardedAdReady by adManager.rewardedReady.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 4 tabs: Earn Coins | Coin History | ₹ Earnings | Referrals
    val tabs = listOf(str.walletEarnCoins, "Coin History", "₹ Earnings", "Referrals")

    // Withdrawal dialog state
    var showWithdrawDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.withdrawalSuccess) {
        state.withdrawalSuccess?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearWithdrawalSuccess()
        }
    }

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

    if (state.isLoading && state.balance == 0) { AppLoader(); return }

    // Admin master switch (Coins page → Economy) — when off, no coins are
    // earned or spent anywhere in the app. Show a clear paused state
    // instead of the normal earn/spend tabs (balance stays visible so
    // users can see what they already have).
    val coinsEnabled = viewModel.coinsConfig.config.collectAsState().value.enabled
    if (!coinsEnabled) {
        Column(Modifier.fillMaxSize().background(cs.background)) {
            CoinHeroHeader(coins = state.balance, sellerWallet = state.sellerWallet, isLoadingWallet = state.isLoadingSellerWallet, onBack = { navController.popBackStackSafe() })
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⏸️", fontSize = 48.sp)
                    Text("Coin Rewards Paused", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Text(
                        "Earning and spending coins is temporarily turned off. Your existing balance is safe and will be usable again once it's back on.",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        // FIX: Header and tabs pinned — only content area scrolls
        Column(Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)) {
            // Pinned header — does not scroll
            CoinHeroHeader(coins = state.balance, sellerWallet = state.sellerWallet, isLoadingWallet = state.isLoadingSellerWallet, onBack = { navController.popBackStackSafe() })
            // Pinned tab row — does not scroll
            CoinTabRow(selectedTab = selectedTab, tabs = tabs, onSelect = { selectedTab = it })

            // Scrollable content area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(cs.background),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                when (selectedTab) {

                    // ─── EARN TAB ─────────────────────────────────────
                    0 -> {
                        // Coin Store hidden for this phase — re-enable when store is ready

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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        item { SectionHeader(str.walletEarnCoins, "Complete tasks to earn coins") }
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
                                            "daily_quiz" -> {
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
                                            "referral_signup" -> {
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
                                    Text(str.walletNoTasks, color = cs.onSurfaceVariant)
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

                    // ─── ₹ EARNINGS TAB ──────────────────────────────────
                    2 -> {
                        // Earnings summary card
                        item(key = "earnings_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.isLoadingSellerWallet) {
                                    AppLoader(modifier = Modifier.fillMaxWidth().height(140.dp))
                                } else {
                                    val wallet = state.sellerWallet
                                    // Balance card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF047857))))
                                            .padding(20.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Available to Withdraw", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.8f))
                                            Text("₹${wallet?.balance ?: 0}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            HorizontalDivider(color = Color.White.copy(0.25f))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text("Total earned", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                                    Text("₹${wallet?.totalEarned ?: 0}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Button(
                                                    onClick = { showWithdrawDialog = true },
                                                    enabled = (wallet?.balance ?: 0) >= 100,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.White,
                                                        contentColor   = Color(0xFF047857),
                                                        disabledContainerColor = Color.White.copy(0.3f),
                                                        disabledContentColor   = Color.White.copy(0.6f)
                                                    )
                                                ) {
                                                    Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Withdraw", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if ((wallet?.balance ?: 0) < 100) {
                                                Text("Minimum withdrawal is ₹100. Keep selling to unlock!", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                            }
                                        }
                                    }
                                    // Info note
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF0FDF4)).padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ℹ️", fontSize = 14.sp)
                                        Text("Withdrawals are processed to your UPI ID within 2–3 business days. Platform takes a 15% fee on each sale.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                                    }
                                }
                            }
                        }

                        // Transaction history
                        val txns = state.sellerWallet?.transactions ?: emptyList()
                        if (txns.isEmpty() && !state.isLoadingSellerWallet) {
                            item(key = "earnings_empty") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("💰", fontSize = 40.sp)
                                        Text("No transactions yet", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Sell study materials to earn real money", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                    }
                                }
                            }
                        } else {
                            item(key = "earnings_hist_hdr") {
                                SectionHeader("Transaction History", "${txns.size} transactions")
                            }
                            items(txns, key = { it.id }) { txn ->
                                val isCredit = txn.type == "sale_credit"
                                val isWithdrawal = txn.type == "withdrawal"
                                val statusEmoji = when(txn.status) { "disbursed" -> "✅"; "pending" -> "⏳"; "failed" -> "❌"; else -> "•" }
                                val statusColor = when(txn.status) { "disbursed" -> BpscColors.Success; "pending" -> Color(0xFFF59E0B); "failed" -> Color(0xFFE74C3C); else -> BpscColors.TextHint }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cs.surface)
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(38.dp).clip(CircleShape).background(if (isCredit) BpscColors.Success.copy(0.12f) else if (isWithdrawal) Color(0xFF3B82F6).copy(0.12f) else Color(0xFFE74C3C).copy(0.12f)), Alignment.Center) {
                                        Text(if (isCredit) "📈" else if (isWithdrawal) "🏦" else statusEmoji, fontSize = 16.sp)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(txn.materialTitle ?: txn.description ?: if (isCredit) "Sale credit" else if (isWithdrawal) "Withdrawal" else txn.type,
                                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(txn.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                                            txn.createdAt?.take(10)?.let { Text("· $it", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint) }
                                        }
                                    }
                                    Text("${if (isCredit) "+" else "−"}₹${txn.amount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (isCredit) BpscColors.Success else Color(0xFFE74C3C))
                                }
                            }
                        }
                    }

                    // ─── REFERRALS TAB ────────────────────────────────
                    3 -> {
                        // Referral coin amounts are admin-configurable on
                        // the Coins page (Referrals & Social) — read live
                        // values instead of hardcoding them here.
                        val refSignup     = viewModel.coinsConfig.coins("referral_signup", 50)
                        val refEngagement = viewModel.coinsConfig.coins("referral_engagement", 50)
                        val refActive     = viewModel.coinsConfig.coins("referral_active", 50)
                        val refJoined     = viewModel.coinsConfig.coins("referral_joined", 25)
                        val refTotal      = refSignup + refEngagement + refActive
                        item {
                            ReferralHeader(
                                code         = viewModel.getReferralCode(),
                                totalEarned  = state.referralStats?.totalEarned ?: 0,
                                totalFriends = state.referralStats?.totalReferrals ?: 0,
                                totalPerFriend = refTotal,
                                isLoading    = state.isLoadingReferrals,
                                onShare      = {
                                    val code = viewModel.getReferralCode()
                                    val msg = "Join BPSCNotes and ace your BPSC exam! 🎯\n\nUse my referral code: $code and earn $refJoined bonus coins when you sign up.\n\nhttps://play.google.com/store/apps/details?id=com.example.bpscnotes"
                                    context.startActivity(android.content.Intent.createChooser(
                                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, msg)
                                        }, "Invite a friend"
                                    ))
                                }
                            )
                        }
                        item {
                            MilestoneExplainer(signup = refSignup, engagement = refEngagement, active = refActive, joined = refJoined)
                        }
                        val referees = state.referralStats?.referees ?: emptyList()
                        if (referees.isEmpty() && !state.isLoadingReferrals) {
                            item { ReferralEmptyState(totalPerFriend = refTotal, onShare = {
                                val code = viewModel.getReferralCode()
                                val msg = "Join BPSCNotes! Use code: $code\nhttps://play.google.com/store/apps/details?id=com.example.bpscnotes"
                                context.startActivity(android.content.Intent.createChooser(
                                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, msg)
                                    }, "Invite a friend"
                                ))
                            }) }
                        } else {
                            item {
                                Text("Your Referrals (${referees.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                            items(referees, key = { it.id }) { referee ->
                                RefereeCard(referee = referee)
                            }
                        }
                    }
                }
            }
        }
    }

    // Withdrawal dialog — shown over the Scaffold
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentBalance = state.sellerWallet?.balance ?: 0,
            isWithdrawing  = state.isWithdrawing,
            onDismiss      = { showWithdrawDialog = false },
            onConfirm      = { amount, upiId ->
                showWithdrawDialog = false
                viewModel.requestWithdrawal(amount, upiId)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// HERO HEADER
// ─────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────
// WALLET HERO — dual card: Coins (left) + ₹ Real Wallet (right)
// ─────────────────────────────────────────────────────────────
@Composable
private fun CoinHeroHeader(
    coins:        Int,
    sellerWallet: com.example.bpscnotes.data.remote.api.WalletData?,
    isLoadingWallet: Boolean,
    onBack:       () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val animCoins by animateFloatAsState(targetValue = coins.toFloat(), animationSpec = tween(1200), label = "coinAnim")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A2C5B), Color(0xFF0D47A1))))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Decorative circles
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.05f), 200.dp.toPx(), Offset(size.width + 40.dp.toPx(), 0f))
            drawCircle(Color.White.copy(0.04f), 120.dp.toPx(), Offset(-30.dp.toPx(), size.height))
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top row: back button + title
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text("My Wallets", style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.size(36.dp)) // balance the back button
            }

            // Dual wallet cards
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Coins card ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🪙", fontSize = 16.sp)
                            Text("Coins", style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(0.9f), fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "${animCoins.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White, fontWeight = FontWeight.ExtraBold
                        )
                        HorizontalDivider(color = Color.White.copy(0.2f), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            WalletMiniStat("Earned", "+$coins")
                        }
                    }
                }

                // ── Real money card ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF047857))))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(16.dp))
                            Text("₹ Wallet", style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(0.9f), fontWeight = FontWeight.SemiBold)
                        }
                        if (isLoadingWallet) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "₹${sellerWallet?.balance ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White, fontWeight = FontWeight.ExtraBold
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(0.2f), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            WalletMiniStat("Earned", "₹${sellerWallet?.totalEarned ?: 0}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletMiniStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
// ─────────────────────────────────────────────────────────────
// TAB ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun CoinTabRow(selectedTab: Int, tabs: List<String>, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surface)
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
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
                Text(str.walletDailyStreak, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.Bold)
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
    val cs = MaterialTheme.colorScheme
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
                Text(task.subtitle.orEmpty(), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
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
    val cs = MaterialTheme.colorScheme
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
                Text(str.coins, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
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
                Text(str.coins, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EMPTY HISTORY STATE
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState() {
    val cs = MaterialTheme.colorScheme
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
                color = cs.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(transaction.subtitle.orEmpty(), style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text(str.walletCoins, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outline, thickness = 0.5.dp)
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

// ─────────────────────────────────────────────────────────────
// REFERRAL UI COMPONENTS
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReferralHeader(
    code:         String,
    totalEarned:  Int,
    totalFriends: Int,
    totalPerFriend: Int,
    isLoading:    Boolean,
    onShare:      () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var codeCopied by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                    Text("🎁", fontSize = 22.sp)
                }
                Column {
                    Text("Invite Friends, Earn Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    Text("Up to $totalPerFriend coins per friend", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                }
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ReferralStatChip("👥", "$totalFriends", "Invited")
                ReferralStatChip("🪙", "$totalEarned", "Earned")
                ReferralStatChip("✨", "${totalFriends * totalPerFriend}", "Potential")
            }

            // Code box
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Your Referral Code", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = BpscColors.Primary, letterSpacing = 2.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Copy
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (codeCopied) BpscColors.Success.copy(0.15f) else BpscColors.PrimaryLight)
                        .clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Referral code", code))
                            codeCopied = true
                        }, contentAlignment = Alignment.Center) {
                        Icon(if (codeCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy, null,
                            tint = if (codeCopied) BpscColors.Success else BpscColors.Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Share button
            Button(
                onClick  = onShare,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Invite a Friend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReferralStatChip(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BpscColors.Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
    }
}

@Composable
private fun MilestoneExplainer(signup: Int, engagement: Int, active: Int, joined: Int) {
    val milestones = listOf(
        Triple("✍️", "Friend Signs Up", "$signup 🪙"),
        Triple("📚", "Friend Enrolls / Uploads", "$engagement 🪙"),
        Triple("🏆", "Friend Completes 5 Quizzes", "$active 🪙"),
    )
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How It Works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            milestones.forEachIndexed { i, (emoji, label, coins) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 14.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Step ${i + 1}: $label", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                    }
                    Text(coins, style = MaterialTheme.typography.labelMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                }
                if (i < milestones.size - 1) {
                    Box(Modifier.padding(start = 16.dp).width(1.dp).height(12.dp).background(BpscColors.Divider))
                }
            }
            HorizontalDivider(color = BpscColors.Divider)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = BpscColors.CoinGold, modifier = Modifier.size(16.dp))
                Text("Your friend also gets $joined 🪙 for joining!", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun RefereeCard(referee: com.example.bpscnotes.data.remote.api.RefereeDto) {
    val cs = MaterialTheme.colorScheme
    val m  = referee.milestones
    val completedCount = listOf(m.signup.earned, m.engagement.earned, m.active.earned).count { it }
    val progress = completedCount / 3f

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                        Text(referee.name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(referee.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text(referee.joinedAt.take(10), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    }
                }
                // Total coins earned from this referee
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("🪙", fontSize = 11.sp)
                    Text("+${referee.totalCoinsEarned}", style = MaterialTheme.typography.labelMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text("$completedCount / 3 milestones", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                }
                val animProgress by androidx.compose.animation.core.animateFloatAsState(progress, androidx.compose.animation.core.tween(800), label = "ref_prog")
                LinearProgressIndicator(
                    progress    = { animProgress },
                    modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color       = if (completedCount == 3) BpscColors.Success else BpscColors.Primary,
                    trackColor  = BpscColors.Divider
                )
            }

            // Milestone chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MilestoneChip("Signed Up",   m.signup.earned,     m.signup.coins)
                MilestoneChip("Enrolled",    m.engagement.earned, m.engagement.coins)
                MilestoneChip("5 Quizzes",   m.active.earned,     m.active.coins)
            }
        }
    }
}

@Composable
private fun RowScope.MilestoneChip(label: String, earned: Boolean, coins: Int) {
    Row(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
            .background(if (earned) Color(0xFFE8FDF4) else Color(0xFFF5F5F5))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (earned) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null, tint = if (earned) BpscColors.Success else BpscColors.TextHint, modifier = Modifier.size(12.dp)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (earned) Color(0xFF1A7A4A) else BpscColors.TextHint, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1)
            if (earned && coins > 0) Text("+${coins}🪙", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ReferralEmptyState(totalPerFriend: Int, onShare: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("👋", fontSize = 48.sp)
            Text("No referrals yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Text("Share your code and earn up to $totalPerFriend coins for every friend who joins and stays active!",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onShare, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Invite Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────
// WITHDRAW DIALOG — UPI ID + amount input → requestWithdrawal()
// ─────────────────────────────────────────────────────────────
@Composable
private fun WithdrawDialog(
    currentBalance: Int,
    isWithdrawing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, upiId: String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var amountText by remember { mutableStateOf("") }
    var upiId      by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isWithdrawing) onDismiss() },
        shape            = RoundedCornerShape(20.dp),
        containerColor   = cs.surface,
        icon = {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF059669).copy(0.12f)), Alignment.Center) {
                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Withdraw Earnings", fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge)
                Text("Available: ₹$currentBalance",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Amount field
                OutlinedTextField(
                    value         = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() }; error = null },
                    label         = { Text("Amount (₹)") },
                    placeholder   = { Text("Min ₹100") },
                    prefix        = { Text("₹") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                // UPI ID field
                OutlinedTextField(
                    value         = upiId,
                    onValueChange = { upiId = it; error = null },
                    label         = { Text("UPI ID (optional)") },
                    placeholder   = { Text("e.g. name@upi") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = cs.error)
                }
                // Info note
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0FDF4)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ℹ️", fontSize = 12.sp)
                    Text("Processed within 2–3 business days. 15% platform fee already deducted from earnings.",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    when {
                        amount < 100 -> error = "Minimum withdrawal is ₹100"
                        amount > currentBalance -> error = "Exceeds available balance (₹$currentBalance)"
                        else -> onConfirm(amount, upiId.trim().ifBlank { null })
                    }
                },
                enabled  = !isWithdrawing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                if (isWithdrawing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Request Withdrawal", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWithdrawing) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        }
    )
}