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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
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
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.bpscnotes.data.remote.api.CourseDto

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
    val price: Int, val originalPrice: Int, val totalLessons: Int, val totalHours: Float,
    val rating: Float, val reviewCount: Int, val studentsEnrolled: Int,
    val bpscRelevance: Int, val syllabusCoverage: Int, val isPaid: Boolean,
    val isFeatured: Boolean = false, val isLimitedOffer: Boolean = false,
    val offerEndsHours: Int = 0, val tags: List<String> = emptyList(),
    val trialLessonTitle: String = "", val description: String = "",
    val reviews: List<CourseReview> = emptyList(), val syllabus: List<String> = emptyList(),
)

data class CourseReview(val name: String, val rating: Float, val comment: String, val date: String)

data class LearningCourse(
    val id: String, val title: String, val instructor: String, val subject: String,
    val totalLessons: Int, val completedLessons: Int, val totalMinutes: Int,
    val studiedMinutes: Int, val lastStudied: String, val status: CourseStatus,
    val isPaid: Boolean, val hasCertificate: Boolean = false,
    val certificateDate: String? = null, val rating: Float = 0f,
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
    id = id,
    title = title,
    instructor = instructor ?: "BPSCNotes",
    subject = subject,
    price = price,
    originalPrice = originalPrice,
    totalLessons = totalLessons,
    totalHours = totalHours.toFloatOrNull() ?: 0f,
    rating = rating.toFloatOrNull() ?: 0f,
    reviewCount = review_count,
    studentsEnrolled = enrollmentCount,
    bpscRelevance = 0,
    syllabusCoverage = 0,
    isPaid = isPaid,
    isFeatured = is_featured,
    tags = exam_tags,
    trialLessonTitle = trial_lesson_title ?: "",
    description =  title?: ""
)

private fun CourseDto.toLearningCourse(): LearningCourse = LearningCourse(
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
    isPaid = isPaid
)

