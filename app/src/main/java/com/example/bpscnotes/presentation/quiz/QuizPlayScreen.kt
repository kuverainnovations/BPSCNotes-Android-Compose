package com.example.bpscnotes.presentation.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay

/**
 * QuizPlayScreen — active quiz session.
 *
 * Reached from: QuizDetailScreen "Start Quiz" button.
 * Calls viewModel.startQuiz(quizId) to fetch questions from backend.
 * On submit → shows QuizResultScreen inline.
 */
@Composable
fun QuizPlayScreen(
    navController: NavHostController,
    quizId: String,
    viewModel: QuizViewModel= hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Start quiz on first entry
    LaunchedEffect(quizId) {
        if (state.activeSession == null && !state.isStartingQuiz) {
            viewModel.startQuiz(quizId)
        }
    }

    when {
        // ── Result ─────────────────────────────────────────────
        state.result != null && state.activeSession != null -> {
            QuizResultScreen(
                session       = state.activeSession!!,
                result        = state.result!!,
                onRetake      = {
                    viewModel.exitSession()
                    viewModel.startQuiz(quizId)
                },
                onExit        = {
                    viewModel.exitSession()
                    navController.popBackStack()
                }
            )
        }

        // ── Active play ─────────────────────────────────────────
        state.activeSession != null && !state.isStartingQuiz -> {
            QuizPlayerContent(
                session       = state.activeSession!!,
                viewModel     = viewModel,
                onExit        = {
                    viewModel.exitSession()
                    navController.popBackStack()
                }
            )
        }

        // ── Starting (fetching questions) ───────────────────────
        state.isStartingQuiz -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(40.dp))
                    Text("Preparing your quiz…", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Loading questions", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
            }
        }

        // ── Start error ──────────────────────────────────────────
        state.startError != null -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.padding(24.dp)
                ) {
                    Text("⚠️", fontSize = 48.sp)
                    Text("Couldn't Start Quiz", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(state.startError!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = { viewModel.startQuiz(quizId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) { Text("Try Again") }
                    OutlinedButton(
                        onClick  = { viewModel.exitSession(); navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) { Text("Go Back") }
                }
            }
        }

        else -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ PLAYER
// ─────────────────────────────────────────────────────────────

private val optionLetters = listOf("a", "b", "c", "d")
private val optionLabels  = listOf("A", "B", "C", "D")

@Composable
private fun QuizPlayerContent(
    session: QuizSession,
    viewModel: QuizViewModel,
    onExit: () -> Unit
) {
    val state          by viewModel.uiState.collectAsState()
    val questions       = session.questions
    var currentIndex   by remember { mutableIntStateOf(0) }
    var showHint       by remember(currentIndex) { mutableStateOf(false) }
    var timeLeft       by remember(currentIndex) { mutableIntStateOf(30) }
    var timerRunning   by remember(currentIndex) { mutableStateOf(true) }
    var totalTimeSecs  by remember { mutableIntStateOf(0) }
    var showReview     by remember { mutableStateOf(false) }
    var submitClicked  by remember { mutableStateOf(false) }

    val current           = questions.getOrNull(currentIndex) ?: return
    val selectedLetter    = viewModel.getAnswer(current.id)
    val hasAnswered        = selectedLetter != null
    val isLastQuestion     = currentIndex == questions.size - 1
    val answeredCount      = questions.count { q -> viewModel.getAnswer(q.id) != null }
    val progress          by animateFloatAsState((currentIndex + 1).toFloat() / questions.size, tween(400), label = "p")

    // Timer
    LaunchedEffect(currentIndex) {
        timeLeft = 30; timerRunning = true
        while (timeLeft > 0 && timerRunning) { delay(1000L); timeLeft--; totalTimeSecs++ }
    }

    // Auto-submit on last question timeout
    LaunchedEffect(timeLeft, currentIndex) {
        if (timeLeft == 0 && isLastQuestion && !submitClicked) {
            submitClicked = true
            viewModel.submitQuiz(totalTimeSecs)
        }
    }

    // Submitting overlay
    if (state.isSubmitting) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Submitting quiz…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Submit error snackbar
    state.submitError?.let { err ->
        LaunchedEffect(err) {
            delay(3000); viewModel.clearErrors()
        }
    }

    // Mid-session review screen
    if (showReview) {
        QuizReviewScreen(
            questions   = questions,
            userAnswers = state.selectedAnswers,
            onBack      = { showReview = false }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), Offset(0f, 0f), Offset(400f, 200f)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        // Exit
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onExit), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        // Counter + title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Q ${currentIndex + 1} / ${questions.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(session.title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), maxLines = 1)
                        }
                        // Timer + review button
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Review all
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { showReview = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            // Timer ring
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                    val s = 3.dp.toPx()
                                    drawArc(Color.White.copy(0.2f), -90f, 360f, false, style = Stroke(s))
                                    drawArc(
                                        color      = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B),
                                        startAngle = -90f, sweepAngle = (timeLeft / 30f) * 360f,
                                        useCenter  = false, style = Stroke(s, cap = StrokeCap.Round)
                                    )
                                }
                                Text("$timeLeft", style = MaterialTheme.typography.labelSmall, color = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    // Progress bar
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                    }
                    // Answered count
                    Text("$answeredCount / ${questions.size} answered", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f))
                }
            }

            // ── Question + Options ─────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tags row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Text("💡", fontSize = 14.sp)
                                    Text("Read each option carefully. Eliminate the obvious wrong answers first.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404))
                                }
                            }
                        }
                    }
                }

                // Options
                current.options.forEachIndexed { i, option ->
                    val letter       = optionLetters[i]
                    val isSelected   = selectedLetter == letter
                    val bgColor      = if (isSelected) BpscColors.PrimaryLight else Color.White
                    val borderColor  = if (isSelected) BpscColors.Primary     else BpscColors.Divider
                    val textColor    = if (isSelected) BpscColors.Primary     else BpscColors.TextPrimary

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.recordAnswer(current.id, letter)
                                timerRunning = false
                                showHint = false
                            }
                            .padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(30.dp).clip(CircleShape)
                                .background(borderColor.copy(if (isSelected) 0.2f else 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(optionLabels[i], style = MaterialTheme.typography.titleMedium, color = borderColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Text(option, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
                        if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Bottom bar ─────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Submit error
                state.submitError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Hint button
                    OutlinedButton(
                        onClick  = { showHint = !showHint },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, BpscColors.CoinGold),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.CoinGold)
                    ) {
                        Text("💡", fontSize = 14.sp); Spacer(Modifier.width(4.dp))
                        Text("Hint", style = MaterialTheme.typography.titleMedium)
                    }

                    // Skip / Next / Submit
                    Button(
                        onClick = {
                            when {
                                isLastQuestion -> {
                                    if (!submitClicked) {
                                        submitClicked = true
                                        viewModel.submitQuiz(totalTimeSecs)
                                    }
                                }
                                else -> {
                                    currentIndex++; showHint = false; timerRunning = true
                                }
                            }
                        },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (isLastQuestion) Color(0xFF2ECC71) else BpscColors.Primary
                        ),
                        enabled = !state.isSubmitting
                    ) {
                        Text(
                            when {
                                isLastQuestion && hasAnswered -> "Submit Quiz 🏆"
                                isLastQuestion               -> "Submit & Finish"
                                hasAnswered                  -> "Next Question →"
                                else                         -> "Skip →"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Previous button (not on first question)
                if (currentIndex > 0) {
                    TextButton(
                        onClick  = { currentIndex--; showHint = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.size(16.dp))
                        Text("← Previous Question", style = MaterialTheme.typography.bodyMedium)
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
    session: QuizSession,
    result: QuizResult,
    onRetake: () -> Unit,
    onExit: () -> Unit
) {
    var showDetailReview by remember { mutableStateOf(false) }
    val accuracy          = result.accuracy
    val progress         by animateFloatAsState(accuracy.toFloat() / 100f, tween(1200), label = "arc")

    if (showDetailReview) {
        QuizAnswerReviewScreen(
            answerDetails = result.answerDetails,
            onBack        = { showDetailReview = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(Modifier.height(60.dp).statusBarsPadding())

            // Trophy
            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { accuracy >= 80 -> "Excellent!"; accuracy >= 50 -> "Good Job!"; else -> "Keep Practicing!" }, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(session.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2
                    val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, progress * 360f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${accuracy.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    Text(if (result.isPassed) "✅ Passed" else "❌ Failed", style = MaterialTheme.typography.labelSmall, color = if (result.isPassed) Color(0xFF2ECC71) else Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ResultStat("✅", "${result.correctCount}",  "Correct",  BpscColors.Success)
                    ResultStat("❌", "${result.wrongCount}",    "Wrong",    Color(0xFFE74C3C))
                    ResultStat("⏭️", "${result.skippedCount}", "Skipped",  BpscColors.TextSecondary)
                    ResultStat("🪙", "+${result.coinsEarned}", "Coins",    BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Accuracy bar
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Accuracy",             style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${accuracy.toInt()}%", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BpscColors.Surface)) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF64B5F6))), RoundedCornerShape(5.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${result.totalQuestions} questions · ${result.timeTakenSecs}s taken", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        Text("Pass mark: ${session.passingScore}%", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showDetailReview = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Review All Questions", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color.White.copy(0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text("Retake Quiz", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color.White.copy(0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.7f))) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text("Back to Quizzes", style = MaterialTheme.typography.titleMedium)
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
// POST-SUBMIT REVIEW (correct answers + explanations)
// ─────────────────────────────────────────────────────────────



@Composable
fun ReviewCard(index: Int, detail: QuizAnswerDetail) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
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
                val bg    = when { isCorrectOpt -> Color(0xFFE8FDF4); isUserOpt && !isCorrectOpt -> Color(0xFFFEE8E8); else -> Color.Transparent }
                val color = when { isCorrectOpt -> BpscColors.Success; isUserOpt && !isCorrectOpt -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(optionLabels[i], style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
                    Text(option, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
                    when { isCorrectOpt -> Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp)); isUserOpt && !isCorrectOpt -> Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp)) }
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

// ─────────────────────────────────────────────────────────────
// SHARED CHIPS
// ─────────────────────────────────────────────────────────────

