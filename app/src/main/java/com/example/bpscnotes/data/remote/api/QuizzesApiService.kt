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
        @Query("exam")    exam: String? = null,
        @Query("today")   today: Boolean? = null
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
     * POST /quizzes/:id/purchase — unlock a paid mock test with coins.
     * The server settles the balance; on success re-fetch the list so the
     * card flips to "Unlocked".
     */
    @POST("quizzes/{id}/purchase")
    suspend fun purchaseQuiz(
        @Path("id") id: String
    ): ApiResponse<QuizPurchaseData>

    /**
     * Money path. Called after Play Billing reports a successful purchase;
     * the server re-verifies the token with Google before granting access,
     * so a forged token gets nothing.
     */
    @POST("quizzes/{id}/purchase/gplay/verify")
    suspend fun verifyGplayQuizPurchase(
        @Path("id") id: String,
        @Body body: GplayQuizVerifyRequest
    ): ApiResponse<QuizPurchaseData>

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

    /**
     * POST /quizzes/custom — build a practice test from the question pool.
     * Returns the id of a real quiz, which is then started like any other.
     */
    @POST("quizzes/custom")
    suspend fun createCustomQuiz(
        @Body body: CustomQuizRequest
    ): ApiResponse<CustomQuizData>

    /** POST /quizzes/:id/abandon — mark session abandoned on back-press or app kill */
    @POST("quizzes/{id}/abandon")
    suspend fun abandonQuiz(
        @Path("id") id: String,
        @Body body: Map<String, String> = emptyMap()
    ): ApiResponse<Unit>

    /** GET /quizzes/:id/leaderboard — top scores for this quiz */
    @GET("quizzes/{id}/leaderboard")
    suspend fun getLeaderboard(
        @Path("id") id: String
    ): ApiResponse<QuizLeaderboardResponse>
}

/** Config the user picked in the "Create Custom Test" sheet. */
data class CustomQuizRequest(
    val subjects:        List<String> = emptyList(),   // empty = all subjects
    val questionCount:   Int          = 30,
    val durationMins:    Int          = 45,
    val negativeMarking: Boolean      = true,
)

data class CustomQuizData(
    val quizId:         String = "",
    /** May be lower than requested when the pool is thin — worth surfacing. */
    val totalQuestions: Int    = 0,
    val durationMins:   Int    = 0,
)

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
/** Result of unlocking a paid mock test with coins. */
data class QuizPurchaseData(
    @com.google.gson.annotations.SerializedName("quizId")      val quizId: String = "",
    @com.google.gson.annotations.SerializedName("coinsSpent")  val coinsSpent: Int = 0,
    /** Coin balance after the debit — lets the wallet chip update without a refetch. */
    @com.google.gson.annotations.SerializedName("balance")     val balance: Int = 0,
    @com.google.gson.annotations.SerializedName("alreadyOwned") val alreadyOwned: Boolean = false,
)

data class GplayQuizVerifyRequest(
    val purchaseToken: String,
    val clientPriceInr: Double? = null,
)
