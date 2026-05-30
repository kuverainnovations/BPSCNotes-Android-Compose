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
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ══════════════════════════════════════════════════════════════
// COURSE DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════
data class CoursesData(
    val courses: List<CourseDto>
)

data class CourseDetailResponse(
    @SerializedName("course")
    val course: CourseDto,

    @SerializedName("chapters")
    val chapters: List<Chapter>? = null
)

data class CourseDto(

    val id: String,
    val title: String,
    val slug: String = "",
    val description: String?,
    val instructor: String?,
    @SerializedName("instructor_bio")      val instructorBio: String? = null,
    @SerializedName("instructor_students") val instructorStudents: String? = null,
    @SerializedName("instructor_courses")  val instructorCourses: Int = 1,
    val subject: String = "",
    val price: Int = 0,
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
    @SerializedName("total_chapters")
    val totalChapters: Int = 0,
    @SerializedName("total_hours")
    val totalHours: String = "0",
    val rating: String = "0",
    val review_count: Int,
    @SerializedName("enrollment_count")
    val enrollmentCount: Int,
    val bpsc_relevance: Int,
    val syllabus_coverage: Int,
    val language: String = "",
    val trial_lesson_title: String?,
    val exam_tags: List<String> = emptyList(),
    val status: String = "published",
    val meta_keywords: String?,
    val created_by: String = "",
    val created_at: String = "",
    val updated_at: String = "",
    val enrollment: Enrollment? = null,
    val chapters: List<Chapter> = emptyList(),
    @SerializedName("what_you_learn")      val whatYouLearn: List<String> = emptyList(),
    @SerializedName("has_certificate")     val hasCertificate: Boolean = true,
    @SerializedName("rating_distribution") val ratingDistribution: RatingDistribution? = null,

    @SerializedName("has_reviewed")
    val hasReviewed: Boolean = false,
    val reviews: List<CourseReview>? = null
)

data class CoursesResponseData(val courses: List<CourseDto> = emptyList())

data class SubscriptionPlanDto(
    val id: String? = null, val name: String? = null,
    val price: Int? = null, @SerializedName("original_price") val originalPrice: Int? = null,
    val duration: String? = null, @SerializedName("billing_cycle") val billingCycle: String? = null,
    @SerializedName("bonus_coins") val bonusCoins: Int? = null, val savings: Int? = null
)

data class SubscriptionPlansData(val plans: List<SubscriptionPlanDto> = emptyList())

// ── Payment DTOs ─────────────────────────────────────────────────────────
data class CreateSubscriptionRequest(
    val plan: String,
    val couponCode: String? = null,
    val coinsToUse: Int = 0
)

data class CreateSubscriptionResponse(
    val subscriptionId: String,
    val razorpayOrderId: String?,
    val razorpayKeyId: String?,
    val breakdown: PaymentBreakdown
)

data class PaymentBreakdown(
    val baseAmount: Int,
    val coinDiscount: Int,
    val couponDiscount: Int,
    val finalAmount: Int,
    val coinsUsed: Int,
    val couponCode: String?
)

data class ConfirmSubscriptionRequest(
    val razorpayOrderId: String,
    val transactionId: String,        // razorpayPaymentId
    val razorpaySignature: String,
    val paymentMethod: String = "upi",
    val upiId: String? = null
)

data class CoursePurchaseRequiredData(
    val price: Int,
    val razorpayOrderId: String?,
    val razorpayKeyId: String?,
    val courseTitle: String?,
    val courseId: String?,
    val code: String?
)

data class ConfirmCoursePurchaseRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
    val paymentMethod: String = "upi"
)

// ══════════════════════════════════════════════════════════════
// QUIZ DTOs
// ══════════════════════════════════════════════════════════════

/**
 * Lightweight quiz card — used in lobby list and dashboard.
 * is_attempted is now returned by the fixed backend SQL.
 */
