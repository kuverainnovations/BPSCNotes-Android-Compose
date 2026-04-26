package com.example.bpscnotes.presentation.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.core.ui.t.BpscColors

/**
 * QuizReviewScreen
 *
 * Shown from two places:
 *  1. QuizSessionScreen — during session (no correct answers yet;
 *     shows "not revealed" state with just selected answer highlighted)
 *  2. QuizSummaryScreen — after submit via QuizAnswerReviewScreen
 *     (full correct-answer + explanation detail)
 *
 * This version handles case (1): questions during session where
 * correctOptionLetter may be null (not yet revealed by backend).
 *
 * Parameters:
 *  @param questions  List<QuizSessionQuestion> from the active session
 *  @param userAnswers  Map<questionId, letter> — what the user chose ("a"|"b"|"c"|"d")
 *  @param onBack  navigate back
 */
@Composable
internal fun QuizReviewScreen(
    questions: List<QuizSessionQuestion>,
    userAnswers: Map<String, String>,
    onBack: () -> Unit
) {
    val optLetters = listOf("a", "b", "c", "d")

    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier.size(34.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        "Review Answers",
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${questions.size} questions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.7f)
                    )
                }
            }
        }

        LazyColumn(
            contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(questions) { index, question ->
                val selectedLetter  = userAnswers[question.id] ?: ""      // "" = skipped
                val selectedIndex   = optLetters.indexOf(selectedLetter)  // -1 if skipped
                val correctLetter   = question.correctOptionLetter         // null before submit
                val correctIndex    = optLetters.indexOf(correctLetter ?: "")
                val isSkipped       = selectedLetter.isEmpty()

                // During play (before submit): we don't know correct answer yet
                val resultKnown = correctLetter != null
                val isCorrect   = resultKnown && selectedLetter == correctLetter

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = when {
                            isSkipped   -> Color.White
                            !resultKnown -> Color.White              // during play — neutral
                            isCorrect   -> Color(0xFFF0FBF5)
                            else        -> Color(0xFFFEF0F0)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        // Row: question number + subject + result badge
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier         = Modifier.size(26.dp).clip(CircleShape)
                                        .background(BpscColors.PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = BpscColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                SubjectChip(question.subject)
                                DifficultyChip(question.difficulty)
                            }

                            Text(
                                when {
                                    isSkipped    -> "⏭ Skipped"
                                    !resultKnown -> "📝 Answered"  // before submit
                                    isCorrect    -> "✅ Correct"
                                    else         -> "❌ Wrong"
                                },
                                style      = MaterialTheme.typography.labelSmall,
                                color      = when {
                                    isSkipped    -> BpscColors.TextSecondary
                                    !resultKnown -> BpscColors.Primary
                                    isCorrect    -> BpscColors.Success
                                    else         -> Color(0xFFE74C3C)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Question text
                        Text(
                            question.question,
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = BpscColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        )

                        // Options
                        question.options.forEachIndexed { i, option ->
                            val isUserChoice   = i == selectedIndex
                            val isCorrectOpt   = resultKnown && i == correctIndex

                            val bgOpt = when {
                                isCorrectOpt                    -> Color(0xFFE8FDF4)
                                isUserChoice && !isCorrectOpt
                                        && resultKnown          -> Color(0xFFFEE8E8)
                                isUserChoice && !resultKnown    -> BpscColors.PrimaryLight  // selected, result pending
                                else                            -> Color.Transparent
                            }
                            val txtOpt = when {
                                isCorrectOpt                    -> BpscColors.Success
                                isUserChoice && !isCorrectOpt
                                        && resultKnown          -> Color(0xFFE74C3C)
                                isUserChoice && !resultKnown    -> BpscColors.Primary
                                else                            -> BpscColors.TextSecondary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgOpt)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    listOf("A","B","C","D")[i],
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = txtOpt,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    option,
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = txtOpt,
                                    modifier = Modifier.weight(1f)
                                )
                                when {
                                    isCorrectOpt -> Icon(
                                        Icons.Rounded.CheckCircle, null,
                                        tint     = BpscColors.Success,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    isUserChoice && !isCorrectOpt && resultKnown -> Icon(
                                        Icons.Rounded.Cancel, null,
                                        tint     = Color(0xFFE74C3C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Explanation — only shown if revealed (after submit)
                        if (!question.explanation.isNullOrBlank()) {
                            HorizontalDivider(color = BpscColors.Divider)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("💡", fontSize = 13.sp)
                                Text(
                                    question.explanation!!,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    color      = BpscColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                        } else if (!resultKnown && !isSkipped) {
                            // Before submit — let user know explanations come after
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.PrimaryLight)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("ℹ️", fontSize = 12.sp)
                                Text(
                                    "Correct answer & explanation revealed after submission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BpscColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
