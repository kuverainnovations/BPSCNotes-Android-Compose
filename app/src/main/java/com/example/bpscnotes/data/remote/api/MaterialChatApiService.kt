package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ════════════════════════════════════════════════════════════
// FILE: data/remote/api/MaterialChatApiService.kt
// Phase 5 — buyer <-> uploader chat scoped to a material, plus
// "Report / Escalate to Support".
// ════════════════════════════════════════════════════════════

// ── DTOs ────────────────────────────────────────────────────

data class MaterialChatDto(
    val id: String,
    @SerializedName("material_id")    val materialId: String,
    @SerializedName("material_title") val materialTitle: String? = null,
    @SerializedName("buyer_id")       val buyerId: String,
    @SerializedName("uploader_id")    val uploaderId: String,
    val status: String = "open",           // "open" | "escalated" | "closed"
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("created_at")      val createdAt: String? = null,
)

data class ChatThreadDto(
    val id: String,
    @SerializedName("material_id")     val materialId: String,
    @SerializedName("material_title")  val materialTitle: String? = null,
    @SerializedName("buyer_id")        val buyerId: String,
    @SerializedName("uploader_id")     val uploaderId: String,
    val status: String = "open",
    @SerializedName("other_party_name") val otherPartyName: String? = null,
    val role: String = "buyer",            // "buyer" | "uploader" — current user's role in this thread
    @SerializedName("last_message")    val lastMessage: String? = null,
    @SerializedName("unread_count")    val unreadCount: Int = 0,
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("created_at")      val createdAt: String? = null,
)

data class ChatThreadsData(
    val threads: List<ChatThreadDto> = emptyList(),
)

data class MaterialChatMessageDto(
    val id: String,
    @SerializedName("sender_id") val senderId: String,
    val message: String,
    @SerializedName("is_read")   val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class ChatThreadDetailData(
    val chat: ChatThreadDto,
    val messages: List<MaterialChatMessageDto> = emptyList(),
)

data class SendChatMessageRequest(val message: String)

data class EscalateChatRequest(
    val category: String,   // "refund" | "dispute" | "content" | "seller_misconduct" | "other"
    val reason: String,
)

data class EscalateChatData(
    val escalationId: String? = null,
    val alreadyOpen: Boolean = false,
)

// ── Retrofit interface ─────────────────────────────────────────

interface MaterialChatApiService {

    /** POST /study-materials/:id/chat — get-or-create a chat thread (buyer only) */
    @POST("study-materials/{id}/chat")
    suspend fun getOrCreateThread(@Path("id") materialId: String): ApiResponse<MaterialChatDto>

    /** GET /material-chats — inbox of all threads (as buyer or uploader) */
    @GET("material-chats")
    suspend fun listThreads(): ApiResponse<ChatThreadsData>

    /** GET /material-chats/:chatId — thread + message history */
    @GET("material-chats/{chatId}")
    suspend fun getThread(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<ChatThreadDetailData>

    /** POST /material-chats/:chatId/messages — REST fallback for sending a message */
    @POST("material-chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body body: SendChatMessageRequest,
    ): ApiResponse<MaterialChatMessageDto>

    /** POST /material-chats/:chatId/escalate — report to support */
    @POST("material-chats/{chatId}/escalate")
    suspend fun escalate(
        @Path("chatId") chatId: String,
        @Body body: EscalateChatRequest,
    ): ApiResponse<EscalateChatData>
}