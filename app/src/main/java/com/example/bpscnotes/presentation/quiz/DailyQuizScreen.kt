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
import com.example.bpscnotes.presentation.quiz.components.LobbyStatChip
import com.example.bpscnotes.presentation.quiz.components.SectionLabel
import kotlinx.coroutines.*

// ─────────────────────────────────────────────────────────────
// ENTRY POINT — screen wired to ViewModel
// ─────────────────────────────────────────────────────────────

@Composable
fun DailyQuizScreen(
    navController: NavHostController,
    date: String,                              // quizId from nav arg (or "today" for lobby)
    viewModel: QuizViewModel
) {
    val state by viewModel.uiState.collectAsState()

    // If a specific quiz was navigated to directly (from dashboard), start it immediately
    LaunchedEffect(date) {
        if (date.isNotEmpty() && date != "today") {
            viewModel.startQuiz(date)
        } else {
            viewModel.loadLobby()   // 🔥 FIX
        }
    }

    /*LaunchedEffect(Unit) {
        viewModel.loadLobby()
    }
*/
    when {
        // ── Result screen ─────────────────────────────────────
        state.result != null && state.activeSession != null -> {
            QuizSummaryScreen(
                session      = state.activeSession!!,
                result       = state.result!!,
                onReviewAll  = { /* handled inside summary via showReviewAll flag */ },
                onExit       = { viewModel.exitSession() },
                navController = navController
            )
        }

        // ── Active play screen ────────────────────────────────
        state.activeSession != null && !state.isLoadingDetail -> {
            QuizSessionScreen(
                session       = state.activeSession!!,
                viewModel     = viewModel,
                onExit        = { viewModel.exitSession() }
            )
        }

        // ── Loading quiz detail ───────────────────────────────
        state.isLoadingDetail -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Loading quiz...", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                }
            }
        }
        //state.isLoadingDetail || state.isLoadingList -> show loader

        // ── Detail error ──────────────────────────────────────
        state.detailError != null -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text(state.detailError!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.clearErrors() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                        Text("Back to Quizzes")
                    }
                }
            }
        }

        // ── Lobby ─────────────────────────────────────────────
        else -> {
            QuizLobbyScreen(
                navController = navController,
                state         = state,
                onStartQuiz   = { quizId -> viewModel.startQuiz(quizId) },
                onRetryList   = { viewModel.loadLobby() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LOBBY
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizLobbyScreen(
    navController: NavHostController,
    state: QuizUiState,
    onStartQuiz: (String) -> Unit,
    onRetryList: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header with REAL user stats from ViewModel
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 300f)
                    ))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Daily Quiz", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Test your knowledge today", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Coins — REAL from user profile
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Text(
                                "${state.userProfile?.coins ?: "--"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.CoinGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats strip — REAL from user profile
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val streak   = state.userProfile?.streak
                        val rank     = state.userProfile?.rank
                        val solved   = state.userProfile?.quizzesAttempted
                        val accuracy = state.userProfile?.accuracy?.toDoubleOrNull() ?: 0.0
                        val formatted = String.format("%.2f", accuracy)

                        LobbyStatChip("🎯", if (accuracy != null) "$formatted%" else "--", "Accuracy")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🔥", if (streak != null) "$streak" else "--", "Day Streak")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🏆", if (rank != null) "#$rank" else "--", "Rank")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("✅", if (solved != null) "$solved" else "--", "Solved")
                    }
                }
            }

            // Content
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
                            Button(onClick = onRetryList, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Try Again") }
                        }
                    }
                }
                state.dailyQuizzes.isEmpty() && state.mockTestQuizzes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                            Text("No quizzes today", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Check back later!", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.dailyQuizzes.isNotEmpty()) {
                            item { SectionLabel("🆓 Daily Quizzes", "Resets every day at midnight") }
                            items(state.dailyQuizzes, key = { it.id }) { quiz ->
                                QuizPreviewCard(quiz = quiz, onStart = { onStartQuiz(quiz.id) })
                            }
                        }
                        if (state.mockTestQuizzes.isNotEmpty()) {
                            item { Spacer(Modifier.height(4.dp)) }
                            item { SectionLabel("📝 Mock Tests", "Full length practice exams") }
                            items(state.mockTestQuizzes, key = { it.id }) { quiz ->
                                QuizPreviewCard(quiz = quiz, onStart = { onStartQuiz(quiz.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun QuizPreviewCard(quiz: QuizPreviewDto, onStart: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(BpscColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(when (quiz.type) { "daily"->"📅"; "topic"->"📝"; "mock"->"📋"; else->"🎯" }, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (quiz.isAttempted) {
                        Text("✓", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
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
                if (quiz.isAttempted && quiz.avgScore > 0) {
                    Text("Last score: ${quiz.avgScore.toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick   = onStart,
                shape     = RoundedCornerShape(10.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                modifier  = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(if (quiz.isAttempted) "Retry" else "Start", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ SESSION — wired to ViewModel
// ─────────────────────────────────────────────────────────────

enum class AnswerState { None, Correct, Wrong }

@Composable
internal fun QuizSessionScreen(
    session: QuizSession,
    viewModel: QuizViewModel,
    onExit: () -> Unit
) {
    val state              by viewModel.uiState.collectAsState()
    val questions           = session.questions
    var currentIndex       by remember { mutableIntStateOf(0) }
    // Local ephemeral UI state — reset per question
    var localSelectedIndex by remember(currentIndex) { mutableIntStateOf(-1) }
    var answerState        by remember(currentIndex) { mutableStateOf(AnswerState.None) }
    var showExplanation    by remember(currentIndex) { mutableStateOf(false) }
    var showHint           by remember(currentIndex) { mutableStateOf(false) }
    var timeLeft           by remember(currentIndex) { mutableIntStateOf(30) }
    var timerActive        by remember(currentIndex) { mutableStateOf(true) }
    var streak             by remember { mutableIntStateOf(0) }
    var totalTimeSecs      by remember { mutableIntStateOf(0) }
    var isQuizComplete     by remember { mutableStateOf(false) }
    var showReviewAll      by remember { mutableStateOf(false) }

    val scope   = rememberCoroutineScope()
    val current = questions.getOrNull(currentIndex)

    val optLetters = listOf("a", "b", "c", "d")

    // ── Submit when quiz flagged complete ──────────────────────
    LaunchedEffect(isQuizComplete) {
        if (isQuizComplete) viewModel.submitQuiz(totalTimeSecs)
    }

    // ── Per-question timer ─────────────────────────────────────
    LaunchedEffect(currentIndex, timerActive) {
        timeLeft = 30
        while (timeLeft > 0 && timerActive && answerState == AnswerState.None) {
            delay(1000L)
            timeLeft--
            totalTimeSecs++
        }
        if (timeLeft == 0 && answerState == AnswerState.None && current != null) {
            // Auto-skip — no answer recorded (ViewModel treats missing = skipped)
            if (currentIndex < questions.size - 1) {
                currentIndex++
            } else {
                isQuizComplete = true
            }
        }
    }

    // ── Show result when API returns ──────────────────────────
    if (state.result != null) return  // parent handles this via DailyQuizScreen

    // ── Review screen ─────────────────────────────────────────
    if (showReviewAll && state.activeSession != null) {
        QuizReviewScreen(
            questions   = state.activeSession!!.questions,
            userAnswers = state.selectedAnswers,
            onBack      = { showReviewAll = false }
        )
        return
    }

    if (current == null) return

    val progress     = (currentIndex + 1).toFloat() / questions.size
    val animProgress by animateFloatAsState(progress, tween(500), label = "qprog")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), Offset(0f, 0f), Offset(400f, 200f)))
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onExit), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("Q ${currentIndex + 1} / ${questions.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (streak > 1) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("🔥", fontSize = 11.sp)
                                    Text("$streak", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                    val s = 3.dp.toPx()
                                    drawArc(Color.White.copy(0.2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(s))
                                    drawArc(if (timeLeft > 10) Color.White else Color(0xFFFF6B6B), -90f, (timeLeft / 30f) * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(s, cap = StrokeCap.Round))
                                }
                                Text("$timeLeft", style = MaterialTheme.typography.labelSmall, color = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject + difficulty chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SubjectChip(current.subject)
                    DifficultyChip(current.difficulty)
                }

                // Question card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(current.question, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
                        AnimatedVisibility(visible = showHint) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF8E1)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💡", fontSize = 16.sp)
                                    Text("Think carefully about each option before selecting.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404))
                                }
                            }
                        }
                    }
                }

                // Options
                current.options.forEachIndexed { index, option ->
                    val optionState = when {
                        answerState == AnswerState.None && localSelectedIndex == index -> "selected"
                        answerState != AnswerState.None && optLetters[index] == viewModel.getAnswer(current.id) && answerState == AnswerState.Correct -> "correct"
                        answerState != AnswerState.None && optLetters[index] == viewModel.getAnswer(current.id) && answerState == AnswerState.Wrong -> "wrong"
                        else -> "default"
                    }
                    val bg      = when (optionState) { "selected"->"bg"; "correct"->"correct"; "wrong"->"wrong"; else->"default" }
                    val bgColor = when (bg) { "bg"->BpscColors.PrimaryLight; "correct"->Color(0xFFE8FDF4); "wrong"->Color(0xFFFEE8E8); else->Color.White }
                    val border  = when (bg) { "bg"->BpscColors.Primary; "correct"->BpscColors.Success; "wrong"->Color(0xFFE74C3C); else->BpscColors.Divider }
                    val txtClr  = when (bg) { "bg"->BpscColors.Primary; "correct"->BpscColors.Success; "wrong"->Color(0xFFE74C3C); else->BpscColors.TextPrimary }

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bgColor).border(1.5.dp, border, RoundedCornerShape(14.dp))
                            .clickable(enabled = answerState == AnswerState.None) {
                                localSelectedIndex = index
                                timerActive        = false
                                val letter         = optLetters[index]
                                viewModel.recordAnswer(current.id, letter)
                                // Determine correct/wrong — we don't know correctOption yet (backend doesn't send it),
                                // so we mark answer visually as "selected" only; the visual correct/wrong shows after submit.
                                // For instant feedback we show it as "answered" and allow proceeding.
                                answerState     = AnswerState.Correct  // visual placeholder — real eval is server-side
                                if (streak >= 0) streak++
                                showExplanation = false                 // no client-side explanation — shown after submit
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(border.copy(alpha = if (optionState == "default") 0.1f else 0.2f)), contentAlignment = Alignment.Center) {
                            Text(listOf("A","B","C","D")[index], style = MaterialTheme.typography.titleMedium, color = border, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Text(option, style = MaterialTheme.typography.bodyLarge, color = txtClr, modifier = Modifier.weight(1f))
                        if (optionState == "selected") Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Bottom bar ────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (answerState == AnswerState.None) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { showHint = !showHint },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, BpscColors.CoinGold),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.CoinGold)
                        ) { Text("💡", fontSize = 14.sp); Spacer(Modifier.width(4.dp)); Text("Hint", style = MaterialTheme.typography.titleMedium) }

                        OutlinedButton(
                            onClick = {
                                streak = 0
                                if (currentIndex < questions.size - 1) { currentIndex++ }
                                else { isQuizComplete = true }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, BpscColors.Divider),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                        ) { Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Skip", style = MaterialTheme.typography.titleMedium) }
                    }
                } else {
                    if (state.isSubmitting) {
                        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Button(
                            onClick  = {
                                if (currentIndex < questions.size - 1) {
                                    currentIndex++
                                } else {
                                    isQuizComplete = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                        ) {
                            Text(
                                if (currentIndex < questions.size - 1) "Next Question →" else "Submit & See Results 🏆",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ SUMMARY — uses QuizResult from ViewModel
// ─────────────────────────────────────────────────────────────

@Composable
internal fun QuizSummaryScreen(
    session: QuizSession,
    result: QuizResult,
    onReviewAll: () -> Unit,
    onExit: () -> Unit,
    navController: NavHostController
) {
    var showReviewAll by remember { mutableStateOf(false) }

    if (showReviewAll) {
        // Review screen using populated question data (correct answers + explanations now available)
        QuizAnswerReviewScreen(
            answerDetails = result.answerDetails,
            onBack        = { showReviewAll = false }
        )
        return
    }

    val accuracy = result.accuracy
    val progress by animateFloatAsState(accuracy.toFloat() / 100f, tween(1200), label = "sum")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp).statusBarsPadding())

            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { accuracy >= 80 -> "Excellent!"; accuracy >= 50 -> "Good Job!"; else -> "Keep Practicing!" }, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(session.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2
                    val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, progress * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${accuracy.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryStatItem("✅", "${result.correctCount}", "Correct",  BpscColors.Success)
                    SummaryStatItem("❌", "${result.wrongCount}",   "Wrong",    Color(0xFFE74C3C))
                    SummaryStatItem("⏭️", "${result.skippedCount}", "Skipped",  BpscColors.TextSecondary)
                    SummaryStatItem("🪙", "+${result.coinsEarned}", "Coins",    BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Accuracy", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${accuracy.toInt()}%", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BpscColors.Surface)) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF64B5F6))), RoundedCornerShape(5.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${result.totalQuestions} questions · ${result.timeTakenSecs}s", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        Text(if (result.isPassed) "✅ Passed" else "❌ Not passed", style = MaterialTheme.typography.bodyMedium, color = if (result.isPassed) BpscColors.Success else Color(0xFFE74C3C), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = { showReviewAll = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Review All Questions", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick  = onExit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color.White.copy(0.4f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Back to Quizzes", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryStatItem(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// ANSWER REVIEW — per-question detail with correct answers + explanations
// ─────────────────────────────────────────────────────────────

@Composable
internal fun QuizAnswerReviewScreen(
    answerDetails: List<QuizAnswerDetail>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)))).statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Review Questions", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${answerDetails.size} questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(answerDetails) { index, detail ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            detail.isSkipped -> Color.White
                            detail.isCorrect -> Color(0xFFF0FBF5)
                            else             -> Color(0xFFFEF0F0)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                                }
                                SubjectChip(detail.question.subject)
                                DifficultyChip(detail.question.difficulty)
                            }
                            Text(
                                when { detail.isSkipped -> "⏭ Skipped"; detail.isCorrect -> "✅ Correct"; else -> "❌ Wrong" },
                                style = MaterialTheme.typography.labelSmall,
                                color = when { detail.isSkipped -> BpscColors.TextSecondary; detail.isCorrect -> BpscColors.Success; else -> Color(0xFFE74C3C) },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(detail.question.question, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

                        detail.question.options.forEachIndexed { i, option ->
                            val isCorrectOpt = i == detail.correctIndex
                            val isUserOpt    = i == detail.selectedIndex
                            val bgOpt = when { isCorrectOpt -> Color(0xFFE8FDF4); isUserOpt && !isCorrectOpt -> Color(0xFFFEE8E8); else -> Color.Transparent }
                            val txtOpt = when { isCorrectOpt -> BpscColors.Success; isUserOpt && !isCorrectOpt -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bgOpt).padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(listOf("A","B","C","D")[i], style = MaterialTheme.typography.labelSmall, color = txtOpt, fontWeight = FontWeight.ExtraBold)
                                Text(option, style = MaterialTheme.typography.bodyMedium, color = txtOpt, modifier = Modifier.weight(1f))
                                if (isCorrectOpt) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                                if (isUserOpt && !isCorrectOpt) Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp))
                            }
                        }

                        if (detail.explanation.isNotEmpty()) {
                            HorizontalDivider(color = BpscColors.Divider)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("💡", fontSize = 13.sp)
                                Text(detail.explanation, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SHARED CHIP HELPERS
// ─────────────────────────────────────────────────────────────

@Composable
internal fun SubjectChip(subject: String) {
    val colors = mapOf(
        "Polity"    to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "History"   to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Economy"   to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK"  to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
        "Science"   to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    )
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
internal fun DifficultyChip(difficulty: String) {
    val (color, bg) = when (difficulty.lowercase()) {
        "easy" -> Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4))
        "hard" -> Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8))
        else   -> Pair(Color(0xFFF39C12), Color(0xFFFFF8E1))
    }
    Text(difficulty.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}
