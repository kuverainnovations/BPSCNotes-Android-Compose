package com.example.bpscnotes.presentation.mocktests

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.data.remote.api.QuizQuestionDto
import com.example.bpscnotes.presentation.quiz.formatMarks
import com.example.bpscnotes.presentation.quiz.MarkingSchemeRow
import kotlinx.coroutines.*

enum class MockTestType { Full, SubjectWise, PreviousYear, Custom }
enum class QuestionStatus { Unattempted, Attempted, MarkedForReview, AttemptedAndMarked }

data class MockTest(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: MockTestType,
    val totalQuestions: Int,
    val durationMinutes: Int,
    val subject: String?,
    val year: Int? = null,
    val isPaid: Boolean = false,
    // negativeMarking = marks deducted per wrong answer (only meaningful when
    // negativeMarkingEnabled is true). Sourced from the admin-configured
    // quiz.marks_per_wrong — no longer a hardcoded client default.
    val negativeMarkingEnabled: Boolean = false,
    val negativeMarking: Float = 0f,
    val marksPerCorrect: Float = 1f,
    val totalAttempts: Int = 0,
    val averageScore: Float = 0f,
    val isFeatured: Boolean = false,
    val coinsReward: Int = 10,
    /** True when scheduledFor is in the future — test cannot be started yet */
    val isScheduledFuture: Boolean = false,
)

data class MockQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val subject: String,
    val difficulty: String,
    val marks: Float = 1f,
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val score: Float,
    val timeTaken: String,
    val isCurrentUser: Boolean = false,
)




enum class MockTestState { Lobby, Instructions, Active, Analysis, Leaderboard }

private fun QuizPreviewDto.toMockTest(): MockTest {
    // Detect if test is scheduled for a future date (not yet started)
    val isScheduledFuture = scheduledFor?.let { scheduled ->
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val d = sdf.parse(scheduled)
            d != null && d.after(java.util.Date())
        } catch (e: Exception) { false }
    } ?: false

    return MockTest(
        id             = id,
        title          = title,
        subtitle       = buildString {
            append("$totalQuestions Qs · ${durationMins}m")
            if (isScheduledFuture) append(" · Upcoming")
        },
        type           = when (type) {
            "mock"          -> MockTestType.Full
            "topic"         -> MockTestType.SubjectWise
            "previous_year" -> MockTestType.PreviousYear
            else            -> MockTestType.Full
        },
        totalQuestions  = totalQuestions,
        durationMinutes = durationMins,
        subject         = subject.takeIf { it.isNotBlank() },
        isPaid          = false,
        negativeMarkingEnabled = negativeMarkingEnabled,
        negativeMarking         = marksPerWrong.toFloat(),
        marksPerCorrect          = marksPerCorrect.toFloat(),
        totalAttempts   = attemptCount,
        averageScore    = avgScore.toFloat(),
        isFeatured      = false,
        coinsReward     = coinsReward,
        isScheduledFuture = isScheduledFuture
    )
}

/** Maps API QuizQuestionDto → UI MockQuestion. */
private fun QuizQuestionDto.toMockQuestion(): MockQuestion = MockQuestion(
    id       = id,
    question = questionText,
    options  = optionTexts,  // dynamic 2-4 options

    // Backend hides correct answers during active quiz
    correctIndex = when (correctOption?.lowercase()?.trim()) {
        "a" -> 0
        "b" -> 1
        "c" -> 2
        "d" -> 3
        else -> -1
    },

    explanation = explanation ?: "",
    subject     = subject ?: "General",
    difficulty  = difficulty,
)


