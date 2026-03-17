package com.example.bpscnotes.presentation.activerecall

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.*
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class Flashcard(
    val id: String,
    val subject: String,
    val topic: String,
    val question: String,
    val answer: String,
    val hint: String,
    val example: String,
    val relatedMcq: FlashMcq?,
    val difficulty: FlashDifficulty,
)

data class FlashMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

enum class FlashDifficulty(val label: String, val color: Color) {
    Easy("Easy",     Color(0xFF2ECC71)),
    Medium("Medium", Color(0xFFF39C12)),
    Hard("Hard",     Color(0xFFE74C3C))
}

enum class CardRating { Mastered, Weak, Skipped }

val mockFlashcards = listOf(
    Flashcard("f1", "Polity", "Fundamental Rights",
        "Which Article guarantees Right to Equality?",
        "Articles 14–18 guarantee Right to Equality. Article 14 ensures equality before law, Article 15 prohibits discrimination, Article 16 ensures equal opportunity in public employment.",
        "Think about Part III of the Constitution.",
        "Example: Article 14 was invoked in the landmark case of E.P. Royappa v. State of Tamil Nadu.",
        FlashMcq("Article 14 deals with?", listOf("Right to Freedom", "Equality before Law", "Right to Education", "Right to Privacy"), 1),
        FlashDifficulty.Easy),
    Flashcard("f2", "Polity", "Directive Principles",
        "What is the significance of DPSP in the Indian Constitution?",
        "Directive Principles of State Policy (Part IV, Articles 36-51) are non-justiciable guidelines for the State to achieve social and economic democracy. They reflect the positive obligations of the State.",
        "DPSP is borrowed from the Irish Constitution.",
        "Example: Article 45 directed free education for children, which later became a Fundamental Right via Article 21A.",
        FlashMcq("DPSP is contained in which Part?", listOf("Part III", "Part IV", "Part V", "Part VI"), 1),
        FlashDifficulty.Medium),
    Flashcard("f3", "History", "Mughal Empire",
        "Who was the last great Mughal Emperor and why?",
        "Aurangzeb (1658-1707) is considered the last great Mughal Emperor. His reign saw the greatest territorial expansion but also sowed seeds of decline through religious policies and prolonged Deccan campaigns.",
        "He was also known as Alamgir.",
        "Example: Under Aurangzeb, the Mughal Empire reached its greatest extent covering nearly 4 million sq km.",
        FlashMcq("Aurangzeb's reign lasted approximately?", listOf("20 years", "30 years", "49 years", "60 years"), 2),
        FlashDifficulty.Medium),
    Flashcard("f4", "Geography", "River Systems",
        "Explain the concept of river basin vs watershed.",
        "A river basin is the entire area drained by a river and its tributaries. A watershed (or divide) is the elevated boundary separating adjacent drainage basins. Every river basin is bounded by watersheds.",
        "Think of it as a bowl (basin) vs the rim (watershed).",
        "Example: The Ganga basin covers about 26% of India's total geographical area.",
        FlashMcq("Which is the largest river basin in India?", listOf("Indus", "Ganga", "Godavari", "Krishna"), 1),
        FlashDifficulty.Easy),
    Flashcard("f5", "Economy", "Monetary Policy",
        "What is the difference between Repo Rate and Reverse Repo Rate?",
        "Repo Rate is the rate at which RBI lends money to commercial banks. Reverse Repo Rate is the rate at which RBI borrows money from commercial banks. Repo Rate > Reverse Repo Rate always.",
        "Repo = RBI gives money. Reverse Repo = RBI takes money.",
        "Example: If Repo Rate is 6.5%, banks borrow at 6.5%. If Reverse Repo is 6.25%, banks deposit at 6.25%.",
        FlashMcq("Which rate does RBI use to absorb excess liquidity?", listOf("Repo Rate", "Bank Rate", "Reverse Repo Rate", "MSF Rate"), 2),
        FlashDifficulty.Hard),
    Flashcard("f6", "Bihar GK", "Geography",
        "Name the major rivers flowing through North Bihar and their significance.",
        "Major rivers: Ganga (south), Kosi (called Sorrow of Bihar), Gandak, Bagmati, Mahananda. These rivers cause annual floods but also make the soil fertile for agriculture.",
        "North Bihar is part of the Indo-Gangetic plain.",
        "Example: The Kosi river has shifted its course by 120 km westward over the past 250 years.",
        FlashMcq("Which river is called the 'Sorrow of Bihar'?", listOf("Ganga", "Gandak", "Kosi", "Son"), 2),
        FlashDifficulty.Easy),
    Flashcard("f7", "History", "Freedom Movement",
        "What was the significance of the Dandi March (1930)?",
        "The Dandi March (Salt March) was led by Gandhi from March 12 to April 6, 1930. It was a 386 km march to protest the British salt tax. It launched the Civil Disobedience Movement and attracted worldwide attention.",
        "It started from Sabarmati Ashram, Ahmedabad.",
        "Example: Gandhi and 78 followers walked to Dandi, Gujarat, where he made salt from seawater, violating British law.",
        FlashMcq("The Dandi March lasted approximately?", listOf("15 days", "24 days", "30 days", "45 days"), 1),
        FlashDifficulty.Medium),
    Flashcard("f8", "Economy", "Banking",
        "What is Statutory Liquidity Ratio (SLR)?",
        "SLR is the minimum percentage of deposits that banks must maintain as liquid assets (cash, gold, government securities) with themselves. Currently set by RBI. Higher SLR = less money for lending.",
        "SLR is about what banks keep with themselves, not RBI.",
        "Example: If SLR is 18% and a bank has ₹100 crore deposits, it must maintain ₹18 crore as liquid assets.",
        FlashMcq("SLR is maintained by banks with?", listOf("RBI", "Themselves", "SEBI", "Finance Ministry"), 1),
        FlashDifficulty.Hard),
)

