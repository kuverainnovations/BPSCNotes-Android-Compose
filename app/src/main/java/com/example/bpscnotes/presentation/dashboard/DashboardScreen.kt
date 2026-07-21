package com.example.bpscnotes.presentation.dashboard

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ads.DashboardBannerStrip
import com.example.bpscnotes.core.language.LanguageManager
import com.example.bpscnotes.core.language.LanguageSwitchButton
import com.example.bpscnotes.core.analytics.Event
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.DashboardSkeleton
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.data.remote.api.BannerDto
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.DailyTargetDto
import com.example.bpscnotes.data.remote.api.LiveClassDto
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.data.remote.api.UserStatsData
import com.example.bpscnotes.data.remote.dto.UserDto
import com.example.bpscnotes.domain.model.DayProgress
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.shared.BookmarkViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// NOTE: NO static data anywhere in this file.
//       Every value comes from DashboardViewModel.uiState.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    adManager: AdManager? = null,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    bookmarkViewModel: BookmarkViewModel   = hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state    by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val bookmarkedIds by bookmarkViewModel.bookmarkedIds.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showTargetSheet by remember { mutableStateOf(false) }
    var showActivityDetail by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()

    // On every resume: lightweight always-refresh (targets + notif badge) plus
    // a debounced full reload for stats, graph, hours, coins — so coming back
    // from a study session or quiz always shows fresh data without hammering
    // the API on every 2-second navigation hop.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            dashboardViewModel.refreshOnResume()
        }
    }
    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = state.isLoading,
        onRefresh = { dashboardViewModel.refresh() }
    ) {


        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            //windowInsets = WindowInsets(0, 0, 0, 0),
            drawerContent = {
                BpscDrawer(
                    user = state.user,
                    notifCount = state.unreadNotifCount,
                    onClose = { scope.launch { drawerState.close() } },
                    navController = navController
                )
            }
        ) {
            // ── FIX: outer Box so full-screen overlays (e.g. activity history) can sit on top ──
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cs.background)
                ) {
                    // Offline banner — shows automatically when no internet
                    com.example.bpscnotes.core.network.OfflineBanner()

                    // Auto-refresh when coming back online — evict stale cache first
                    val isOnline = com.example.bpscnotes.core.network.rememberIsOnline()
                    var wasOffline by remember { mutableStateOf(false) }
                    LaunchedEffect(isOnline) {
                        if (!isOnline) {
                            wasOffline = true
                        } else if (wasOffline) {
                            wasOffline = false
                            // Small delay so network is stable before hitting server
                            kotlinx.coroutines.delay(500)
                            dashboardViewModel.refresh()
                        }
                    }

                    // ── STICKY HEADER — never scrolls ──────────────────────────
                    val notifCount = state.unreadNotifCount
                    DashboardHeader(
                        user          = state.user,
                        stats         = state.stats,
                        greeting      = dashboardViewModel.getGreeting(),
                        targets       = state.dailyTargets,
                        notifCount    = notifCount,
                        onMenuClick   = { scope.launch { drawerState.open() } },
                        onNotifClick  = { dashboardViewModel.clearUnreadNotifCount() },
                        navController = navController
                    )

                    // ── SCROLLABLE BODY ─────────────────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {

                            // ── Global error banner ────────────────────────────────────
                            if (state.error != null) {
                                ErrorBanner(message = state.error!!) { dashboardViewModel.refresh() }
                            }

                            // ── Loading indicator (first load only) ────────────────────
                            if (state.isLoading && state.courses.isEmpty()) {
                                LoadingSection()
                            }

                            // ── Continue Learning — in-progress quiz takes priority ─────
                            val inProgress = state.inProgressSession
                            val recentAttempts = state.stats?.recentAttempts ?: emptyList()
                            if (inProgress != null) {
                                ContinueLearningSection(
                                    inProgressSession = inProgress,
                                    navController     = navController
                                )
                            } else if (recentAttempts.isNotEmpty()) {
                                ContinueLearningSection(
                                    recentAttempt = recentAttempts.first(),
                                    navController = navController
                                )
                            }

                            // ── Today's target card ────────────────────────────────────
                            TodayTargetCard(
                                targets = state.dailyTargets,
                                isLoading = state.isLoading,
                                onCreateTarget = { showTargetSheet = true },
                                onClick = { navController.navigate(Screen.DailyTargets.route) }
                            )

                            // ── Banners (between targets and weekly consistency) ────────
                            BannerSection(
                                banners = state.banners,
                                isLoading = state.isLoading,
                                navController = navController
                            )

                            // ── Weekly consistency (no fake fallback) ──────────────────
                            WeeklyConsistencyCard(
                                data = state.weeklyActivity,
                                streak = state.stats?.currentStreak ?: state.user?.streak ?: 0,
                                isLoading = state.isLoading,
                                onSeeAll = { showActivityDetail = true }
                            )

                            // Ad banner below weekly consistency chart
                            if (adManager != null) {
                                BannerAdView(
                                    adUnitId = adManager.getBannerAdUnitId()
                                )
                            }


                            // ── Daily quizzes ──────────────────────────────────────────
                            DailyQuizSection(
                                quizzes = state.dailyQuizzes,
                                isLoading = state.isLoading,
                                navController = navController
                            )

                            // ── Quick access ───────────────────────────────────────────
                            QuickAccessSection(
                                navController = navController,
                                bookmarkCount = bookmarkedIds.size
                            )

                            // ── Recommended courses ────────────────────────────────────
                            // Already-enrolled courses don't belong in a
                            // recommendation rail (QA 09-Jul issue 9)
                            RecommendedSection(
                                courses = state.courses.filter { it.enrollment == null },
                                isLoading = state.isLoading,
                                navController = navController
                            )

                            MyScheduleSection(
                                liveClasses        = state.liveClasses,
                                registeredClassIds = state.registeredClassIds,
                                navController      = navController,
                                viewModel          = dashboardViewModel
                            )

                            // Schedule toast
                            state.scheduleToast?.let { msg ->
                                LaunchedEffect(msg) {
                                    kotlinx.coroutines.delay(3_000)
                                    dashboardViewModel.clearScheduleToast()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BpscColors.Primary)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            AchievementsSection(
                                achievements = state.achievements
                            )
                            // ── FIX: enough bottom padding to clear the bottom nav bar ──
                            Spacer(modifier = Modifier
                                .navigationBarsPadding()
                                .height(5.dp))
                            // Ad banner bottom
                            if (adManager != null) {
                                BannerAdView(
                                    adUnitId = adManager.getBannerAdUnitId()
                                )
                            }
                            Spacer(modifier = Modifier
                                .navigationBarsPadding()
                                .height(5.dp))
                        }  // end scrollable Column


                        if (showTargetSheet) {
                            if (state.dailyTargets.size >= 10) {
                                Toast.makeText(
                                    LocalContext.current,
                                    str.targetMax,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Box
                            }
                            CreateTargetSheet(
                                viewModel = dashboardViewModel,
                                onDismiss = { showTargetSheet = false })
                        }
                    }  // end Box(weight=1f)
                }  // end outer Column

                // Full-screen activity detail — covers dashboard when shown
                if (showActivityDetail) {
                    BackHandler { showActivityDetail = false }
                    ActivityDetailFullScreen(
                        data        = state.monthlyActivity,
                        quizMins    = state.monthlyQuizMins,
                        roomMins    = state.monthlyRoomMins,
                        caMins      = state.monthlyCaMins,
                        lessonMins  = state.monthlyLessonMins,
                        onBack      = { showActivityDetail = false }
                    )
                }
            }  // end overlay Box
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR BANNER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    val str = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3CD))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("⚠️", fontSize = 14.sp)
        Text(
            message,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color(0xFF856404),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text(str.retry, color = BpscColors.Primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOADING SHIMMER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LoadingSection() {
    DashboardSkeleton()
}

// ─────────────────────────────────────────────────────────────────────────────
// BANNER SECTION — with proper click navigation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BannerSection(
    banners: List<BannerDto>,
    isLoading: Boolean,
    navController: NavHostController
) {
    val cs = MaterialTheme.colorScheme
    // Skeleton while loading
    if (isLoading && banners.isEmpty()) {
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(2) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
        return
    }
    if (banners.isEmpty()) return

    // Better banner: full-width auto-scrolling pager with dots
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { banners.size }

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(3500)
            val next = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val banner = banners[page]
            val colors = parseGradientSafe(banner.bgGradient ?: "")
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { navigateBanner(banner, navController) },
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = colors.ifEmpty { listOf(Color(0xFF0D47A1), Color(0xFF1976D2)) },
                                start = Offset(0f, 0f), end = Offset(500f, 250f)
                            )
                        ).padding(18.dp)
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.06f), 100.dp.toPx(), Offset(size.width + 10.dp.toPx(), -20.dp.toPx()))
                        drawCircle(Color.White.copy(0.04f), 60.dp.toPx(), Offset(size.width * 0.6f, size.height + 10.dp.toPx()))
                    }
                    // Image accent (optional) — matches admin list preview's
                    // <img className="absolute right-0 top-0 h-full w-auto
                    // object-cover opacity-30"/>: full-height, right-aligned,
                    // 30% opacity, sitting ON TOP of the bg_gradient which
                    // always fills the whole banner. No scrim needed — at
                    // 30% opacity the image doesn't compete with the title/
                    // subtitle/CTA text.
                    if (!banner.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = banner.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.FillHeight,
                            alpha = 0.3f,
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth(0.7f)
                    ) {
                        Text(banner.title, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.ExtraBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        banner.subtitle?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // CTA pill — pinned to BottomEnd, always visible
                    val ctaText = banner.ctaLabel?.takeIf { it.isNotBlank() }
                        ?: if (!banner.actionLink.isNullOrBlank()) "Open" else "View"
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(ctaText, style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Dot indicators
        if (banners.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { i ->
                    val width by animateDpAsState(
                        if (i == pagerState.currentPage) 20.dp else 6.dp,
                        label = "dot$i"
                    )
                    Box(modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i == pagerState.currentPage) BpscColors.Primary
                            else cs.outline
                        ))
                }
            }
        }
    }
}

