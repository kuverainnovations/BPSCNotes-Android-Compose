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
}
