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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

@Composable
fun TopicQuizScreen(
    navController: NavHostController,
    subject: String,
    topicTitle: String
) {
    // Filter questions matching this subject from mock data
    val topicQuestions = mockQuizSessions
        .flatMap { it.questions }
        .filter { it.subject.equals(subject, ignoreCase = true) }
        .ifEmpty {
            // Fallback — use all free questions if no subject match
            mockQuizSessions.first { it.isFree }.questions
        }

    val topicSession = QuizSession(
        id       = "topic_${subject}",
        title    = topicTitle,
        subtitle = "${topicQuestions.size} questions · $subject",
        isFree   = true,
        questions = topicQuestions
    )

    var isStarted by remember { mutableStateOf(false) }

    if (isStarted) {
        // Reuse the same QuizSessionScreen — just pass topic session
        QuizSessionScreen(
            session       = topicSession,
            navController = navController,
            onExit        = { navController.popBackStack() }
        )
    } else {
        // Topic quiz intro screen
        Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            Offset(0f, 0f), Offset(400f, 300f)
                        ))
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Topic Quiz", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            Text(topicTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Quiz Details", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TopicQuizStat("📝", "${topicQuestions.size}", "Questions")
                                TopicQuizStat("⏱️", "30s",                   "Per Question")
                                TopicQuizStat("🪙", "+${topicQuestions.size}","Max Coins")
                            }

                            HorizontalDivider(color = BpscColors.Divider)

                            // Rules
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

                    // Subject tag
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubjectChip(subject)
                        Text("${topicQuestions.size} Questions", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick   = { isStarted = true },
                        modifier  = Modifier.fillMaxWidth().height(54.dp),
                        shape     = RoundedCornerShape(14.dp),
                        colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Text("Start Topic Quiz 🚀", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicQuizStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}