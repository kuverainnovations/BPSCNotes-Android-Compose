package com.example.bpscnotes.presentation.quiz

import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bpscnotes.core.ads.AdManager
import android.app.Activity
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════
// QuizPlayScreen — entry point for all quiz types
// Handles: text MCQ, image-question MCQ, image-option MCQ (Testbook style)
// ═════════════════════════════════════════════════════════════════
@Composable
fun QuizPlayScreen(
    navController: NavHostController,
    quizId:        String,
    adManager:     AdManager,
    viewModel:     QuizViewModel = hiltViewModel()
) {
    val context  = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(quizId) {
        if (state.activeSession == null && !state.isStartingQuiz) {
            viewModel.startQuiz(quizId)
        }
    }

    when {
        state.result != null && state.activeSession != null -> {
            QuizResultScreen(
                session   = state.activeSession!!,
                result    = state.result!!,
                onRetake  = { viewModel.exitSession(); viewModel.startQuiz(quizId) },
                onExit    = {
                    viewModel.exitSession()
                    // Show interstitial after quiz result (enforces 20min cooldown internally)
                    activity?.let { act ->
                        adManager.showInterstitialIfReady(act) {
                            navController.popBackStack()
                        }
                    } ?: navController.popBackStack()
                }
            )
        }
        state.activeSession != null && !state.isStartingQuiz -> {
            QuizPlayerContent(
                session  = state.activeSession!!,
                viewModel = viewModel,
                onExit   = { viewModel.exitSession(); navController.popBackStack() }
            )
        }
        state.isStartingQuiz -> {
            Box(Modifier
                .fillMaxSize()
                .background(BpscColors.Surface), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(40.dp))
                    Text("Preparing quiz…", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        state.startError != null -> {
            Box(Modifier
                .fillMaxSize()
                .background(BpscColors.Surface), Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 48.sp)
                    Text("Couldn't Start Quiz", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.startError!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.startQuiz(quizId) }, modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Try Again") }
                    OutlinedButton(onClick = { viewModel.exitSession(); navController.popBackStack() }, modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Go Back") }
                }
            }
        }
        else -> Box(Modifier
            .fillMaxSize()
            .background(BpscColors.Surface), Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
    }
}

// ═════════════════════════════════════════════════════════════════
// MAIN QUIZ PLAYER
// ═════════════════════════════════════════════════════════════════
private val optionLetters = listOf("a", "b", "c", "d")
private val optionLabels  = listOf("A", "B", "C", "D")

@Composable
private fun QuizPlayerContent(
    session:   QuizSession,
    viewModel: QuizViewModel,
    onExit:    () -> Unit
) {
    val state         by viewModel.uiState.collectAsState()
    val questions      = session.questions
    var currentIndex  by remember { mutableIntStateOf(0) }
    var showHint      by remember(currentIndex) { mutableStateOf(false) }
    var timeLeft      by remember(currentIndex) { mutableIntStateOf(45) }
    var timerRunning  by remember(currentIndex) { mutableStateOf(true) }
    var totalTimeSecs by remember { mutableIntStateOf(0) }
    var showReview    by remember { mutableStateOf(false) }
    var submitClicked by remember { mutableStateOf(false) }

    val current        = questions.getOrNull(currentIndex) ?: return
    val selectedLetter = viewModel.getAnswer(current.id)
    val hasAnswered     = selectedLetter != null
    val isLastQuestion  = currentIndex == questions.size - 1
    val answeredCount   = questions.count { q -> viewModel.getAnswer(q.id) != null }
    val progress       by animateFloatAsState((currentIndex + 1).toFloat() / questions.size, tween(400), label = "p")

    // Timer — 45s for image questions (more time needed), 30s for text
    val timerMax = if (current.isImageQuestion || current.isImageOptions) 45 else 30

    LaunchedEffect(currentIndex) {
        timeLeft = timerMax; timerRunning = true
        while (timeLeft > 0 && timerRunning) { delay(1000L); timeLeft--; totalTimeSecs++ }
    }

    LaunchedEffect(timeLeft, currentIndex) {
        if (timeLeft == 0 && isLastQuestion && !submitClicked) {
            submitClicked = true; viewModel.submitQuiz(totalTimeSecs)
        }
    }

    if (state.isSubmitting) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.4f)), Alignment.Center) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Submitting quiz…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showReview) {
        QuizReviewScreen(questions = questions, userAnswers = state.selectedAnswers, onBack = { showReview = false })
        return
    }

    Box(Modifier
        .fillMaxSize()
        .background(BpscColors.Surface)) {
        Column(Modifier.fillMaxSize()) {

            // ── HEADER ──────────────────────────────────────────
            QuizHeader(
                currentIndex  = currentIndex,
                totalCount    = questions.size,
                title         = session.title,
                answeredCount = answeredCount,
                timeLeft      = timeLeft,
                timerMax      = timerMax,
                progress      = progress,
                isImageQuiz   = current.isImageQuestion || current.isImageOptions,
                onExit        = onExit,
                onReview      = { showReview = true }
            )

            // ── QUESTION + OPTIONS ───────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject/difficulty tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectChip(current.subject)
                    DifficultyChip(current.difficulty)
                    if (current.isImageQuestion) {
                        Text("🖼️ Image Q", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF3E5F5))
                                .padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }

                // Question card — handles text, image, or both
                QuestionCard(question = current, showHint = showHint)

                // Options — text or image grid
                if (current.isImageOptions) {
                    ImageOptionsGrid(
                        question       = current,
                        selectedLetter = selectedLetter,
                        onSelect       = { letter ->
                            viewModel.recordAnswer(current.id, letter)
                            timerRunning = false; showHint = false
                        }
                    )
                } else {
                    TextOptionsList(
                        question       = current,
                        selectedLetter = selectedLetter,
                        onSelect       = { letter ->
                            viewModel.recordAnswer(current.id, letter)
                            timerRunning = false; showHint = false
                        }
                    )
                }
            }

            // ── BOTTOM BAR ───────────────────────────────────────
            QuizBottomBar(
                isLastQuestion = isLastQuestion,
                hasAnswered    = hasAnswered,
                showHint       = showHint,
                isSubmitting   = state.isSubmitting,
                submitError    = state.submitError,
                currentIndex   = currentIndex,
                onHint         = { showHint = !showHint },
                onNext         = { currentIndex++; showHint = false; timerRunning = true },
                onSubmit       = { if (!submitClicked) { submitClicked = true; viewModel.submitQuiz(totalTimeSecs) } },
                onPrev         = { currentIndex--; showHint = false }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// QUESTION CARD — text or image question
// ═════════════════════════════════════════════════════════════════
@Composable
private fun QuestionCard(question: QuizSessionQuestion, showHint: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Main question image (full width, like Testbook)
            if (question.isImageQuestion && question.questionImageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BpscColors.Surface)
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model             = question.questionImageUrl,
                        contentDescription = "Question image",
                        modifier          = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        contentScale      = ContentScale.Fit
                    )
                }
            }

            // Question text (shown even for image questions — may have supplementary text)
            if (question.question.isNotBlank()) {
                Text(
                    question.question,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = BpscColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                )
            }

            // Hint panel
            AnimatedVisibility(visible = showHint) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 14.sp)
                    Log.e("TAG", "QuestionCard: ${question.explanation}")
                    Text(
                        question.explanation?.takeIf { it.isNotBlank() }
                            ?: if (question.isImageOptions)
                                "Look carefully at each image option. Consider shapes, patterns, and relationships."
                            else
                                "Read each option carefully. Eliminate the obviously wrong answers first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF856404)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// IMAGE OPTIONS GRID — 2×2 grid (Testbook style)
// ═════════════════════════════════════════════════════════════════
@Composable
private fun ImageOptionsGrid(
    question:       QuizSessionQuestion,
    selectedLetter: String?,
    onSelect:       (String) -> Unit
) {
    // 2×2 grid layout
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Select the correct option:",
            style  = MaterialTheme.typography.labelLarge,
            color  = BpscColors.TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        for (row in 0..1) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0..1) {
                    val index  = row * 2 + col
                    val letter = optionLetters[index]
                    val label  = optionLabels[index]
                    val imageUrl   = question.optionImages.getOrNull(index)
                    val optionText = question.options.getOrNull(index) ?: ""
                    val isSelected = selectedLetter == letter

                    ImageOptionCard(
                        label      = label,
                        imageUrl   = imageUrl,
                        optionText = optionText,
                        isSelected = isSelected,
                        modifier   = Modifier.weight(1f),
                        onClick    = { onSelect(letter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageOptionCard(
    label:      String,
    imageUrl:   String?,
    optionText: String,
    isSelected: Boolean,
    modifier:   Modifier,
    onClick:    () -> Unit
) {
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val borderColor = if (isSelected) BpscColors.Primary else BpscColors.Divider
    val bgColor     = if (isSelected) BpscColors.PrimaryLight else Color.White

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Option letter badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) BpscColors.Primary else BpscColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else BpscColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
        }

        // Option image
        if (!imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BpscColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model              = imageUrl,
                    contentDescription = "Option $label image",
                    modifier           = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale       = ContentScale.Fit
                )
            }
        }

        // Option text (if any — for mixed type)
        if (optionText.isNotBlank()) {
            Text(
                optionText,
                style   = MaterialTheme.typography.bodySmall,
                color   = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// TEXT OPTIONS LIST — classic vertical list
// ═════════════════════════════════════════════════════════════════
@Composable
private fun TextOptionsList(
    question:       QuizSessionQuestion,
    selectedLetter: String?,
    onSelect:       (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { i, option ->
            if (option.isBlank()) return@forEachIndexed
            val letter      = optionLetters[i]
            val isSelected  = selectedLetter == letter
            val bgColor     = if (isSelected) BpscColors.PrimaryLight else Color.White
            val borderColor = if (isSelected) BpscColors.Primary else BpscColors.Divider
            val textColor   = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { onSelect(letter) }
                    .padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(if (isSelected) 0.2f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(optionLabels[i], style = MaterialTheme.typography.titleMedium,
                        color = borderColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }

                // Option may include a small inline image (mixed type)
                val optImage = question.optionImages.getOrNull(i)
                if (!optImage.isNullOrBlank()) {
                    AsyncImage(
                        model              = optImage,
                        contentDescription = null,
                        modifier           = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale       = ContentScale.Fit
                    )
                }

                Text(option, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
                if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// HEADER
// ═════════════════════════════════════════════════════════════════
@Composable
private fun QuizHeader(
    currentIndex: Int, totalCount: Int, title: String,
    answeredCount: Int, timeLeft: Int, timerMax: Int,
    progress: Float, isImageQuiz: Boolean,
    onExit: () -> Unit, onReview: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(400f, 200f)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    .clickable(onClick = onExit), Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q ${currentIndex + 1} / $totalCount", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable(onClick = onReview), Alignment.Center) {
                        Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Timer ring
                    Box(Modifier.size(40.dp), Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            val s = 3.dp.toPx()
                            drawArc(Color.White.copy(0.2f), -90f, 360f, false, style = Stroke(s))
                            drawArc(
                                color = if (timeLeft > timerMax / 3) Color.White else Color(0xFFFF6B6B),
                                startAngle = -90f, sweepAngle = (timeLeft.toFloat() / timerMax) * 360f,
                                useCenter = false, style = Stroke(s, cap = StrokeCap.Round)
                            )
                        }
                        Text("$timeLeft", style = MaterialTheme.typography.labelSmall,
                            color = if (timeLeft > timerMax / 3) Color.White else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            // Progress bar
            Box(Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(0.2f))) {
                Box(Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)),
                        RoundedCornerShape(3.dp)
                    ))
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("$answeredCount / $totalCount answered", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f))
                if (isImageQuiz) Text("🖼️ Image Quiz", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.65f))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// BOTTOM BAR
// ═════════════════════════════════════════════════════════════════
@Composable
private fun QuizBottomBar(
    isLastQuestion: Boolean, hasAnswered: Boolean, showHint: Boolean,
    isSubmitting: Boolean, submitError: String?,
    currentIndex: Int,
    onHint: () -> Unit, onNext: () -> Unit,
    onSubmit: () -> Unit, onPrev: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        submitError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onHint,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, if (showHint) BpscColors.CoinGold else BpscColors.Divider),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (showHint) BpscColors.CoinGold else BpscColors.TextSecondary)
            ) {
                Text("💡", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text("Hint", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick  = if (isLastQuestion) onSubmit else onNext,
                modifier = Modifier
                    .weight(2.5f)
                    .height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isLastQuestion) Color(0xFF2ECC71) else BpscColors.Primary
                ),
                enabled = !isSubmitting
            ) {
                Text(
                    when {
                        isLastQuestion && hasAnswered -> "Submit Quiz 🏆"
                        isLastQuestion               -> "Finish & Submit"
                        hasAnswered                  -> "Next →"
                        else                         -> "Skip →"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        if (currentIndex > 0) {
            TextButton(onClick = onPrev, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.size(16.dp))
                Text("← Previous", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// RESULT SCREEN
// ═════════════════════════════════════════════════════════════════
@Composable
private fun QuizResultScreen(
    session: QuizSession, result: QuizResult,
    onRetake: () -> Unit, onExit: () -> Unit
) {
    var showDetailReview by remember { mutableStateOf(false) }
    val accuracy          = result.accuracy
    val progress         by animateFloatAsState(accuracy.toFloat() / 100f, tween(1200), label = "arc")

    if (showDetailReview) {
        QuizAnswerReviewScreen(answerDetails = result.answerDetails, onBack = { showDetailReview = false })
        return
    }

    Box(Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0A2472),
                    Color(0xFF1565C0),
                    BpscColors.Surface
                )
            )
        )) {
        Column(Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier
                .height(60.dp)
                .statusBarsPadding())
            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { accuracy >= 80 -> "Excellent!"; accuracy >= 50 -> "Good Job!"; else -> "Keep Practicing!" },
                style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(session.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(Modifier.size(130.dp), Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, progress * 360f, false,
                        style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${accuracy.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    Text(if (result.isPassed) "✅ Passed" else "❌ Failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (result.isPassed) Color(0xFF2ECC71) else Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Card(Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier
                    .fillMaxWidth()
                    .padding(20.dp), Arrangement.SpaceEvenly) {
                    ResultStat("✅", "${result.correctCount}", "Correct", BpscColors.Success)
                    ResultStat("❌", "${result.wrongCount}", "Wrong", Color(0xFFE74C3C))
                    ResultStat("⏭️", "${result.skippedCount}", "Skipped", BpscColors.TextSecondary)
                    ResultStat("🪙", "+${result.coinsEarned}", "Coins", BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Accuracy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${accuracy.toInt()}%", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(BpscColors.Surface)) {
                        Box(Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        BpscColors.Primary,
                                        Color(0xFF64B5F6)
                                    )
                                ), RoundedCornerShape(5.dp)
                            ))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("${result.totalQuestions} questions · ${result.timeTakenSecs}s", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        Text("Pass: ${session.passingScore}%", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Column(Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showDetailReview = true }, Modifier
                    .fillMaxWidth()
                    .height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.RateReview, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Review All Questions", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onRetake, Modifier
                    .fillMaxWidth()
                    .height(48.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retake Quiz", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onExit, Modifier
                    .fillMaxWidth()
                    .height(48.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.7f))) {
                    Icon(Icons.Rounded.Home, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
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

// ═════════════════════════════════════════════════════════════════
// ANSWER REVIEW — shows correct/wrong with images
// ═════════════════════════════════════════════════════════════════
@Composable
fun ReviewCard(index: Int, detail: QuizAnswerDetail) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = when {
            detail.isSkipped -> Color.White
            detail.isCorrect -> Color(0xFFF0FBF5)
            else             -> Color(0xFFFEF0F0)
        }),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BpscColors.PrimaryLight), Alignment.Center) {
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

            // Show question image in review
            if (detail.question.isImageQuestion && detail.question.questionImageUrl != null) {
                AsyncImage(
                    model = detail.question.questionImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Text(detail.question.question, style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

            // Options with color coding — handles image options too
            if (detail.question.isImageOptions) {
                // 2×2 grid in review
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..1) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0..1) {
                                val i          = row * 2 + col
                                val isCorrect  = i == detail.correctIndex
                                val isUserPick = i == detail.selectedIndex
                                val bg = when { isCorrect -> Color(0xFFE8FDF4); isUserPick && !isCorrect -> Color(0xFFFEE8E8); else -> BpscColors.Surface }
                                val imgUrl = detail.question.optionImages.getOrNull(i)
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .border(
                                            2.dp, when {
                                                isCorrect -> BpscColors.Success; isUserPick -> Color(
                                                    0xFFE74C3C
                                                ); else -> Color.Transparent
                                            }, RoundedCornerShape(10.dp)
                                        )
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(optionLabels[i], style = MaterialTheme.typography.labelSmall,
                                            color = when { isCorrect -> BpscColors.Success; isUserPick -> Color(0xFFE74C3C); else -> BpscColors.TextHint },
                                            fontWeight = FontWeight.ExtraBold)
                                        if (!imgUrl.isNullOrBlank()) {
                                            AsyncImage(imgUrl, null, Modifier
                                                .fillMaxWidth()
                                                .height(90.dp), contentScale = ContentScale.Fit)
                                        }
                                        if (isCorrect) Text("✓ Correct", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                detail.question.options.forEachIndexed { i, option ->
                    if (option.isBlank()) return@forEachIndexed
                    val isCorrectOpt = i == detail.correctIndex
                    val isUserOpt    = i == detail.selectedIndex
                    val bg    = when { isCorrectOpt -> Color(0xFFE8FDF4); isUserOpt && !isCorrectOpt -> Color(0xFFFEE8E8); else -> Color.Transparent }
                    val color = when { isCorrectOpt -> BpscColors.Success; isUserOpt && !isCorrectOpt -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(optionLabels[i], style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
                        // Inline image in mixed mode
                        val optImg = detail.question.optionImages.getOrNull(i)
                        if (!optImg.isNullOrBlank()) {
                            AsyncImage(optImg, null, Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit)
                        }
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )

                        when {
                            isCorrectOpt -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = BpscColors.Success,
                                modifier = Modifier.size(14.dp)
                            )

                            isUserOpt && !isCorrectOpt -> Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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