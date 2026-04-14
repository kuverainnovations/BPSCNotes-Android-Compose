package com.example.bpscnotes.presentation.aisummary

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
import javax.inject.Inject

data class CaSummaryPoint(
    val headline: String,
    val summary: String,
    val bpscRelevance: String,
    val likelyQuestion: String
)

data class CaSummaryUiState(
    val points: List<CaSummaryPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val date: String = "",
    val category: String = "All"
)

@HiltViewModel
class CaSummaryViewModel @Inject constructor(
    private val claudeApi: ClaudeApiService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CaSummaryUiState())
    val uiState: StateFlow<CaSummaryUiState> = _uiState.asStateFlow()

    // Call this with the raw current affairs headlines from your existing CA screen
    fun summarise(headlines: List<String>, date: String, category: String = "All") {
        if (headlines.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, error = null, date = date, category = category) }

        viewModelScope.launch {
            val result = fetchSummary(headlines, category)
            result.fold(
                onSuccess = { points -> _uiState.update { it.copy(points = points, isLoading = false) } },
                onFailure = { e  -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun retry(headlines: List<String>, date: String, category: String) {
        summarise(headlines, date, category)
    }

    private suspend fun fetchSummary(headlines: List<String>, category: String): Result<List<CaSummaryPoint>> =
        withContext(Dispatchers.IO) {
            try {
                val headlineText = headlines.take(10).joinToString("\n") { "- $it" }

                val prompt = """
                    You are a BPSC exam coach. Analyse these current affairs headlines and create BPSC-focused summaries.
                    
                    Headlines:
                    $headlineText
                    
                    For each headline return a JSON array with this format:
                    [
                      {
                        "headline": "Short headline (max 8 words)",
                        "summary": "2-3 sentence explanation of what happened and why it matters",
                        "bpscRelevance": "Why this is important for BPSC exam in one sentence",
                        "likelyQuestion": "One probable BPSC MCQ question based on this news"
                      }
                    ]
                    
                    Focus on: Bihar news, India-Bihar relations, Government schemes, Economy, Polity, Environment.
                    Return ONLY the JSON array.
                """.trimIndent()

                val response = claudeApi.sendMessage(
                    ClaudeRequestDto(
                        system = "You are a BPSC exam current affairs analyst. Return only valid JSON arrays.",
                        messages = listOf(ClaudeMessageDto(role = "user", content = prompt))
                    )
                )

                val raw = response.content.firstOrNull()?.text ?: throw Exception("Empty response")
                val jsonStart = raw.indexOf('[')
                val jsonEnd   = raw.lastIndexOf(']') + 1
                val jsonStr   = raw.substring(jsonStart, jsonEnd)
                val arr = org.json.JSONArray(jsonStr)

                val points = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    CaSummaryPoint(
                        headline       = obj.getString("headline"),
                        summary        = obj.getString("summary"),
                        bpscRelevance  = obj.getString("bpscRelevance"),
                        likelyQuestion = obj.getString("likelyQuestion")
                    )
                }
                Result.success(points)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
