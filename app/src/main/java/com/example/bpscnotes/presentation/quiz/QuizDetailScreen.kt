package com.example.bpscnotes.presentation.quiz

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen

/**
 * QuizDetailScreen — Quiz Intro / Pre-start screen.
 *
 * Flow:
 *   Dashboard card click / QuizList card click
 *     → this screen (shows title, rules, stats)
 *     → "Start Quiz" → navigates to QuizPlay
 *
 * Does NOT start the quiz — just shows info.
 */
@Composable
fun QuizDetailScreen(
    navController: NavHostController,
    quizId: String,
    viewModel: QuizViewModel= hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(quizId) {
        viewModel.loadQuizDetail(quizId)
    }

    when {
        state.isLoadingDetail -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        }
        state.detailError != null -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text(state.detailError!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { navController.popBackStack() }) { Text("Go Back") }
                        Button(onClick = { viewModel.loadQuizDetail(quizId) }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
                    }
                }
            }
        }
        state.quizDetail != null -> {
            QuizIntroContent(
                quiz          = state.quizDetail!!,
                navController = navController,
                quizId        = quizId
            )
        }
        else -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        }
    }
}

@Composable
private fun QuizIntroContent(
    quiz: QuizPreviewDto,
    navController: NavHostController,
    quizId: String
) {
    val difficultyColor = when (quiz.difficulty.lowercase()) {
        "easy"   -> Color(0xFF2ECC71)
        "hard"   -> Color(0xFFE74C3C)
        else     -> Color(0xFFF39C12)
    }

    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface).verticalScroll(rememberScrollState())) {

        // ── Hero header ─────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                    Offset(0f, 0f), Offset(400f, 300f)
                ))
                .statusBarsPadding()
        ) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                drawCircle(Color.White.copy(0.04f), 90.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.8f))
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                // Back button
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(16.dp))

                // Quiz type chip
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        quiz.type.replaceFirstChar { it.uppercase() },
                        style    = MaterialTheme.typography.labelSmall,
                        color    = BpscColors.CoinGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    if (quiz.isAttempted) {
                        Text(
                            "✓ Attempted",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = BpscColors.Success,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Text(
                    quiz.title,
                    style      = MaterialTheme.typography.headlineSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 30.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    quiz.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.75f)
                )
                Spacer(Modifier.height(20.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.12f))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChipWhite("📝", "${quiz.totalQuestions}", "Questions")
                    VerticalDividerWhite()
                    StatChipWhite("⏱️", "${quiz.durationMins}m", "Duration")
                    VerticalDividerWhite()
                    StatChipWhite("🪙", "${quiz.coinsReward}", "Coins")
                    VerticalDividerWhite()
                    StatChipWhite("🎯", "${quiz.passingScore}%", "To Pass")
                }
            }
        }

        // ── Body ────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Difficulty + attempts
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(difficultyColor.copy(0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.BarChart, null, tint = difficultyColor, modifier = Modifier.size(14.dp))
                    Text(
                        quiz.difficulty.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = difficultyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (quiz.attemptCount > 0) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.PrimaryLight)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.People, null, tint = BpscColors.Primary, modifier = Modifier.size(14.dp))
                        Text(
                            "${quiz.attemptCount} attempts",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (quiz.isAttempted && quiz.avgScore > 0) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.Success.copy(0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.Star, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                        Text(
                            "Your best: ${quiz.avgScore.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Rules card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Quiz Rules", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = BpscColors.Divider)
                    listOf(
                        "📝  ${quiz.totalQuestions} questions to answer",
                        "⏱️  ${quiz.durationMins} minutes total time limit",
                        "🎯  Score ${quiz.passingScore}% or above to pass",
                        "🪙  Earn ${quiz.coinsReward} coins on passing",
                        "⏭️  You can skip and revisit questions",
                        "📊  Detailed review after submission",
                        "✅  Once submitted, answers cannot be changed"
                    ).forEach { rule ->
                        Text(
                            rule,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = BpscColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Exam tags if available
            if (quiz.examTags.isNotEmpty()) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📚 Relevant Exams", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quiz.examTags.forEach { tag ->
                                Text(
                                    tag,
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = BpscColors.Primary,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Start / Retry button ─────────────────────────────
            Button(
                onClick  = {
                    // Navigate to QuizPlay — that screen calls startQuiz()
                    navController.navigate(Screen.QuizPlayer.createRoute(quizId))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (quiz.isAttempted) "Retake Quiz 🔄" else "Start Quiz 🚀",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun StatChipWhite(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, fontSize = 15.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f), fontSize = 9.sp)
    }
}

@Composable
private fun VerticalDividerWhite() {
    Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(0.2f)))
}
