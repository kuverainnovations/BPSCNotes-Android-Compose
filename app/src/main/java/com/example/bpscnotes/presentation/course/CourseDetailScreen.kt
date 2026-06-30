package com.example.bpscnotes.presentation.course

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.CourseReview
import com.example.bpscnotes.data.remote.api.Lesson
import com.example.bpscnotes.data.remote.api.RatingDistribution
import androidx.compose.ui.platform.LocalContext
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.payment.launchCashfree
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
/** Format a rupee amount: whole number when exact (₹10), two decimals when fractional (₹3.30) */
private fun fmtRs(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "₹${amount.toLong()}"
    else "₹${"%.2f".format(amount)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    nav: NavHostController,
    courseId: String,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }

    val state   by viewModel.uiState.collectAsState()
    val str     = LocalStrings.current
    val cs      = MaterialTheme.colorScheme
    val context = LocalContext.current
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("course_detail") }
    var expandedChapter     by remember { mutableStateOf<String?>(null) }
    var showEnrollSuccessDialog by remember { mutableStateOf(false) }
    var showBuyDialog       by remember { mutableStateOf(false) }
    var dialogCoins         by remember { mutableStateOf(0) }

    // Coin economy config for slider
    val coinsConfig = viewModel.coinsConfig

    // ── Course purchase dialog (same design as material purchase) ──
    if (showBuyDialog) {
        val course = state.course
        val price  = state.purchasePrice.takeIf { it > 0 } ?: course?.price ?: 0.0
        val userCoins = state.userCoins
        val coinToInrRate  = coinsConfig.economy.coinToInrRate
        val globalMaxCoins = coinsConfig.economy.maxCoinsPerPurchase
        val perCourseMax   = course?.maxCoinsRedeemable?.takeIf { it > 0 } ?: globalMaxCoins
        val priceInCoins   = if (coinToInrRate > 0) kotlin.math.ceil(price / coinToInrRate).toInt() else 0
        val maxApplicable  = minOf(perCourseMax, userCoins, priceInCoins).coerceAtLeast(0)
        val coinDiscount: Double = minOf(price, dialogCoins * coinToInrRate)
        val amountDue:    Double = (price - coinDiscount).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showBuyDialog = false; dialogCoins = 0 },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Enroll in Course", fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge)
                    Text(course?.title ?: "", style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant, maxLines = 2)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Price row
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Course price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(fmtRs(price), style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                    }
                    // Coin slider
                    if (maxApplicable > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFFBEB))
                                .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪙", fontSize = 18.sp)
                                    Column {
                                        Text("Redeem Coins", style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                        Text("You have $userCoins 🪙 available",
                                            style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309))
                                    }
                                }
                                Box(Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (dialogCoins > 0) BpscColors.CoinGold else Color(0xFFE5E7EB))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                    Text(if (dialogCoins > 0) "−${fmtRs(coinDiscount)}" else "₹0 off",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (dialogCoins > 0) Color.White else Color(0xFF6B7280),
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                            Slider(
                                value         = dialogCoins.toFloat(),
                                onValueChange = { dialogCoins = it.toInt() },
                                valueRange    = 0f..maxApplicable.toFloat(),
                                steps         = 0,
                                colors = SliderDefaults.colors(
                                    thumbColor         = BpscColors.CoinGold,
                                    activeTrackColor   = BpscColors.CoinGold,
                                    inactiveTrackColor = Color(0xFFE5E7EB)
                                )
                            )
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("1 🪙 = ₹$coinToInrRate", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                                Text("$maxApplicable 🪙 max", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            }
                            if (dialogCoins > 0) Text("Using $dialogCoins coins  ·  saves ${fmtRs(coinDiscount)}",
                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = cs.outline)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("You pay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(fmtRs(amountDue), style = MaterialTheme.typography.titleLarge,
                                color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                            if (dialogCoins > 0) Text("🪙 $dialogCoins coins applied (−${fmtRs(coinDiscount)})",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.enroll(courseId, dialogCoins) },
                    enabled  = !state.isEnrolling,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.CoinGold)
                ) {
                    if (state.isEnrolling) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Processing…", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (amountDue == 0.0) "🪙 Pay with coins" else "Pay ${fmtRs(amountDue)}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuyDialog = false; dialogCoins = 0 }) {
                    Text("Cancel", color = cs.onSurfaceVariant)
                }
            }
        )
    }

    LaunchedEffect(state.enrollSuccess) {
        if (state.enrollSuccess) {
            showEnrollSuccessDialog = true
            viewModel.clearMessages()
        }
    }

    if (showEnrollSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollSuccessDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                }
            },
            title = {
                Text("Successfully Enrolled! 🎉",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center)
            },
            text = {
                Text("You're now enrolled in ${state.course?.title ?: "this course"}. Start learning right away!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEnrollSuccessDialog = false
                        nav.navigate(Screen.MyLearningCourses.route)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) { Text("Go to My Courses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollSuccessDialog = false }) {
                    Text("Continue Browsing", color = BpscColors.TextSecondary)
                }
            }
        )
    }

    // Launch Cashfree SDK directly when enroll() returns a 402 with payment session.
    LaunchedEffect(state.purchaseSessionId) {
        val sessionId = state.purchaseSessionId ?: return@LaunchedEffect
        val orderId   = state.purchaseProviderOrderId ?: return@LaunchedEffect
        showBuyDialog = false
        dialogCoins   = 0
        viewModel.clearPurchaseRequired()
        launchCashfree(
            context     = context,
            sessionId   = sessionId,
            orderId     = orderId,
            environment = state.purchasePaymentEnvironment,
            onSuccess   = { cfPaymentId -> viewModel.confirmCoursePurchase(cfPaymentId) },
            onFailure   = { code, msg  -> viewModel.handleCoursePaymentFailure(code, msg) }
        )
    }

    // ── Rating bottom sheet ───────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (state.showRatingSheet) {
        RatingBottomSheet(
            sheetState   = sheetState,
            isSubmitting = state.isSubmittingRating,
            error        = state.ratingError,
            onDismiss    = { viewModel.dismissRatingSheet() },
            onSubmit     = { stars, comment -> viewModel.submitRating(courseId, stars, comment) }
        )
    }

    when {
        state.isLoading -> AppLoader()

        state.error != null && state.course == null -> ErrorState(
            message = state.error!!,
            onRetry = { viewModel.load(courseId) }
        )

        state.course != null -> {
            val course   = state.course!!
            val chapters = state.chapters

            LaunchedEffect(chapters) {
                if (expandedChapter == null && chapters.isNotEmpty())
                    expandedChapter = chapters.first().id
            }

            val totalLessons     = chapters.sumOf { it.lessons?.size ?: 0 }
            val completedLessons = chapters.sumOf { ch ->
                ch.lessons?.count { it.is_completed == true } ?: 0
            }
            val progress   = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
            val animProg   by animateFloatAsState(progress, tween(1000), label = "prog")
            val accent     = subjectAccent(course.subject)
            //val isEnrolled = course.enrollment?.status == "active" || course.enrollment?.status == "completed"

            val isEnrolled =
                course.enrollment?.status in listOf("active", "completed")
            // FIX 1: allDone drives both the bottom bar state AND review banner gate
            val allDone   = isEnrolled && totalLessons > 0 && completedLessons >= totalLessons
            // FIX 2: isRatingSubmitted is now persistent via companion set in ViewModel
            val canReview = allDone && !course.hasReviewed

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val density = LocalDensity.current

            // Collapsing header: collapsed height = status bar + 16dp top padding + 36dp icon row.
            // 64dp was too small and clipped the back button on devices with tall status bars.
            val statusBarDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapsedHeightDp = statusBarDp + 52.dp  // 16dp column padding + 36dp icon row
            var expandedHeightDp by remember { mutableStateOf(500.dp) }
            var expandedHeightLocked by remember { mutableStateOf(false) }

            val scrollOffsetPx by remember {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
                    else listState.firstVisibleItemScrollOffset.toFloat()
                }
            }
            val collapseProgress by remember {
                derivedStateOf {
                    val expandPx  = with(density) { expandedHeightDp.toPx() }
                    val collapsePx = with(density) { collapsedHeightDp.toPx() }
                    val range = expandPx - collapsePx
                    if (range <= 0f) 0f else (scrollOffsetPx / range).coerceIn(0f, 1f)
                }
            }
            val currentHeaderHeightDp by remember {
                derivedStateOf {
                    val expandPx  = with(density) { expandedHeightDp.toPx() }
                    val collapsePx = with(density) { collapsedHeightDp.toPx() }
                    val currentPx = expandPx + (collapsePx - expandPx) * collapseProgress
                    with(density) { currentPx.toDp() }
                }
            }

            // HeroHeader is pinned — only the content below it scrolls
            Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().background(cs.background),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    state          = listState
                ) {
                    // Spacer item[0] — same height as the expanded header.
                    // firstVisibleItemScrollOffset on THIS item = true scroll from the very top
                    // with no 1-frame lag (unlike layoutInfo which is updated in the layout phase).
                    item {
                        Spacer(Modifier.fillMaxWidth().height(expandedHeightDp))
                    }

                    if (state.error != null) {
                        item {
                            Text(
                                state.error!!, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3F3)).padding(12.dp)
                            )
                        }
                    }

                    if (course.whatYouLearn.isNotEmpty()) {
                        item { WhatYouLearnSection(course.whatYouLearn, accent) }
                    }

                    // Certificate banner — TEMP #15: hidden
                    // item { CertificateBanner(...) }

                    item {
                        SectionHeader(
                            title    = str.coursesContent,
                            subtitle = "${chapters.size} chapters · $totalLessons lessons",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }

                    if (chapters.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                Text(str.coursesNoCurriculum, color = cs.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(chapters, key = { it.id }) { chapter ->
                            ChapterCard(
                                chapter     = chapter,
                                accent      = accent,
                                expanded    = expandedChapter == chapter.id,
                                onToggle    = { expandedChapter = if (expandedChapter == chapter.id) null else chapter.id },
                                // FIX 3: Use LessonViewer route (same as lesson tap) not NotesReader
                                onLessonTap = { lesson ->
                                    nav.navigate(Screen.LessonViewer.createRoute(courseId, lesson.id))
                                }
                            )
                        }
                    }

                    // Instructor section hidden per request
                    // if (course.instructor != null) {
                    //     item { InstructorSection(course, accent) }
                    // }

                    // Rate banner: only when all done AND not yet submitted (persistent)
                    if (canReview) {
                        item { RateCourseBanner(accent) { viewModel.showRatingSheet() } }
                    }

                    // Success strip: shown after submission
                    if (course.hasReviewed || state.isRatingSubmitted) {
                        item { ReviewSubmittedBanner() }
                    }

                    // Reviews: 100% from API
                    item {
                        ReviewsSection(
                            reviews            = course.reviews,
                            rating             = course.rating.toFloatOrNull() ?: 0f,
                            reviewCount        = course.review_count,
                            ratingDistribution = course.ratingDistribution,
                            accent             = accent
                        )
                    }
                }

                // ── Pinned collapsing HeroHeader ──────────────
                // graphicsLayer clip runs in the DRAW phase on the GPU —
                // no layout invalidation on every frame → zero jank.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val clipH = currentHeaderHeightDp.toPx()
                            clip = true
                            shape = GenericShape { size, _ ->
                                addRect(Rect(0f, 0f, size.width, clipH))
                            }
                        }
                ) {
                    Box(Modifier.onGloballyPositioned { coords ->
                        val measuredDp = with(density) { coords.size.height.toDp() }
                        if (!expandedHeightLocked && measuredDp > 60.dp) {
                            expandedHeightDp = measuredDp
                            expandedHeightLocked = true
                        }
                    }) {
                        HeroHeader(
                            course, accent, totalLessons, completedLessons, animProg,
                            isEnrolled, collapseProgress
                        ) { nav.popBackStackSafe() }
                    }
                }
            } // end pinned-header Box

            // ── FAB for all CTAs ──────────────────────────────────────────
            Box(
                Modifier.fillMaxSize().padding(bottom = 24.dp, end = 20.dp).navigationBarsPadding(),
                contentAlignment = Alignment.BottomEnd
            ) {
                when {
                    // Course completed — green pill chip
                    isEnrolled && allDone -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BpscColors.Success)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(str.courseCourseCompleted, style = MaterialTheme.typography.labelLarge,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Enrolled, not done: Start / Continue Learning FAB
                    isEnrolled -> {
                        val onContinue = {
                            val next = chapters
                                .flatMap { it.lessons ?: emptyList() }
                                .firstOrNull { it.is_completed != true && !it.is_locked }
                            if (next != null) nav.navigate(Screen.LessonViewer.createRoute(courseId, next.id))
                        }
                        ExtendedFloatingActionButton(
                            onClick        = onContinue,
                            containerColor = accent,
                            contentColor   = Color.White,
                            icon           = { Icon(Icons.Rounded.PlayArrow, null) },
                            text           = { Text(if (completedLessons > 0) str.courseContinueLearning else str.courseStartLearning, fontWeight = FontWeight.Bold) }
                        )
                    }
                    // Not enrolled, paid: Buy FAB → dialog → Cashfree
                    course.isPaid -> {
                        ExtendedFloatingActionButton(
                            onClick        = { if (!state.isEnrolling) { showBuyDialog = true; dialogCoins = 0 } },
                            containerColor = BpscColors.CoinGold,
                            contentColor   = Color.White,
                            icon           = { Icon(Icons.Rounded.ShoppingCart, null) },
                            text           = { Text("Buy — ${fmtRs(course.price ?: 0.0)}", fontWeight = FontWeight.Bold) }
                        )
                    }
                    // Not enrolled, free: Enroll Free FAB
                    else -> {
                        ExtendedFloatingActionButton(
                            onClick        = { if (!state.isEnrolling) viewModel.enroll(courseId) },
                            containerColor = if (state.isEnrolling) accent.copy(alpha = 0.6f) else accent,
                            contentColor   = Color.White,
                            icon           = {
                                if (state.isEnrolling)
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else
                                    Icon(Icons.Rounded.School, null)
                            },
                            text = { Text(str.courseEnrollFree, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RATING BOTTOM SHEET
// ─────────────────────────────────────────────────────────────

// FIX: enrollment_count is now a live count of real enrollments
// (previously a static seeded vanity number like 3000-5000, always
// showing "3k students" regardless of actual enrollment). For real
// counts under 1000 — the common case — show the exact number with
// proper singular/plural; only abbreviate to "Xk" for 1000+.
private fun formatStudentCount(count: Int): String = when {
    count <= 0   -> "No students yet"
    count == 1   -> "1 student"
    count < 1000 -> "$count students"
    count < 10000 -> "%.1fk students".format(count / 1000f)
    else         -> "${count / 1000}k students"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBottomSheet(
    sheetState:   SheetState,
    isSubmitting: Boolean,
    error:        String?,
    onDismiss:    () -> Unit,
    onSubmit:     (Int, String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var selectedStars by remember { mutableIntStateOf(0) }
    var comment       by remember { mutableStateOf("") }
    val haptic        = LocalHapticFeedback.current
    val starLabels    = listOf("", "Poor", "Fair", "Good", "Great", "Excellent!")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDE1E8)))
            }
        }
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(str.coursesRate, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
                Text(str.coursesRateSubtitle,
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }

            // Stars
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                Row(Modifier.align(Alignment.CenterHorizontally), Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        val scale by animateFloatAsState(
                            if (star <= selectedStars) 1.3f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "s$star"
                        )
                        Icon(
                            Icons.Rounded.Star, "$star stars",
                            tint     = if (star <= selectedStars) Color(0xFFFFD700) else Color(0xFFDDE1E8),
                            modifier = Modifier
                                .size(52.dp)
                                .scale(scale)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) {
                                    selectedStars = star
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                        )
                    }
                }

                AnimatedContent(
                    targetState  = if (selectedStars > 0) starLabels[selectedStars] else str.courseTapStar,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "lbl"
                ) { lbl ->
                    Text(lbl, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = if (selectedStars > 0) Color(0xFFFFB300) else BpscColors.TextHint)
                }
            }

            HorizontalDivider(color = cs.outline)

            // Comment
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${str.coursesRate} (Optional)", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    placeholder   = {
                        Text(str.coursesRateSubtitle,
                            color = BpscColors.TextHint, style = MaterialTheme.typography.bodyMedium)
                    },
                    modifier  = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                    shape     = RoundedCornerShape(14.dp),
                    minLines  = 4, maxLines = 6,
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = BpscColors.Primary,
                        unfocusedBorderColor    = cs.outline,
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = BpscColors.Surface
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface)
                )
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    Text("${comment.length}/500", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                }
            }

            if (error != null) {
                Text(
                    error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3F3)).padding(10.dp)
                )
            }

            Button(
                onClick  = { if (selectedStars > 0 && !isSubmitting) onSubmit(selectedStars, comment) },
                enabled  = selectedStars > 0 && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFFFD700),
                    contentColor           = Color(0xFF1A1A1A),
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor   = Color(0xFF9E9E9E)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color(0xFF1A1A1A), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.coursesSubmitReview, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RATE COURSE BANNER  — shown only after 100 % completion
// ─────────────────────────────────────────────────────────────

