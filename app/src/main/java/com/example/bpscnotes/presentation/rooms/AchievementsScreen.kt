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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
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
    val str = LocalStrings.current
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
                    //  .statusBarsPadding()
                    .padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(str.profileAchievements, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(str.achievementsSubtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
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
                        Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(BpscColors.Primary)) { Text(str.retry) }
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

// ── Redesigned achievement display — matches profile screenshot ──────────
// Earned: circle badges in a row with name + date
// In Progress: card with progress bar + percentage

@Composable
private fun AchievementGrid(items: List<AchievementDto>, accentColor: Color) {
    val str = LocalStrings.current
    val earned     = items.filter { it.isEarned }
    val inProgress = items.filter { !it.isEarned }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Earned badges — circle icons in a row
        if (earned.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(earned) { ach ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape)
                                .border(2.dp, accentColor, CircleShape)
                                .background(accentColor.copy(0.1f)),
                            Alignment.Center
                        ) { Text(ach.emoji, fontSize = 28.sp) }
                        Text(
                            ach.title, style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 2, fontSize = 9.sp,
                            lineHeight = 12.sp, overflow = TextOverflow.Ellipsis
                        )
                        ach.earnedAt?.let { at ->
                            Text(at.take(10), style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // In Progress — cards with progress bars
        if (inProgress.isNotEmpty()) {
            Text(
                str.achievementsInProgress,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BpscColors.TextPrimary
            )
            inProgress.forEach { ach ->
                val pct = if ((ach.goalTarget ?: 0) > 0)
                    ((ach.currentValue ?: 0).toFloat() / ach.goalTarget!!).coerceIn(0f, 1f)
                else 0f
                val animPct by animateFloatAsState(pct, tween(800), label = "ach_pct_${ach.id}")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(Color(0xFFF5F5F5)).alpha(0.6f),
                            Alignment.Center
                        ) { Text(ach.emoji, fontSize = 22.sp) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(ach.title, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                                Text("${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accentColor, fontWeight = FontWeight.ExtraBold)
                            }
                            ach.description?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                    color = BpscColors.TextSecondary, maxLines = 1)
                            }
                            LinearProgressIndicator(
                                progress   = { animPct },
                                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color      = accentColor,
                                trackColor = accentColor.copy(0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}