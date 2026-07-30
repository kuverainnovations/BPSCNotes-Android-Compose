package com.example.bpscnotes.presentation.navigation

import com.example.bpscnotes.presentation.navigation.Routes.Screen

/**
 * The single mapping from a push notification's `screen` field to an in-app route.
 *
 * There used to be two of these and they disagreed. The in-app notification list
 * matched the `screen` vocabulary the backend actually sends; the cold-start
 * handler in BpscNavHost matched `type` values instead ("quizzes", "new_course",
 * "ca_update"), which the backend never puts in `screen`. The result was that
 * tapping a notification inside the app worked, and tapping the same notification
 * from the system tray did nothing for 10 of the 12 screens the backend sends.
 *
 * Returning a route string rather than navigating keeps both call sites free to
 * apply their own NavOptions (cold start wants launchSingleTop).
 *
 * @return the route to navigate to, or null if this screen has nowhere to go.
 */
fun notificationRoute(screen: String, ids: Map<String, String?> = emptyMap()): String? {
    fun id(key: String) = ids[key]?.takeIf { it.isNotBlank() }

    return when (screen) {
        "home"              -> Screen.Main.route
        "notifications"     -> Screen.NotificationSettings.route
        "my_courses"        -> Screen.MyLearningCourses.route
        "courses"           -> id("courseId")?.let { Screen.CourseDetail.createRoute(it) }
                                   ?: Screen.MyLearning.route
        "study_materials"   -> Screen.StudyMaterials.route
        // Sent by the backend for upload approved/rejected, so this is always
        // about the user's OWN upload — land them on My Uploads.
        "library"           -> if (id("noteId") != null) Screen.MyUploads.route
                               else Screen.StudyMaterials.route
        "quiz_list"         -> id("quizId")?.let { Screen.QuizDetail.createRoute(it) }
                                   ?: Screen.QuizList.route
        "daily_targets"     -> Screen.DailyTargets.route
        "jobs"              -> Screen.JobVacancies.route
        "rooms_hub"         -> Screen.RoomsHub.route
        "weekly_challenges" -> Screen.WeeklyChallenges.route
        "answer_writing"    -> id("questionId")?.let { Screen.AnswerWritingDetail.createRoute(it) }
                                   ?: Screen.AnswerWriting.route
        // The backend sends 'flashcards' but no such screen exists, and any
        // unrecognised value is a no-op — the notification is still marked read.
        else                -> null
    }
}

/** Every id field the backend attaches to a push payload, for the intent extras. */
val NOTIFICATION_ID_KEYS = listOf(
    "courseId", "quizId", "noteId", "questionId",
    "materialId", "affairId", "jobId", "roomId", "classId",
)
