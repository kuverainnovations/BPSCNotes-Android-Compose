package com.example.bpscnotes.presentation.quiz

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.quiz.components.LobbyStatChip
import com.example.bpscnotes.presentation.quiz.components.SectionLabel

/**
 * QuizListScreen — shows all quizzes grouped by type.
 * Reached from: drawer "Daily Quizzes" / dashboard "See all".
 * Card click → QuizDetailScreen (intro).
 */
@Composable
fun QuizListScreen(
    navController: NavHostController,
    viewModel: QuizViewModel= hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // Only reload if list is empty (avoid re-fetching on back navigation)
        if (state.dailyQuizzes.isEmpty() && state.mockTestQuizzes.isEmpty() && !state.isLoadingList) {
            viewModel.loadLobby()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {

        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 300f)
                    )
                )
                .statusBarsPadding()
        ) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.7f))
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier         = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Daily Quizzes", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("Test your knowledge", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        }
                    }
                    // Real user coins
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🪙", fontSize = 13.sp)
                        Text(
                            "${state.userProfile?.coins ?: "--"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Real user stats strip
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.1f))
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val p = state.userProfile
                    LobbyStatChip("🎯", if (p != null) "${p.accuracy}%" else "--",    "Accuracy")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                    LobbyStatChip("🔥", if (p != null) "${p.streak}" else "--",               "Streak")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                    LobbyStatChip("🏆", if (p?.rank != null) "#${p.rank}" else "--",          "Rank")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                    LobbyStatChip("📝", if (p != null) "${p.quizzesAttempted}" else "--",     "Solved")
                }
            }
        }

        // ── Content ────────────────────────────────────────────
        when {
            state.isLoadingList -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            }
            state.listError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text(state.listError!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadLobby() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
                    }
                }
            }
            state.dailyQuizzes.isEmpty() && state.mockTestQuizzes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📝", fontSize = 48.sp)
                        Text("No quizzes available", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Check back later!", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.dailyQuizzes.isNotEmpty()) {
                        item { SectionLabel("📅 Daily Quizzes", "Resets every day at midnight") }
                        items(state.dailyQuizzes, key = { it.id }) { quiz ->
                            QuizCard(quiz = quiz) {
                                navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                            }
                        }
                    }
                    if (state.mockTestQuizzes.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)) }
                        item { SectionLabel("📋 Mock Tests", "Full length practice exams") }
                        items(state.mockTestQuizzes, key = { it.id }) { quiz ->
                            QuizCard(quiz = quiz) {
                                navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuizCard(quiz: QuizPreviewDto, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier         = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(BpscColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(when (quiz.type) { "daily"->"📅"; "topic"->"📝"; "mock"->"📋"; else->"🎯" }, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (quiz.isAttempted) {
                        Text("✓ Done", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(quiz.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text("·", color = BpscColors.TextHint)
                    Text("${quiz.totalQuestions}Q", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text("·", color = BpscColors.TextHint)
                    Text("${quiz.durationMins}min", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    if (quiz.coinsReward > 0) {
                        Text("·", color = BpscColors.TextHint)
                        Text("🪙${quiz.coinsReward}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                    }
                }
                if (quiz.isAttempted) {
                    Text("Last score: ${quiz.avgScore.toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
        }
    }
}