val flashSubjects = listOf("All", "Polity", "History", "Geography", "Economy", "Bihar GK")

// ─────────────────────────────────────────────────────────────
// LOBBY SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun ActiveRecallScreen(navController: NavHostController) {
    val masteredIds  = remember { mutableStateListOf<String>() }
    val weakIds      = remember { mutableStateListOf<String>() }
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var retryWeak     by remember { mutableStateOf(false) }

    if (activeSubject != null) {
        val sessionCards = if (retryWeak) {
            mockFlashcards.filter { weakIds.contains(it.id) }
        } else {
            if (activeSubject == "All") mockFlashcards
            else mockFlashcards.filter { it.subject == activeSubject }
        }.let { cards ->
            // Spaced repetition — weak cards appear first + more often
            val weak   = cards.filter { weakIds.contains(it.id) }
            val normal = cards.filter { !weakIds.contains(it.id) && !masteredIds.contains(it.id) }
            val master = cards.filter { masteredIds.contains(it.id) }
            weak + weak + normal + master  // weak shown twice
        }.distinctBy { it.id }

        FlashcardSessionScreen(
            cards      = sessionCards,
            masteredIds = masteredIds,
            weakIds    = weakIds,
            onRate     = { card, rating ->
                when (rating) {
                    CardRating.Mastered -> { masteredIds.add(card.id); weakIds.remove(card.id) }
                    CardRating.Weak     -> { weakIds.add(card.id); masteredIds.remove(card.id) }
                    CardRating.Skipped  -> {}
                }
            },
            onExit     = { activeSubject = null; retryWeak = false }
        )
    } else {
        FlashcardLobbyScreen(
            masteredIds  = masteredIds,
            weakIds      = weakIds,
            navController = navController,
            onStartSubject = { subject -> activeSubject = subject; retryWeak = false },
            onRetryWeak    = { activeSubject = "All"; retryWeak = true }
        )
    }
}

