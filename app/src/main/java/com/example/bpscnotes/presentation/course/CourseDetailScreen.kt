package com.example.bpscnotes.presentation.course

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.CourseDto
import com.google.gson.annotations.SerializedName

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
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        state.error != null && state.course == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️", fontSize = 40.sp)
                Text(state.error!!, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                Button(onClick = { viewModel.load(courseId) }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Retry") }
            }
        }
        state.course != null -> {
            val course   = state.course!!
            val chapters = state.chapters

            LaunchedEffect(chapters) {
                if (expandedChapter == null && chapters.isNotEmpty()) {
                    expandedChapter = chapters.first().id
                }
            }
            val totalLessons    = chapters.sumOf { it.lessons?.size?:0 }
            val completedLessons = chapters.sumOf { ch -> ch.lessons?.count { it.isCompleted == true }?:0 }
            val progress = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
            val animProg by animateFloatAsState(progress, tween(1000), label = "prog")

            val subjectColors = mapOf(
                "Bihar GK" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
                "Polity"   to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
                "Economy"  to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
                "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
                "History"  to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8))
            )
            val (accent, _) = subjectColors[course.subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

            LazyColumn(
                modifier       = Modifier.fillMaxSize().background(BpscColors.Surface),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {

                // ── Hero header ───────────────────────────────
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF051D56), Color(0xFF0D47A1), Color(0xFF1976D2)), Offset(0f,0f), Offset(400f,300f)))
                            .statusBarsPadding()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            IconButton(
                                onClick  = { nav.popBackStack() },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.height(12.dp))

                            // Subject badge
                            Text(
                                course.subject,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = accent,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Spacer(Modifier.height(10.dp))

                                // Text(course.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                            Spacer(Modifier.height(6.dp))

                            course.instructor?.let {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Person, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            // Stats strip
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                CourseStatChip("📚", "${totalLessons}", "Lessons")
                                CourseStatChip("⏱️", "${course.totalHours}h", "Total")
                                CourseStatChip("⭐", "${course.rating}", "Rating")
                                CourseStatChip("👥", "${course.enrollmentCount}", "Enrolled")
                            }

                            // Progress bar (if enrolled)
                            if (course.enrollment?.status=="active" && totalLessons > 0) {
                                Spacer(Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("$completedLessons / $totalLessons lessons", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                                        Text("${(animProg * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.2f))) {
                                        Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(3.dp)))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Error banner (non-fatal) ──────────────────
                if (state.error != null) {
                    item {
                        Text(
                            state.error!!,
                            color    = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3F3)).padding(12.dp)
                        )
                    }
                }

                // ── CTA card ──────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                if (course.isPaid) {
                                    Text("₹${course.price}", style = MaterialTheme.typography.headlineMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                                    if (course.originalPrice > course.price) {
                                        Text("₹${course.originalPrice}", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextHint)
                                    }
                                } else {
                                    Text("FREE", style = MaterialTheme.typography.headlineMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(
                                    if (course.enrollment?.status=="active") "You're enrolled ✓" else if (course.isPaid) "One-time purchase" else "Enroll for free",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BpscColors.TextSecondary
                                )
                            }
                            Button(
                                onClick  = {
                                    if (course.enrollment?.status!="active") viewModel.enroll(courseId)
                                },
                                enabled  = !state.isEnrolling,
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = if (course.enrollment?.status=="active") BpscColors.Success else BpscColors.Primary
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (state.isEnrolling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text(if (course.enrollment?.status=="active") "Continue" else "Enroll Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Chapters ───────────────────────────────────
                println(chapters+"hgjhj")
                if (chapters.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No curriculum available yet.", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        }
                    }
                } else {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Course Curriculum", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                            Text("${chapters.size} chapters", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                        }
                    }

                    items(chapters, key = { it.id }) { chapter ->
                        val isExpanded = expandedChapter == chapter.id
                        val doneLessons = chapter.lessons?.count { it.isCompleted == true }
                        val totalLessons = chapter.lessons?.size ?: 0

                        Text("$doneLessons/$totalLessons completed")
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column {
                                // Chapter header
                                Row(
                                    modifier              = Modifier.fillMaxWidth().clickable {
                                        expandedChapter = if (isExpanded) null else chapter.id
                                    }.padding(16.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${chapter.sortOrder}", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$doneLessons/${totalLessons} completed", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                                    }
                                    Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = BpscColors.TextHint)
                                }

                                // Lessons list
                                if (isExpanded) {
                                    HorizontalDivider(color = BpscColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                                    chapter.lessons?.forEach { lesson ->
                                        LessonRow(lesson = lesson)
                                    }
                                    Spacer(Modifier.height(4.dp))
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
private fun CourseStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

@Composable
private fun LessonRow(lesson: LessonDto) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(32.dp).clip(CircleShape)
                .background(if (lesson.isCompleted == true) BpscColors.Success.copy(0.1f) else BpscColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    lesson.isCompleted == true -> Icons.Rounded.CheckCircle
                    lesson.isLocked      -> Icons.Rounded.Lock
                    lesson.type == "video" -> Icons.Rounded.PlayCircle
                    else                 -> Icons.Rounded.Article
                },
                contentDescription = null,
                tint   = when {
                    lesson.isCompleted == true -> BpscColors.Success
                    lesson.isLocked    -> BpscColors.TextHint
                    else               -> BpscColors.Primary
                },
                modifier = Modifier.size(18.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                lesson.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = if (lesson.isLocked) BpscColors.TextHint else BpscColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lesson.type, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                if (lesson.durationMins > 0) {
                    Text("·", color = BpscColors.TextHint)
                    Text("${lesson.durationMins}min", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                if (lesson.isFreePreview) {
                    Text("·", color = BpscColors.TextHint)
                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


data class ChapterDto(
    val id: String,
    val title: String,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    val lessons: List<LessonDto>? = null
)

data class LessonDto(
    val id: String,
    val title: String,
    @SerializedName("duration_mins") val durationMins: Int = 0,
    val type: String = "video",
    @SerializedName("is_free_preview") val isFreePreview: Boolean = false,
    @SerializedName("is_locked") val isLocked: Boolean = true,
    @SerializedName("is_completed") val isCompleted: Boolean? = null,
    @SerializedName("sort_order") val sortOrder: Int = 0
)

data class CourseDetailResponse(
    @SerializedName("course")
    val course: CourseDto,

    @SerializedName("chapters")
    val chapters: List<ChapterDto>? = null
)



// ── ViewModel ─────────────────────────────────────────────────

data class CourseDetailUiState(
    val course: CourseDto? = null,
    val chapters: List<ChapterDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val enrollSuccess: Boolean = false,
    val isEnrolling: Boolean = false
)



