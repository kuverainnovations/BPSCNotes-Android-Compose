package com.example.bpscnotes.data.remote.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.example.bpscnotes.presentation.course.CourseDetailResponse
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ══════════════════════════════════════════════════════════════
// COURSE DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class CourseDto(

val id: String,
val title: String,
val slug: String,
val description: String?,
val instructor: String?,
val instructor_bio: String?,
val subject: String,
val price: Int,
@SerializedName("original_price")
val originalPrice: Int,
@SerializedName("is_paid")
val isPaid: Boolean,
val is_featured: Boolean,
val is_limited_offer: Boolean,
val offer_ends_at: String?,
val thumbnail_url: String?,
@SerializedName("total_lessons")
val totalLessons: Int,
@SerializedName("total_hours")
val totalHours: String,
val rating: String,
val review_count: Int,
@SerializedName("enrollment_count")
val enrollmentCount: Int,
val bpsc_relevance: Int,
val syllabus_coverage: Int,
val language: String,
val trial_lesson_title: String?,
val exam_tags: List<String>,
val status: String,
val meta_keywords: String?,
val created_by: String,
val created_at: String,
val updated_at: String,
val enrollment: Enrollment?,
val chapters: List<Chapter>,
val reviews: List<Review>? // currently null

)
data class Review(
    val id: String? = null
    // Add fields later when API provides
)

data class CoursesResponseData(val courses: List<CourseDto> = emptyList())

// ══════════════════════════════════════════════════════════════
// QUIZ DTOs
// ══════════════════════════════════════════════════════════════

/**
 * Lightweight quiz card — used in lobby list and dashboard.
 * is_attempted is now returned by the fixed backend SQL.
 */
data class QuizPreviewDto(
    val id: String,
    val title: String,
    val subject: String,
    val type: String,
    val difficulty: String = "medium",
    @SerializedName("total_questions") val totalQuestions: Int = 0,
    @SerializedName("duration_mins")   val durationMins: Int = 15,
    @SerializedName("coins_reward")    val coinsReward: Int = 0,
    @SerializedName("passing_score")   val passingScore: Int = 60,
    @SerializedName("exam_tags")       val examTags: List<String> = emptyList(),
    @SerializedName("attempt_count")   val attemptCount: Int = 0,
    @SerializedName("avg_score")       val avgScore: Double = 0.0,
    // FIX: backend now returns this as a boolean via SQL COALESCE
    @SerializedName("is_attempted")    val isAttempted: Boolean = false,
    @SerializedName("my_last_score")   val myLastScore: Int? = null,
    @SerializedName("scheduled_for")   val scheduledFor: String? = null,
    val status: String = "published"
)

data class QuizzesResponseData(val quizzes: List<QuizPreviewDto> = emptyList())

/**
 * Single question returned by GET /quizzes/:id
 * NOTE: correct_option and explanation are NOT returned during play
 * (backend deliberately excludes them to prevent cheating).
 * They are returned in the submit result instead.
 */
data class QuizQuestionDto(
    val id: String,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("option_a")      val optionA: String,
    @SerializedName("option_b")      val optionB: String,
    @SerializedName("option_c")      val optionC: String,
    @SerializedName("option_d")      val optionD: String,
    val subject: String? = null,
    val difficulty: String = "medium",
    @SerializedName("sort_order")    val sortOrder: Int = 0
)

data class QuizDetailData(
    val quiz: QuizPreviewDto,
    val questions: List<QuizQuestionDto> = emptyList()
)

// ── Submit ────────────────────────────────────────────────────

data class QuizSubmitRequest(
    val answers: List<QuizAnswerRequest>,
    @SerializedName("timeTakenSecs") val timeTakenSecs: Int = 0
)

data class QuizAnswerRequest(
    val questionId: String,
    val answer: String           // "a" | "b" | "c" | "d"
)

/**
 * Per-question result returned in submit response.
 * correctAnswer + explanation are revealed here (not during play).
 */
data class QuizAnswerResultDto(
    val questionId: String,
    val answer: String,                          // what the user chose
    @SerializedName("isCorrect")     val isCorrect: Boolean = false,
    @SerializedName("correctAnswer") val correctAnswer: String = "",
    val explanation: String? = null              // explanation from backend
)

