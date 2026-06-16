package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.ChallengeDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/ChallengesScreen.kt
// ════════════════════════════════════════════════════════════

@Composable
fun ChallengesScreen(
    navController: NavHostController,
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.claimSuccess) {
        if (state.claimSuccess != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF303F9F)), Offset(0f,0f), Offset(400f,300f)))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(str.challengesTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(state.weekLabel.ifEmpty { str.challengesThisWeek }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        }
                    }
                    if (state.challenges.isNotEmpty()) {
                        val done  = state.challenges.count { it.isCompleted }
                        val total = state.challenges.size
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.12f)).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$done/$total completed", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${state.challenges.count { it.rewardClaimed }} claimed", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        }
                    }
                }
            }

            when {
                state.isLoading && state.challenges.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
                state.challenges.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚡", fontSize = 48.sp)
                        Text(str.challengesNone, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(str.challengesCheckBack, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    }
                }
                else -> {
                    // Hoisted here — remember{} requires @Composable scope, not LazyListScope
                    val daysLeft = remember {
                        val cal = java.util.Calendar.getInstance()
                        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                        if (dow == 1) 0 else 8 - dow
                    }
                    val allDone = state.challenges.all { it.isCompleted }
                    val sorted  = remember(state.challenges) {
                        state.challenges.sortedWith(compareBy(
                            { if (it.rewardClaimed) 2 else 0 },
                            { if (it.isCompleted && !it.rewardClaimed) 1 else 0 },
                            { if (it.progressPct == 0 && !it.isCompleted) 1 else 0 }
                        ))
                    }

                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // ── Week deadline banner ──────────────────────────
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            allDone       -> BpscColors.Success.copy(0.1f)
                                            daysLeft <= 1 -> Color(0xFFFFF3E0)
                                            else          -> BpscColors.PrimaryLight.copy(0.5f)
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    when {
                                        allDone       -> "✅"
                                        daysLeft <= 1 -> "⚠️"
                                        else          -> "📅"
                                    },
                                    fontSize = 20.sp
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when {
                                            allDone       -> "All challenges complete!"
                                            daysLeft == 0 -> "Last chance — resets tonight"
                                            daysLeft == 1 -> "1 day left this week"
                                            else          -> "$daysLeft days left this week"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            allDone       -> BpscColors.Success
                                            daysLeft <= 1 -> Color(0xFFE65100)
                                            else          -> BpscColors.Primary
                                        }
                                    )
                                    val incomplete = state.challenges.count { !it.isCompleted }
                                    if (!allDone) {
                                        Text(
                                            "$incomplete challenge${if (incomplete != 1) "s" else ""} remaining — complete them before Sunday to earn coins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BpscColors.TextSecondary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Sort: in-progress → not started → complete+unclaimed → claimed last
                        items(sorted, key = { it.id }) { ch ->
                            ChallengeCard(
                                challenge  = ch,
                                isClaiming = state.isClaiming,
                                onClaim    = { viewModel.claimReward(ch.id) }
                            )
                        }
                    }
                }
            }
        }

        // Success snackbar
        if (state.claimSuccess != null) {
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding(), containerColor = BpscColors.Success, contentColor = Color.White) {
                Text(state.claimSuccess!!, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: ChallengeDto, isClaiming: Boolean, onClaim: () -> Unit) {
    val str = LocalStrings.current
    val isComplete = challenge.isCompleted
    val claimed    = challenge.rewardClaimed
    val pct        = challenge.progressPct
    val goalLabel  = when (challenge.goal.type) {
        "study_hours"  -> "${challenge.goal.target.toInt()}h study"
        "quizzes"      -> "${challenge.goal.target.toInt()} quizzes"
        "goals"        -> "${challenge.goal.target.toInt()} goals"
        "sessions"     -> "${challenge.goal.target.toInt()} sessions"
        "streak_days"  -> "${challenge.goal.target.toInt()} day streak"
        else           -> "${challenge.goal.target} ${challenge.goal.type}"
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (isComplete && !claimed) Color(0xFFF0F9FF) else Color.White), elevation = CardDefaults.cardElevation(if (isComplete) 3.dp else 1.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(challenge.emoji, fontSize = 28.sp)
                    Column {
                        Text(challenge.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (challenge.targetTierEmoji != null) {
                            Text("${challenge.targetTierEmoji} ${challenge.targetTierKey?.replaceFirstChar { it.uppercase() } ?: ""} Room only", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (challenge.coinsReward > 0) Text("🪙${challenge.coinsReward}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                        if (challenge.xpReward > 0) Text("⚡${challenge.xpReward}xp", style = MaterialTheme.typography.labelSmall, color = Color(0xFF283593), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${str.challengesGoal}: $goalLabel", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = if (isComplete) BpscColors.Success else BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BpscColors.Divider)) {
                    Box(modifier = Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(if (isComplete) BpscColors.Success else BpscColors.Primary, RoundedCornerShape(4.dp)))
                }
                Text("${challenge.userProgress.toInt()} / ${challenge.goal.target.toInt()} ${if (challenge.goal.type == "study_hours") "hours" else ""}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
            }

            when {
                claimed -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BpscColors.Success.copy(0.1f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(str.challengesClaimed, style = MaterialTheme.typography.titleMedium, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                }
                isComplete -> Button(onClick = onClaim, enabled = !isClaiming, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Success)) {
                    if (isClaiming) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else { Text(str.challengesClaim, style = MaterialTheme.typography.titleMedium) }
                }
                else -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BpscColors.Surface).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                    Text(str.challengesKeepStudying, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}