data class QuizPreviewDto(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val type: String = "mock",
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
// ── Question type ──────────────────────────────────────────────
// text  = classic text question (no image)
// image = question has a main image shown above the text
enum class QuestionType { text, image }

// ── Option type ────────────────────────────────────────────────
// text  = all 4 options are text strings (classic MCQ)
// image = all 4 options are images (shown as 2×2 grid — Testbook style)
// mixed = options have text + optional image below
enum class OptionType { text, image, mixed }

data class QuizQuestionDto(
    val id: String = "",
    @SerializedName("question_text")      val questionText: String = "",
    @SerializedName("option_a")           val optionA: String = "",
    @SerializedName("option_b")           val optionB: String = "",
    @SerializedName("option_c")           val optionC: String = "",
    @SerializedName("option_d")           val optionD: String = "",
    @SerializedName("correct_option")
    val correctOption: String? = null,
    val subject: String? = null,
    val difficulty: String = "medium",
    val explanation: String = "Nothing",
    @SerializedName("sort_order")         val sortOrder: Int = 0,
    // Image support (added for image-based MCQs)
    @SerializedName("question_type")      val questionType: String = "text",  // "text" | "image"
    @SerializedName("question_image_url") val questionImageUrl: String? = null,
    @SerializedName("option_type")        val optionType: String = "text",   // "text" | "image" | "mixed"
    @SerializedName("option_a_image")     val optionAImage: String? = null,
    @SerializedName("option_b_image")     val optionBImage: String? = null,
    @SerializedName("option_c_image")     val optionCImage: String? = null,
    @SerializedName("option_d_image")     val optionDImage: String? = null,
) {
    val isImageQuestion: Boolean get() = questionType == "image" && !questionImageUrl.isNullOrBlank()
    val isImageOptions:  Boolean get() = optionType == "image"
    val optionImages:    List<String?> get() = listOf(optionAImage, optionBImage, optionCImage, optionDImage)
    val optionTexts:     List<String>  get() = listOf(optionA, optionB, optionC, optionD)
}

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

    val isPassed: Boolean = false,
    val coinsEarned: Int = 0,
    val timeTakenSecs: Int = 0,

    val rank: Int = 0,
    val percentile: Double = 0.0,

    val answers: List<QuizAnswerResultDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// CURRENT AFFAIRS DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class CurrentAffairDto(
    val id: String,
    val title: String,
    val summary: String = "",
    @SerializedName("full_content") val fullContent: String? = null,
    val category: String = "",
    val source: String? = null,
    val date: String = "",
    @SerializedName("is_important")    val isImportant: Boolean = false,
    @SerializedName("exam_tags")       val examTags: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerializedName("view_count")      val viewCount: Int = 0,
    @SerializedName("bookmark_count")  val bookmarkCount: Int = 0,
    @SerializedName("is_bookmarked")   val isBookmarked: Boolean = false,
    @SerializedName("mcq_count")       val mcqCount: Int = 0,
    val chapters: List<Chapter> = emptyList(),
)
data class Chapter(
    val id: String,
    val title: String,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    val lessons: List<Lesson>
)
data class Lesson(
    val id: String,
    val title: String,
    @SerializedName("duration_mins")   val duration_mins: Int = 0,
    val type: String = "pdf",
    @SerializedName("notes_url")       val notes_url: String? = null,
    @SerializedName("video_url")       val video_url: String? = null,
    @SerializedName("is_free_preview") val is_free_preview: Boolean = false,
    @SerializedName("is_locked")       val is_locked: Boolean = true,
    @SerializedName("is_completed")    val is_completed: Boolean? = null,
    @SerializedName("sort_order")      val sort_order: Int = 0,
    @SerializedName("watch_time_secs") val watch_time_secs: Int = 0
)

// ── Lesson detail response ────────────────────────────────────
data class LessonDetailResponseData(val lesson: Lesson)

// ── Complete lesson request / response ───────────────────────
data class CompleteLessonRequest(
    @SerializedName("watchTimeSecs") val watchTimeSecs: Int = 0
)
data class CompleteLessonResponse(
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    @SerializedName("total_chapters")
    val totalChapters: Int = 0,
    val isCompleted: Boolean = false
)
data class Enrollment(
    val id: String,
    val user_id: String? = null,
    val course_id: String? = null,
    val status: String = "active",
    val completed_lessons: Int = 0,
    val last_lesson_id: String? = null,
    @SerializedName("total_minutes")   val totalMinutes: Int = 0,
    @SerializedName("studied_minutes") val studiedMinutes: Int = 0,
    @SerializedName("last_studied_at") val lastStudiedAt: String? = null,
    val enrolled_at: String? = null,
    val completed_at: String? = null
)

