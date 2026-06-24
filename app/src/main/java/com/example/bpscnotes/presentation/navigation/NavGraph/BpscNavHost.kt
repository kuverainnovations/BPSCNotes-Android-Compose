package com.example.bpscnotes.presentation.navigation.NavGraph

import com.example.bpscnotes.core.ui.AppLoader
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import dagger.hilt.android.EntryPointAccessors
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.presentation.activerecall.ActiveRecallScreen
import com.example.bpscnotes.presentation.auth.examsetup.ExamSetupScreen
import com.example.bpscnotes.presentation.auth.login.LoginScreen
import com.example.bpscnotes.presentation.auth.onboarding.OnboardingScreen
import com.example.bpscnotes.presentation.auth.otp.OtpScreen
import com.example.bpscnotes.presentation.auth.register.RegisterScreen
import com.example.bpscnotes.core.language.LanguageSelectionScreen
import com.example.bpscnotes.presentation.auth.splash.SplashScreen
import com.example.bpscnotes.presentation.course.CourseDetailScreen
import com.example.bpscnotes.presentation.currentaffairs.CurrentAffairsScreen
import com.example.bpscnotes.core.notifications.FcmDebugScreen
import com.example.bpscnotes.presentation.currentaffairs.CaMcqQuizScreen
import com.example.bpscnotes.presentation.currentaffairs.CaArticleDetailScreen
import com.example.bpscnotes.presentation.payment.SubscriptionPaymentScreen
import com.example.bpscnotes.presentation.payment.CoursePaymentScreen
import com.example.bpscnotes.presentation.dashboard.DailyTargetsScreen
import com.example.bpscnotes.presentation.dashboard.DashboardScreen
import com.example.bpscnotes.presentation.elibrary.ELibraryScreen
import com.example.bpscnotes.presentation.jobvacancies.JobVacanciesScreen
import com.example.bpscnotes.presentation.mocktests.MockTestsScreen
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.navigation.MainShell.MainShell
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.nofification.NotificationSettingsScreen
import com.example.bpscnotes.presentation.placeholders.DownloadsScreen
import com.example.bpscnotes.presentation.profile.EditProfileScreen
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import com.example.bpscnotes.presentation.placeholders.NotesReaderScreen
import com.example.bpscnotes.presentation.placeholders.SubscriptionScreen
import com.example.bpscnotes.presentation.profile.ProfileScreen
import com.example.bpscnotes.presentation.quiz.DailyQuizScreen
import com.example.bpscnotes.presentation.quiz.QuizDetailScreen
import com.example.bpscnotes.presentation.quiz.QuizListScreen
import com.example.bpscnotes.presentation.quiz.QuizPlayScreen
import com.example.bpscnotes.presentation.quiz.QuizViewModel
import com.example.bpscnotes.presentation.quiz.TopicQuizScreen
import com.example.bpscnotes.presentation.readingrooms.ReadingRoomsScreen
import com.example.bpscnotes.presentation.rooms.RoomsHubScreen
import com.example.bpscnotes.presentation.rooms.LeaderboardScreen
import com.example.bpscnotes.presentation.course.LessonViewerScreen
import com.example.bpscnotes.presentation.liveclasses.LiveClassViewerScreen
import com.example.bpscnotes.presentation.rooms.StudyFocusScreen
import com.example.bpscnotes.presentation.rooms.AchievementsScreen
import com.example.bpscnotes.presentation.rooms.ChallengesScreen
import com.example.bpscnotes.presentation.rooms.StudySessionViewModel
import com.example.bpscnotes.presentation.rooms.TierRoomsViewModel
import com.example.bpscnotes.presentation.settings.SettingsScreen
import com.example.bpscnotes.presentation.studymaterials.StudyMaterialsScreen
import com.example.bpscnotes.presentation.studymaterials.MaterialChatScreen
import com.example.bpscnotes.presentation.studymaterials.ChatInboxScreen
import com.example.bpscnotes.presentation.studymaterials.PdfViewerScreen
import com.example.bpscnotes.presentation.studymaterials.VideoPlayerScreen
import com.example.bpscnotes.presentation.studymaterials.ImageViewerScreen
import com.example.bpscnotes.presentation.wallet.CoinWalletScreen
import com.example.bpscnotes.presentation.auth.mpin.MpinLoginScreen
import com.example.bpscnotes.presentation.auth.mpin.CreateMpinScreen
import com.example.bpscnotes.presentation.auth.mpin.ResetMpinScreen
import com.example.bpscnotes.presentation.settings.ChangeMpinScreen

