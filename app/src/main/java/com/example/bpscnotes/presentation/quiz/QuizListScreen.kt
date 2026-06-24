package com.example.bpscnotes.presentation.quiz

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.quiz.components.LobbyStatChip
import com.example.bpscnotes.presentation.quiz.components.SectionLabel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

/**
 * QuizListScreen — shows all quizzes grouped by type.
 * Reached from: drawer str.quizDaily + "s" / dashboard "See all".
 * Card click → QuizDetailScreen (intro).
 */
@Composable
fun QuizListScreen(
    navController: NavHostController,
    viewModel: QuizViewModel= hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("quiz_list") }

    LaunchedEffect(Unit) {
        // Only reload if list is empty (avoid re-fetching on back navigation)
        if (state.dailyQuizzes.isEmpty() && state.topicQuizzes.isEmpty() && !state.isLoadingList) {
            viewModel.loadLobby()
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = state.isLoadingList && state.allQuizzes.isNotEmpty(),
        onRefresh = { viewModel.loadLobby() }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(cs.background)) {

            // ── Header ─────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            Offset(0f, 0f), Offset(400f, 300f)
                        )
                    )
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier         = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStackSafe() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.quizDaily + "s", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.quizTitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Real user coins
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Text(
                                "${state.userProfile?.coins ?: "--"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Real user stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val p = state.userProfile
                        LobbyStatChip("🎯", if (p != null) "${p.accuracy}%" else "--",    "Accuracy")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🔥", if (p != null) "${p.streak}" else "--",               "Streak")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🏆", if (p?.rank != null) "#${p.rank}" else "--",          "Rank")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("📝", if (p != null) "${p.quizzesAttempted}" else "--",     "Attempted")
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.15f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(17.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (state.searchQuery.isEmpty()) {
                                    Text("Search quizzes…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f))
                                }
                                inner()
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.2f))
                                    .clickable { viewModel.setSearchQuery("") },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Type filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("all" to "All", "daily" to "📅 Daily", "topic" to "📝 Topic", "mock" to "📋 Mock")
                            .forEach { (type, label) ->
                                val selected = state.selectedType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) Color.White else Color.White.copy(0.15f))
                                        .clickable { viewModel.setTypeFilter(type) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color(0xFF0A2472) else Color.White,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                }
                            }
                    }
                }
            }

            // ── Content ────────────────────────────────────────────
            when {
                state.isLoadingList -> {
                    com.example.bpscnotes.core.ui.ListScreenSkeleton(headerHeight = 140.dp, itemCount = 5, itemHeight = 85.dp)
                }
                state.listError != null -> AppErrorState(message = state.listError!!, onRetry = { viewModel.loadLobby() })
                state.dailyQuizzes.isEmpty() && state.topicQuizzes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📝", fontSize = 48.sp)
                            Text(str.quizNoAvailable, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                            Text(str.quizCheckLater, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.dailyQuizzes.isNotEmpty()) {
                            item { SectionLabel("📅 " + str.quizDaily + "s", "Today's scheduled quizzes — resets at midnight") }
                            items(state.dailyQuizzes, key = { it.id }) { quiz ->
                                QuizCard(quiz = quiz) {
                                    if (quiz.totalQuestions == 0) {
                                        android.widget.Toast.makeText(context,
                                            "\"${quiz.title}\" " + str.quizNoQuestions,
                                            android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                                    }
                                }
                            }
                        }
                        if (state.topicQuizzes.isNotEmpty()) {
                            item { Spacer(Modifier.height(4.dp)) }
                            item { SectionLabel("📝 " + str.quizTopic + "s", "Practice specific subjects") }
                            items(state.topicQuizzes, key = { it.id }) { quiz ->
                                QuizCard(quiz = quiz) {
                                    if (quiz.totalQuestions == 0) {
                                        android.widget.Toast.makeText(context,
                                            "\"${quiz.title}\" " + str.quizNoQuestions,
                                            android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // end PullToRefreshBox
}

@Composable
internal fun QuizCard(quiz: QuizPreviewDto, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme

    // Vector icon + tint per quiz type — smaller and crisper than the old
    // 52dp emoji box, and consistent across devices (emoji rendering varies).
    val (typeIcon, typeColor) = when (quiz.type) {
        "daily" -> Icons.Rounded.CalendarToday to Color(0xFF1565C0)
        "topic" -> Icons.Rounded.MenuBook      to Color(0xFF8E44AD)
        "mock"  -> Icons.Rounded.Assignment    to Color(0xFFE67E22)
        else    -> Icons.Rounded.Quiz          to BpscColors.Primary
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Leading icon — 40dp (down from 52dp), with a small "done"
            // badge on the corner so the title row doesn't need its own pill ──
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(20.dp))
                }
                if (quiz.isAttempted) {
                    Box(
                        modifier = Modifier.size(15.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(BpscColors.Success)
                            .border(2.dp, cs.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(9.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                // Title row — now gets the full line to itself (the old "Done"
                // pill that used to eat into this space moved to the icon badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        quiz.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (quiz.coinsReward > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🪙", fontSize = 9.sp)
                            Text("+${quiz.coinsReward}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57F17), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }

                // Subject row — the topic name is now a colored chip of its own
                // instead of small gray text squeezed between Q-count and duration,
                // so it's the second thing your eye lands on after the title.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectChip(quiz.subject)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.HelpOutline, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
                        Text("${quiz.totalQuestions}Q", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Schedule, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
                        Text("${quiz.durationMins}m", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    }
                }

                // Result / negative-marking row — only rendered when there's
                // something to say, so untried quizzes stay a tidy 2-line card
                if (quiz.isAttempted || quiz.negativeMarkingEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (quiz.isAttempted) {
                            val lastScore = quiz.myLastScore ?: quiz.avgScore.toInt()
                            Text(
                                "Last score: $lastScore%",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                        if (quiz.negativeMarkingEnabled) {
                            Text(
                                "⚠️ -${formatMarks(quiz.marksPerWrong)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE74C3C),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                modifier = Modifier.clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFFEE8E8))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
        }
    }
}