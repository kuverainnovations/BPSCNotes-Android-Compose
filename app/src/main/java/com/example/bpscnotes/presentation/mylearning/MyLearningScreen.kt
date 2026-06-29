package com.example.bpscnotes.presentation.mylearning

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlayLesson
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.presentation.payment.launchCashfree

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
enum class CourseStatus { InProgress, Completed, NotStarted, Wishlist }

enum class LibraryContentType(
    val label: String,
    val emoji: String,
    val color: Color,
    val bg: Color
) {
    PDF("PDF Notes", "📄", Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    PYQ("Prev. Papers", "📝", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    Book("Books", "📚", Color(0xFF1565C0), Color(0xFFE8F0FD)),
    Video("Video Notes", "🎬", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    Bookmarked("My Bookmarks", "🔖", Color(0xFFF39C12), Color(0xFFFFF8E1)),
}

data class StoreItem(
    val id: String, val title: String, val instructor: String, val subject: String,
    val price: Double, val originalPrice: Double, val totalLessons: Int, val totalHours: Float,
    val rating: Float, val reviewCount: Int, val studentsEnrolled: Int,
    val bpscRelevance: Int, val syllabusCoverage: Int, val isPaid: Boolean,
    val isFeatured: Boolean = false, val isLimitedOffer: Boolean = false,
    val offerEndsHours: Int = 0, val tags: List<String> = emptyList(),
    val trialLessonTitle: String = "", val description: String = "",
    val reviews: List<CourseReview> = emptyList(), val syllabus: List<String> = emptyList(),
    val maxCoinsRedeemable: Int? = null,
)

data class CourseReview(val name: String, val rating: Float, val comment: String, val date: String)

data class LearningCourse(
    val id: String, val title: String, val instructor: String, val subject: String,
    val totalLessons: Int, val completedLessons: Int, val totalMinutes: Int,
    val studiedMinutes: Int, val lastStudied: String, val status: CourseStatus,
    val isPaid: Boolean, val hasCertificate: Boolean = false,
    val certificateDate: String? = null, val rating: Float = 0f,
    val certificateUrl: String? = null,   // URL to view/download certificate
)

data class LibraryItem(
    val id: String, val title: String, val subject: String, val type: LibraryContentType,
    val author: String, val pages: Int, val fileSizeMb: Float, val downloads: Int,
    val rating: Float, val isPremium: Boolean, val isNew: Boolean = false,
    val isTrending: Boolean = false, val isPinned: Boolean = false,
    val isDownloaded: Boolean = false, val uploadedDate: String,
    val description: String, val tags: List<String> = emptyList(),
)


private fun CourseDto.toStoreItem(): StoreItem = StoreItem(
    id                = id,
    title             = title,
    instructor        = instructor ?: "BPSCNotes",
    subject           = subject,
    price             = price,
    originalPrice     = originalPrice,
    totalLessons      = totalLessons,
    totalHours        = totalHours.toFloatOrNull() ?: 0f,
    rating            = rating.toFloatOrNull() ?: 0f,
    reviewCount       = review_count,
    studentsEnrolled  = enrollmentCount,
    bpscRelevance     = bpsc_relevance,
    syllabusCoverage  = syllabus_coverage,
    isPaid            = isPaid,
    maxCoinsRedeemable = maxCoinsRedeemable,
    isFeatured        = is_featured,
    isLimitedOffer    = is_limited_offer,
    // API can send explicit null for list fields — orEmpty() guards every one
    tags              = exam_tags.orEmpty(),
    trialLessonTitle  = trial_lesson_title ?: "",
    description       = description ?: title,
    syllabus          = chapters.orEmpty().mapNotNull { it.title.takeIf { t -> t.isNotBlank() } }
        .ifEmpty { whatYouLearn.orEmpty() },
    reviews           = reviews.orEmpty().map { r ->
        CourseReview(name = r.reviewerName ?: "Student", rating = r.rating,
            comment = r.comment ?: "", date = formatCertDate(r.createdAt))
    }
)

private fun CourseDto.toLearningCourse(certUrls: Map<String, String> = emptyMap()): LearningCourse = LearningCourse(
    id = id,
    title = title,
    instructor = instructor ?: "BPSCNotes",
    subject = subject,
    totalLessons = totalLessons,
    completedLessons = enrollment?.completed_lessons?:0,
    totalMinutes = ((totalHours.toFloatOrNull() ?: (0f * 60))).toInt(),
    studiedMinutes = enrollment?.completed_lessons ?: (0 * 10), // approx or backend later
    lastStudied = enrollment?.lastStudiedAt ?: "",
    status =
        when {
            totalLessons <= 0 -> CourseStatus.NotStarted
            (enrollment?.completed_lessons ?: 0) >= totalLessons ->
                CourseStatus.Completed
            (enrollment?.completed_lessons ?: 0) > 0 ->
                CourseStatus.InProgress
            else ->
                CourseStatus.NotStarted
        },
    isPaid           = isPaid,
    hasCertificate   = hasCertificate,
    certificateDate  = enrollment?.completed_at,
    certificateUrl   = certUrls[id],   // populated from GET /users/certificates once generated
)

val storeSubjects = listOf(
    "All",
    "Polity",
    "History",
    "Geography",
    "Economy",
    "Bihar GK",
    "Science"
)
// ─────────────────────────────────────────────────────────────
// TIMESTAMP FORMATTER
// ─────────────────────────────────────────────────────────────
private fun formatStudiedDate(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        // Try ISO with fractional seconds + timezone offset: 2026-05-31T06:11:20.854001+00:00
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",   // microseconds + offset
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",       // millis + offset
            "yyyy-MM-dd'T'HH:mm:ssXXX",            // no fraction + offset
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",        // millis + Z
            "yyyy-MM-dd'T'HH:mm:ss'Z'",            // no fraction + Z
            "yyyy-MM-dd"
        )
        var parsed: java.util.Date? = null
        for (fmt in fmts) {
            try {
                parsed = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                    .apply { isLenient = true }.parse(raw)
                if (parsed != null) break
            } catch (_: Exception) {}
        }
        if (parsed == null) return raw

        val now   = java.util.Date()
        val diffMs = now.time - parsed.time
        val diffMins = diffMs / 60_000
        val diffHrs  = diffMs / 3_600_000
        val diffDays = diffMs / 86_400_000

        when {
            diffMins < 2   -> "Just now"
            diffMins < 60  -> "$diffMins min ago"
            diffHrs  < 24  -> "$diffHrs hr${if (diffHrs > 1) "s" else ""} ago"
            diffDays == 1L -> "Yesterday"
            diffDays < 7   -> "$diffDays days ago"
            diffDays < 30  -> "${diffDays / 7} week${if (diffDays / 7 > 1) "s" else ""} ago"
            else -> java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(parsed)
        }
    } catch (_: Exception) { raw }
}

private fun formatCertDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        var parsed: java.util.Date? = null
        for (fmt in fmts) {
            try {
                parsed = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                    .apply { isLenient = true }.parse(raw)
                if (parsed != null) break
            } catch (_: Exception) {}
        }
        if (parsed == null) return raw
        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(parsed)
    } catch (_: Exception) { raw }
}

