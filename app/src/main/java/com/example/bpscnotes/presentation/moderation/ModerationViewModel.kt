package com.example.bpscnotes.presentation.moderation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.BlockedUserDto
import com.example.bpscnotes.data.remote.api.ModerationApiService
import com.example.bpscnotes.data.remote.api.ReportRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// ModerationViewModel — one shared VM behind every report/block
// action in the app.
//
// Deliberately not per-screen: reporting is identical wherever it is
// offered (chat, peer review, marketplace), and every screen that offers
// it would otherwise repeat the same four calls. Screens scope this to
// themselves with hiltViewModel() and read `state` for the in-flight and
// confirmation UI.
// ════════════════════════════════════════════════════════════

data class ModerationState(
    val isSubmitting: Boolean = false,
    /** One-shot confirmation to surface as a snackbar, then clear. */
    val message: String? = null,
    val error: String? = null,
    // Blocked-user management (Settings → Blocked users)
    val isLoadingBlocks: Boolean = false,
    val blocked: List<BlockedUserDto> = emptyList(),
    /** Ids acted on this session — lets a list row show "Blocked" instantly. */
    val blockedIds: Set<String> = emptySet(),
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val api: ModerationApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(ModerationState())
    val state: StateFlow<ModerationState> = _state.asStateFlow()

    fun clearMessages() = _state.update { it.copy(message = null, error = null) }

    /**
     * Submit a report.
     *
     * [onDone] fires only on success so the caller can close its sheet; a
     * failure leaves the sheet open with the text intact, because a report
     * the user has typed out should never be silently thrown away.
     */
    fun report(
        contentType: String,
        contentId: String,
        reason: String,
        details: String? = null,
        onDone: () -> Unit = {},
    ) {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                val res = api.report(
                    ReportRequest(
                        contentType = contentType,
                        contentId   = contentId,
                        reason      = reason,
                        details     = details?.trim()?.takeIf { d -> d.isNotBlank() },
                    )
                )
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        message = res.data?.message
                            ?: "Thanks — our team will review this within 24 hours.",
                    )
                }
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "report: ${e.message}", e)
                _state.update {
                    it.copy(isSubmitting = false, error = "Could not send the report. Please try again.")
                }
            }
        }
    }

    fun block(userId: String, displayName: String? = null, onDone: () -> Unit = {}) {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                api.block(userId)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        blockedIds   = it.blockedIds + userId,
                        message      = "${displayName ?: "This user"} is blocked. You won't see their content.",
                    )
                }
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "block: ${e.message}", e)
                _state.update { it.copy(isSubmitting = false, error = "Could not block. Please try again.") }
            }
        }
    }

    fun unblock(userId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                api.unblock(userId)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        blocked      = it.blocked.filterNot { b -> b.id == userId },
                        blockedIds   = it.blockedIds - userId,
                        message      = "Unblocked. Their content will appear again.",
                    )
                }
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "unblock: ${e.message}", e)
                _state.update { it.copy(isSubmitting = false, error = "Could not unblock. Please try again.") }
            }
        }
    }

    fun loadBlocked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBlocks = true) }
            try {
                val res = api.listBlocked()
                val list = res.data?.blocks.orEmpty()
                _state.update {
                    it.copy(
                        isLoadingBlocks = false,
                        blocked         = list,
                        blockedIds      = list.map { b -> b.id }.toSet(),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadBlocked: ${e.message}", e)
                _state.update {
                    it.copy(isLoadingBlocks = false, error = "Could not load your blocked list.")
                }
            }
        }
    }

    private companion object { const val TAG = "ModerationVM" }
}
