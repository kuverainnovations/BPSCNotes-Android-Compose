package com.example.bpscnotes.presentation.quiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.quiz.QuizAnswerDetail
import com.example.bpscnotes.presentation.quiz.ReviewCard

@Composable
fun SubjectChip(subject: String) {
    Text(
        subject,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE3F2FD))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFF1565C0),
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
fun DifficultyChip(difficulty: String) {
    val color = when (difficulty.lowercase()) {
        "easy" -> Color(0xFF2ECC71)
        "medium" -> Color(0xFFF39C12)
        "hard" -> Color(0xFFE74C3C)
        else -> Color.Gray
    }

    Text(
        difficulty,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall
    )
}

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
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(answerDetails) { index, detail ->
                ReviewCard(index = index, detail = detail)
            }
        }
    }
}

@Composable
internal fun LobbyStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}