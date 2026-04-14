package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.ClaudeRequestDto
import com.example.bpscnotes.data.remote.dto.ClaudeResponseDto
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApiService {

    @Headers(
        "anthropic-version: 2023-06-01",
        "content-type: application/json"
    )
    @POST("messages")
    suspend fun sendMessage(
        @Body request: ClaudeRequestDto
    ): ClaudeResponseDto
}
