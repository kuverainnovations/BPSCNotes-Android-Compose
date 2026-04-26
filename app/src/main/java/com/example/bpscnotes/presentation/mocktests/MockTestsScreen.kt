package com.example.bpscnotes.presentation.mocktests

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
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
    val subject: String?,          // null = all subjects
    val year: Int? = null,          // for previous year papers
    val isPaid: Boolean = false,
    val negativeMarking: Float = 0.33f,
    val totalAttempts: Int = 0,
    val averageScore: Float = 0f,
    val isFeatured: Boolean = false,
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

val mockTests = listOf(
    MockTest("mt1", "BPSC 69th Full Mock Test", "100 Questions · 2 Hours · All Subjects",
        MockTestType.Full, 100, 120, null, isPaid = false, totalAttempts = 12450, averageScore = 62f, isFeatured = true),
    MockTest("mt2", "BPSC 68th Full Mock Test", "100 Questions · 2 Hours · All Subjects",
        MockTestType.Full, 100, 120, null, isPaid = true, totalAttempts = 8900, averageScore = 58f),
    MockTest("mt3", "Polity Mini Test", "30 Questions · 45 Minutes",
        MockTestType.SubjectWise, 30, 45, "Polity", isPaid = false, totalAttempts = 5200, averageScore = 68f),
    MockTest("mt4", "History Mini Test", "25 Questions · 35 Minutes",
        MockTestType.SubjectWise, 25, 35, "History", isPaid = false, totalAttempts = 4100, averageScore = 60f),
    MockTest("mt5", "Economy Mini Test", "20 Questions · 30 Minutes",
        MockTestType.SubjectWise, 20, 30, "Economy", isPaid = true, totalAttempts = 3200, averageScore = 55f),
    MockTest("mt6", "Bihar GK Mini Test", "30 Questions · 40 Minutes",
        MockTestType.SubjectWise, 30, 40, "Bihar GK", isPaid = false, totalAttempts = 6800, averageScore = 72f),
    MockTest("mt7", "BPSC 67th Previous Year", "150 Questions · 2 Hours",
        MockTestType.PreviousYear, 150, 120, null, year = 2022, isPaid = false, totalAttempts = 15600, averageScore = 65f, isFeatured = true),
    MockTest("mt8", "BPSC 66th Previous Year", "150 Questions · 2 Hours",
        MockTestType.PreviousYear, 150, 120, null, year = 2020, isPaid = true, totalAttempts = 11200, averageScore = 61f),
)

val sampleQuestions = listOf(
    MockQuestion("mq1", "Which Article of the Indian Constitution abolishes untouchability?",
        listOf("Article 14", "Article 17", "Article 21", "Article 25"), 1,
        "Article 17 abolishes untouchability in any form.", "Polity", "Easy"),
    MockQuestion("mq2", "The term 'Secular' was added to the Preamble by which Amendment?",
        listOf("42nd Amendment", "44th Amendment", "52nd Amendment", "86th Amendment"), 0,
        "The 42nd Constitutional Amendment Act 1976 added 'Socialist', 'Secular' and 'Integrity' to the Preamble.", "Polity", "Medium"),
    MockQuestion("mq3", "Repo Rate is set by which institution?",
        listOf("SEBI", "NABARD", "RBI", "Finance Ministry"), 2,
        "The Reserve Bank of India (RBI) sets the Repo Rate through its Monetary Policy Committee.", "Economy", "Easy"),
    MockQuestion("mq4", "Which river is known as the 'Sorrow of Bihar'?",
        listOf("Ganga", "Kosi", "Gandak", "Son"), 1,
        "The Kosi river is called the 'Sorrow of Bihar' due to its frequent flooding.", "Bihar GK", "Easy"),
    MockQuestion("mq5", "The Dandi March was led by Gandhi in which year?",
        listOf("1919", "1925", "1930", "1942"), 2,
        "The Dandi March or Salt March was led from March 12 to April 6, 1930.", "History", "Medium"),
    MockQuestion("mq6", "Which Schedule deals with Anti-Defection Law?",
        listOf("8th Schedule", "9th Schedule", "10th Schedule", "12th Schedule"), 2,
        "The Tenth Schedule added by 52nd Amendment 1985 deals with Anti-Defection.", "Polity", "Hard"),
    MockQuestion("mq7", "SLR is maintained by banks with whom?",
        listOf("RBI", "Themselves", "SEBI", "Finance Ministry"), 1,
        "SLR (Statutory Liquidity Ratio) is maintained by banks with themselves as liquid assets.", "Economy", "Hard"),
    MockQuestion("mq8", "Which dynasty built the Nalanda University?",
        listOf("Maurya", "Gupta", "Pala", "Chola"), 2,
        "Nalanda University was established during the Gupta period but greatly patronised by the Pala dynasty.", "History", "Medium"),
    MockQuestion("mq9", "Bihar was separated from Bengal Presidency in which year?",
        listOf("1905", "1912", "1920", "1935"), 1,
        "Bihar was separated from Bengal Presidency and made a separate province in 1912.", "Bihar GK", "Medium"),
    MockQuestion("mq10", "What is the concept of Judicial Review borrowed from?",
        listOf("UK", "USA", "Ireland", "Canada"), 1,
        "India borrowed the concept of Judicial Review from the USA (Marbury vs Madison, 1803).", "Polity", "Medium"),
)

