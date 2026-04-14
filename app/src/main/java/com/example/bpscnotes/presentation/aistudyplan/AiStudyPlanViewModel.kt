package com.example.bpscnotes.presentation.aistudyplan

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
import javax.inject.Inject

// ── Models ───────────────────────────────────────────────────

data class StudyTask(
    val subject: String,
    val topic: String,
    val durationMinutes: Int,
    val priority: String,       // "High", "Medium", "Low"
    val tip: String,
    var isCompleted: Boolean = false
)

data class StudyDay(
    val day: String,
    val focus: String,
    val tasks: List<StudyTask>,
    val totalMinutes: Int
)

data class StudyPlanUiState(
    val days: List<StudyDay> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val completedTasks: Set<String> = emptySet()
)

// Subject progress input — pass from your existing profile/dashboard data
data class SubjectProgress(
    val subject: String,
    val progressPercent: Int    // e.g. Polity=82, History=65, Geography=48
)

// ── ViewModel ────────────────────────────────────────────────

@HiltViewModel
class AiStudyPlanViewModel @Inject constructor(
    private val claudeApi: ClaudeApiService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(StudyPlanUiState())
    val uiState: StateFlow<StudyPlanUiState> = _uiState.asStateFlow()

    fun generatePlan(
        subjectProgress: List<SubjectProgress>,
        dailyHours: Int = 4,
        daysUntilExam: Int = 90
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = fetchPlan(subjectProgress, dailyHours, daysUntilExam)
            result.fold(
                onSuccess = { days -> _uiState.update { it.copy(days = days, isLoading = false) } },
                onFailure = { e  -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun toggleTask(dayIndex: Int, taskIndex: Int) {
        val key = "$dayIndex-$taskIndex"
        _uiState.update { state ->
            val completed = state.completedTasks.toMutableSet()
            if (key in completed) completed.remove(key) else completed.add(key)
            state.copy(completedTasks = completed)
        }
    }

    fun isTaskCompleted(dayIndex: Int, taskIndex: Int) =
        "${dayIndex}-${taskIndex}" in _uiState.value.completedTasks

    private suspend fun fetchPlan(
        subjectProgress: List<SubjectProgress>,
        dailyHours: Int,
        daysUntilExam: Int
    ): Result<List<StudyDay>> = withContext(Dispatchers.IO) {
        try {
            val progressText = subjectProgress.joinToString(", ") {
                "${it.subject}: ${it.progressPercent}%"
            }

            val prompt = """
                Create a 7-day personalised BPSC study plan.
                
                Student's current subject progress:
                $progressText
                
                Available study time: $dailyHours hours/day
                Days until exam: $daysUntilExam days
                
                Focus more time on weaker subjects (lower percentage).
                
                Return ONLY a JSON array for 7 days in this exact format:
                [
                  {
                    "day": "Monday",
                    "focus": "Main focus area for the day",
                    "tasks": [
                      {
                        "subject": "Polity",
                        "topic": "Fundamental Rights",
                        "durationMinutes": 60,
                        "priority": "High",
                        "tip": "One quick study tip for this topic"
                      }
                    ],
                    "totalMinutes": 240
                  }
                ]
                
                Rules:
                - Each day should have 3-5 tasks
                - Total daily minutes should match $dailyHours hours = ${dailyHours * 60} minutes
                - Priority: "High" for weak subjects, "Medium" for average, "Low" for strong
                - Include Current Affairs every day (15-20 min)
                - Vary topics across days — don't repeat same topic
                - Return ONLY the JSON array
            """.trimIndent()

            val response = claudeApi.sendMessage(
                ClaudeRequestDto(
                    system = "You are a BPSC exam strategy expert. Return only valid JSON arrays as instructed.",
                    messages = listOf(ClaudeMessageDto(role = "user", content = prompt))
                )
            )

            val raw = response.content.firstOrNull()?.text ?: throw Exception("Empty response")
            val jsonStart = raw.indexOf('[')
            val jsonEnd   = raw.lastIndexOf(']') + 1
            val arr = JSONArray(raw.substring(jsonStart, jsonEnd))

            val days = (0 until arr.length()).map { i ->
                val dayObj   = arr.getJSONObject(i)
                val tasksArr = dayObj.getJSONArray("tasks")
                val tasks = (0 until tasksArr.length()).map { j ->
                    val t = tasksArr.getJSONObject(j)
                    StudyTask(
                        subject         = t.getString("subject"),
                        topic           = t.getString("topic"),
                        durationMinutes = t.getInt("durationMinutes"),
                        priority        = t.getString("priority"),
                        tip             = t.getString("tip")
                    )
                }
                StudyDay(
                    day          = dayObj.getString("day"),
                    focus        = dayObj.getString("focus"),
                    tasks        = tasks,
                    totalMinutes = dayObj.getInt("totalMinutes")
                )
            }
            Result.success(days)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