/**
 * FIX: matches actual backend response shape:
 * { "data": { "attemptId": "...", "score": 80, "correct": 4, "total": 5, ... } }
 *
 * OLD (wrong): QuizResultData(val result: QuizResultDto)
 * NEW (correct): flat object matching backend successResponse() keys
 */
data class QuizResultData(
    val attemptId: String? = null,
    val score: Int = 0,
    val correct: Int = 0,
    val total: Int = 0,
    val wrong: Int = 0,
    val accuracy: Double = 0.0,
    @SerializedName("isPassed")       val isPassed: Boolean = false,
    @SerializedName("coinsEarned")    val coinsEarned: Int = 0,
    @SerializedName("timeTakenSecs")  val timeTakenSecs: Int = 0,
    val answers: List<QuizAnswerResultDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// CURRENT AFFAIRS DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class CurrentAffairDto(
    val id: String,
    val title: String,
    val summary: String,
    @SerializedName("full_content") val fullContent: String? = null,
    val category: String,
    val source: String? = null,
    val date: String,
    @SerializedName("is_important")    val isImportant: Boolean = false,
    @SerializedName("exam_tags")       val examTags: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerializedName("view_count")      val viewCount: Int = 0,
    @SerializedName("bookmark_count")  val bookmarkCount: Int = 0,
    @SerializedName("is_bookmarked")   val isBookmarked: Boolean = false,
    val chapters: List<Chapter>,

    )
data class Chapter(
    val id: String,
    val title: String,
    val sort_order: Int,
    val lessons: List<Lesson>
)
data class Lesson(
    val id: String,
    val title: String,
    val duration_mins: Int,
    val type: String,
    val is_free_preview: Boolean,
    val is_locked: Boolean,
    val sort_order: Int
)
data class Enrollment(
    val id: String,
    val user_id: String,
    val course_id: String,
    val completed_lessons: Int,
    val last_lesson_id: String?,
    val status: String,
    val enrolled_at: String,
    val completed_at: String?
)

data class AffairsResponseData(val affairs: List<CurrentAffairDto> = emptyList())

// ══════════════════════════════════════════════════════════════
// BANNER DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class BannerDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @SerializedName("image_url")   val imageUrl: String? = null,
    @SerializedName("action_link") val actionLink: String? = null,
    val type: String = "promotion",
    @SerializedName("bg_gradient") val bgGradient: String? = null,
    val target: String = "all",
    @SerializedName("is_active")   val isActive: Boolean = true
)

data class BannersResponseData(val banners: List<BannerDto> = emptyList())

// ══════════════════════════════════════════════════════════════
// USER STATS DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class WeeklyActivityDto(val date: String, val activity: Int)

data class UserStatsData(
    @SerializedName("weekly_activity")     val weeklyActivity: List<WeeklyActivityDto> = emptyList(),
    @SerializedName("total_study_minutes") val totalStudyMinutes: Int = 0,
    @SerializedName("current_streak")      val currentStreak: Int = 0,
    @SerializedName("longest_streak")      val longestStreak: Int = 0,
    val accuracy: Double = 0.0,
    @SerializedName("quizzes_attempted")   val quizzesAttempted: Int = 0
)

// ══════════════════════════════════════════════════════════════
// DAILY TARGETS DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class DailyTargetDto(
    val id: String,
    val title: String,
    val subject: String,
    @SerializedName("is_completed")        val isCompleted: Boolean = false,
    @SerializedName("total_questions")     val totalQuestions: Int = 10,
    @SerializedName("attempted_questions") val attemptedQuestions: Int = 0,
    @SerializedName("estimated_minutes")   val estimatedMinutes: Int = 25,
    val difficulty: String = "medium",
    @SerializedName("time_slot")           val timeSlot: String = "morning",
    @SerializedName("is_carried_forward")  val isCarriedForward: Boolean = false,
    @SerializedName("linked_quiz_id")      val linkedQuizId: String? = null,
    @SerializedName("linked_note_id")      val linkedNoteId: String? = null
)

data class DailyTargetsSummary(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    @SerializedName("completion_pct")  val completionPct: Int = 0,
    @SerializedName("coins_available") val coinsAvailable: Int = 0
)