@Composable
fun MockTestsScreen(
    navController: NavHostController,
    adManager:     com.example.bpscnotes.core.ads.AdManager? = null,
    viewModel:     MockTestsViewModel = hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val context  = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("mock_tests") }
    // FIX: Only show mock tests — exclude daily quizzes which appear in the daily quiz section
    val allTests = remember(state.allTests) {
        state.allTests
            .filter { it.type == "mock" || it.type == "previous_year" || it.type == "topic" }
            .map { it.toMockTest() }
    }

    if (state.isLoading && allTests.isEmpty()) {
        com.example.bpscnotes.core.ui.ListScreenSkeleton(headerHeight = 160.dp, itemCount = 4, itemHeight = 90.dp)
        return
    }

    if (state.error != null && allTests.isEmpty()) {
        com.example.bpscnotes.core.ui.AppErrorState(
            message = state.error ?: "Failed to load tests",
            onRetry = { viewModel.retry() }
        )
        return
    }


    var screenState     by remember { mutableStateOf(MockTestState.Lobby) }
    var selectedTest    by remember { mutableStateOf<MockTest?>(null) }
    var showCustomSheet by remember { mutableStateOf(false) }

    // Test session state
    val userAnswers   = remember { mutableStateMapOf<String, Int>() }
    val bookmarked    = remember { mutableStateListOf<String>() }
    val reviewMarked  = remember { mutableStateListOf<String>() }
    var finalScore    by remember { mutableStateOf(0f) }




    // ── Questions loading overlay ─────────────────────────────
    if (state.isLoadingQuestions) {
        AppLoader(message = str.quizLoadingQ)
    }

    // ── Questions error ───────────────────────────────────────
    if (state.questionsError != null) {
        com.example.bpscnotes.core.ui.AppErrorDialog(
            message      = state.questionsError!!,
            dismissLabel = str.ok,
            onDismiss    = { viewModel.clearQuestions(); screenState = MockTestState.Lobby }
        )
    }

    // ── Submit error ──────────────────────────────────────────
    if (state.submitError != null) {
        com.example.bpscnotes.core.ui.AppErrorDialog(
            message      = "${state.submitError}\n\nYour answers were recorded locally. Please try again.",
            retryLabel   = str.tryAgain,
            dismissLabel = str.goBack,
            onRetry      = { selectedTest?.let { t -> viewModel.submitTest(t.id, userAnswers, 0) } },
            onDismiss    = { viewModel.clearQuestions(); screenState = MockTestState.Lobby }
        )
    }

    when (screenState) {
        MockTestState.Lobby -> {
            // Show ad when user navigates back from mock test section
            androidx.activity.compose.BackHandler {
                if (adManager != null && activity != null)
                    adManager.showInterstitialIfReady(activity) { navController.popBackStackSafe() }
                else navController.popBackStackSafe()
            }
            MockTestLobbyScreen(
                navController   = navController,
                onStartTest     = { test ->
                    selectedTest = test
                    userAnswers.clear(); bookmarked.clear(); reviewMarked.clear()
                    screenState  = MockTestState.Instructions
                },
                onCustomTest    = { showCustomSheet = true },
                viewModel       = viewModel
            )
        }
        MockTestState.Instructions -> selectedTest?.let { test ->
            TestInstructionsScreen(
                test    = test,
                onStart = {
                    // Load real questions from API before going to Active
                    viewModel.loadQuestionsForTest(test.id)
                    screenState = MockTestState.Active
                },
                onBack  = { screenState = MockTestState.Lobby }
            )
        }
        MockTestState.Active -> selectedTest?.let { test ->
            val questions = state.activeQuestions.map { it.toMockQuestion() }
            when {
                state.questionsError != null -> {
                    /* handled by AlertDialog above */
                }
                questions.isEmpty() && !state.isLoadingQuestions -> {
                    // Questions not yet loaded — show waiting state
                    com.example.bpscnotes.core.ui.AppLoader(message = str.quizPreparingQ)
                }
                questions.isNotEmpty() -> {
                    val testStartTime = remember { System.currentTimeMillis() }
                    key(test.id) {
                        ActiveTestScreen(
                            test          = test,
                            questions     = questions,
                            userAnswers   = userAnswers,
                            bookmarked    = bookmarked,
                            reviewMarked  = reviewMarked,
                            onSubmit      = { score ->
                                finalScore = score
                                val elapsed = ((System.currentTimeMillis() - testStartTime) / 1000).toInt()
                                viewModel.submitTest(test.id, userAnswers, elapsed)
                                // Show ad after completing test, then go to Analysis
                                if (adManager != null && activity != null)
                                    adManager.showInterstitialIfReady(activity) { screenState = MockTestState.Analysis }
                                else
                                    screenState = MockTestState.Analysis
                            },
                            onExit = {
                                viewModel.clearQuestions()
                                if (adManager != null && activity != null)
                                    adManager.showInterstitialIfReady(activity) { screenState = MockTestState.Lobby }
                                else
                                    screenState = MockTestState.Lobby
                            }
                        )
                    }
                }
            }
        }
        MockTestState.Analysis -> selectedTest?.let { test ->
            val questions = state.activeQuestions.map { it.toMockQuestion() }
            // Back press from Analysis → show ad → go to Lobby
            BackHandler {
                viewModel.clearQuestions()
                if (adManager != null && activity != null)
                    adManager.showInterstitialIfReady(activity) { screenState = MockTestState.Lobby }
                else
                    screenState = MockTestState.Lobby
            }
            TestAnalysisScreen(
                test              = test,
                questions         = questions,
                userAnswers       = userAnswers,
                score             = finalScore,
                submitResult      = state.submitResult,
                onViewLeaderboard = {
                    selectedTest?.let { t -> viewModel.loadLeaderboard(t.id) }
                    screenState = MockTestState.Leaderboard
                },
                onRetry           = {
                    userAnswers.clear(); bookmarked.clear(); reviewMarked.clear()
                    screenState = MockTestState.Active
                },
                onExit            = {
                    viewModel.clearQuestions()
                    if (adManager != null && activity != null)
                        adManager.showInterstitialIfReady(activity) { screenState = MockTestState.Lobby }
                    else screenState = MockTestState.Lobby
                }
            )
        }
        MockTestState.Leaderboard -> {
            val lbEntries = state.leaderboard.map { e ->
                LeaderboardEntry(
                    rank          = e.rank,
                    name          = e.userName,
                    score         = e.score,
                    // Show "X/Y correct · Xs" format
                    timeTaken     = "${e.correctAnswers}/${e.totalQuestions} · ${e.timeTakenSecs}s",
                    isCurrentUser = e.isCurrentUser
                )
            }
            TestLeaderboardScreen(
                entries           = lbEntries,
                isLoading         = state.isLoadingLeaderboard,
                quizTitle         = selectedTest?.title ?: str.mockLeaderboard,
                errorMessage      = state.leaderboardError,
                onBack            = { screenState = MockTestState.Analysis }
            )
        }
    }

    if (showCustomSheet) {
        CustomTestSheet(
            onDismiss   = { showCustomSheet = false },
            onStart     = { customTest ->
                selectedTest = customTest
                userAnswers.clear(); bookmarked.clear(); reviewMarked.clear()
                showCustomSheet = false
                screenState     = MockTestState.Instructions
            }
        )
    }
}

