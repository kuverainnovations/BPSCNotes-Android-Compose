package com.example.bpscnotes.presentation.currentaffairs

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.bpscnotes.data.remote.api.CaMcqDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// Answer DTO — returned from server after user taps (anti-cheat pattern)
data class CaMcqAnswerDto(
    val correct: String = "",
    val explanation: String? = null
)

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

    LaunchedEffect(affairId) { viewModel.loadMcqs(affairId) }

    // ── Silent background time tracker ────────────────────────
    TrackStudyTime(
        activityType = "mcq"
    ) { type, secs -> viewModel.logStudyTime(type, secs) }

    var currentIndex  by remember { mutableIntStateOf(0) }
    var answers       by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showResult    by remember { mutableStateOf(false) }
    var showReview    by remember { mutableStateOf(false) }

    val navigateBack: () -> Unit = {
        if (adManager != null && activity != null)
            adManager.showInterstitialIfReady(activity) { navController.popBackStackSafe() }
        else
            navController.popBackStackSafe()
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
                mcqs      = mcqs,
                answers   = answers,
                serverAnswers = mcqAnswers,
                onBack    = { showReview = false }
            )

            showResult -> ResultScreen(
                mcqs          = mcqs,
                answers       = answers,
                serverAnswers = mcqAnswers,
                onRetry       = {
                    answers = emptyMap()
                    currentIndex = 0
                    showResult = false
                    viewModel.loadMcqs(affairId)
                },
                onReview      = { showReview = true },
                onBack        = navigateBack,
                adManager     = adManager,
                activity      = activity
            )

            else -> QuizScreen(
                mcqs          = mcqs,
                currentIndex  = currentIndex,
                answers       = answers,
                serverAnswers = mcqAnswers,
                adManager     = adManager,
                onAnswer      = { qId, letter ->
                    answers = answers + (qId to letter)
                    viewModel.fetchMcqAnswer(qId)  // fetch correct answer after user taps
                },
                onNext        = {
                    if (currentIndex < mcqs.size - 1) currentIndex++
                    else showResult = true
                },
                onBack        = { if (currentIndex > 0) currentIndex-- else navigateBack() }
            )
        }
    }
}