val mockLeaderboard = listOf(
    LeaderboardEntry(1, "Priya Singh",    95.33f, "1h 45m"),
    LeaderboardEntry(2, "Amit Kumar",    92.67f, "1h 52m"),
    LeaderboardEntry(3, "Rahul Kumar",   89.33f, "1h 58m", isCurrentUser = true),
    LeaderboardEntry(4, "Sneha Verma",   87.0f,  "2h 00m"),
    LeaderboardEntry(5, "Ravi Shankar",  85.33f, "1h 49m"),
    LeaderboardEntry(6, "Pooja Kumari",  82.0f,  "1h 55m"),
    LeaderboardEntry(7, "Ajay Yadav",    79.33f, "2h 00m"),
    LeaderboardEntry(8, "Divya Pandey",  77.0f,  "1h 42m"),
)

enum class MockTestState { Lobby, Instructions, Active, Analysis, Leaderboard }

private fun QuizPreviewDto.toMockTest(): MockTest = MockTest(
    id = id,
    title = title,
    subtitle = "${totalQuestions} Questions · ${durationMins} min · ${difficulty}",
    type = when (type) {
        "mock" -> MockTestType.Full
        "topic" -> MockTestType.SubjectWise
        "previous_year" -> MockTestType.PreviousYear
        else -> MockTestType.Full
    },
    totalQuestions = totalQuestions,
    durationMinutes = durationMins,
    subject = subject.takeIf { it.isNotBlank() },
    isPaid = false,
    totalAttempts = attemptCount,
    averageScore = avgScore.toFloat() ?: 0f,
    isFeatured = false
)