data class DailyTargetsResponseData(
    val targets: List<DailyTargetDto> = emptyList(),
    val summary: DailyTargetsSummary  = DailyTargetsSummary()
)

data class CreateTargetRequest(
    val title: String? = null,
    val titles: List<String>? = null,
    val subject: String = "General",
    val difficulty: String = "medium",
    @SerializedName("time_slot")           val timeSlot: String = "morning",
    @SerializedName("estimated_minutes")   val estimatedMinutes: Int = 25,
    @SerializedName("total_questions")     val totalQuestions: Int = 10
)

data class CompleteTargetResponse(
    val id: String,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("coins_earned") val coinsEarned: Int = 0
)

// ══════════════════════════════════════════════════════════════
// API SERVICE INTERFACES
// ══════════════════════════════════════════════════════════════

interface CoursesApiService {
    @GET("courses")
    suspend fun getCourses(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 20,
        @Query("type")    type: String? = null,
        @Query("subject") subject: String? = null,
        @Query("search")  search: String? = null,
        @Query("exam")    exam: String? = null
    ): ApiResponse<CoursesResponseData>

   /* @GET("courses/{id}")
    suspend fun getCourseDetail(@Path("id") id: String): ApiResponse<CourseDetailResponse>
*/
    @GET("courses/{id}")
    suspend fun getCourseDetail(@Path("id") id: String): ApiResponse<JsonObject>
    @POST("courses/{id}/enroll")
    suspend fun enrollCourse(@Path("id") id: String): ApiResponse<Any>
}


interface CurrentAffairsApiService {
    @GET("current-affairs")
    suspend fun getAffairs(
        @Query("page")      page: Int = 1,
        @Query("limit")     limit: Int = 50,
        @Query("date")      date: String? = null,
        @Query("category")  category: String? = null,
        @Query("exam")      exam: String? = null,
        @Query("important") important: Boolean? = null
    ): ApiResponse<AffairsResponseData>

    @GET("current-affairs/{id}")
    suspend fun getAffairDetail(@Path("id") id: String): ApiResponse<Any>

    @POST("current-affairs/{id}/bookmark")
    suspend fun toggleBookmark(@Path("id") id: String): ApiResponse<Any>
}

interface BannersApiService {
    @GET("banners")
    suspend fun getBanners(): ApiResponse<BannersResponseData>
}

interface UserStatsApiService {
    @GET("users/stats")
    suspend fun getStats(): ApiResponse<UserStatsData>
}

interface DailyTargetsApiService {
    @GET("users/daily-targets")
    suspend fun getDailyTargets(): ApiResponse<DailyTargetsResponseData>

    @POST("users/daily-targets")
    suspend fun createTargets(@Body body: CreateTargetRequest): ApiResponse<DailyTargetsResponseData>

    @PATCH("users/daily-targets/{id}/complete")
    suspend fun toggleComplete(@Path("id") id: String): ApiResponse<CompleteTargetResponse>

    @DELETE("users/daily-targets/{id}")
    suspend fun deleteTarget(@Path("id") id: String): ApiResponse<Any>
}


// ══════════════════════════════════════════════════════════════
// LIVE CLASSES DTOs  — GET /live-classes
// Powers: DashboardScreen.MyScheduleSection
// ══════════════════════════════════════════════════════════════

data class LiveClassDto(
    val id: String,
    val title: String,
    val instructor: String,
    val subject: String,
    @SerializedName("scheduled_at") val scheduledAt: String,
    @SerializedName("duration_mins") val durationMins: Int = 60,
    @SerializedName("meeting_link") val meetingLink: String? = null,
    @SerializedName("is_live") val isLive: Boolean = false,
    @SerializedName("registered_count") val registeredCount: Int = 0,
    @SerializedName("exam_tags") val examTags: List<String> = emptyList(),
    val status: String = "scheduled"    // "scheduled" | "live" | "ended"
)

data class LiveClassesResponseData(
    @SerializedName("liveClasses") val liveClasses: List<LiveClassDto> = emptyList()
)

interface LiveClassesApiService {
    @GET("users/live-classes")
    suspend fun getLiveClasses(
        @Query("limit") limit: Int = 5,
        @Query("status") status: String? = null
    ): ApiResponse<LiveClassesResponseData>

    @POST("users/live-classes/{id}/register")
    suspend fun register(@Path("id") id: String): ApiResponse<Any>
}