fun parseGradientSafe(gradient: String?): List<Color> {
    val map = mapOf(
        "red-500" to Color(0xFFF44336),
        "pink-600" to Color(0xFFD81B60),
        "green-600" to Color(0xFF43A047),
        "teal-600" to Color(0xFF00897B),
        "yellow-500" to Color(0xFFFFC107),
        "orange-500" to Color(0xFFFF9800),
        "purple-600" to Color(0xFF8E24AA),
        "purple-800" to Color(0xFF6A1B9A),
    )

    if (gradient.isNullOrEmpty()) {
        return defaultGradient()
    }

    // FIX: admin's banner color picker sends a hex color (e.g. "#1565C0"),
    // not one of the named "from-x to-y" gradient presets below. Parse hex
    // as a solid color and build a 2-color gradient by darkening it
    // slightly, so admin-selected colors actually show up in the app
    // instead of always falling back to the default blue.
    if (gradient.startsWith("#")) {
        return try {
            val solid = Color(android.graphics.Color.parseColor(gradient))
            val darker = Color(
                red   = (solid.red   * 0.75f),
                green = (solid.green * 0.75f),
                blue  = (solid.blue  * 0.75f),
                alpha = solid.alpha
            )
            listOf(solid, darker)
        } catch (e: IllegalArgumentException) {
            defaultGradient()
        }
    }

    val parts = gradient.split(" ")

    val from = parts.find { it.startsWith("from-") }?.removePrefix("from-")
    val to = parts.find { it.startsWith("to-") }?.removePrefix("to-")

    val fromColor = map[from]
    val toColor = map[to]

    return when {
        fromColor != null && toColor != null -> listOf(fromColor, toColor)
        fromColor != null -> listOf(fromColor, fromColor) // duplicate fallback
        toColor != null -> listOf(toColor, toColor)
        else -> defaultGradient()
    }
}

fun defaultGradient() = listOf(
    Color(0xFF0D47A1),
    Color(0xFF1976D2)
)