@Composable
fun MockTestsScreen(navController: NavHostController,
                    viewModel: MockTestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val allTests = remember(state.allTests) {
        state.allTests.map { it.toMockTest() }
    }

    if (state.isLoading && allTests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.error != null && allTests.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: ${state.error}")
            Button(onClick = { viewModel.retry() }) {
                Text("Retry")
            }
        }
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




    when (screenState) {
        MockTestState.Lobby -> MockTestLobbyScreen(
            navController   = navController,
            onStartTest     = { test ->
                selectedTest = test
                userAnswers.clear(); bookmarked.clear(); reviewMarked.clear()
                screenState  = MockTestState.Instructions
            },
            onCustomTest    = { showCustomSheet = true },
            viewModel=viewModel
        )
        MockTestState.Instructions -> selectedTest?.let { test ->
            TestInstructionsScreen(
                test    = test,
                onStart = { screenState = MockTestState.Active },
                onBack  = { screenState = MockTestState.Lobby }
            )
        }
        MockTestState.Active -> selectedTest?.let { test ->
            ActiveTestScreen(
                test          = test,
                questions     = sampleQuestions.take(test.totalQuestions.coerceAtMost(sampleQuestions.size)),
                userAnswers   = userAnswers,
                bookmarked    = bookmarked,
                reviewMarked  = reviewMarked,
                onSubmit      = { score ->
                    finalScore  = score
                    screenState = MockTestState.Analysis
                },
                onExit        = { screenState = MockTestState.Lobby }
            )
        }
        MockTestState.Analysis -> selectedTest?.let { test ->
            TestAnalysisScreen(
                test            = test,
                questions       = sampleQuestions.take(test.totalQuestions.coerceAtMost(sampleQuestions.size)),
                userAnswers     = userAnswers,
                score           = finalScore,
                onViewLeaderboard = { screenState = MockTestState.Leaderboard },
                onRetry         = {
                    userAnswers.clear(); bookmarked.clear(); reviewMarked.clear()
                    screenState = MockTestState.Active
                },
                onExit          = { screenState = MockTestState.Lobby }
            )
        }
        MockTestState.Leaderboard -> TestLeaderboardScreen(
            entries = mockLeaderboard,
            onBack  = { screenState = MockTestState.Analysis }
        )
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

    val state by viewModel.uiState.collectAsState()

    val allTests = remember(state.allTests) {
        state.allTests.map { it.toMockTest() }
    }
    var selectedType by remember { mutableStateOf<MockTestType?>(null) }
    val tabs = listOf("All", "Full Mock", "Mini Tests", "Prev. Year", "Custom")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
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
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            Column {
                                Text("Mock Tests", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Practice like the real exam", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Custom test button
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(0.15f))
                                .clickable(onClick = onCustomTest)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Custom", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
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

                    // Type filter tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        itemsIndexed(tabs) { index, tab ->
                            val type = when (index) {
                                1 -> MockTestType.Full; 2 -> MockTestType.SubjectWise
                                3 -> MockTestType.PreviousYear; else -> null
                            }
                            val sel = if (index == 0) selectedType == null else selectedType == type
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (sel) Color.White else Color.White.copy(0.15f))
                                    .clickable { selectedType = if (index == 0) null else type }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(tab,
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
                // Featured tests
                val featured = filtered.filter { it.isFeatured }
                if (featured.isNotEmpty() && selectedType == null) {
                    item {
                        Text("⭐ Featured", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
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
                            MockTestType.Full         -> "Full Mock Tests"
                            MockTestType.SubjectWise  -> "Subject-wise Mini Tests"
                            MockTestType.PreviousYear -> "Previous Year Papers"
                            null                      -> "All Tests"
                            else                      -> "Tests"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = BpscColors.TextPrimary,
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(60.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

@Composable
private fun MockTestCard(test: MockTest, isFeatured: Boolean, onStart: () -> Unit) {
    val typeColor = when (test.type) {
        MockTestType.Full         -> Pair(Color(0xFF1565C0), Color(0xFFE8F0FD))
        MockTestType.SubjectWise  -> Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4))
        MockTestType.PreviousYear -> Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD))
        MockTestType.Custom       -> Pair(Color(0xFFE67E22), Color(0xFFFFF0EA))
    }
    val typeLabel = when (test.type) {
        MockTestType.Full         -> "Full Mock"
        MockTestType.SubjectWise  -> "Mini Test"
        MockTestType.PreviousYear -> "Prev. Year"
        MockTestType.Custom       -> "Custom"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(test.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(test.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
                        // Negative marking
                        Text("-${test.negativeMarking}m", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 6.dp, vertical = 2.dp))
                        if (test.year != null) {
                            Text("${test.year}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = BpscColors.Divider)

            // Stats + Start button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat(Icons.Rounded.People, "${(test.totalAttempts / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${test.totalAttempts}" }}", "Attempts")
                    MiniStat(Icons.Rounded.BarChart, "${test.averageScore.toInt()}%", "Avg Score")
                    MiniStat(Icons.Rounded.Timer, "${test.durationMinutes}m", "Duration")
                }
                Button(
                    onClick  = onStart,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (test.isPaid) BpscColors.CoinGold else BpscColors.Primary
                    ),
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(if (test.isPaid) "Unlock" else "Start", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
        Column {
            Text(value, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding().padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Test Overview", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(140.dp)
                        ) {
                            item { InfoTile("📝", "Questions", "${test.totalQuestions}") }
                            item { InfoTile("⏱️", "Duration", "${test.durationMinutes} min") }
                            item { InfoTile("✅", "Marks/Q",  "+1.0") }
                            item { InfoTile("❌", "Negative",  "-${test.negativeMarking}") }
                        }
                    }
                }

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Instructions", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        listOf(
                            Triple("🟢", "Attempted",            "Questions you have answered"),
                            Triple("🔵", "Marked for Review",    "Questions you want to revisit"),
                            Triple("⚪", "Unattempted",          "Questions not yet answered"),
                            Triple("🟡", "Attempted + Marked",   "Answered but flagged for review"),
                            Triple("📖", "Question Navigator",   "Tap the grid icon to jump to any question"),
                            Triple("🔖", "Bookmark",             "Save important questions for later"),
                            Triple("⏰", "Auto Submit",          "Test submits automatically when timer ends"),
                            Triple("↩️",  "Resume",              "You can resume if you exit accidentally"),
                        ).forEach { (emoji, title, desc) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Start button
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp)) {
                Button(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Test", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun InfoTile(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(BpscColors.Surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
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
    var currentIndex    by remember { mutableIntStateOf(0) }
    var timeLeft        by remember { mutableIntStateOf(test.durationMinutes * 60) }
    var showNavigator   by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    val scope           = rememberCoroutineScope()
    val current         = questions.getOrNull(currentIndex)

    // Timer
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        // Auto submit
        val score = calculateScore(questions, userAnswers, test.negativeMarking)
        onSubmit(score)
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

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
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
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onExit), contentAlignment = Alignment.Center) {
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

                        // Navigator + Submit
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).clickable { showNavigator = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).clickable { showSubmitDialog = true }.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text("Submit", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Status counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusChip("🟢", "$attempted",   "Answered")
                        StatusChip("🔵", "$marked",      "Review")
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
                        DiffBadge(current.difficulty)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        current.question,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }

                // Options
                current.options.forEachIndexed { index, option ->
                    val isSelected = userAnswers[current.id] == index
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) BpscColors.PrimaryLight else Color.White)
                            .border(1.5.dp, if (isSelected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(14.dp))
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
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { if (currentIndex > 0) currentIndex-- },
                        enabled  = currentIndex > 0,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, BpscColors.Divider),
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
                            if (currentIndex < questions.size - 1) "Save & Next →" else "Submit Test",
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                        Text("Question Navigator", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(Triple("🟢","Answered", BpscColors.Success), Triple("🔵","Review", BpscColors.Primary), Triple("⚪","Pending", BpscColors.TextHint)).forEach { (e, l, c) ->
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
                title = { Text("Submit Test?", fontWeight = FontWeight.Bold) },
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
                            val score = calculateScore(questions, userAnswers, test.negativeMarking)
                            onSubmit(score)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                        shape  = RoundedCornerShape(10.dp)
                    ) { Text("Submit") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSubmitDialog = false }, shape = RoundedCornerShape(10.dp)) { Text("Review") }
                }
            )
        }
    }
}

