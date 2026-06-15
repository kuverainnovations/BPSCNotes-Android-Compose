package com.example.bpscnotes.presentation.studymaterials

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.ChatNewMessageEvent
import com.example.bpscnotes.data.remote.MaterialChatSocketManager
import com.example.bpscnotes.data.remote.api.MaterialChatMessageDto
import com.example.bpscnotes.data.remote.api.ChatThreadDto
import com.example.bpscnotes.data.remote.api.EscalateChatRequest
import com.example.bpscnotes.data.remote.api.MaterialChatApiService
import com.example.bpscnotes.data.remote.api.SendChatMessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// MaterialChatViewModel — Phase 5
//
// Drives MaterialChatScreen: loads the thread + message history,
// connects to the /material-chat WebSocket for real-time delivery,
// sends messages (WS primary, REST fallback), and handles the
// "Report / Escalate to Support" flow.
// ════════════════════════════════════════════════════════════

data class MaterialChatUiState(
    val isLoading: Boolean = true,
    val chat: ChatThreadDto? = null,
    val messages: List<MaterialChatMessageDto> = emptyList(),
    val currentUserId: String = "",
    val isSending: Boolean = false,
    val inputText: String = "",
    val error: String? = null,

    // Escalation
    val showEscalateSheet: Boolean = false,
    val isEscalating: Boolean = false,
    val escalationCategory: String = "other",
    val escalationReason: String = "",
    val escalationSuccess: String? = null,
)

private const val TAG = "MaterialChatVM"

@HiltViewModel
class MaterialChatViewModel @Inject constructor(
    private val api: MaterialChatApiService,
    private val socketManager: MaterialChatSocketManager,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MaterialChatUiState())
    val state: StateFlow<MaterialChatUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(currentUserId = tokenStore.getUserId() ?: "") }

        viewModelScope.launch {
            socketManager.newMessages.collect { event ->
                onIncomingMessage(event)
            }
        }
    }

    // ── Load thread + connect socket ────────────────────────────
    fun loadThread(chatId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getThread(chatId)
                val data = res.data
                _state.update { it.copy(
                    isLoading = false,
                    chat = data?.chat,
                    messages = data?.messages ?: emptyList(),
                )}
                socketManager.connect()
                socketManager.joinChat(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "loadThread: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load chat")) }
            }
        }
    }

    private fun onIncomingMessage(event: ChatNewMessageEvent) {
        val chatId = _state.value.chat?.id ?: return
        if (event.chatId != chatId) return

        // Avoid duplicates — a message we sent ourselves may already be
        // in the list from sendMessage()'s optimistic update.
        if (_state.value.messages.any { it.id == event.id }) return

        _state.update { it.copy(
            messages = it.messages + MaterialChatMessageDto(
                id = event.id,
                senderId = event.senderId,
                message = event.message,
                isRead = false,
                createdAt = event.createdAt,
            )
        )}
    }

    // ── Input ────────────────────────────────────────────────────
    fun setInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    // ── Send a message ──────────────────────────────────────────
    // Primary path: WebSocket (instant, broadcasts to both parties).
    // The server echoes our own message back via chat:new_message,
    // so we don't need an optimistic local insert — but if the socket
    // is disconnected, fall back to REST so the message isn't lost.
    fun sendMessage() {
        val chatId = _state.value.chat?.id ?: return
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return
        if (text.length > 1000) {
            _state.update { it.copy(error = "Message too long (max 1000 characters)") }
            return
        }

        _state.update { it.copy(inputText = "") }

        if (socketManager.isConnected.value) {
            socketManager.sendMessage(text)
        } else {
            // REST fallback — optimistic insert since there's no socket echo
            viewModelScope.launch {
                _state.update { it.copy(isSending = true) }
                try {
                    val res = api.sendMessage(chatId, SendChatMessageRequest(text))
                    res.data?.let { msg ->
                        _state.update { s -> s.copy(messages = s.messages + msg, isSending = false) }
                    } ?: _state.update { it.copy(isSending = false) }
                } catch (e: Exception) {
                    _state.update { it.copy(isSending = false, error = e.toUserMessage("Failed to send"), inputText = text) }
                }
            }
        }
    }

    // ── Escalation ────────────────────────────────────────────────
    fun openEscalateSheet() = _state.update { it.copy(showEscalateSheet = true, escalationSuccess = null) }
    fun closeEscalateSheet() = _state.update { it.copy(showEscalateSheet = false) }
    fun setEscalationCategory(category: String) = _state.update { it.copy(escalationCategory = category) }
    fun setEscalationReason(reason: String) = _state.update { it.copy(escalationReason = reason) }

    fun submitEscalation() {
        val chatId = _state.value.chat?.id ?: return
        val reason = _state.value.escalationReason.trim()
        if (reason.isEmpty()) {
            _state.update { it.copy(error = "Please describe the issue") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isEscalating = true) }
            try {
                val res = api.escalate(chatId, EscalateChatRequest(
                    category = _state.value.escalationCategory,
                    reason = reason,
                ))
                _state.update { it.copy(
                    isEscalating = false,
                    showEscalateSheet = false,
                    escalationSuccess = res.message ?: "Escalated to support",
                    chat = it.chat?.copy(status = "escalated"),
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isEscalating = false, error = e.toUserMessage("Failed to escalate")) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearEscalationSuccess() = _state.update { it.copy(escalationSuccess = null) }

    override fun onCleared() {
        super.onCleared()
        socketManager.leaveChat()
    }
}