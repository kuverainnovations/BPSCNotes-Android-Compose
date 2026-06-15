package com.example.bpscnotes.presentation.studymaterials

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.ChatThreadDto
import com.example.bpscnotes.data.remote.api.MaterialChatApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// ChatInboxViewModel — Phase 5
//
// Lists all of the current user's material chat threads, whether
// they're the buyer or the uploader. This is how an uploader
// discovers that a buyer has messaged them about a material.
// ════════════════════════════════════════════════════════════

data class ChatInboxUiState(
    val isLoading: Boolean = true,
    val threads: List<ChatThreadDto> = emptyList(),
    val error: String? = null,
)

private const val TAG = "ChatInboxVM"

@HiltViewModel
class ChatInboxViewModel @Inject constructor(
    private val api: MaterialChatApiService,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatInboxUiState())
    val state: StateFlow<ChatInboxUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.listThreads()
                _state.update { it.copy(
                    isLoading = false,
                    threads = res.data?.threads ?: emptyList(),
                )}
            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load chats")) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
