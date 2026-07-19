package com.example.bpscnotes.presentation.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.BookmarkedQuestionDto

@Composable
fun BookmarkedQuestionsScreen(
    navController: NavHostController,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val str = com.example.bpscnotes.core.language.LocalStrings.current

    Column(Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Column {
                    Text(str.bookmarksTitle, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${state.questions.size} saved questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }

        when {
            state.isLoading && state.questions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoader() }
            }
            state.error != null && state.questions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(str.bookmarksLoadFail, color = BpscColors.TextSecondary)
                        Button(onClick = { viewModel.loadBookmarks(refresh = true) }) { Text(str.retry) }
                    }
                }
            }
            state.questions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔖", fontSize = 48.sp)
                        Text(str.bookmarksEmpty, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            str.bookmarksEmptyHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.questions) { index, q ->
                        BookmarkedQuestionCard(
                            index        = index,
                            question     = q,
                            onUnbookmark = { viewModel.toggleBookmark(q.id) }
                        )
                    }
                    if (state.isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    if (state.hasMore && !state.isLoading) {
                        item { LaunchedEffect(state.page) { viewModel.loadBookmarks() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkedQuestionCard(
    index: Int,
    question: BookmarkedQuestionDto,
    onUnbookmark: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                        Alignment.Center
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    }
                    if (question.quizTitle.isNotBlank()) {
                        Text(
                            question.quizTitle,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = BpscColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                IconButton(onClick = onUnbookmark, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Bookmark, "Remove bookmark", tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                }
            }

            Text(question.question, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

            val correctLetter = question.correct.uppercase()
            val options = listOf(
                "A" to question.optionA,
                "B" to question.optionB,
                "C" to question.optionC,
                "D" to question.optionD,
            ).filter { it.second.isNotBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (letter, text) ->
                    val isCorrect = letter == correctLetter
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCorrect) Color(0xFFE8FDF4) else cs.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(letter, style = MaterialTheme.typography.labelSmall, color = if (isCorrect) BpscColors.Success else BpscColors.TextSecondary, fontWeight = FontWeight.ExtraBold)
                        Text(text, style = MaterialTheme.typography.bodySmall, color = if (isCorrect) BpscColors.Success else cs.onSurface)
                        if (isCorrect) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (question.explanation != null && question.explanation.isNotBlank()) {
                HorizontalDivider(color = cs.outline.copy(alpha = 0.3f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💡", fontSize = 13.sp)
                    Text(question.explanation, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, lineHeight = 18.sp)
                }
            }
        }
    }
}
