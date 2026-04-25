package com.example.bpscnotes.presentation.quiz

import androidx.compose.animation.*
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
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.data.remote.api.QuizResultDto
import kotlinx.coroutines.*

/**
 * Entry composable: receives optional quizId from navigation.
 * - If quizId is "today" or empty → show lobby (list of quizzes)
 * - If quizId is a real UUID → immediately start that quiz session
 */
@Composable
fun DailyQuizScreen(
    navController: NavHostController,
    date: String,                              // nav arg — may be quizId or "today"
    viewModel: QuizViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var activeQuizId by remember { mutableStateOf<String?>(null) }

    // If a specific quiz was requested from Dashboard, load it immediately
    LaunchedEffect(date) {
        if (date.isNotEmpty() && date != "today" &&
            // simple UUID heuristic: not a date format
            !date.contains("-") || date.length > 10
        ) {
            activeQuizId = date
            viewModel.loadQuizDetail(date)
        }
    }

    when {
        // ── Active session with result ────────────────────────
        sessionState.result != null -> {
            QuizResultScreen(
                quizTitle    = sessionState.quiz?.title ?: "Quiz",
                questions    = sessionState.questions,
                selectedAnswers = sessionState.sessionAnswerIndices,
                result       = sessionState.result!!,
                onExit       = { activeQuizId = null; viewModel.clearSession() },
                navController = navController
            )
        }

        // ── Active session in progress ────────────────────────
        activeQuizId != null && !sessionState.isLoading && sessionState.questions.isNotEmpty() -> {
            QuizSessionScreen(
                viewModel     = viewModel,
                sessionState  = sessionState,
                onExit        = { activeQuizId = null; viewModel.clearSession() }
            )
        }

        // ── Loading quiz detail ───────────────────────────────
        activeQuizId != null && sessionState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Loading quiz...", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                }
            }
        }

        // ── Lobby ─────────────────────────────────────────────
        else -> {
            QuizLobbyScreen(
                navController = navController,
                viewModel     = viewModel,
                onStartQuiz   = { quizId ->
                    activeQuizId = quizId
                    viewModel.loadQuizDetail(quizId)
                }
            )
        }
    }
}

// helper: convert selectedAnswers (questionId→letter) to answer index for review
private val QuizSessionUiState.sessionAnswerIndices: Map<String, Int>
    get() = selectedAnswers.mapValues { (_, letter) ->
        when (letter) { "a"->0; "b"->1; "c"->2; else->3 }
    }

