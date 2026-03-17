package com.example.bpscnotes.presentation.navigation.Routes

sealed class Screen(val route: String) {
    // Auth
    object Splash       : Screen("splash")
    object Onboarding   : Screen("onboarding")
    object Login        : Screen("login")
    object Otp          : Screen("otp/{mobile}") {
        fun createRoute(mobile: String) = "otp/$mobile"
    }

    // Main shell
    object Main         : Screen("main")

    // Bottom nav tabs
    object Dashboard    : Screen("dashboard")
    object MyLearning   : Screen("my_learning")
    object ELibrary     : Screen("e_library")
    object Profile      : Screen("profile")

    // Dashboard children
    object DailyTargets     : Screen("daily_targets")
    object CurrentAffairs   : Screen("current_affairs")
    object DailyQuiz        : Screen("daily_quiz/{date}") {
        fun createRoute(date: String) = "daily_quiz/$date"
    }

    object TopicQuiz    : Screen("topic_quiz/{subject}/{topicTitle}") {
        fun createRoute(subject: String, topicTitle: String) =
            "topic_quiz/${subject.encodeUrl()}/${topicTitle.encodeUrl()}"
    }
    object ActiveRecall     : Screen("active_recall")
    object MockTests        : Screen("mock_tests")
    object JobVacancies     : Screen("job_vacancies")

    // Study content
    object CourseDetail   : Screen("course/{courseId}") {
        fun createRoute(id: String) = "course/$id"
    }
    object NotesReader    : Screen("notes/{noteId}") {
        fun createRoute(id: String) = "notes/$id"
    }
    object Downloads      : Screen("downloads")

    // Group study
    object ReadingRooms   : Screen("reading_rooms")
    object ReadingRoomActive : Screen("reading_room/{roomId}") {
        fun createRoute(id: String) = "reading_room/$id"
    }

    // Wallet / payments
    object CoinWallet     : Screen("wallet")
    object Subscription   : Screen("subscription")

    // Settings
    object NotificationSettings : Screen("notification_settings")
    object Settings             : Screen("settings")
}

fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