// ══════════════════════════════════════════════════════════════
// JOB VACANCIES DTOs  — GET /jobs
// Powers: JobVacanciesScreen
// ══════════════════════════════════════════════════════════════

data class JobVacancyDto(
    val id: String,
    val title: String,
    val department: String,
    val category: String,                            // "BPSC" | "Bihar Govt" | "Central Govt" | "Private" | "Part-time"
    @SerializedName("total_posts") val totalPosts: Int,
    val location: String,
    @SerializedName("salary_range") val salaryRange: String,
    val qualification: String,
    @SerializedName("age_limit") val ageLimit: String,
    @SerializedName("apply_end_date") val applyEndDate: String,    // ISO date
    @SerializedName("notification_date") val notificationDate: String,
    @SerializedName("apply_start_date") val applyStartDate: String,
    @SerializedName("exam_date") val examDate: String?,
    @SerializedName("official_link") val officialLink: String,
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("nearby_districts") val nearbyDistricts: List<String> = emptyList(),
    val status: String = "active"
)

data class JobsResponseData(
    val jobs: List<JobVacancyDto> = emptyList(),
    val total: Int = 0
)

interface JobsApiService {
    @GET("jobs")
    suspend fun getJobs(
        @Query("page")     page: Int = 1,
        @Query("limit")    limit: Int = 50,
        @Query("category") category: String? = null,
        @Query("search")   search: String? = null,
        @Query("status")   status: String = "active"
    ): ApiResponse<JobsResponseData>
}

// ══════════════════════════════════════════════════════════════
// STUDY ROOMS DTOs  — GET /study-rooms
// Powers: ReadingRoomsScreen
// ══════════════════════════════════════════════════════════════

data class StudyRoomDto(
    val id: String,
    val name: String,
    val subject: String,                            // "Polity" | "History" | etc.
    @SerializedName("today_focus") val todayFocus: String,
    @SerializedName("host_name") val hostName: String,
    @SerializedName("current_members") val currentMembers: Int,
    @SerializedName("max_members") val maxMembers: Int,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("is_member") val isMember: Boolean = false,
    val tags: List<String> = emptyList(),
    @SerializedName("duration_mins") val durationMins: Int = 120,
    @SerializedName("join_code") val joinCode: String? = null,   // null for public rooms
    val status: String = "active"
)

data class StudyRoomsResponseData(
    val rooms: List<StudyRoomDto> = emptyList()
)

interface StudyRoomsApiService {
    @GET("study-rooms")
    suspend fun getRooms(
        @Query("subject") subject: String? = null,
        @Query("search")  search: String? = null,
        @Query("limit")   limit: Int = 30
    ): ApiResponse<StudyRoomsResponseData>

    @POST("study-rooms/{id}/join")
    suspend fun joinRoom(
        @Path("id") id: String,
        @Body body: JoinRoomRequest = JoinRoomRequest()
    ): ApiResponse<Any>

    @POST("study-rooms/{id}/leave")
    suspend fun leaveRoom(@Path("id") id: String): ApiResponse<Any>

    @POST("study-rooms")
    suspend fun createRoom(@Body body: CreateRoomRequest): ApiResponse<StudyRoomDto>
}

data class JoinRoomRequest(
    @SerializedName("join_code") val joinCode: String? = null
)

data class CreateRoomRequest(
    val name: String,
    val subject: String,
    @SerializedName("today_focus") val todayFocus: String,
    @SerializedName("max_members") val maxMembers: Int = 20,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("duration_mins") val durationMins: Int = 120
)

// ══════════════════════════════════════════════════════════════
// COINS DTOs  — GET /coins/*
// Powers: CoinWalletScreen
// ══════════════════════════════════════════════════════════════

data class CoinBalanceDto(
    val balance: Int,
    @SerializedName("totalEarned") val totalEarned: Int = 0,
    @SerializedName("totalSpent") val totalSpent: Int = 0,
//    @SerializedName("check_in_streak") val checkInStreak: Int = 0,
//    @SerializedName("checked_in_today") val checkedInToday: Boolean = false
)

