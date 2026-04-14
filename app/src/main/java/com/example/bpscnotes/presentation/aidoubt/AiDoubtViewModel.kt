package com.example.bpscnotes.presentation.aidoubt

import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.domain.repository.AiDoubtRepository
import com.example.bpscnotes.domain.repository.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiDoubtUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dailyQuestionsUsed: Int = 0,        // Track usage for freemium
    val dailyLimit: Int = 10,               // Free tier: 10/day, Pro: unlimited
    val isPro: Boolean = false
)

@HiltViewModel
class AiDoubtViewModel @Inject constructor(
    private val repository: AiDoubtRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AiDoubtUiState())
    val uiState: StateFlow<AiDoubtUiState> = _uiState.asStateFlow()

    init {
        // Add welcome message
        _uiState.update { state ->
            state.copy(
                messages = listOf(
                    ChatMessage(
                        role = "assistant",
                        content = "नमस्ते! 👋 मैं आपका BPSC AI Tutor हूँ।\n\nआप मुझसे BPSC से related कोई भी doubt पूछ सकते हैं — Polity, History, Geography, Economy, Current Affairs, Bihar GK.\n\nHindi या English — जिस भाषा में comfortable हों, पूछें!\n\n*Free plan: ${_uiState.value.dailyLimit} questions/day*"
                    )
                )
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun sendMessage() {
        val question = _uiState.value.inputText.trim()
        if (question.isEmpty()) return

        // Check daily limit for free users
        if (!_uiState.value.isPro &&
            _uiState.value.dailyQuestionsUsed >= _uiState.value.dailyLimit) {
            _uiState.update {
                it.copy(
                    errorMessage = "Daily limit reached! Upgrade to Pro for unlimited questions. 🚀"
                )
            }
            return
        }

        // Add user message + loading placeholder
        val userMessage = ChatMessage(role = "user", content = question)
        val loadingMessage = ChatMessage(role = "assistant", content = "", isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + loadingMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null,
                dailyQuestionsUsed = state.dailyQuestionsUsed + 1
            )
        }

        viewModelScope.launch {
            val historyWithoutLoading = _uiState.value.messages
                .filter { !it.isLoading }
                .dropLast(0)  // include user message just added

            val result = repository.askDoubt(
                question = question,
                chatHistory = historyWithoutLoading.dropLast(1)  // exclude the loading msg
            )

            result.fold(
                onSuccess = { answer ->
                    _uiState.update { state ->
                        // Replace loading message with real answer
                        val updatedMessages = state.messages.dropLast(1) +
                                ChatMessage(role = "assistant", content = answer)
                        state.copy(messages = updatedMessages, isLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.dropLast(1) +
                                ChatMessage(
                                    role = "assistant",
                                    content = "⚠️ Something went wrong. Please check your internet and try again."
                                )
                        state.copy(
                            messages = updatedMessages,
                            isLoading = false,
                            errorMessage = error.message,
                            dailyQuestionsUsed = state.dailyQuestionsUsed - 1 // refund on error
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}