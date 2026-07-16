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
                        listOf(str.awQuestionsTab, str.awMyAnswersTab, str.awInsightsTab).forEachIndexed { i, label ->
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
                tab == 1 -> MyAnswersTab(state.mySubmissions, navController)
                else     -> InsightsTab(state.insights, state.reviewStats)
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
        // Peer review card — review others' answers, earn credits
        item(key = "peer_review") { PeerReviewCard(navController) }

        items(questions.filter { it.id != today?.id }, key = { it.id }) { q ->
            QuestionCard(q) {
                navController.navigate(Screen.AnswerWritingDetail.createRoute(q.id))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// PEER REVIEW CARD — stats + Review Now, or the reason it's locked.
// ─────────────────────────────────────────────────────────────
@Composable
private fun PeerReviewCard(
    navController: NavHostController,
    viewModel: AnswerWritingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val stats = state.reviewStats ?: return
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    Card(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Groups, null, tint = BpscColors.Success, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(str.awPeerReview, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                        Text(
                            str.awPeerReviewSub, style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant, lineHeight = 16.sp, fontSize = 11.sp
                        )
                    }
                }
                if (stats.canReview) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BpscColors.Success, RoundedCornerShape(12.dp))
                            .clickable { navController.navigate(Screen.PeerReview.route) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(str.awReviewNow, style = MaterialTheme.typography.labelMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (!stats.canReview) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF8E1)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔒", fontSize = 13.sp)
                    Text(
                        if (stats.lockedReason == "no_submission") str.awReviewLockedNoSub else str.awReviewLockedNotReviewed,
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A5B00), lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = cs.outline.copy(0.3f))
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                PeerStat("${stats.reviewsGiven}", str.awReviewsGiven, BpscColors.Success, Modifier.weight(1f))
                PeerStat("${stats.pendingAvailable}", str.awPendingReviews, Indigo, Modifier.weight(1f))
                PeerStat("${stats.reviewCredits} ⭐", str.awReviewCredits, Color(0xFF7E57C2), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PeerStat(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        "reviewed", "peer_reviewed" -> str.awStatusReviewed
        "submitted"                 -> str.awStatusPending
        else                        -> str.awStatusNew
    }
}

@Composable
private fun QuestionCard(q: AnswerQuestionDto, onClick: () -> Unit) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val (chipBg, chipColor) = when (q.myStatus) {
        "reviewed", "peer_reviewed" -> Color(0xFFE8FDF4) to BpscColors.Success
        "submitted"                 -> Color(0xFFFFF8E1) to Color(0xFFB45309)
        else                        -> IndigoSoft to Indigo
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

// ─────────────────────────────────────────────────────────────
// INSIGHTS TAB — personal writing & review stats (client mockup 3,
// personal slice; community leaderboards come later).
// ─────────────────────────────────────────────────────────────
@Composable
private fun InsightsTab(
    insights: com.example.bpscnotes.data.remote.api.AnswerInsightsData?,
    reviewStats: com.example.bpscnotes.data.remote.api.ReviewStatsData?,
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    if (insights == null || insights.answersWritten == 0) {
        EmptyBlock("📊", str.awInsightsTitle, str.awNoInsights)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(str.awInsightsTitle, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Text(str.awInsightsSub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

        // ── Stat tiles (2 × 4 grid) ─────────────────────────────
        val tiles = listOf(
            InsightTileData("📝", "${insights.answersWritten}", str.awAnswersWritten,
                if (insights.answersThisMonth > 0) "↑ ${insights.answersThisMonth} ${str.awThisMonth}" else null, Indigo),
            InsightTileData("💬", "${insights.reviewsGiven}", str.awReviewsGiven, null, BpscColors.Success),
            InsightTileData("💌", "${insights.reviewsReceived}", str.awReviewsReceived, null, Color(0xFFE91E63)),
            InsightTileData("⭐", insights.avgRating?.let { "%.1f".format(it) } ?: "—", str.awAvgRating,
                insights.avgRating?.let { if (it >= 4.0) str.awKeepItUp else null }, Color(0xFFF59E0B)),
            InsightTileData("🔥", "${insights.writingStreak}", "${str.awWritingStreak} (${str.awDays})",
                if (insights.writingStreak > 0) str.awKeepItUp else null, Color(0xFFFF5722)),
            InsightTileData("🏅", insights.avgMentorScore?.let { "%.1f".format(it) } ?: "—", str.awMentorScore, null, Color(0xFF7E57C2)),
            InsightTileData("🪙", "${insights.reviewCredits}", str.awReviewCredits, null, Color(0xFFB45309)),
            InsightTileData("✍️", "${insights.totalWords}", str.awTotalWords, null, Color(0xFF00897B)),
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t -> InsightTile(t, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // ── Monthly goal progress ───────────────────────────────
        val goalProgress = (insights.answersThisMonth.toFloat() / insights.monthlyGoal).coerceIn(0f, 1f)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎯", fontSize = 18.sp)
                        Column {
                            Text(str.awGoalTitle, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                            Text(str.awGoalBody, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Text(
                        "${insights.answersThisMonth} / ${insights.monthlyGoal}",
                        style = MaterialTheme.typography.titleSmall, color = Indigo, fontWeight = FontWeight.ExtraBold
                    )
                }
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Indigo,
                    trackColor = IndigoSoft,
                )
                if (goalProgress >= 1f) {
                    Text(str.awGoalDone, style = MaterialTheme.typography.labelMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // ── Reviewer level ──────────────────────────────────────
        val levelInfo = when {
            insights.reviewsGiven >= 50 -> Triple(str.awLevelExpert, "🏆", null)
            insights.reviewsGiven >= 20 -> Triple(str.awLevelAdvanced, "🥇", 50)
            insights.reviewsGiven >= 5  -> Triple(str.awLevelActive, "🥈", 20)
            else                        -> Triple(str.awLevelBeginner, "🌱", 5)
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoSoft.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(levelInfo.second, fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(str.awReviewerLevel, style = MaterialTheme.typography.labelSmall, color = Indigo, fontWeight = FontWeight.Bold)
                    Text(levelInfo.first, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                    levelInfo.third?.let { nextAt ->
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (insights.reviewsGiven.toFloat() / nextAt).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = Indigo, trackColor = Color.White,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${insights.reviewsGiven} / $nextAt",
                            style = MaterialTheme.typography.labelSmall, color = Indigo, fontWeight = FontWeight.Bold, fontSize = 10.sp
                        )
                    }
                }
                if (reviewStats?.canReview == true) {
                    Text(
                        "⭐ ${reviewStats.reviewCredits}",
                        style = MaterialTheme.typography.titleSmall, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF8E1))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private data class InsightTileData(
    val emoji: String, val value: String, val label: String, val note: String?, val color: Color,
)

@Composable
private fun InsightTile(t: InsightTileData, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.emoji, fontSize = 14.sp)
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp, maxLines = 1)
            }
            Text(t.value, style = MaterialTheme.typography.headlineSmall, color = t.color, fontWeight = FontWeight.ExtraBold)
            t.note?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 10.sp)
            }
        }
    }
}