@Composable
private fun MockTestLobbyScreen(
    navController: NavHostController,
    onStartTest: (MockTest) -> Unit,
    onCustomTest: () -> Unit,
    viewModel: MockTestsViewModel,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current

    val state by viewModel.uiState.collectAsState()

    // FIX: Only show mock tests — exclude daily quizzes which appear in the daily quiz section
    val allTests = remember(state.allTests) {
        state.allTests
            .filter { it.type == "mock" || it.type == "previous_year" || it.type == "topic" }
            .map { it.toMockTest() }
    }
    var selectedType by remember { mutableStateOf<MockTestType?>(null) }

    // Derive tabs dynamically — only show tab if tests of that type exist
    val hasFull     = allTests.any { it.type == MockTestType.Full }
    val hasMini     = allTests.any { it.type == MockTestType.SubjectWise }
    val hasPrevYear = allTests.any { it.type == MockTestType.PreviousYear }

    data class MockTab(val label: String, val type: MockTestType?)
    val tabs = buildList {
        // "All" only earns its place if it aggregates 2+ of the tabs below -
        // otherwise it's identical to (and confusingly duplicates) the single tab that exists
        if (listOf(hasFull, hasMini, hasPrevYear).count { it } > 1) add(MockTab(str.filterAll, null))
        if (hasFull)     add(MockTab(str.quizFullMock, MockTestType.Full))
        if (hasMini)     add(MockTab("Mini Tests", MockTestType.SubjectWise))
        if (hasPrevYear) add(MockTab(str.quizPrevYear, MockTestType.PreviousYear))
    }

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset(0f, 0f), Offset(400f, 400f)
                    ))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
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
                            ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            Column {
                                Text(str.quizMock + "s", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.quizPracticeReal, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Custom test button
                        /* Box(
                             modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                 .background(Color.White.copy(0.15f))
                                 .clickable(onClick = onCustomTest)
                                 .padding(horizontal = 12.dp, vertical = 8.dp)
                         ) {
                             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                 Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                 Text(str.mockCreateCustom, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                             }
                         }*/
                    }

                    Spacer(Modifier.height(14.dp))

                    // Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LobbyChip("📝", "${allTests.size}", "Tests")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyChip("✅", state.userQuizzesAttempted.toString(), "Attempted")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyChip("🏆", "#${state.userRank ?: "--"}", "Best Rank")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyChip("📊", "${state.userAccuracy.toInt()}%", "Best Score")
                    }

                    Spacer(Modifier.height(12.dp))

                    // Type filter tabs — dynamic from available test types
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(tabs) { tab ->
                            val sel = selectedType == tab.type
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (sel) Color.White else Color.White.copy(0.15f))
                                    .clickable {
                                        selectedType = when {
                                            tab.type == null -> null
                                            tab.type == selectedType -> null
                                            else -> tab.type
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(tab.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sel) BpscColors.Primary else Color.White,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            val filtered = if (selectedType == null) {
                allTests
            } else {
                allTests.filter { it.type == selectedType }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Explain Full Mock when that filter is active
                if (selectedType == MockTestType.Full) {
                    item(key = "full_mock_info") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BpscColors.PrimaryLight.copy(0.5f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📋", fontSize = 20.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("What is a Full Mock Test?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, color = BpscColors.Primary)
                                Text("Simulates the complete BPSC exam — full syllabus, full question count, timed. Best taken as exam practice after completing your preparation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BpscColors.TextSecondary)
                            }
                        }
                    }
                }
                // Empty state — no tests available
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillParentMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(80.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) { Text("📝", fontSize = 36.sp) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        if (selectedType != null) str.quizNoTestsCategory
                                        else str.quizNoTestsYet,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = cs.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        str.quizTestsComingSoon,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    return@LazyColumn   // hide all section headers when no data
                }
                // Featured tests
                val featured = filtered.filter { it.isFeatured }
                if (featured.isNotEmpty() && selectedType == null) {
                    item {
                        Text(str.quizFeatured, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                    }
                    items(featured) { test ->
                        MockTestCard(test = test, isFeatured = true, onStart = { onStartTest(test) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                // All tests
                item {
                    Text(
                        when (selectedType) {
                            MockTestType.Full         -> str.quizFullMock
                            MockTestType.SubjectWise  -> str.quizMiniTest
                            MockTestType.PreviousYear -> str.quizPrevYear
                            null                      -> str.quizAllTests
                            else                      -> "Tests"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                items(if (selectedType == null) filtered.filter { !it.isFeatured } else filtered) { test ->
                    MockTestCard(test = test, isFeatured = false, onStart = { onStartTest(test) })
                }
            }
        }
    }
}

@Composable
private fun LobbyChip(icon: String, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(60.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

@Composable
private fun MockTestCard(test: MockTest, isFeatured: Boolean, onStart: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val typeColor = when (test.type) {
        MockTestType.Full         -> Pair(Color(0xFF1565C0), Color(0xFFE8F0FD))
        MockTestType.SubjectWise  -> Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4))
        MockTestType.PreviousYear -> Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD))
        MockTestType.Custom       -> Pair(Color(0xFFE67E22), Color(0xFFFFF0EA))
    }
    val typeLabel = when (test.type) {
        MockTestType.Full         -> str.quizFullMock
        MockTestType.SubjectWise  -> str.quizMiniTest
        MockTestType.PreviousYear -> str.quizPrevYear
        MockTestType.Custom       -> "Custom"
    }

    // Determine card state
    val hasNoQuestions = test.totalQuestions == 0
    val isClickable    = !test.isScheduledFuture && !hasNoQuestions

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(enabled = isClickable, onClick = onStart),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isFeatured) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(typeColor.second),
                    contentAlignment = Alignment.Center
                ) {
                    Text(when (test.type) {
                        MockTestType.Full -> "📋"; MockTestType.SubjectWise -> "📝"
                        MockTestType.PreviousYear -> "📅"; else -> "⚙️"
                    }, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Title row — coins pinned to the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            test.title,
                            style    = MaterialTheme.typography.titleMedium,
                            color    = if (hasNoQuestions) cs.onSurfaceVariant else cs.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (test.coinsReward > 0 && !hasNoQuestions && !test.isScheduledFuture) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFF8E1))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🪙", fontSize = 9.sp)
                                Text("+${test.coinsReward}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57F17), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(test.subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Type badge
                        Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = typeColor.first, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(typeColor.second).padding(horizontal = 6.dp, vertical = 2.dp))
                        // Free/Paid
                        if (test.isPaid) {
                            Text("PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 6.dp, vertical = 2.dp))
                        } else {
                            Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        // Negative marking — only shown when the admin actually enabled it for this test
                        if (test.negativeMarkingEnabled) {
                            Text("⚠️ -${formatMarks(test.negativeMarking.toDouble())}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontSize = 9.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (test.year != null) {
                            Text("${test.year}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                // Right chevron — or lock/clock/no-questions icon
                Icon(
                    imageVector = when {
                        test.isScheduledFuture -> Icons.Rounded.Schedule
                        hasNoQuestions         -> Icons.Rounded.LockClock
                        else                   -> Icons.Rounded.ChevronRight
                    },
                    contentDescription = null,
                    tint     = when {
                        test.isScheduledFuture -> BpscColors.TextHint
                        hasNoQuestions         -> BpscColors.TextHint
                        else                   -> BpscColors.TextHint
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = cs.outline)

            // Stats row + status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat(Icons.Rounded.People, "${(test.totalAttempts / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${test.totalAttempts}" }}", "Attempts")
                    MiniStat(Icons.Rounded.BarChart, "${test.averageScore.toInt()}%", str.quizAvgScore)
                    MiniStat(Icons.Rounded.Timer, "${test.durationMinutes}m", str.quizDuration)
                }
                // Status chip — replaces the Start button
                when {
                    test.isScheduledFuture -> Text("🕐 Soon", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextHint, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(cs.background).padding(horizontal = 8.dp, vertical = 4.dp))
                    hasNoQuestions -> Text("No Questions", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 8.dp, vertical = 4.dp))
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(BpscColors.PrimaryLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Tap to view", style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.Primary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
        Column {
            Text(value, style = MaterialTheme.typography.labelSmall, color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// INSTRUCTIONS SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun TestInstructionsScreen(
    test: MockTest,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding().padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFF57F17).copy(0.25f)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🪙", fontSize = 14.sp)
                            Text("+${test.coinsReward} Coins", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(test.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(test.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Test info grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(str.quizTestOverview, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(140.dp)
                        ) {
                            item { InfoTile("📝", str.quizQuestions, "${test.totalQuestions}") }
                            item { InfoTile("⏱️", str.quizDuration, "${test.durationMinutes} min") }
                            item { InfoTile("✅", "Marks/Q",  "+${formatMarks(test.marksPerCorrect.toDouble())}") }
                            item {
                                if (test.negativeMarkingEnabled)
                                    InfoTile("❌", "Negative",  "-${formatMarks(test.negativeMarking.toDouble())}")
                                else
                                    InfoTile("➖", "Negative",  "None")
                            }
                        }
                    }
                }

                // Marking scheme — full breakdown so the rules are unambiguous
                // before the user commits to starting the test.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Marking Scheme", style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = cs.outline)
                        MarkingSchemeRow("Total Marks", formatMarks(test.totalQuestions * test.marksPerCorrect.toDouble()))
                        MarkingSchemeRow("Correct Answer", "+${formatMarks(test.marksPerCorrect.toDouble())} Marks", valueColor = BpscColors.Success)
                        MarkingSchemeRow(
                            "Wrong Answer",
                            if (test.negativeMarkingEnabled) "-${formatMarks(test.negativeMarking.toDouble())} Marks" else "0 Marks",
                            valueColor = if (test.negativeMarkingEnabled) Color(0xFFE74C3C) else cs.onSurface
                        )
                        MarkingSchemeRow("Unanswered Questions", "0 Marks")
                        if (test.negativeMarkingEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEE8E8))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(
                                    "Negative Marking Applicable: ${formatMarks(test.negativeMarking.toDouble())} marks will be deducted for every incorrect answer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0392B),
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(str.mockInstructions, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        buildList {
                            if (test.negativeMarkingEnabled) {
                                add(Triple("✅", "Marking Scheme", "Each correct answer carries +${formatMarks(test.marksPerCorrect.toDouble())} marks; each incorrect answer carries -${formatMarks(test.negativeMarking.toDouble())} marks. No marks are deducted for unanswered questions."))
                            }
                            add(Triple("🟢", "Attempted",            "Questions you have answered"))
                            add(Triple("🔵", "Marked for Review",    "Questions you want to revisit"))
                            add(Triple("⚪", "Unattempted",          "Questions not yet answered"))
                            add(Triple("🟡", "Attempted + Marked",   "Answered but flagged for review"))
                            add(Triple("📖", str.quizNavTitle,   "Tap the grid icon to jump to any question"))
                            add(Triple("🔖", "Bookmark",             "Save important questions for later"))
                            add(Triple("⏰", "Auto Submit",          "Test submits automatically when timer ends"))
                            add(Triple("↩️",  "Resume",              str.quizCanResume))
                        }.forEach { (emoji, title, desc) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Start button
            Box(modifier = Modifier.fillMaxWidth().background(cs.surface).padding(20.dp)) {
                Button(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.quizStartTest, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun InfoTile(icon: String, label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(cs.background).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACTIVE TEST SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun ActiveTestScreen(
    test: MockTest,
    questions: List<MockQuestion>,
    userAnswers: MutableMap<String, Int>,
    bookmarked: MutableList<String>,
    reviewMarked: MutableList<String>,
    onSubmit: (Float) -> Unit,
    onExit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var currentIndex     by remember { mutableIntStateOf(0) }
    var timeLeft         by remember { mutableIntStateOf(test.durationMinutes * 60) }
    var showNavigator    by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showQuitDialog   by remember { mutableStateOf(false) }

    // Intercept system back — show quit dialog
    BackHandler { showQuitDialog = true }

    // Quit confirmation dialog
    if (showQuitDialog) {
        com.example.bpscnotes.core.ui.AppQuitDialog(
            title     = "Quit Mock Test?",
            body      = "Your answers will be lost. Are you sure you want to quit?",
            quitLabel = str.quizQuit,
            keepLabel = str.quizKeepGoing,
            onConfirm = { showQuitDialog = false; onExit() },
            onDismiss = { showQuitDialog = false }
        )
    }
    val current         = questions.getOrNull(currentIndex)

    var submitClicked by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        // Auto submit — guard against double submit
        if (!submitClicked) {
            submitClicked = true
            onSubmit(0f)  // score ignored — server calculates and returns real score
        }
    }

    val attempted     = userAnswers.size
    val marked        = reviewMarked.size
    val unattempted   = questions.size - attempted

    val hours   = timeLeft / 3600
    val minutes = (timeLeft % 3600) / 60
    val seconds = timeLeft % 60
    val timeStr = if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    val isLowTime = timeLeft < 300 // less than 5 min

    if (current == null) return

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Test header ──────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1250B0))))
                    .statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                            .clickable { showQuitDialog = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Timer
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (isLowTime) Color(0xFFE74C3C) else Color.White.copy(0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(timeStr, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }

                        // Coins + Navigator + Submit
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFF57F17).copy(0.3f)).padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🪙", fontSize = 10.sp)
                                Text("+${test.coinsReward}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold)
                            }
                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).clickable { showNavigator = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(cs.surface).clickable { showSubmitDialog = true }.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(str.submit, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Status counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusChip("🟢", "$attempted",   "Answered")
                        StatusChip("🔵", "$marked",      str.quizReview)
                        StatusChip("⚪", "$unattempted", "Pending")
                        StatusChip("📊", "${currentIndex + 1}/${questions.size}", "Current")
                    }

                    // Progress bar
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth((currentIndex + 1f) / questions.size).fillMaxHeight().background(Color.White, RoundedCornerShape(2.dp)))
                    }
                }
            }

            // ── Question ─────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Q number + subject + bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) { Text("${currentIndex + 1}", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                        SubjectBadge(current.subject)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Mark for review
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (reviewMarked.contains(current.id)) Color(0xFFE8F0FD) else BpscColors.Surface)
                                .clickable {
                                    if (reviewMarked.contains(current.id)) reviewMarked.remove(current.id)
                                    else reviewMarked.add(current.id)
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Flag, null, tint = if (reviewMarked.contains(current.id)) BpscColors.Primary else BpscColors.TextHint, modifier = Modifier.size(16.dp)) }
                        // Bookmark
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (bookmarked.contains(current.id)) Color(0xFFFFF8E1) else BpscColors.Surface)
                                .clickable {
                                    if (bookmarked.contains(current.id)) bookmarked.remove(current.id)
                                    else bookmarked.add(current.id)
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(
                            if (bookmarked.contains(current.id)) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            null, tint = if (bookmarked.contains(current.id)) BpscColors.CoinGold else BpscColors.TextHint,
                            modifier = Modifier.size(16.dp)) }
                    }
                }

                // Question text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        current.question,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }

                // Options
                current.options.forEachIndexed { index, option ->
                    if (option.isBlank()) return@forEachIndexed
                    val isSelected = userAnswers[current.id] == index
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) BpscColors.PrimaryLight else Color.White)
                            .border(1.5.dp, if (isSelected) BpscColors.Primary else cs.outline, RoundedCornerShape(14.dp))
                            .clickable {
                                if (userAnswers[current.id] == index) userAnswers.remove(current.id)
                                else userAnswers[current.id] = index
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (isSelected) BpscColors.Primary else BpscColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("A","B","C","D")[index], style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else BpscColors.TextSecondary, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(option, style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary,
                            modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Navigation ───────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(cs.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { if (currentIndex > 0) currentIndex-- },
                        enabled  = currentIndex > 0,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, cs.outline),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prev", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick  = { if (currentIndex < questions.size - 1) currentIndex++ else showSubmitDialog = true },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Text(
                            if (currentIndex < questions.size - 1) str.quizSaveNext else str.quizSubmitTest,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Question navigator overlay
        if (showNavigator) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { showNavigator = false }, contentAlignment = Alignment.BottomCenter) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                        Text(str.quizNavTitle, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(Triple("🟢","Answered", BpscColors.Success), Triple("🔵",str.quizReview, BpscColors.Primary), Triple("⚪","Pending", BpscColors.TextHint)).forEach { (e, l, c) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(e, fontSize = 11.sp)
                                    Text(l, style = MaterialTheme.typography.labelSmall, color = c, fontSize = 9.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            itemsIndexed(questions) { index, q ->
                                val isAnswered = userAnswers.containsKey(q.id)
                                val isReview   = reviewMarked.contains(q.id)
                                val isCurrent  = index == currentIndex
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(when { isCurrent -> BpscColors.Primary; isAnswered && isReview -> Color(0xFF3498DB); isAnswered -> BpscColors.Success.copy(0.2f); isReview -> BpscColors.Primary.copy(0.2f); else -> BpscColors.Surface })
                                        .border(if (isCurrent) 2.dp else 0.dp, BpscColors.Primary, RoundedCornerShape(8.dp))
                                        .clickable { currentIndex = index; showNavigator = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent || isAnswered) BpscColors.Primary else BpscColors.TextSecondary,
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Submit dialog
        if (showSubmitDialog) {
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                containerColor   = Color.White,
                shape            = RoundedCornerShape(20.dp),
                title = { Text(str.quizSubmitTestTitle, fontWeight = FontWeight.Bold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Answered: $attempted / ${questions.size}", style = MaterialTheme.typography.bodyLarge)
                        if (unattempted > 0) Text("⚠️ $unattempted questions unanswered", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE74C3C))
                        if (marked > 0) Text("🔵 $marked marked for review", style = MaterialTheme.typography.bodyLarge, color = BpscColors.Primary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitDialog = false
                            if (!submitClicked) {
                                submitClicked = true
                                onSubmit(0f)  // score ignored — server calculates real score
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                        shape  = RoundedCornerShape(10.dp)
                    ) { Text(str.submit) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSubmitDialog = false }, shape = RoundedCornerShape(10.dp)) { Text(str.quizReview) }
                }
            )
        }
    }
}

@Composable
private fun StatusChip(icon: String, value: String, label: String) {
    val str = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(icon, fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 8.sp)
    }
}

@Composable
private fun SubjectBadge(subject: String) {
    val str = LocalStrings.current
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun DiffBadge(difficulty: String) {
    val str = LocalStrings.current
    val color = when (difficulty) { "Easy" -> Color(0xFF2ECC71); "Hard" -> Color(0xFFE74C3C); else -> Color(0xFFF39C12) }
    Text(difficulty, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(0.1f)).padding(horizontal = 7.dp, vertical = 2.dp))
}

// ─────────────────────────────────────────────────────────────
// ANALYSIS SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun TestAnalysisScreen(
    test: MockTest,
    questions: List<MockQuestion>,
    userAnswers: Map<String, Int>,
    score: Float,
    submitResult: com.example.bpscnotes.data.remote.api.QuizResultData? = null,
    onViewLeaderboard: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val resultAnswers = submitResult?.answers ?: emptyList()

    // Prefer the authoritative backend counts — `resultAnswers` includes BOTH
    // attempted and skipped questions (skipped ones carry answer="", isCorrect=false),
    // so deriving wrong/skipped purely from isCorrect would wrongly count every
    // skipped question as "wrong". submitResult.wrong/unanswered already separate
    // these correctly server-side.
    val correct = submitResult?.correct ?: resultAnswers.count { it.isCorrect }
    val wrong   = submitResult?.wrong   ?: resultAnswers.count { !it.isCorrect && it.answer.isNotBlank() }
    val skipped = submitResult?.unanswered ?: (questions.size - resultAnswers.count { it.answer.isNotBlank() })

    val percentage = submitResult?.score ?: 0
    val rank       = submitResult?.rank
    val percentile = submitResult?.percentile
    val coinsEarned  = submitResult?.coinsEarned ?: 0

    val animProg   by animateFloatAsState(percentage / 100f, tween(1200), label = "ap")
    // Subject-wise breakdown
    val subjects   = questions.map { it.subject }.distinct()
    val subjectStats = subjects.map { sub ->
        val subQs      = questions.filter { it.subject == sub }
        val subCorrect = subQs.count { userAnswers[it.id] == it.correctIndex }
        Triple(sub, subCorrect, subQs.size)
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(24.dp))

            Text(if (percentage >= 80) "🏆" else if (percentage >= 50) "👍" else "💪", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { percentage >= 80 -> "Outstanding!"; percentage >= 60 -> "Well Done!"; percentage >= 40 -> "Good Effort!"; else -> str.quizKeepPracticing },
                style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(test.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(20.dp))

            // Score ring
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, animProg * 360f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$percentage%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(str.quizScore, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Rank + percentile
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surface.copy(0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 24.sp)
                        Text("#$rank", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(str.quizYourRank, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surface.copy(0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📈", fontSize = 24.sp)
                        Text("${percentile}%", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(str.mockPercentile, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Coins earned banner (show even if 0 so user knows why no coins)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = if (coinsEarned > 0) Color(0xFFFFF8E1) else Color.White.copy(0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🪙", fontSize = 28.sp)
                        Column {
                            Text(
                                if (coinsEarned > 0) str.quizCoinsEarned2 else str.quizNoCoins,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (coinsEarned > 0) Color(0xFF5D4037) else Color.White.copy(0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (coinsEarned > 0) str.quizAddedWallet else str.quizAlreadyEarned,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (coinsEarned > 0) Color(0xFF8D6E63) else Color.White.copy(0.5f)
                            )
                        }
                    }
                    Text(
                        if (coinsEarned > 0) "+$coinsEarned" else "0",
                        style     = MaterialTheme.typography.headlineSmall,
                        color     = if (coinsEarned > 0) Color(0xFFF57F17) else Color.White.copy(0.4f),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AnalysisStat("✅", "$correct",        str.quizCorrect,   BpscColors.Success)
                    AnalysisStat("❌", "$wrong",          str.quizWrong,     Color(0xFFE74C3C))
                    AnalysisStat("⏭️", "$skipped",        "Skipped",   BpscColors.TextSecondary)
                    AnalysisStat("🪙", if (coinsEarned > 0) "+$coinsEarned" else "0", "Coins", Color(0xFFF57F17))
                }
            }

            // ── Marks breakdown — only for tests with negative marking enabled ──
            if (submitResult?.negativeMarkingEnabled == true) {
                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Marks Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        HorizontalDivider(color = cs.outline)
                        MarkingSchemeRow("Correct Answers", "$correct")
                        MarkingSchemeRow("Marks Earned", "+${formatMarks(submitResult.marksObtained)}", valueColor = BpscColors.Success)
                        MarkingSchemeRow("Wrong Answers", "$wrong")
                        MarkingSchemeRow("Negative Marks", "-${formatMarks(submitResult.negativeMarks)}", valueColor = Color(0xFFE74C3C))
                        HorizontalDivider(color = cs.outline)
                        MarkingSchemeRow("Final Score", "${formatMarks(submitResult.finalScore)} / ${formatMarks(submitResult.totalMarks)}", valueColor = BpscColors.Primary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Subject breakdown
            /*    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(str.quizSubjectAnalysis, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        subjectStats.forEach { (subject, correct, total) ->
                            val pct = if (total > 0) correct.toFloat() / total else 0f
                            val animSubProg by animateFloatAsState(pct, tween(1000), label = "sub$subject")
                            val subColors = mapOf("Polity" to Color(0xFF9B59B6), "History" to Color(0xFFE74C3C), "Geography" to Color(0xFF1ABC9C),
                                "Economy" to Color(0xFFE67E22), "Bihar GK" to Color(0xFFF39C12), "Science" to Color(0xFF2ECC71))
                            val color = subColors[subject] ?: BpscColors.Primary
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(subject, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                                    Text("$correct/$total", style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(cs.background)) {
                                    Box(modifier = Modifier.fillMaxWidth(animSubProg).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    }
                }*/

            Spacer(Modifier.height(16.dp))

            // Buttons
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onViewLeaderboard, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.Leaderboard, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.quizViewLeaderboard, style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.4f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.quizRetryTest, style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.8f))) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.quizBackToTests, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnalysisStat(icon: String, value: String, label: String, color: Color) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 18.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// LEADERBOARD SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun TestLeaderboardScreen(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean = false,
    quizTitle: String = "",  // caller supplies; str not available in default params
    errorMessage: String? = null,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding().padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(str.mockLeaderboard, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${entries.size} participants", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }

        // Top 3 podium
        Row(
            modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color(0xFF1565C0), BpscColors.Surface))
            ).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            entries.getOrNull(1)?.let { PodiumItem(it, 2, 80.dp) }
            entries.getOrNull(0)?.let { PodiumItem(it, 1, 100.dp) }
            entries.getOrNull(2)?.let { PodiumItem(it, 3, 65.dp) }
        }

        // Full list
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (entry.isCurrentUser) BpscColors.PrimaryLight else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(if (entry.isCurrentUser) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Rank
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(when (entry.rank) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> BpscColors.Surface }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (entry.rank <= 3) listOf("🥇","🥈","🥉")[entry.rank - 1] else "#${entry.rank}",
                                fontSize = if (entry.rank <= 3) 16.sp else 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(entry.name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                                if (entry.isCurrentUser) Text(str.focusYou, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("Time: ${entry.timeTaken}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                        Text("${entry.score}", style = MaterialTheme.typography.titleLarge, color = if (entry.isCurrentUser) BpscColors.Primary else BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumItem(entry: LeaderboardEntry, position: Int, height: Dp) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val medalColors = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(listOf("🥇","🥈","🥉")[position - 1], fontSize = 28.sp)
        Text(entry.name.split(" ").first(), style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("${entry.score}", style = MaterialTheme.typography.titleMedium, color = medalColors[position - 1], fontWeight = FontWeight.ExtraBold)
        Box(modifier = Modifier.width(70.dp).height(height).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(medalColors[position - 1].copy(0.3f)))
    }
}

// ─────────────────────────────────────────────────────────────
// CUSTOM TEST SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTestSheet(
    onDismiss: () -> Unit,
    onStart: (MockTest) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val allSubjects    = listOf("Polity", "History", "Geography", "Economy", "Bihar GK", "Science")
    val selectedSubs   = remember { mutableStateListOf<String>() }
    var questionCount  by remember { mutableIntStateOf(30) }
    var durationMins   by remember { mutableIntStateOf(45) }
    var negativeMarking by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(str.quizCreateCustom, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)

            // Subject selection
            Text(str.quizSelectSubjects, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSubjects) { sub ->
                    val sel = selectedSubs.contains(sub)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                            .border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
                            .clickable { if (sel) selectedSubs.remove(sub) else selectedSubs.add(sub) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(sub, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // Question count slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(str.quizQuestions, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Text("$questionCount", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                }
                Slider(value = questionCount.toFloat(), onValueChange = { questionCount = it.toInt() },
                    valueRange = 10f..100f, steps = 8,
                    colors = SliderDefaults.colors(thumbColor = BpscColors.Primary, activeTrackColor = BpscColors.Primary))
            }

            // Duration slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(str.quizDuration, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Text("$durationMins min", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                }
                Slider(value = durationMins.toFloat(), onValueChange = { durationMins = it.toInt() },
                    valueRange = 15f..180f, steps = 10,
                    colors = SliderDefaults.colors(thumbColor = BpscColors.Primary, activeTrackColor = BpscColors.Primary))
            }

            // Negative marking toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(str.quizNegativeMarking, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Text(str.mockNegativeMarking, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Switch(
                    checked = negativeMarking, onCheckedChange = { negativeMarking = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BpscColors.Primary)
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val customTest = MockTest(
                        id = "custom_${System.currentTimeMillis()}",
                        title = str.quizCustomTest,
                        subtitle = "$questionCount Questions · ${durationMins} min · ${if (selectedSubs.isEmpty()) "All Subjects" else selectedSubs.joinToString(", ")}",
                        type = MockTestType.Custom,
                        totalQuestions = questionCount,
                        durationMinutes = durationMins,
                        subject = if (selectedSubs.size == 1) selectedSubs.first() else null,
                        isPaid = false,
                        negativeMarking = if (negativeMarking) 0.33f else 0f
                    )
                    onStart(customTest)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(str.quizStartCustom, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SCORE CALCULATOR
// ─────────────────────────────────────────────────────────────
private fun calculateScore(
    questions: List<MockQuestion>,
    userAnswers: Map<String, Int>,
    negativeMarking: Float
): Float {
    var score = 0f
    questions.forEach { q ->
        val answer = userAnswers[q.id]
        when {
            answer == null                    -> {} // skipped — no marks
            answer == q.correctIndex          -> score += q.marks
            else                              -> score -= negativeMarking
        }
    }
    return score.coerceAtLeast(0f)
}