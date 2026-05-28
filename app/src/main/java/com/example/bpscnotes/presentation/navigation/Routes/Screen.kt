package com.example.bpscnotes.presentation.navigation.Routes

sealed class Screen(val route: String) {
    // Auth
    object Splash              : Screen("splash")
    object LanguageSelection   : Screen("language_selection")
    object Onboarding          : Screen("onboarding")
    object Login        : Screen("login")
    object Otp          : Screen("otp/{mobile}") {
        fun createRoute(mobile: String) = "otp/$mobile"
    }
    object Register     : Screen("register/{tempToken}") {
        fun createRoute(tempToken: String) = "register/${tempToken.encodeUrl()}"
    }
    // NEW: exam setup after registration
    object ExamSetup  : Screen("exam_setup")

    // Main shell
    object Main         : Screen("main")

    // Bottom nav tabs
    object Dashboard    : Screen("dashboard")
    object MyLearning   : Screen("my_learning")
    object ELibrary     : Screen("rooms_hub")
    object Profile      : Screen("profile")

    // Dashboard children
    object DailyTargets   : Screen("daily_targets")
    object CurrentAffairs   : Screen("current_affairs")
    object Payment          : Screen("payment")
    data class CoursePayment(val placeholder: String = "") : Screen(
        "course_payment/{courseId}/{courseTitle}/{price}/{orderId}/{keyId}"
    ) {
        fun createRoute(courseId: String, courseTitle: String, price: Int,
                        orderId: String?, keyId: String?) =
            "course_payment/$courseId/${courseTitle.take(50)}/$price/${orderId ?: "none"}/${keyId ?: "none"}"
    }
    data class CaMcqQuiz(val affairId: String = "") : Screen("ca_mcq_quiz/{affairId}") {
        fun createRoute(id: String) = "ca_mcq_quiz/$id"
    }
    object DailyQuiz      : Screen("daily_quiz/{date}") {
        fun createRoute(date: String) = "daily_quiz/$date"
    }

    // Quiz module (NEW — PRODUCTION FLOW)

    object QuizList : Screen("quiz_list")

    object QuizDetail : Screen("quiz_detail/{quizId}") {
        fun createRoute(quizId: String) = "quiz_detail/$quizId"
    }

    object QuizPlayer : Screen("quiz_player/{quizId}") {
        fun createRoute(quizId: String) = "quiz_player/$quizId"
    }

    object TopicQuiz      : Screen("topic_quiz/{subject}/{topicTitle}") {
        fun createRoute(subject: String, topicTitle: String) =
            "topic_quiz/${subject.encodeUrl()}/${topicTitle.encodeUrl()}"
    }
    object ActiveRecall   : Screen("active_recall")
    object MockTests      : Screen("mock_tests")
    object JobVacancies   : Screen("job_vacancies")

    // Study content
    object CourseDetail  : Screen("course/{courseId}") {
        fun createRoute(id: String) = "course/$id"
    }
    object NotesReader   : Screen("notes/{noteId}") {
        fun createRoute(id: String) = "notes/$id"
    }

    // Full lesson viewer — handles PDF, video, quiz, live
    object LessonViewer  : Screen("lesson/{courseId}/{lessonId}") {
        fun createRoute(courseId: String, lessonId: String) = "lesson/$courseId/$lessonId"
    }
    object Downloads     : Screen("downloads")

    // Group study
    // ── Tier-Based Study Rooms (Phase 1) ───────────────────────
    object RoomsHub    : Screen("rooms_hub")
    object StudyFocus        : Screen("study_focus")
    object Achievements      : Screen("achievements")
    object WeeklyChallenges  : Screen("weekly_challenges")
    object ReadingRooms      : Screen("reading_rooms")
    object ReadingRoomActive : Screen("reading_room/{roomId}") {
        fun createRoute(id: String) = "reading_room/$id"
    }

    object StudyMaterials : Screen("study_materials")

    // PDF Viewer with page locking
    object PdfViewer : Screen("pdf_viewer/{fileUrl}/{title}/{freePages}/{isPurchased}") {
        fun createRoute(fileUrl: String, title: String, freePages: Int, isPurchased: Boolean) =
            "pdf_viewer/${fileUrl.encodeUrl()}/${title.encodeUrl()}/$freePages/$isPurchased"
    }

    // Wallet / payments
    object CoinWallet   : Screen("wallet")
    object Subscription : Screen("subscription")

    // Settings
    object NotificationSettings : Screen("notification_settings")
    object Settings             : Screen("settings")
    object EditProfile          : Screen("edit_profile")
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")