/** Format a rupee amount: whole number when exact (₹10), two decimals when fractional (₹3.30) */
private fun fmtRs(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "₹${amount.toLong()}"
    else "₹${"%.2f".format(amount)}"

@Composable
fun MyLearningScreen(
    navController: NavHostController,
    adManager: com.example.bpscnotes.core.ads.AdManager? = null,
    startTab: Int = 1,   // default to My Courses tab — Marketplace accessible by switching
    viewModel: MyLearningViewModel = hiltViewModel(),
    fromScreen: String=""
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("my_learning") }

    var selectedTab by rememberSaveable { mutableIntStateOf(startTab) }

    // Switch to My Courses tab (index 1) when enrollment succeeds
    // and clear the trigger so it doesn't fire again on recomposition
    LaunchedEffect(state.justEnrolledId) {
        if (state.justEnrolledId != null) {
            selectedTab = 0          // 0 = My Courses, 1 = Marketplace
            viewModel.clearJustEnrolled()
        }
    }

    val userCoins = state.userCoins

    val storeItems = remember(state.storeCourses) {
        state.storeCourses.map { it.toStoreItem() }
    }

    val learningCourses = remember(state.enrolledCourses, state.certificateUrls) {
        state.enrolledCourses
            .filter { it.enrollment != null }
            .map { it.toLearningCourse(state.certificateUrls) }
    }

    // ✅ Loading — show skeleton instead of white screen
    // Use state.storeCourses directly (not storeItems which is a remember derivative)
    if (state.isLoading && state.storeCourses.isEmpty() && state.enrolledCourses.isEmpty()) {
        com.example.bpscnotes.core.ui.MyLearningSkeleton()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {


        // ── Header ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                        Offset(0f, 0f), Offset(500f, 500f)
                    )
                )
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    Color.White.copy(0.05f),
                    160.dp.toPx(),
                    Offset(size.width + 20.dp.toPx(), -50.dp.toPx())
                )
                drawCircle(
                    Color.White.copy(0.04f),
                    80.dp.toPx(),
                    Offset(-20.dp.toPx(), size.height * 0.7f)
                )
            }
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 46.dp, bottom = 30.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fromScreen=="nav-host") {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            str.navMyLearning,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            str.splashTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.6f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🪙", fontSize = 13.sp)
                        Text(
                            "$userCoins",
                            style = MaterialTheme.typography.labelLarge,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2 main tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.1f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("📚  My Courses", "🛍️  " + str.marketTitle).forEachIndexed { index, tab ->
                        val sel = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Color.White else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (sel) BpscColors.Primary else Color.White.copy(0.8f),
                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> MyCoursesTab(
                navController  = navController,
                courses        = learningCourses,
                savedCourses   = remember(state.savedCourses) { state.savedCourses.map { it.toLearningCourse() } },
                savedCourseIds = state.savedCourseIds,
                onToggleSave   = viewModel::toggleSave,
                adManager      = adManager,
                error          = state.error,
                onRetry        = viewModel::load
            )
            1 -> StoreTab(
                navController  = navController,
                userCoins      = userCoins,
                courses        = storeItems,
                savedCourseIds = state.savedCourseIds,
                viewModel      = viewModel,
                subjects       = state.subjects.ifEmpty { listOf("All") },
                adManager=adManager
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STORE TAB  (same as before — no change needed)
// ─────────────────────────────────────────────────────────────
@Composable
private fun StoreTab(
    navController:  NavHostController,
    userCoins:      Int,
    courses:        List<StoreItem>,
    savedCourseIds: Set<String>,
    viewModel:      MyLearningViewModel,
    subjects:       List<String> = listOf("All"),
    adManager:      com.example.bpscnotes.core.ads.AdManager? = null,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var selectedSubject by remember { mutableStateOf(str.filterAll) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<StoreItem?>(null) }
    // FIX 6: Use savedCourseIds from ViewModel (API-backed) instead of in-memory wishlist
    val focusManager = LocalFocusManager.current

    val filtered = courses.filter { course ->
        val matchesSub = selectedSubject == str.filterAll || course.subject == selectedSubject
        val matchesSearch = searchQuery.isEmpty() ||
                course.title.contains(searchQuery, ignoreCase = true) ||
                course.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesSub && matchesSearch
    }
    val featured = filtered.filter { it.isFeatured }
    val free = filtered.filter { !it.isPaid }
    val paid = filtered.filter { it.isPaid && !it.isFeatured }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surface)
                .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                null,
                tint = BpscColors.TextHint,
                modifier = Modifier.size(18.dp)
            )
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text(
                        str.materialsSearchHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextHint
                    )
                    inner()
                }
            )
            if (searchQuery.isNotEmpty()) Icon(
                Icons.Rounded.Close,
                null,
                tint = BpscColors.TextHint,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { searchQuery = "" })
        }

        // Subject filter — dynamic from API
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subjects) { sub ->
                val sel = selectedSubject == sub
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else Color.White)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else cs.outline,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedSubject = sub }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Empty state — no courses at all or no match for filter/search
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(BpscColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) { Text("🛍️", fontSize = 36.sp) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            str.courseNoCoursesYet,
                            style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            if (searchQuery.isNotEmpty() || selectedSubject != str.filterAll)
                                "Try a different subject or search term"
                            else
                                "Courses will appear here once added by admin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (featured.isNotEmpty()) {
                    item { StoreSectionHeader(str.jobsFeatured, "${featured.size} courses") }
                    items(featured) { course ->
                        StoreCourseCard(
                            course,
                            savedCourseIds.contains(course.id),
                            { viewModel.toggleSave(course.id)
                            }) { selectedCourse = course }; Spacer(Modifier.height(12.dp))
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (free.isNotEmpty()) {
                    item { StoreSectionHeader("🆓 " + str.coursesFree + " Courses", "${free.size} courses") }
                    items(free) { course ->
                        StoreCourseCard(
                            course,
                            savedCourseIds.contains(course.id),
                            { viewModel.toggleSave(course.id)
                            }) { selectedCourse = course }; Spacer(Modifier.height(12.dp))
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (paid.isNotEmpty()) {
                    item { StoreSectionHeader("🔒 " + str.premium + " Courses", "${paid.size} courses") }
                    items(paid) { course ->
                        StoreCourseCard(
                            course,
                            savedCourseIds.contains(course.id),
                            { viewModel.toggleSave(course.id)
                            }) { selectedCourse = course }; Spacer(Modifier.height(12.dp))
                    }
                }
                // ── Bottom banner ad ─────────────────────────────────
                item {
                    if (adManager != null) {
                        BannerAdView(adUnitId = adManager.getBannerAdUnitId())
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    selectedCourse?.let { course ->
        CourseDetailSheet(
            navController = navController,
            course       = course,
            userCoins    = userCoins,
            isWishlisted = savedCourseIds.contains(course.id),
            viewModel    = viewModel,
            onWishlist   = { viewModel.toggleSave(course.id) },
            onDismiss    = { selectedCourse = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// MY COURSES TAB  (Courses + Study Materials sub-tabs)
// ─────────────────────────────────────────────────────────────
/*@Composable
private fun MyCoursesTab(navController: NavHostController, courses: List<LearningCourse>) {
    val str = LocalStrings.current
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
    val cs = MaterialTheme.colorScheme
        // Sub-tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("📖 " + str.coursesTitle, "📂 " + str.materialsTitle).forEachIndexed { index, label ->
                val sel = subTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else cs.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { subTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        when (subTab) {
            0 -> EnrolledCoursesContent(navController = navController,courses=courses)
            1 -> StudyMaterialsContent()
        }
    }
}*/

@Composable
private fun MyCoursesTab(
    navController:  NavHostController,
    courses:        List<LearningCourse>,
    savedCourses:   List<LearningCourse> = emptyList(),
    savedCourseIds: Set<String>          = emptySet(),
    onToggleSave:   (String) -> Unit     = {},
    adManager:      com.example.bpscnotes.core.ads.AdManager? = null,
    error:          String?              = null,
    onRetry:        () -> Unit           = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        // Directly show enrolled courses
        // (StudyMaterialsContent kept for future Dashboard use)

        EnrolledCoursesContent(
            navController  = navController,
            courses        = courses,
            savedCourses   = savedCourses,
            savedCourseIds = savedCourseIds,
            onToggleSave   = onToggleSave,
            adManager      = adManager,
            error          = error,
            onRetry        = onRetry
        )
    }
}

// ─────────────────────────────────────────────────────────────
// SUB-TAB 1 — ENROLLED COURSES
// ─────────────────────────────────────────────────────────────
@Composable
private fun EnrolledCoursesContent(
    navController:  NavHostController,
    courses:        List<LearningCourse>,
    savedCourses:   List<LearningCourse> = emptyList(),
    savedCourseIds: Set<String>          = emptySet(),
    onToggleSave:   (String) -> Unit     = {},
    adManager:      com.example.bpscnotes.core.ads.AdManager? = null,
    error:          String?              = null,
    onRetry:        () -> Unit           = {},
) {
    val cs  = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf(str.filterAll, str.courseInProgress, str.coursesCompleted, "Saved")

    val filtered = courses.filter { course ->
        when (selectedFilter) {
            1    -> course.status == CourseStatus.InProgress
            2    -> course.status == CourseStatus.Completed
            3    -> savedCourseIds.contains(course.id)
            else -> true
        }
    }
    val inProgress   = courses.filter { it.status == CourseStatus.InProgress }
    val completed    = courses.filter { it.status == CourseStatus.Completed }
    // Only show cards for certificates that are actually ready to download —
    // a completed course without a generated certificate yet won't show a
    // dead-end card (avoids the "not yet available" toast entirely)
    val certificates = courses.filter { it.hasCertificate && !it.certificateUrl.isNullOrBlank() }
    val continueWith = inProgress.maxByOrNull { it.lastStudied }
    val displayList  = if (selectedFilter == 3) savedCourses else filtered

    val totalProgress = if (courses.sumOf { it.totalLessons } > 0)
        courses.sumOf { it.completedLessons }.toFloat() / courses.sumOf { it.totalLessons }
    else 0f
    val animProg by animateFloatAsState(totalProgress, tween(1200), label = "tp")

    if (courses.isEmpty()) {
        if (error != null) {
            AppErrorState(message = error, onRetry = onRetry)
            return
        }
        Box(
            Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) { Text("📚", fontSize = 36.sp) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str.courseNoCoursesYet, style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(str.courseExploreStore, style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        return
    }

    // One LazyColumn = everything scrolls together, max screen for the list
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 10.dp)) {

        // ── 1. Overall progress strip ──
        item {
            Box(Modifier.fillMaxWidth().background(cs.surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(str.courseOverallProgress, style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text("${(totalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(cs.background)) {
                        Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(BpscColors.Primary, Color(0xFF64B5F6))), RoundedCornerShape(4.dp)))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        LearningStatItem("📚", "${courses.size}", str.coursesEnrolled)
                        LearningStatItem("▶️", "${inProgress.size}", str.courseInProgress)
                        LearningStatItem("✅", "${completed.size}", str.coursesCompleted)
                        // TEMP #15: certificates hidden — LearningStatItem("🏆", "${certificates.size}", "Certs")
                    }
                }
            }
        }

        // ── 2. Filter chips ──
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filters) { index, filter ->
                    val sel = selectedFilter == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BpscColors.Primary else Color.White)
                            .border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(filter, style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else BpscColors.TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // ── 3. Continue Learning card (All tab, has in-progress) ──
        if (selectedFilter == 0 && continueWith != null) {
            item {
                Column(Modifier.padding(horizontal = 0.dp)) {
                    ContinueCard(
                        course     = continueWith,
                        onContinue = { navController.navigate(Screen.CourseDetail.createRoute(continueWith.id)) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // ── 4. My Certificates (All tab, has certs) — TEMP #15: hidden
        // if (selectedFilter == 0 && certificates.isNotEmpty()) { ... }

        // ── 5. Course list ──
        if (displayList.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 64.dp, start = 32.dp, end = 32.dp),
                    Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) { Text("📭", fontSize = 32.sp) }
                        Text(str.courseNoCoursesYet, style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Try a different subject filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        } else {
            items(displayList) { course ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    CourseProgressCard(
                        course           = course,
                        isWishlisted     = savedCourseIds.contains(course.id),
                        onToggleWishlist = { onToggleSave(course.id) },
                        onClick          = { navController.navigate(Screen.CourseDetail.createRoute(course.id)) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        // ── Bottom banner ad — always last item ─────────────────
        item {
            if (adManager != null) {
                BannerAdView(adUnitId = adManager.getBannerAdUnitId())
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}


// ─────────────────────────────────────────────────────────────
// LIBRARY DETAIL SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryDetailSheet(
    item: LibraryItem, isBookmarked: Boolean, isDownloaded: Boolean,
    onBookmark: () -> Unit, onDownload: () -> Unit, onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.type.color.copy(
                                    red = item.type.color.red * 0.6f,
                                    green = item.type.color.green * 0.6f,
                                    blue = item.type.color.blue * 0.6f
                                ), item.type.color
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.type.emoji, fontSize = 22.sp)
                        Text(
                            item.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.85f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        if (!item.isPremium) Text(
                            "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8FDF4))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        else Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    Text(
                        "By ${item.author} · ${item.uploadedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.75f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SheetStatWhite("📄", "${item.pages} pages"); SheetStatWhite(
                        "💾",
                        "${item.fileSizeMb} MB"
                    )
                        SheetStatWhite(
                            "⬇️",
                            "${(item.downloads / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${item.downloads}" }}"
                        ); SheetStatWhite("⭐", "${item.rating}")
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    //    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    lineHeight = 24.sp
                )
                if (item.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        item.tags.forEach { tag ->
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Primary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.PrimaryLight)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.background)
                        .border(1.dp, cs.outline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.type.emoji, fontSize = 44.sp)
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurfaceVariant
                        )
                        Text(
                            str.courseTapRead,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextHint
                        )
                    }
                }
                if (item.isPremium) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🔒", fontSize = 22.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                str.materialsPremiumContent,
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                str.materialUnlockPro,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBookmark,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isBookmarked) BpscColors.CoinGold else cs.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp)); Text(
                    if (isBookmarked) "Saved" else "Save",
                    style = MaterialTheme.typography.titleMedium
                )
                }
                Button(
                    onClick = { onDownload() },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDownloaded) BpscColors.Success else if (item.isPremium) BpscColors.CoinGold else BpscColors.Primary)
                ) {
                    Icon(
                        when {
                            isDownloaded -> Icons.Rounded.CheckCircle; item.isPremium -> Icons.Rounded.Lock; else -> Icons.Rounded.Download
                        }, null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp)); Text(
                    when {
                        isDownloaded -> str.materialsDownloadedDone; item.isPremium -> str.materialsUnlockPro; else -> str.materialsDownloadFree
                    }, style = MaterialTheme.typography.titleMedium
                )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// UPLOAD SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadNotesSheet(onDismiss: () -> Unit) {
    val str = LocalStrings.current
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selType by remember { mutableStateOf(LibraryContentType.PDF) }
    var description by remember { mutableStateOf("") }

    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                str.materialsUploadTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                str.mlShareNotes,
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant
            )
            HorizontalDivider(color = cs.outline)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(str.myLearningNotesTitle) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(str.materialsFilterSubject) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Text(
                str.mlContentType,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    LibraryContentType.values()
                        .filter { it != LibraryContentType.Bookmarked }) { type ->
                    val sel = selType == type
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) type.color else type.bg)
                            .clickable { selType = type }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(type.emoji, fontSize = 12.sp)
                        Text(
                            type.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else type.color,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(str.coursesRateSubtitle) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 4
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.background)
                    .border(1.5.dp, cs.outline, RoundedCornerShape(14.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AttachFile,
                        null,
                        tint = BpscColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        str.mlAttachFile,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank() && subject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(
                    Icons.Rounded.Upload,
                    null,
                    modifier = Modifier.size(18.dp)
                ); Spacer(Modifier.width(8.dp)); Text(
                str.materialsSubmitReview,
                style = MaterialTheme.typography.titleMedium
            )
            }
            Text(
                str.materialsReviewNote,
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STORE COURSE CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun StoreCourseCard(
    course: StoreItem,
    isWishlisted: Boolean,
    onWishlist: () -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val (accent, bg) = subjectColorMap()[course.subject] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )
    val discount =
        if (course.originalPrice > 0) ((1f - course.price.toFloat() / course.originalPrice) * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(subjectEmoji(course.subject), fontSize = 30.sp)
                    if (course.isLimitedOffer) Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE74C3C))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) { Text("🔥", fontSize = 8.sp) }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!course.isPaid) Text(
                            "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE8FDF4))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                        if (discount > 0) Text(
                            "$discount% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE74C3C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFFEE8E8))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                        course.tags.take(1).forEach { tag ->
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(bg)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Text(
                        course.instructor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { i ->
                            Icon(
                                Icons.Rounded.Star,
                                null,
                                tint = if (i < course.rating.toInt()) BpscColors.CoinGold else cs.outline,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            "${course.rating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "(${course.reviewCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextHint
                        )
                        Text("·", color = BpscColors.TextHint)
                        Text(
                            "${(course.studentsEnrolled / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${course.studentsEnrolled}" }} students",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                /*Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isWishlisted) Color(0xFFFFF8E1) else BpscColors.Surface)
                        .clickable(onClick = onWishlist), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isWishlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        tint = if (isWishlisted) BpscColors.CoinGold else BpscColors.TextHint,
                        modifier = Modifier.size(15.dp)
                    )
                }*/
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseInfoChip(Icons.Rounded.PlayLesson, "${course.totalLessons} lessons")
                    CourseInfoChip(Icons.Rounded.Schedule, "${course.totalHours}h")
                    CourseInfoChip(Icons.Rounded.BarChart, "${course.bpscRelevance}% BPSC")
                }
                if (course.isPaid) Column(horizontalAlignment = Alignment.End) {
                    if (course.originalPrice > course.price) {
                        Text(
                            fmtRs(course.originalPrice),
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextHint,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        fmtRs(course.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        "🪙 coins applicable",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold,
                        fontSize = 9.sp
                    )
                } else Text(
                    "FREE",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.Success,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (course.isLimitedOffer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE8E8))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔥", fontSize = 13.sp)
                    Text(
                        "Limited offer ends in ${course.offerEndsHours}h",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        str.courseGrabNow,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COURSE DETAIL SHEET + PAYMENT
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailSheet(
    navController: NavHostController,
    course: StoreItem,
    userCoins: Int,
    isWishlisted: Boolean,
    viewModel: MyLearningViewModel,
    onWishlist: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.uiState.collectAsState()
    var showEnrollSuccessDialog by remember { mutableStateOf(false) }
    var showBuyDialog           by remember { mutableStateOf(false) }
    var dialogCoins             by remember { mutableStateOf(0) }

    // Coin economy for slider
    val coinToInrRate  = viewModel.coinsConfig.economy.coinToInrRate
    val globalMaxCoins = viewModel.coinsConfig.economy.maxCoinsPerPurchase
    val maxCoinsPer    = globalMaxCoins
    val price         = course.price
    val priceInCoins  = if (coinToInrRate > 0) kotlin.math.ceil(price / coinToInrRate).toInt() else 0
    val maxApplicable = minOf(maxCoinsPer, userCoins, priceInCoins).coerceAtLeast(0)
    val coinDiscount: Double = minOf(price, dialogCoins * coinToInrRate)
    val amountDue:    Double = (price - coinDiscount).coerceAtLeast(0.0)

    // ── Coin purchase dialog ─────────────────────────────────────────────
    if (showBuyDialog) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false; dialogCoins = 0 },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Enroll in Course", fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge)
                    Text(course.title, style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant, maxLines = 2)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Course price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(fmtRs(price), style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                    }
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
                                Text("0 🪙", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
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
                    onClick  = { viewModel.enrollWithCoins(course.id, course.title, dialogCoins) },
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

    // Show success dialog when enrollment succeeds for this specific course
    LaunchedEffect(state.justEnrolledId) {
        if (state.justEnrolledId == course.id) {
            showBuyDialog = false; dialogCoins = 0
            showEnrollSuccessDialog = true
        }
    }

    // Paid course: backend returned Cashfree session → launch SDK directly
    LaunchedEffect(state.purchaseSessionId) {
        val sessionId = state.purchaseSessionId ?: return@LaunchedEffect
        val orderId   = state.purchaseProviderOrderId ?: return@LaunchedEffect
        if (state.purchaseCourseId != course.id) return@LaunchedEffect
        showBuyDialog = false; dialogCoins = 0
        viewModel.clearPurchaseRequired()
        launchCashfree(
            context     = context,
            sessionId   = sessionId,
            orderId     = orderId,
            environment = state.purchasePaymentEnvironment,
            onSuccess   = { cfPaymentId -> viewModel.confirmCoursePurchase(course.id, cfPaymentId) },
            onFailure   = { code, msg  -> viewModel.handleCoursePaymentFailure(code, msg) }
        )
    }

    if (showEnrollSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollSuccessDialog = false; onDismiss() },
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
                Text("You're now enrolled in ${course.title}. Start learning right away!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEnrollSuccessDialog = false
                        onDismiss()
                        // Already navigates to My Courses via justEnrolledId LaunchedEffect in parent
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) { Text("Go to My Courses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollSuccessDialog = false; onDismiss() }) {
                    Text("Continue Browsing", color = BpscColors.TextSecondary)
                }
            }
        )
    }

    val (accent, bg) = subjectColorMap()[course.subject] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(
                                    red = accent.red * 0.6f,
                                    green = accent.green * 0.6f,
                                    blue = accent.blue * 0.6f
                                ), accent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(subjectEmoji(course.subject), fontSize = 18.sp)
                        course.tags.take(2).forEach { tag ->
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        course.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    Text(
                        "By ${course.instructor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.8f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SheetStatWhite("⭐", "${course.rating} (${course.reviewCount})")
                        SheetStatWhite(
                            "👥",
                            "${(course.studentsEnrolled / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${course.studentsEnrolled}" }} enrolled"
                        )
                        SheetStatWhite("📊", "${course.bpscRelevance}% BPSC")
                    }
                }
            }
            val sheetScrollState = rememberScrollState()
            // The sheet's own drag-to-dismiss and this content's
            // verticalScroll both react to a small downward drag while
            // scrolled to the top - each nudges the content position,
            // which is the vertical "flicker" on light drags. Absorb that
            // boundary case here so only the sheet's drag/settle animates.
            val sheetContentNestedScroll = remember(sheetScrollState) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        if (sheetScrollState.value == 0 && available.y > 0f) available else Offset.Zero
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(sheetContentNestedScroll)
                    .verticalScroll(sheetScrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat("📚", "${course.totalLessons}", "Lessons"); DetailStat(
                    "⏱️",
                    "${course.totalHours}h",
                    "Duration"
                )
                    DetailStat("📊", "${course.syllabusCoverage}%", "Syllabus"); DetailStat(
                    "🎯",
                    "${course.bpscRelevance}%",
                    "BPSC Rel."
                )
                }
                Text(
                    str.courseAbout,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    course.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    lineHeight = 24.sp
                )
                if (course.trialLessonTitle.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BpscColors.PrimaryLight)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BpscColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                str.courseFreeTrial,
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                course.trialLessonTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = cs.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            str.courseWatch,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                // Reviews
                if (course.reviews.isNotEmpty()) {
                    Text(
                        str.courseStudentReviews,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    course.reviews.forEach { review ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.background),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        review.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = cs.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                Icons.Rounded.Star,
                                                null,
                                                tint = if (i < review.rating.toInt()) BpscColors.CoinGold else cs.outline,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    review.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant
                                )
                                Text(
                                    review.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.TextHint
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outline)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FIX: Coin slider removed — real-money Cashfree payment only
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        navController.navigate(Screen.CourseDetail.createRoute(course.id))
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PlayLesson, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Details & Curriculum", style = MaterialTheme.typography.titleSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onWishlist,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isWishlisted) BpscColors.CoinGold else cs.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isWishlisted) BpscColors.CoinGold else BpscColors.TextSecondary)
                    ) {
                        Icon(
                            if (isWishlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (course.isPaid) {
                                showBuyDialog = true
                            } else {
                                viewModel.enroll(course.id, course.title)
                                onDismiss()
                            }
                        },
                        enabled = !state.isEnrolling,
                        modifier = Modifier
                            .weight(3f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (!course.isPaid) BpscColors.Success
                                else BpscColors.CoinGold
                        )
                    ) {
                            if (state.isEnrolling) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    if (!course.isPaid) Icons.Rounded.PlayArrow else Icons.Rounded.ShoppingCart,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (!course.isPaid) str.courseEnrollFree
                                    else "Buy — ${fmtRs(course.price)}",
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
// CERTIFICATE CARD
// ─────────────────────────────────────────────────────────────
// NOTE: This card is only rendered for courses where
// course.certificateUrl is non-null (see `certificates` filter
// above) — i.e. the certificate PDF has already been generated by
// the backend. So `downloadCertificate()` can safely assume the
// URL exists; no "not yet available" fallback is needed here.
    @Composable
    private fun CertificateCard(course: LearningCourse) {
        val str = LocalStrings.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val (accent, _) = subjectColorMap()[course.subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

        // Capture str values before non-composable lambdas
        val certTitle    = str.courseCertTitle    // "Certificate of Completion" — existing key
        val downloadLabel = str.materialsDownload  // "Download" — existing key
        val shareLabel    = str.courseShareCertBtn // "Share 🎓" — existing key

        // Open certificate PDF in browser / PDF viewer
        fun downloadCertificate() {
            val url = course.certificateUrl ?: return
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        }

        // Share certificate link
        fun shareCertificate() {
            val url       = course.certificateUrl
            val shareText = if (!url.isNullOrBlank())
                "I completed ${course.title} on BPSCNotes! 🏆\n$url"
            else
                "I completed ${course.title} on BPSCNotes! 🏆"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Certificate"))
        }

        Card(
            modifier  = Modifier.width(220.dp),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(accent, accent.copy(0.75f)), Offset(0f, 0f), Offset(220f, 160f)))
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFFFD700)))
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("🏆", fontSize = 16.sp)
                        Text(certTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold)
                    }
                    Text(course.title,
                        style = MaterialTheme.typography.titleSmall, color = Color.White,
                        fontWeight = FontWeight.ExtraBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    if (!course.certificateDate.isNullOrBlank()) {
                        Text("${str.coursesCompleted} · ${formatCertDate(course.certificateDate)}",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                    }
                    // Download + Share row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.25f))
                                .clickable { downloadCertificate() }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(downloadLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.15f))
                                .clickable { shareCertificate() }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(shareLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
// CONTINUE CARD
// ─────────────────────────────────────────────────────────────
    @Composable
    private fun ContinueCard(course: LearningCourse, onContinue: () -> Unit) {
        val str = LocalStrings.current
        val progress =
            if (course.totalLessons > 0) course.completedLessons.toFloat() / course.totalLessons else 0f
        val animProg by animateFloatAsState(progress, tween(1000), label = "cp")
        val (accent, _) = subjectColorMap()[course.subject] ?: Pair(
            BpscColors.Primary,
            BpscColors.PrimaryLight
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            val cs = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(accent, accent.copy(alpha = 0.75f)),
                            Offset(0f, 0f),
                            Offset(400f, 150f)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = 240.dp, y = (-20).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.08f))
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayCircle,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        ); Text(
                        str.courseContinueLearning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.85f)
                    )
                    }
                    Text(
                        course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${course.completedLessons}/${course.totalLessons} lessons · ${formatStudiedDate(course.lastStudied)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.75f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animProg)
                                .fillMaxHeight()
                                .background(Color.White, RoundedCornerShape(3.dp))
                        )
                    }
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.surface)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        ); Spacer(Modifier.width(6.dp)); Text(
                        str.courseContinueLearning,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
// COURSE PROGRESS CARD
// ─────────────────────────────────────────────────────────────
    @Composable
    private fun CourseProgressCard(
        course: LearningCourse,
        isWishlisted: Boolean,
        onToggleWishlist: () -> Unit,
        onClick: () -> Unit
    ) {
        val cs = MaterialTheme.colorScheme
        val progress =
            if (course.totalLessons > 0) course.completedLessons.toFloat() / course.totalLessons else 0f
        val animProg by animateFloatAsState(progress, tween(1000), label = "pp")
        val (accent, bg) = subjectColorMap()[course.subject] ?: Pair(
            BpscColors.Primary,
            BpscColors.PrimaryLight
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(subjectEmoji(course.subject), fontSize = 24.sp)
                    if (course.isPaid) Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BpscColors.CoinGold)
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        course.instructor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${course.completedLessons}/${course.totalLessons} lessons",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(bg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animProg)
                                .fillMaxHeight()
                                .background(accent, RoundedCornerShape(3.dp))
                        )
                    }
                    if (course.hasCertificate && course.certificateDate != null) Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🏆", fontSize = 11.sp); Text(
                        "Certified · ${formatCertDate(course.certificateDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
    @Composable
    private fun StoreSectionHeader(title: String, subtitle: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cs = MaterialTheme.colorScheme
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }
    }


    @Composable
    private fun CourseInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val cs = MaterialTheme.colorScheme
            Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }



    @Composable
    private fun LibSmallStat(icon: String, value: String, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val cs = MaterialTheme.colorScheme
            Text(icon, fontSize = 11.sp)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                fontSize = 8.sp
            )
        }
    }

    @Composable
    private fun DetailStat(icon: String, value: String, label: String) {
        val cs = MaterialTheme.colorScheme
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cs.background)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                fontSize = 9.sp
            )
        }
    }

    @Composable
    private fun SheetStatWhite(icon: String, value: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val cs = MaterialTheme.colorScheme
            Text(icon, fontSize = 12.sp)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.85f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    @Composable
    private fun LearningStatItem(icon: String, value: String, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val cs = MaterialTheme.colorScheme
            Text(icon, fontSize = 13.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                fontSize = 9.sp
            )
        }
    }

    private fun subjectColorMap() = mapOf(
        "All" to Pair(Color(0xFF1565C0), Color(0xFFE8F0FD)),
        "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
        "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
        "Current Affairs" to Pair(Color(0xFF1565C0), Color(0xFFE8F0FD)),
    )

    private fun subjectEmoji(subject: String) = when (subject) {
        "All" -> "📚"; "Polity" -> "⚖️"; "History" -> "🏛️"; "Geography" -> "🗺️"
        "Economy" -> "💰"; "Bihar GK" -> "🏔️"; "Science" -> "🔬"; "Current Affairs" -> "📰"; else -> "📖"
    }