val storeSubjects = listOf(
    "All",
    "All Subjects",
    "Polity",
    "History",
    "Geography",
    "Economy",
    "Bihar GK",
    "Science"
)
@Composable
fun MyLearningScreen(
    navController: NavHostController,
    startTab: Int = 0,
    viewModel: MyLearningViewModel = hiltViewModel(),
    fromScreen: String=""
) {
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    var selectedTab by rememberSaveable { mutableIntStateOf(startTab) }

    // Switch to My Courses tab (index 1) when enrollment succeeds
    // and clear the trigger so it doesn't fire again on recomposition
    LaunchedEffect(state.justEnrolledId) {
        if (state.justEnrolledId != null) {
            selectedTab = 1          // 0 = Store, 1 = My Courses
            viewModel.clearJustEnrolled()
        }
    }

    val userCoins = state.userCoins

    val storeItems = remember(state.storeCourses) {
        state.storeCourses.map { it.toStoreItem() }
    }

    val learningCourses = remember(state.enrolledCourses) {
        state.enrolledCourses
            .filter { it.enrollment != null }
            .map { it.toLearningCourse() }
    }

    // ✅ Loading
    if (state.isLoading && storeItems.isEmpty()) {
        Box(Modifier
            .fillMaxSize()
            .background(BpscColors.Surface), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
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
            //.statusBarsPadding()
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
                    if (fromScreen=="nav-host") Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            str.navMyLearning,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Learn, grow and rank higher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.6f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Text(
                            "$userCoins coins",
                            style = MaterialTheme.typography.bodyMedium,
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
                    listOf("🛍️  Store", "📚  My Courses").forEachIndexed { index, tab ->
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
            0 -> StoreTab(
                navController  = navController,
                userCoins      = userCoins,
                courses        = storeItems,
                savedCourseIds = state.savedCourseIds,
                viewModel      = viewModel
            )
            1 -> MyCoursesTab(
                navController  = navController,
                courses        = learningCourses,
                savedCourses   = remember(state.savedCourses) { state.savedCourses.map { it.toLearningCourse() } },
                savedCourseIds = state.savedCourseIds,
                onToggleSave   = viewModel::toggleSave
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
    viewModel:      MyLearningViewModel
) {
    var selectedSubject by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<StoreItem?>(null) }
    // FIX 6: Use savedCourseIds from ViewModel (API-backed) instead of in-memory wishlist
    val focusManager = LocalFocusManager.current

    val filtered = courses.filter { course ->
        val matchesSub = selectedSubject == "All" || course.subject == selectedSubject
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
                .background(Color.White)
                .border(1.dp, BpscColors.Divider, RoundedCornerShape(14.dp))
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BpscColors.TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text(
                        "Search courses...",
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

        // Subject filter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(storeSubjects) { sub ->
                val sel = selectedSubject == sub
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else Color.White)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else BpscColors.Divider,
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

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (featured.isNotEmpty()) {
                item { StoreSectionHeader("⭐ Featured", "${featured.size} courses") }
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
                item { StoreSectionHeader("🆓 Free Courses", "${free.size} courses") }
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
                item { StoreSectionHeader("🔒 Premium Courses", "${paid.size} courses") }
                items(paid) { course ->
                    StoreCourseCard(
                        course,
                        savedCourseIds.contains(course.id),
                        { viewModel.toggleSave(course.id)
                        }) { selectedCourse = course }; Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    selectedCourse?.let { course ->
        CourseDetailSheet(
            course       = course,
            userCoins    = userCoins,
            isWishlisted = savedCourseIds.contains(course.id),
            onWishlist   = { viewModel.toggleSave(course.id) },
            // FIX 2: actually call enroll API for free courses
            onEnroll     = { courseId ->
                viewModel.enroll(courseId)
                selectedCourse = null
            },
            onDismiss    = { selectedCourse = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// MY COURSES TAB  (Courses + Study Materials sub-tabs)
// ─────────────────────────────────────────────────────────────
/*@Composable
private fun MyCoursesTab(navController: NavHostController, courses: List<LearningCourse>) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Sub-tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("📖 My Courses", "📂 Study Materials").forEachIndexed { index, label ->
                val sel = subTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else BpscColors.Divider,
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
    onToggleSave:   (String) -> Unit     = {}
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
            onToggleSave   = onToggleSave
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
    onToggleSave:   (String) -> Unit     = {}
) {
    val str = LocalStrings.current
    var selectedFilter by remember { mutableIntStateOf(0) }
    val wishlist = remember { mutableStateListOf<String>() }
    val filters = listOf("All", "In Progress", str.coursesCompleted, "Saved")

    val filtered = courses.filter { course ->
        when (selectedFilter) {
            1 -> course.status == CourseStatus.InProgress
            2 -> course.status == CourseStatus.Completed
            3 -> savedCourseIds.contains(course.id)
            else -> true
        }
    }

    val inProgress = courses.filter { it.status == CourseStatus.InProgress }
    val completed = courses.filter { it.status == CourseStatus.Completed }
    val certificates = courses.filter { it.hasCertificate }
    val totalProgress = if (courses.isNotEmpty())
        courses.sumOf { it.completedLessons }
            .toFloat() / courses.sumOf { it.totalLessons }
    else 0f
    val animProg by animateFloatAsState(totalProgress, tween(1200), label = "tp")

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Overall Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(totalProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BpscColors.Surface)
                    // .statusBarsPadding()

                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProg)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        BpscColors.Primary,
                                        Color(0xFF64B5F6)
                                    )
                                ), RoundedCornerShape(4.dp)
                            )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LearningStatItem("📚", "${courses.size}", str.coursesEnrolled)
                    LearningStatItem("▶️", "${inProgress.size}", "In Progress")
                    LearningStatItem("✅", "${completed.size}", str.coursesCompleted)
                    LearningStatItem("🏆", "${certificates.size}", "Certs")
                }
            }
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filters) { index, filter ->
                val sel = selectedFilter == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else Color.White)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else BpscColors.Divider,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        filter,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        val continueWith =
            inProgress.maxByOrNull { it.lastStudied }
        if (selectedFilter == 0 && continueWith != null) {
            ContinueCard(
                course = continueWith,
                onContinue = { navController.navigate(Screen.CourseDetail.createRoute(continueWith.id)) })
            Spacer(Modifier.height(8.dp))
        }
        // When "Saved" filter is active, show savedCourses (not enrolled) too
        val displayList = if (selectedFilter == 3) savedCourses else filtered

        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📚", fontSize = 48.sp)
                    Text(
                        "No courses here yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Explore Store tab to enroll",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Certificates section
                if (selectedFilter == 0 && certificates.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StoreSectionHeader("🏆 My Certificates", "${certificates.size} earned")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(certificates) { course -> CertificateCard(course = course) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }
                items(displayList) { course ->
                    CourseProgressCard(
                        course = course,
                        isWishlisted = wishlist.contains(course.id),
                        onToggleWishlist = {
                            if (wishlist.contains(course.id)) wishlist.remove(
                                course.id
                            ) else wishlist.add(course.id)
                        },
                        onClick = { navController.navigate(Screen.CourseDetail.createRoute(course.id)) }
                    )
                }
            }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
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
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BpscColors.TextSecondary,
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
                        .background(BpscColors.Surface)
                        .border(1.dp, BpscColors.Divider, RoundedCornerShape(16.dp)),
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
                            color = BpscColors.TextSecondary
                        )
                        Text(
                            "Tap Read to open full document",
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
                                "Premium Content",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlock with BPSCNotes Pro",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.TextSecondary
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = BpscColors.Divider)
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
                        if (isBookmarked) BpscColors.CoinGold else BpscColors.Divider
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
                        isDownloaded -> "Downloaded ✓"; item.isPremium -> "Unlock with Pro"; else -> "Download Free"
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
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selType by remember { mutableStateOf(LibraryContentType.PDF) }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
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
                "Upload Your Notes",
                style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Share notes with 10,000+ BPSC aspirants",
                style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextSecondary
            )
            HorizontalDivider(color = BpscColors.Divider)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notes Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Text(
                "Content Type",
                style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary,
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
                label = { Text("Brief Description") },
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
                    .background(BpscColors.Surface)
                    .border(1.5.dp, BpscColors.Divider, RoundedCornerShape(14.dp))
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
                        "Tap to attach file (PDF / DOC)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary
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
                "Submit for Review",
                style = MaterialTheme.typography.titleMedium
            )
            }
            Text(
                "📋 All uploads are reviewed before publishing",
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Text(
                        course.instructor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { i ->
                            Icon(
                                Icons.Rounded.Star,
                                null,
                                tint = if (i < course.rating.toInt()) BpscColors.CoinGold else BpscColors.Divider,
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
                            color = BpscColors.TextSecondary
                        )
                    }
                }
                Box(
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
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CourseInfoChip(Icons.Rounded.PlayLesson, "${course.totalLessons} lessons")
                CourseInfoChip(Icons.Rounded.Schedule, "${course.totalHours}h")
                CourseInfoChip(Icons.Rounded.BarChart, "${course.bpscRelevance}% BPSC")
                Spacer(Modifier.weight(1f))
                if (course.isPaid) Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "₹${course.originalPrice}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextHint,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text(
                            "₹${course.price}",
                            style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        "🪙 coins applicable",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.CoinGold,
                        fontSize = 9.sp
                    )
                } else Text(
                    "FREE",
                    style = MaterialTheme.typography.titleLarge,
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
                        "Grab now →",
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
    course: StoreItem,
    userCoins: Int,
    isWishlisted: Boolean,
    onWishlist: () -> Unit,
    onEnroll: (courseId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPayment by remember { mutableStateOf(false) }
    var coinsToUse by remember { mutableIntStateOf(0) }
    var couponCode by remember { mutableStateOf("") }
    var couponApplied by remember { mutableStateOf(false) }
    var couponDiscount by remember { mutableIntStateOf(0) }
    var expandSyllabus by remember { mutableStateOf(false) }

    val (accent, bg) = subjectColorMap()[course.subject] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )
    val maxCoins = minOf(userCoins, (course.price * 0.5).toInt())
    val coinDiscount = (coinsToUse * 0.10).toInt()
    val finalPrice = maxOf(0, course.price - coinDiscount - couponDiscount)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()) {
            if (!showPayment) {
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
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
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
                        "About this Course",
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        course.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary,
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
                                    "Free Trial Lesson",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    course.trialLessonTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BpscColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "Watch →",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.Primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    // Syllabus
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Course Syllabus",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (expandSyllabus) "Show less ↑" else "Show all ↓",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { expandSyllabus = !expandSyllabus })
                        }
                        (if (expandSyllabus) course.syllabus else course.syllabus.take(3)).forEachIndexed { i, item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(BpscColors.PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.Primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BpscColors.TextPrimary
                                )
                            }
                        }
                    }
                    // Reviews
                    if (course.reviews.isNotEmpty()) {
                        Text(
                            "Student Reviews",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        course.reviews.forEach { review ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BpscColors.Surface),
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
                                            color = BpscColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row {
                                            repeat(5) { i ->
                                                Icon(
                                                    Icons.Rounded.Star,
                                                    null,
                                                    tint = if (i < review.rating.toInt()) BpscColors.CoinGold else BpscColors.Divider,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        review.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BpscColors.TextSecondary
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
                HorizontalDivider(color = BpscColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onWishlist,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isWishlisted) BpscColors.CoinGold else BpscColors.Divider
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
                        onClick = { if (course.isPaid) showPayment = true else onEnroll(course.id) },
                        modifier = Modifier
                            .weight(3f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (!course.isPaid) BpscColors.Success else BpscColors.Primary)
                    ) {
                        Icon(
                            if (!course.isPaid) Icons.Rounded.PlayArrow else Icons.Rounded.ShoppingCart,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (!course.isPaid) "Enroll Free" else "Buy Now — ₹${course.price}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                // Payment screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0A2472),
                                    Color(0xFF1565C0)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { showPayment = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                "Complete Purchase",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            course.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Price summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BpscColors.Surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Price Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            PriceRow("Course Price", "₹${course.price}", false)
                            if (coinDiscount > 0) PriceRow(
                                "Coins Discount ($coinsToUse 🪙)",
                                "- ₹$coinDiscount",
                                true
                            )
                            if (couponApplied && couponDiscount > 0) PriceRow(
                                "Coupon ($couponCode)",
                                "- ₹$couponDiscount",
                                true
                            )
                            HorizontalDivider(color = BpscColors.Divider)
                            PriceRow("Total Payable", "₹$finalPrice", false, isTotal = true)
                        }
                    }
                    // Coins slider
                    if (userCoins > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Use Coins for Discount",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = BpscColors.TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ); Text(
                                        "1 coin = ₹0.10 · Max 50% via coins",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BpscColors.TextSecondary
                                    )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "🪙 $coinsToUse",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = BpscColors.CoinGold,
                                            fontWeight = FontWeight.ExtraBold
                                        ); Text(
                                        "= ₹$coinDiscount off",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BpscColors.Success,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    }
                                }
                                Slider(
                                    value = coinsToUse.toFloat(),
                                    onValueChange = { coinsToUse = it.toInt() },
                                    valueRange = 0f..maxCoins.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = BpscColors.CoinGold,
                                        activeTrackColor = BpscColors.CoinGold
                                    )
                                )
                                Text(
                                    "You have $userCoins coins available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BpscColors.TextSecondary
                                )
                            }
                        }
                    }
                    // Coupon
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Coupon Code",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = couponCode,
                                    onValueChange = {
                                        couponCode = it.uppercase(); couponApplied =
                                        false; couponDiscount = 0
                                    },
                                    placeholder = { Text("Enter coupon code") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    enabled = !couponApplied
                                )
                                Button(
                                    onClick = {
                                        when (couponCode) {
                                            "BPSC50" -> {
                                                couponApplied = true; couponDiscount =
                                                    (course.price * 0.05).toInt()
                                            }

                                            "SAVE100" -> {
                                                couponApplied = true; couponDiscount = 100
                                            }

                                            "FIRST" -> {
                                                couponApplied = true; couponDiscount = 50
                                            }

                                            else -> {
                                                couponApplied = false; couponDiscount = 0
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = couponCode.isNotBlank() && !couponApplied,
                                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                                ) { Text(if (couponApplied) "✓" else "Apply") }
                            }
                            if (couponApplied && couponDiscount > 0) Text(
                                "✅ Coupon applied! Saved ₹$couponDiscount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.Success,
                                fontWeight = FontWeight.SemiBold
                            )
                            else if (couponCode.isNotEmpty() && !couponApplied) Text(
                                "❌ Invalid code. Try: BPSC50, SAVE100, FIRST",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE74C3C)
                            )
                            Text(
                                "💡 Try: BPSC50 · SAVE100 · FIRST",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint
                            )
                        }
                    }
                    // UPI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Pay via UPI",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    "GPay" to "G",
                                    "PhonePe" to "P",
                                    "Paytm" to "₹",
                                    "BHIM" to "B"
                                ).forEach { (app, letter) ->
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BpscColors.Surface)
                                            .border(
                                                1.dp,
                                                BpscColors.Divider,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { }
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            letter,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = BpscColors.Primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            app,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BpscColors.TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                "Or enter UPI ID manually:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.TextSecondary
                            )
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                placeholder = { Text("yourname@upi") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                        }
                    }
                }
                HorizontalDivider(color = BpscColors.Divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (coinsToUse > 0) Text(
                        "🪙 Using $coinsToUse coins = ₹$coinDiscount discount applied",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.CoinGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Icon(
                            Icons.Rounded.CurrencyRupee,
                            null,
                            modifier = Modifier.size(18.dp)
                        ); Spacer(Modifier.width(8.dp))
                        Text(
                            "Pay ₹$finalPrice via UPI",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        "🔒 Secure payment · Instant access after payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextHint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CERTIFICATE CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun CertificateCard(course: LearningCourse) {
    val (accent, _) = subjectColorMap()[course.subject] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(accent, accent.copy(alpha = 0.7f)),
                        Offset(0f, 0f),
                        Offset(200f, 140f)
                    )
                )
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFFFFD700)))
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🏆", fontSize = 16.sp); Text(
                    "Certificate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.ExtraBold
                )
                }
                Text(
                    course.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Text(
                    "Completed · ${course.certificateDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.8f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.2f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    ); Spacer(Modifier.width(4.dp)); Text(
                    "Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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
                    "Continue where you left off",
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
                    "${course.completedLessons}/${course.totalLessons} lessons · ${course.lastStudied}",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    ); Spacer(Modifier.width(6.dp)); Text(
                    "Continue Learning",
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    course.instructor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${course.completedLessons}/${course.totalLessons} lessons",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextSecondary
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
                    "Certified · ${course.certificateDate}",
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
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )
    }
}


@Composable
private fun CourseInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextSecondary,
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
        Text(icon, fontSize = 11.sp)
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextPrimary,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BpscColors.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary,
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
private fun PriceRow(label: String, value: String, isDiscount: Boolean, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isTotal) BpscColors.TextPrimary else BpscColors.TextSecondary,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value, style = MaterialTheme.typography.bodyLarge, color = when {
                isTotal -> BpscColors.Primary; isDiscount -> BpscColors.Success; else -> BpscColors.TextPrimary
            }, fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun LearningStatItem(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary,
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
    "All Subjects" to Pair(Color(0xFF1565C0), Color(0xFFE8F0FD)),
    "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
    "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
    "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
    "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    "Current Affairs" to Pair(Color(0xFF1565C0), Color(0xFFE8F0FD)),
)

private fun subjectEmoji(subject: String) = when (subject) {
    "All Subjects" -> "📚"; "Polity" -> "⚖️"; "History" -> "🏛️"; "Geography" -> "🗺️"
    "Economy" -> "💰"; "Bihar GK" -> "🏔️"; "Science" -> "🔬"; "Current Affairs" -> "📰"; else -> "📖"
}