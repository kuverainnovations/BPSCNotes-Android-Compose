package com.example.bpscnotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClaudeRequestDto(
    val model: String = "claude-sonnet-4-20250514",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val system: String,
    val messages: List<ClaudeMessageDto>
)

data class ClaudeMessageDto(
    val role: String,   // "user" or "assistant"
    val content: String
)

data class ClaudeResponseDto(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContentDto>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: ClaudeUsageDto
)

data class ClaudeContentDto(
    val type: String,   // "text"
    val text: String
)

data class ClaudeUsageDto(
    @SerializedName("input_tokens")  val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)
