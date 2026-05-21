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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.CourseReview
import com.example.bpscnotes.data.remote.api.Lesson
import com.example.bpscnotes.data.remote.api.RatingDistribution
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    nav: NavHostController,
    courseId: String,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }

    val state by viewModel.uiState.collectAsState()
    var expandedChapter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.enrollSuccess) {
        if (state.enrollSuccess) viewModel.clearMessages()
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
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }

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

            // FIX 4: Preserve scroll position when returning from LessonViewer
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                modifier       = Modifier.fillMaxSize().background(BpscColors.Surface),
                contentPadding = PaddingValues(bottom = 110.dp),
                state          = listState
            ) {

                item {
                    HeroHeader(
                        course, accent, totalLessons, completedLessons, animProg, isEnrolled
                    ) { nav.popBackStack() }
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

                if (course.hasCertificate) {
                    item { CertificateBanner() }
                }

                item {
                    SectionHeader(
                        title    = "📋 Course Content",
                        subtitle = "${chapters.size} chapters · $totalLessons lessons",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }

                if (chapters.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text("No curriculum available yet.", color = BpscColors.TextSecondary)
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

                if (course.instructor != null) {
                    item { InstructorSection(course, accent) }
                }

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

            // FIX 4: Bottom bar — "Completed" state when all lessons done
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BottomCta(
                    course      = course,
                    accent      = accent,
                    isEnrolled  = isEnrolled,
                    allDone     = allDone,
                    isEnrolling = state.isEnrolling,
                    onEnroll    = { viewModel.enroll(courseId) },
                    // FIX 3: Correct route — same LessonViewer used by lesson tap
                    onContinue  = {
                        val next = chapters
                            .flatMap { it.lessons ?: emptyList() }
                            .firstOrNull { it.is_completed != true && !it.is_locked }
                        if (next != null) {
                            nav.navigate(Screen.LessonViewer.createRoute(courseId, next.id))
                        }
                    },
                    completedLessons=completedLessons
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RATING BOTTOM SHEET
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBottomSheet(
    sheetState:   SheetState,
    isSubmitting: Boolean,
    error:        String?,
    onDismiss:    () -> Unit,
    onSubmit:     (Int, String) -> Unit
) {
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
                Text("Rate this Course", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = BpscColors.TextPrimary)
                Text("Your feedback helps thousands of students",
                    style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
                    targetState  = if (selectedStars > 0) starLabels[selectedStars] else "Tap a star to rate",
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "lbl"
                ) { lbl ->
                    Text(lbl, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = if (selectedStars > 0) Color(0xFFFFB300) else BpscColors.TextHint)
                }
            }

            HorizontalDivider(color = BpscColors.Divider)

            // Comment
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Write a Review  (Optional)", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, color = BpscColors.TextPrimary)
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    placeholder   = {
                        Text("Share what you liked or what could be improved…",
                            color = BpscColors.TextHint, style = MaterialTheme.typography.bodyMedium)
                    },
                    modifier  = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                    shape     = RoundedCornerShape(14.dp),
                    minLines  = 4, maxLines = 6,
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = BpscColors.Primary,
                        unfocusedBorderColor    = BpscColors.Divider,
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = BpscColors.Surface
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = BpscColors.TextPrimary)
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
                    Text("Submit Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
                        Text("You've completed this course!", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Help others by sharing your experience",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f), lineHeight = 18.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(6.dp))
                    Text("Rate now", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
                }
                Button(
                    onClick  = onTap,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Rounded.RateReview, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Write a Review", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
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
            "Thank you! Your review has been submitted.",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEWS SECTION — 100 % API-driven
// FIX: null and emptyList both show "No reviews yet" — no infinite spinner
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
    // FIX: treat null as empty — API returns null when no reviews exist, not as a loading indicator
    val reviewList = reviews ?: emptyList()

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(
                "⭐ Student Reviews",
                style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold
            )
            if (reviewCount > 0) {
                Text("$reviewCount reviews", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
                    .background(Color.White)
                    .padding(vertical = 36.dp),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💬", fontSize = 36.sp)
                    Text("No reviews yet", style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Be the first to share your experience!",
                        style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary,
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
                    Text("Show $remaining more reviews",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            if (visibleCount > PAGE_SIZE) {
                TextButton(onClick = { visibleCount = PAGE_SIZE }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ExpandLess, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Show less", style = MaterialTheme.typography.bodyMedium)
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
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f".format(rating), style = MaterialTheme.typography.displaySmall,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(5) { i ->
                        val filled = i < rating
                        val half   = !filled && i < (rating + 0.5f)
                        Icon(
                            when { filled -> Icons.Rounded.Star; half -> Icons.Rounded.StarHalf; else -> Icons.Rounded.StarOutline },
                            null,
                            tint     = if (filled || half) Color(0xFFFFD700) else BpscColors.Divider,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text("$reviewCount ratings", style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(5, 4, 3, 2, 1).forEach { star ->
                    val pct  = ratingDistribution?.pct(star) ?: 0f
                    val anim by animateFloatAsState(pct, tween(700, delayMillis = (5 - star) * 70), label = "b$star")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("$star", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary,
                            modifier = Modifier.width(10.dp), textAlign = TextAlign.Center)
                        Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(BpscColors.Surface)) {
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
                                    style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary,
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
                                    Text("Verified", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                            tint     = if (i < review.rating.toInt()) Color(0xFFFFD700) else BpscColors.Divider,
                            modifier = Modifier.size(13.dp))
                    }
                }
            }
            if (!review.comment.isNullOrBlank()) {
                Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 22.sp)
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
    completedLessons: Int, animProg: Float, isEnrolled: Boolean, onBack: () -> Unit
) {
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
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.06f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.7f))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable(onClick = onBack), Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                // FIX 5: Share now actually works
                val shareContext = androidx.compose.ui.platform.LocalContext.current
                val courseTitle  = course.title
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f))
                        .clickable {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"

                                putExtra(
                                    android.content.Intent.EXTRA_SUBJECT,
                                    "Check this BPSC course: $courseTitle"
                                )

                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "I'm studying \"$courseTitle\" on BPSCNotes app! Join me → https://bpscnotes.in/courses/${course.id}"
                                )
                            }
                            shareContext.startActivity(android.content.Intent.createChooser(shareIntent, "Share Course"))
                        },
                    Alignment.Center
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(course.subject, accent, Color.White)
                if (course.isPaid) Badge("PRO", BpscColors.CoinGold, Color(0xFFFFF8E1))
                else Badge("FREE", BpscColors.Success, Color(0xFFE8FDF4))
            }
            Text(course.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            course.instructor?.let {
                Text("By $it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
            }
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
                    Text("${(course.enrollmentCount / 1000f).toInt()}k students",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(Icons.Rounded.PlayLesson, "$totalLessons lessons")
                InfoPill(Icons.Rounded.Schedule, "${course.totalHours}h total")
                InfoPill(Icons.Rounded.Language, course.language)
            }
            if (isEnrolled && totalLessons > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Your Progress", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
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

// ─────────────────────────────────────────────────────────────
// WHAT YOU'LL LEARN
// ─────────────────────────────────────────────────────────────

@Composable
private fun WhatYouLearnSection(items: List<String>, accent: Color) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📚 What You'll Learn", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { item ->
                            Row(Modifier.weight(1f), Arrangement.spacedBy(6.dp), Alignment.Top) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                                Text(item, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, lineHeight = 18.sp)
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
private fun CertificateBanner() {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        RoundedCornerShape(16.dp), CardDefaults.cardColors(), CardDefaults.cardElevation(3.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))) {
            Row(Modifier.padding(16.dp), Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
                Text("🏆", fontSize = 32.sp)
                Column {
                    Text("Certificate of Completion", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Earn a verified certificate after completing all lessons",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
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
                    Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$done/${lessons.size} completed", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null,
                    tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                HorizontalDivider(color = BpscColors.Divider)
                lessons.forEachIndexed { i, lesson ->
                    LessonRow(lesson, accent) { if (!lesson.is_locked) onLessonTap(lesson) }
                    if (i < lessons.size - 1)
                        HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, accent: Color, onTap: () -> Unit) {
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("👨‍🏫 About the Instructor", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val initials = course.instructor?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "?"
                    Box(Modifier.size(56.dp).clip(CircleShape).background(accent.copy(0.15f)).border(2.dp, accent, CircleShape), Alignment.Center) {
                        Text(initials, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(course.instructor ?: "", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                        Text("BPSC Subject Expert", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!course.instructorStudents.isNullOrEmpty())
                        InstructorStat(Icons.Rounded.People, "${course.instructorStudents} students")
                    InstructorStat(Icons.Rounded.PlayLesson, "${course.instructorCourses} courses")
                    InstructorStat(Icons.Rounded.Star, "${course.rating} rating")
                }
                if (!course.instructorBio.isNullOrBlank())
                    Text(course.instructorBio, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 22.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BOTTOM CTA
// FIX: allDone = show "Course Completed" chip instead of Continue button
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomCta(
    course:      CourseDto,
    accent:      Color,
    isEnrolled:  Boolean,
    allDone:     Boolean,            // NEW param
    isEnrolling: Boolean,
    onEnroll:    () -> Unit,
    onContinue:  () -> Unit,
    completedLessons: Int
) {
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
                            "Course Completed",
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
                        // FIX 3: Show "Start Learning" if 0 lessons done, "Continue Learning" if started
                        Text(
                            if (completedLessons > 0) "Continue Learning" else "Start Learning",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // ── Not enrolled: Enroll button ──
                else -> {
                    Button(
                        onClick  = onEnroll,
                        enabled  = !isEnrolling,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        if (isEnrolling) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (course.isPaid) "Enroll — ₹${course.price}" else "Enroll Free",
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
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun Badge(text: String, textColor: Color, bgColor: Color) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bgColor).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.2f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun InstructorStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Retry")
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