package com.example.bpscnotes.presentation.currentaffairs

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppEmptyState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.AppQuitDialog
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.CaMcqDto

@Composable
fun CaMcqQuizScreen(
    navController: NavHostController,
    affairId:      String,
    adManager:     AdManager? = null,
    viewModel:     CurrentAffairsViewModel = hiltViewModel()
) {
    val context    = LocalContext.current
    val activity   = context as? Activity
    val mcqs       by viewModel.mcqs.collectAsState()
    val mcqLoading by viewModel.mcqLoading.collectAsState()
    val str        = LocalStrings.current

    LaunchedEffect(affairId) { viewModel.loadMcqs(affairId) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var answers      by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showResult   by remember { mutableStateOf(false) }

    // Back from result screen → show interstitial ad
    val navigateBack: () -> Unit = {
        if (adManager != null && activity != null)
            adManager.showInterstitialIfReady(activity) { navController.popBackStack() }
        else
            navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        when {
            // ── Loading ──────────────────────────────────────────
            mcqLoading -> AppLoader(message = str.caLoadingQ)

            // ── Empty ────────────────────────────────────────────
            mcqs.isEmpty() -> AppEmptyState(
                emoji       = "❓",
                title       = str.caNoMcqs,
                body        = str.caNoMcqsBody,
                actionLabel = str.caGoBack,
                onAction    = { navController.popBackStack() }
            )

            // ── Result screen ────────────────────────────────────
            showResult -> ResultScreen(
                mcqs    = mcqs,
                answers = answers,
                onRetry = { answers = emptyMap(); currentIndex = 0; showResult = false },
                onBack  = navigateBack
            )

            // ── Quiz screen ──────────────────────────────────────
            else -> QuizScreen(
                mcqs         = mcqs,
                currentIndex = currentIndex,
                answers      = answers,
                onAnswer     = { qId, letter -> answers = answers + (qId to letter) },
                onNext       = {
                    if (currentIndex < mcqs.size - 1) currentIndex++
                    else showResult = true
                },
                onBack       = { if (currentIndex > 0) currentIndex-- else navigateBack() }
            )
        }
    }
}

@Composable
private fun QuizScreen(
    mcqs:     List<CaMcqDto>,
    currentIndex: Int,
    answers:  Map<String, String>,
    onAnswer: (String, String) -> Unit,
    onNext:   () -> Unit,
    onBack:   () -> Unit,
) {
    val str      = LocalStrings.current
    val q        = mcqs[currentIndex]
    val answered = answers[q.id]
    var showQuit by remember { mutableStateOf(false) }
    val progress by animateFloatAsState((currentIndex + 1f) / mcqs.size, tween(400), label = "prog")

    // Intercept back — always show quit dialog during quiz
    BackHandler { showQuit = true }

    // ── Quit confirmation dialog ──────────────────────────────
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

        // ── Header ────────────────────────────────────────────
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
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "Q ${currentIndex + 1} / ${mcqs.size}",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    val diff      = runCatching { q.difficulty }.getOrNull().orEmpty()
                    val diffColor = when (diff) {
                        "easy" -> Color(0xFF2ECC71); "hard" -> Color(0xFFE74C3C); else -> Color(0xFFF39C12)
                    }
                    if (diff.isNotBlank()) {
                        Text(
                            diff.replaceFirstChar { it.uppercaseChar() },
                            style    = MaterialTheme.typography.labelSmall,
                            color    = diffColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(diffColor.copy(0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    } else { Spacer(Modifier.size(36.dp)) }
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

        // ── Scrollable content ────────────────────────────────
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
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = BpscColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    modifier   = Modifier.padding(18.dp)
                )
            }

            // Options — dynamic 2–4 options, skip blank
            val optionPairs = listOf("a" to q.optionA, "b" to q.optionB, "c" to q.optionC, "d" to q.optionD)
                .filter { (_, text) -> text.isNotBlank() }

            optionPairs.forEach { (letter, text) ->
                val isSelected  = answered == letter
                val isCorrect   = answered != null && letter == q.correct
                val isWrong     = isSelected && letter != q.correct
                val bgColor     = when { isCorrect -> Color(0xFFE8FDF4); isWrong -> Color(0xFFFEE8E8); isSelected -> BpscColors.PrimaryLight; else -> Color.White }
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
                        Text(letter.uppercase(), style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected || isCorrect || isWrong) Color.White else BpscColors.TextHint,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Text(text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when { isCorrect -> Color(0xFF1A7A4A); isWrong -> Color(0xFFB71C1C); isSelected -> BpscColors.Primary; else -> BpscColors.TextPrimary },
                        modifier = Modifier.weight(1f))
                    if (isCorrect) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
                }
            }

            // Explanation
            if (answered != null && !q.explanation.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF8E1)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(q.explanation, style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D4037), lineHeight = 22.sp)
                }
            }
        }

        // ── Next / Submit button ──────────────────────────────
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
    mcqs:    List<CaMcqDto>,
    answers: Map<String, String>,
    onRetry: () -> Unit,
    onBack:  () -> Unit,
) {
    val correct  = mcqs.count { answers[it.id] == it.correct }
    val pct      = if (mcqs.isNotEmpty()) (correct * 100) / mcqs.size else 0
    val animPct  by animateFloatAsState(pct / 100f, tween(1200), label = "pct")
    val str      = LocalStrings.current

    // Back press on result → triggers onBack (which shows ad)
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
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ResultStat("✅", "$correct", str.quizCorrect, Color(0xFF2ECC71))
                ResultStat("❌", "${mcqs.size - correct}", str.quizWrong, Color(0xFFE74C3C))
                ResultStat("📝", "${mcqs.size}", "Total", BpscColors.Primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
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
private fun ResultStat(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
    }
}