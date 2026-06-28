package com.example.bpscnotes.presentation.studysessions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.StudySessionHistoryApiService
import com.example.bpscnotes.data.remote.api.StudySessionHistoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudySessionHistoryUiState(
    val sessions:  List<StudySessionHistoryDto> = emptyList(),
    val isLoading: Boolean                      = true,
    val error:     String?                      = null,
    // computed totals
    val totalSecs: Int                          = 0,
    val totalXp:   Int                          = 0,
)

@HiltViewModel
class StudySessionHistoryViewModel @Inject constructor(
    private val api: StudySessionHistoryApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudySessionHistoryUiState())
    val uiState: StateFlow<StudySessionHistoryUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "StudySessionHistoryVM" }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getSessions()
                val sessions = res.data?.sessions ?: emptyList()
                _uiState.update {
                    it.copy(
                        sessions  = sessions,
                        isLoading = false,
                        totalSecs = sessions.sumOf { s -> s.durationSecs },
                        totalXp   = sessions.sumOf { s -> s.xpEarned },
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load study sessions")) }
            }
        }
    }
}
