package com.example.bpscnotes.presentation.currentaffairs

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppEmptyState
import com.example.bpscnotes.core.ui.AppErrorDialog
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.AppQuitDialog
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.analytics.Event
import com.example.bpscnotes.data.remote.api.CaMcqDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import kotlinx.coroutines.delay

// Answer DTO — returned from server after user taps (anti-cheat pattern)
data class CaMcqAnswerDto(
    val correct: String = "",
    val explanation: String? = null
)

/**
 * Per-question scoring outcome — implements the exact BPSC negative
 * marking rule as specified by the client, not the generic "skip = 0"
 * convention used elsewhere in the app:
 *
 *   Correct                       → +marksPerCorrect
 *   Wrong (attempted, incorrect)  → −marksPerWrong  (0 if negative marking is off)
 *   Option E — Not Attempted      → 0  (always, regardless of negative marking)
 *   Left completely blank         → −marksPerWrong  (SAME penalty as wrong —
 *                                    this is the one place BPSC rules diverge
 *                                    from "skip costs nothing")
 *
 * "Option E" here means the standardized not-attempting choice shown in
 * place of a blank option_e slot (see QuizScreen) — not a genuine custom
 * 5th answer an admin typed in, which scores like any other option.
 */
data class CaMcqQuestionResult(
    val mcq: CaMcqDto,
    val userAnswer: String?,
    val correctLetter: String,
    val isCorrect: Boolean,
    val isNotAttempting: Boolean,
    val isBlank: Boolean,
    val marks: Double,
)

data class CaMcqMarksSummary(
    val results: List<CaMcqQuestionResult>,
    val correct: Int,
    val wrong: Int,
    val notAttempted: Int,
    val blank: Int,
    val negativeMarkingEnabled: Boolean,
    val marksObtained: Double,
    val negativeMarks: Double,
    val finalScore: Double,
    val totalMarks: Double,
)

fun computeCaMcqResults(
    mcqs: List<CaMcqDto>,
    answers: Map<String, String>,
    serverAnswers: Map<String, CaMcqAnswerDto>,
    markingConfig: com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto,
): CaMcqMarksSummary {
    val negEnabled = markingConfig.negativeMarkingEnabled
    val results = mcqs.map { q ->
        val userAnswer = answers[q.id]
        val correctLetter = serverAnswers[q.id]?.correct?.takeIf { it.isNotBlank() } ?: q.correct
        // Tapped the standardized "Option E — Not Attempting" slot, which
        // only exists when the admin left option_e blank (see QuizScreen).
        // "skip" = new Not-Attempting key (works whether or not optionE is real).
        // "e" + blank optionE = legacy Not-Attempting from old sessions.
        val isNotAttempting = userAnswer == "skip" || (userAnswer == "e" && q.optionE.isBlank())
        val isBlank   = userAnswer == null
        val isCorrect = !isBlank && !isNotAttempting && userAnswer == correctLetter
        val marks = when {
            isNotAttempting -> 0.0
            isCorrect        -> markingConfig.marksPerCorrect
            // Both "attempted but wrong" and "left fully blank" lose marks
            // identically once negative marking is on — per BPSC rule.
            else              -> if (negEnabled) -markingConfig.marksPerWrong else 0.0
        }
        CaMcqQuestionResult(q, userAnswer, correctLetter, isCorrect, isNotAttempting, isBlank, marks)
    }
    val correct      = results.count { it.isCorrect }
    val notAttempted = results.count { it.isNotAttempting }
    val blank         = results.count { it.isBlank && !it.isNotAttempting }
    val wrong          = results.size - correct - notAttempted - blank
    val marksObtained = correct * markingConfig.marksPerCorrect
    val negativeMarks = results.sumOf { if (it.marks < 0) -it.marks else 0.0 }
    val finalScore      = results.sumOf { it.marks }
    val totalMarks       = mcqs.size * markingConfig.marksPerCorrect
    return CaMcqMarksSummary(results, correct, wrong, notAttempted, blank, negEnabled, marksObtained, negativeMarks, finalScore, totalMarks)
}

