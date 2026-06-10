package com.example.bpscnotes.presentation.auth.mpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.core.notifications.FcmTokenManager
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MpinUiState(
    val isLoading: Boolean         = false,
    val error: String?             = null,
    // Navigation signals
    val navigateToMain: Boolean    = false,
    val navigateToRegister: String? = null,   // tempToken for new user (shouldn't occur in MPIN flow)
    val navigateToCreateMpin: Boolean = false, // after OTP verify for existing user without MPIN
    // Lockout countdown
    val isLocked: Boolean          = false,
    val lockedSecondsLeft: Int     = 0,
    // MPIN entry state (6 digits, tracked as index cursor)
    val mpinDigits: List<String>   = List(6) { "" },
    // Confirm MPIN (for create / reset screens)
    val confirmDigits: List<String> = List(6) { "" },
    val confirmError: String?      = null,
    val mpinCreated: Boolean       = false,
)

@HiltViewModel
class MpinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val fcmTokenManager: FcmTokenManager,
    private val cacheInvalidator: CacheInvalidator,
) : ViewModel() {

    private val _state = MutableStateFlow(MpinUiState())
    val state: StateFlow<MpinUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    // ── Numpad input helpers ────────────────────────────────────
    fun onMpinDigit(digit: String) {
        val current = _state.value.mpinDigits.toMutableList()
        val nextEmpty = current.indexOfFirst { it.isEmpty() }
        if (nextEmpty == -1) return
        current[nextEmpty] = digit
        _state.update { it.copy(mpinDigits = current, error = null) }
    }

    fun onMpinBackspace() {
        val current = _state.value.mpinDigits.toMutableList()
        val lastFilled = current.indexOfLast { it.isNotEmpty() }
        if (lastFilled == -1) return
        current[lastFilled] = ""
        _state.update { it.copy(mpinDigits = current, error = null) }
    }

    fun onConfirmDigit(digit: String) {
        val current = _state.value.confirmDigits.toMutableList()
        val nextEmpty = current.indexOfFirst { it.isEmpty() }
        if (nextEmpty == -1) return
        current[nextEmpty] = digit
        _state.update { it.copy(confirmDigits = current, confirmError = null) }
    }

    fun onConfirmBackspace() {
        val current = _state.value.confirmDigits.toMutableList()
        val lastFilled = current.indexOfLast { it.isNotEmpty() }
        if (lastFilled == -1) return
        current[lastFilled] = ""
        _state.update { it.copy(confirmDigits = current, confirmError = null) }
    }

    fun clearMpin() {
        _state.update { it.copy(mpinDigits = List(6) { "" }, confirmDigits = List(6) { "" }, error = null, confirmError = null) }
    }

    private fun mpin() = _state.value.mpinDigits.joinToString("")
    private fun confirmMpin() = _state.value.confirmDigits.joinToString("")

    // ── Login with MPIN ────────────────────────────────────────
    fun loginWithMpin(mobile: String) {
        val pin = mpin()
        if (pin.length < 6) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authRepository.loginMpin(mobile, pin)
                if (response.success) {
                    tokenStore.saveUserMobile(mobile)
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (_: Exception) {}
                    cacheInvalidator.evict()
                    _state.update { it.copy(isLoading = false, navigateToMain = true) }
                } else {
                    val msg = response.message
                    // Parse lockout from message
                    val locked = msg.contains("locked", ignoreCase = true) ||
                                 msg.contains("Too many", ignoreCase = true)
                    _state.update { it.copy(isLoading = false, error = msg, isLocked = locked) }
                    if (locked) startCountdown()
                    clearMpin()
                }
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                val msg = try {
                    org.json.JSONObject(body).optString("message", "Login failed")
                } catch (_: Exception) { "Login failed" }
                val locked = msg.contains("locked", ignoreCase = true) ||
                             msg.contains("Too many", ignoreCase = true)
                _state.update { it.copy(isLoading = false, error = msg, isLocked = locked) }
                if (locked) startCountdown()
                clearMpin()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                clearMpin()
            }
        }
    }

    // ── Create MPIN (after registration) ──────────────────────
    fun createMpin() {
        val pin     = mpin()
        val confirm = confirmMpin()
        if (pin.length < 6) { _state.update { it.copy(error = "Enter all 6 digits") }; return }
        if (pin != confirm)  { _state.update { it.copy(confirmError = "MPINs do not match") }; clearMpin(); return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = authRepository.createMpin(pin)
                if (result.mpinCreated) {
                    _state.update { it.copy(isLoading = false, mpinCreated = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Failed to create MPIN") }
                    clearMpin()
                }
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                val msg = try { org.json.JSONObject(body).optString("message", "Failed") } catch (_: Exception) { "Failed" }
                _state.update { it.copy(isLoading = false, error = msg) }
                clearMpin()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to create MPIN") }
                clearMpin()
            }
        }
    }

    // ── Reset MPIN (after OTP verify — forgot flow) ───────────
    fun resetMpin(mobile: String, otp: String) {
        val pin     = mpin()
        val confirm = confirmMpin()
        if (pin.length < 6) { _state.update { it.copy(error = "Enter all 6 digits") }; return }
        if (pin != confirm)  { _state.update { it.copy(confirmError = "MPINs do not match") }; clearMpin(); return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authRepository.resetMpin(mobile, otp, pin)
                if (response.success) {
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (_: Exception) {}
                    cacheInvalidator.evict()
                    _state.update { it.copy(isLoading = false, mpinCreated = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = response.message) }
                    clearMpin()
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Reset failed") }
                clearMpin()
            }
        }
    }

    // ── Lockout countdown ─────────────────────────────────────
    fun initLockout(secondsLeft: Int) {
        _state.update { it.copy(isLocked = true, lockedSecondsLeft = secondsLeft) }
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.lockedSecondsLeft > 0) {
                delay(1000L)
                _state.update { it.copy(lockedSecondsLeft = it.lockedSecondsLeft - 1) }
            }
            _state.update { it.copy(isLocked = false, lockedSecondsLeft = 0, error = null) }
        }
    }

    fun consumeNavigateToMain()  { _state.update { it.copy(navigateToMain = false) } }
    fun consumeMpinCreated()     { _state.update { it.copy(mpinCreated = false) } }
    fun clearError()             { _state.update { it.copy(error = null, confirmError = null) } }

    override fun onCleared() { super.onCleared(); countdownJob?.cancel() }
}