@Composable
private fun QuizScreen(
    mcqs:          List<CaMcqDto>,
    currentIndex:  Int,
    answers:       Map<String, String>,
    serverAnswers: Map<String, CaMcqAnswerDto>,
    adManager:     AdManager?,
    onAnswer:      (String, String) -> Unit,
    onNext:        () -> Unit,
    onBack:        () -> Unit,
) {
    val str      = LocalStrings.current
    val cs       = MaterialTheme.colorScheme
    val q        = mcqs[currentIndex]
    val answered = answers[q.id]
    val serverAns = serverAnswers[q.id]
    var showQuit by remember { mutableStateOf(false) }
    val progress by animateFloatAsState((currentIndex + 1f) / mcqs.size, tween(400), label = "prog")

    BackHandler { showQuit = true }

    if (showQuit) {
        AppQuitDialog(
            title     = "Quit MCQ Quiz?",
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                    // Spacer for layout balance
                    Spacer(Modifier.size(36.dp))
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

        // ── Banner Ad ──────────────────────────────────────────
        adManager?.let {
            BannerAdView(adUnitId = it.getBannerAdUnitId())
        }

        // ── Scrollable content ─────────────────────────────────
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
                Text(
                    q.question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }

            // Options — correct/wrong only revealed after server answer returns
            val optionPairs = listOf("a" to q.optionA, "b" to q.optionB, "c" to q.optionC,
                "d" to q.optionD, "e" to q.optionE).filter { it.second.isNotBlank() }

            optionPairs.forEach { (letter, text) ->
                val isSelected = answered == letter
                val correctLetter = serverAns?.correct
                val isCorrect  = correctLetter != null && letter == correctLetter
                val isWrong    = isSelected && correctLetter != null && letter != correctLetter
                val bgColor    = when { isCorrect -> Color(0xFFE8FDF4); isWrong -> Color(0xFFFEE8E8); isSelected -> BpscColors.PrimaryLight; else -> Color.White }
                val borderColor = when { isCorrect -> Color(0xFF2ECC71); isWrong -> Color(0xFFE74C3C); isSelected -> BpscColors.Primary; else -> BpscColors.Divider }

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
                            when { isCorrect -> Color(0xFF2ECC71); isWrong -> Color(0xFFE74C3C); isSelected -> BpscColors.Primary; else -> BpscColors.Surface }
                        ), contentAlignment = Alignment.Center
                    ) {
                        Text(letter.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected || isCorrect || isWrong) Color.White else BpscColors.TextHint,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Text(text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when { isCorrect -> Color(0xFF1A7A4A); isWrong -> Color(0xFFB71C1C); isSelected -> BpscColors.Primary; else -> BpscColors.TextPrimary },
                        modifier = Modifier.weight(1f))
                    if (isCorrect) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
                    if (isWrong) Icon(Icons.Rounded.Close, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                }
            }

            // Explanation — shown after server answer returns
            val explanation = serverAns?.explanation ?: if (answered != null) q.explanation else null
            if (answered != null && !explanation.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF8E1)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(explanation, style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D4037), lineHeight = 22.sp)
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
                    Text("Checking answer...", style = MaterialTheme.typography.bodySmall, color = BpscColors.Primary)
                }
            }
        }

        // ── Next / Submit button ───────────────────────────────
        if (answered != null) {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick  = onNext,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text(
                        if (currentIndex < mcqs.size - 1) str.caNxtQuestion else str.caSeeResults,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    mcqs:          List<CaMcqDto>,
    answers:       Map<String, String>,
    serverAnswers: Map<String, CaMcqAnswerDto>,
    onRetry:       () -> Unit,
    onReview:      () -> Unit,
    onBack:        () -> Unit,
    adManager:     AdManager?,
    activity:      Activity?,
) {
    val str     = LocalStrings.current
    val correct = mcqs.count { q -> serverAnswers[q.id]?.correct == answers[q.id] }
    val skipped = mcqs.count { q -> answers[q.id] == null }
    val wrong   = mcqs.size - correct - skipped
    val pct     = if (mcqs.isNotEmpty()) (correct * 100) / mcqs.size else 0
    val animPct by animateFloatAsState(pct / 100f, tween(1200), label = "pct")

    BackHandler { onBack() }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.statusBarsPadding())
        Spacer(Modifier.height(32.dp))
        Text(if (pct >= 80) "🏆" else if (pct >= 50) "👍" else "📚", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            when { pct >= 80 -> str.caExcellent; pct >= 60 -> str.caWellDone; pct >= 40 -> str.caGoodEffort; else -> str.caKeepPracticing },
            style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(24.dp))

        // Score ring
        Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
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
                Text("$pct%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(str.quizScore, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Stats card
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ResultStat("✅", "$correct", str.quizCorrect, Color(0xFF2ECC71))
                ResultStat("❌", "$wrong",   str.quizWrong,   Color(0xFFE74C3C))
                ResultStat("⏭️", "$skipped", "Skipped",       BpscColors.TextSecondary)
                ResultStat("📝", "${mcqs.size}", "Total",      BpscColors.Primary)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Banner ad on result screen
        adManager?.let {
            BannerAdView(adUnitId = it.getBannerAdUnitId())
            Spacer(Modifier.height(16.dp))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(str.quizReviewAll, style = MaterialTheme.typography.titleMedium)
            }
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(str.tryAgain, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Icon(Icons.Rounded.ArrowBack, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(str.caBackToArticle, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ReviewScreen(
    mcqs:          List<CaMcqDto>,
    answers:       Map<String, String>,
    serverAnswers: Map<String, CaMcqAnswerDto>,
    onBack:        () -> Unit,
) {
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
                    Text("Answer Review", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${mcqs.size} questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(mcqs) { index, q ->
                val userAnswer   = answers[q.id]
                val serverAns    = serverAnswers[q.id]
                val correctLetter = serverAns?.correct
                val isCorrect    = correctLetter != null && userAnswer == correctLetter
                val isSkipped    = userAnswer == null
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = when {
                        isSkipped -> Color.White
                        isCorrect -> Color(0xFFF0FBF5)
                        else      -> Color(0xFFFEF0F0)
                    }),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                when { isSkipped -> "⏭ Skipped"; isCorrect -> "✅ Correct"; else -> "❌ Wrong" },
                                style = MaterialTheme.typography.labelSmall,
                                color = when { isSkipped -> BpscColors.TextSecondary; isCorrect -> Color(0xFF2ECC71); else -> Color(0xFFE74C3C) },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(q.question, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
                        // Show options with correct/wrong highlighting
                        listOf("a" to q.optionA, "b" to q.optionB, "c" to q.optionC, "d" to q.optionD, "e" to q.optionE)
                            .filter { it.second.isNotBlank() }
                            .forEach { (letter, text) ->
                                val isCrct = correctLetter != null && letter == correctLetter
                                val isUser = letter == userAnswer
                                val bg = when { isCrct -> Color(0xFFE8FDF4); isUser && !isCrct -> Color(0xFFFEE8E8); else -> Color.Transparent }
                                val tc = when { isCrct -> Color(0xFF2ECC71); isUser && !isCrct -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(letter.uppercase(), style = MaterialTheme.typography.labelSmall, color = tc, fontWeight = FontWeight.ExtraBold)
                                    Text(text, style = MaterialTheme.typography.bodyMedium, color = tc, modifier = Modifier.weight(1f))
                                    if (isCrct) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(14.dp))
                                    if (isUser && !isCrct) Icon(Icons.Rounded.Close, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp))
                                }
                            }
                        val explanation = serverAns?.explanation ?: q.explanation
                        if (!explanation.isNullOrBlank()) {
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