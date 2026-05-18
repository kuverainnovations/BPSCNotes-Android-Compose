package com.example.bpscnotes.presentation.course

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.Lesson
import com.example.bpscnotes.data.remote.api.CourseReview
import com.example.bpscnotes.data.remote.api.RatingDistribution
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
// DATA CLASSES (chapter / lesson already defined elsewhere)
// ─────────────────────────────────────────────────────────────

// Chapter = Chapter (from AppApiService — use project class directly)
// Lesson  = Lesson  (from AppApiService — use project class directly)
// Defined in AppApiService.kt as data class Chapter / data class Lesson

// ─────────────────────────────────────────────────────────────
// UI STATE + VIEW MODEL
// ─────────────────────────────────────────────────────────────

data class CourseDetailUiState(
    val course: CourseDto? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val enrollSuccess: Boolean = false,
    val isEnrolling: Boolean = false
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

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
            val completedLessons = chapters.sumOf { ch -> ch.lessons?.count { it.is_completed == true } ?: 0 }
            val progress         = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
            val animProg         by animateFloatAsState(progress, tween(1000), label = "prog")

            val accent = subjectAccent(course.subject)
            val isEnrolled = course.enrollment?.status == "active"

            LazyColumn(
                modifier       = Modifier.fillMaxSize().background(BpscColors.Surface),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {

                // ── HERO HEADER ───────────────────────────────
                item {
                    HeroHeader(
                        course           = course,
                        accent           = accent,
                        totalLessons     = totalLessons,
                        completedLessons = completedLessons,
                        animProg         = animProg,
                        isEnrolled       = isEnrolled,
                        onBack           = { nav.popBackStack() }
                    )
                }

                // ── ERROR BANNER ──────────────────────────────
                if (state.error != null) {
                    item {
                        Text(
                            state.error!!,
                            color    = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3F3)).padding(12.dp)
                        )
                    }
                }

                // ── CTA CARD ──────────────────────────────────
                //item { CtaCard(course = course, state = state, isEnrolled = isEnrolled, courseId = courseId, viewModel = viewModel) }

                // ── WHAT YOU'LL LEARN ─────────────────────────
                if (course.whatYouLearn.isNotEmpty()) {
                    item { WhatYouLearnSection(items = course.whatYouLearn, accent = accent) }
                }

                // ── CERTIFICATE ───────────────────────────────
                if (course.hasCertificate) {
                    item { CertificateBanner() }
                }

                // ── COURSE CONTENT ────────────────────────────
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
                            chapter    = chapter,
                            accent     = accent,
                            expanded   = expandedChapter == chapter.id,
                            onToggle   = { expandedChapter = if (expandedChapter == chapter.id) null else chapter.id },
                            onLessonTap = { lesson ->
                                nav.navigate(Screen.LessonViewer.createRoute(courseId, lesson.id))
                            }
                        )
                    }
                }

                // ── ABOUT INSTRUCTOR ──────────────────────────
                if (course.instructor != null) {
                    item { InstructorSection(course = course, accent = accent) }
                }

                // ── REVIEWS ───────────────────────────────────
                if (!course.reviews.isNullOrEmpty()) {
                    item {
                        ReviewsSection(
                            reviews            = course.reviews!!,
                            rating             = course.rating.toFloatOrNull() ?: 0f,
                            reviewCount        = course.review_count,
                            ratingDistribution = course.ratingDistribution
                        )
                    }
                }
            }

            // ── BOTTOM CTA ────────────────────────────────────
            Box(
                modifier        = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BottomCta(
                    course        = course,
                    accent        = accent,
                    isEnrolled    = isEnrolled,
                    isEnrolling   = state.isEnrolling,
                    onEnroll      = { viewModel.enroll(courseId) },
                    onContinue    = {
                        val firstUnfinished = chapters
                            .flatMap { it.lessons ?: emptyList() }
                            .firstOrNull { it.is_completed != true && !it.is_locked }
                        firstUnfinished?.let { nav.navigate(Screen.NotesReader.createRoute(it.id)) }
                    }
                )
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
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(accent.copy(red = accent.red * 0.5f, green = accent.green * 0.5f, blue = accent.blue * 0.5f), accent),
                Offset(0f, 0f), Offset(400f, 300f)
            ))
            .statusBarsPadding()
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.06f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 80.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.7f))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Back + share row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable(onClick = onBack), Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)), Alignment.Center) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(course.subject, accent, Color.White)
                if (course.isPaid) Badge("PRO", BpscColors.CoinGold, Color(0xFFFFF8E1))
                else Badge("FREE", BpscColors.Success, Color(0xFFE8FDF4))
            }

            // Title + instructor
            Text(course.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            course.instructor?.let {
                Text("By $it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
            }

            // Rating + students row
            val ratingFloat = course.rating.toFloatOrNull() ?: 0f
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < ratingFloat.toInt()) Color(0xFFFFD700) else Color.White.copy(0.3f), modifier = Modifier.size(13.dp)) }
                    Text("$ratingFloat", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("(${course.review_count})", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
                Text("·", color = Color.White.copy(0.5f))
                Text("${(course.enrollmentCount / 1000f).toInt()}k students", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
            }

            // Quick info pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(Icons.Rounded.PlayLesson, "$totalLessons lessons")
                InfoPill(Icons.Rounded.Schedule, "${course.totalHours}h total")
                InfoPill(Icons.Rounded.Language, course.language)
            }

            // Progress (if enrolled)
            if (isEnrolled && totalLessons > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Your Progress", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        Text("$completedLessons/$totalLessons · ${(animProg * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
// CTA CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun CtaCard(
    course: CourseDto, state: CourseDetailUiState,
    isEnrolled: Boolean, courseId: String,
    viewModel: CourseDetailViewModel
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Column(Modifier.weight(1f)) {
                if (course.isPaid) {
                    Text("₹${course.price}", style = MaterialTheme.typography.headlineMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    if (course.originalPrice > course.price)
                        Text("₹${course.originalPrice}", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextHint)
                } else {
                    Text("FREE", style = MaterialTheme.typography.headlineMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    if (isEnrolled) "You're enrolled ✓" else if (course.isPaid) "One-time purchase" else "Enroll for free",
                    style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary
                )
            }
            Button(
                onClick  = { if (!isEnrolled) viewModel.enroll(courseId) },
                enabled  = !state.isEnrolling,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = if (isEnrolled) BpscColors.Success else BpscColors.Primary),
                modifier = Modifier.height(48.dp)
            ) {
                if (state.isEnrolling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (isEnrolled) "Continue" else "Enroll Now", fontWeight = FontWeight.Bold)
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
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text("🏆", fontSize = 32.sp)
                Column {
                    Text("Certificate of Completion", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Earn a verified certificate after completing all lessons", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
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
    val lessons      = chapter.lessons ?: emptyList()
    val doneLessons  = lessons.count { it.is_completed == true }
    val totalLessons = lessons.size

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape)
                        .background(if (doneLessons == totalLessons && totalLessons > 0) BpscColors.Success.copy(0.1f) else accent.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (doneLessons == totalLessons && totalLessons > 0) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle,
                        null,
                        tint     = if (doneLessons == totalLessons && totalLessons > 0) BpscColors.Success else accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$doneLessons/$totalLessons completed", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
            }

            // Lessons
            if (expanded) {
                HorizontalDivider(color = BpscColors.Divider)
                lessons.forEachIndexed { index, lesson ->
                    LessonRow(lesson = lesson, accent = accent, onTap = { if (!lesson.is_locked) onLessonTap(lesson) })
                    if (index < lessons.size - 1) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, accent: Color, onTap: () -> Unit) {
    val typeEmoji = when (lesson.type) { "quiz" -> "❓"; "notes" -> "📄"; "live" -> "🔴"; else -> "🎬" }
    Row(
        modifier              = Modifier.fillMaxWidth()
            .alpha(if (lesson.is_locked) 0.5f else 1f)
            .clickable(enabled = !lesson.is_locked, onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier         = Modifier.size(30.dp).clip(CircleShape).background(
                when { lesson.is_completed == true -> BpscColors.Success.copy(0.15f); lesson.is_locked -> BpscColors.Surface; else -> accent.copy(0.1f) }
            ),
            contentAlignment = Alignment.Center
        ) {
            when {
                lesson.is_completed == true -> Icon(Icons.Rounded.Check, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                lesson.is_locked            -> Icon(Icons.Rounded.Lock, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                else                       -> Text(typeEmoji, fontSize = 12.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = if (lesson.is_locked) BpscColors.TextHint else BpscColors.TextPrimary,
                    fontWeight = if (lesson.is_completed == true) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (lesson.is_free_preview) Text("Free", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 5.dp, vertical = 2.dp))
            }
            if (lesson.duration_mins > 0)
                Text("${lesson.duration_mins}min", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
        }
        if (lesson.is_completed == true) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
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
                // Avatar + name
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val initials = course.instructor
                        ?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "?"
                    Box(Modifier.size(56.dp).clip(CircleShape).background(accent.copy(0.15f)).border(2.dp, accent, CircleShape), Alignment.Center) {
                        Text(initials, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(course.instructor ?: "", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                        Text("BPSC Subject Expert", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                }
                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!course.instructorStudents.isNullOrEmpty())
                        InstructorStat(Icons.Rounded.People, "${course.instructorStudents} students")
                    InstructorStat(Icons.Rounded.PlayLesson, "${course.instructorCourses} courses")
                    InstructorStat(Icons.Rounded.Star, "${course.rating} rating")
                }
                // Bio
                if (!course.instructorBio.isNullOrBlank()) {
                    Text(course.instructorBio, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 22.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STUDENT REVIEWS
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReviewsSection(
    reviews: List<CourseReview>,
    rating: Float,
    reviewCount: Int,
    ratingDistribution: RatingDistribution?
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("⭐ Student Reviews", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text("$reviewCount reviews", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }

        // Rating summary card
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(2.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
                // Big rating number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$rating", style = MaterialTheme.typography.displaySmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Row { repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < rating.toInt()) BpscColors.CoinGold else BpscColors.Divider, modifier = Modifier.size(16.dp)) } }
                    Text("Course Rating", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                }
                // Bar chart
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5, 4, 3, 2, 1).forEach { star ->
                        val pct = ratingDistribution?.pct(star) ?: when (star) { 5 -> 0.78f; 4 -> 0.14f; 3 -> 0.05f; 2 -> 0.02f; else -> 0.01f }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$star", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, modifier = Modifier.width(10.dp))
                            Icon(Icons.Rounded.Star, null, tint = BpscColors.CoinGold, modifier = Modifier.size(10.dp))
                            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)) {
                                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(BpscColors.CoinGold, RoundedCornerShape(3.dp)))
                            }
                            Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp, modifier = Modifier.width(26.dp))
                        }
                    }
                }
            }
        }

        // Individual review cards
        reviews.forEach { review -> ReviewCard(review = review) }
    }
}

@Composable
private fun ReviewCard(review: CourseReview) {
    val avatarColors = listOf(Color(0xFF9B59B6), Color(0xFF1565C0), Color(0xFF2ECC71), Color(0xFFE67E22), Color(0xFFE74C3C))
    val color        = avatarColors[(review.reviewerName.hashCode() and 0x7FFFFFFF) % avatarColors.size]
    val initials     = review.reviewerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(Color.White), CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(color), Alignment.Center) {
                        Text(initials, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment =  Alignment.CenterVertically) {
                            Text(review.reviewerName, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            if (review.isVerified) Row(
                                Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 5.dp, vertical = 2.dp),
                                Arrangement.spacedBy(2.dp), Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Verified, null, tint = BpscColors.Primary, modifier = Modifier.size(10.dp))
                                Text("Verified", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!review.createdAt.isNullOrEmpty()) {
                            Text(formatDate(review.createdAt), style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
                Row { repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < review.rating.toInt()) BpscColors.CoinGold else BpscColors.Divider, modifier = Modifier.size(12.dp)) } }
            }
            if (!review.comment.isNullOrBlank())
                Text(review.comment, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 22.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BOTTOM CTA
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomCta(
    course: CourseDto, accent: Color, isEnrolled: Boolean,
    isEnrolling: Boolean, onEnroll: () -> Unit, onContinue: () -> Unit
) {
    Box(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (isEnrolled) {
            Button(
                onClick  = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Continue Learning", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = onEnroll,
                    enabled  = !isEnrolling,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    if (isEnrolling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (course.isPaid) "Enroll — ₹${course.price}" else "Enroll Free", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SMALL HELPERS
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
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
        }
    }
}

private fun subjectAccent(subject: String): Color = when (subject) {
    "Bihar GK"     -> Color(0xFF2ECC71)
    "Polity"       -> Color(0xFF9B59B6)
    "Economy"      -> Color(0xFFE67E22)
    "Geography"    -> Color(0xFF1ABC9C)
    "History"      -> Color(0xFFE74C3C)
    "Science & Tech" -> Color(0xFF2980B9)
    "All Subjects" -> Color(0xFF1565C0)
    else           -> Color(0xFF1565C0)
}

private fun formatDate(iso: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val d = sdf.parse(iso) ?: return iso
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(d)
} catch (e: Exception) { iso.take(10) }