data class CheckInDayDto(
    val day: Int,                                   // 1–7
    val label: String,                              // "Mon", "Tue" etc.
    @SerializedName("is_done") val isDone: Boolean,
    @SerializedName("is_today") val isToday: Boolean,
    @SerializedName("bonus_label") val bonusLabel: String = "",  // "+5 Gold" for day 7
    @SerializedName("is_bonus") val isBonus: Boolean = false
)

data class EarnTaskDto(
    val id: String,
    val title: String,
    val subtitle: String,
    @SerializedName("coins_reward") val coinsReward: Int,
    val icon: String,                               // "quiz" | "study" | "referral" | "ad"
    @SerializedName("action_label") val actionLabel: String,
    @SerializedName("is_completed") val isCompleted: Boolean = false,
    @SerializedName("is_ad") val isAd: Boolean = false,
    @SerializedName("action_bg") val actionBg: Color,
    @SerializedName("icon_bg") val iconBg: Color,
    @SerializedName("icon_tint") val iconTint: Color,
    @SerializedName("action_text_color") val actionTextColor: Color,
    )

 fun mapIcon(icon: String): ImageVector {
    return when (icon) {
        "quiz"     -> Icons.Rounded.Quiz
        "study"    -> Icons.Rounded.MenuBook
        "referral" -> Icons.Rounded.CardGiftcard
        "ad"       -> Icons.Rounded.PlayCircle
        else       -> Icons.Rounded.HelpOutline
    }
}

data class CoinTransactionDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val coins: Int,
    val type: String,                               // "earned" | "spent"
    val date: String,
    val icon: String                                // "quiz" | "daily" | "store" | "referral"
)

data class CoinsBalanceResponseData(
    val balance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
    @SerializedName("check_in_days")
    val checkInDays: List<CheckInDayDto> = emptyList()
)

data class EarnTasksResponseData(
    val tasks: List<EarnTaskDto> = emptyList()
)

data class CoinTransactionsResponseData(
    val transactions: List<CoinTransactionDto> = emptyList()
)

interface CoinsApiService {
    @GET("coins/balance")
    suspend fun getBalance(): ApiResponse<CoinsBalanceResponseData>

    @GET("coins/tasks")
    suspend fun getEarnTasks(): ApiResponse<EarnTasksResponseData>

    @GET("coins/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 50
    ): ApiResponse<CoinTransactionsResponseData>

    @POST("coins/check-in")
    suspend fun checkIn(): ApiResponse<CoinBalanceDto>

    @POST("coins/tasks/{id}/claim")
    suspend fun claimTask(@Path("id") id: String): ApiResponse<Any>

    // ══════════════════════════════════════════════════════════════
// FLASHCARD DTOs — GET /flashcards
//
// Backend response shape:
// {
//   "success": true,
//   "data": {
//     "flashcards": [
//       {
//         "id": "...",
//         "subject": "Polity",
//         "topic": "Fundamental Rights",
//         "question": "Which Article guarantees Right to Equality?",
//         "answer": "Articles 14–18 ...",
//         "hint": "Think about Part III ...",
//         "example": "Article 14 was invoked in ...",
//         "difficulty": "easy",
//         "related_mcq": {
//           "question": "Article 14 deals with?",
//           "options": ["Right to Freedom", "Equality before Law", ...],
//           "correct_index": 1
//         }
//       }
//     ]
//   }
// }
// ══════════════════════════════════════════════════════════════

    data class FlashMcqDto(
        val question: String,
        val options: List<String>,
        @SerializedName("correct_index") val correctIndex: Int
    )

    data class FlashcardDto(
        val id: String,
        val subject: String,
        val topic: String,
        val question: String,
        val answer: String,
        val hint: String = "",
        val example: String = "",
        val difficulty: String = "medium",                     // "easy" | "medium" | "hard"
        @SerializedName("related_mcq") val relatedMcq: FlashMcqDto? = null
    )

    data class FlashcardsResponseData(
        val flashcards: List<FlashcardDto> = emptyList()
    )

    interface FlashcardsApiService {
        /**
         * GET /flashcards
         * Returns flashcards filtered by subject.
         * subject = null → all subjects
         */
        @GET("flashcards")
        suspend fun getFlashcards(
            @Query("subject") subject: String? = null,
            @Query("limit")   limit: Int = 100
        ): ApiResponse<FlashcardsResponseData>
    }

}
