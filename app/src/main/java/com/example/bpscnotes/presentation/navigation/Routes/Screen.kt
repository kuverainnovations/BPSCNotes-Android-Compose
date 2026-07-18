package com.example.bpscnotes.presentation.navigation.Routes

sealed class Screen(val route: String) {
    // Auth
    object Splash              : Screen("splash")
    object LanguageSelection   : Screen("language_selection")
    object Onboarding          : Screen("onboarding")
    object Login        : Screen("login")
    object Otp : Screen("otp/{mobile}?context={context}") {
        fun createRoute(mobile: String) = "otp/$mobile"
        /** context: "registration" (default) or "forgot_mpin" */
        fun createRouteWithContext(mobile: String, context: String = "registration") =
            "otp/${mobile.encodeUrl()}?context=${context.encodeUrl()}"
    }
    object Register     : Screen("register/{tempToken}") {
        fun createRoute(tempToken: String) = "register/${tempToken.encodeUrl()}"
    }
    // NEW: exam setup after registration
    object ExamSetup  : Screen("exam_setup")
    // Post-login onboarding wizard: exam → target year → daily goal
    object OnboardingWizard : Screen("onboarding_wizard")

    // Main shell
    object Main         : Screen("main")

    // Bottom nav tabs
    object Dashboard    : Screen("dashboard")
    object MyLearning        : Screen("my_learning")
    object MyLearningCourses : Screen("my_learning_courses")  // opens directly on My Courses tab
    object ELibrary     : Screen("elibrary")
    object Profile      : Screen("profile")

    // Dashboard children
    object DailyTargets   : Screen("daily_targets")
    object CurrentAffairs   : Screen("current_affairs")
    object Payment          : Screen("payment")
    object CoursePayment : Screen(
        "course_payment/{courseId}/{courseTitle}/{price}/{sessionId}/{orderId}/{env}"
    ) {
        fun createRoute(courseId: String, courseTitle: String, price: Double,
                        paymentSessionId: String?,
                        providerOrderId: String? = null,
                        paymentEnvironment: String = "sandbox") =
            "course_payment/${courseId.encodeUrl()}/${courseTitle.take(50).encodeUrl()}/$price/${(paymentSessionId ?: "none").encodeUrl()}/${(providerOrderId ?: "none").encodeUrl()}/${paymentEnvironment.encodeUrl()}"
    }
    data class CaMcqQuiz(val affairId: String = "") : Screen("ca_mcq_quiz/{affairId}") {
        fun createRoute(id: String) = "ca_mcq_quiz/$id"
    }
    object CaArticleDetail : Screen("ca_article_detail/{affairId}") {
        fun createRoute(id: String) = "ca_article_detail/$id"
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
    object Notebook       : Screen("notebook")
    object MockTests      : Screen("mock_tests")
    // ── Answer Writing (Mains practice) ─────────────────────────
    object AnswerWriting       : Screen("answer_writing")
    object AnswerWritingDetail : Screen("answer_writing_detail/{questionId}") {
        fun createRoute(id: String) = "answer_writing_detail/$id"
    }
    object PeerReview          : Screen("answer_writing_peer_review")
    object JobVacancies   : Screen("job_vacancies")
    object JobDetail      : Screen("job_detail/{jobId}") {
        fun createRoute(id: String) = "job_detail/$id"
    }

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
    object Leaderboard          : Screen("leaderboard")
    object GlobalLeaderboard    : Screen("global_leaderboard")
    object StudySessionHistory  : Screen("study_session_history")
    object ReadingRoomActive : Screen("reading_room/{roomId}") {
        fun createRoute(id: String) = "reading_room/$id"
    }

    object StudyMaterials : Screen("study_materials")

    // Same screen as StudyMaterials, but opens pre-filtered to one material
    // type - used when navigating from a specific item (e.g. Premium
    // Content cards) so the user lands near what they tapped instead of
    // an unfiltered "all materials" list.
    object StudyMaterialsFiltered : Screen("study_materials_filtered/{type}") {
        fun createRoute(type: String) = "study_materials_filtered/$type"
    }

    // Phase 5: Chat with uploader for a purchased material
    object MaterialChat : Screen("material_chat/{chatId}") {
        fun createRoute(chatId: String) = "material_chat/$chatId"
    }

    // Phase 5: Inbox of all chat threads (buyer + uploader roles)
    object ChatInbox : Screen("chat_inbox")

    // Full-screen upload wizard (replaces ModalBottomSheet)
    object UploadMaterial : Screen("upload_material")

    // PDF Viewer with page locking
    // All string args are query params so encoded slashes in file:// URLs never break path matching.
    object PdfViewer : Screen(
        "pdf_viewer?fileUrl={fileUrl}&title={title}&freePages={freePages}&isPurchased={isPurchased}&materialId={materialId}&price={price}"
    ) {
        fun createRoute(fileUrl: String, title: String, freePages: Int, isPurchased: Boolean,
                        materialId: String = "", price: Double = 0.0) =
            "pdf_viewer" +
            "?fileUrl=${android.net.Uri.encode(fileUrl)}" +
            "&title=${android.net.Uri.encode(title)}" +
            "&freePages=$freePages" +
            "&isPurchased=$isPurchased" +
            "&materialId=${android.net.Uri.encode(materialId)}" +
            "&price=$price"
    }

    object VideoPlayer : Screen("video_player/{videoUrl}/{title}") {
        fun createRoute(videoUrl: String, title: String) =
            "video_player/${videoUrl.encodeUrl()}/${title.encodeUrl()}"
    }

    object ImageViewer : Screen("image_viewer/{imageUrl}/{title}") {
        fun createRoute(imageUrl: String, title: String) =
            "image_viewer/${imageUrl.encodeUrl()}/${title.encodeUrl()}"
    }

    // Search, Bookmarks & Coin Store
    object GlobalSearch          : Screen("global_search")
    object BookmarkedQuestions   : Screen("bookmarked_questions")
    object CoinStore             : Screen("coin_store")

    // Wallet / payments
    object CoinWallet   : Screen("wallet")
    object Subscription : Screen("subscription")

    // Settings
    object NotificationSettings : Screen("notification_settings")
    object Settings             : Screen("settings")
    object EditProfile          : Screen("edit_profile")
    object ChangeMpin           : Screen("change_mpin")

    // ── MPIN Auth ─────────────────────────────────────────────
    object MpinLogin  : Screen("mpin_login/{mobile}") {
        fun createRoute(mobile: String) = "mpin_login/${mobile.encodeUrl()}"
    }
    object CreateMpin : Screen("create_mpin")
    object ForgotMpin : Screen("forgot_mpin/{mobile}") {
        fun createRoute(mobile: String) = "forgot_mpin/${mobile.encodeUrl()}"
    }
    // OTP verified for forgot-mpin; otp is passed so ResetMpin can call backend
    object ResetMpin  : Screen("reset_mpin/{mobile}/{otp}") {
        fun createRoute(mobile: String, otp: String) =
            "reset_mpin/${mobile.encodeUrl()}/${otp.encodeUrl()}"
    }

    // Live Class Viewer — in-app WebView for live meetings
    object LiveClassViewer : Screen("live_class/{url}/{title}/{instructor}/{durationMins}") {
        fun createRoute(url: String, title: String, instructor: String, durationMins: Int) =
            "live_class/${url.encodeUrl()}/${title.encodeUrl()}/${instructor.encodeUrl()}/$durationMins"
    }
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")