// ── Course review ────────────────────────────────────────────
data class CourseReview(
    val id: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,
    @SerializedName("is_verified")    val isVerified: Boolean = false,
    @SerializedName("reviewer_name") val reviewerName: String? = null,
    @SerializedName("avatar_url")    val avatarUrl: String? = null,
    @SerializedName("created_at")    val createdAt: String? = null
)

// ── Rating distribution ──────────────────────────────────────
data class RatingDistribution(
    @SerializedName("5") val five:  Int = 0,
    @SerializedName("4") val four:  Int = 0,
    @SerializedName("3") val three: Int = 0,
    @SerializedName("2") val two:   Int = 0,
    @SerializedName("1") val one:   Int = 0
) {
    val total get() = (five + four + three + two + one).coerceAtLeast(1)
    fun pct(star: Int) = when(star) {
        5 -> five; 4 -> four; 3 -> three; 2 -> two; else -> one
    }.toFloat() / total
}

// ── Submit review request ────────────────────────────────────
data class SubmitReviewRequest(
    val rating: Int,
    val comment: String = ""
)

data class AffairsResponseData(val affairs: List<CurrentAffairDto> = emptyList())

// ══════════════════════════════════════════════════════════════
// BANNER DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class BannerDto(
    val id: String = "",
    val title: String = "",
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

data class WeeklyActivityDto(val date: String = "", val activity: Int = 0)

data class SubjectAccuracyDto(
    val subject:     String = "",
    val attempts:    String = "0",                     // backend sends as STRING
    @SerializedName("avg_accuracy") val avgAccuracy: String? = null  // percentage 0-100, or null
)

data class UserStatsData(
    @SerializedName("weekly_activity")     val weeklyActivity: List<WeeklyActivityDto> = emptyList(),
    @SerializedName("total_study_minutes") val totalStudyMinutes: Int = 0,
    @SerializedName("current_streak")      val currentStreak: Int = 0,
    @SerializedName("longest_streak")      val longestStreak: Int = 0,
    val accuracy: Double = 0.0,
    val rank: Int? = null,
    @SerializedName("quizzes_attempted")   val quizzesAttempted: Int = 0,
    // Subject-wise accuracy from quiz attempts — now mapped from API
    @SerializedName("subjectAccuracy")     val subjectAccuracy: List<SubjectAccuracyDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// DAILY TARGETS DTOs  (unchanged)
// ══════════════════════════════════════════════════════════════

data class DailyTargetDto(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
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


data class AppConfigWrapper(
    @com.google.gson.annotations.SerializedName("config")
    val config: Map<String, String>? = null
)


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

    @GET("courses/{id}")
    suspend fun getCourseDetail(@Path("id") id: String): ApiResponse<JsonObject>

    /** GET /subscriptions/plans — subscription plan options */
    @GET("subscriptions/plans")
    suspend fun getSubscriptionPlans(): ApiResponse<SubscriptionPlansData>

    /** POST /subscriptions/create — create order + get Razorpay order ID */
    @POST("subscriptions/create")
    suspend fun createSubscription(@Body dto: CreateSubscriptionRequest): ApiResponse<CreateSubscriptionResponse>

    /** POST /subscriptions/:id/confirm — verify signature, activate subscription */
    @POST("subscriptions/{id}/confirm")
    suspend fun confirmSubscription(
        @Path("id") id: String,
        @Body dto: ConfirmSubscriptionRequest
    ): ApiResponse<Any>

    /** GET /subscriptions/status — active subscription for current user */
    @GET("subscriptions/status")
    suspend fun getSubscriptionStatus(): ApiResponse<Any>

    /** POST /courses/:id/purchase/confirm — verify payment, enroll in course */
    @POST("courses/{id}/purchase/confirm")
    suspend fun confirmCoursePurchase(
        @Path("id") id: String,
        @Body dto: ConfirmCoursePurchaseRequest
    ): ApiResponse<Any>

    @POST("courses/{id}/enroll")
    suspend fun enrollCourse(@Path("id") id: String): ApiResponse<Any>

    @POST("courses/{id}/save")
    suspend fun saveCourse(@Path("id") id: String): ApiResponse<Any>

    @DELETE("courses/{id}/save")
    suspend fun unsaveCourse(@Path("id") id: String): ApiResponse<Any>

    @GET("courses/saved")
    suspend fun getSavedCourses(): ApiResponse<CoursesResponseData>

    // Get full lesson detail including notes_url / video_url / is_completed
    @GET("courses/{courseId}/lessons/{lessonId}")
    suspend fun getLessonDetail(
        @Path("courseId") courseId: String,
        @Path("lessonId") lessonId: String
    ): ApiResponse<LessonDetailResponseData>

    // Mark a lesson complete — updates lesson_progress + enrollment progress
    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    suspend fun completeCourseLesson(
        @Path("courseId") courseId: String,
        @Path("lessonId") lessonId: String,
        @Body request: CompleteLessonRequest
    ): ApiResponse<CompleteLessonResponse>

    // Submit a star rating + comment for a course
    @POST("courses/{id}/review")
    suspend fun submitCourseReview(
        @Path("id") courseId: String,
        @Body request: SubmitReviewRequest
    ): ApiResponse<Any>
}


data class CaMcqDto(
    val id: String = "",
    val question: String = "",
    @com.google.gson.annotations.SerializedName("option_a") val optionA: String = "",
    @com.google.gson.annotations.SerializedName("option_b") val optionB: String = "",
    @com.google.gson.annotations.SerializedName("option_c") val optionC: String = "",
    @com.google.gson.annotations.SerializedName("option_d") val optionD: String = "",
    @com.google.gson.annotations.SerializedName("option_e") val optionE: String = "",
    val correct: String = "a",
    val explanation: String? = null,
    val difficulty: String = "medium"
)

data class CaMcqsResponseData(
    val mcqs: List<CaMcqDto> = emptyList()
)

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

    @GET("current-affairs/{id}/mcqs")
    suspend fun getMcqs(@Path("id") id: String): ApiResponse<CaMcqsResponseData>
}