// Inline study time tracker — starts timing when screen opens, logs on leave
@androidx.compose.runtime.Composable
private fun TrackStudyTime(
    activityType: String,
    onLog: (activityType: String, durationSecs: Int) -> Unit
) {
    val startTime = androidx.compose.runtime.remember { System.currentTimeMillis() }
    androidx.compose.runtime.DisposableEffect(activityType) {
        onDispose {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000)
                .toInt().coerceIn(0, 3600)
            if (elapsed >= 10) onLog(activityType, elapsed)
        }
    }
}

@Composable
fun CaMcqQuizScreen(
    navController: NavHostController,
    affairId:      String,
    adManager:     AdManager? = null,
    viewModel:     CurrentAffairsViewModel = hiltViewModel()
) {
    val str        = LocalStrings.current
    val context    = LocalContext.current
    val activity   = context as? Activity
    val mcqs       by viewModel.mcqs.collectAsState()
    val mcqLoading by viewModel.mcqLoading.collectAsState()
    val mcqError   by viewModel.mcqError.collectAsState()
    val mcqAnswers by viewModel.mcqAnswers.collectAsState()
    val mcqMarkingConfig by viewModel.mcqMarkingConfig.collectAsState()

    LaunchedEffect(affairId) { viewModel.loadMcqs(affairId) }

    // ── Silent background time tracker ────────────────────────
    TrackStudyTime(
        activityType = "mcq"
    ) { type, secs -> viewModel.logStudyTime(type, secs) }

    var currentIndex  by remember { mutableIntStateOf(0) }
    var answers       by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showResult    by remember { mutableStateOf(false) }
    var showAdBreak   by remember { mutableStateOf(false) }   // shown between quiz end and result
    var showReview    by remember { mutableStateOf(false) }
    var retryKey      by remember { mutableIntStateOf(0) }  // increments on each retry to reset timer
    // Per-question 30-second countdown
    var questionSecsLeft by remember { mutableIntStateOf(30) }

    // Helper: quiz is done → show ad break first (if adManager present), then result.
    // Called from timer auto-advance and from onNext button when last question answered.
    fun finishQuiz() {
        if (adManager != null) showAdBreak = true
        else                   showResult  = true
    }

    // Per-question countdown — resets on each question AND on retry.
    // Stops counting (freezes) as soon as the current question is answered,
    // so it doesn't keep running and then auto-advance/jump unexpectedly.
    LaunchedEffect(currentIndex, retryKey) {
        if (showResult || showReview || showAdBreak) return@LaunchedEffect
        questionSecsLeft = 30
        val questionId = mcqs.getOrNull(currentIndex)?.id
        while (questionSecsLeft > 0 && answers[questionId] == null) {
            delay(1_000L)
            questionSecsLeft--
        }
        // Time's up (and still unanswered) — auto-advance to next question
        if (!showResult && !showReview && !showAdBreak && answers[questionId] == null) {
            if (currentIndex < mcqs.size - 1) currentIndex++
            else finishQuiz()
        }
    }

    // Simple back navigation — no interstitial on back press (ads only on quiz completion)
    val navigateBack: () -> Unit = { navController.popBackStackSafe() }

    // Computed once here and shared by ResultScreen + ReviewScreen — previously
    // each screen called computeCaMcqResults() independently from the same
    // inputs, which is harmless today (pure function) but is the wrong shape
    // if this summary is ever persisted, and is simply redundant work.
    val summary = remember(mcqs, answers, mcqAnswers, mcqMarkingConfig) {
        computeCaMcqResults(mcqs, answers, mcqAnswers, mcqMarkingConfig)
    }

    // Fire start/completion analytics — fire when quiz is truly done
    // (i.e. showAdBreak goes true, or showResult if no ad manager)
    LaunchedEffect(mcqs) {
        if (mcqs.isNotEmpty()) Event.caMcqStarted(affairId, mcqs.size)
    }
    LaunchedEffect(showAdBreak, showResult) {
        if (showAdBreak || showResult) {
            Event.caMcqCompleted(affairId, summary.correct, summary.results.size)
            viewModel.submitMcqAttempt(affairId, answers)
        }
    }

    // Error dialog for load failure
    if (mcqError != null) {
        AppErrorDialog(
            message      = mcqError!!,
            retryLabel   = str.tryAgain,
            dismissLabel = str.back,
            onRetry      = { viewModel.loadMcqs(affairId) },
            onDismiss    = { viewModel.clearMcqError(); navController.popBackStackSafe() }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        when {
            mcqLoading -> AppLoader(message = str.caLoadingQ)

            mcqs.isEmpty() && mcqError == null -> AppEmptyState(
                emoji       = "❓",
                title       = str.caNoMcqs,
                body        = str.caNoMcqsBody,
                actionLabel = str.caGoBack,
                onAction    = { navController.popBackStackSafe() }
            )

            showReview -> ReviewScreen(
                summary       = summary,
                serverAnswers = mcqAnswers,
                onBack        = { showReview = false }
            )

            // ── Ad break: Quiz Completed → Ad → Result Screen ──────────
            // Shown ONLY after all questions answered (not during quiz,
            // not between questions). onAdBreakComplete transitions to
            // the actual result screen.
            showAdBreak -> McqAdBreakScreen(
                adManager = adManager,
                onAdBreakComplete = {
                    showAdBreak = false
                    showResult  = true
                }
            )

            showResult -> ResultScreen(
                summary       = summary,
                onRetry       = {
                    answers = emptyMap()
                    currentIndex = 0
                    showResult = false
                    showAdBreak = false
                    retryKey++          // forces timer LaunchedEffect to re-run even if index stays 0
                    viewModel.loadMcqs(affairId)
                },
                onReview      = { showReview = true },
                onBack        = navigateBack,
                adManager     = adManager,
            )

            else -> QuizScreen(
                mcqs          = mcqs,
                currentIndex  = currentIndex,
                answers       = answers,
                serverAnswers = mcqAnswers,
                markingConfig = mcqMarkingConfig,
                adManager     = adManager,
                questionSecsLeft = questionSecsLeft,
                onAnswer      = { qId, letter ->
                    answers = answers + (qId to letter)
                    viewModel.fetchMcqAnswer(qId)  // fetch correct answer after user taps
                },
                onNext        = {
                    if (currentIndex < mcqs.size - 1) currentIndex++
                    else finishQuiz()             // was: showResult = true
                },
                onBack        = navigateBack
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MCQ AD BREAK SCREEN
// Shown after the last question is answered and before the
// result screen is revealed. A 15-second countdown wrapping an
// AdMob banner. No skip button — auto-advances when done.
// ─────────────────────────────────────────────────────────────
@Composable
private fun McqAdBreakScreen(
    adManager: AdManager?,
    onAdBreakComplete: () -> Unit,
) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val AD_SECS = 15
    var secondsLeft by remember { mutableIntStateOf(AD_SECS) }

    LaunchedEffect(Unit) {
        secondsLeft = AD_SECS
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        onAdBreakComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF1A1A1A)).statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        str.caCalculating,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFAAAAAA)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (secondsLeft <= 5) Color(0xFF8B0000) else Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Timer, null,
                            tint = if (secondsLeft <= 5) Color(0xFFFF6B6B) else Color(0xFFAAAAAA),
                            modifier = Modifier.size(13.dp))
                        Text(
                            "$secondsLeft",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (secondsLeft <= 5) Color(0xFFFF6B6B) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress bar — fills as countdown proceeds
            val animProg by animateFloatAsState(
                1f - (secondsLeft / AD_SECS.toFloat()), tween(800), label = "adprog"
            )
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF2A2A2A))) {
                Box(
                    modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5))))
                )
            }

            // Ad slot + countdown display
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (adManager != null) {
                        BannerAdView(adUnitId = adManager.getBannerAdUnitId())
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            color = Color(0xFF1565C0),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Results ready in $secondsLeft seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            // Bottom strip
            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                    .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    str.caScoring,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun QuizScreen(
    mcqs:             List<CaMcqDto>,
    currentIndex:     Int,
    answers:          Map<String, String>,
    serverAnswers:    Map<String, CaMcqAnswerDto>,
    markingConfig:    com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto,
    adManager:        AdManager?,
    questionSecsLeft: Int = 30,
    onAnswer:         (String, String) -> Unit,
    onNext:           () -> Unit,
    onBack:           () -> Unit,
) {
    val str      = LocalStrings.current
    val cs       = MaterialTheme.colorScheme
    val q        = mcqs[currentIndex]
    val answered = answers[q.id]
    val serverAns = serverAnswers[q.id]
    var showQuit    by remember { mutableStateOf(false) }
    var hintVisible by remember(q.id) { mutableStateOf(false) }
    val progress by animateFloatAsState((currentIndex + 1f) / mcqs.size, tween(400), label = "prog")
    val hasHint = !q.hint.isNullOrBlank()

    BackHandler { showQuit = true }

    if (showQuit) {
        AppQuitDialog(
            title     = str.mcqQuitTitle,
            body      = "${answers.size}/${mcqs.size} questions answered. Progress will be lost.",
            quitLabel = str.quizQuit,
            keepLabel = str.quizKeepGoing,
            onConfirm = { showQuit = false; onBack() },
            onDismiss = { showQuit = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { showQuit = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp)) }

                    Text(
                        "Q ${currentIndex + 1} / ${mcqs.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.Bold
                    )

                    // 30-second countdown timer
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val sweep = (questionSecsLeft / 30f) * 360f
                            val timerColor = when {
                                questionSecsLeft > 15 -> Color(0xFF4CAF50)
                                questionSecsLeft > 8  -> Color(0xFFFF9800)
                                else                  -> Color(0xFFF44336)
                            }
                            drawArc(Color.White.copy(0.2f), -90f, 360f, false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(timerColor, -90f, sweep, false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text(
                            "$questionSecsLeft",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
                // Progress bar
                Box(modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)),
                            RoundedCornerShape(3.dp)))
                }
            }
        }

        // ── Scrollable content ─────────────────────────────────
        // NOTE: No banner ads during the quiz — ads only after completion.
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Question card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                McqQuestionHtml(
                    html     = q.question,
                    modifier = Modifier.fillMaxWidth().padding(18.dp)
                )
                if (q.isMatchQuestion && q.matchData != null) {
                    com.example.bpscnotes.presentation.quiz.MatchTableWidget(
                        matchData = q.matchData!!,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    )
                }
            }

            // Options — correct/wrong only revealed after server answer returns.
            // "Not Attempting" (key="skip") is always available when negative
            // marking is on so users always have a penalty-free escape, even
            // when the admin filled in a real 5th option for letter E.
            val showNotAttempting = true
            val optionPairs = (listOf("a" to q.optionA, "b" to q.optionB, "c" to q.optionC,
                "d" to q.optionD, "e" to q.optionE).filter { it.second.isNotBlank() }) +
                    (if (showNotAttempting) listOf("skip" to "Not Attempting This Question") else emptyList())

            optionPairs.forEach { (letter, text) ->
                val isNotAttemptOption = letter == "skip"
                val isSelected = answered == letter
                val correctLetter = serverAns?.correct
                val isCorrect  = !isNotAttemptOption && correctLetter != null && letter == correctLetter
                val isWrong    = !isNotAttemptOption && isSelected && correctLetter != null && letter != correctLetter
                val bgColor    = when {
                    isNotAttemptOption && isSelected -> Color(0xFFFFF3E0)
                    isCorrect -> Color(0xFFE8FDF4); isWrong -> Color(0xFFFEE8E8); isSelected -> BpscColors.PrimaryLight
                    else -> Color.White
                }
                val borderColor = when {
                    isNotAttemptOption && isSelected -> Color(0xFFF57F17)
                    isCorrect -> Color(0xFF2ECC71); isWrong -> Color(0xFFE74C3C); isSelected -> BpscColors.Primary
                    else -> BpscColors.Divider
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable(enabled = answered == null) { onAnswer(q.id, letter) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(
                            when {
                                isNotAttemptOption && isSelected -> Color(0xFFF57F17)
                                isCorrect -> Color(0xFF2ECC71); isWrong -> Color(0xFFE74C3C); isSelected -> BpscColors.Primary
                                else -> BpscColors.Surface
                            }
                        ), contentAlignment = Alignment.Center
                    ) {
                        Text(if (isNotAttemptOption) "⊘" else letter.uppercase().take(1),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected || isCorrect || isWrong) Color.White else BpscColors.TextHint,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isNotAttemptOption && isSelected -> Color(0xFFE65100)
                                isCorrect -> Color(0xFF1A7A4A); isWrong -> Color(0xFFB71C1C); isSelected -> BpscColors.Primary
                                else -> BpscColors.TextPrimary
                            })
                        if (isNotAttemptOption && markingConfig.negativeMarkingEnabled) {
                            Text(str.caNoPenalty,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100).copy(alpha = 0.8f))
                        }
                    }
                    if (isCorrect) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
                    if (isWrong) Icon(Icons.Rounded.Close, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                }
            }

            // Warn the user up front that a truly-blank question (timeout
            // without choosing anything, including "Not Attempting") will
            // be penalized like a wrong answer — per BPSC rules this is
            // NOT the same as a free skip.
            if (markingConfig.negativeMarkingEnabled && answered == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEE8E8)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 13.sp)
                    Text(
                        if (showNotAttempting)
                            "Letting time run out without choosing an option counts as wrong (-${com.example.bpscnotes.presentation.quiz.formatMarks(markingConfig.marksPerWrong)}). Tap \"Not Attempting\" instead if you don't know this one."
                        else
                            "Letting time run out without answering counts as wrong (-${com.example.bpscnotes.presentation.quiz.formatMarks(markingConfig.marksPerWrong)}).",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC0392B),
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Explanation — shown only AFTER answering ──
            val explanation = serverAns?.explanation ?: if (answered != null) q.explanation else null
            if (answered != null && !explanation.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📖", fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            str.quizHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Text(explanation, style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B5E20), lineHeight = 22.sp)
                    }
                }
            }

            // Waiting indicator while fetching correct answer
            if (answered != null && serverAns == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(BpscColors.PrimaryLight).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BpscColors.Primary)
                    Text(str.caChecking, style = MaterialTheme.typography.bodySmall, color = BpscColors.Primary)
                }
            }
        }

        // ── Static bottom bar: Hint + Next ────────────────────
        if (hasHint || answered != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cs.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hint text — expands above buttons when toggled
                    if (hasHint) {
                        AnimatedVisibility(visible = hintVisible) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF8E1))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💡", fontSize = 14.sp)
                                Text(
                                    q.hint ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF795548),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    // Button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasHint) {
                            OutlinedButton(
                                onClick  = { hintVisible = !hintVisible },
                                modifier = Modifier
                                    .let { if (answered != null) it.weight(1f) else it.fillMaxWidth() }
                                    .height(52.dp),
                                shape  = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (hintVisible) BpscColors.CoinGold else cs.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (hintVisible) BpscColors.CoinGold else BpscColors.TextSecondary
                                )
                            ) {
                                Text("💡", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (hintVisible) "Hide" else "Hint",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        if (answered != null) {
                            Button(
                                onClick  = onNext,
                                modifier = Modifier
                                    .let { if (hasHint) it.weight(1.5f) else it.fillMaxWidth() }
                                    .height(52.dp),
                                shape  = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                            ) {
                                Text(
                                    if (currentIndex < mcqs.size - 1) str.caNxtQuestion else str.caSeeResults,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    summary:   CaMcqMarksSummary,
    onRetry:   () -> Unit,
    onReview:  () -> Unit,
    onBack:    () -> Unit,
    adManager: AdManager?,
) {
    val str     = LocalStrings.current
    val cs      = MaterialTheme.colorScheme
    val correct = summary.correct
    val wrong = summary.wrong
    val notAttempted = summary.notAttempted
    val blank = summary.blank
    val pct     = if (summary.results.isNotEmpty()) (correct * 100) / summary.results.size else 0
    val animPct by animateFloatAsState(pct / 100f, tween(1200), label = "pct")

    val negativeMarkingEnabled = summary.negativeMarkingEnabled
    val marksObtained = summary.marksObtained
    val negativeMarks = summary.negativeMarks
    val finalScore     = summary.finalScore
    val totalMarks      = summary.totalMarks

    BackHandler { onBack() }

    Column(Modifier.fillMaxSize()) {

        // ── TOP: gradient header ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (pct >= 80) "🏆" else if (pct >= 50) "👍" else "📚", fontSize = 44.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                when { pct >= 80 -> str.caExcellent; pct >= 60 -> str.caWellDone; pct >= 40 -> str.caGoodEffort; else -> str.caKeepPracticing },
                style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(108.dp), Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, animPct * 360f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pct%", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(str.quizScore, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }
        }

        // ── BOTTOM: white card ────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().weight(0.58f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = cs.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
                    .navigationBarsPadding()
            ) {
                // Stats row
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    ResultStat("✅", "$correct", str.quizCorrect, Color(0xFF2ECC71))
                    ResultStat("❌", "$wrong", str.quizWrong, Color(0xFFE74C3C))
                    ResultStat("📝", "${summary.results.size}", "Total", BpscColors.Primary)
                    if (notAttempted > 0) {
                        ResultStat(
                            "⊘", "$notAttempted",
                            if (negativeMarkingEnabled) "Safe Skip" else "Skipped",
                            Color(0xFFF57F17)
                        )
                    } else if (blank > 0) {
                        ResultStat("⏰", "$blank", "Left Blank", Color(0xFFE74C3C))
                    }
                }

                // Compact marks breakdown — inline 3-col row
                if (negativeMarkingEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(cs.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+${com.example.bpscnotes.presentation.quiz.formatMarks(marksObtained)}",
                                style = MaterialTheme.typography.labelMedium, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                            Text(str.walletEarned, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(cs.outline))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("-${com.example.bpscnotes.presentation.quiz.formatMarks(negativeMarks)}",
                                style = MaterialTheme.typography.labelMedium, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                            Text(str.penalty, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(cs.outline))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${com.example.bpscnotes.presentation.quiz.formatMarks(finalScore)}/${com.example.bpscnotes.presentation.quiz.formatMarks(totalMarks)}",
                                style = MaterialTheme.typography.labelMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            Text(str.finalScore, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = cs.outline)
                Spacer(Modifier.height(10.dp))

                // Banner ad
                adManager?.let {
                    BannerAdView(adUnitId = it.getBannerAdUnitId())
                    Spacer(Modifier.height(8.dp))
                }

                // Primary: Review All
                Button(
                    onClick  = onReview,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.RateReview, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.quizReviewAll, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                // Secondary: Try Again + Back side by side
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = onRetry,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                    ) {
                        Icon(Icons.Rounded.Refresh, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(str.tryAgain, style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick  = onBack,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, BpscColors.TextSecondary.copy(0.5f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(str.caBackToArticle, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    summary:       CaMcqMarksSummary,
    serverAnswers: Map<String, CaMcqAnswerDto>,
    onBack:        () -> Unit,
) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val cs  = MaterialTheme.colorScheme
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding().padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(str.caAnswerReview, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${summary.results.size} questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            summary.results.forEachIndexed { index, r ->
                val q = r.mcq
                val userAnswer = r.userAnswer
                val correctLetter = r.correctLetter
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = when {
                        r.isNotAttempting -> Color(0xFFFFF8EC)
                        r.isBlank          -> Color.White
                        r.isCorrect       -> Color(0xFFF0FBF5)
                        else               -> Color(0xFFFEF0F0)
                    }),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    when {
                                        r.isNotAttempting -> "⊘ Not Attempted"
                                        r.isBlank          -> "⏰ Left Blank"
                                        r.isCorrect       -> "✅ Correct"
                                        else               -> "❌ Wrong"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        r.isNotAttempting -> Color(0xFFF57F17)
                                        r.isBlank          -> Color(0xFFE74C3C)
                                        r.isCorrect       -> Color(0xFF2ECC71)
                                        else               -> Color(0xFFE74C3C)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                if (summary.negativeMarkingEnabled) {
                                    Text(
                                        if (r.marks > 0) "+${com.example.bpscnotes.presentation.quiz.formatMarks(r.marks)}"
                                        else if (r.marks < 0) com.example.bpscnotes.presentation.quiz.formatMarks(r.marks)
                                        else "0",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            r.marks > 0  -> Color(0xFF2ECC71)
                                            r.marks < 0  -> Color(0xFFE74C3C)
                                            else          -> BpscColors.TextSecondary
                                        },
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        McqQuestionHtml(html = q.question, modifier = Modifier.fillMaxWidth())
                        // Show options: only reveal correct answer if user got it right
                        val showNotAttemptOpt = true
                        (listOf("a" to q.optionA, "b" to q.optionB, "c" to q.optionC, "d" to q.optionD, "e" to q.optionE)
                            .filter { it.second.isNotBlank() } +
                                (if (showNotAttemptOpt) listOf("skip" to "Not Attempting This Question") else emptyList()))
                            .forEach { (letter, text) ->
                                val isUser = letter == userAnswer
                                val isCrct = r.isCorrect && letter == correctLetter
                                val bg = when { isCrct -> Color(0xFFE8FDF4); isUser && !isCrct -> Color(0xFFFEE8E8); else -> Color.Transparent }
                                val tc = when { isCrct -> Color(0xFF2ECC71); isUser && !isCrct -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (letter == "skip") "⊘" else letter.uppercase(), style = MaterialTheme.typography.labelSmall, color = tc, fontWeight = FontWeight.ExtraBold)
                                    Text(text, style = MaterialTheme.typography.bodyMedium, color = tc, modifier = Modifier.weight(1f))
                                    if (isCrct) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(14.dp))
                                    if (isUser && !isCrct) Icon(Icons.Rounded.Close, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp))
                                }
                            }
                        if (r.isBlank) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ℹ️", fontSize = 13.sp)
                                Text(
                                    str.caTimedOut,
                                    style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, lineHeight = 18.sp
                                )
                            }
                        }
                        // Explanation only shown when user answered correctly
                        val explanation = serverAnswers[q.id]?.explanation ?: q.explanation
                        if (!explanation.isNullOrBlank() && r.isCorrect) {
                            HorizontalDivider(color = cs.outline)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("💡", fontSize = 13.sp)
                                Text(explanation, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
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
// ── McqQuestionHtml ──────────────────────────────────────────────────────────
// Renders question text that may contain TipTap HTML (tables, bold, lists)
// using a WebView. Falls back to simple Text if there are no HTML tags.
//
// KEY DESIGN: wrapped in key(html) so that factory is re-invoked for every
// distinct html value. This ensures the onPageFinished closure always captures
// the correct MutableIntState for the current question — without key(), the
// factory closure would keep writing to the first question's state object while
// subsequent questions read from different state objects (Compose closure bug).
@Composable
private fun McqQuestionHtml(html: String, modifier: Modifier = Modifier) {
    val isHtml = remember(html) { html.trimStart().startsWith("<") }

    if (!isHtml) {
        Text(
            html,
            style      = MaterialTheme.typography.bodyLarge,
            color      = BpscColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            modifier   = modifier
        )
        return
    }

    val styledHtml = remember(html) {
        """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
        <style>
          * { box-sizing: border-box; }
          html, body {
            margin: 0; padding: 0;
            height: auto !important;
            overflow: visible !important;
          }
          body {
            font-family: sans-serif;
            font-size: 15px;
            line-height: 1.65;
            color: #1a1a2e;
            background: transparent;
          }
          p { margin: 0 0 8px; }
          strong, b { font-weight: 700; }
          em, i { font-style: italic; }
          ul, ol { padding-left: 20px; margin: 6px 0; }
          li { margin-bottom: 4px; }
          .table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; width: 100%; }
          table {
            border-collapse: collapse;
            min-width: 100%;
            font-size: 13px;
          }
          th {
            background: #ede9fe;
            color: #4c1d95;
            font-weight: 700;
            text-align: left;
            padding: 7px 10px;
            border: 1px solid #c4b5fd;
          }
          td {
            padding: 6px 10px;
            border: 1px solid #e2e8f0;
            vertical-align: top;
          }
          tr:nth-child(even) td { background: #f8f7ff; }
        </style></head><body>
        ${html.replace(Regex("<table"), "<div class='table-wrap'><table")
            .replace(Regex("</table>"), "</table></div>")}
        <div id="__end__" style="clear:both;height:0;padding:0;margin:0;"></div>
        </body></html>
        """.trimIndent()
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    // Initial height estimate from stripped text length — close enough to avoid a
    // jarring jump, then corrected by JS after the page renders.
    val estimatedInitPx = with(density) {
        val textLen = html.replace(Regex("<[^>]+>"), "").length
        val dp = (60 + textLen / 3).coerceIn(60, if (html.contains("<table")) 500 else 220)
        dp.dp.roundToPx()
    }
    // State is keyed on html so it resets when the question changes.
    var webViewHeight by remember(html) { mutableIntStateOf(estimatedInitPx) }

    // key(html) forces AndroidView destruction + recreation whenever html changes.
    // This guarantees factory's onPageFinished closure captures the CURRENT
    // webViewHeight state and not a stale one from a previous question.
    key(html) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled      = true
                        domStorageEnabled      = false
                        loadWithOverviewMode   = true
                        useWideViewPort        = true
                        builtInZoomControls    = false
                        displayZoomControls    = false
                        setSupportZoom(false)
                        @Suppress("DEPRECATION")
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    }
                    isScrollContainer            = false
                    isVerticalScrollBarEnabled   = false
                    isHorizontalScrollBarEnabled = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            // view.post defers until after Compose has applied the initial
                            // height modifier — ensures the WebView is laid out at its
                            // Compose-determined size before we measure document height.
                            view.post {
                                view.evaluateJavascript(
                                    "(function(){ var s=document.getElementById('__end__'); var h=s?s.offsetTop:0; return h>0?h:Math.max(document.body.scrollHeight,document.documentElement.scrollHeight); })()"
                                ) { result ->
                                    val cssH = result?.trim()?.toFloatOrNull() ?: 0f
                                    if (cssH > 8f) {
                                        webViewHeight = (cssH * ctx.resources.displayMetrics.density).toInt()
                                    }
                                }
                            }
                        }
                    }
                    loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                }
            },
            // update is empty: key(html) handles recreation on content change;
            // height changes apply automatically via the modifier below.
            update = { },
            modifier = modifier.height(with(density) { webViewHeight.toDp() })
        )
    }
}