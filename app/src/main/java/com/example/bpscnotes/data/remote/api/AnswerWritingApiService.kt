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
    /** The day this question belongs to (IST): scheduled_for, else created day */
    @SerializedName("effective_date")   val effectiveDate: String? = null,
    @SerializedName("is_today")         val isToday: Boolean = false,
    @SerializedName("is_pyq")           val isPyq: Boolean = false,
    @SerializedName("pyq_year")         val pyqYear: Int? = null,
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
    /** Only non-null once submitted AND the next IST day has arrived */
    @SerializedName("model_answer")  val modelAnswer: String? = null,
    /** true → submitted, model answer exists, but it reveals tomorrow */
    @SerializedName("model_answer_tomorrow") val modelAnswerTomorrow: Boolean = false,
    @SerializedName("is_pyq")        val isPyq: Boolean = false,
    @SerializedName("pyq_year")      val pyqYear: Int? = null,
    @SerializedName("scheduled_for") val scheduledFor: String? = null,
)

data class AnswerSubmissionDto(
    val id: String = "",
    @SerializedName("question_id")     val questionId: String = "",
    /** Typed answers only — null for handwritten photo answers */
    @SerializedName("answer_text")     val answerText: String? = null,
    /** Handwritten answers — photo URLs of the notebook pages */
    @SerializedName("answer_images")   val answerImages: List<String>? = null,
    /** PDF answers — hosted URL of the uploaded PDF */
    @SerializedName("answer_pdf")      val answerPdf: String? = null,
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
    val id: String = "",
    /** "yes" | "partly" | "no" — did it address the question demand */
    val verdict: String = "yes",
    val rating: Int = 0,
    @SerializedName("improvement_area")  val improvementArea: String? = null,
    @SerializedName("improvement_areas") val improvementAreas: List<String>? = null,
    val suggestion: String? = null,
    @SerializedName("created_at")       val createdAt: String? = null,
    // ── "Was this review useful?" — only the answer's author votes ──
    @SerializedName("helpful_votes")    val helpfulVotes: Int = 0,
    @SerializedName("unhelpful_votes")  val unhelpfulVotes: Int = 0,
    /** null = not voted yet, true = helpful, false = not helpful */
    @SerializedName("my_vote")          val myVote: Boolean? = null,
)

data class ReviewVoteData(
    val helpfulVotes: Int = 0,
    val unhelpfulVotes: Int = 0,
    val myVote: Boolean = false,
)

data class VoteReviewRequest(val helpful: Boolean)

data class AnswerQuestionsData(val questions: List<AnswerQuestionDto> = emptyList())
data class MyAnswerSubmissionsData(val submissions: List<AnswerSubmissionDto> = emptyList())
data class AnswerQuestionDetailData(
    val question: AnswerQuestionDetailDto = AnswerQuestionDetailDto(),
    val submission: AnswerSubmissionDto? = null,
    val peerReviews: List<PeerReviewDto> = emptyList(),
    /**
     * Reciprocity (client rule): the reviews on my answer stay hidden until
     * I have reviewed someone else's answer to this same question. The count
     * is still sent while locked — it is the reason to go and review.
     */
    val peerReviewsLocked: Boolean = false,
    val peerReviewCount: Int = 0,
    /** How many answers on this question I could review right now */
    val reviewableCount: Int = 0,
)

// ── Insights — GET /answer-writing/insights ───────────────────

data class WeaknessDto(val area: String = "", val count: Int = 0)

data class LeaderboardReviewerDto(
    val name: String = "",
    @SerializedName("reviews_given")   val reviewsGiven: Int = 0,
    @SerializedName("review_credits")  val reviewCredits: Int = 0,
    @SerializedName("helpful_reviews") val helpfulReviews: Int = 0,
    @SerializedName("reviewer_rating") val reviewerRating: Double? = null,
    @SerializedName("is_me")           val isMe: Boolean = false,
)
data class LeaderboardWriterDto(
    val name: String = "",
    val answers: Int = 0,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("is_me")      val isMe: Boolean = false,
)
data class AnswerLeaderboardData(
    val topReviewers: List<LeaderboardReviewerDto> = emptyList(),
    val topWriters: List<LeaderboardWriterDto> = emptyList(),
)

