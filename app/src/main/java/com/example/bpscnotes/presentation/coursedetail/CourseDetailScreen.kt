package com.example.bpscnotes.presentation.coursedetail

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

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class Lesson(
    val id: String,
    val title: String,
    val duration: String,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = false,
    val isFreePreview: Boolean = false,
    val type: LessonType = LessonType.Video,
)

enum class LessonType(val emoji: String) {
    Video("🎬"), Quiz("❓"), Notes("📄"), Live("🔴")
}

data class Chapter(
    val title: String,
    val lessons: List<Lesson>,
)

data class CourseReview(
    val name: String,
    val initials: String,
    val color: Color,
    val rating: Float,
    val comment: String,
    val date: String,
    val isVerified: Boolean = true,
)

data class CourseDetail(
    val id: String,
    val title: String,
    val subject: String,
    val instructor: String,
    val instructorBio: String,
    val instructorStudents: String,
    val instructorCourses: Int,
    val description: String,
    val price: Int,
    val totalLessons: Int,
    val completedLessons: Int,
    val totalHours: Float,
    val rating: Float,
    val reviewCount: Int,
    val studentsEnrolled: Int,
    val isPaid: Boolean,
    val isEnrolled: Boolean,
    val hasCertificate: Boolean,
    val language: String,
    val lastUpdated: String,
    val chapters: List<Chapter>,
    val reviews: List<CourseReview>,
    val whatYouLearn: List<String>,
)

