package com.example.bpscnotes.presentation.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.*

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val subject: String,
    val difficulty: QuizDifficulty,
    val hint: String,
    val isPaid: Boolean = false,
    val courseTag: String? = null,   // non-null = course-linked paid question
)

enum class QuizDifficulty(val label: String, val color: Color) {
    Easy("Easy",     Color(0xFF2ECC71)),
    Medium("Medium", Color(0xFFF39C12)),
    Hard("Hard",     Color(0xFFE74C3C))
}

data class QuizSession(
    val id: String,
    val title: String,
    val subtitle: String,
    val isFree: Boolean,
    val courseTag: String? = null,
    val questions: List<QuizQuestion>
)

val mockQuizSessions = listOf(
    QuizSession(
        id = "free_daily",
        title = "Daily Free Quiz",
        subtitle = "5 questions · Free for everyone",
        isFree = true,
        questions = listOf(
            QuizQuestion("q1", "Which Article of the Indian Constitution abolishes untouchability?",
                listOf("Article 14", "Article 17", "Article 21", "Article 25"),
                1, "Article 17 abolishes untouchability and forbids its practice in any form.", "Polity", QuizDifficulty.Easy,
                "Think about Fundamental Rights chapter."),
            QuizQuestion("q2", "The term 'Secular' was added to the Preamble by which Amendment?",
                listOf("42nd Amendment", "44th Amendment", "52nd Amendment", "86th Amendment"),
                0, "The 42nd Constitutional Amendment Act 1976 added the words 'Socialist', 'Secular' and 'Integrity' to the Preamble.", "Polity", QuizDifficulty.Medium,
                "This amendment was passed during the Emergency period."),
            QuizQuestion("q3", "Who is known as the Father of Indian Constitution?",
                listOf("Jawaharlal Nehru", "Mahatma Gandhi", "B.R. Ambedkar", "Sardar Patel"),
                2, "Dr. B.R. Ambedkar was the Chairman of the Drafting Committee and is known as the Father of the Indian Constitution.", "Polity", QuizDifficulty.Easy,
                "He was the Chairman of the Drafting Committee."),
            QuizQuestion("q4", "Repo Rate is set by which institution?",
                listOf("SEBI", "NABARD", "RBI", "Finance Ministry"),
                2, "The Reserve Bank of India (RBI) sets the Repo Rate through its Monetary Policy Committee.", "Economy", QuizDifficulty.Easy,
                "Think about India's central bank."),
            QuizQuestion("q5", "Which river is known as the 'Sorrow of Bihar'?",
                listOf("Ganga", "Kosi", "Gandak", "Son"),
                1, "The Kosi river is called the 'Sorrow of Bihar' due to its frequent flooding and course changes causing massive destruction.", "Bihar GK", QuizDifficulty.Medium,
                "This river frequently changes its course causing floods."),
        )
    ),
    QuizSession(
        id = "polity_advanced",
        title = "Polity Advanced",
        subtitle = "10 questions · BPSC Prelims Course",
        isFree = false,
        courseTag = "BPSC Prelims Complete",
        questions = listOf(
            QuizQuestion("q6", "Which Schedule of the Constitution deals with Anti-Defection Law?",
                listOf("8th Schedule", "9th Schedule", "10th Schedule", "12th Schedule"),
                2, "The Tenth Schedule, added by the 52nd Amendment Act 1985, contains provisions regarding disqualification on grounds of defection.", "Polity", QuizDifficulty.Hard,
                "It was added by the 52nd Amendment.", true, "BPSC Prelims Complete"),
            QuizQuestion("q7", "The concept of Judicial Review in India is borrowed from which country?",
                listOf("UK", "USA", "Ireland", "Canada"),
                1, "India borrowed the concept of Judicial Review from the USA, where it was established in Marbury vs Madison (1803).", "Polity", QuizDifficulty.Medium,
                "Think about the oldest democracy.", true, "BPSC Prelims Complete"),
        )
    )
)

// ─────────────────────────────────────────────────────────────
// QUIZ LOBBY — Choose which quiz to attempt
// ─────────────────────────────────────────────────────────────
@Composable
fun DailyQuizScreen(navController: NavHostController, date: String) {
    var activeSession by remember { mutableStateOf<QuizSession?>(null) }

    if (activeSession != null) {
        QuizSessionScreen(
            session       = activeSession!!,
            navController = navController,
            onExit        = { activeSession = null }
        )
    } else {
        QuizLobbyScreen(
            navController = navController,
            onStartSession = { activeSession = it }
        )
    }
}