data class AnswerInsightsData(
    val topWeaknesses: List<WeaknessDto> = emptyList(),
    val answersWritten: Int = 0,
    val answersThisMonth: Int = 0,
    val reviewsGiven: Int = 0,
    val reviewsReceived: Int = 0,
    val avgRating: Double? = null,
    val avgMentorScore: Double? = null,
    val mentorReviewed: Int = 0,
    val reviewCredits: Int = 0,
    val totalWords: Int = 0,
    val writingStreak: Int = 0,
    val monthlyGoal: Int = 10,
    // ── Reviewer reputation ───────────────────────────────────
    /** My reviews the author judged useful */
    val helpfulReviews: Int = 0,
    /** My reviews that got any vote — the denominator behind the rating */
    val votedReviews: Int = 0,
    /** 1..5, null until one of my reviews has been voted on */
    val reviewerRating: Double? = null,
    /** Consistently unhelpful — reviews still count, but earn no coins */
    val lowReputation: Boolean = false,
    val coinsFromReviews: Int = 0,
    /** My position among all reviewers by volume, null if I've given none */
    val reviewerRank: Int? = null,
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
    @SerializedName("answer_pdf")    val answerPdf: String? = null,
    @SerializedName("word_count")    val wordCount: Int = 0,
    /**
     * Reviews this answer already has. Note the API deliberately does NOT
     * send its average rating — a visible score anchors the reviewer before
     * they've formed their own view. The count is safe: it shows which
     * answers still need help.
     */
    @SerializedName("peer_review_count") val peerReviewCount: Int = 0,
    /** House-authored sample answer, shown labelled rather than as a peer's */
    @SerializedName("is_seed")       val isSeed: Boolean = false,
    @SerializedName("question_id")   val questionId: String = "",
    @SerializedName("question_text") val questionText: String = "",
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")    val wordLimit: Int = 250,
    // ── Already reviewed by me? The answer stays on the list so I can re-read
    //    it to learn; the app opens it read-only with the review I gave. ──
    @SerializedName("reviewed_by_me")      val reviewedByMe: Boolean = false,
    @SerializedName("my_verdict")          val myVerdict: String? = null,
    @SerializedName("my_rating")           val myRating: Int = 0,
    @SerializedName("my_improvement_area") val myImprovementArea: String? = null,
    @SerializedName("my_improvement_areas") val myImprovementAreas: List<String>? = null,
    @SerializedName("my_suggestion")       val mySuggestion: String? = null,
    // ── "Helpful? 👍👎" on this answer (quick reaction while reviewing) ──
    @SerializedName("helpful_count")       val helpfulCount: Int = 0,
    @SerializedName("not_helpful_count")   val notHelpfulCount: Int = 0,
    /** null = not voted, true = 👍, false = 👎 */
    @SerializedName("my_helpful_vote")     val myHelpfulVote: Boolean? = null,
)

data class AnswerHelpfulData(
    val helpfulCount: Int = 0,
    val notHelpfulCount: Int = 0,
    val myVote: Boolean = false,
)
data class NextReviewData(val submission: ReviewAssignmentDto? = null)
data class ReviewListData(
    val submissions: List<ReviewAssignmentDto> = emptyList(),
    /** true → reviewing here unlocks the reviews waiting on my own answer */
    val unlocksMyReviews: Boolean = false,
)

/** Peer review screen 1 — a question I attempted, with answers awaiting review */
data class ReviewQuestionDto(
    val id: String = "",
    @SerializedName("question_text")       val questionText: String = "",
    val subject: String? = null,
    val marks: Int = 10,
    @SerializedName("word_limit")          val wordLimit: Int = 250,
    @SerializedName("is_pyq")              val isPyq: Boolean = false,
    @SerializedName("pyq_year")            val pyqYear: Int? = null,
    /** Answers under this question I can review right now */
    @SerializedName("pending_count")       val pendingCount: Int = 0,
    /** Answers I can open under this question — pending + already reviewed */
    @SerializedName("answer_count")        val answerCount: Int = 0,
    /** Reviews I have already given on this question */
    @SerializedName("my_reviews_here")     val myReviewsHere: Int = 0,
    /** Reviews waiting on MY answer to this question */
    @SerializedName("my_reviews_received") val myReviewsReceived: Int = 0,
    /** true → I have reviews waiting here that one review of mine would unlock */
    @SerializedName("unlocks_my_reviews")  val unlocksMyReviews: Boolean = false,
)
data class ReviewQuestionsData(
    val questions: List<ReviewQuestionDto> = emptyList(),
    val totalPending: Int = 0,
)

