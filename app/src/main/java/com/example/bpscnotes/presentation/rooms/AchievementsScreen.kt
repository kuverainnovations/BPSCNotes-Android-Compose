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
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.AchievementDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

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
    val cs = MaterialTheme.colorScheme
    val categories = state.grouped.keys.toList()

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
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
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
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
                state.isLoading -> AppLoader()
                state.error != null -> AppErrorState(message = state.error!!, onRetry = { viewModel.load() })
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    categories.forEach { cat ->
                        val items = state.grouped[cat] ?: return@forEach
                        val meta  = CATEGORY_META[cat] ?: Pair("🎯", BpscColors.Primary)
                        item(key = "header_$cat") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(meta.first, fontSize = 16.sp)
                                Text(cat.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
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

// ── Clear achievement card layout ─────────────────────────────
// Each achievement shows status clearly:
// ✅ Earned: green check, title, description, earned date
// 🔄 In Progress: progress bar with exact count (e.g. 3/10 quizzes)
// 🔒 Locked: locked icon, what you need to do

@Composable
private fun AchievementGrid(items: List<AchievementDto>, accentColor: Color) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val earned     = items.filter { it.isEarned }
    val inProgress = items.filter { !it.isEarned }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Earned achievements — clear green cards
        if (earned.isNotEmpty()) {
            earned.forEach { ach ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FBF5)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .border(2.dp, accentColor, CircleShape)
                                .background(accentColor.copy(0.12f)),
                            Alignment.Center
                        ) { Text(ach.emoji, fontSize = 24.sp) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.onSurface)
                            ach.description?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            ach.earnedAt?.let { at ->
                                Text("✅ Earned on ${at.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Success, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.Rounded.CheckCircle, null,
                            tint = BpscColors.Success, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // In Progress — clear progress cards
        if (inProgress.isNotEmpty()) {
            if (earned.isNotEmpty()) Spacer(Modifier.height(4.dp))
            Text(
                "🔄 " + str.achievementsInProgress,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )
            inProgress.forEach { ach ->
                val current = ach.currentValue ?: 0
                val target = ach.goalTarget ?: 0
                val pct = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
                val animPct by animateFloatAsState(pct, tween(800), label = "ach_pct_${ach.id}")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = cs.surface),
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
                                    fontWeight = FontWeight.Bold, color = cs.onSurface)
                                Text("${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accentColor, fontWeight = FontWeight.ExtraBold)
                            }
                            ach.description?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant, maxLines = 1)
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