/** Navigate based on banner.actionLink or banner.type */
private fun navigateBanner(banner: BannerDto, nav: NavHostController) {
    val link = banner.actionLink
    when {
        // Deep links to a specific course/quiz (not offered by the admin
        // dropdown today, but kept for future direct-link banners)
        link != null && link.startsWith("/course/") ->
            nav.navigate(Screen.CourseDetail.createRoute(link.removePrefix("/course/")))
        link != null && link.startsWith("/quiz/") ->
            nav.navigate(Screen.QuizDetail.createRoute(link.removePrefix("/quiz/")))

        // The 12 routes actually offered in the admin "CTA Route" dropdown
        // (ROUTES array in banners/page.tsx) — map each to its real screen.
        link == "/quizzes"         -> nav.navigate(Screen.QuizList.route)
        link == "/current-affairs" -> nav.navigate(Screen.CurrentAffairs.route)
        link == "/study-materials" -> nav.navigate(Screen.StudyMaterials.route)
        link == "/wallet"          -> nav.navigate(Screen.CoinWallet.route)
        link == "/subscription"    -> nav.navigate(Screen.Subscription.route)
        link == "/rooms_hub"       -> nav.navigate(Screen.RoomsHub.route)
        link == "/jobs"            -> nav.navigate(Screen.JobVacancies.route)
        link == "/leaderboard"     -> nav.navigate(Screen.Leaderboard.route)
        link == "/flashcards"      -> nav.navigate(Screen.ActiveRecall.route)
        link == "/achievements"    -> nav.navigate(Screen.Achievements.route)
        link == "/challenges"      -> nav.navigate(Screen.WeeklyChallenges.route)
        link == "/courses"         -> nav.navigate(Screen.MyLearningCourses.route)

        // No recognized route set — fall back to banner.type, then to
        // My Learning as a last resort.
        banner.type == "course"       -> nav.navigate(Screen.MyLearning.route)
        banner.type == "quiz"         -> nav.navigate(Screen.QuizList.route)
        banner.type == "subscription" -> nav.navigate(Screen.Subscription.route)
        banner.type == "job"          -> nav.navigate(Screen.JobVacancies.route)
        else                          -> nav.navigate(Screen.MyLearning.route)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAILY QUIZ SECTION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DailyQuizSection(
    quizzes: List<QuizPreviewDto>,
    isLoading: Boolean,
    navController: NavHostController
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            SectionHeader(str.dashboardTodayQuizzes)
            TextButton(onClick = {
                navController.navigate(Screen.QuizList.route)}) {
                Text(str.seeAll, color = BpscColors.Primary, style = MaterialTheme.typography.bodyMedium)
            }
        }



        when {
            isLoading && quizzes.isEmpty() -> {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(3) {
                        Box(modifier = Modifier
                            .width(190.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE0E0E0)))
                    }
                }
            }
            quizzes.isEmpty() -> {
                EmptyState(
                    emoji    = "📝",
                    message  = str.dashboardNoQuizzes,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            else -> {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(quizzes, key = { it.id }) { quiz ->
                        Card(
                            modifier  = Modifier
                                .width(190.dp)
                                .clickable {
                                    if (quiz.totalQuestions == 0) {
                                        android.widget.Toast.makeText(
                                            context,
                                            str.dashboardQuizNoQuestions,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                                    }
                                },
                            shape     = RoundedCornerShape(18.dp),
                            colors    = CardDefaults.cardColors(containerColor = cs.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        quiz.type.replaceFirstChar { it.uppercase() },
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = BpscColors.Primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BpscColors.PrimaryLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    if (quiz.isAttempted) {
                                        Text(
                                            str.dashboardDone,
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = BpscColors.Success,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BpscColors.Success.copy(0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${quiz.totalQuestions}Q", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                    Text("${quiz.durationMins}min", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                    if (quiz.coinsReward > 0) Text("🪙${quiz.coinsReward}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEADER — real stats from API
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    user: UserDto?,
    stats: UserStatsData?,
    greeting: String,
    targets: List<DailyTargetDto>,
    notifCount: Int = 0,
    onMenuClick: () -> Unit,
    onNotifClick: () -> Unit = {},
    navController: NavHostController
) {
    // Computed values — all from API, never hardcoded
    val name         = user?.name ?: ""
    val coins        = user?.coins ?: 0
    val streak       = stats?.currentStreak ?: user?.streak ?: 0
    val rank         = user?.rank
    val studyMinutes = stats?.totalStudyMinutes ?: user?.totalStudyMinutes ?: 0
    val todayStudyMinutes = stats?.todayStudyMinutes ?: 0
    val accuracy     = stats?.accuracy ?: user?.accuracy ?: 0.0
    val str = LocalStrings.current

    val completed = targets.count { it.isCompleted }
    val total     = targets.size
    val targetProgress = if (total > 0) completed.toFloat() / total else 0f
    val animTargetProg by animateFloatAsState(targetProgress, tween(1000), label = "headerProg")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF051D56),
                        Color(0xFF0A2472),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0),
                        Color(0xFF1976D2)
                    ),
                    start = Offset(0f, 0f), end = Offset(600f, 700f)
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.05f), 200.dp.toPx(), Offset(size.width + 50.dp.toPx(), -70.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 110.dp.toPx(), Offset(size.width - 10.dp.toPx(), size.height * 0.5f))
            drawCircle(Color.White.copy(0.06f), 70.dp.toPx(),  Offset(-25.dp.toPx(), size.height * 0.78f))
            drawArc(Color.White.copy(0.03f), 150f, 160f, false,
                Offset(size.width - 130.dp.toPx(), size.height * 0.15f),
                androidx.compose.ui.geometry.Size(160.dp.toPx(), 160.dp.toPx()), style = Stroke(40.dp.toPx()))
            val sp = 28.dp.toPx(); var x = sp
            while (x < size.width) { var y = sp
                while (y < size.height) { drawCircle(Color.White.copy(0.05f), 1.dp.toPx(), Offset(x, y)); y += sp }; x += sp }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(0.3f),
                        Color.White.copy(0.7f),
                        Color.White.copy(0.3f),
                        Color.Transparent
                    )
                )
            ))

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 34.dp)) {

            val coinDisplay = when {
                coins >= 1_000_000 -> "${coins / 1_000_000}M"
                coins >= 1_000     -> "${String.format("%.1f", coins / 1000f)}k"
                else               -> "$coins"
            }

            // ── TOP BAR — 3 balanced items, notification always visible ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu button with notification badge
                BadgedBox(badge = {
                    if (notifCount > 0) Badge(
                        containerColor = Color(0xFFEF5350),
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                    ) {
                        Text(
                            if (notifCount > 99) "99+" else "$notifCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = Color.White
                        )
                    }
                }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Box(Modifier.width(16.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(11.dp).height(2.dp).background(Color.White.copy(0.6f), RoundedCornerShape(1.dp)))
                            Box(Modifier.width(16.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        }
                    }
                }

                // Center — logo + app name (compact)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(0.1f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.bpscnotes.app.R.drawable.ic_bpsc_logo),
                        contentDescription = "BPSCNotes",
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        "BPSCNotes",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right — Wallet pill + Search
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wallet + coin pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(0.13f))
                            .border(0.5.dp, Color.White.copy(0.25f), RoundedCornerShape(22.dp))
                            .clickable { navController.navigate(Screen.CoinWallet.route) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("🪙", fontSize = 11.sp, lineHeight = 13.sp)
                        Text(
                            coinDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    // Search circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                            .clickable { navController.navigate(Screen.GlobalSearch.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── HERO ROW ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(BpscColors.Success))
                        Text(
                            greeting,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.7f)
                        )
                    }
                    // Name
                    Text(
                        if (name.isNotEmpty()) "${name.split(" ").first()} 👋" else str.dashboardAspirant,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )
                    // Streak chip
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFF8F00).copy(0.2f))
                                .border(0.5.dp, Color(0xFFFFB300).copy(0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Whatshot, null, tint = BpscColors.CoinGold, modifier = Modifier.size(13.dp))
                            Text(
                                if (streak > 0) "$streak ${str.profileDayStreak}" else str.dashboardStreak,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Daily target circular progress ring
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clickable { navController.navigate(Screen.DailyTargets.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        val inset  = stroke / 2
                        val arcSz  = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                        // Track
                        drawArc(Color.White.copy(0.18f), -90f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                        // Progress
                        if (animTargetProg > 0f) {
                            drawArc(
                                brush      = Brush.sweepGradient(listOf(Color(0xFF42A5F5), Color(0xFF64B5F6), Color.White.copy(0.9f))),
                                startAngle = -90f,
                                sweepAngle = animTargetProg * 360f,
                                useCenter  = false,
                                topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                                size       = arcSz,
                                style      = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            if (total == 0) "--" else "${(animTargetProg * 100).toInt()}%",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 13.sp
                        )
                        Text(
                            if (total == 0) "targets" else "done",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White.copy(0.65f),
                            fontSize = 8.sp
                        )
                    }
                }

            }
            Spacer(Modifier.height(14.dp))

            // ── Stats strip — ALL values from API ─────────────────────────
            val rankText  = if (rank != null) "#$rank" else "--"
            // Today's study time — resets daily (backend computes live from
            // study_sessions for CURRENT_DATE, not the cumulative lifetime total).
            val hoursText = when {
                todayStudyMinutes <= 0 -> "--"
                todayStudyMinutes < 60 -> "${todayStudyMinutes}m"
                else -> "${todayStudyMinutes / 60}h ${todayStudyMinutes % 60}m"
            }
            val accuracyValue = stats?.accuracy
                ?: user?.accuracy?.toDoubleOrNull()
                ?: 0.0

            val accText = if (accuracyValue > 0.0) {
                "${accuracyValue.toInt()}%"
            } else {
                "--"
            }
            val topicsText = if (total > 0) "$completed/$total" else "--"

            Row(modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(0.16f),
                            Color.White.copy(0.08f)
                        )
                    )
                )
                .border(
                    0.5.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.Transparent)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 4.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                HeaderStat("📚", topicsText, "Topics")
                HeaderStatDivider()
                HeaderStat("🏆", rankText,  "Rank")
                HeaderStatDivider()
                HeaderStat("⏱️", hoursText,  "Today")
                HeaderStatDivider()
                HeaderStat("✅", accText,   str.dashboardAccuracy)
            }
        }
    }
}

@Composable
private fun HeaderStat(icon: String, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.width(66.dp)) {
        Text(icon, fontSize = 15.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeaderStatDivider() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(modifier = Modifier
        .width(0.5.dp)
        .height(36.dp)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(0.28f),
                    Color.Transparent
                )
            )
        ))
}

