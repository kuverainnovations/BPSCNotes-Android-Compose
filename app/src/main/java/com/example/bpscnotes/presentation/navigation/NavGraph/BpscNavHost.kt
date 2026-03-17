package com.example.bpscnotes.presentation.navigation.NavGraph

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.bpscnotes.presentation.auth.login.LoginScreen
import com.example.bpscnotes.presentation.auth.onboarding.OnboardingScreen
import com.example.bpscnotes.presentation.auth.otp.OtpScreen
import com.example.bpscnotes.presentation.auth.splash.SplashScreen
import com.example.bpscnotes.presentation.navigation.MainShell.MainShell
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.dashboard.DailyTargetsScreen
import com.example.bpscnotes.presentation.currentaffairs.CurrentAffairsScreen
import com.example.bpscnotes.presentation.quiz.DailyQuizScreen
import com.example.bpscnotes.presentation.quiz.TopicQuizScreen
import com.example.bpscnotes.presentation.activerecall.ActiveRecallScreen
import com.example.bpscnotes.presentation.wallet.CoinWalletScreen
import com.example.bpscnotes.presentation.mocktests.MockTestsScreen
import com.example.bpscnotes.presentation.jobvacancies.JobVacanciesScreen
import com.example.bpscnotes.presentation.readingrooms.ReadingRoomsScreen
import com.example.bpscnotes.presentation.mylearning.MyLearningScreen
import com.example.bpscnotes.presentation.profile.ProfileScreen
import com.example.bpscnotes.presentation.placeholders.SubscriptionScreen
import com.example.bpscnotes.presentation.placeholders.DownloadsScreen
import com.example.bpscnotes.presentation.placeholders.CourseDetailScreen
import com.example.bpscnotes.presentation.placeholders.NotesReaderScreen
import com.example.bpscnotes.presentation.elibrary.ELibraryScreen
import com.example.bpscnotes.presentation.nofification.NotificationSettingsScreen
import com.example.bpscnotes.presentation.settings.SettingsScreen


@Composable
fun BpscNavHost(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route)     { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Login.route)      { LoginScreen(navController) }
        composable(
            Screen.Otp.route,
            arguments = listOf(navArgument("mobile") { type = NavType.StringType })
        ) {
            OtpScreen(navController = navController, mobile = it.arguments?.getString("mobile") ?: "")
        }
        composable(Screen.Main.route) {
            MainShell(rootNavController = navController)
        }

        // ── Real screens ─────────────────────────────────────
        composable(Screen.DailyTargets.route)   { DailyTargetsScreen(navController) }
        composable(Screen.CurrentAffairs.route) { CurrentAffairsScreen(navController) }
        composable(
            Screen.DailyQuiz.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { DailyQuizScreen(navController, it.arguments?.getString("date") ?: "") }
        composable(
            Screen.TopicQuiz.route,
            arguments = listOf(
                navArgument("subject")    { type = NavType.StringType },
                navArgument("topicTitle") { type = NavType.StringType }
            )
        ) {
            val subject    = it.arguments?.getString("subject")    ?: ""
            val topicTitle = it.arguments?.getString("topicTitle") ?: ""
            TopicQuizScreen(
                navController = navController,
                subject       = java.net.URLDecoder.decode(subject,    "UTF-8"),
                topicTitle    = java.net.URLDecoder.decode(topicTitle, "UTF-8")
            )
        }
        composable(Screen.ActiveRecall.route)   { ActiveRecallScreen(navController) }
        composable(Screen.MockTests.route)      { MockTestsScreen(navController) }
        composable(Screen.JobVacancies.route)   { JobVacanciesScreen(navController) }
        composable(Screen.ReadingRooms.route)   { ReadingRoomsScreen(navController) }
        composable(Screen.MyLearning.route)     { MyLearningScreen(navController) }

        // ── Placeholders ─────────────────────────────────────
        composable(Screen.ELibrary.route)       { ELibraryScreen(navController) }
        composable(Screen.Profile.route)        { ProfileScreen(navController) }
        composable(Screen.CoinWallet.route)     { CoinWalletScreen(navController) }
        composable(Screen.Subscription.route)   { SubscriptionScreen(navController) }
        composable(Screen.Downloads.route)      { DownloadsScreen(navController) }
        composable(Screen.Settings.route)       { SettingsScreen(navController) }
        composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController) }
        composable(
            Screen.CourseDetail.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { CourseDetailScreen(navController, it.arguments?.getString("courseId") ?: "") }
        composable(
            Screen.NotesReader.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { NotesReaderScreen(navController, it.arguments?.getString("noteId") ?: "") }
    }
}