val mockCourseDetail = CourseDetail(
    id = "lc1",
    title = "BPSC 70th Complete Preparation Course",
    subject = "All Subjects",
    instructor = "BPSCNotes Team",
    instructorBio = "Expert team of IAS/PCS officers and subject specialists with 10+ years of teaching experience. Helped 5000+ students crack BPSC.",
    instructorStudents = "18K+",
    instructorCourses = 12,
    description = "The most comprehensive BPSC 70th preparation course. Covers all subjects with detailed video lectures, PDF notes, quizzes and full mock tests. Specially curated by IAS officers.",
    price = 1999,
    totalLessons = 180,
    completedLessons = 72,
    totalHours = 45f,
    rating = 4.9f,
    reviewCount = 2840,
    studentsEnrolled = 18500,
    isPaid = true,
    isEnrolled = true,
    hasCertificate = true,
    language = "Hindi + English",
    lastUpdated = "March 2026",
    whatYouLearn = listOf(
        "Complete Polity & Constitution coverage",
        "Modern & Ancient Indian History",
        "Indian & Bihar Geography",
        "Indian Economy & Budget 2026",
        "Bihar GK — 500+ facts",
        "Science & Technology updates",
        "Current Affairs Jan-Dec 2025",
        "Exam strategy & time management",
    ),
    chapters = listOf(
        Chapter("Chapter 1: Polity & Constitution", listOf(
            Lesson("l1",  "Introduction to Indian Constitution",     "12 min", isCompleted = true,  isFreePreview = true),
            Lesson("l2",  "Preamble — Deep Dive",                   "18 min", isCompleted = true),
            Lesson("l3",  "Fundamental Rights — Articles 12-35",    "32 min", isCompleted = true),
            Lesson("l4",  "DPSP & Fundamental Duties",              "24 min", isCompleted = true),
            Lesson("l5",  "Parliament — Structure & Functions",     "28 min", isCompleted = false),
            Lesson("l6",  "Polity Chapter Quiz",                    "15 min", isCompleted = false, type = LessonType.Quiz),
        )),
        Chapter("Chapter 2: Modern Indian History", listOf(
            Lesson("l7",  "British East India Company",             "20 min", isCompleted = false, isLocked = false),
            Lesson("l8",  "1857 Revolt — First War of Independence","25 min", isCompleted = false),
            Lesson("l9",  "Nationalist Movements",                  "30 min", isCompleted = false),
            Lesson("l10", "Gandhi Era & Civil Disobedience",        "35 min", isCompleted = false),
            Lesson("l11", "History Chapter Quiz",                   "10 min", isCompleted = false, type = LessonType.Quiz),
        )),
        Chapter("Chapter 3: Bihar GK", listOf(
            Lesson("l12", "Bihar Geography — Rivers & Mountains",   "22 min", isCompleted = false, isLocked = true),
            Lesson("l13", "Bihar History — Ancient Period",         "28 min", isCompleted = false, isLocked = true),
            Lesson("l14", "Bihar Economy & Development",            "20 min", isCompleted = false, isLocked = true),
            Lesson("l15", "Bihar Current Affairs 2025-26",          "18 min", isCompleted = false, isLocked = true),
        )),
        Chapter("Chapter 4: Indian Economy", listOf(
            Lesson("l16", "Indian Economy Basics",                  "20 min", isCompleted = false, isLocked = true),
            Lesson("l17", "RBI & Monetary Policy",                  "25 min", isCompleted = false, isLocked = true),
            Lesson("l18", "Budget 2026 Analysis",                   "30 min", isCompleted = false, isLocked = true),
        )),
    ),
    reviews = listOf(
        CourseReview("Priya Singh",  "PS", Color(0xFF9B59B6), 5f,   "Best course for BPSC prep! Cleared prelims in first attempt. Bihar GK section is outstanding.", "10 Mar 2026"),
        CourseReview("Amit Kumar",   "AK", Color(0xFF1565C0), 4.5f, "Very detailed and well structured. The notes quality is excellent. Highly recommended!", "08 Mar 2026"),
        CourseReview("Sneha Verma",  "SV", Color(0xFF2ECC71), 5f,   "Worth every rupee. The mock tests are exactly like the real exam pattern.", "05 Mar 2026"),
        CourseReview("Ravi Shankar", "RS", Color(0xFFE67E22), 4f,   "Good course overall. Could add more Economy content but Polity and History are excellent.", "01 Mar 2026"),
    ),
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun CourseDetailScreen(
    navController: NavHostController,
    courseId: String,
) {
    val course        = mockCourseDetail  // swap with ViewModel lookup by courseId
    val progress      = if (course.totalLessons > 0) course.completedLessons.toFloat() / course.totalLessons else 0f
    val animProgress  by animateFloatAsState(progress, tween(1000), label = "prog")
    val downloaded    = remember { mutableStateOf(false) }
    var expandedChapter by remember { mutableStateOf<String?>(course.chapters.first().title) }

    val subjectColors = mapOf(
        "All Subjects" to Pair(Color(0xFF1565C0), Color(0xFFE8F0FD)),
        "Polity"       to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "History"      to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Bihar GK"     to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
        "Economy"      to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
    )
    val (accent, bg) = subjectColors[course.subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Hero header ──────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(
                            listOf(accent.copy(red = accent.red * 0.5f, green = accent.green * 0.5f, blue = accent.blue * 0.5f), accent),
                            Offset(0f, 0f), Offset(400f, 300f)
                        ))
                        .statusBarsPadding()
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.06f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                        drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.7f))
                    }
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Back + actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable { }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (downloaded.value) BpscColors.Success else Color.White.copy(0.2f))
                                    .clickable { downloaded.value = !downloaded.value }, contentAlignment = Alignment.Center) {
                                    Icon(if (downloaded.value) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Subject + paid badge
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(course.subject, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 3.dp))
                            if (course.isPaid) Text("PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 3.dp))
                            else Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 8.dp, vertical = 3.dp))
                        }

                        // Title
                        Text(course.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                        Text("By ${course.instructor}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))

                        // Stats
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < course.rating.toInt()) Color(0xFFFFD700) else Color.White.copy(0.3f), modifier = Modifier.size(14.dp)) }
                                Text("${course.rating}", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("(${course.reviewCount})", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                            Text("·", color = Color.White.copy(0.5f))
                            Text("${(course.studentsEnrolled / 1000f).toInt()}k students", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }

                        // Quick info row
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CourseInfoPill(Icons.Rounded.PlayLesson, "${course.totalLessons} lessons")
                            CourseInfoPill(Icons.Rounded.Schedule, "${course.totalHours}h total")
                            CourseInfoPill(Icons.Rounded.Language, course.language)
                        }

                        // Progress (if enrolled)
                        if (course.isEnrolled) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Your Progress", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                    Text("${course.completedLessons}/${course.totalLessons} · ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                                    Box(modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(Color.White, RoundedCornerShape(3.dp)))
                                }
                            }
                        }
                    }
                }
            }

            // ── What you'll learn ────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("📚 What You'll Learn")
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            course.whatYouLearn.chunked(2).forEach { pair ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { item ->
                                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(16.dp).padding(top = 2.dp))
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

            // ── Certificate ───────────────────────────────────
            if (course.hasCertificate) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("🏆", fontSize = 32.sp)
                                Column {
                                    Text("Certificate of Completion", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text("Earn a verified certificate after completing all lessons", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Course content / chapters ─────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("📋 Course Content")
                    Text("${course.chapters.size} chapters · ${course.totalLessons} lessons", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
            }

            items(course.chapters) { chapter ->
                val isExpanded = expandedChapter == chapter.title
                val completedInChapter = chapter.lessons.count { it.isCompleted }

                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column {
                        // Chapter header
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandedChapter = if (isExpanded) null else chapter.title }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (completedInChapter == chapter.lessons.size) BpscColors.Success.copy(0.1f) else accent.copy(0.1f)), contentAlignment = Alignment.Center) {
                                Icon(if (completedInChapter == chapter.lessons.size) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle,
                                    null, tint = if (completedInChapter == chapter.lessons.size) BpscColors.Success else accent, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("$completedInChapter/${chapter.lessons.size} completed", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                            Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp))
                        }

                        // Lessons list
                        if (isExpanded) {
                            HorizontalDivider(color = BpscColors.Divider)
                            chapter.lessons.forEachIndexed { index, lesson ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .alpha(if (lesson.isLocked) 0.5f else 1f)
                                        .clickable(enabled = !lesson.isLocked) { navController.navigate(Screen.NotesReader.createRoute(lesson.id)) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Lesson number / status
                                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(
                                        when { lesson.isCompleted -> BpscColors.Success.copy(0.15f); lesson.isLocked -> BpscColors.Surface; else -> accent.copy(0.1f) }
                                    ), contentAlignment = Alignment.Center) {
                                        when {
                                            lesson.isCompleted -> Icon(Icons.Rounded.Check, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                                            lesson.isLocked   -> Icon(Icons.Rounded.Lock, null, tint = BpscColors.TextHint, modifier = Modifier.size(12.dp))
                                            else              -> Text(lesson.type.emoji, fontSize = 12.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = if (lesson.isLocked) BpscColors.TextHint else BpscColors.TextPrimary,
                                                fontWeight = if (lesson.isCompleted) FontWeight.Normal else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                            if (lesson.isFreePreview) Text("Free", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF4)).padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                        Text(lesson.duration, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                                    }
                                    if (lesson.isCompleted) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
                                }
                                if (index < chapter.lessons.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = BpscColors.Divider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Instructor ───────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("👨‍🏫 About the Instructor")
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(accent.copy(0.15f)).border(2.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(course.instructor.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                        style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                                }
                                Column {
                                    Text(course.instructor, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                    Text("BPSC Subject Expert", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                InstructorStat(Icons.Rounded.People, "${course.instructorStudents} students")
                                InstructorStat(Icons.Rounded.PlayLesson, "${course.instructorCourses} courses")
                                InstructorStat(Icons.Rounded.Star, "${course.rating} rating")
                            }
                            Text(course.instructorBio, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 22.sp)
                        }
                    }
                }
            }

            // ── Reviews ───────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("⭐ Student Reviews")
                        Text("${course.reviewCount} reviews", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                    }
                    // Rating summary
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${course.rating}", style = MaterialTheme.typography.displaySmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                Row { repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < course.rating.toInt()) BpscColors.CoinGold else BpscColors.Divider, modifier = Modifier.size(16.dp)) } }
                                Text("Course Rating", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5 to 0.78f, 4 to 0.14f, 3 to 0.05f, 2 to 0.02f, 1 to 0.01f).forEach { (star, pct) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("$star", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, modifier = Modifier.width(10.dp))
                                        Icon(Icons.Rounded.Star, null, tint = BpscColors.CoinGold, modifier = Modifier.size(10.dp))
                                        Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)) {
                                            Box(modifier = Modifier.fillMaxWidth(pct).fillMaxHeight().background(BpscColors.CoinGold, RoundedCornerShape(3.dp)))
                                        }
                                        Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp, modifier = Modifier.width(26.dp))
                                    }
                                }
                            }
                        }
                    }
                    // Individual reviews
                    course.reviews.forEach { review ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(review.color), contentAlignment = Alignment.Center) {
                                            Text(review.initials, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(review.name, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                                if (review.isVerified) Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Icon(Icons.Rounded.Verified, null, tint = BpscColors.Primary, modifier = Modifier.size(10.dp))
                                                    Text("Verified", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(review.date, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                                        }
                                    }
                                    Row { repeat(5) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < review.rating.toInt()) BpscColors.CoinGold else BpscColors.Divider, modifier = Modifier.size(12.dp)) } }
                                }
                                Text(review.comment, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, lineHeight = 22.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Bottom CTA ───────────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (course.isEnrolled) {
                Button(
                    onClick  = { navController.navigate(Screen.NotesReader.createRoute(course.chapters.first().lessons.first { !it.isCompleted }.id)) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue Learning", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, accent),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                    ) {
                        Text("Try Free", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick  = { navController.navigate(Screen.Subscription.route) },
                        modifier = Modifier.weight(2f).height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enroll — ₹${course.price}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun CourseInfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.2f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun InstructorStat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}