@Composable
private fun RateCourseBanner(accent: Color, onTap: () -> Unit) {
    val str = LocalStrings.current
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        0.08f, 0.18f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "glow"
    )
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFF0D1B5E), Color(0xFF1565C0), accent), Offset.Zero, Offset(700f, 300f))
        )) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(glow), 90.dp.toPx(), Offset(size.width - 30.dp.toPx(), 10.dp.toPx()))
                drawCircle(Color.White.copy(glow * 0.6f), 50.dp.toPx(), Offset(10.dp.toPx(), size.height))
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(0.15f)), Alignment.Center) {
                        Text("🎓", fontSize = 26.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(str.coursesCompleted, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(str.coursesBeFirst,
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), lineHeight = 18.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(6.dp))
                    Text(str.coursesRate, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
                }
                Button(
                    onClick  = onTap,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.coursesRate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEW SUBMITTED SUCCESS STRIP
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReviewSubmittedBanner() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8F5E9))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(20.dp))
        Text(
            str.coursesThankYou,
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEWS SECTION — 100 % API-driven
// FIX: null and emptyList both show str.coursesNoReviews — no infinite spinner
// ─────────────────────────────────────────────────────────────

private const val PAGE_SIZE = 10

@Composable
private fun ReviewsSection(
    reviews:            List<CourseReview>?,   // null when API returns JSON null
    rating:             Float,
    reviewCount:        Int,
    ratingDistribution: RatingDistribution?,
    accent:             Color
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    // FIX: treat null as empty — API returns null when no reviews exist, not as a loading indicator
    val reviewList = reviews ?: emptyList()

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(
                str.coursesReviews,
                style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (reviewCount > 0) {
                Text("$reviewCount ${str.coursesReviews}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1)
            }
        }

        // Summary card — only when real data exists
        if (rating > 0f || reviewCount > 0) {
            RatingSummaryCard(rating, reviewCount, ratingDistribution)
        }

        // Reviews or empty state — never a spinner here
        if (reviewList.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.surface)
                    .padding(vertical = 36.dp),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💬", fontSize = 36.sp)
                    Text(str.coursesNoReviews, style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(str.coursesBeFirst,
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }
        } else {
            var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }
            val displayed = reviewList.take(visibleCount)

            displayed.forEachIndexed { index, review ->
                key(review.id ?: index) {
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(tween(160, delayMillis = (index % PAGE_SIZE) * 30)) +
                                slideInVertically(tween(160, delayMillis = (index % PAGE_SIZE) * 30)) { it / 4 }
                    ) {
                        ReviewCard(review)
                    }
                }
            }

            if (visibleCount < reviewList.size) {
                val remaining = reviewList.size - visibleCount
                OutlinedButton(
                    onClick  = { visibleCount += PAGE_SIZE },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.5.dp, accent.copy(0.4f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${str.seeAll} ($remaining)",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            if (visibleCount > PAGE_SIZE) {
                TextButton(onClick = { visibleCount = PAGE_SIZE }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ExpandLess, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(str.profileShowLess, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// RATING SUMMARY CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun RatingSummaryCard(rating: Float, reviewCount: Int, ratingDistribution: RatingDistribution?) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f".format(rating), style = MaterialTheme.typography.displaySmall,
                    color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(5) { i ->
                        val filled = i < rating
                        val half   = !filled && i < (rating + 0.5f)
                        Icon(
                            when { filled -> Icons.Rounded.Star; half -> Icons.Rounded.StarHalf; else -> Icons.Rounded.StarOutline },
                            null,
                            tint     = if (filled || half) Color(0xFFFFD700) else cs.outline,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text("$reviewCount ${str.coursesReviews}", style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(5, 4, 3, 2, 1).forEach { star ->
                    val pct  = ratingDistribution?.pct(star) ?: 0f
                    val anim by animateFloatAsState(pct, tween(700, delayMillis = (5 - star) * 70), label = "b$star")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("$star", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                            modifier = Modifier.width(10.dp), textAlign = TextAlign.Center)
                        Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(cs.background)) {
                            Box(
                                Modifier.fillMaxWidth(anim).fillMaxHeight().background(
                                    Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))),
                                    RoundedCornerShape(4.dp)
                                )
                            )
                        }
                        Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextHint, fontSize = 9.sp,
                            modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEW CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(review: CourseReview) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val avatarColors = listOf(
        Color(0xFF9B59B6), Color(0xFF1565C0), Color(0xFF2ECC71),
        Color(0xFFE67E22), Color(0xFFE74C3C), Color(0xFF16A085), Color(0xFF2C3E50)
    )
    val color    = avatarColors[(review.reviewerName.hashCode() and 0x7FFFFFFF) % avatarColors.size]
    val initials = review.reviewerName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("")

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Row(Modifier.weight(1f), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(color), Alignment.Center) {
                        initials?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            review.reviewerName?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyLarge, color = cs.onSurface,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            if (review.isVerified) {
                                Row(
                                    Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE3F2FD)).padding(horizontal = 5.dp, vertical = 2.dp),
                                    Arrangement.spacedBy(2.dp), Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Verified, null, tint = BpscColors.Primary, modifier = Modifier.size(10.dp))
                                    Text(str.coursesVerified, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (!review.createdAt.isNullOrEmpty()) {
                            Text(formatDate(review.createdAt), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        Icon(Icons.Rounded.Star, null,
                            tint     = if (i < review.rating.toInt()) Color(0xFFFFD700) else cs.outline,
                            modifier = Modifier.size(13.dp))
                    }
                }
            }
            if (!review.comment.isNullOrBlank()) {
                Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, lineHeight = 22.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HERO HEADER
// ─────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    course: CourseDto, accent: Color, totalLessons: Int,
    completedLessons: Int, animProg: Float, isEnrolled: Boolean,
    collapseProgress: Float,
    onBack: () -> Unit
) {
    val str = LocalStrings.current
    // Detail content fades out in the first 60% of collapse; compact title fades in at 40%+
    val detailAlpha = (1f - collapseProgress * 1.8f).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.35f) * 2.5f).coerceIn(0f, 1f)

    Box(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(
                listOf(
                    accent.copy(red = accent.red * 0.5f, green = accent.green * 0.5f, blue = accent.blue * 0.5f),
                    accent
                ),
                Offset.Zero, Offset(400f, 300f)
            )
        ).statusBarsPadding()
    ) {
        if (detailAlpha > 0f) {
            Canvas(Modifier.matchParentSize().alpha(detailAlpha)) {
                drawCircle(Color.White.copy(0.06f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top row: back + compact title (on collapse) + share
            val shareContext = androidx.compose.ui.platform.LocalContext.current
            val courseTitle  = course.title
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable(onClick = onBack), Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                if (compactTitleAlpha > 0f) {
                    Text(
                        courseTitle,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).alpha(compactTitleAlpha),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f))
                        .clickable {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Check this BPSC course: $courseTitle")
                                putExtra(android.content.Intent.EXTRA_TEXT,
                                    "I'm studying \"$courseTitle\" on BPSCNotes app! Join me → https://bpscnotes.in/courses/${course.id}")
                            }
                            shareContext.startActivity(android.content.Intent.createChooser(shareIntent, str.caShare))
                        },
                    Alignment.Center
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            // Detail content — fades out on collapse
            if (detailAlpha > 0f) {
                Column(
                    modifier = Modifier.alpha(detailAlpha),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(course.subject, accent, Color.White)
                        if (course.isPaid) Badge("PRO", BpscColors.CoinGold, Color(0xFFFFF8E1))
                        else Badge(str.coursesFree, BpscColors.Success, Color(0xFFE8FDF4))
                    }
                    Text(course.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                        fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    val ratingFloat = course.rating.toFloatOrNull() ?: 0f
                    if (ratingFloat > 0f || course.review_count > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(5) { i ->
                                    Icon(Icons.Rounded.Star, null,
                                        tint = if (i < ratingFloat.toInt()) Color(0xFFFFD700) else Color.White.copy(0.3f),
                                        modifier = Modifier.size(13.dp))
                                }
                                Text("$ratingFloat", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("(${course.review_count})", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                            Text("·", color = Color.White.copy(0.5f))
                            Text(formatStudentCount(course.enrollmentCount),
                                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(Icons.Rounded.PlayLesson, "$totalLessons lessons")
                        InfoPill(Icons.Rounded.Schedule, "${course.totalHours}h total")
                        if (course.language.isNotBlank() && course.language != str.courseHindiEnglish) {
                            InfoPill(Icons.Rounded.Language, course.language)
                        }
                    }
                    if (isEnrolled && totalLessons > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(str.courseYourProgress, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                Text("$completedLessons/$totalLessons · ${(animProg * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                                Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Color.White, RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WHAT YOU'LL LEARN
// ─────────────────────────────────────────────────────────────

@Composable
private fun WhatYouLearnSection(items: List<String>, accent: Color) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(str.courseWhatYouLearn, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { item ->
                            Row(Modifier.weight(1f), Arrangement.spacedBy(6.dp), Alignment.Top) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                                Text(item, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, lineHeight = 18.sp)
                            }
                        }
                        if (pair.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CERTIFICATE BANNER
// ─────────────────────────────────────────────────────────────

@Composable
private fun CertificateBanner(
    isComplete: Boolean = false,
    courseName: String = "",
    certificateUrl: String? = null,
    certificateId: String? = null,
    isDownloading: Boolean = false,
    viewModel: CourseDetailViewModel? = null,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        RoundedCornerShape(16.dp), CardDefaults.cardColors(), CardDefaults.cardElevation(3.dp)) {
        Box(Modifier.fillMaxWidth().background(
            Brush.linearGradient(if (isComplete) listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
            else listOf(Color(0xFF0A2472), Color(0xFF1565C0)))
        )) {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isComplete) "🎓" else "🏆", fontSize = 32.sp)
                    Column {
                        Text(if (isComplete) str.courseCertEarned else str.courseCertTitle,
                            style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(if (isComplete) str.courseCertTap
                        else str.courseCertComplete,
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f))
                    }
                }
                if (isComplete) {
                    Button(
                        onClick = {
                            val shareText = "I just completed '$courseName' on BPSCNotes! 🎓"
                            // FIX: previously this ALWAYS shared plain text only.
                            // Now: if the certificate PDF has been generated,
                            // download it and attach the real file. Falls back
                            // to text-only sharing if no certificate yet or the
                            // download fails.
                            if (certificateUrl != null && certificateId != null && viewModel != null) {
                                scope.launch {
                                    val uri = viewModel.downloadCertificateForShare(certificateUrl, certificateId)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        if (uri != null) {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        } else {
                                            type = "text/plain"
                                        }
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, str.courseShareCert))
                                }
                            } else {
                                // Certificate still being generated — share text for now
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, str.courseShareCert))
                            }
                        },
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = cs.surface),
                        shape  = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF1B5E20))
                        } else {
                            Text(str.courseShareCertBtn, color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CHAPTER CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun ChapterCard(
    chapter: Chapter, accent: Color, expanded: Boolean,
    onToggle: () -> Unit, onLessonTap: (Lesson) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val lessons = chapter.lessons ?: emptyList()
    val done    = lessons.count { it.is_completed == true }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        RoundedCornerShape(14.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val allComplete = done == lessons.size && lessons.isNotEmpty()
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(if (allComplete) BpscColors.Success.copy(0.1f) else accent.copy(0.1f)), Alignment.Center) {
                    Icon(if (allComplete) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle, null,
                        tint = if (allComplete) BpscColors.Success else accent, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$done/${lessons.size} completed", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null,
                    tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                HorizontalDivider(color = cs.outline)
                lessons.forEachIndexed { i, lesson ->
                    LessonRow(lesson, accent) { if (!lesson.is_locked) onLessonTap(lesson) }
                    if (i < lessons.size - 1)
                        HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = cs.outline, thickness = 0.5.dp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, accent: Color, onTap: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (lesson.is_locked) 0.5f else 1f)
            .clickable(enabled = !lesson.is_locked, onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(
            when { lesson.is_completed == true -> BpscColors.Success.copy(0.15f); lesson.is_locked -> BpscColors.Surface; else -> accent.copy(0.1f) }
        ), Alignment.Center) {
            when {
                lesson.is_completed == true -> Icon(Icons.Rounded.Check, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                lesson.is_locked            -> Icon(Icons.Rounded.Lock, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                else                        -> Text("📄", fontSize = 12.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge,
                color = if (lesson.is_locked) BpscColors.TextHint else BpscColors.TextPrimary,
                fontWeight = if (lesson.is_completed == true) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (lesson.duration_mins > 0)
                Text("${lesson.duration_mins}min", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
        }
        if (lesson.is_completed == true)
            Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// ABOUT INSTRUCTOR
// ─────────────────────────────────────────────────────────────

@Composable
private fun InstructorSection(course: CourseDto, accent: Color) {
    val cs = MaterialTheme.colorScheme
    val str= LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(str.courseAboutInstructor, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val initials = course.instructor?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "?"
                    Box(Modifier.size(56.dp).clip(CircleShape).background(accent.copy(0.15f)).border(2.dp, accent, CircleShape), Alignment.Center) {
                        Text(initials, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(course.instructor ?: "", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                        Text(str.courseBpscExpert, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!course.instructorStudents.isNullOrEmpty())
                        InstructorStat(Icons.Rounded.People, "${course.instructorStudents} students")
                    InstructorStat(Icons.Rounded.PlayLesson, "${course.instructorCourses} courses")
                    InstructorStat(Icons.Rounded.Star, "${course.rating} rating")
                }
                if (!course.instructorBio.isNullOrBlank())
                    Text(course.instructorBio, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, lineHeight = 22.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BOTTOM CTA
// FIX: allDone = show str.courseCourseCompleted chip instead of Continue button
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomCta(
    course:          CourseDto,
    accent:          Color,
    isEnrolled:      Boolean,
    allDone:         Boolean,
    isEnrolling:     Boolean,
    onEnroll:        () -> Unit,
    onShowBuyDialog: () -> Unit,   // for paid courses — shows dialog before network call
    onContinue:      () -> Unit,
    completedLessons: Int
) {
    val str = LocalStrings.current
    Surface(Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
        Box(
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            when {
                // ── Course fully done: show a non-interactive "completed" chip ──
                isEnrolled && allDone -> {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BpscColors.Success.copy(0.12f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint     = BpscColors.Success,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            str.courseCourseCompleted,
                            style      = MaterialTheme.typography.titleMedium,
                            color      = BpscColors.Success,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // ── Enrolled but not done: Continue Learning ──
                isEnrolled -> {
                    Button(
                        onClick  = onContinue,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        // FIX 3: Show str.courseStartLearning if 0 lessons done, str.courseContinueLearning if started
                        Text(
                            if (completedLessons > 0) str.courseContinueLearning else str.courseStartLearning,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // ── Not enrolled: Enroll / Buy button ──
                else -> {
                    Button(
                        onClick  = { if (course.isPaid) onShowBuyDialog() else onEnroll() },
                        enabled  = !isEnrolling,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (course.isPaid) BpscColors.CoinGold else accent
                        )
                    ) {
                        if (isEnrolling) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (course.isPaid) "Buy — ${fmtRs(course.price ?: 0.0)}" else str.courseEnrollFree,
                                style = MaterialTheme.typography.titleMedium
                            )
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
private fun SectionHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(
            title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun Badge(text: String, textColor: Color, bgColor: Color) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bgColor).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.2f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun InstructorStat(icon: ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.retry)
            }
        }
    }
}

private fun subjectAccent(subject: String): Color = when (subject) {
    "Bihar GK"            -> Color(0xFF2ECC71)
    "Polity"              -> Color(0xFF9B59B6)
    "Economy", "Economics"-> Color(0xFFE67E22)
    "Geography"           -> Color(0xFF1ABC9C)
    "History"             -> Color(0xFFE74C3C)
    "Science & Tech"      -> Color(0xFF2980B9)
    else                  -> Color(0xFF1565C0)
}

private fun formatDate(iso: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(sdf.parse(iso)!!)
} catch (_: Exception) { iso.take(10) }