interface BannersApiService {
    @GET("banners")
    suspend fun getBanners(): ApiResponse<BannersResponseData>
}

interface UserStatsApiService {
    /** GET /app-config — admin-controlled settings (coin rates, limits, flags) */
    @retrofit2.http.GET("app-config")
    suspend fun getAppConfig(): com.example.bpscnotes.data.remote.dto.ApiResponse<AppConfigWrapper>

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
    @SerializedName("scheduled_at")    val scheduledAt: String = "",
    @SerializedName("duration_mins")   val durationMins: Int = 60,
    @SerializedName("meeting_link")    val meetingLink: String? = null,
    @SerializedName("is_live")         val isLive: Boolean = false,
    @SerializedName("registered_count") val registeredCount: Int = 0,
    @SerializedName("exam_tags")       val examTags: List<String> = emptyList(),
    val status: String = "scheduled",   // "scheduled" | "live" | "ended"
    // FIX: is_registered comes from GET /users/live-classes — no extra API call needed
    // Backend already joins live_class_registrations to set this per user
    @SerializedName("is_registered")   val isRegistered: Boolean = false
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
    val id: String = "",
    val title: String = "",
    @SerializedName("department", alternate = ["organization"]) val department: String? = null,
    val category: String? = null,
    @SerializedName("total_posts") val totalPosts: Int? = null,
    val location: String? = null,
    @SerializedName("salary_range") val salaryRange: String? = null,
    val qualification: String? = null,
    @SerializedName("age_limit") val ageLimit: String? = null,
    @SerializedName("apply_end_date", alternate = ["last_date"]) val applyEndDate: String? = null,
    @SerializedName("notification_date") val notificationDate: String? = null,
    @SerializedName("apply_start_date") val applyStartDate: String? = null,
    @SerializedName("exam_date") val examDate: String? = null,
    @SerializedName("official_link", alternate = ["application_link"]) val officialLink: String? = null,
    val description: String? = null,
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("is_urgent") val isUrgent: Boolean = false,
    @SerializedName("nearby_districts") val nearbyDistricts: List<String>? = null,
    @SerializedName("is_saved") val isSaved: Boolean = false,
    val status: String? = null
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

