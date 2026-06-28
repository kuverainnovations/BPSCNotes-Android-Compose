package com.example.bpscnotes.presentation.rooms

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.GlobalLeaderboardApiService
import com.example.bpscnotes.data.remote.api.LeaderboardData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalLeaderboardUiState(
    val data: LeaderboardData? = null,
    val isLoading: Boolean     = true,
    val error: String?         = null,
    val selectedType: String   = "coins",
)

@HiltViewModel
class GlobalLeaderboardViewModel @Inject constructor(
    private val api: GlobalLeaderboardApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalLeaderboardUiState())
    val uiState: StateFlow<GlobalLeaderboardUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "GlobalLeaderboardVM" }

    init { load("coins") }

    fun selectType(type: String) {
        if (_uiState.value.selectedType == type) return
        _uiState.update { it.copy(selectedType = type) }
        load(type)
    }

    fun load(type: String = _uiState.value.selectedType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getLeaderboard(type = type)
                _uiState.update { it.copy(data = res.data, isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load leaderboard")) }
            }
        }
    }
}