@Composable
fun BpscNavHost(navController: NavHostController, adManager: AdManager,) {
    val context = LocalContext.current

    // ── Session expired (401) → redirect to Login ───────────────────────
    // AuthInterceptor fires this whenever the server returns 401.
    // Handles mid-session expiry without each screen needing its own check.
    val authEventBus = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.example.bpscnotes.core.network.AuthEventBusEntryPoint::class.java
        ).authEventBus()
    }
    LaunchedEffect(Unit) {
        authEventBus.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // ── Root-level safety net ──────────────────────────────────────────────
    // Registered FIRST = LOWEST priority. Only fires when every inner
    // BackHandler (MainShell, NavHost internal) is inactive — i.e. during
    // the brief window of a rapid double-tap while MainShell is transitioning.
    //   • Something to pop  → pop it (normal navigation, no black screen)
    //   • Nothing to pop    → minimise the app (move to background)
    // ── Root safety-net BackHandler ──────────────────────────────────────
    // Root safety net — ONLY fires on terminal/entry screens.
    // For all other screens, NavHost's own back handler pops normally.
    // This closes the brief race window during a transition where
    // MainShell's BackHandler hasn't registered yet — preventing a
    // spurious pop that empties the stack and causes a white screen.
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentTopRoute = currentEntry?.destination?.route
    val isAtRoot = currentTopRoute == Screen.Main.route
            || currentTopRoute == Screen.Splash.route
            || currentTopRoute == Screen.Login.route
            || currentTopRoute == Screen.LanguageSelection.route
            || currentTopRoute == Screen.Onboarding.route
            || currentTopRoute == Screen.ExamSetup.route

    BackHandler(enabled = isAtRoot) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val cs = MaterialTheme.colorScheme

    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        NavHost(
            navController         = navController,
            startDestination      = Screen.Splash.route,
            // Disable all transitions — eliminates white flash between screens
            // The animation gap between entering/exiting screens was the white screen source
            enterTransition       = { EnterTransition.None },
            exitTransition        = { ExitTransition.None },
            popEnterTransition    = { EnterTransition.None },
            popExitTransition     = { ExitTransition.None }
        ) {
            // ── Auth ─────────────────────────────────────────────────
            composable(Screen.Splash.route)            { SplashScreen(navController) }
            composable(Screen.LanguageSelection.route) { LanguageSelectionScreen(navController) }
            composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
            composable(Screen.Login.route)      { LoginScreen(navController) }



            composable(
                Screen.Otp.route,
                arguments = listOf(
                    navArgument("mobile")  { type = NavType.StringType },
                    navArgument("context") { type = NavType.StringType; defaultValue = "registration" }
                )
            ) { back ->
                val mobile = java.net.URLDecoder.decode(back.arguments?.getString("mobile") ?: "", "UTF-8")
                val ctx    = java.net.URLDecoder.decode(back.arguments?.getString("context") ?: "registration", "UTF-8")
                OtpScreen(
                    navController = navController,
                    mobile        = mobile,
                    otpContext    = ctx
                )
            }

            // Register screen — reached when isNewUser == true after OTP verify
            composable(
                Screen.Register.route,
                arguments = listOf(navArgument("tempToken") { type = NavType.StringType })
            ) {
                val raw       = it.arguments?.getString("tempToken") ?: ""
                val tempToken = java.net.URLDecoder.decode(raw, "UTF-8")
                RegisterScreen(navController = navController, tempToken = tempToken)
            }

            // ── NEW: Exam Setup flow ──────────────────────────────────
            // Reached after: Register success OR from Splash when examSetupDone=false
            composable(Screen.ExamSetup.route) {
                ExamSetupScreen(navController = navController)
            }
            // ── Main shell ────────────────────────────────────────────
            composable(Screen.Main.route) { backStackEntry ->
                // Scope the session ViewModel to THIS entry (Screen.Main) so every child
                // composable (StudyFocusScreen, MainShell PIP) shares the SAME instance.
                val sessionVM: com.example.bpscnotes.presentation.rooms.StudySessionViewModel =
                    hiltViewModel(backStackEntry)
                val tiersVM: com.example.bpscnotes.presentation.rooms.TierRoomsViewModel =
                    hiltViewModel(backStackEntry)
                MainShell(
                    rootNavController = navController,
                    adManager         = adManager,
                    sessionViewModel  = sessionVM,
                    tiersViewModel    = tiersVM
                )
            }

            // ── Real screens ─────────────────────────────────────────
            composable(Screen.DailyTargets.route)   { DailyTargetsScreen(navController) }
            composable(Screen.CurrentAffairs.route) { CurrentAffairsScreen(navController, adManager = adManager) }

            // ── Payment screens ──────────────────────────────────────
            composable(Screen.Payment.route) {
                SubscriptionPaymentScreen(navController)
            }

            // FCM Debug screen — only available in debug builds
            val isDebug = (android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE and
                    context.applicationInfo.flags) != 0
            if (isDebug) {
                composable("fcm_debug") {
                    FcmDebugScreen(navController)
                }
            }

            composable(
                route     = "course_payment/{courseId}/{courseTitle}/{price}/{sessionId}",
                arguments = listOf(
                    navArgument("courseId")    { type = NavType.StringType },
                    navArgument("courseTitle") { type = NavType.StringType },
                    navArgument("price")       { type = NavType.IntType    },
                    navArgument("sessionId")   { type = NavType.StringType }
                )
            ) { back ->
                val courseId       = java.net.URLDecoder.decode(back.arguments?.getString("courseId") ?: return@composable, "UTF-8")
                val courseTitle    = java.net.URLDecoder.decode(back.arguments?.getString("courseTitle") ?: "", "UTF-8")
                val price          = back.arguments?.getInt("price") ?: 0
                val paymentSession = java.net.URLDecoder.decode(back.arguments?.getString("sessionId") ?: "none", "UTF-8").takeIf { it != "none" }
                CoursePaymentScreen(navController, courseId, courseTitle, price, paymentSession)
            }

            composable(
                route     = "ca_mcq_quiz/{affairId}",
                arguments = listOf(navArgument("affairId") { type = NavType.StringType })
            ) { backStackEntry ->
                val affairId = backStackEntry.arguments?.getString("affairId") ?: return@composable
                CaMcqQuizScreen(navController = navController, affairId = affairId)
            }

            composable(
                route     = "ca_article_detail/{affairId}",
                arguments = listOf(navArgument("affairId") { type = NavType.StringType })
            ) { backStackEntry ->
                val affairId = backStackEntry.arguments?.getString("affairId") ?: return@composable
                CaArticleDetailScreen(navController = navController, affairId = affairId)
            }

            composable(
                Screen.DailyQuiz.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    try { navController.getBackStackEntry(Screen.Main.route) }
                    catch (_: Exception) { backStackEntry }
                }

                val viewModel: QuizViewModel = hiltViewModel(parentEntry)

                DailyQuizScreen(
                    navController = navController,
                    date = backStackEntry.arguments?.getString("date") ?: "",
                    viewModel = viewModel
                )
            }
            /* composable(
                 Screen.TopicQuiz.route,
                 arguments = listOf(
                     navArgument("subject")    { type = NavType.StringType },
                     navArgument("topicTitle") { type = NavType.StringType }
                 )
             ) {
                 TopicQuizScreen(
                     navController = navController,
                     subject       = java.net.URLDecoder.decode(it.arguments?.getString("subject") ?: "", "UTF-8"),
                     topicTitle    = java.net.URLDecoder.decode(it.arguments?.getString("topicTitle") ?: "", "UTF-8")
                 )
             }*/

            composable(
                Screen.TopicQuiz.route,
                arguments = listOf(
                    navArgument("subject") { type = NavType.StringType },
                    navArgument("topicTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val subject = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("subject") ?: "",
                    "UTF-8"
                )

                val topicTitle = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("topicTitle") ?: "",
                    "UTF-8"
                )

                TopicQuizScreen(
                    navController = navController,
                    subject = subject,
                    topicTitle = topicTitle
                )
            }

            // ✅ Quiz List Screen
            composable(Screen.QuizList.route) {
                QuizListScreen(navController)
            }

// ✅ Quiz Detail Screen
            composable(
                route = Screen.QuizDetail.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId") ?: return@composable

                QuizDetailScreen(
                    quizId = quizId,
                    navController = navController
                )
            }

// ✅ Quiz Player Screen
            composable(
                route = Screen.QuizPlayer.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId") ?: return@composable
                QuizPlayScreen(quizId = quizId, navController = navController, adManager = adManager)
            }

            composable(Screen.ActiveRecall.route)  { ActiveRecallScreen(navController, adManager = adManager) }
            composable(Screen.MockTests.route)     { MockTestsScreen(navController, adManager = adManager) }
            composable(Screen.JobVacancies.route)  { JobVacanciesScreen(navController, adManager = adManager) }
            // ── Tier Room System (Phase 1) ──────────────────────────
            composable(Screen.RoomsHub.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    try { navController.getBackStackEntry(Screen.Main.route) }
                    catch (_: Exception) { backStackEntry }
                }
                val sessionVM: StudySessionViewModel = hiltViewModel(parentEntry)
                val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)
                RoomsHubScreen(
                    navController = navController,
                    sessionViewModel = sessionVM,
                    tiersViewModel = tiersVM
                )
            }

            composable(Screen.StudyFocus.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    try { navController.getBackStackEntry(Screen.Main.route) }
                    catch (_: Exception) { backStackEntry }
                }
                val sessionVM: StudySessionViewModel = hiltViewModel(parentEntry)
                val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)

                StudyFocusScreen(
                    navController  = navController,
                    viewModel      = sessionVM,
                    tiersViewModel = tiersVM,
                    adManager      = adManager
                )
            }


            composable(Screen.StudyMaterials.route)   { StudyMaterialsScreen(navController, adManager = adManager) }
            composable(
                route     = Screen.StudyMaterialsFiltered.route,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { back ->
                StudyMaterialsScreen(
                    navController  = navController,
                    adManager      = adManager,
                    initialTypeKey = back.arguments?.getString("type")
                )
            }

            // Phase 5: Chat with uploader for a purchased material
            composable(
                route = Screen.MaterialChat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { back ->
                val chatId = back.arguments?.getString("chatId") ?: ""
                MaterialChatScreen(navController = navController, chatId = chatId)
            }

            // Phase 5: Inbox of all chat threads
            composable(Screen.ChatInbox.route) { ChatInboxScreen(navController = navController) }


            // PDF Viewer — custom in-app renderer with page locking
            composable(
                route     = Screen.PdfViewer.route,
                arguments = listOf(
                    navArgument("fileUrl")     { type = NavType.StringType },
                    navArgument("title")       { type = NavType.StringType },
                    navArgument("freePages")   { type = NavType.IntType; defaultValue = 3 },
                    navArgument("isPurchased") { type = NavType.BoolType; defaultValue = false },
                )
            ) { back ->
                val fileUrl     = back.arguments?.getString("fileUrl")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                val title       = back.arguments?.getString("title")?.let  { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Document"
                val freePages   = back.arguments?.getInt("freePages")  ?: 3
                val isPurchased = back.arguments?.getBoolean("isPurchased") ?: false
                PdfViewerScreen(
                    fileUrl       = fileUrl,
                    title         = title,
                    freePages     = freePages,
                    isPurchased   = isPurchased,
                    navController = navController,
                    adManager     = adManager,
                    authToken     = ""
                )
            }

            // In-app Video Player with interstitial ads
            composable(
                route     = Screen.VideoPlayer.route,
                arguments = listOf(
                    navArgument("videoUrl") { type = NavType.StringType },
                    navArgument("title")    { type = NavType.StringType },
                )
            ) { back ->
                val videoUrl = back.arguments?.getString("videoUrl")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                val title    = back.arguments?.getString("title")?.let    { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Video"
                VideoPlayerScreen(
                    videoUrl      = videoUrl,
                    title         = title,
                    navController = navController,
                    adManager     = adManager
                )
            }

            // In-app Image Viewer with pinch-zoom and interstitial ads
            composable(
                route     = Screen.ImageViewer.route,
                arguments = listOf(
                    navArgument("imageUrl") { type = NavType.StringType },
                    navArgument("title")    { type = NavType.StringType },
                )
            ) { back ->
                val imageUrl = back.arguments?.getString("imageUrl")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                val title    = back.arguments?.getString("title")?.let    { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Image"
                ImageViewerScreen(
                    imageUrl      = imageUrl,
                    title         = title,
                    navController = navController,
                    adManager     = adManager
                )
            }

            composable(Screen.Achievements.route)     { AchievementsScreen(navController) }
            composable(Screen.WeeklyChallenges.route) { ChallengesScreen(navController) }

            composable(Screen.Leaderboard.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    try { navController.getBackStackEntry(Screen.Main.route) }
                    catch (_: Exception) { backStackEntry }
                }
                val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)
                LeaderboardScreen(navController = navController, viewModel = tiersVM)
            }
            composable(Screen.ReadingRooms.route)  { ReadingRoomsScreen(navController) }
            composable(Screen.MyLearning.route)         { MyLearningScreen(navController, startTab = 0, fromScreen = "nav-host") }
            composable(Screen.MyLearningCourses.route)  { MyLearningScreen(navController, fromScreen = "nav-host") }

            // ── Placeholders ─────────────────────────────────────────
            composable(Screen.ELibrary.route)      { ELibraryScreen(navController) }
            composable(Screen.Profile.route)       { ProfileScreen(navController) }
            composable(Screen.CoinWallet.route)    { CoinWalletScreen(navController, adManager = adManager) }
            composable(Screen.Subscription.route)  { SubscriptionScreen(navController) }
            composable(Screen.EditProfile.route)    { EditProfileScreen(navController) }
            composable(Screen.Downloads.route)     { DownloadsScreen(navController) }
            composable(Screen.Settings.route)      { SettingsScreen(navController) }
            composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }

            // ── MPIN Auth ────────────────────────────────────────────
            composable(
                Screen.MpinLogin.route,
                arguments = listOf(navArgument("mobile") { type = NavType.StringType })
            ) { back ->
                val mobile = java.net.URLDecoder.decode(
                    back.arguments?.getString("mobile") ?: "", "UTF-8"
                )
                MpinLoginScreen(navController = navController, mobile = mobile)
            }

            composable(Screen.CreateMpin.route) {
                CreateMpinScreen(navController = navController)
            }

            composable(
                Screen.ForgotMpin.route,
                arguments = listOf(navArgument("mobile") { type = NavType.StringType })
            ) { back ->
                val mobile = java.net.URLDecoder.decode(
                    back.arguments?.getString("mobile") ?: "", "UTF-8"
                )
                ForgotMpinRedirectScreen(navController = navController, mobile = mobile)
            }

            composable(
                Screen.ResetMpin.route,
                arguments = listOf(
                    navArgument("mobile") { type = NavType.StringType },
                    navArgument("otp")    { type = NavType.StringType }
                )
            ) { back ->
                val mobile = java.net.URLDecoder.decode(back.arguments?.getString("mobile") ?: "", "UTF-8")
                val otp    = java.net.URLDecoder.decode(back.arguments?.getString("otp")    ?: "", "UTF-8")
                ResetMpinScreen(navController = navController, mobile = mobile, otp = otp)
            }

            composable(Screen.ChangeMpin.route) {
                ChangeMpinScreen(navController = navController)
            }

            // Live Class Viewer — in-app WebView
            composable(
                Screen.LiveClassViewer.route,
                arguments = listOf(
                    navArgument("url")          { type = NavType.StringType },
                    navArgument("title")        { type = NavType.StringType },
                    navArgument("instructor")   { type = NavType.StringType },
                    navArgument("durationMins") { type = NavType.IntType }
                )
            ) {
                LiveClassViewerScreen(
                    navController = navController,
                    meetingUrl    = java.net.URLDecoder.decode(it.arguments?.getString("url") ?: "", "UTF-8"),
                    classTitle    = java.net.URLDecoder.decode(it.arguments?.getString("title") ?: "", "UTF-8"),
                    instructor    = java.net.URLDecoder.decode(it.arguments?.getString("instructor") ?: "", "UTF-8"),
                    durationMins  = it.arguments?.getInt("durationMins") ?: 60
                )
            }

            composable(
                Screen.CourseDetail.route,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { CourseDetailScreen(navController, it.arguments?.getString("courseId") ?: "") }

            composable(
                Screen.NotesReader.route,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { NotesReaderScreen(navController, it.arguments?.getString("noteId") ?: "") }

            // ── Lesson Viewer (PDF / Video / Quiz / Live) ─────────
            composable(
                Screen.LessonViewer.route,
                arguments = listOf(
                    navArgument("courseId")  { type = NavType.StringType },
                    navArgument("lessonId")  { type = NavType.StringType }
                )
            ) { backStack ->
                LessonViewerScreen(
                    nav      = navController,
                    courseId = backStack.arguments?.getString("courseId") ?: "",
                    lessonId = backStack.arguments?.getString("lessonId") ?: ""
                )
            }
        }
    }
}

/**
 * Lightweight composable that immediately navigates to OtpScreen with forgot_mpin context.
 * Exists as a named @Composable so LaunchedEffect + Modifier work correctly.
 */
@Composable
private fun ForgotMpinRedirectScreen(
    navController: androidx.navigation.NavHostController,
    mobile: String
) {
    LaunchedEffect(Unit) {
        navController.navigate(
            Screen.Otp.createRouteWithContext(mobile, "forgot_mpin")
        ) {
            popUpTo(Screen.ForgotMpin.createRoute(mobile)) { inclusive = true }
        }
    }
    AppLoader()
}