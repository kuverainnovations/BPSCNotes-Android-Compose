package com.example.bpscnotes.presentation.aimcq

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMcqScreen(
    navController: NavHostController,
    subject: String = "General",
    topic: String = "BPSC General Knowledge",
    viewModel: AiMcqViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(subject, topic) {
        if (state.questions.isEmpty() && !state.isLoading) {
            viewModel.generateQuestions(subject, topic)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Practice Quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BpscColors.TextPrimary)
                        Text(topic, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = BpscColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BpscColors.CardBg)
            )
        },
        containerColor = BpscColors.Surface
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            when {
                state.isLoading -> LoadingState(subject = subject, topic = topic)
                state.error != null -> ErrorState(error = state.error!!, onRetry = { viewModel.retry() })
                state.isFinished -> ResultState(score = state.score, total = state.questions.size, subject = subject, topic = topic, onRetry = { viewModel.retry() }, onBack = { navController.popBackStack() })
                state.questions.isNotEmpty() -> QuizState(state = state, onSelect = viewModel::selectAnswer, onNext = viewModel::nextQuestion)
            }
        }
    }
}

// ── Loading state ────────────────────────────────────────────

@Composable
private fun LoadingState(subject: String, topic: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF7C4DFF).copy(alpha), Color(0xFF5C4DFF).copy(alpha * 0.5f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Generating AI Questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Creating 5 BPSC-style MCQs for\n$subject · $topic", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF7C4DFF),
            trackColor = BpscColors.Divider
        )
    }
}

// ── Error state ──────────────────────────────────────────────

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Check your internet connection and try again.", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

// ── Quiz state ───────────────────────────────────────────────

@Composable
private fun QuizState(
    state: AiMcqUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    val question = state.questions[state.currentIndex]
    val progress = (state.currentIndex + 1).toFloat() / state.questions.size

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${state.currentIndex + 1}/${state.questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = BpscColors.TextSecondary,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF7C4DFF),
                trackColor = BpscColors.Divider
            )
            Text("Score: ${state.score}", style = MaterialTheme.typography.labelMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
        }

        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BpscColors.CardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF7C4DFF).copy(0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(14.dp))
                    Text("AI Generated · ${state.subject}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C4DFF), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Text(question.question, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
            }
        }

        // Options
        question.options.forEachIndexed { index, option ->
            OptionCard(
                text = option,
                index = index,
                isSelected = state.selectedAnswer == index,
                isRevealed = state.isAnswerRevealed,
                isCorrect = index == question.correctIndex,
                onClick = { onSelect(index) }
            )
        }

        // Explanation
        AnimatedVisibility(visible = state.isAnswerRevealed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20), lineHeight = 20.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Next button
        AnimatedVisibility(visible = state.isAnswerRevealed) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(
                    if (state.currentIndex >= state.questions.size - 1) "See Results" else "Next Question",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OptionCard(
    text: String,
    index: Int,
    isSelected: Boolean,
    isRevealed: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val label = listOf("A", "B", "C", "D")[index]

    val bgColor = when {
        !isRevealed -> if (isSelected) BpscColors.PrimaryLight else BpscColors.CardBg
        isCorrect   -> Color(0xFFE8F5E9)
        isSelected  -> Color(0xFFFFEBEE)
        else        -> BpscColors.CardBg
    }
    val borderColor = when {
        !isRevealed -> if (isSelected) BpscColors.Primary else BpscColors.Divider
        isCorrect   -> Color(0xFF2ECC71)
        isSelected  -> Color(0xFFE74C3C)
        else        -> BpscColors.Divider
    }
    val labelBg = when {
        !isRevealed -> if (isSelected) BpscColors.Primary else BpscColors.Divider
        isCorrect   -> Color(0xFF2ECC71)
        isSelected  -> Color(0xFFE74C3C)
        else        -> BpscColors.Divider
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !isRevealed) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(labelBg),
            contentAlignment = Alignment.Center
        ) {
            if (isRevealed && isCorrect) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else if (isRevealed && isSelected && !isCorrect) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, modifier = Modifier.weight(1f))
    }
}

// ── Result state ─────────────────────────────────────────────

@Composable
private fun ResultState(
    score: Int,
    total: Int,
    subject: String,
    topic: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val percentage = (score.toFloat() / total * 100).toInt()
    val (emoji, message, color) = when {
        percentage >= 80 -> Triple("🏆", "Excellent! Keep it up!", Color(0xFF2ECC71))
        percentage >= 60 -> Triple("👍", "Good job! Practice more.", Color(0xFFF39C12))
        else             -> Triple("📚", "Keep studying, you'll get it!", Color(0xFFE74C3C))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("$subject · $topic", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        Spacer(Modifier.height(32.dp))

        // Score circle
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape)
                .background(color.copy(0.1f))
                .border(3.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score/$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
                Text("$percentage%", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("New AI Questions", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back to Topics", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary)
        }
    }
}
