package com.example.bpscnotes.presentation.readingrooms

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingRoomsUiState(
    val rooms: List<StudyRoomDto>  = emptyList(),
    val isLoading: Boolean         = true,
    val isJoining: Boolean         = false,
    val error: String?             = null,
    val successMessage: String?    = null
)

@HiltViewModel
class ReadingRoomsViewModel @Inject constructor(
    private val api: StudyRoomsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingRoomsUiState())
    val uiState: StateFlow<ReadingRoomsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(subject: String? = null, search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.rooms.isEmpty(), error = null) }
            try {
                val response = api.getRooms(subject = subject?.takeIf { it.isNotBlank() }, search = search?.takeIf { it.isNotBlank() })
                _uiState.update { it.copy(rooms = response.data?.rooms ?: emptyList(), isLoading = false) }
            } catch (e: Exception) {
                Log.e("ReadingRoomsVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load study rooms") }
            }
        }
    }

    fun joinRoom(roomId: String, joinCode: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }
            try {
                api.joinRoom(roomId, JoinRoomRequest(joinCode = joinCode))
                _uiState.update { it.copy(isJoining = false, successMessage = "Joined room! 🎉") }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, error = e.message ?: "Failed to join room") }
            }
        }
    }

    fun createRoom(body: CreateRoomRequest, onSuccess: (StudyRoomDto) -> Unit) {
        viewModelScope.launch {
            try {
                val result = api.createRoom(body).data
                if (result != null) {
                    _uiState.update { s -> s.copy(rooms = listOf(result) + s.rooms) }
                    onSuccess(result)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create room") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null, error = null) } }
    fun retry() = load()
}
