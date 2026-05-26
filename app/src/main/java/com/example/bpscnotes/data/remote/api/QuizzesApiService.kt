package com.example.bpscnotes.data.remote.api

// ── This file shows ONLY the changed QuizzesApiService.
// Copy-paste to replace the interface in your existing AppApiService.kt
// Everything else (DTOs, other interfaces) stays unchanged.

import com.example.bpscnotes.data.remote.dto.ApiResponse
import retrofit2.http.*

interface QuizzesApiService {

    /** GET /quizzes — list with is_attempted flag */
    @GET("quizzes")
    suspend fun getQuizzes(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 50,
        @Query("type")    type: String? = null,
        @Query("subject") subject: String? = null,
        @Query("date")    date: String? = null,
        @Query("exam")    exam: String? = null
    ): ApiResponse<QuizzesResponseData>

    /**
     * GET /quizzes/:id — quiz metadata ONLY (no questions).
     * Used by QuizDetailScreen (intro before starting).
     */
    @GET("quizzes/{id}")
    suspend fun getQuizDetail(
        @Path("id") id: String
    ): ApiResponse<QuizDetailData>

    /**
     * POST /quizzes/:id/start
     * Creates an attempt session and returns questions.
     * Only called when user taps "Start Quiz".
     */
    @POST("quizzes/{id}/start")
    suspend fun startQuiz(
        @Path("id") id: String
    ): ApiResponse<QuizDetailData>   // same shape: { quiz, questions }

    /** POST /quizzes/:id/submit */
    @POST("quizzes/{id}/submit")
    suspend fun submitQuiz(
        @Path("id") id: String,
        @Body body: QuizSubmitRequest
    ): ApiResponse<QuizResultData>

    /** GET /quizzes/:id/leaderboard — top scores for this quiz */
    @GET("quizzes/{id}/leaderboard")
    suspend fun getLeaderboard(
        @Path("id") id: String
    ): ApiResponse<QuizLeaderboardResponse>
}

data class QuizLeaderboardResponse(
    val leaderboard: List<QuizLeaderboardItemResponse> = emptyList()
)

data class QuizLeaderboardItemResponse(
    @com.google.gson.annotations.SerializedName("rank_position")   val rankPosition: Int = 0,
    @com.google.gson.annotations.SerializedName("user_id")         val userId: String = "",
    @com.google.gson.annotations.SerializedName("user_name")       val userName: String = "",
    @com.google.gson.annotations.SerializedName("score")           val score: Float = 0f,
    @com.google.gson.annotations.SerializedName("correct_answers") val correctAnswers: Int = 0,
    @com.google.gson.annotations.SerializedName("total_questions") val totalQuestions: Int = 0,
    @com.google.gson.annotations.SerializedName("time_taken_secs") val timeTakenSecs: Int = 0,
    @com.google.gson.annotations.SerializedName("is_current_user") val isCurrentUser: Boolean = false
)