@Composable
private fun QuizLobbyScreen(
    navController: NavHostController,
    onStartSession: (QuizSession) -> Unit
) {
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
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
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
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Daily Quiz", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Test your knowledge today", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Coins display
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Text("142", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LobbyStatChip("🎯", "87%",  "Accuracy")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🔥", "7",    "Day Streak")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("🏆", "#3",   "Rank")
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                        LobbyStatChip("✅", "142",  "Solved")
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Free section
                item {
                    SectionLabel("🆓 Free Daily Quizzes", "Resets every day at midnight")
                }
                items(mockQuizSessions.filter { it.isFree }) { session ->
                    QuizSessionCard(session = session, onStart = { onStartSession(session) })
                }

                item { Spacer(Modifier.height(4.dp)) }

                // Paid section
                item {
                    SectionLabel("🔒 Course Quizzes", "Unlock with course purchase")
                }
                items(mockQuizSessions.filter { !it.isFree }) { session ->
                    QuizSessionCard(session = session, onStart = { onStartSession(session) })
                }
            }
        }
    }
}

@Composable
private fun LobbyStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(64.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun QuizSessionCard(session: QuizSession, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (session.isFree) BpscColors.PrimaryLight else Color(0xFFFFF8E1)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (session.isFree) "📝" else "🔒", fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    if (session.isFree) {
                        Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
                    } else {
                        Text("PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(session.subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                if (session.courseTag != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.MenuBook, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                        Text(session.courseTag, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    }
                }
            }

            // Start / Unlock button
            Button(
                onClick = onStart,
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.isFree) BpscColors.Primary else BpscColors.CoinGold
                ),
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(
                    if (session.isFree) "Start" else "Unlock",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ SESSION — One question at a time
// ─────────────────────────────────────────────────────────────
enum class AnswerState { None, Correct, Wrong }

@Composable
internal fun QuizSessionScreen(
    session: QuizSession,
    navController: NavHostController,
    onExit: () -> Unit
) {
    val questions        = session.questions
    var currentIndex     by remember { mutableIntStateOf(0) }
    var selectedOption   by remember { mutableIntStateOf(-1) }
    var answerState      by remember { mutableStateOf(AnswerState.None) }
    var showExplanation  by remember { mutableStateOf(false) }
    var showHint         by remember { mutableStateOf(false) }
    var timeLeft         by remember { mutableIntStateOf(30) }
    var timerActive      by remember { mutableStateOf(true) }
    var streak           by remember { mutableIntStateOf(0) }
    var correctCount     by remember { mutableIntStateOf(0) }
    var skippedIds       = remember { mutableStateListOf<String>() }
    var isQuizComplete   by remember { mutableStateOf(false) }
    var showReviewAll    by remember { mutableStateOf(false) }

    // User answers map: questionId -> selectedIndex (-1 = skipped)
    val userAnswers      = remember { mutableStateMapOf<String, Int>() }

    val scope = rememberCoroutineScope()
    val current = questions.getOrNull(currentIndex)

    // Timer
    LaunchedEffect(currentIndex, timerActive) {
        timeLeft = 30
        while (timeLeft > 0 && timerActive && answerState == AnswerState.None) {
            delay(1000)
            timeLeft--
        }
        // Auto skip on timeout
        if (timeLeft == 0 && answerState == AnswerState.None && current != null) {
            skippedIds.add(current.id)
            userAnswers[current.id] = -1
            if (currentIndex < questions.size - 1) {
                currentIndex++
                selectedOption  = -1
                answerState     = AnswerState.None
                showExplanation = false
                showHint        = false
                timerActive     = true
            } else {
                isQuizComplete = true
            }
        }
    }

    // Quiz complete screen
    if (isQuizComplete) {
        QuizSummaryScreen(
            session      = session,
            questions    = questions,
            userAnswers  = userAnswers,
            correctCount = correctCount,
            skippedCount = skippedIds.size,
            onReviewAll  = { showReviewAll = true },
            onExit       = onExit,
            navController = navController
        )
        return
    }

    // Review all screen
    if (showReviewAll) {
        QuizReviewScreen(
            questions   = questions,
            userAnswers = userAnswers,
            onBack      = { showReviewAll = false }
        )
        return
    }

    if (current == null) return

    val progress = (currentIndex + 1).toFloat() / questions.size
    val animProgress by animateFloatAsState(progress, tween(500), label = "qprog")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(400f, 200f)
                    ))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f)).clickable(onClick = onExit),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Question counter
                        Text(
                            "Q ${currentIndex + 1} / ${questions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        // Streak + Timer
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (streak > 1) {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFFFF3CD))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("🔥", fontSize = 11.sp)
                                    Text("$streak", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            // Timer circle
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 3.dp.toPx()
                                    drawArc(Color.White.copy(0.2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                                    drawArc(
                                        color = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B),
                                        startAngle = -90f,
                                        sweepAngle = (timeLeft / 30f) * 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    "$timeLeft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (timeLeft > 10) Color.White else Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject + difficulty
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SubjectChip(current.subject)
                    DifficultyChip(current.difficulty)
                    if (current.isPaid) {
                        Text("Course Q", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                // Question card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            current.question,
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )

                        // Hint
                        AnimatedVisibility(visible = showHint) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFF8E1)).padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💡", fontSize = 16.sp)
                                    Text(current.hint, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404))
                                }
                            }
                        }
                    }
                }

                // Options
                current.options.forEachIndexed { index, option ->
                    val optionState = when {
                        answerState == AnswerState.None && selectedOption == index -> "selected"
                        answerState != AnswerState.None && index == current.correctIndex -> "correct"
                        answerState != AnswerState.None && index == selectedOption && selectedOption != current.correctIndex -> "wrong"
                        else -> "default"
                    }
                    val bg = when (optionState) {
                        "selected" -> BpscColors.PrimaryLight
                        "correct"  -> Color(0xFFE8FDF4)
                        "wrong"    -> Color(0xFFFEE8E8)
                        else       -> Color.White
                    }
                    val border = when (optionState) {
                        "selected" -> BpscColors.Primary
                        "correct"  -> BpscColors.Success
                        "wrong"    -> Color(0xFFE74C3C)
                        else       -> BpscColors.Divider
                    }
                    val textColor = when (optionState) {
                        "selected" -> BpscColors.Primary
                        "correct"  -> BpscColors.Success
                        "wrong"    -> Color(0xFFE74C3C)
                        else       -> BpscColors.TextPrimary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.5.dp, border, RoundedCornerShape(14.dp))
                            .clickable(enabled = answerState == AnswerState.None) {
                                selectedOption = index
                                timerActive    = false
                                val isCorrect  = index == current.correctIndex
                                answerState    = if (isCorrect) AnswerState.Correct else AnswerState.Wrong
                                if (isCorrect) { correctCount++; streak++ } else streak = 0
                                userAnswers[current.id] = index
                                showExplanation = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option letter
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(border.copy(alpha = if (optionState == "default") 0.1f else 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                listOf("A", "B", "C", "D")[index],
                                style = MaterialTheme.typography.titleMedium,
                                color = border,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        Text(option, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
                        // Result icon
                        when (optionState) {
                            "correct" -> Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(20.dp))
                            "wrong"   -> Icon(Icons.Rounded.Cancel,      null, tint = Color(0xFFE74C3C),  modifier = Modifier.size(20.dp))
                            else      -> {}
                        }
                    }
                }

                // Explanation
                AnimatedVisibility(visible = showExplanation) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (answerState == AnswerState.Correct) Color(0xFFE8FDF4) else Color(0xFFFEF0F0)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (answerState == AnswerState.Correct) "✅" else "❌", fontSize = 14.sp)
                                Text(
                                    if (answerState == AnswerState.Correct) "Correct! +1 Coin" else "Incorrect",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (answerState == AnswerState.Correct) BpscColors.Success else Color(0xFFE74C3C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(current.explanation, style = MaterialTheme.typography.bodyMedium,
                                color = if (answerState == AnswerState.Correct) Color(0xFF1A5C3A) else Color(0xFF8B1C1C),
                                lineHeight = 20.sp)
                        }
                    }
                }
            }

            // ── Bottom action bar ────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (answerState == AnswerState.None) {
                    // Before answering: Hint + Skip
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showHint = !showHint },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BpscColors.CoinGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.CoinGold)
                        ) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Hint (-1 🪙)", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                skippedIds.add(current.id)
                                userAnswers[current.id] = -1
                                timerActive    = false
                                if (currentIndex < questions.size - 1) {
                                    currentIndex++
                                    selectedOption  = -1
                                    answerState     = AnswerState.None
                                    showExplanation = false
                                    showHint        = false
                                    timerActive     = true
                                } else {
                                    isQuizComplete = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BpscColors.Divider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                        ) {
                            Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Skip", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    // After answering: Next
                    Button(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                selectedOption  = -1
                                answerState     = AnswerState.None
                                showExplanation = false
                                showHint        = false
                                timerActive     = true
                            } else {
                                isQuizComplete = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Text(
                            if (currentIndex < questions.size - 1) "Next Question →" else "See Results 🏆",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ SUMMARY
// ─────────────────────────────────────────────────────────────
@Composable
internal fun QuizSummaryScreen(
    session: QuizSession,
    questions: List<QuizQuestion>,
    userAnswers: Map<String, Int>,
    correctCount: Int,
    skippedCount: Int,
    onReviewAll: () -> Unit,
    onExit: () -> Unit,
    navController: NavHostController
) {
    val wrongCount   = questions.size - correctCount - skippedCount
    val accuracy     = if (questions.size > 0) (correctCount * 100f / questions.size).toInt() else 0
    val coinsEarned  = correctCount
    val progress     by animateFloatAsState(accuracy / 100f, tween(1200), label = "sum")

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp).statusBarsPadding())

            // Trophy
            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    accuracy >= 80 -> "Excellent!"
                    accuracy >= 50 -> "Good Job!"
                    else           -> "Keep Practicing!"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(session.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset  = stroke / 2
                    val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)),
                        startAngle = -90f, sweepAngle = progress * 360f, useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset), size = sz
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$accuracy%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats row
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatItem("✅", "$correctCount", "Correct",  BpscColors.Success)
                    SummaryStatItem("❌", "$wrongCount",  "Wrong",    Color(0xFFE74C3C))
                    SummaryStatItem("⏭️", "$skippedCount","Skipped",  BpscColors.TextSecondary)
                    SummaryStatItem("🪙", "+$coinsEarned","Coins",    BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Accuracy bar card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Accuracy", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("$accuracy%", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BpscColors.Surface)) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF64B5F6))), RoundedCornerShape(5.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${questions.size} questions attempted", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        Text("🔥 Streak: $correctCount", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = onReviewAll,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Review All Questions", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick  = onExit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color.White.copy(0.4f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Quizzes", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryStatItem(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEW ALL QUESTIONS
// ─────────────────────────────────────────────────────────────
@Composable
internal fun QuizReviewScreen(
    questions: List<QuizQuestion>,
    userAnswers: Map<String, Int>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Review Questions", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("${questions.size} questions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(questions) { index, question ->
                val userAnswer = userAnswers[question.id] ?: -1
                val isCorrect  = userAnswer == question.correctIndex
                val isSkipped  = userAnswer == -1

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSkipped  -> Color.White
                            isCorrect  -> Color(0xFFF0FBF5)
                            else       -> Color(0xFFFEF0F0)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Q number + result
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape)
                                        .background(BpscColors.PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                                }
                                SubjectChip(question.subject)
                                DifficultyChip(question.difficulty)
                            }
                            Text(
                                when { isSkipped -> "⏭ Skipped"; isCorrect -> "✅ Correct"; else -> "❌ Wrong" },
                                style = MaterialTheme.typography.labelSmall,
                                color = when { isSkipped -> BpscColors.TextSecondary; isCorrect -> BpscColors.Success; else -> Color(0xFFE74C3C) },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(question.question, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

                        // Options
                        question.options.forEachIndexed { i, option ->
                            val isCorrectOpt = i == question.correctIndex
                            val isUserOpt    = i == userAnswer
                            val bg = when {
                                isCorrectOpt             -> Color(0xFFE8FDF4)
                                isUserOpt && !isCorrectOpt -> Color(0xFFFEE8E8)
                                else                     -> Color.Transparent
                            }
                            val textColor = when {
                                isCorrectOpt             -> BpscColors.Success
                                isUserOpt && !isCorrectOpt -> Color(0xFFE74C3C)
                                else                     -> BpscColors.TextSecondary
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(listOf("A","B","C","D")[i], style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.ExtraBold)
                                Text(option, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
                                if (isCorrectOpt) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                                if (isUserOpt && !isCorrectOpt) Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp))
                            }
                        }

                        // Explanation
                        HorizontalDivider(color = BpscColors.Divider)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡", fontSize = 13.sp)
                            Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
internal fun SubjectChip(subject: String) {
    val colors = mapOf(
        "Polity"    to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "History"   to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Economy"   to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK"  to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
        "Science"   to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    )
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
internal fun DifficultyChip(difficulty: QuizDifficulty) {
    Text(difficulty.label, style = MaterialTheme.typography.labelSmall, color = difficulty.color, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(difficulty.color.copy(0.1f)).padding(horizontal = 7.dp, vertical = 2.dp))
}