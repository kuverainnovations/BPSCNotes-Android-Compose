package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ══════════════════════════════════════════════════════════════
// COURSE DTOs
// ══════════════════════════════════════════════════════════════

data class CourseDto(
    val id: String,
    val title: String,
    val subject: String,
    val instructor: String? = null,
    val price: Int = 0,
    @SerializedName("original_price") val originalPrice: Int = 0,
    @SerializedName("is_paid") val isPaid: Boolean = false,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("total_lessons") val totalLessons: Int = 0,
    @SerializedName("completed_lessons") val completedLessons: Int = 0,
    @SerializedName("total_hours") val totalHours: Double = 0.0,
    @SerializedName("exam_tags") val examTags: List<String> = emptyList(),
    val rating: Double = 0.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    @SerializedName("enrollment_count") val enrollmentCount: Int = 0,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("is_enrolled") val isEnrolled: Boolean = false,
    val status: String = "published"
)

data class CoursesResponseData(
    val courses: List<CourseDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// QUIZ DTOs  (lightweight — for dashboard preview & list)
// ══════════════════════════════════════════════════════════════

data class QuizPreviewDto(
    val id: String,
    val title: String,
    val subject: String,
    val type: String,
    val difficulty: String = "medium",
    @SerializedName("total_questions") val totalQuestions: Int = 0,
    @SerializedName("duration_mins") val durationMins: Int = 15,
    @SerializedName("coins_reward") val coinsReward: Int = 0,
    @SerializedName("passing_score") val passingScore: Int = 60,
    @SerializedName("exam_tags") val examTags: List<String> = emptyList(),
    @SerializedName("attempt_count") val attemptCount: Int = 0,
    @SerializedName("avg_score") val avgScore: Double = 0.0,
    @SerializedName("is_attempted") val isAttempted: Boolean = false,
    @SerializedName("scheduled_for") val scheduledFor: String? = null,
    val status: String = "published"
)

data class QuizzesResponseData(
    val quizzes: List<QuizPreviewDto> = emptyList()
)

// Full quiz detail — questions included
data class QuizQuestionDto(
    val id: String,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("option_a") val optionA: String,
    @SerializedName("option_b") val optionB: String,
    @SerializedName("option_c") val optionC: String,
    @SerializedName("option_d") val optionD: String,
    @SerializedName("correct_option") val correctOption: String,   // "a" | "b" | "c" | "d"
    val explanation: String? = null,
    val subject: String? = null,
    val difficulty: String = "medium",
    @SerializedName("sort_order") val sortOrder: Int = 0
)

data class QuizDetailData(
    val quiz: QuizPreviewDto,
    val questions: List<QuizQuestionDto> = emptyList()
)

data class QuizSubmitRequest(
    val answers: List<QuizAnswerRequest>,
    @SerializedName("timeTakenSecs") val timeTakenSecs: Int = 0
)

data class QuizAnswerRequest(
    val questionId: String,
    val answer: String   // "a" | "b" | "c" | "d"
)

data class QuizResultDto(
    val score: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("correct_answers") val correctAnswers: Int,
    @SerializedName("time_taken_secs") val timeTakenSecs: Int,
    @SerializedName("coins_earned") val coinsEarned: Int,
    @SerializedName("is_passed") val isPassed: Boolean,
    val accuracy: Double,
    @SerializedName("new_coin_balance") val newCoinBalance: Int? = null
)

data class QuizResultData(val result: QuizResultDto)

// ══════════════════════════════════════════════════════════════
// CURRENT AFFAIRS DTOs
// ══════════════════════════════════════════════════════════════

data class CurrentAffairDto(
    val id: String,
    val title: String,
    val summary: String,
    @SerializedName("full_content") val fullContent: String? = null,
    val category: String,
    val source: String? = null,
    val date: String,
    @SerializedName("is_important") val isImportant: Boolean = false,
    @SerializedName("exam_tags") val examTags: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerializedName("view_count") val viewCount: Int = 0,
    @SerializedName("bookmark_count") val bookmarkCount: Int = 0,
    @SerializedName("is_bookmarked") val isBookmarked: Boolean = false
)

data class AffairsResponseData(
    val affairs: List<CurrentAffairDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// BANNER DTOs
// ══════════════════════════════════════════════════════════════

data class BannerDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("action_link") val actionLink: String? = null,
    val type: String = "promotion",
    @SerializedName("bg_gradient") val bgGradient: String? = null,
    val target: String = "all",
    @SerializedName("is_active") val isActive: Boolean = true
)

data class BannersResponseData(
    val banners: List<BannerDto> = emptyList()
)

// ══════════════════════════════════════════════════════════════
// USER STATS DTOs  (for dashboard: weekly activity, accuracy)
// ══════════════════════════════════════════════════════════════

data class WeeklyActivityDto(
    val date: String,    // "Mon", "Tue", etc. or ISO date
    val activity: Int    // 0-100 score
)

data class UserStatsData(
    @SerializedName("weekly_activity") val weeklyActivity: List<WeeklyActivityDto> = emptyList(),
    @SerializedName("total_study_minutes") val totalStudyMinutes: Int = 0,
    @SerializedName("current_streak") val currentStreak: Int = 0,
    @SerializedName("longest_streak") val longestStreak: Int = 0,
    val accuracy: Double = 0.0,
    @SerializedName("quizzes_attempted") val quizzesAttempted: Int = 0
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

    @GET("courses/{id}")
    suspend fun getCourseDetail(
        @Path("id") id: String
    ): ApiResponse<Any>

    @POST("courses/{id}/enroll")
    suspend fun enrollCourse(
        @Path("id") id: String
    ): ApiResponse<Any>
}

interface QuizzesApiService {

    @GET("quizzes")
    suspend fun getQuizzes(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 20,
        @Query("type")    type: String? = null,
        @Query("subject") subject: String? = null,
        @Query("date")    date: String? = null,
        @Query("exam")    exam: String? = null
    ): ApiResponse<QuizzesResponseData>

    @GET("quizzes/{id}")
    suspend fun getQuizDetail(
        @Path("id") id: String
    ): ApiResponse<QuizDetailData>

    @POST("quizzes/{id}/submit")
    suspend fun submitQuiz(
        @Path("id") id: String,
        @Body body: QuizSubmitRequest
    ): ApiResponse<QuizResultData>
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
    suspend fun getAffairDetail(
        @Path("id") id: String
    ): ApiResponse<Any>

    @POST("current-affairs/{id}/bookmark")
    suspend fun toggleBookmark(
        @Path("id") id: String
    ): ApiResponse<Any>
}

interface BannersApiService {

    @GET("banners")
    suspend fun getBanners(): ApiResponse<BannersResponseData>
}

interface UserStatsApiService {

    @GET("users/stats")
    suspend fun getStats(): ApiResponse<UserStatsData>
}