    @POST("jobs/{id}/save")
    suspend fun toggleSaveJob(
        @Path("id") jobId: String
    ): ApiResponse<Any>
}

// ══════════════════════════════════════════════════════════════
// STUDY ROOMS DTOs  — GET /study-rooms
// Powers: ReadingRoomsScreen
// ══════════════════════════════════════════════════════════════

data class StudyRoomDto(
    val id: String,
    val name: String,
    val subject: String,                            // "Polity" | "History" | etc.
    @SerializedName("today_focus") val todayFocus: String = "",
    @SerializedName("host_name") val hostName: String = "",
    @SerializedName("current_members") val currentMembers: Int = 0,
    @SerializedName("max_members") val maxMembers: Int = 0,
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
    @SerializedName("today_focus") val todayFocus: String = "",
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
    @SerializedName("totalEarned")        val totalEarned: Int = 0,
    @SerializedName("totalSpent")         val totalSpent: Int = 0,
    @SerializedName("check_in_streak")    val checkInStreak: Int = 0,
    @SerializedName("checked_in_today")   val checkedInToday: Boolean = false,
    @SerializedName("coinsEarned")        val coinsEarned: Int? = null,
    @SerializedName("alreadyClaimed")     val alreadyClaimed: Boolean = false
)

data class CheckInDayDto(
    val day: Int = 0,
    val label: String = "",
    @SerializedName("is_done") val isDone: Boolean = false,
    @SerializedName("is_today") val isToday: Boolean = false,
    @SerializedName("bonus_label") val bonusLabel: String = "",  // "+5 Gold" for day 7
    @SerializedName("is_bonus") val isBonus: Boolean = false
)

data class EarnTaskDto(
    val id: String,
    val title: String = "",
    val subtitle: String = "",

    val coinsReward: Int = 0,

    val icon: String = "quiz",

    val actionLabel: String = "Claim",

    val isCompleted: Boolean = false,
    val isAd: Boolean = false,

    // FIX: action tells the app WHERE to navigate when task is tapped
    // quiz=daily quiz, study=study materials, upload=upload screen,
    // referral=share/refer, ad=watch ad, checkin=check-in
    val action: String = "quiz",

    val actionBgHex: String? = "#F4B400",
    val iconBgHex: String? = "#FFF8E1",
    val iconTintHex: String? = "#F4B400",
    val actionTextColorHex: String? = "#FFFFFF",
) {
    val actionBg: Color         get() = parseColor(actionBgHex, Color(0xFF1565C0))
    val iconBg: Color           get() = parseColor(iconBgHex,   Color(0xFFE3F2FD))
    val iconTint: Color         get() = parseColor(iconTintHex, Color(0xFF1565C0))
    val actionTextColor: Color  get() = parseColor(actionTextColorHex, Color.White)

    companion object {
        fun parseColor(hex: String?, fallback: Color): Color = try {
            if (hex.isNullOrBlank()) fallback
            else Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
        } catch (e: Exception) { fallback }
    }
}

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
    @SerializedName("id")          val id: String = "",
    @SerializedName("title")       val title: String = "",
    @SerializedName("subtitle")    val subtitle: String = "",
    @SerializedName("coins")       val coins: Int = 0,
    @SerializedName("amount")      val amount: Int = 0,       // fallback field name
    @SerializedName("type")        val type: String = "",
    @SerializedName("date")        val date: String = "",
    @SerializedName("created_at")  val createdAt: String = "",  // fallback field name
    @SerializedName("icon")        val icon: String = "",
    @SerializedName("action")      val action: String = ""
) {
    // Use whichever field the backend sends
    val displayAmount: Int get() = if (coins != 0) coins else amount
    val displayDate: String get() = date.ifBlank { createdAt }
}

data class CoinsBalanceResponseData(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    @SerializedName("check_in_streak")   val checkInStreak: Int = 0,
    @SerializedName("checked_in_today")  val checkedInToday: Boolean = false,
    @SerializedName("check_in_days")     val checkInDays: List<CheckInDayDto> = emptyList()
)

