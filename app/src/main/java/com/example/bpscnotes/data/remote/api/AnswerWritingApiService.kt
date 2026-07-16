package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ════════════════════════════════════════════════════════════
// FILE: data/remote/api/AnswerWritingApiService.kt
// DTOs + Retrofit interface for Answer Writing (Mains practice).
//
// Backend: backend/src/modules/answer-writing/answer-writing.module.ts
//   GET  /answer-writing            — published questions + my status
//   GET  /answer-writing/my         — my submission history
//   GET  /answer-writing/:id        — detail (+ my submission; model
//                                     answer only after I've submitted)
//   POST /answer-writing/:id/submit — one attempt per question, earns coins
// ════════════════════════════════════════════════════════════

/** One row of GET /answer-writing */
data class AnswerQuestionDto(
    val id: String = "",
    @SerializedName("question_text")    val questionText: String = "",
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")       val wordLimit: Int = 250,
    @SerializedName("scheduled_for")    val scheduledFor: String? = null,
    @SerializedName("created_at")       val createdAt: String? = null,
    @SerializedName("is_today")         val isToday: Boolean = false,
    @SerializedName("is_submitted")     val isSubmitted: Boolean = false,
    /** null | "submitted" | "reviewed" */
    @SerializedName("my_status")        val myStatus: String? = null,
    @SerializedName("my_score")         val myScore: Double? = null,
    @SerializedName("submission_count") val submissionCount: Int = 0,
)

/** Question inside GET /answer-writing/:id — includes tips + model answer (post-submit) */
data class AnswerQuestionDetailDto(
    val id: String = "",
    @SerializedName("question_text") val questionText: String = "",
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")    val wordLimit: Int = 250,
    val tips: String? = null,
    /** Only non-null once this user has submitted their own answer */
    @SerializedName("model_answer")  val modelAnswer: String? = null,
    @SerializedName("scheduled_for") val scheduledFor: String? = null,
)

data class AnswerSubmissionDto(
    val id: String = "",
    @SerializedName("question_id")     val questionId: String = "",
    /** Typed answers only — null for handwritten photo answers */
    @SerializedName("answer_text")     val answerText: String? = null,
    /** Handwritten answers — photo URLs of the notebook pages */
    @SerializedName("answer_images")   val answerImages: List<String>? = null,
    @SerializedName("word_count")      val wordCount: Int = 0,
    @SerializedName("time_taken_secs") val timeTakenSecs: Int? = null,
    /** "submitted" | "peer_reviewed" | "reviewed" (mentor-graded) */
    val status: String = "submitted",
    val score: Double? = null,
    val feedback: String? = null,
    @SerializedName("peer_review_count") val peerReviewCount: Int = 0,
    @SerializedName("avg_peer_rating")   val avgPeerRating: Double? = null,
    @SerializedName("created_at")      val createdAt: String? = null,
    @SerializedName("reviewed_at")     val reviewedAt: String? = null,
    // present on GET /answer-writing/my rows (joined question info)
    @SerializedName("question_text")   val questionText: String? = null,
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")      val wordLimit: Int = 250,
)

/** One anonymous peer review received on my submission */
data class PeerReviewDto(
    /** "yes" | "partly" | "no" — did it address the question demand */
    val verdict: String = "yes",
    val rating: Int = 0,
    @SerializedName("improvement_area") val improvementArea: String? = null,
    val suggestion: String? = null,
    @SerializedName("created_at")       val createdAt: String? = null,
)

data class AnswerQuestionsData(val questions: List<AnswerQuestionDto> = emptyList())
data class MyAnswerSubmissionsData(val submissions: List<AnswerSubmissionDto> = emptyList())
data class AnswerQuestionDetailData(
    val question: AnswerQuestionDetailDto = AnswerQuestionDetailDto(),
    val submission: AnswerSubmissionDto? = null,
    val peerReviews: List<PeerReviewDto> = emptyList(),
)

// ── Peer review — GET /answer-writing/review/* ────────────────

data class ReviewStatsData(
    val reviewsGiven: Int = 0,
    val reviewCredits: Int = 0,
    val pendingAvailable: Int = 0,
    val canReview: Boolean = false,
    /** null | "no_submission" | "not_reviewed_yet" */
    val lockedReason: String? = null,
)

/** Anonymous answer assigned for me to review (question fields joined in) */
data class ReviewAssignmentDto(
    val id: String = "",
    @SerializedName("answer_text")   val answerText: String? = null,
    @SerializedName("answer_images") val answerImages: List<String>? = null,
    @SerializedName("word_count")    val wordCount: Int = 0,
    @SerializedName("question_id")   val questionId: String = "",
    @SerializedName("question_text") val questionText: String = "",
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")    val wordLimit: Int = 250,
)
data class NextReviewData(val submission: ReviewAssignmentDto? = null)

data class SubmitPeerReviewRequest(
    val verdict: String,                 // yes | partly | no
    val rating: Int,                     // 1..5
    val improvementArea: String? = null, // content|structure|analysis|bihar_angle|presentation|conclusion
    val suggestion: String? = null,
)
data class SubmitPeerReviewData(
    val reviewCredits: Int = 0,
    val coinsEarned: Int = 0,
)

data class SubmitAnswerRequest(
    val answerText: String,
    val timeTakenSecs: Int? = null,
)
data class SubmitAnswerData(
    val submission: AnswerSubmissionDto? = null,
    val coinsEarned: Int = 0,
    val modelAnswer: String? = null,
)

interface AnswerWritingApiService {

    @GET("answer-writing")
    suspend fun getQuestions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("subject") subject: String? = null,
    ): ApiResponse<AnswerQuestionsData>

    @GET("answer-writing/my")
    suspend fun getMySubmissions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<MyAnswerSubmissionsData>

    @GET("answer-writing/{id}")
    suspend fun getQuestionDetail(@Path("id") id: String): ApiResponse<AnswerQuestionDetailData>

    @POST("answer-writing/{id}/submit")
    suspend fun submitAnswer(
        @Path("id") id: String,
        @Body body: SubmitAnswerRequest,
    ): ApiResponse<SubmitAnswerData>

    /** Handwritten answer — 1-5 photos of the notebook pages */
    @Multipart
    @POST("answer-writing/{id}/submit-photo")
    suspend fun submitPhotoAnswer(
        @Path("id") id: String,
        @Part images: List<okhttp3.MultipartBody.Part>,
        @Part("timeTakenSecs") timeTakenSecs: okhttp3.RequestBody,
    ): ApiResponse<SubmitAnswerData>

    // ── Peer review ────────────────────────────────────────────

    @GET("answer-writing/review/stats")
    suspend fun getReviewStats(): ApiResponse<ReviewStatsData>

    @GET("answer-writing/review/next")
    suspend fun getNextToReview(): ApiResponse<NextReviewData>

    @POST("answer-writing/review/{submissionId}")
    suspend fun submitPeerReview(
        @Path("submissionId") submissionId: String,
        @Body body: SubmitPeerReviewRequest,
    ): ApiResponse<SubmitPeerReviewData>
}