@Composable
private fun StatusChip(icon: String, value: String, label: String) {
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
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun DiffBadge(difficulty: String) {
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
    onViewLeaderboard: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val correct    = questions.count { q -> userAnswers[q.id] == q.correctIndex }
    val wrong      = questions.count { q -> userAnswers.containsKey(q.id) && userAnswers[q.id] != q.correctIndex }
    val skipped    = questions.size - correct - wrong
    val maxScore   = questions.size.toFloat()
    val percentage = if (maxScore > 0) (score / maxScore * 100).toInt() else 0
    val rank       = 3
    val percentile = 95.2f
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
            Text(when { percentage >= 80 -> "Outstanding!"; percentage >= 60 -> "Well Done!"; percentage >= 40 -> "Good Effort!"; else -> "Keep Practicing!" },
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
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Rank + percentile
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 24.sp)
                        Text("#$rank", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Your Rank", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📈", fontSize = 24.sp)
                        Text("${percentile}%", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Percentile", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AnalysisStat("✅", "$correct",        "Correct",   BpscColors.Success)
                    AnalysisStat("❌", "$wrong",          "Wrong",     Color(0xFFE74C3C))
                    AnalysisStat("⏭️", "$skipped",        "Skipped",   BpscColors.TextSecondary)
                    AnalysisStat("📊", "${score.toInt()}","Score",     BpscColors.Primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Subject breakdown
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subject-wise Analysis", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    subjectStats.forEach { (subject, correct, total) ->
                        val pct = if (total > 0) correct.toFloat() / total else 0f
                        val animSubProg by animateFloatAsState(pct, tween(1000), label = "sub$subject")
                        val subColors = mapOf("Polity" to Color(0xFF9B59B6), "History" to Color(0xFFE74C3C), "Geography" to Color(0xFF1ABC9C),
                            "Economy" to Color(0xFFE67E22), "Bihar GK" to Color(0xFFF39C12), "Science" to Color(0xFF2ECC71))
                        val color = subColors[subject] ?: BpscColors.Primary
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(subject, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("$correct/$total", style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BpscColors.Surface)) {
                                Box(modifier = Modifier.fillMaxWidth(animSubProg).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Buttons
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onViewLeaderboard, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.Leaderboard, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Leaderboard", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.4f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Test", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.8f))) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Tests", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnalysisStat(icon: String, value: String, label: String, color: Color) {
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
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
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
                    Text("Leaderboard", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
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
                                Text(entry.name, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                if (entry.isCurrentUser) Text("You", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("Time: ${entry.timeTaken}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
            Text("Create Custom Test", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)

            // Subject selection
            Text("Select Subjects", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSubjects) { sub ->
                    val sel = selectedSubs.contains(sub)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                            .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
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
                    Text("Questions", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("$questionCount", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                }
                Slider(value = questionCount.toFloat(), onValueChange = { questionCount = it.toInt() },
                    valueRange = 10f..100f, steps = 8,
                    colors = SliderDefaults.colors(thumbColor = BpscColors.Primary, activeTrackColor = BpscColors.Primary))
            }

            // Duration slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("$durationMins min", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                }
                Slider(value = durationMins.toFloat(), onValueChange = { durationMins = it.toInt() },
                    valueRange = 15f..180f, steps = 10,
                    colors = SliderDefaults.colors(thumbColor = BpscColors.Primary, activeTrackColor = BpscColors.Primary))
            }

            // Negative marking toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Negative Marking", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("-0.33 per wrong answer", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
                        title = "Custom Test",
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
                Text("Start Custom Test 🚀", style = MaterialTheme.typography.titleMedium)
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