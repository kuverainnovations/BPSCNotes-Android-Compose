package com.example.bpscnotes.presentation.auth.otp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.core.network.CacheInvalidator
import com.example.bpscnotes.core.notifications.FcmTokenManager
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// What the OTP screen should do after successful verification
sealed class OtpResult {
    object NavigateToMain          : OtpResult()     // existing user via registration context (edge case)
    data class NavigateToRegister(val tempToken: String) : OtpResult()  // new user
    data class NavigateToResetMpin(val mobile: String, val otp: String) : OtpResult()  // forgot_mpin context
    data class NavigateToCreateMpin(val mobile: String) : OtpResult()   // existing user with no MPIN
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val fcmTokenManager: FcmTokenManager,
    private val cacheInvalidator: CacheInvalidator
) : BaseViewModel() {

    private val _result = MutableLiveData<OtpResult?>()
    val result: LiveData<OtpResult?> = _result

    private val _resendSuccess = MutableLiveData(false)
    val resendSuccess: LiveData<Boolean> = _resendSuccess

    /**
     * context: "registration" — normal OTP after new-user mobile entry
     *          "forgot_mpin" — OTP for resetting MPIN
     */
    fun verifyOtp(mobile: String, otp: String, context: String = "registration") {
        launchWithLoading {
            if (context == "forgot_mpin") {
                // For forgot_mpin: we don't call verifyOtp backend endpoint.
                // We pass the mobile+otp to ResetMpinScreen which calls /auth/reset-mpin.
                // This way the OTP is consumed exactly once by the reset endpoint.
                _result.postValue(OtpResult.NavigateToResetMpin(mobile, otp))
                return@launchWithLoading
            }

            // context == "registration"
            val response = authRepository.verifyOtp(mobile, otp)
            Log.d("OTP_VERIFY", "RESPONSE = $response")

            if (!response.success) return@launchWithLoading

            val data = response.data

            when {
                // Existing user login via OTP (fallback — normally they use MPIN)
                data != null && !data.isNewUser && data.accessToken != null -> {
                    Log.d("OTP_VERIFY", "EXISTING USER LOGIN")
                    tokenStore.saveUserMobile(mobile)
                    try { fcmTokenManager.syncTokenIfNeeded() } catch (e: Exception) {
                        Log.e("OTP_VERIFY", "FCM sync failed", e)
                    }
                    cacheInvalidator.evictAll()
                    if (!data.hasMpin) {
                        // Existing user with no MPIN yet → send to CreateMpin
                        _result.postValue(OtpResult.NavigateToCreateMpin(mobile))
                    } else {
                        _result.postValue(OtpResult.NavigateToMain)
                    }
                }

                // New user — go to register
                data != null && data.isNewUser && data.tempToken != null -> {
                    _result.postValue(OtpResult.NavigateToRegister(data.tempToken))
                }

                else -> {
                    Log.e("OTP_VERIFY", "INVALID RESPONSE")
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

    /** Called on OtpScreen entry when context == "forgot_mpin" to trigger OTP send */
    fun sendForgotMpinOtp(mobile: String) {
        launchWithLoading {
            authRepository.forgotMpin(mobile)
            _resendSuccess.postValue(true)
        }
    }

    fun onResultConsumed() { _result.value = null }
    fun onResendConsumed() { _resendSuccess.value = false }}