data class EarnTasksResponseData(
    val tasks: List<EarnTaskDto> = emptyList()
)

data class CoinTransactionsResponseData(
    val transactions: List<CoinTransactionDto> = emptyList()
)

data class AdRewardRequest(val source: String = "wallet")
data class AdConfigDto(
    @com.google.gson.annotations.SerializedName("coinsPerAd") val coinsPerAd: Int = 10,
    @com.google.gson.annotations.SerializedName("minAdsPerSession") val minAdsPerSession: Int = 2
)

interface CoinsApiService {
    @GET("coins/balance")
    suspend fun getBalance(): ApiResponse<CoinsBalanceResponseData>

    @GET("coins/tasks")
    suspend fun getEarnTasks(): ApiResponse<EarnTasksResponseData>

    @GET("coins/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 50,@Query("page") page: Int = 1
    ): ApiResponse<CoinTransactionsResponseData>

    @POST("coins/check-in")
    suspend fun checkIn(): ApiResponse<CoinBalanceDto>

    @POST("coins/tasks/{id}/claim")
    suspend fun claimTask(@Path("id") id: String): ApiResponse<CoinBalanceDto>

    /** POST /coins/ad-reward — credit coins after watching a rewarded ad */
    @POST("coins/ad-reward")
    suspend fun recordAdReward(@Body body: AdRewardRequest): ApiResponse<CoinBalanceDto>

    /** GET /coins/ad-config — fetch admin-configured coins per ad */
    @GET("coins/ad-config")
    suspend fun getAdConfig(): ApiResponse<AdConfigDto>

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
        val id: String = "",
        val subject: String = "",
        val topic: String = "",
        val question: String = "",
        val answer: String = "",
        val hint: String = "",
        val example: String = "",
        val difficulty: String = "medium",
        /** 'text' (default) or 'image' — drives rendering in ActiveRecallScreen */
        @SerializedName("card_type")  val cardType: String = "text",
        /** Cloudinary URL — non-null only when cardType == 'image' */
        @SerializedName("image_url")  val imageUrl: String? = null,
        @SerializedName("back_image_url")  val backImageUrl: String? = null,
        @SerializedName("related_mcq") val relatedMcq: FlashMcqDto? = null
    )

    data class FlashcardsResponseData(
        val flashcards: List<FlashcardDto> = emptyList()
    )

    data class FlashcardProgressData(
        val mastered: List<String> = emptyList(),
        val weak:     List<String> = emptyList(),
        val total:    Int          = 0
    )

    data class SaveProgressRequest(
        val flashcardId: String,
        val rating:      String,   // "mastered" | "weak" | "skipped"
        val streak:      Int = 0
    )

    interface FlashcardsApiService {
        @GET("flashcards")
        suspend fun getFlashcards(
            @Query("subject") subject: String? = null,
            @Query("limit")   limit: Int = 200
        ): ApiResponse<FlashcardsResponseData>

        /** GET /flashcards/progress — load user's mastered/weak IDs from backend */
        @GET("flashcards/progress")
        suspend fun getProgress(): ApiResponse<FlashcardProgressData>

        /** POST /flashcards/progress — save a card rating to backend */
        @POST("flashcards/progress")
        suspend fun saveProgress(@Body request: SaveProgressRequest): ApiResponse<Any>
    }




}

// ── Exam DTOs ──────────────────────────────────────────────────

data class ExamDto(
    val name: String = "",
    @SerializedName("full_name")     val fullName: String = "",
    val category: String              = "",
    val emoji: String                 = "🎯",
    @SerializedName("sort_order")    val sortOrder: Int = 0,
    // Extra fields for the redesigned onboarding UI
    val difficulty: String?           = null,    // "hard"|"medium"|"easy"
    val subjects: List<String>        = emptyList(),
    @SerializedName("student_count") val studentCount: Int = 0,
    @SerializedName("prep_months")   val prepMonths: Int = 0
)

data class ExamsResponseData(
    val exams: List<ExamDto> = emptyList()
)

data class ExamTargetRequest(
    @SerializedName("primaryExam")   val primaryExam: String,
    @SerializedName("secondaryExam") val secondaryExam: String? = null,
    @SerializedName("targetYear")    val targetYear: Int? = null
)