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
    val isLoading: Boolean          = false,
    val error: String?              = null,
    val navigateToMain: Boolean     = false,
    val navigateToRegister: String? = null,
    val navigateToCreateMpin: Boolean = false,
    val isLocked: Boolean           = false,
    val lockedSecondsLeft: Int      = 0,
    // 4-digit MPIN
    val mpinDigits: List<String>    = List(4) { "" },
    val confirmDigits: List<String> = List(4) { "" },
    val confirmError: String?       = null,
    val mpinCreated: Boolean        = false,
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
        _state.update { it.copy(mpinDigits = List(4) { "" }, confirmDigits = List(4) { "" }) }
    }

    private fun mpin()        = _state.value.mpinDigits.joinToString("")
    private fun confirmMpin() = _state.value.confirmDigits.joinToString("")

    // ── Login via biometric ────────────────────────────────────
    // Biometric proves local identity. If user has a valid JWT token,
    // navigate directly to Main without re-authenticating with backend.
    // If no token, fall back to MPIN entry.
    fun loginWithMpinViaBiometric(mobile: String) {
        viewModelScope.launch {
            val token = tokenStore.getToken()
            val savedMobile = tokenStore.getUserMobile()

            // Token must exist AND belong to the same mobile number
            val tokenBelongsToThisUser = !token.isNullOrBlank() &&
                    (savedMobile == mobile || savedMobile == "+91$mobile" ||
                            "+91$savedMobile" == mobile || savedMobile == mobile.removePrefix("+91"))

            if (!tokenBelongsToThisUser) {
                // No token or different user — must enter MPIN
                _state.update { it.copy(error = "Please enter your MPIN to continue.") }
                return@launch
            }

            // Validate token is still alive by calling /auth/me
            _state.update { it.copy(isLoading = true) }
            try {
                val user = authRepository.getMe()
                if (user != null) {
                    // Token is valid — proceed to Main
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (_: Exception) {}
                    cacheInvalidator.evictAll()
                    _state.update { it.copy(isLoading = false, navigateToMain = true) }
                } else {
                    // Token invalid — clear the session only and ask for MPIN.
                    // FIX: previously called tokenStore.clearAll(), a FULL WIPE
                    // (per TokenStore's own docs, intended for account
                    // deletion) that also erased is_onboarded/user_mobile/
                    // has_mpin/biometric_enabled. That made a transient
                    // /auth/me failure (or a genuinely expired token) wipe
                    // onboarding status too, so the NEXT cold start showed
                    // Onboarding again even though this is a returning user.
                    // clearSessionOnly() clears auth_token but preserves
                    // user_mobile/has_mpin/is_onboarded/biometric_enabled.
                    tokenStore.clearSessionOnly()
                    _state.update { it.copy(isLoading = false, error = "Session expired. Please enter your MPIN.") }
                }
            } catch (_: Exception) {
                // Network error or 401 — token expired (or request failed).
                // FIX: see above — clearSessionOnly(), not clearAll(), so a
                // transient network error doesn't wipe is_onboarded/has_mpin.
                tokenStore.clearSessionOnly()
                _state.update { it.copy(isLoading = false, error = "Session expired. Please enter your MPIN.") }
            }
        }
    }

    // ── Login with MPIN ────────────────────────────────────────
    fun loginWithMpin(mobile: String, pin: String? = null) {
        val actualPin = pin ?: mpin()
        if (actualPin.length < 4) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authRepository.loginMpin(mobile, actualPin)
                if (response.success) {
                    tokenStore.saveUserMobile(mobile)
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (_: Exception) {}
                    cacheInvalidator.evictAll()
                    _state.update { it.copy(isLoading = false, navigateToMain = true) }
                } else {
                    val msg = response.message
                    val locked = msg.contains("locked", ignoreCase = true) ||
                            msg.contains("Too many", ignoreCase = true)
                    _state.update { it.copy(isLoading = false, error = msg, isLocked = locked) }
                    if (locked) startCountdown()
                    clearMpin()
                }
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                val msg = try { org.json.JSONObject(body).optString("message", "Incorrect MPIN") } catch (_: Exception) { "Incorrect MPIN" }
                val locked = msg.contains("locked", ignoreCase = true) || msg.contains("Too many", ignoreCase = true)
                _state.update { it.copy(isLoading = false, error = msg, isLocked = locked) }
                if (locked) startCountdown()
                clearMpin()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                clearMpin()
            }
        }
    }

    fun createMpin() {
        val pin     = mpin()
        val confirm = confirmMpin()
        if (pin.length < 4) { _state.update { it.copy(error = "Enter all 4 digits") }; return }
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

    fun resetMpin(mobile: String, otp: String) {
        val pin     = mpin()
        val confirm = confirmMpin()
        if (pin.length < 4) { _state.update { it.copy(error = "Enter all 4 digits") }; return }
        if (pin != confirm)  { _state.update { it.copy(confirmError = "MPINs do not match") }; clearMpin(); return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authRepository.resetMpin(mobile, otp, pin)
                if (response.success) {
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (_: Exception) {}
                    cacheInvalidator.evictAll()
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
    /** Called on screen entry to wipe any stale navigation flags from previous session */
    fun resetState()             { _state.update { MpinUiState() } }

    override fun onCleared() { super.onCleared(); countdownJob?.cancel() }
}