data class SubmitPeerReviewRequest(
    val verdict: String,                       // yes | partly | no
    val rating: Int,                           // 1..5
    /** Up to 3 of: introduction|structure|content|value_addition|analysis|conclusion */
    val improvementAreas: List<String>? = null,
    val improvementArea: String? = null,       // legacy single field
    val suggestion: String? = null,
)
data class SubmitPeerReviewData(
    val reviewCredits: Int = 0,
    val coinsEarned: Int = 0,
    /** true → no coins this time; the message explains why */
    val lowReputation: Boolean = false,
    val reviewerRating: Double? = null,
    /** true → this review just unlocked the feedback on my own answer here */
    val unlockedMyReviews: Boolean = false,
    /** How many reviews are waiting on my answer to this question */
    val myReviewCount: Int = 0,
    val questionId: String = "",
)

data class SubmitAnswerRequest(
    val answerText: String,
    val timeTakenSecs: Int? = null,
)
data class SubmitAnswerData(
    val submission: AnswerSubmissionDto? = null,
    val coinsEarned: Int = 0,
    val modelAnswer: String? = null,
    val modelAnswerTomorrow: Boolean = false,
)

interface AnswerWritingApiService {

    @GET("answer-writing")
    suspend fun getQuestions(
        @Query("page") page: Int = 1,
        // Higher default so the "Previous Questions" history isn't truncated.
        @Query("limit") limit: Int = 100,
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

    /** PDF answer — a single uploaded PDF of the answer */
    @Multipart
    @POST("answer-writing/{id}/submit-pdf")
    suspend fun submitPdfAnswer(
        @Path("id") id: String,
        @Part pdf: okhttp3.MultipartBody.Part,
        @Part("timeTakenSecs") timeTakenSecs: okhttp3.RequestBody,
    ): ApiResponse<SubmitAnswerData>

    // ── Peer review ────────────────────────────────────────────

    @GET("answer-writing/insights")
    suspend fun getInsights(): ApiResponse<AnswerInsightsData>

    @GET("answer-writing/leaderboard")
    suspend fun getLeaderboard(): ApiResponse<AnswerLeaderboardData>

    @GET("answer-writing/review/stats")
    suspend fun getReviewStats(): ApiResponse<ReviewStatsData>

    @GET("answer-writing/review/next")
    suspend fun getNextToReview(): ApiResponse<NextReviewData>

    /** Peer review screen 1 — questions with answers awaiting my review */
    @GET("answer-writing/review/questions")
    suspend fun getReviewQuestions(): ApiResponse<ReviewQuestionsData>

    /** Peer review screen 2 — answers to review, scoped to one question */
    @GET("answer-writing/review/list")
    suspend fun getReviewList(
        @Query("questionId") questionId: String? = null,
    ): ApiResponse<ReviewListData>

    @POST("answer-writing/review/{submissionId}")
    suspend fun submitPeerReview(
        @Path("submissionId") submissionId: String,
        @Body body: SubmitPeerReviewRequest,
    ): ApiResponse<SubmitPeerReviewData>

    /** "Was this review useful?" — the answer's author rates a review they received */
    @POST("answer-writing/review/vote/{reviewId}")
    suspend fun voteOnReview(
        @Path("reviewId") reviewId: String,
        @Body body: VoteReviewRequest,
    ): ApiResponse<ReviewVoteData>

    /** "Helpful? 👍👎" on an answer while reviewing it */
    @POST("answer-writing/answer/{submissionId}/helpful")
    suspend fun voteAnswerHelpful(
        @Path("submissionId") submissionId: String,
        @Body body: VoteReviewRequest,
    ): ApiResponse<AnswerHelpfulData>
}