// ─────────────────────────────────────────────────────────────────────────────
// TODAY'S TARGET CARD — from state.dailyTargets (API)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TodayTargetCard(
    targets: List<DailyTargetDto>,
    isLoading: Boolean,
    onCreateTarget: () -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val completed  = targets.count { it.isCompleted }
    val total      = targets.size
    val progress   = if (total > 0) completed.toFloat() / total else 0f
    val animProg  by animateFloatAsState(progress, tween(1000), label = "prog")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // ✅ spacing instead
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    BpscColors.Primary,
                                    Color(0xFF1976D2)
                                )
                            )
                        ), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.TrackChanges, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(str.dashboardTodayFocus, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        when {
                            isLoading && targets.isEmpty() -> Text(str.loading, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                            total == 0                     -> Text(str.dashboardNoTargets2, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                            else                           -> Text("$completed/$total Topics", style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Icon(Icons.Rounded.KeyboardArrowRight, null, tint = cs.outline, modifier = Modifier.size(20.dp))
            }

           // Spacer(Modifier.height(16.dp))
            /*Box(modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BpscColors.PrimaryLight)) {
                Box(modifier = Modifier
                    .fillMaxWidth(animProg)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1565C0),
                                Color(0xFF42A5F5)
                            )
                        )
                    ))
            }*/
            Spacer(Modifier.height(16.dp))

            when {
                isLoading && targets.isEmpty() -> {
                    repeat(3) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEEEEEE)))
                    }
                }
                targets.isEmpty() -> {
                    Text(str.dashboardNoTargets, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                else -> {
                    targets.take(2).forEach { t ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (t.isCompleted) BpscColors.Success.copy(0.12f) else BpscColors.Surface), contentAlignment = Alignment.Center) {
                                Icon(if (t.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (t.isCompleted) BpscColors.Success else BpscColors.TextHint, modifier = Modifier.size(16.dp))
                            }
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, color = if (t.isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                           /* Text(t.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BpscColors.PrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp))*/
                        }
                    }
                    if (targets.size > 2) Text("+${targets.size - 2} more topics", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.5.dp, BpscColors.Primary, RoundedCornerShape(13.dp))
                .clickable(onClick = onCreateTarget), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Add, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    Text(str.dashboardCreateTarget, style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WEEKLY CONSISTENCY — no dummy fallback
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeeklyConsistencyCard(
    data: List<DayProgress>,
    streak: Int,
    isLoading: Boolean,
    onSeeAll: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = cs.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(str.dashboardWeeklyConsistency, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                    Text(str.dashboardWeeklySubtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BpscColors.AccentLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Whatshot, null, tint = BpscColors.Accent, modifier = Modifier.size(14.dp))
                        Text(if (streak > 0) "$streak streak" else "0 streak", style = MaterialTheme.typography.labelSmall, color = BpscColors.Accent, fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = onSeeAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(str.seeAll, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.Primary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                // Loading state
                isLoading && data.isEmpty() -> {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
                // Empty state (stats API returned no data)
                data.isEmpty() -> {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📊", fontSize = 32.sp)
                            Text(str.dashboardNoActivity, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            Text(str.dashboardStartStudying, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
                // Chart (real API data)
                else -> {
                    // ── Y-axis = minutes of study activity ─────────────────
                    // Sources: each quiz completed ≈ 5 min, study room sessions = actual minutes
                    // Y-axis ceiling is LOCKED per data set using `remember(data)` so it
                    // doesn't rescale every recompose and make the chart visually jump.

                    // Format as "Xh Ym" when >= 60, else "Xm"
                    fun fmtMin(m: Int): String = when {
                        m == 0  -> "0"
                        m >= 60 -> "${m / 60}h${if (m % 60 > 0) " ${m % 60}m" else ""}"
                        else    -> "${m}m"
                    }

                    // Compute ceiling ONCE per data set — stable across recompositions
                    // so the graph doesn't visually shift when today's score updates
                    val ceilMins by remember(data) {
                        val rawMax = data.maxOfOrNull { it.score } ?: 0
                        // Minimum 60 min ceiling; snap up to next 30-min boundary
                        val ceil   = maxOf(60, ((rawMax + 29) / 30) * 30)
                        mutableStateOf(ceil)
                    }
                    val maxVal  = ceilMins.toFloat()
                    val yLabels = listOf(ceilMins, ceilMins * 3 / 4, ceilMins / 2, ceilMins / 4, 0)

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)) {

                        // Y-axis labels
                        Column(
                            modifier = Modifier.width(36.dp).fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            yLabels.forEach { v ->
                                Text(
                                    fmtMin(v),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = BpscColors.TextHint,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                )
                            }
                        }

                        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            val w = size.width; val h = size.height; val count = data.size
                            if (count < 2) return@Canvas
                            val stepX = w / (count - 1).toFloat()

                            // Map each day's minutes → pixel Y (clamped so nothing clips)
                            val points = data.mapIndexed { i, d ->
                                val ratio = (d.score.toFloat() / maxVal).coerceIn(0f, 1f)
                                Offset(i * stepX, h - ratio * h)
                            }

                            // Horizontal grid lines
                            listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                                drawLine(Color(0xFFF0F0F0), Offset(0f, h * r), Offset(w, h * r), 1.dp.toPx())
                            }

                            // Fill area under curve
                            val fillPath = Path().apply {
                                moveTo(points.first().x, h)
                                points.forEach { lineTo(it.x, it.y) }
                                lineTo(points.last().x, h)
                                close()
                            }
                            drawPath(fillPath, Brush.verticalGradient(
                                listOf(BpscColors.Primary.copy(0.18f), BpscColors.Primary.copy(0f)), 0f, h
                            ))

                            // Smooth bezier curve
                            val linePath = Path()
                            points.forEachIndexed { i, pt ->
                                if (i == 0) linePath.moveTo(pt.x, pt.y)
                                else {
                                    val prev = points[i - 1]
                                    val cx1 = prev.x + (pt.x - prev.x) * 0.5f
                                    val cx2 = pt.x - (pt.x - prev.x) * 0.5f
                                    linePath.cubicTo(cx1, prev.y, cx2, pt.y, pt.x, pt.y)
                                }
                            }
                            drawPath(linePath, Color(0xFF1565C0), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

                            // Data point dots
                            points.forEachIndexed { i, pt ->
                                val isToday = i == count - 1
                                val outerR  = if (isToday) 7.dp.toPx() else 4.5.dp.toPx()
                                val innerR  = if (isToday) 3.5.dp.toPx() else 2.dp.toPx()
                                drawCircle(if (isToday) BpscColors.Accent else BpscColors.Primary, outerR, pt)
                                drawCircle(Color.White, innerR, pt)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // X-axis: day label + value below each dot
                    val maxScore = data.maxOfOrNull { it.score } ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        data.forEachIndexed { i, d ->
                            val isToday  = i == data.size - 1
                            val isMaxDay = d.score == maxScore && maxScore > 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                // Day label
                                Text(
                                    d.day,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (isToday) BpscColors.Primary else BpscColors.TextSecondary,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize   = 10.sp,
                                )
                                // Show value for every day that has data
                                if (d.score > 0) {
                                    Text(
                                        fmtMin(d.score),
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = when {
                                            isToday  -> BpscColors.Primary
                                            isMaxDay -> BpscColors.Accent
                                            else     -> BpscColors.TextHint
                                        },
                                        fontSize   = 8.sp,
                                        fontWeight = if (isToday || isMaxDay) FontWeight.Bold else FontWeight.Normal,
                                    )
                                } else {
                                    // Zero day — show dash so spacing stays consistent
                                    Text(
                                        "–",
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = BpscColors.TextHint.copy(alpha = 0.4f),
                                        fontSize = 8.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            str.dashStudyActivityLegend,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = BpscColors.TextHint,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTINUE LEARNING — last quiz attempt card (Testbook/Unacademy style)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ContinueLearningSection(
    inProgressSession: com.example.bpscnotes.data.remote.api.InProgressSessionDto,
    navController: NavHostController
) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val progress = if (inProgressSession.totalQuestions > 0)
        inProgressSession.answeredCount.toFloat() / inProgressSession.totalQuestions
    else 0f
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
       // SectionHeader(title = str.dashboardContinueLearning)
        Spacer(Modifier.height(4.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.QuizPlayer.createRoute(inProgressSession.quizId)) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF9800).copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (inProgressSession.quizType) { "daily" -> "📅"; "mock" -> "📋"; else -> "📝" },
                        fontSize = 22.sp
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        str.dashResumeQuiz,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        inProgressSession.quizTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${inProgressSession.answeredCount}/${inProgressSession.totalQuestions} answered",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8D6E63)
                    )
                    Box(
                        Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFFF9800).copy(0.2f))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress).fillMaxHeight()
                                .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ContinueLearningSection(
    recentAttempt: com.example.bpscnotes.data.remote.api.RecentQuizAttemptDto,
    navController: NavHostController
) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        SectionHeader(title = str.dashboardContinueLearning)
        Spacer(Modifier.height(10.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.QuizList.route) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(BpscColors.Primary.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (recentAttempt.type) { "daily" -> "📅"; "mock" -> "📋"; else -> "📝" },
                        fontSize = 22.sp
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        recentAttempt.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${recentAttempt.subject} · Last score: ${recentAttempt.score}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextSecondary
                    )
                    val scoreProgress = recentAttempt.score / 100f
                    Box(
                        Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BpscColors.Primary.copy(0.15f))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(scoreProgress).fillMaxHeight()
                                .background(
                                    when {
                                        recentAttempt.score >= 80 -> BpscColors.Success
                                        recentAttempt.score >= 50 -> BpscColors.Primary
                                        else -> Color(0xFFE74C3C)
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK ACCESS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickAccessSection(navController: NavHostController, bookmarkCount: Int) {
    val str = LocalStrings.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        SectionHeader(title = str.dashboardQuickAccess)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LargeQuickCard(str.dashboardCurrentAffairs, "Today's Updates", Icons.Rounded.Newspaper, listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1976D2)), Modifier.fillMaxWidth()) { navController.navigate(Screen.CurrentAffairs.route) }
                if (bookmarkCount > 0) {
                    Row(modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF8E1))
                        .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Bookmark, null, tint = BpscColors.CoinGold, modifier = Modifier.size(11.dp))
                        Text("$bookmarkCount", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                }
            }
            LargeQuickCard("Courses", str.dashboardContinueLearning, Icons.Rounded.MenuBook, listOf(Color(0xFFBF360C), Color(0xFFE64A19), Color(0xFFFF5722)), Modifier.weight(1f)) { navController.navigate(Screen.MyLearningCourses.route) }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallQuickCard("Active\nRecall", Icons.Rounded.Psychology, BpscColors.PrimaryLight, BpscColors.Primary, Modifier.weight(1f)) { navController.navigate(Screen.ActiveRecall.route) }
            SmallQuickCard("Mock\nTests",  Icons.Rounded.Assignment, Color(0xFFFFF4EC), BpscColors.Accent, Modifier.weight(1f)) { navController.navigate(Screen.MockTests.route) }
            SmallQuickCard("Student\nHub", Icons.Rounded.Groups, Color(0xFFE8FDF4), BpscColors.Success, Modifier.weight(1f)) { navController.navigate(Screen.StudyMaterials.route) }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallQuickCard("Job\nAlerts",  Icons.Rounded.Work, Color(0xFFFFF0EA), Color(0xFFE67E22), Modifier.weight(1f)) { navController.navigate(Screen.JobVacancies.route) }
            SmallQuickCard("Downloads",   Icons.Rounded.Download, Color(0xFFEDE7F6), Color(0xFF7E57C2), Modifier.weight(1f)) { navController.navigate(Screen.Downloads.route) }
            SmallQuickCard("My\nNotebook", Icons.Rounded.EditNote, Color(0xFFE0F7FA), Color(0xFF00838F), Modifier.weight(1f)) { navController.navigate(Screen.Notebook.route) }
        }
        Spacer(Modifier.height(10.dp))
        // Answer Writing — full-width feature banner (Mains practice)
        AnswerWritingBanner { navController.navigate(Screen.AnswerWriting.route) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANSWER WRITING BANNER — full-width gradient feature card on the dashboard.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnswerWritingBanner(onClick: () -> Unit) {
    val str = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB)),
                    Offset(0f, 0f), Offset(800f, 300f)
                )
            )
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.06f), 90.dp.toPx(), Offset(size.width - 10.dp.toPx(), -20.dp.toPx()))
            drawCircle(Color.White.copy(0.05f), 55.dp.toPx(), Offset(30.dp.toPx(), size.height + 10.dp.toPx()))
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✍️", fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        str.awTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        str.dashNewBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1A237E), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFD54F))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    str.awDashSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.75f)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = Color.White.copy(0.8f), modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECOMMENDED COURSES — always shows (skeleton / empty / list)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecommendedSection(
    courses: List<CourseDto>,
    isLoading: Boolean,
    navController: NavHostController
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = str.dashboardRecommended)
            // MyLearningCourses opens the Marketplace/Store tab (all courses).
            // MyLearning(startTab=0) landed on "My Courses" — wrong place for
            // a recommendations "See All" (QA 09-Jul issue 9).
            TextButton(onClick = { navController.navigate(Screen.MyLearningCourses.route) }) {
                Text(str.seeAll, color = BpscColors.Primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        when {
            isLoading && courses.isEmpty() -> {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(3) { Box(modifier = Modifier
                        .width(168.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE0E0E0))) }
                }
            }
            courses.isEmpty() -> EmptyState("📚", "No courses available right now.", Modifier.padding(horizontal = 16.dp))
            else -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(courses, key = { it.id }) { course ->
                    // Course detail navigation unchanged — DO NOT MODIFY
                    CourseCard(course = course, onClick = { navController.navigate(Screen.CourseDetail.createRoute(course.id)) })
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: CourseDto, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val subjectColors = mapOf(
        "Bihar GK"    to Triple(Color(0xFF2ECC71), Color(0xFFE8FDF4), "📗"),
        "Polity"      to Triple(Color(0xFF9B59B6), Color(0xFFF3E8FD), "⚖️"),
        "Economy"     to Triple(Color(0xFFE67E22), Color(0xFFFFF0EA), "📈"),
        "Geography"   to Triple(Color(0xFF1ABC9C), Color(0xFFE8FDF8), "🗺️"),
        "History"     to Triple(Color(0xFFE74C3C), Color(0xFFFEE8E8), "📜"),
        "Science"     to Triple(Color(0xFF3498DB), Color(0xFFE8F4FD), "🔬"),
        "Mathematics" to Triple(Color(0xFFF39C12), Color(0xFFFFF8E1), "➗"),
    )
    val (accent, bg, emoji) = subjectColors[course.subject] ?: Triple(BpscColors.Primary, BpscColors.PrimaryLight, "📚")
    val rawCompleted = course.enrollment?.completed_lessons?.toFloat() ?: 0f
    val progress = if (course.totalLessons > 0) (rawCompleted / course.totalLessons).coerceIn(0f, 1f) else 0f
    val hasThumbnail = !course.thumbnail_url.isNullOrBlank()

    Card(
        modifier  = Modifier.width(168.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            // ── Thumbnail / Fallback ──────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(78.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasThumbnail) {
                    AsyncImage(
                        model        = course.thumbnail_url,
                        contentDescription = course.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier     = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(0.15f), Color.Transparent),
                                    startY = 0f, endY = 60f
                                ),
                                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(accent.copy(0.15f), bg)),
                                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 28.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                course.subject.ifBlank { "Course" },
                                style      = MaterialTheme.typography.labelSmall,
                                color      = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 9.sp
                            )
                        }
                    }
                }
                if (course.isPaid) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.CoinGold)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(str.badgePro, style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // ── Fixed-height details — all cards same size ────
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .height(76.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title always exactly 2 lines
                Text(
                    course.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    minLines   = 2,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                // Instructor
                Text(
                    course.instructor ?: "BPSCNotes",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Progress / price — no dot artifact at 0%
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress   = { progress },
                            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color      = accent,
                            trackColor = bg
                        )
                        Text(
                            "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    } else {
                        // Plain Box — zero progress, no dot/nub
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(bg)
                        )
                        Text(
                            if (course.isPaid) "₹${course.price}" else "Free",
                            style      = MaterialTheme.typography.titleSmall,
                            color      = if (course.isPaid) BpscColors.Accent else BpscColors.Success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// SCHEDULE  (placeholder until Live Classes API — intentional, noted clearly)
// ─────────────────────────────────────────────────────────────────────────────
data class ScheduleItem(val title: String, val time: String, val icon: ImageVector, val isLive: Boolean = false, val color: Color)
data class Achievement(val emoji: String, val label: String, val color: Color, val earned: Boolean)

@Composable
private fun MyScheduleSection(
    liveClasses:        List<LiveClassDto>,
    registeredClassIds: Set<String>,
    navController:      NavHostController,
    viewModel:          DashboardViewModel
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var showHistory by remember { mutableStateOf(false) }

    val upcoming = remember(liveClasses) { liveClasses.filter { it.status != "ended" } }
    val history  = remember(liveClasses) { liveClasses.filter { it.status == "ended" } }
    val displayed = if (showHistory) history else upcoming

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SectionHeader(str.dashboardMySchedule, str.dashboardUpcomingEvents)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (history.isNotEmpty()) {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(
                            if (showHistory) "Upcoming" else "History (${history.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = BpscColors.Primary
                        )
                    }
                }
                /*TextButton(onClick = { navController.navigate(com.example.bpscnotes.presentation.navigation.Routes.Screen.StudySessionHistory.route) }) {
                    Text(str.dashSessions, style = MaterialTheme.typography.labelMedium, color = BpscColors.Primary)
                }*/
            }
        }
        Spacer(Modifier.height(12.dp))

        if (displayed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BpscColors.PrimaryLight)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (showHistory) "No past classes" else str.dashboardNoClasses,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
            return
        }

        displayed.forEach { item ->

            val isLive = item.status == "live"
            val isEnded = item.status == "ended"

            val color = when {
                isLive  -> Color(0xFFE74C3C)
                isEnded -> BpscColors.TextSecondary
                else    -> BpscColors.Primary
            }

            val isRegistered = registeredClassIds.contains(item.id)
            val onCardClick: () -> Unit = {
                when (item.status) {
                    "live" -> {
                        val link = item.meetingLink
                        if (!link.isNullOrBlank()) {
                            navController.navigate(
                                Screen.LiveClassViewer.createRoute(
                                    url          = link,
                                    title        = item.title,
                                    instructor   = item.instructor,
                                    durationMins = item.durationMins
                                )
                            )
                        } else {
                            viewModel.setScheduleToast(str.dashboardNoMeetingLink)
                        }
                    }
                    "ended" -> { viewModel.setScheduleToast(str.dashboardClassEnded) }
                    else -> {
                        if (!isRegistered) {
                            viewModel.registerLiveClass(item.id)
                            com.example.bpscnotes.core.analytics.Event.liveClassRegistered(item.id, item.title)
                        }
                        else viewModel.setScheduleToast(str.dashboardAlreadyRegistered)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable(onClick = onCardClick)
                    .then(if (isEnded) Modifier.alpha(0.65f) else Modifier),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(if (isEnded) 0.dp else 2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {

                    // Left color strip
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(72.dp)
                            .background(
                                color,
                                RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Icon box
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(color.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isEnded) Icons.Rounded.History else Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Text section
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${item.instructor}: ${item.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatDateTime(item.scheduledAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }

                        // Status badge
                        when {
                            isLive -> Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE74C3C))
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(cs.surface))
                                Text(str.roomsLive, style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                            isEnded -> Text(
                                str.dashEnded,
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(cs.background)
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                            isRegistered -> Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BpscColors.Success.copy(0.1f))
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null,
                                    tint = BpscColors.Success, modifier = Modifier.size(12.dp))
                                Text(str.dashRegistered, style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Success, fontWeight = FontWeight.Bold)
                            }
                            else -> Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BpscColors.Primary.copy(0.1f))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(str.dashRegister, style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDateTime(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, hh:mm a")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        iso
    }
}
@Composable
private fun AchievementsSection(achievements: List<AchievementItem>) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    if (achievements.isEmpty()) return
    var selectedAchievement by remember { mutableStateOf<AchievementItem?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader("Achievements")
        Spacer(Modifier.height(14.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(achievements) { a ->
                val progressRatio = (a.progress.toFloat() / a.max).coerceIn(0f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedAchievement = a }
                ) {
                    Box(
                        modifier = Modifier.size(68.dp).clip(CircleShape)
                            .background(if (a.earned) Color(a.colorHex).copy(0.12f) else cs.outline)
                            .border(2.dp, if (a.earned) Color(a.colorHex) else BpscColors.TextHint.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(a.emoji, fontSize = 28.sp, modifier = Modifier.alpha(if (a.earned) 1f else 0.4f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.label, style = MaterialTheme.typography.labelSmall,
                        color = if (a.earned) BpscColors.TextPrimary else BpscColors.TextHint,
                        textAlign = TextAlign.Center, lineHeight = 14.sp, minLines = 2, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text("${a.progress}/${a.max}", style = MaterialTheme.typography.labelSmall,
                        color = Color(a.colorHex), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { progressRatio },
                        modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(10.dp)),
                        color = Color(a.colorHex), trackColor = cs.outline)
                }
            }
        }
    }

    // Achievement detail dialog (Fix #12)
    selectedAchievement?.let { a ->
        val progressRatio = (a.progress.toFloat() / a.max).coerceIn(0f, 1f)
        AlertDialog(
            onDismissRequest = { selectedAchievement = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = cs.surface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(if (a.earned) Color(a.colorHex).copy(0.12f) else cs.outline)
                        .border(2.dp, if (a.earned) Color(a.colorHex) else BpscColors.TextHint.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(a.emoji, fontSize = 36.sp, modifier = Modifier.alpha(if (a.earned) 1f else 0.4f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(a.label, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = cs.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Status pill
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (a.earned) Color(0xFF2E7D32).copy(0.08f) else BpscColors.PrimaryLight)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (a.earned) "🏆" else "🔒", fontSize = 16.sp)
                        Text(
                            if (a.earned) "Achievement Unlocked!" else "Keep going!",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (a.earned) Color(0xFF2E7D32) else BpscColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Progress
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(str.dashProgress, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            Text("${a.progress} / ${a.max}", style = MaterialTheme.typography.bodyMedium,
                                color = Color(a.colorHex), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { progressRatio },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = Color(a.colorHex), trackColor = cs.outline)
                        Text("${(progressRatio * 100).toInt()}% complete",
                            style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    }
                    // description field does not exist on AchievementItem — removed
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAchievement = null }) {
                    Text(str.closeLabel, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CREATE TARGET SHEET
// ─────────────────────────────────────────────────────────────────────────────
/*@Composable
fun CreateTargetSheet(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var inputText by remember { mutableStateOf("") }
    val addedItems = remember { mutableStateListOf<String>() }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier.fillMaxWidth().clickable(enabled = false, onClick = {}), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(cs.outline).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                Text(str.dashboardCreateCustomTarget, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(str.dashboardBuildPlan, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cs.background).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f).padding(vertical = 12.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface), decorationBox = { inner -> if (inputText.isEmpty()) Text(str.dashboardWhatNext, color = BpscColors.TextHint); inner() }, singleLine = true)
                    IconButton(onClick = { if (inputText.isNotBlank() && addedItems.size < 5) { addedItems.add(inputText.trim()); inputText = "" } }, modifier = Modifier.size(36.dp).clip(CircleShape).background(BpscColors.Primary)) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${addedItems.size} / 5 targets added", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                if (addedItems.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    addedItems.forEachIndexed { i, item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) { Text("${i+1}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold) }
                            Text(item, modifier = Modifier.weight(1f))
                            IconButton(onClick = { addedItems.removeAt(i) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Close, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { if (addedItems.isNotEmpty()) onDismiss() }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = addedItems.isNotEmpty(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Text("Set Target (${addedItems.size} items)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}*/

// ─────────────────────────────────────────────────────────────────────────────
// DRAWER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BpscDrawer(
    user: UserDto?,
    notifCount: Int = 0,
    onClose: () -> Unit,
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current

    // FIX: this drawer's Logout button previously only navigated to
    // Login (popUpTo(0)) without ever calling tokenStore.logout() or
    // POST /auth/logout — auth_token/user_mobile/has_mpin all stayed on
    // disk. The Login screen LOOKED logged out, but killing and
    // reopening the app sent Splash straight back to Main with the
    // same account, since the token was still present. Reuse
    // SettingsViewModel.logOut() (POST /auth/logout + tokenStore.logout()
    // + cache eviction), then do the same full Activity restart Settings
    // does so every Hilt ViewModel is actually destroyed.
    val settingsState by settingsViewModel.state.collectAsState()
    val drawerContext = LocalContext.current
    LaunchedEffect(settingsState.loggedOut) {
        if (settingsState.loggedOut) {
            val activity = drawerContext as? android.app.Activity ?: return@LaunchedEffect
            val intent = android.content.Intent(activity, activity::class.java).apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)
        }
    }
    val menuItems = listOf(
        // ── Study (0-4) ──────────────────────────────────────
        Triple(Icons.Rounded.TrackChanges,  str.targetTitle,          Screen.DailyTargets.route),
        Triple(Icons.Rounded.Quiz,          str.quizDaily + "zes",    Screen.QuizList.route),
        Triple(Icons.Rounded.Newspaper,     str.caTitle,              Screen.CurrentAffairs.route),
        Triple(Icons.Rounded.Psychology,    str.dashboardActiveRecall,Screen.ActiveRecall.route),
        Triple(Icons.Rounded.EmojiEvents,   str.profileAchievements,  Screen.Achievements.route),
        // ── Explore (5-8) ────────────────────────────────────
        Triple(Icons.Rounded.Leaderboard,   "Leaderboard",            Screen.GlobalLeaderboard.route),
        Triple(Icons.Rounded.Download,      "Downloads",              Screen.Downloads.route),
        Triple(Icons.Rounded.Work,          str.jobsTitle,            Screen.JobVacancies.route),
        // ── Account (9-10) ───────────────────────────────────
        Triple(Icons.Rounded.Notifications, "Notifications",          Screen.NotificationSettings.route),
        Triple(Icons.Rounded.Settings,      str.drawerSettings,       Screen.Settings.route),
    )
    val scrollState = rememberScrollState()
    ModalDrawerSheet(drawerShape = RoundedCornerShape(0.dp), drawerContainerColor = Color.White, windowInsets = WindowInsets(0,0,0,0), modifier = Modifier
        .width(300.dp)
        .fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF051D56),
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0)
                        ), Offset(0f, 0f), Offset(300f, 200f)
                    )
                )
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f))
                        .border(1.5.dp, Color.White.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        if (!user?.avatarUrl.isNullOrBlank()) {
                            val navCtx = androidx.compose.ui.platform.LocalContext.current
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(navCtx)
                                    .data(user?.avatarUrl)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = "Profile photo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            val initials = user?.name?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "?"
                            Text(initials, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(user?.name ?: "Aspirant", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(user?.email ?: (user?.mobile ?: ""), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.72f))
                        Spacer(Modifier.height(4.dp))
                        if (settingsViewModel.coinsConfig.config.collectAsState().value.enabled) {
                            Row(modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFB300).copy(0.2f))
                                .border(0.5.dp, Color(0xFFFFD54F).copy(0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🪙", fontSize = 11.sp)
                                Text("${user?.coins ?: 0} Coins", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // Language switch button in drawer header
                        LanguageSwitchButton()
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp)) {
                    // Grouped menu items with section headers
                    val groups = listOf(
                        "Study"   to menuItems.take(5),
                        "Explore" to menuItems.drop(5).take(4),
                        "Account" to menuItems.drop(9),
                    )
                    groups.forEachIndexed { gIdx, (groupLabel, groupItems) ->
                        if (gIdx > 0) Spacer(Modifier.height(4.dp))
                        Text(
                            groupLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.TextHint,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                        groupItems.forEach { (icon, label, route) ->
                            val isNotifItem = route == Screen.NotificationSettings.route
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClose(); navController.navigate(route) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                                }
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface,
                                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (isNotifItem && notifCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF5350)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (notifCount > 99) "99+" else "$notifCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Icon(Icons.Rounded.KeyboardArrowRight, null, tint = cs.outline, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        if (gIdx < groups.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = cs.outline, thickness = 0.5.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (scrollState.value < scrollState.maxValue) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(0f),
                                    Color.White
                                )
                            )
                        )) {
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = BpscColors.TextHint, modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center))
                    }
                }
                if (scrollState.maxValue > 0) {
                    val thumbOffset by remember { derivedStateOf { scrollState.value.toFloat() / scrollState.maxValue.toFloat() } }
                    Box(modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .background(cs.outline, RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .align(Alignment.TopStart)
                            .offset(y = with(LocalDensity.current) { (scrollState.value * 0.3f).toDp() })
                            .background(BpscColors.Primary.copy(0.4f), RoundedCornerShape(2.dp)))
                    }
                }
            }
            Column(modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(color = cs.outline); Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BPSCNotes", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                }
                Spacer(Modifier.height(8.dp))
                // Confirmation dialog before logging out — same dialog the
                // Settings/Profile page shows; the drawer used to log out
                // immediately on tap (QA 18-07 issue 2).
                var showLogoutConfirm by remember { mutableStateOf(false) }
                if (showLogoutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showLogoutConfirm = false },
                        containerColor   = cs.surface,
                        shape            = RoundedCornerShape(24.dp),
                        icon = {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFCE4EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Logout, null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                            }
                        },
                        title = {
                            Text(
                                str.logoutTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                        },
                        text = {
                            Text(
                                str.logoutMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick  = { showLogoutConfirm = false; onClose(); settingsViewModel.logOut() },
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text(str.drawerLogout, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutConfirm = false }) {
                                Text(str.cancel, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
                OutlinedButton(onClick = { showLogoutConfirm = true }, modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE74C3C)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))) {
                    Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(str.drawerLogout, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED HELPERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(emoji: String, message: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(BpscColors.PrimaryLight)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(emoji, fontSize = 20.sp)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun LargeQuickCard(title: String, subtitle: String, icon: ImageVector, gradient: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(modifier = modifier
        .height(118.dp)
        .clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradient, Offset(0f, 0f), Offset(200f, 200f)))
            .padding(16.dp)) {
            Box(modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp))
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                Box(modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Column { Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.75f)) }
            }
        }
    }
}

@Composable
private fun SmallQuickCard(title: String, icon: ImageVector, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(modifier = modifier
        .height(92.dp)
        .clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cs.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp)) }
            Text(title, style = MaterialTheme.typography.labelSmall, color = cs.onSurface, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
        }
    }
}
// ════════════════════════════════════════════════════════════
// ACTIVITY DETAIL FULL SCREEN
// Latest week first, inline day tap tooltip — full-screen page (not bottom sheet)
// ════════════════════════════════════════════════════════════
@Composable
private fun ActivityDetailFullScreen(
    data:       List<DayProgress>,
    quizMins:   Map<String, Int>,
    roomMins:   Map<String, Int>,
    caMins:     Map<String, Int>,
    lessonMins: Map<String, Int>,
    onBack:     () -> Unit
) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val cs  = MaterialTheme.colorScheme
    val dfmt = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val cal  = remember { java.util.Calendar.getInstance() }

    val dateKeys = remember(data) {
        (27 downTo 0).map { daysAgo ->
            cal.timeInMillis = System.currentTimeMillis() - daysAgo * 86_400_000L
            dfmt.format(cal.time)
        }
    }

    val colorQuiz   = Color(0xFF1565C0)
    val colorRoom   = Color(0xFF2E7D32)
    val colorCa     = Color(0xFFF57F17)
    val colorLesson = Color(0xFF6A1B9A)

    // Track which day the user tapped — null = no selection
    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top App Bar ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .padding(start = 8.dp, end = 8.dp, top = 38.dp, bottom =8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(str.dashStudyActivity, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(str.dashStudyActivitySub,
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                    Box(Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Legend ──────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(cs.background).padding(horizontal = 12.dp, vertical = 8.dp),
                    Arrangement.SpaceEvenly
                ) {
                    LegendDot(colorQuiz,   "Quizzes")
                    LegendDot(colorRoom,   "Study Room")
                    LegendDot(colorCa,     "Curr. Affairs")
                    LegendDot(colorLesson, "Lessons")
                }

                // ── Summary stats ────────────────────────────────────
                if (data.isNotEmpty()) {
                    val activeDays  = data.count { it.score > 0 }
                    val totalMins   = data.sumOf { it.score }
                    val totalQuiz   = quizMins.values.sum()
                    val totalRoom   = roomMins.values.sum()
                    val totalCa     = caMins.values.sum()
                    val totalLesson = lessonMins.values.sum()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(BpscColors.PrimaryLight.copy(0.3f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            Arrangement.SpaceEvenly
                        ) {
                            ActivityStat("📅", "$activeDays", "Active Days")
                            ActivityStat("⏱️",
                                if (totalMins >= 60) "${totalMins/60}h ${totalMins%60}m" else "${totalMins}m",
                                "Total (28d)")
                            ActivityStat("🏆", "${data.maxOfOrNull { it.score } ?: 0}m", "Best Day")
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                            BreakdownChip(colorQuiz,   "📝", if (totalQuiz >= 60) "${totalQuiz/60}h ${totalQuiz%60}m" else "${totalQuiz}m", "Quiz",    Modifier.weight(1f))
                            BreakdownChip(colorRoom,   "🏠", if (totalRoom >= 60) "${totalRoom/60}h ${totalRoom%60}m" else "${totalRoom}m", "Room",    Modifier.weight(1f))
                            BreakdownChip(colorCa,     "📰", if (totalCa >= 60) "${totalCa/60}h ${totalCa%60}m" else "${totalCa}m", "CA",      Modifier.weight(1f))
                            BreakdownChip(colorLesson, "📚", if (totalLesson >= 60) "${totalLesson/60}h ${totalLesson%60}m" else "${totalLesson}m", "Lessons", Modifier.weight(1f))
                        }
                    }
                }

                // ── Inline day tooltip (shows on bar tap, no sheet) ───
                if (selectedDayKey != null) {
                    val dk = selectedDayKey!!
                    val q  = quizMins[dk]   ?: 0
                    val r  = roomMins[dk]   ?: 0
                    val c  = caMins[dk]     ?: 0
                    val l  = lessonMins[dk] ?: 0
                    val total = q + r + c + l
                    val displayDate = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(sdf.parse(dk)!!)
                    } catch (_: Exception) { dk }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BpscColors.Primary.copy(0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(displayDate, style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = BpscColors.Primary)
                            Text(
                                if (total == 0) "No study activity"
                                else buildString {
                                    if (q > 0) append("Quiz ${q}m  ")
                                    if (r > 0) append("Room ${r}m  ")
                                    if (c > 0) append("CA ${c}m  ")
                                    if (l > 0) append("Lessons ${l}m")
                                }.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (q > 0) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorQuiz))
                            if (r > 0) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorRoom))
                            if (c > 0) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorCa))
                            if (l > 0) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorLesson))
                            Text(if (total > 0) "${total}m" else "–",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (total > 0) BpscColors.Primary else cs.onSurfaceVariant)
                            IconButton(onClick = { selectedDayKey = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Rounded.Close, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // ── 4-week stacked bar chart — LATEST FIRST ──────────
                if (data.isNotEmpty() && dateKeys.size == data.size) {
                    val maxTotal = data.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1
                    // Reverse: newest week at top
                    val weeks = data.zip(dateKeys).chunked(7).reversed()

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        weeks.forEachIndexed { reversedIdx, week ->
                            // reversedIdx 0 = this week (most recent), last = oldest
                            val weeksAgo = reversedIdx
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    if (weeksAgo == 0) "This week" else "$weeksAgo week${if (weeksAgo > 1) "s" else ""} ago",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (weeksAgo == 0) BpscColors.Primary else BpscColors.TextHint,
                                    fontWeight = if (weeksAgo == 0) FontWeight.Bold else FontWeight.Normal
                                )
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                                    week.forEach { (day, dateKey) ->
                                        val q = quizMins[dateKey]   ?: 0
                                        val r = roomMins[dateKey]   ?: 0
                                        val c = caMins[dateKey]     ?: 0
                                        val l = lessonMins[dateKey] ?: 0
                                        val total = day.score.coerceAtLeast(q + r + c + l)
                                        val isToday    = dateKey == dateKeys.last()
                                        val isSelected = dateKey == selectedDayKey

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                                ) {
                                                    selectedDayKey = if (isSelected) null else dateKey
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> BpscColors.Primary.copy(0.15f)
                                                            else       -> cs.onSurface.copy(0.06f)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                if (total > 0) {
                                                    val barH = (total.toFloat() / maxTotal).coerceIn(0.05f, 1f)
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .fillMaxHeight(barH)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        verticalArrangement = Arrangement.Bottom
                                                    ) {
                                                        listOf(l to colorLesson, c to colorCa, r to colorRoom, q to colorQuiz)
                                                            .forEach { (mins, color) ->
                                                                if (mins > 0) Box(
                                                                    Modifier.fillMaxWidth()
                                                                        .weight(mins.toFloat().coerceAtLeast(0.1f))
                                                                        .background(color)
                                                                )
                                                            }
                                                    }
                                                }
                                                if (isToday) Box(Modifier.fillMaxSize()
                                                    .border(2.dp, BpscColors.Accent, RoundedCornerShape(6.dp)))
                                                if (isSelected) Box(Modifier.fillMaxSize()
                                                    .border(2.dp, BpscColors.Primary, RoundedCornerShape(6.dp)))
                                            }
                                            Text(
                                                day.day.take(1),
                                                style      = MaterialTheme.typography.labelSmall,
                                                color      = when {
                                                    isSelected -> BpscColors.Primary
                                                    isToday    -> BpscColors.Accent
                                                    else       -> BpscColors.TextHint
                                                },
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end scrollable Column
    } // end outer Column
} // end outer Box (ActivityDetailFullScreen)

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun BreakdownChip(color: Color, emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.1f)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun ActivityStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 18.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BpscColors.Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
}