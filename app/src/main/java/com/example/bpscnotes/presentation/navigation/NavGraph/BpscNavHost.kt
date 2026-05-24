package com.example.bpscnotes.presentation.navigation.NavGraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.presentation.activerecall.ActiveRecallScreen
import com.example.bpscnotes.presentation.auth.examsetup.ExamSetupScreen
import com.example.bpscnotes.presentation.auth.login.LoginScreen
import com.example.bpscnotes.presentation.auth.onboarding.OnboardingScreen
import com.example.bpscnotes.presentation.auth.otp.OtpScreen
import com.example.bpscnotes.presentation.auth.register.RegisterScreen
import com.example.bpscnotes.presentation.auth.splash.SplashScreen
import com.example.bpscnotes.presentation.course.CourseDetailScreen
import com.example.bpscnotes.presentation.currentaffairs.CurrentAffairsScreen
import com.example.bpscnotes.presentation.dashboard.DailyTargetsScreen
import com.example.bpscnotes.presentation.dashboard.DashboardScreen
import com.example.bpscnotes.presentation.elibrary.ELibraryScreen
import com.example.bpscnotes.presentation.jobvacancies.JobVacanciesScreen
import com.example.bpscnotes.presentation.mocktests.MockTestsScreen
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.navigation.MainShell.MainShell
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
import com.example.bpscnotes.presentation.course.LessonViewerScreen
import com.example.bpscnotes.presentation.rooms.StudyFocusScreen
import com.example.bpscnotes.presentation.rooms.AchievementsScreen
import com.example.bpscnotes.presentation.rooms.ChallengesScreen
import com.example.bpscnotes.presentation.rooms.StudySessionViewModel
import com.example.bpscnotes.presentation.rooms.TierRoomsViewModel
import com.example.bpscnotes.presentation.settings.SettingsScreen
import com.example.bpscnotes.presentation.studymaterials.StudyMaterialsScreen
import com.example.bpscnotes.presentation.studymaterials.PdfViewerScreen
import com.example.bpscnotes.presentation.wallet.CoinWalletScreen

@Composable
fun BpscNavHost(navController: NavHostController, adManager: AdManager,) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Auth ─────────────────────────────────────────────────
        composable(Screen.Splash.route)     { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Login.route)      { LoginScreen(navController) }



        composable(
            Screen.Otp.route,
            arguments = listOf(navArgument("mobile") { type = NavType.StringType })
        ) {
            OtpScreen(
                navController = navController,
                mobile        = it.arguments?.getString("mobile") ?: ""
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
        composable(Screen.Main.route) {
            MainShell(rootNavController = navController, adManager =adManager)
        }

        // ── Real screens ─────────────────────────────────────────
        composable(Screen.DailyTargets.route)   { DailyTargetsScreen(navController) }
        composable(Screen.CurrentAffairs.route) { CurrentAffairsScreen(navController) }

        composable(
            Screen.DailyQuiz.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Main.route)
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

        composable(Screen.ActiveRecall.route)  { ActiveRecallScreen(navController) }
        composable(Screen.MockTests.route)     { MockTestsScreen(navController) }
        composable(Screen.JobVacancies.route)  { JobVacanciesScreen(navController, adManager = adManager) }
        // ── Tier Room System (Phase 1) ──────────────────────────
        composable(Screen.RoomsHub.route) { backStackEntry ->

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Main.route)
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
                navController.getBackStackEntry(Screen.Main.route)
            }

            val sessionVM: StudySessionViewModel = hiltViewModel(parentEntry)
            val tiersVM: TierRoomsViewModel = hiltViewModel(parentEntry)

            StudyFocusScreen(
                navController = navController,
                viewModel = sessionVM,
                tiersViewModel = tiersVM,
                adManager = adManager
            )
        }


        composable(Screen.StudyMaterials.route)   { StudyMaterialsScreen(navController) }

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
                authToken     = ""//tokenStore.getToken() ?: ""
            )
        }

        composable(Screen.Achievements.route)     { AchievementsScreen(navController) }
        composable(Screen.WeeklyChallenges.route) { ChallengesScreen(navController) }

        composable(Screen.ReadingRooms.route)  { ReadingRoomsScreen(navController) }
        composable(Screen.MyLearning.route)    { MyLearningScreen(navController, fromScreen = "nav-host") }

        // ── Placeholders ─────────────────────────────────────────
        composable(Screen.ELibrary.route)      { ELibraryScreen(navController) }
        composable(Screen.Profile.route)       { ProfileScreen(navController) }
        composable(Screen.CoinWallet.route)    { CoinWalletScreen(navController, adManager = adManager) }
        composable(Screen.Subscription.route)  { SubscriptionScreen(navController) }
        composable(Screen.EditProfile.route)    { EditProfileScreen(navController) }
        composable(Screen.Downloads.route)     { DownloadsScreen(navController) }
        composable(Screen.Settings.route)      { SettingsScreen(navController) }
        composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }

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