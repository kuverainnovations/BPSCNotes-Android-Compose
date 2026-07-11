package com.example.bpscnotes.presentation.answerwriting

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.AnswerQuestionDto
import com.example.bpscnotes.data.remote.api.AnswerSubmissionDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────
// ANSWER WRITING — question list. Hero = today's question, then all
// questions with status chips; second tab = my submission history.
// ─────────────────────────────────────────────────────────────

private val HeroGradient = listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB))
private val Indigo       = Color(0xFF3949AB)
private val IndigoSoft   = Color(0xFFE8EAF6)

@Composable
fun AnswerWritingScreen(
    navController: NavHostController,
    viewModel: AnswerWritingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableStateOf(0) }   // 0 = Questions, 1 = My Answers

    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("answer_writing") }
    // Refresh statuses when coming back from the writing screen
    LaunchedEffect(Unit) { if (!state.isLoading) viewModel.load() }

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        state        = pullState,
        isRefreshing = state.isLoading && state.questions.isNotEmpty(),
        onRefresh    = { viewModel.load() }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(cs.background)) {

            // ── Hero header ─────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(HeroGradient, Offset(0f, 0f), Offset(400f, 300f)))
                    .statusBarsPadding()
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStackSafe() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.awTitle, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.awSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                            }
                        }
                        Text("✍️", fontSize = 26.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    // Tab pills
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.12f)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(str.awQuestionsTab, str.awMyAnswersTab).forEachIndexed { i, label ->
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (tab == i) Color.White else Color.Transparent)
                                    .clickable { tab = i }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (tab == i) Indigo else Color.White.copy(0.85f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── Body ────────────────────────────────────────────
            when {
                state.isLoading && state.questions.isEmpty() ->
                    com.example.bpscnotes.core.ui.ListScreenSkeleton(headerHeight = 0.dp, itemCount = 5, itemHeight = 110.dp)

                state.listError != null && state.questions.isEmpty() ->
                    AppErrorState(message = state.listError!!, onRetry = { viewModel.load() })

                tab == 0 -> QuestionsTab(state.questions, navController)
                else     -> MyAnswersTab(state.mySubmissions, navController)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
@Composable
private fun QuestionsTab(questions: List<AnswerQuestionDto>, navController: NavHostController) {
    val str = LocalStrings.current
    val today = questions.firstOrNull { it.isToday }

    if (questions.isEmpty()) {
        EmptyBlock("📝", str.awEmpty, str.awEmptyBody)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today's question — hero card
        if (today != null) {
            item(key = "today") {
                TodayQuestionCard(today) {
                    navController.navigate(Screen.AnswerWritingDetail.createRoute(today.id))
                }
            }
        }
        items(questions.filter { it.id != today?.id }, key = { it.id }) { q ->
            QuestionCard(q) {
                navController.navigate(Screen.AnswerWritingDetail.createRoute(q.id))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TodayQuestionCard(q: AnswerQuestionDto, onClick: () -> Unit) {
    val str = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(HeroGradient, Offset(0f, 0f), Offset(600f, 400f)))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.06f), 110.dp.toPx(), Offset(size.width, 0f))
            drawCircle(Color.White.copy(0.05f), 60.dp.toPx(), Offset(20.dp.toPx(), size.height))
        }
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    str.awTodayBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                q.subject?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.9f), fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                q.questionText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White, fontWeight = FontWeight.Bold,
                lineHeight = 22.sp, maxLines = 4, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroMeta("🏅", "${q.marks} ${str.awMarks}")
                    HeroMeta("📝", "${q.wordLimit} ${str.awWords}")
                }
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (q.isSubmitted) statusLabel(q) else str.awStartWriting,
                        style = MaterialTheme.typography.labelLarge,
                        color = Indigo, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMeta(emoji: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(emoji, fontSize = 12.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun statusLabel(q: AnswerQuestionDto): String {
    val str = LocalStrings.current
    return when (q.myStatus) {
        "reviewed"  -> str.awStatusReviewed
        "submitted" -> str.awStatusPending
        else        -> str.awStatusNew
    }
}

@Composable
private fun QuestionCard(q: AnswerQuestionDto, onClick: () -> Unit) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val (chipBg, chipColor) = when (q.myStatus) {
        "reviewed"  -> Color(0xFFE8FDF4) to BpscColors.Success
        "submitted" -> Color(0xFFFFF8E1) to Color(0xFFB45309)
        else        -> IndigoSoft to Indigo
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    q.subject?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it, style = MaterialTheme.typography.labelSmall,
                            color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IndigoSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    q.scheduledFor?.let {
                        Text(
                            it.take(10), style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextHint, fontSize = 10.sp
                        )
                    }
                }
                Text(
                    if (q.myStatus == "reviewed" && q.myScore != null)
                        "✅ ${q.myScore.toInt()}/${q.marks}"
                    else statusLabel(q),
                    style = MaterialTheme.typography.labelSmall,
                    color = chipColor, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(chipBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                q.questionText,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface, fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                    Text("${q.marks} ${str.awMarks}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.Notes, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                    Text("${q.wordLimit} ${str.awWords}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                if (q.submissionCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Groups, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                        Text("${q.submissionCount}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
@Composable
private fun MyAnswersTab(submissions: List<AnswerSubmissionDto>, navController: NavHostController) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    if (submissions.isEmpty()) {
        EmptyBlock("✍️", str.awNoSubmissions, str.awNoSubmissionsBody)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(submissions, key = { it.id }) { s ->
            Card(
                modifier = Modifier.fillMaxWidth()
                    .clickable { navController.navigate(Screen.AnswerWritingDetail.createRoute(s.questionId)) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Score badge / pending icon
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (s.status == "reviewed") Color(0xFFE8FDF4) else Color(0xFFFFF8E1)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (s.status == "reviewed" && s.score != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${s.score.toInt()}", style = MaterialTheme.typography.titleMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                                Text("/${s.marks}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp)
                            }
                        } else {
                            Text("⏳", fontSize = 20.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            s.questionText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${s.wordCount} ${str.awWords} · ${s.createdAt?.take(10) ?: ""}",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EmptyBlock(emoji: String, title: String, body: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            body, style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 20.sp
        )
    }
}