@Composable
private fun FlashcardLobbyScreen(
    masteredIds: List<String>,
    weakIds: List<String>,
    navController: NavHostController,
    onStartSubject: (String) -> Unit,
    onRetryWeak: () -> Unit,
) {
    val totalCards    = mockFlashcards.size
    val masteredCount = masteredIds.size
    val weakCount     = weakIds.size
    val progress      = if (totalCards > 0) masteredCount.toFloat() / totalCards else 0f

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
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
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.7f))
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
                                Text("Active Recall", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Flashcard study sessions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Overall progress
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overall Mastery", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$masteredCount / $totalCards cards", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.2f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(4.dp))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatPill("✅", "$masteredCount", "Mastered", Color(0xFF2ECC71))
                            StatPill("🔄", "$weakCount",     "Needs Work", Color(0xFFE74C3C))
                            StatPill("📚", "${totalCards - masteredCount - weakCount}", "Unseen", Color.White.copy(0.6f))
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retry weak cards banner
                if (weakCount > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onRetryWeak),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF0F0)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE74C3C).copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("🔄", fontSize = 22.sp) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Retry Weak Cards", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                                    Text("$weakCount cards need more practice", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                }
                                Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Subject cards
                item {
                    Text("Choose Subject", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                }

                items(flashSubjects) { subject ->
                    val subjectCards = if (subject == "All") mockFlashcards
                    else mockFlashcards.filter { it.subject == subject }
                    val subMastered  = subjectCards.count { masteredIds.contains(it.id) }
                    val subWeak      = subjectCards.count { weakIds.contains(it.id) }
                    val subProgress  = if (subjectCards.isNotEmpty()) subMastered.toFloat() / subjectCards.size else 0f

                    val subjectColors = mapOf(
                        "All"       to Pair(BpscColors.Primary,     BpscColors.PrimaryLight),
                        "Polity"    to Pair(Color(0xFF9B59B6),      Color(0xFFF3E8FD)),
                        "History"   to Pair(Color(0xFFE74C3C),      Color(0xFFFEE8E8)),
                        "Geography" to Pair(Color(0xFF1ABC9C),      Color(0xFFE8FDF8)),
                        "Economy"   to Pair(Color(0xFFE67E22),      Color(0xFFFFF0EA)),
                        "Bihar GK"  to Pair(Color(0xFFF39C12),      Color(0xFFFFF8E1)),
                    )
                    val (accent, bg) = subjectColors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onStartSubject(subject) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        when (subject) {
                                            "All"       -> "📚"; "Polity" -> "⚖️"; "History" -> "🏛️"
                                            "Geography" -> "🗺️"; "Economy" -> "💰"; "Bihar GK" -> "🏔️"
                                            else        -> "📖"
                                        }, fontSize = 22.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subject, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${subjectCards.size} cards · $subMastered mastered",
                                        style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                }
                                // Start button
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                        .background(accent).padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("Start", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(subProgress).fillMaxHeight()
                                            .background(accent, RoundedCornerShape(3.dp))
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${(subProgress * 100).toInt()}% mastered",
                                        style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 10.sp)
                                    if (subWeak > 0) Text("$subWeak weak",
                                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(icon: String, value: String, label: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 11.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// SESSION SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun FlashcardSessionScreen(
    cards: List<Flashcard>,
    masteredIds: List<String>,
    weakIds: List<String>,
    onRate: (Flashcard, CardRating) -> Unit,
    onExit: () -> Unit,
) {
    var currentIndex  by remember { mutableIntStateOf(0) }
    var isFlipped     by remember { mutableStateOf(false) }
    var streak        by remember { mutableIntStateOf(0) }
    var sessionRatings = remember { mutableStateMapOf<String, CardRating>() }
    var isComplete    by remember { mutableStateOf(false) }

    val current = cards.getOrNull(currentIndex)
    val scope   = rememberCoroutineScope()

    // Swipe offset
    val offsetX   = remember { Animatable(0f) }
    val rotation  = remember { Animatable(0f) }

    // Flip animation
    val flipAngle by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "flip"
    )

    if (isComplete) {
        FlashcardSummaryScreen(
            cards          = cards,
            sessionRatings = sessionRatings,
            streak         = streak,
            onRetryWeak    = {
                // Reset and only retry weak from this session
                currentIndex    = 0
                isFlipped       = false
                isComplete      = false
                sessionRatings.clear()
            },
            onExit = onExit
        )
        return
    }

    if (current == null) {
        isComplete = true
        return
    }

    val progress = (currentIndex + 1).toFloat() / cards.size
    val animProg by animateFloatAsState(progress, tween(500), label = "prog")

    fun rateAndNext(rating: CardRating) {
        onRate(current, rating)
        sessionRatings[current.id] = rating
        if (rating == CardRating.Mastered) streak++ else if (rating == CardRating.Weak) streak = 0
        scope.launch {
            val targetX = when (rating) {
                CardRating.Mastered -> 600f
                CardRating.Weak     -> -600f
                CardRating.Skipped  -> 0f
            }
            if (rating != CardRating.Skipped) {
                offsetX.animateTo(targetX, tween(300))
            }
            if (currentIndex < cards.size - 1) {
                currentIndex++
                isFlipped = false
                offsetX.snapTo(0f)
                rotation.snapTo(0f)
            } else {
                isComplete = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f)).clickable(onClick = onExit),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }

                        Text("${currentIndex + 1} / ${cards.size}",
                            style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)

                        // Streak
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (streak > 0) Color(0xFFFFF3CD) else Color.White.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(if (streak > 0) "🔥" else "💪", fontSize = 12.sp)
                            Text(
                                if (streak > 0) "$streak streak" else "Keep going",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (streak > 0) Color(0xFF856404) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // ── Swipe hint row ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(16.dp))
                    Text("Needs Work", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
                Text("Swipe to rate", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mastered", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.KeyboardArrowRight, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
                }
            }

            // ── Flip card ────────────────────────────────────
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .offset(x = offsetX.value.dp)
                        .graphicsLayer {
                            rotationZ = (offsetX.value / 20f).coerceIn(-15f, 15f)
                        }
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        when {
                                            offsetX.value > 120f  -> rateAndNext(CardRating.Mastered)
                                            offsetX.value < -120f -> rateAndNext(CardRating.Weak)
                                            else                  -> offsetX.animateTo(0f, spring())
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount * 0.4f) }
                                }
                            )
                        }
                        .clickable { isFlipped = !isFlipped }
                ) {
                    // Swipe overlay indicators
                    // ✅ Replace both AnimatedVisibility blocks with simple if checks

                    if (offsetX.value > 60f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BpscColors.Success)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "✅ MASTERED",
                                style      = MaterialTheme.typography.titleMedium,
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (offsetX.value < -60f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE74C3C))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "🔄 WEAK",
                                style      = MaterialTheme.typography.titleMedium,
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Front face
                    if (flipAngle <= 90f) {
                        CardFrontFace(card = current)
                    }
                    // Back face
                    if (flipAngle > 90f) {
                        CardBackFace(
                            card     = current,
                            onRate   = ::rateAndNext
                        )
                    }
                }

                // Tap to flip hint
                if (!isFlipped) {
                    Text(
                        "Tap card to reveal answer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextHint,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }
            }

            // ── Bottom action bar ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isFlipped) {
                    // Before flip
                    OutlinedButton(
                        onClick = { rateAndNext(CardRating.Skipped) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BpscColors.Divider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary)
                    ) {
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { isFlipped = true },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Icon(Icons.Rounded.Flip, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reveal Answer", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // After flip — rate buttons
                    Button(
                        onClick = { rateAndNext(CardRating.Weak) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                    ) {
                        Text("🔄 Weak", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { rateAndNext(CardRating.Mastered) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Success)
                    ) {
                        Text("✅ Got it!", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CARD FRONT
// ─────────────────────────────────────────────────────────────
@Composable
private fun CardFrontFace(card: Flashcard) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top gradient strip
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0A2472), Color(0xFF1E88E5))))
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: subject + difficulty
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FlashSubjectChip(card.subject)
                    FlashDifficultyChip(card.difficulty)
                    Spacer(Modifier.weight(1f))
                    Text(card.topic, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Question
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("❓", fontSize = 36.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        card.question,
                        style = MaterialTheme.typography.titleLarge,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }

                // Hint
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF8E1)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 14.sp)
                    Text(card.hint, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404), lineHeight = 18.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CARD BACK
// ─────────────────────────────────────────────────────────────
@Composable
private fun CardBackFace(
    card: Flashcard,
    onRate: (CardRating) -> Unit,
) {
    var mcqSelected  by remember { mutableIntStateOf(-1) }
    var mcqAnswered  by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top gradient strip — green for answer side
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2ECC71), Color(0xFF1ABC9C))))
            )

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject + flipped indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlashSubjectChip(card.subject)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8FDF4)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("✅", fontSize = 10.sp)
                        Text("Answer", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }

                // Answer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Answer", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                    Text(card.answer, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, lineHeight = 24.sp)
                }

                // Example
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(BpscColors.PrimaryLight).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📌", fontSize = 14.sp)
                    Column {
                        Text("Example", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(card.example, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 20.sp)
                    }
                }

                // Related MCQ
                if (card.relatedMcq != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Related MCQ", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(card.relatedMcq.question, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, lineHeight = 22.sp)
                        card.relatedMcq.options.forEachIndexed { index, option ->
                            val isCorrect = index == card.relatedMcq.correctIndex
                            val isSelected = index == mcqSelected
                            val bg = when {
                                !mcqAnswered              -> if (isSelected) BpscColors.PrimaryLight else BpscColors.Surface
                                isCorrect                 -> Color(0xFFE8FDF4)
                                isSelected && !isCorrect  -> Color(0xFFFEE8E8)
                                else                      -> BpscColors.Surface
                            }
                            val textColor = when {
                                !mcqAnswered              -> if (isSelected) BpscColors.Primary else BpscColors.TextPrimary
                                isCorrect                 -> BpscColors.Success
                                isSelected && !isCorrect  -> Color(0xFFE74C3C)
                                else                      -> BpscColors.TextSecondary
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable(enabled = !mcqAnswered) {
                                        mcqSelected = index
                                        mcqAnswered = true
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                        .background(textColor.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(listOf("A","B","C","D")[index], style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(option, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
                                if (mcqAnswered && isCorrect) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SUMMARY SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
private fun FlashcardSummaryScreen(
    cards: List<Flashcard>,
    sessionRatings: Map<String, CardRating>,
    streak: Int,
    onRetryWeak: () -> Unit,
    onExit: () -> Unit,
) {
    val masteredCount = sessionRatings.values.count { it == CardRating.Mastered }
    val weakCount     = sessionRatings.values.count { it == CardRating.Weak }
    val skippedCount  = sessionRatings.values.count { it == CardRating.Skipped }
    val accuracy      = if (cards.isNotEmpty()) (masteredCount * 100f / cards.size).toInt() else 0
    val animProg by animateFloatAsState(accuracy / 100f, tween(1200), label = "sum")

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(40.dp))

            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                when { accuracy >= 80 -> "Excellent!"; accuracy >= 50 -> "Good Job!"; else -> "Keep Practicing!" },
                style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold
            )
            Text("Session Complete", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(24.dp))

            // Score ring
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset  = stroke / 2
                    val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)),
                        startAngle = -90f, sweepAngle = animProg * 360f, useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset), size = sz
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$accuracy%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Mastered", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("✅", "$masteredCount", "Mastered",  BpscColors.Success)
                    SummaryItem("🔄", "$weakCount",     "Weak",      Color(0xFFE74C3C))
                    SummaryItem("⏭️", "$skippedCount",  "Skipped",   BpscColors.TextSecondary)
                    SummaryItem("🔥", "$streak",        "Best Streak", BpscColors.CoinGold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (weakCount > 0) {
                    Button(
                        onClick  = onRetryWeak,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                    ) {
                        Text("🔄 Retry Weak Cards ($weakCount)", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Button(
                    onClick  = onExit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text("Back to Subjects", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryItem(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun FlashSubjectChip(subject: String) {
    val colors = mapOf(
        "Polity"    to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "History"   to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Economy"   to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK"  to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
        "Science"   to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    )
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun FlashDifficultyChip(difficulty: FlashDifficulty) {
    Text(difficulty.label, style = MaterialTheme.typography.labelSmall, color = difficulty.color, fontSize = 10.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(difficulty.color.copy(0.1f)).padding(horizontal = 8.dp, vertical = 3.dp))
}