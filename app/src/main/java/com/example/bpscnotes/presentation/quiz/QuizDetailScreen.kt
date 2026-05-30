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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun QuizDetailScreen(
    navController: NavHostController,
    quizId: String,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    var localError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(localError) {
        localError?.let {
            snackbarHostState.showSnackbar(it)
            localError = null
        }
    }
    LaunchedEffect(quizId) {
        viewModel.loadQuizDetail(quizId)
    }

    when {
        state.isLoadingDetail -> {
            Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        }
        state.detailError != null -> {
            Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text(state.detailError!!, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { navController.popBackStack() }) { Text(str.goBack) }
                        Button(onClick = { viewModel.loadQuizDetail(quizId) }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text(str.retry) }
                    }
                }
            }
        }
        state.quizDetail != null -> {
            QuizIntroContent(
                quiz              = state.quizDetail!!,
                navController     = navController,
                quizId            = quizId,
                snackbarHostState = snackbarHostState,
                onSetError        = { localError = it },
                viewModel         = viewModel,
            )
        }
        else -> {
            Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        }
    }
}

@Composable
private fun QuizIntroContent(
    quiz: QuizPreviewDto,
    navController: NavHostController,
    quizId: String,
    snackbarHostState: SnackbarHostState,
    onSetError: (String) -> Unit,
    viewModel: QuizViewModel,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state by viewModel.uiState.collectAsState()
    val difficultyColor = when (quiz.difficulty.lowercase()) {
        "easy" -> Color(0xFF2ECC71)
        "hard" -> Color(0xFFE74C3C)
        else   -> Color(0xFFF39C12)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero header ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 300f)
                    ))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 90.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.8f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            quiz.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        if (quiz.isAttempted) {
                            Text(
                                str.quizAttempted,
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(quiz.title, style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(quiz.subject, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.12f))
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChipWhite("📝", "${quiz.totalQuestions}", str.quizQuestions)
                        VerticalDividerWhite()
                        StatChipWhite("⏱️", "${quiz.durationMins}m", str.quizDuration)
                        VerticalDividerWhite()
                        StatChipWhite("🪙", "${quiz.coinsReward}", str.coins)
                        VerticalDividerWhite()
                        StatChipWhite("🎯", "${quiz.passingScore}%", "To Pass")
                    }
                }
            }

            // ── Body ────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(difficultyColor.copy(0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.BarChart, null, tint = difficultyColor, modifier = Modifier.size(14.dp))
                        Text(quiz.difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall, color = difficultyColor, fontWeight = FontWeight.Bold)
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
                            Text("${quiz.attemptCount} attempts",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
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
                            Text("Your best: ${quiz.avgScore.toInt()}%",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(str.quizRules, style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = cs.outline)
                        listOf(
                            "📝  ${quiz.totalQuestions} questions to answer",
                            "⏱️  ${quiz.durationMins} minutes total time limit",
                            "🎯  Score ${quiz.passingScore}% or above to pass",
                            "🪙  Earn ${quiz.coinsReward} coins on passing",
                            "⏭️  " + str.quizSkip.trimEnd(),
                            "📊  " + str.topicQuizReview,
                            "✅  " + str.quizSubmit
                        ).forEach { rule ->
                            Text(rule, style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant, lineHeight = 20.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if ((state.quizDetail?.totalQuestions ?: 0) == 0) {
                            onSetError(str.quizNoQuestions)
                        } else {
                            navController.navigate(Screen.QuizPlayer.createRoute(quizId))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (quiz.isAttempted) str.quizRetake else str.quizStartNow,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
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
