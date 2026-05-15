package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ════════════════════════════════════════════════════════════
// ChatApiService — REST endpoint for chat history
// Used on ChatSheet open to load previous messages before
// live WS messages start arriving.
// ════════════════════════════════════════════════════════════

data class ChatMessageDto(
    val id:         String,
    @SerializedName("senderId")   val senderId:   String,
    @SerializedName("senderName") val senderName: String,
    val message:    String,
    @SerializedName("tierKey")    val tierKey:    String,
    @SerializedName("createdAt")  val createdAt:  String
)

data class ChatHistoryData(
    val messages: List<ChatMessageDto> = emptyList()
)

interface ChatApiService {
    /**
     * GET /rooms/tiers/{tierKey}/messages?limit=50
     * Returns last N messages for this tier room, oldest-first.
     * Called once when the chat sheet opens.
     */
    @GET("rooms/tiers/{tierKey}/messages")
    suspend fun getChatHistory(
        @Path("tierKey") tierKey: String,
        @Query("limit")  limit:   Int = 50
    ): ApiResponse<ChatHistoryData>
}
