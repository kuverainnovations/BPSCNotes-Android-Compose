package com.example.bpscnotes.presentation.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

@Composable
fun TopicQuizScreen(
    navController: NavHostController,
    subject: String,
    topicTitle: String,
    viewModel: QuizViewModel = hiltViewModel()    // shared VM — Hilt provides same instance in same back-stack
) {
    val state by viewModel.uiState.collectAsState()

    // Kick off quiz load when screen appears
    LaunchedEffect(subject, topicTitle) {
        if (state.activeSession == null && !state.isLoadingDetail) {
            viewModel.startTopicQuiz(subject)
        }
    }

    when {
        // ── Result ─────────────────────────────────────────────
        state.result != null && state.activeSession != null -> {
            QuizSummaryScreen(
                session       = state.activeSession!!,
                result        = state.result!!,
                onReviewAll   = { },
                onExit        = { viewModel.exitSession(); navController.popBackStack() },
                navController = navController
            )
        }

        // ── Active play ────────────────────────────────────────
        state.activeSession != null && !state.isLoadingDetail -> {
            QuizSessionScreen(
                session   = state.activeSession!!,
                viewModel = viewModel,
                onExit    = { viewModel.exitSession(); navController.popBackStack() }
            )
        }

        // ── Loading ────────────────────────────────────────────
        state.isLoadingDetail || state.isLoadingList -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Loading quiz...", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                }
            }
        }

        // ── Error ──────────────────────────────────────────────
        state.detailError != null || state.listError != null -> {
            Box(Modifier.fillMaxSize().background(BpscColors.Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚠️", fontSize = 40.sp)
                    Text(state.detailError ?: state.listError ?: "Failed to load quiz",
                        style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    Button(onClick = { viewModel.startTopicQuiz(subject) }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                        Text("Retry")
                    }
                }
            }
        }

        // ── Intro screen (before quiz starts) ─────────────────
        else -> {
            TopicQuizIntroScreen(
                subject    = subject,
                topicTitle = topicTitle,
                navController = navController,
                onStart    = { viewModel.startTopicQuiz(subject) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// INTRO SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
private fun TopicQuizIntroScreen(
    subject: String,
    topicTitle: String,
    navController: NavHostController,
    onStart: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)), Offset(0f, 0f), Offset(400f, 300f)))
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Topic Quiz", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                        Text(topicTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Quiz Details", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TopicStat("📝", "10", "Questions")
                            TopicStat("⏱️", "30s", "Per Question")
                            TopicStat("🪙", "+10", "Max Coins")
                        }
                        HorizontalDivider(color = BpscColors.Divider)
                        listOf(
                            "✅  Each correct answer earns 1 coin",
                            "⏭️  You can skip questions",
                            "💡  Use hints for tricky questions",
                            "⏰  30 seconds per question",
                            "📊  Full review available at the end"
                        ).forEach { rule ->
                            Text(rule, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 20.sp)
                        }
                    }
                }

                SubjectChip(subject)

                Spacer(Modifier.weight(1f))

                Button(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text("Start Topic Quiz 🚀", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun TopicStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}
