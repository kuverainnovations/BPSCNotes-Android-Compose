package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.remote.api.ClaudeApiService
import com.example.bpscnotes.data.remote.dto.ClaudeMessageDto
import com.example.bpscnotes.data.remote.dto.ClaudeRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val role: String,       // "user" or "assistant"
    val content: String,
    val isLoading: Boolean = false
)

@Singleton
class AiDoubtRepository @Inject constructor(
    private val claudeApi: ClaudeApiService
) {

    // BPSC-specific system prompt — the secret sauce
    private val systemPrompt = """
        You are an expert BPSC (Bihar Public Service Commission) exam tutor with deep knowledge of:
        - Bihar General Knowledge and Bihar Special topics
        - Indian Polity and Constitution
        - Indian and World History
        - Indian and World Geography
        - Indian Economy and Bihar Economy
        - General Science
        - Current Affairs relevant to BPSC

        Rules you must follow:
        1. Only answer questions related to BPSC exam preparation. If asked unrelated questions, politely redirect.
        2. Give clear, simple explanations that a student can easily understand.
        3. Always end your answer with: "📝 Likely BPSC Question: [one probable exam question on this topic]"
        4. Detect the language of the student's question — if they write in Hindi, reply in Hindi. If English, reply in English.
        5. Keep answers concise (under 250 words) unless the student asks for detailed explanation.
        6. Use bullet points and structure for complex topics.
        7. When giving facts, mention if they are important for BPSC exam.
        8. Be encouraging and motivating — students are preparing for a tough exam.
    """.trimIndent()

    suspend fun askDoubt(
        question: String,
        chatHistory: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Build message history for context (last 10 messages max to save tokens)
            val historyMessages = chatHistory
                .takeLast(10)
                .filter { !it.isLoading }
                .map { ClaudeMessageDto(role = it.role, content = it.content) }

            // Add current question
            val messages = historyMessages + ClaudeMessageDto(
                role = "user",
                content = question
            )

            val response = claudeApi.sendMessage(
                ClaudeRequestDto(
                    system = systemPrompt,
                    messages = messages
                )
            )

            val answer = response.content.firstOrNull()?.text
                ?: "Sorry, I could not generate a response. Please try again."

            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
