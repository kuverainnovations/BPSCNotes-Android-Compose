package com.example.bpscnotes.presentation.aimcq

import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.data.remote.api.ClaudeApiService
import com.example.bpscnotes.data.remote.dto.ClaudeMessageDto
import com.example.bpscnotes.data.remote.dto.ClaudeRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

// ── Data models ──────────────────────────────────────────────

data class AiMcqQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class AiMcqUiState(
    val questions: List<AiMcqQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val score: Int = 0,
    val isLoading: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null,
    val subject: String = "",
    val topic: String = ""
)

// ── ViewModel ────────────────────────────────────────────────

@HiltViewModel
class AiMcqViewModel @Inject constructor(
    private val claudeApi: ClaudeApiService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AiMcqUiState())
    val uiState: StateFlow<AiMcqUiState> = _uiState.asStateFlow()

    fun generateQuestions(subject: String, topic: String) {
        _uiState.update { it.copy(isLoading = true, error = null, subject = subject, topic = topic, questions = emptyList(), currentIndex = 0, score = 0, isFinished = false) }

        viewModelScope.launch {
            val result = fetchMcqs(subject, topic)
            result.fold(
                onSuccess = { questions ->
                    _uiState.update { it.copy(questions = questions, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun selectAnswer(index: Int) {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.update { it.copy(selectedAnswer = index, isAnswerRevealed = true) }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val isCorrect = state.selectedAnswer == state.questions[state.currentIndex].correctIndex
        val newScore = if (isCorrect) state.score + 1 else state.score
        val nextIndex = state.currentIndex + 1
        val finished = nextIndex >= state.questions.size

        _uiState.update {
            it.copy(
                score = newScore,
                currentIndex = nextIndex,
                selectedAnswer = null,
                isAnswerRevealed = false,
                isFinished = finished
            )
        }
    }

    fun retry() {
        val state = _uiState.value
        generateQuestions(state.subject, state.topic)
    }

    private suspend fun fetchMcqs(subject: String, topic: String): Result<List<AiMcqQuestion>> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Generate exactly 5 multiple choice questions for BPSC exam preparation.
                    Subject: $subject
                    Topic: $topic
                    
                    Return ONLY a valid JSON array, no other text, in this exact format:
                    [
                      {
                        "question": "Question text here?",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctIndex": 0,
                        "explanation": "Brief explanation why this is correct."
                      }
                    ]
                    
                    Rules:
                    - Questions must be BPSC exam style (factual, direct)
                    - correctIndex is 0-based (0=A, 1=B, 2=C, 3=D)
                    - Make questions Bihar/India specific where relevant
                    - One question should be Easy, two Medium, two Hard
                    - Return ONLY the JSON array, nothing else
                """.trimIndent()

                val response = claudeApi.sendMessage(
                    ClaudeRequestDto(
                        system = "You are a BPSC exam question generator. Return only valid JSON arrays as instructed. Never add preamble or explanation outside the JSON.",
                        messages = listOf(ClaudeMessageDto(role = "user", content = prompt))
                    )
                )

                val raw = response.content.firstOrNull()?.text ?: throw Exception("Empty response")

                // Parse JSON
                val jsonStart = raw.indexOf('[')
                val jsonEnd   = raw.lastIndexOf(']') + 1
                val jsonStr   = raw.substring(jsonStart, jsonEnd)
                val jsonArray = JSONArray(jsonStr)

                val questions = (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    val opts = obj.getJSONArray("options")
                    AiMcqQuestion(
                        question     = obj.getString("question"),
                        options      = (0 until opts.length()).map { opts.getString(it) },
                        correctIndex = obj.getInt("correctIndex"),
                        explanation  = obj.getString("explanation")
                    )
                }

                Result.success(questions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
