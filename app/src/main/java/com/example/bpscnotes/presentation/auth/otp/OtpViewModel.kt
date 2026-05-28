package com.example.bpscnotes.presentation.auth.otp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : BaseViewModel() {

    /** true = existing user, token already saved → navigate to Main */
    private val _navigateToMain = MutableLiveData(false)
    val verifySuccess: LiveData<Boolean> = _navigateToMain

    /** non-null = new user → navigate to RegisterScreen with this tempToken */
    private val _navigateToRegister = MutableLiveData<String?>()
    val navigateToRegister: LiveData<String?> = _navigateToRegister

    private val _resendSuccess = MutableLiveData(false)
    val resendSuccess: LiveData<Boolean> = _resendSuccess

    fun verifyOtp(mobile: String, otp: String) {
        launchWithLoading {
            val response = authRepository.verifyOtp(mobile, otp)

            if (!response.success) {
                // Error is surfaced through BaseViewModel._error
                return@launchWithLoading
            }

            val data = response.data
            when {
                // ── Existing user — token already saved in repo ──────
                data != null && !data.isNewUser && data.accessToken != null -> {
                    tokenStore.saveUserMobile(mobile)
                    _navigateToMain.postValue(true)
                }

                // ── New user — collect name in RegisterScreen ────────
                data != null && data.isNewUser && data.tempToken != null -> {
                    _navigateToRegister.postValue(data.tempToken)
                }

                else -> {
                    // Unexpected response shape
                    _navigateToMain.postValue(false)
                }
            }
        }
    }

    fun resendOtp(mobile: String) {
        launchWithLoading {
            val response = authRepository.sendOtp(mobile)
            _resendSuccess.postValue(response.success)
        }
    }

    fun onNavigationConsumed() {
        _navigateToMain.value      = false
        _navigateToRegister.value  = null
    }

    fun onResendConsumed() {
        _resendSuccess.value = false
    }
}