// ─────────────────────────────────────────────────────────────
// LOBBY
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuizLobbyScreen(
    navController: NavHostController,
    viewModel: QuizViewModel,
    onStartQuiz: (String) -> Unit
) {
    val state by viewModel.lobbyState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header with REAL user stats
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)), Offset(0f,0f), Offset(400f,300f))).statusBarsPadding()) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Daily Quiz", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Test your knowledge today", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Stats from API — no more hardcoded 142 / 87% / #3
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.1f)).padding(horizontal = 4.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val acc   = if (state.userAccuracy > 0) "${state.userAccuracy.toInt()}%" else "--"
                        val strk  = if (state.userStreak > 0) "${state.userStreak}" else "0"
                        val rank  = if (state.userRank != null) "#${state.userRank}" else "--"
                        LobbyStatChip("🎯", acc,  "Accuracy")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🔥", strk, "Day Streak")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🏆", rank, "Rank")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("📝", "${state.freeQuizzes.size + state.paidQuizzes.size}", "Available")
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text(state.error!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        Button(onClick = { viewModel.loadLobby() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.freeQuizzes.isNotEmpty()) {
                        item { SectionLabel("🆓 Free Daily Quizzes", "Resets every day at midnight") }
                        items(state.freeQuizzes) { quiz ->
                            QuizPreviewCard(quiz = quiz, onStart = { onStartQuiz(quiz.id) })
                        }
                    }
                    if (state.paidQuizzes.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)) }
                        item { SectionLabel("📝 Mock Tests", "Full length practice exams") }
                        items(state.paidQuizzes) { quiz ->
                            QuizPreviewCard(quiz = quiz, onStart = { onStartQuiz(quiz.id) })
                        }
                    }
                    if (state.freeQuizzes.isEmpty() && state.paidQuizzes.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📝", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No quizzes today", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Check back later!", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun QuizPreviewCard(quiz: QuizPreviewDto, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                Text(when (quiz.type) { "daily"->"📅"; "topic"->"📝"; "mock"->"📋"; else->"🎯" }, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text("✓ Attempted · Avg ${quiz.avgScore.toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(onClick = onStart, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary), modifier = Modifier.height(38.dp), contentPadding = PaddingValues(horizontal = 14.dp)) {
                Text(if (quiz.isAttempted) "Retry" else "Start", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ SESSION SCREEN
// ─────────────────────────────────────────────────────────────

enum class AnswerState { None, Correct, Wrong }

@Composable
private fun QuizSessionScreen(
    viewModel: QuizViewModel,
    sessionState: QuizSessionUiState,
    onExit: () -> Unit
) {
    val questions      = sessionState.questions
    val currentIndex   = sessionState.currentIndex
    val current        = questions.getOrNull(currentIndex) ?: return

    var localSelected  by remember(currentIndex) { mutableIntStateOf(-1) }
    var answerState    by remember(currentIndex) { mutableStateOf(AnswerState.None) }
    var showExplanation by remember(currentIndex) { mutableStateOf(false) }
    var timeLeft       by remember(currentIndex) { mutableIntStateOf(30) }
    var timerActive    by remember(currentIndex) { mutableStateOf(true) }
    var streak         by remember { mutableIntStateOf(0) }

    val optLetters = listOf("a", "b", "c", "d")
    val progress   = (currentIndex + 1).toFloat() / questions.size
    val animProg  by animateFloatAsState(progress, tween(500), label = "qprog")

    // Timer
    LaunchedEffect(currentIndex, timerActive) {
        timeLeft = 30
        while (timeLeft > 0 && timerActive && answerState == AnswerState.None) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0 && answerState == AnswerState.None) {
            // Auto-skip — record no answer
            viewModel.selectAnswer(current.id, optLetters[0]) // default a on timeout
            streak = 0
            if (currentIndex < questions.size - 1) viewModel.nextQuestion()
            else viewModel.submitQuiz(sessionState.quiz?.id ?: "", 30 * questions.size)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), Offset(0f,0f), Offset(400f,200f))).statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onExit), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("Q ${currentIndex + 1} / ${questions.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (streak > 1) Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("🔥", fontSize = 11.sp)
                                Text("$streak", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontWeight = FontWeight.ExtraBold)
                            }
                            // Timer ring
                            Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                    val s = 3.dp.toPx()
                                    drawArc(Color.White.copy(0.2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(s))
                                    drawArc(if (timeLeft > 10) Color.White else Color(0xFFFF6B6B), -90f, (timeLeft/30f)*360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(s, cap = StrokeCap.Round))
                                }
                                Text("$timeLeft", style = MaterialTheme.typography.labelSmall, color = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject + difficulty
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectChip(current.subject)
                    DifficultyChipLocal(current.difficulty)
                }

                // Question card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                    Text(current.question, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, modifier = Modifier.padding(20.dp))
                }

                // Options
                current.options.forEachIndexed { index, option ->
                    val correctIdx = when (current.correctOption) { "a"->0; "b"->1; "c"->2; else->3 }
                    val optState = when {
                        answerState == AnswerState.None && localSelected == index -> "selected"
                        answerState != AnswerState.None && index == correctIdx    -> "correct"
                        answerState != AnswerState.None && index == localSelected && index != correctIdx -> "wrong"
                        else -> "default"
                    }
                    val bg     = when (optState) { "selected"->BpscColors.PrimaryLight; "correct"->Color(0xFFE8FDF4); "wrong"->Color(0xFFFEE8E8); else->Color.White }
                    val border = when (optState) { "selected"->BpscColors.Primary; "correct"->BpscColors.Success; "wrong"->Color(0xFFE74C3C); else->BpscColors.Divider }
                    val txtClr = when (optState) { "selected"->BpscColors.Primary; "correct"->BpscColors.Success; "wrong"->Color(0xFFE74C3C); else->BpscColors.TextPrimary }

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).border(1.5.dp, border, RoundedCornerShape(14.dp))
                            .clickable(enabled = answerState == AnswerState.None) {
                                localSelected = index
                                timerActive   = false
                                val correct   = index == correctIdx
                                answerState   = if (correct) AnswerState.Correct else AnswerState.Wrong
                                if (correct) streak++ else streak = 0
                                viewModel.selectAnswer(current.id, optLetters[index])
                                showExplanation = true
                            }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(border.copy(if (optState=="default") 0.1f else 0.2f)), contentAlignment = Alignment.Center) {
                            Text(listOf("A","B","C","D")[index], style = MaterialTheme.typography.titleMedium, color = border, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Text(option, style = MaterialTheme.typography.bodyLarge, color = txtClr, modifier = Modifier.weight(1f))
                        when (optState) {
                            "correct" -> Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(20.dp))
                            "wrong"   -> Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(20.dp))
                            else -> {}
                        }
                    }
                }

                // Explanation
                AnimatedVisibility(visible = showExplanation && current.explanation.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (answerState == AnswerState.Correct) Color(0xFFE8FDF4) else Color(0xFFFEF0F0)), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (answerState == AnswerState.Correct) "✅" else "❌", fontSize = 14.sp)
                                Text(if (answerState == AnswerState.Correct) "Correct! +1 Coin" else "Incorrect", style = MaterialTheme.typography.titleMedium, color = if (answerState == AnswerState.Correct) BpscColors.Success else Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                            }
                            Text(current.explanation, style = MaterialTheme.typography.bodyMedium, color = if (answerState == AnswerState.Correct) Color(0xFF1A5C3A) else Color(0xFF8B1C1C), lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Bottom bar
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (answerState == AnswerState.None) {
                    OutlinedButton(onClick = {
                        viewModel.selectAnswer(current.id, optLetters[0])
                        if (currentIndex < questions.size - 1) { viewModel.nextQuestion() }
                        else { viewModel.submitQuiz(sessionState.quiz?.id ?: "", 30 * questions.size) }
                    }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Skip")
                    }
                } else {
                    Button(onClick = {
                        if (currentIndex < questions.size - 1) {
                            localSelected = -1; answerState = AnswerState.None; showExplanation = false; timerActive = true
                            viewModel.nextQuestion()
                        } else {
                            viewModel.submitQuiz(sessionState.quiz?.id ?: "", 30 * questions.size)
                        }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary), enabled = !sessionState.isSubmitting) {
                        if (sessionState.isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(if (currentIndex < questions.size - 1) "Next Question →" else "See Results 🏆", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RESULT SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuizResultScreen(
    quizTitle: String,
    questions: List<QuizSessionQuestion>,
    selectedAnswers: Map<String, Int>,
    result: QuizResultDto,
    onExit: () -> Unit,
    navController: NavHostController
) {
    val progress by animateFloatAsState(result.accuracy.toFloat() / 100f, tween(1200), label = "sum")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp).statusBarsPadding())

            Text(if (result.accuracy >= 80) "🏆" else if (result.accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { result.accuracy >= 80 -> "Excellent!"; result.accuracy >= 50 -> "Good Job!"; else -> "Keep Practicing!" }, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(quizTitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke/2
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, progress * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.accuracy.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ResultStat("✅", "${result.correctAnswers}", "Correct",  BpscColors.Success)
                    ResultStat("❌", "${result.totalQuestions - result.correctAnswers}", "Wrong", Color(0xFFE74C3C))
                    ResultStat("🪙", "+${result.coinsEarned}", "Coins", BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Back to Quizzes", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultStat(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
internal fun SubjectChip(subject: String) {
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun DifficultyChipLocal(difficulty: String) {
    val (color, bg) = when (difficulty.lowercase()) {
        "easy"   -> Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4))
        "hard"   -> Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8))
        else     -> Pair(Color(0xFFF39C12), Color(0xFFFFF8E1))
    }
    Text(difficulty.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}