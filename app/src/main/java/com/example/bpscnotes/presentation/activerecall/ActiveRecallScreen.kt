package com.example.bpscnotes.presentation.activerecall

import com.example.bpscnotes.data.local.Sm2CardEntity
import com.example.bpscnotes.data.local.isDueToday
import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.ui.platform.LocalContext
import com.example.bpscnotes.core.ui.AppErrorState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.*

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap

// ─────────────────────────────────────────────────────────────
// CONSTANTS
// ─────────────────────────────────────────────────────────────

private const val ADS_EVERY_N_CARDS = 10       // show ad break after every 10 cards
private const val ADS_PER_BREAK     = 1        // show 1 ad per break
private const val AD_DURATION_SECS  = 30       // 30 seconds per ad (auto-advance)

// All AdMob unit IDs live in AdManager (single place to swap for release)
private val AD_UNIT_ID = com.example.bpscnotes.core.ads.AdManager.BANNER_AD_UNIT_ID

// ─────────────────────────────────────────────────────────────
// UI-ONLY MODELS
// ─────────────────────────────────────────────────────────────


enum class CardRating { Mastered, Weak, Skipped }



// ─────────────────────────────────────────────────────────────
// ENTRY POINT
// ─────────────────────────────────────────────────────────────

@Composable
fun ActiveRecallScreen(
    navController: NavHostController,
    adManager: com.example.bpscnotes.core.ads.AdManager? = null,
    viewModel: ActiveRecallViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val state        by viewModel.uiState.collectAsState()
    val masteredIds  = state.masteredIds
    val weakIds      = state.weakIds
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var retryWeak     by remember { mutableStateOf(false) }

    when {
        state.isLoading && state.allCards.isEmpty() -> {
            com.example.bpscnotes.core.ui.ActiveRecallSkeleton()
        }

        state.error != null && state.allCards.isEmpty() ->
            AppErrorState(message = state.error!!, onRetry = { viewModel.retry() })

        activeSubject != null -> {
            val baseCards = if (retryWeak) {
                state.allCards.filter { weakIds.contains(it.id) }
            } else {
                viewModel.cardsForSubject(activeSubject!!)
            }

            // FIX: "random question" on Next/Previous bug.
            //
            // sessionCards used to be recomputed on EVERY recomposition,
            // re-sorting weak/normal/mastered cards based on the LIVE
            // masteredIds/weakIds sets. But rating a card (Next → Mastered,
            // Previous → Weak) updates those sets immediately, which
            // triggered a recomposition that RE-SORTED the list — moving
            // the just-rated card to a different bucket/position. Meanwhile
            // currentIndex (just a plain Int) stayed the same number, so
            // sessionCards[currentIndex] ended up pointing at a completely
            // different card than the one the user expected as "next" or
            // "previous".
            //
            // Fix: snapshot the weak/mastered ordering ONCE when the
            // session starts (keyed on activeSubject/retryWeak so it's
            // recomputed only when a NEW session begins), then keep that
            // fixed order for the rest of the session. Ratings still
            // update masteredIds/weakIds for persistence/progress tracking
            // — they just no longer reshuffle the in-progress session.
            // FIX (round 2): "I need sequence" — the previous fix stopped
            // the deck from RESHUFFLING mid-session, but it still
            // REORDERED the deck into weak-first/normal/mastered buckets
            // at session start. If the user had any previously-rated
            // cards (mastered/weak) from earlier sessions, those jumped
            // to the front or back instead of staying in natural
            // 1,2,3...N order. Just snapshot baseCards in its given
            // (natural/backend) order — no reordering at all. Ratings
            // still update masteredIds/weakIds for progress tracking;
            // they just don't reorder or reshuffle this session's deck.
            // "Retry Weak Cards" (retryWeak=true) is unaffected — it
            // already filters baseCards to weak-only cards above, in
            // their natural relative order.
            // RANDOMIZE: shuffle the deck each session for varied learning
            val sessionCards = remember(activeSubject, retryWeak, baseCards) { baseCards.shuffled() }

            if (sessionCards.isEmpty()) {
                LaunchedEffect(Unit) { activeSubject = null; retryWeak = false }
            } else {
                FlashcardSessionScreen(
                    cards       = sessionCards,
                    masteredIds = masteredIds,
                    weakIds     = weakIds,
                    subjectKey  = activeSubject ?: "",
                    onRate      = { card, rating, currentStreak ->
                        when (rating) {
                            CardRating.Mastered -> {
                                viewModel.markMastered(card.id, currentStreak)
                                viewModel.sm2Rate(card.id, 3)  // Easy
                            }
                            CardRating.Weak     -> {
                                viewModel.markWeak(card.id)
                                viewModel.sm2Rate(card.id, 0)  // Again
                            }
                            CardRating.Skipped  -> viewModel.sm2Rate(card.id, 1) // Hard
                        }
                    },
                    onExit = { activeSubject = null; retryWeak = false }
                )
            }
        }

        else -> {
            FlashcardLobbyScreen(
                subjects      = viewModel.subjects(),
                allCards      = state.allCards,
                masteredIds   = masteredIds,
                weakIds       = weakIds,
                navController = navController,
                onStartSubject = { subject -> activeSubject = subject; retryWeak = false },
                onRetryWeak    = { activeSubject = str.filterAll; retryWeak = true },
                sm2Schedule   = state.sm2Schedule,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AD BREAK SCREEN
// Shows 2 ads back-to-back, each for 2 minutes, then auto-continues
// ─────────────────────────────────────────────────────────────

@Composable
fun FlashcardAdBreakScreen(
    cardsSeenSoFar: Int,
    totalCards: Int = 0,
    onAdBreakComplete: () -> Unit,
) {
    var currentAdIndex by remember { mutableIntStateOf(0) }   // 0 or 1
    var secondsLeft    by remember { mutableIntStateOf(AD_DURATION_SECS) }
    var adLoaded       by remember { mutableStateOf(false) }
    val scope          = rememberCoroutineScope()

    // Countdown timer — auto-advance to next ad or resume after 2 min
    LaunchedEffect(currentAdIndex) {
        secondsLeft = AD_DURATION_SECS
        adLoaded    = false
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        // Timer done — move to next ad or finish
        if (currentAdIndex < ADS_PER_BREAK - 1) {
            currentAdIndex++
        } else {
            onAdBreakComplete()
        }
    }

    // Block back press during ad break — user cannot skip ads by pressing back
    androidx.activity.compose.BackHandler(enabled = true) {
        // Do nothing — back press blocked during ad
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))  // dark background for ad
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Ad counter pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("📺", fontSize = 13.sp)
                        Text(
                            "Ad ${currentAdIndex + 1} of $ADS_PER_BREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Countdown pill
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (secondsLeft <= 10) Color(0xFF8B0000)
                                else Color(0xFF2A2A2A)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            null,
                            tint = if (secondsLeft <= 10) Color(0xFFFF6B6B) else Color(0xFFAAAAAA),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            "%d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (secondsLeft <= 10) Color(0xFFFF6B6B) else Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // ── Progress bar (fills as time runs) ───────────────
            val progress = 1f - (secondsLeft.toFloat() / AD_DURATION_SECS)
            val animProg by animateFloatAsState(progress, tween(800), label = "adprog")
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF2A2A2A))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProg)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5)))
                        )
                )
            }

            // ── Ad content area ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Real AdMob banner ad — key() forces full recreate for each ad slot
                key(currentAdIndex) {
                    AndroidView(
                        factory = { context ->
                            AdView(context).apply {
                                setAdSize(AdSize.LARGE_BANNER)
                                adUnitId = AD_UNIT_ID
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() { adLoaded = true }
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        adLoaded = true
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Loading overlay until ad arrives
                if (!adLoaded) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1565C0),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                        )
                        Text(
                            "Loading ad...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888),
                        )
                    }
                }
            }

            // ── Bottom info bar ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (currentAdIndex < ADS_PER_BREAK - 1)
                                "Next ad in %d:%02d".format(secondsLeft / 60, secondsLeft % 60)
                            else
                                "Resuming in %d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888),
                        )
                    }
                    // Visual dot indicators for ad 1 / ad 2
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(ADS_PER_BREAK) { i ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            i < currentAdIndex  -> Color(0xFF4CAF50)  // done
                                            i == currentAdIndex -> Color(0xFF42A5F5)  // active
                                            else                -> Color(0xFF444444)  // upcoming
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SESSION SCREEN  (with ad break logic wired in)
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashcardSessionScreen(
    cards: List<CoinsApiService.FlashcardDto>,
    masteredIds: Set<String>,
    weakIds: Set<String>,
    subjectKey: String = "",
    onRate: (CoinsApiService.FlashcardDto, CardRating, Int) -> Unit,
    onExit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current

    // ── State hoisted above ad break so it survives when showAdBreak=true causes early return ──
    var cardsCompleted  by remember { mutableIntStateOf(0) }
    var totalCardsInSession by remember { mutableIntStateOf(0) }
    var showAdBreak     by remember { mutableStateOf(false) }

    // Resume from last position — keyed by subject so different subjects track independently
    val prefs = remember { context.getSharedPreferences("flashcard_session_prefs", android.content.Context.MODE_PRIVATE) }
    val prefsKey = if (subjectKey.isNotBlank()) "idx_$subjectKey" else null
    var resumeIndex by remember {
        mutableIntStateOf(
            prefsKey?.let { prefs.getInt(it, 0).coerceIn(0, (cards.size - 1).coerceAtLeast(0)) } ?: 0
        )
    }
    // Streak and ratings MUST be here (not inside the ad-break guard) so they
    // survive the early return — otherwise streak resets to 0 after every ad break
    var streak          by remember { mutableIntStateOf(0) }
    val sessionRatings   = remember { mutableStateMapOf<String, CardRating>() }
    var isComplete      by remember { mutableStateOf(false) }

    // ── AD BREAK ────────────────────────────────────────────
    if (showAdBreak) {
        FlashcardAdBreakScreen(
            cardsSeenSoFar    = cardsCompleted,
            totalCards        = totalCardsInSession,
            onAdBreakComplete = { showAdBreak = false },
        )
        return
    }

    // ── FLASHCARD SESSION ────────────────────────────────────
    var currentIndex    by remember(resumeIndex) { mutableIntStateOf(resumeIndex) }
    var isFlipped       by remember { mutableStateOf(false) }
    val current          = cards.getOrNull(currentIndex)

    val flipAngle by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "flip",
    )

    if (isComplete) {
        FlashcardSummaryScreen(
            cards          = cards,
            sessionRatings = sessionRatings,
            streak         = streak,
            onRetryWeak    = {
                currentIndex  = 0; isFlipped = false; isComplete = false
                sessionRatings.clear(); cardsCompleted = 0
            },
            onExit = onExit,
        )
        return
    }
    if (current == null) { isComplete = true; return }

    val progress = (currentIndex + 1).toFloat() / cards.size
    val animProg by animateFloatAsState(progress, tween(500), label = "prog")

    fun rateAndNext(rating: CardRating) {
        sessionRatings[current.id] = rating
        if (rating == CardRating.Mastered) streak++ else if (rating == CardRating.Weak) streak = 0
        onRate(current, rating, streak)

        val targetIndex = when (rating) {
            CardRating.Weak -> (currentIndex - 1).coerceAtLeast(0)
            else            -> currentIndex + 1
        }

        if (rating != CardRating.Weak && targetIndex > currentIndex) {
            cardsCompleted++
        }

        if (rating != CardRating.Weak &&
            cardsCompleted % ADS_EVERY_N_CARDS == 0 &&
            targetIndex < cards.size
        ) {
            resumeIndex = targetIndex
            prefsKey?.let { prefs.edit().putInt(it, targetIndex).apply() }
            isFlipped           = false
            totalCardsInSession = cards.size
            showAdBreak         = true
        } else if (targetIndex < cards.size) {
            currentIndex = targetIndex
            prefsKey?.let { prefs.edit().putInt(it, targetIndex).apply() }
            isFlipped    = false
        } else {
            prefsKey?.let { prefs.edit().remove(it).apply() }
            isComplete = true
        }
    }

    // Intercept back press — go back to lobby, not navigate up to home
    androidx.activity.compose.BackHandler(enabled = true) {
        onExit()
    }

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable(onClick = onExit), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("${currentIndex + 1} / ${cards.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(34.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                    }
                    // "Tap card to reveal answer" — always visible in header, clearly readable
                    // Requirement: move hint to top section, make clearly visible
                    if (!isFlipped) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(0.12f))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👆", fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                str.recallTapReveal,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(0.9f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    // Ad break countdown (only when close)
                    val cardsUntilAd = ADS_EVERY_N_CARDS - (cardsCompleted % ADS_EVERY_N_CARDS)
                    if (cardsUntilAd <= 3 && currentIndex < cards.size - 1) {
                        Text(
                            "📺 Ad break in $cardsUntilAd card${if (cardsUntilAd == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.6f),
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }

            // Navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left arrow — previous card (Revise Again)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentIndex > 0) Color(0xFFF59E0B).copy(0.15f)
                            else Color.Transparent
                        )
                        .clickable(enabled = currentIndex > 0) {
                            rateAndNext(CardRating.Weak)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = if (currentIndex > 0) Color(0xFFF59E0B) else Color.Transparent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Right arrow — next card (Got it / Mastered)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BpscColors.Success.copy(0.15f))
                        .clickable {
                            rateAndNext(CardRating.Mastered)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = BpscColors.Success,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Flip card
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {

                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer {
                                rotationY = flipAngle
                                cameraDistance = 12f * density
                                alpha = if (flipAngle > 85f && flipAngle < 95f) 0f else 1f
                            }
                            .clickable { isFlipped = !isFlipped },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFlipped && flipAngle > 90f) {
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = -1f }) {
                                CardBackFace(card = current, onRate = ::rateAndNext)
                            }
                        } else if (!isFlipped && flipAngle > 90f) {
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = -1f }) {
                                CardFrontFace(card = current)
                            }
                        } else {
                            CardFrontFace(card = current)
                        }
                    }
                }

                // Hint is now shown in header row (above the card) — not here
            }

            // Bottom actions
            Row(modifier = Modifier.fillMaxWidth().background(cs.surface).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isFlipped) {
                    OutlinedButton(
                        onClick = { rateAndNext(CardRating.Skipped) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cs.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.TextSecondary),
                    ) {
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { isFlipped = true },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    ) {
                        Icon(Icons.Rounded.Flip, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(str.recallRevealAnswer, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = { rateAndNext(CardRating.Weak) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    ) {
                        Text(str.recallReviseAgain, style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { rateAndNext(CardRating.Mastered) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Success),
                    ) {
                        Text(str.recallGotItBtn, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LOBBY
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashcardLobbyScreen(
    subjects: List<String>,
    allCards: List<CoinsApiService.FlashcardDto>,
    masteredIds: Set<String>,
    weakIds: Set<String>,
    navController: NavHostController,
    onStartSubject: (String) -> Unit,
    onRetryWeak: () -> Unit,
    sm2Schedule: Map<String, Sm2CardEntity> = emptyMap(),
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("flashcard_session_prefs", android.content.Context.MODE_PRIVATE) }
    val hasResumePosition = remember { prefs.getInt("idx_${str.filterAll}", 0) > 0 }
    // Filter progress IDs against actual loaded cards to prevent stale/deleted IDs inflating counts
    val allCardIds    = allCards.map { it.id }.toSet()
    val totalCards    = allCards.size
    val masteredCount = masteredIds.count { it in allCardIds }
    val weakCount     = weakIds.count { it in allCardIds }
    val progress      = if (totalCards > 0) masteredCount.toFloat() / totalCards else 0f
    // SM-2: count cards due today
    val dueCount = if (sm2Schedule.isEmpty()) 0
                   else allCards.count { card ->
                       val entry = sm2Schedule[card.id] ?: return@count true
                       entry.isDueToday()
                   }

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)), Offset(0f, 0f), Offset(400f, 400f))).statusBarsPadding()) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                    drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.recallTitle, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.recallSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(str.recallOverallMastery, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$masteredCount / $totalCards cards", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.2f))) {
                            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(4.dp)))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Stats: Mastered + Needs Work only (Unseen removed per spec)
                            StatPill("✅", "$masteredCount", str.recallMastered, Color(0xFF2ECC71))
                            StatPill("🔄", "$weakCount", "Needs Work", Color(0xFFE74C3C))
                            if (sm2Schedule.isNotEmpty()) {
                                StatPill("📅", "$dueCount", "Due Today", Color(0xFFFFD54F))
                            } else {
                                StatPill("📊", "${(progress * 100).toInt()}%", "Progress", Color.White)
                            }
                        }
                    }
                }
            }

            // Compute distinct subjects outside LazyColumn — remember requires @Composable context
            val distinctSubjects = remember(allCards) {
                allCards.map { it.subject }.distinct().filter { it.isNotBlank() }.sorted()
            }

            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (weakCount > 0) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onRetryWeak), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF0F0)), elevation = CardDefaults.cardElevation(0.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE74C3C).copy(0.1f)), contentAlignment = Alignment.Center) { Text("🔄", fontSize = 22.sp) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(str.recallRetryWeak, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                                    Text("$weakCount cards need more practice", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (allCards.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📚", fontSize = 48.sp)
                                Text(str.recallNoCards, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                                Text(str.recallAskAdmin, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                    return@LazyColumn
                }

                item {
                    // ── Start Session Card ────────────────────────────
                    val totalCards   = allCards.size
                    val masteredCount2 = masteredIds.count { it in allCardIds }
                    val weakCount2   = weakIds.count { it in allCardIds }
                    val unseenCount  = (totalCards - masteredCount2 - weakCount2).coerceAtLeast(0)
                    val progress2    = if (totalCards > 0) masteredCount2.toFloat() / totalCards else 0f
                    val animProg by animateFloatAsState(progress2, tween(900), label = "lobby_prog")

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(24.dp),
                        colors    = CardDefaults.cardColors(containerColor = cs.surface),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            // Top row: icon + title
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BpscColors.PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) { Text("📚", fontSize = 28.sp) }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("All Flashcards", style = MaterialTheme.typography.titleLarge,
                                        color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                                    Text("$totalCards cards across all subjects",
                                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                }
                            }

                            // Stats row
                            // Stats row: Mastered + Needs Work only (Total/Unseen removed per spec)
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(cs.background).padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("✅", fontSize = 18.sp)
                                    Text("$masteredCount2", style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF2ECC71), fontWeight = FontWeight.ExtraBold)
                                    Text("Mastered", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                                Box(Modifier.width(1.dp).height(40.dp).background(cs.outline))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("🔄", fontSize = 18.sp)
                                    Text("$weakCount2", style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFE74C3C), fontWeight = FontWeight.ExtraBold)
                                    Text("Needs Work", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                                Box(Modifier.width(1.dp).height(40.dp).background(cs.outline))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("📊", fontSize = 18.sp)
                                    Text("${(progress2 * 100).toInt()}%", style = MaterialTheme.typography.titleMedium,
                                        color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                            }

                            // Progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Overall Mastery", style = MaterialTheme.typography.labelMedium,
                                        color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    Text("${(progress2 * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
                                        color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                                }
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(cs.background)) {
                                    Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                                        .background(Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF42A5F5))),
                                            RoundedCornerShape(4.dp)))
                                }
                            }

                            // Start button
                            Button(
                                onClick   = { onStartSubject(str.filterAll) },
                                modifier  = Modifier.fillMaxWidth().height(52.dp),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                            ) {
                                Text(
                                    if (hasResumePosition) "▶  Resume Session" else "▶  Start Session",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Subject-wise progress rings ───────────────────────────
                if (distinctSubjects.isNotEmpty()) {
                    item {
                        Text(
                            "📖 By Subject",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(distinctSubjects, key = { it }) { subject ->
                        val subjectCards    = allCards.filter { it.subject == subject }
                        val subjectTotal    = subjectCards.size
                        val subjectMastered = masteredIds.count { id -> subjectCards.any { it.id == id } }
                        val subjectWeak     = weakIds.count { id -> subjectCards.any { it.id == id } }
                        val subjectProgress = if (subjectTotal > 0) subjectMastered.toFloat() / subjectTotal else 0f
                        val animSubjProg by animateFloatAsState(subjectProgress, tween(900), label = "subj_${subject}")

                        Card(
                            modifier  = Modifier.fillMaxWidth().clickable { onStartSubject(subject) },
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = cs.surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Circular progress ring
                                Box(
                                    modifier         = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(Modifier.size(56.dp)) {
                                        val stroke = 5.dp.toPx()
                                        val inset  = stroke / 2
                                        val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                                        // Track
                                        drawArc(
                                            color    = Color(0xFFE8EAF6),
                                            startAngle = -90f, sweepAngle = 360f,
                                            useCenter = false,
                                            style    = Stroke(stroke),
                                            topLeft  = Offset(inset, inset), size = sz
                                        )
                                        // Fill
                                        drawArc(
                                            brush    = Brush.sweepGradient(listOf(BpscColors.Primary, Color(0xFF42A5F5))),
                                            startAngle = -90f, sweepAngle = animSubjProg * 360f,
                                            useCenter = false,
                                            style    = Stroke(stroke, cap = StrokeCap.Round),
                                            topLeft  = Offset(inset, inset), size = sz
                                        )
                                    }
                                    Text(
                                        "${(subjectProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.Primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        subject,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$subjectTotal cards · $subjectMastered mastered" +
                                                if (subjectWeak > 0) " · $subjectWeak to review" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant
                                    )
                                    // Mini progress bar
                                    Box(
                                        Modifier.fillMaxWidth().height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(cs.background)
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(animSubjProg).fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF42A5F5))),
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }

                                // Start chip
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BpscColors.PrimaryLight)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "▶",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BpscColors.Primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
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
    val cs = MaterialTheme.colorScheme
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.12f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 11.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// CARD FRONT
// ─────────────────────────────────────────────────────────────

@Composable
private fun CardFrontFace(card: CoinsApiService.FlashcardDto) {
    val cs = MaterialTheme.colorScheme
    val isImageCard = card.cardType == "image" && !card.imageUrl.isNullOrBlank()

    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cs.surface), elevation = CardDefaults.cardElevation(8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Brush.horizontalGradient(listOf(Color(0xFF0A2472), Color(0xFF1E88E5)))))

            if (isImageCard) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlashSubjectChip(card.subject); Spacer(Modifier.weight(1f))
                        Text(card.topic, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    AsyncImage(model = card.imageUrl, contentDescription = "Question image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)))
                    if (card.question.isNotBlank()) {
                        Text(card.question, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                    if (card.hint.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF8E1)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 14.sp); Text(card.hint, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404), lineHeight = 18.sp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlashSubjectChip(card.subject);  Spacer(Modifier.weight(1f))
                        Text(card.topic, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❓", fontSize = 36.sp); Spacer(Modifier.height(16.dp))
                        Text(card.question, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 28.sp)
                    }
                    if (card.hint.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF8E1)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 14.sp); Text(card.hint, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404), lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CARD BACK
// ─────────────────────────────────────────────────────────────

@Composable
private fun CardBackFace(card: CoinsApiService.FlashcardDto, onRate: (CardRating) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var mcqSelected by remember { mutableIntStateOf(-1) }
    var mcqAnswered by remember { mutableStateOf(false) }
    val hasBackImage = !card.backImageUrl.isNullOrBlank()
    val str=LocalStrings.current

    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cs.surface), elevation = CardDefaults.cardElevation(8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Brush.horizontalGradient(listOf(Color(0xFF2ECC71), Color(0xFF1ABC9C)))))
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FlashSubjectChip(card.subject)
                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✅", fontSize = 10.sp); Text(str.recallAnswer, style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str.recallAnswer, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    if (hasBackImage) {
                        AsyncImage(model = card.backImageUrl, contentDescription = "Answer image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F5F5)))
                        if (card.answer.isNotBlank()) { Text(card.answer, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, lineHeight = 22.sp) }
                    } else {
                        Text(card.answer, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, lineHeight = 24.sp)
                    }
                }
                if (card.example.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📌", fontSize = 14.sp)
                        Column { Text(str.recallExample, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(2.dp)); Text(card.example, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, lineHeight = 20.sp) }
                    }
                }
                card.relatedMcq?.let { mcq ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(str.recallRelatedMcq, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text(mcq.question, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, lineHeight = 22.sp)
                        mcq.options.forEachIndexed { index, option ->
                            val isCorrect  = index == mcq.correctIndex
                            val isSelected = index == mcqSelected
                            val bg        = when { !mcqAnswered -> if (isSelected) BpscColors.PrimaryLight else BpscColors.Surface; isCorrect -> Color(0xFFE8FDF4); isSelected && !isCorrect -> Color(0xFFFEE8E8); else -> BpscColors.Surface }
                            val textColor = when { !mcqAnswered -> if (isSelected) BpscColors.Primary else BpscColors.TextPrimary; isCorrect -> BpscColors.Success; isSelected && !isCorrect -> Color(0xFFE74C3C); else -> BpscColors.TextSecondary }
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).clickable(enabled = !mcqAnswered) { mcqSelected = index; mcqAnswered = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(textColor.copy(0.1f)), contentAlignment = Alignment.Center) { Text(listOf("A","B","C","D")[index], style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.ExtraBold) }
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
private fun FlashcardSummaryScreen(cards: List<CoinsApiService.FlashcardDto>, sessionRatings: Map<String, CardRating>, streak: Int, onRetryWeak: () -> Unit, onExit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val masteredCount = sessionRatings.values.count { it == CardRating.Mastered }
    val weakCount     = sessionRatings.values.count { it == CardRating.Weak }
    val skippedCount  = sessionRatings.values.count { it == CardRating.Skipped }
    val accuracy      = if (cards.isNotEmpty()) (masteredCount * 100f / cards.size).toInt() else 0
    val animProg by animateFloatAsState(accuracy / 100f, tween(1200), label = "sum")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), BpscColors.Surface)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.statusBarsPadding()); Spacer(Modifier.height(40.dp))
            Text(if (accuracy >= 80) "🏆" else if (accuracy >= 50) "👍" else "💪", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(when { accuracy >= 80 -> str.quizExcellent; accuracy >= 50 -> str.quizGoodJob; else -> str.quizKeepPracticing }, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(str.recallSessionComplete, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx(); val inset = stroke / 2; val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(0.15f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke), topLeft = Offset(inset, inset), size = sz)
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)), -90f, animProg * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = sz)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$accuracy%", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(str.recallMastered, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }
            Spacer(Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryItem("✅", "$masteredCount", str.recallMastered, BpscColors.Success)
                    SummaryItem("🔄", "$weakCount", "Weak", Color(0xFFE74C3C))
                    SummaryItem("⏭️", "$skippedCount", "Skipped", BpscColors.TextSecondary)
                    SummaryItem("🔥", "$streak", "Best Streak", BpscColors.CoinGold)
                }
            }
            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (weakCount > 0) {
                    Button(onClick = onRetryWeak, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))) {
                        Text("🔄 Retry Weak Cards ($weakCount)", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Text(str.recallBackToSubjects, style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun FlashSubjectChip(subject: String) {
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}