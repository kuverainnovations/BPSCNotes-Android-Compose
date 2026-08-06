package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// ══════════════════════════════════════════════════════════════
// MODERATION — reporting content and blocking users
//
// Google Play's User Generated Content policy requires both, in-app, on
// every surface where one user can see another's content. In this app that
// is tier room chat, answer peer review, and marketplace uploads.
//
// Blocking is one-directional: it only changes what the blocker sees, and
// the blocked user is never notified. The server enforces it on read — the
// app never filters blocked content itself, so a stale client cannot leak
// what a block was meant to hide.
// ══════════════════════════════════════════════════════════════

/** Report categories. Must match REPORT_REASONS in the backend module. */
object ReportReason {
    const val SPAM           = "spam"
    const val HARASSMENT     = "harassment"
    const val HATE_SPEECH    = "hate_speech"
    const val SEXUAL_CONTENT = "sexual_content"
    const val VIOLENCE       = "violence"
    const val COPYRIGHT      = "copyright"
    const val MISINFORMATION = "misinformation"
    const val PERSONAL_INFO  = "personal_info"
    const val OTHER          = "other"
}

/** Reportable surfaces. Must match CONTENT_TYPES in the backend module. */
object ReportContentType {
    const val ROOM_MESSAGE   = "room_message"
    const val PEER_REVIEW    = "peer_review"
    const val ANSWER         = "answer"
    const val STUDY_MATERIAL = "study_material"
    const val USER           = "user"
}

data class ReportRequest(
    val contentType: String,
    val contentId:   String,
    val reason:      String,
    val details:     String? = null,
)

data class ReportData(
    val reported: Boolean = false,
    val message:  String  = "",
)

data class BlockedUserDto(
    val id:        String  = "",
    val name:      String  = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("created_at") val createdAt: String  = "",
)

data class BlockedListData(val blocks: List<BlockedUserDto> = emptyList())
data class BlockStateData(val blocked: Boolean = false)

interface ModerationApiService {

    /** POST /moderation/report — flag content or a user for review. */
    @POST("moderation/report")
    suspend fun report(@Body body: ReportRequest): ApiResponse<ReportData>

    /** GET /moderation/blocks — everyone this user has blocked. */
    @GET("moderation/blocks")
    suspend fun listBlocked(): ApiResponse<BlockedListData>

    /** POST /moderation/block/{userId} — idempotent. */
    @POST("moderation/block/{userId}")
    suspend fun block(@Path("userId") userId: String): ApiResponse<BlockStateData>

    /** DELETE /moderation/block/{userId} — idempotent. */
    @DELETE("moderation/block/{userId}")
    suspend fun unblock(@Path("userId") userId: String): ApiResponse<BlockStateData>
}
