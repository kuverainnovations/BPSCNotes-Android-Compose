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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.AchievementDto

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/AchievementsScreen.kt
// ════════════════════════════════════════════════════════════

private val CATEGORY_META = mapOf(
    "study"     to Pair("📚", Color(0xFF1565C0)),
    "streak"    to Pair("🔥", Color(0xFFE65100)),
    "quiz"      to Pair("📝", Color(0xFF6A1B9A)),
    "social"    to Pair("👥", Color(0xFF00695C)),
    "tier"      to Pair("🏆", Color(0xFFF57F17)),
    "challenge" to Pair("⚡", Color(0xFF283593)),
)

@Composable
fun AchievementsScreen(
    navController: NavHostController,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val categories = state.grouped.keys.toList()

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(400f, 300f)
                    ))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Achievements", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("Track your milestones", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        }
                    }
                    // Progress pill
                    if (state.totalCount > 0) {
                        val pct = (state.earnedCount * 100f / state.totalCount).toInt()
                        val animPct by animateFloatAsState(pct / 100f, tween(800), label = "ach_pct")
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.12f)).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${state.earnedCount} / ${state.totalCount} unlocked", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                                    Box(Modifier.fillMaxWidth(animPct).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                                }
                            }
                            Text("$pct%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
                state.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp); Text(state.error!!, textAlign = TextAlign.Center, color = BpscColors.TextSecondary)
                        Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(BpscColors.Primary)) { Text("Retry") }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    categories.forEach { cat ->
                        val items = state.grouped[cat] ?: return@forEach
                        val meta  = CATEGORY_META[cat] ?: Pair("🎯", BpscColors.Primary)
                        item(key = "header_$cat") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(meta.first, fontSize = 16.sp)
                                Text(cat.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                Text("${items.count { it.isEarned }}/${items.size}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                            }
                        }
                        item(key = "grid_$cat") {
                            AchievementGrid(items = items, accentColor = meta.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementGrid(items: List<AchievementDto>, accentColor: Color) {
    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ach ->
                    AchievementBadge(achievement = ach, accentColor = accentColor, modifier = Modifier.weight(1f))
                }
                // Fill empty cells
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: AchievementDto, accentColor: Color, modifier: Modifier = Modifier) {
    val earned   = achievement.isEarned
    val bgColor  = if (earned) accentColor.copy(0.1f) else Color(0xFFF5F5F5)
    val border   = if (earned) accentColor.copy(0.4f) else Color(0xFFE0E0E0)
    val alpha    = if (earned) 1f else 0.45f

    Card(modifier = modifier.alpha(alpha).border(if (earned) 1.5.dp else 1.dp, border, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(if (earned) 2.dp else 0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(achievement.emoji, fontSize = 28.sp)
            Text(achievement.title, style = MaterialTheme.typography.labelSmall, color = if (earned) BpscColors.TextPrimary else BpscColors.TextHint, fontWeight = if (earned) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp, fontSize = 10.sp)
            if (earned && achievement.coinsReward > 0) {
                Text("🪙${achievement.coinsReward}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
            if (earned) {
                Icon(Icons.Rounded.CheckCircle, null, tint = accentColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}
