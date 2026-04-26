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
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.mock.MockData
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.example.bpscnotes.data.remote.api.CheckInDayDto
import com.example.bpscnotes.data.remote.api.EarnTaskDto
import com.example.bpscnotes.data.remote.api.mapIcon
import com.example.bpscnotes.presentation.wallet.CoinWalletViewModel

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────

enum class TransactionType { EARNED, SPENT }

enum class CheckInStatus { DONE, BONUS, TODAY, LOCKED }

data class CheckInDay(
    val label: String,
    val status: CheckInStatus,
    val bonusLabel: String = ""   // e.g. "+5 Gold" shown below circle
)

data class EarnTask(
    val id: String,
    val title: String,
    val subtitle: String,
    val coinsReward: Int,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val actionLabel: String,
    val actionBg: Color,
    val actionTextColor: Color,
    val isCompleted: Boolean = false,
    val isAd: Boolean = false       // ad tasks show a short timer / ad badge
)

data class CoinTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val coins: Int,
    val type: TransactionType,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val timeLabel: String
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun CoinWalletScreen(
    navController: NavHostController,
    viewModel: CoinWalletViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Earn Coins", "History")

    // ✅ Loading
    if (state.isLoading && state.balance == 0) {
        Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.CoinGold)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BpscColors.Surface),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        // ✅ HEADER (API BALANCE)
        item {
            CoinHeroHeader(
                coins = state.balance,
                onBack = { navController.popBackStack() }
            )
        }

        item {
            CoinTabRow(
                selectedTab = selectedTab,
                tabs = tabs,
                onSelect = { selectedTab = it }
            )
        }

        when (selectedTab) {

            // ───── EARN TAB ─────
            0 -> {

                item {
                    SectionHeader(
                        "Daily Check-in",
                        "Login daily to maintain streak"
                    )
                }

                item {
                    DailyCheckInCard(
                        days = state.checkInDays,
                        onCheckIn = { viewModel.checkIn() },
                        isLoading = state.isCheckingIn,
                        doneToday = state.checkedInToday
                    )
                }

                item {
                    SectionHeader("Earn Coins", null)
                }

                items(state.earnTasks, key = { it.id }) { task ->
                    EarnTaskRow(
                        index = 0,
                        task = task,
                        onClick = { viewModel.claimTask(task.id) }
                    )
                }

                if (state.earnTasks.isEmpty() && !state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No tasks available", color = BpscColors.TextSecondary)
                        }
                    }
                }
            }

            // ───── HISTORY TAB ─────
            1 -> {


                item {
                    SectionHeader("Transaction History", null)
                }



                if (state.transactions.isEmpty() && !state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions yet")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HERO HEADER  — gold gradient matching screenshot exactly
// ─────────────────────────────────────────────────────────────

@Composable
private fun CoinHeroHeader(coins: Int, onBack: () -> Unit) {
    val animCoins by animateFloatAsState(
        targetValue   = coins.toFloat(),
        animationSpec = tween(1400),
        label         = "coinAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAC84A),   // bright top gold
                        Color(0xFFF0A500),   // mid amber
                        Color(0xFFE59400),   // warm base
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Decorative blobs — same as Dashboard/Profile/JobVacancies
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.12f),
                radius = 160.dp.toPx(),
                center = Offset(size.width + 20.dp.toPx(), -40.dp.toPx())
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.08f),
                radius = 80.dp.toPx(),
                center = Offset(-20.dp.toPx(), size.height * 0.75f)
            )
            // Concentric coin rings
            drawCircle(
                color  = Color.White.copy(alpha = 0.07f),
                radius = 100.dp.toPx(),
                center = Offset(size.width / 2, size.height * 0.52f),
                style  = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.05f),
                radius = 130.dp.toPx(),
                center = Offset(size.width / 2, size.height * 0.52f),
                style  = Stroke(width = 1.dp.toPx())
            )
            // Dot grid
            val dotSpacing = 28.dp.toPx()
            var x = dotSpacing
            while (x < size.width) {
                var y = dotSpacing
                while (y < size.height) {
                    drawCircle(Color.White.copy(alpha = 0.07f), 1.dp.toPx(), Offset(x, y))
                    y += dotSpacing
                }
                x += dotSpacing
            }
        }

        // Shiny accent line at top — same as all other headers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(0.4f), Color.White.copy(0.7f), Color.White.copy(0.4f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back + title row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(0.5.dp, Color.White.copy(0.3f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Coin Wallet",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                // Balance button top-right (matches screenshot)
                Spacer(Modifier.size(38.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Big coin icon — exactly like screenshot (rupee circle)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFEE58), Color(0xFFFFB300))
                        )
                    )
                    .border(3.dp, Color.White.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🪙", fontSize = 34.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Coin count — big number like screenshot
            Text(
                text       = "${animCoins.toInt().formatWithComma()} Coins",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(14.dp))

            // Redeem Now button — matches screenshot exactly
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp))
                    .clickable { /* navigate to redeem */ }
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Redeem Now",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun CoinTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onSelect: (Int) -> Unit
) {
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
        Text(
            title,
            style      = MaterialTheme.typography.titleLarge,
            color      = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DAILY CHECK-IN CARD  — matches screenshot exactly
// ─────────────────────────────────────────────────────────────

@Composable
private fun DailyCheckInCard(
    days: List<CheckInDayDto>,
    onCheckIn: () -> Unit,
    isLoading: Boolean,
    doneToday: Boolean
) {
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
                .padding(16.dp)
        ) {
            // Day circles row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                days.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Bonus label above circle
                        if (day.bonusLabel.isNotEmpty()) {
                            Text(
                                day.bonusLabel,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = BpscColors.CoinGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 8.sp
                            )
                        } else {
                            Spacer(Modifier.height(14.dp)) // keep alignment
                        }
                        val status = when {
                            day.isToday -> CheckInStatus.TODAY
                            day.isDone -> CheckInStatus.DONE
                            day.isBonus -> CheckInStatus.BONUS
                            else -> CheckInStatus.LOCKED
                        }
                        // Circle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when (status) {
                                        CheckInStatus.DONE   -> BpscColors.CoinGold
                                        CheckInStatus.BONUS  -> BpscColors.CoinGold
                                        CheckInStatus.TODAY  -> BpscColors.Surface
                                        CheckInStatus.LOCKED -> BpscColors.Surface
                                    }
                                )
                                .border(
                                    width  = if (status == CheckInStatus.TODAY) 2.dp else 0.dp,
                                    color  = if (status == CheckInStatus.TODAY) BpscColors.CoinGold else Color.Transparent,
                                    shape  = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (status) {
                                CheckInStatus.DONE, CheckInStatus.BONUS -> {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                CheckInStatus.TODAY -> {
                                    Icon(
                                        Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint     = BpscColors.CoinGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                CheckInStatus.LOCKED -> {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint     = BpscColors.TextHint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Day label below
                        Text(
                            day.label,
                            style  = MaterialTheme.typography.labelSmall,
                            color  = when (status) {
                                CheckInStatus.DONE, CheckInStatus.BONUS -> BpscColors.TextPrimary
                                CheckInStatus.TODAY  -> BpscColors.CoinGold
                                CheckInStatus.LOCKED -> BpscColors.TextHint
                            },
                            fontWeight = if (status == CheckInStatus.TODAY) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// EARN TASK ROW  — numbered list exactly like screenshot
// ─────────────────────────────────────────────────────────────

@Composable
private fun EarnTaskRow(index: Int, task: EarnTaskDto, onClick: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Index number
        Text(
            "$index.",
            style      = MaterialTheme.typography.bodyLarge,
            color      = BpscColors.TextSecondary,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(24.dp),
            textAlign  = TextAlign.End
        )

        // Icon circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(task.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                mapIcon(task.icon),
                 null,
                tint     = task.iconTint,
                modifier = Modifier.size(22.dp)
            )

            Icon(Icons.Rounded.ArrowUpward,
                null,
                tint = BpscColors.Success,
                modifier = Modifier.size(14.dp))

        }

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    task.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = BpscColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                // Ad badge
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
            Text(
                task.subtitle,
                style  = MaterialTheme.typography.bodyMedium,
                color  = BpscColors.TextSecondary
            )
        }

        // Action button — exactly like screenshot
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(task.actionBg)
                .clickable(enabled = !task.isCompleted) { /* handle action */ }
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                task.actionLabel,
                style      = MaterialTheme.typography.labelSmall,
                color      = task.actionTextColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 12.sp
            )
        }
    }

    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        color     = BpscColors.Divider,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────────────────────
// HISTORY SUMMARY ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun HistorySummaryRow(transactions: List<CoinTransaction>) {
    val totalEarned = transactions.filter { it.type == TransactionType.EARNED }.sumOf { it.coins }
    val totalSpent  = transactions.filter { it.type == TransactionType.SPENT  }.sumOf { kotlin.math.abs(it.coins) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Earned card
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFE8FDF4)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.ArrowUpward, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                    Text("Earned", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                }
                Text(
                    "+$totalEarned",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = BpscColors.Success,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("coins total", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }

        // Spent card
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.ArrowDownward, null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Text(
                    "-$totalSpent",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = Color(0xFFC62828),
                    fontWeight = FontWeight.ExtraBold
                )
                Text("coins total", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TRANSACTION ROW
// ─────────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(transaction: CoinTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(transaction.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                transaction.icon,
                contentDescription = null,
                tint     = transaction.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.title,
                style      = MaterialTheme.typography.bodyMedium,
                color      = BpscColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                transaction.subtitle,
                style    = MaterialTheme.typography.labelSmall,
                color    = BpscColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                transaction.timeLabel,
                style    = MaterialTheme.typography.labelSmall,
                color    = BpscColors.TextHint,
                fontSize = 10.sp
            )
        }

        // Coin amount
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("🪙", fontSize = 11.sp)
                Text(
                    if (transaction.type == TransactionType.EARNED) "+${transaction.coins}"
                    else "${transaction.coins}",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = if (transaction.type == TransactionType.EARNED) BpscColors.Success else Color(0xFFE53935),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp
                )
            }
            Text(
                "coins",
                style    = MaterialTheme.typography.labelSmall,
                color    = BpscColors.TextHint,
                fontSize = 9.sp
            )
        }
    }

    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = BpscColors.Divider,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────

private fun Int.formatWithComma(): String {
    return "%,d".format(this)
}