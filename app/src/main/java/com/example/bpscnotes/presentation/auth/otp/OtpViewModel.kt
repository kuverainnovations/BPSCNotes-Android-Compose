package com.example.bpscnotes.presentation.auth.otp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.core.notifications.FcmTokenManager
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.domain.repository.AuthRepository
import com.example.bpscnotes.core.network.CacheInvalidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val fcmTokenManager: FcmTokenManager,
    private val cacheInvalidator: CacheInvalidator
) : BaseViewModel() {

    /** Existing user → Main */
    private val _navigateToMain = MutableLiveData(false)
    val verifySuccess: LiveData<Boolean> = _navigateToMain

    /** New user → Register */
    private val _navigateToRegister = MutableLiveData<String?>()
    val navigateToRegister: LiveData<String?> = _navigateToRegister

    private val _resendSuccess = MutableLiveData(false)
    val resendSuccess: LiveData<Boolean> = _resendSuccess

    fun verifyOtp(mobile: String, otp: String) {

        launchWithLoading {

            val response = authRepository.verifyOtp(mobile, otp)

            Log.d("OTP_VERIFY", "RESPONSE = $response")

            if (!response.success) {
                return@launchWithLoading
            }

            val data = response.data

            when {

                // EXISTING USER
                data != null &&
                        !data.isNewUser &&
                        data.accessToken != null -> {

                    Log.d("OTP_VERIFY", "LOGIN SUCCESS")

                    // Save mobile
                    tokenStore.saveUserMobile(mobile)

                    // IMPORTANT
                    // Sync FCM token AFTER login success
                    try {

                        Log.d("OTP_VERIFY", "SYNCING FCM TOKEN")

                        fcmTokenManager.syncTokenIfNeeded()

                        Log.d("OTP_VERIFY", "FCM TOKEN SYNC DONE")

                    } catch (e: Exception) {

                        Log.e(
                            "OTP_VERIFY",
                            "FCM SYNC FAILED",
                            e
                        )
                    }

                    // Clear any cached responses from the previous user session
                    // so the new user never sees Suresh's courses/profile
                    cacheInvalidator.evict()

                    _navigateToMain.postValue(true)
                }

                // NEW USER
                data != null &&
                        data.isNewUser &&
                        data.tempToken != null -> {

                    _navigateToRegister.postValue(data.tempToken)
                }

                else -> {

                    Log.e(
                        "OTP_VERIFY",
                        "INVALID RESPONSE"
                    )

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

        _navigateToMain.value = false
        _navigateToRegister.value = null
    }

    fun onResendConsumed() {

        _resendSuccess.value = false
    }
}