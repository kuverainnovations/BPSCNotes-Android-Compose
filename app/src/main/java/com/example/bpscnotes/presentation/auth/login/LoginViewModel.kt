package com.example.bpscnotes.presentation.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class LoginDestination {
    data class Otp(val mobile: String) : LoginDestination()
    data class MpinLogin(val mobile: String, val lockedSeconds: Int = 0) : LoginDestination()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _destination = MutableLiveData<LoginDestination?>()
    val destination: LiveData<LoginDestination?> = _destination

    fun proceed(mobile: String) {
        if (!isValidMobile(mobile)) {
            // BaseViewModel exposes error via _error LiveData
            return
        }
        launchWithLoading {
            // 1. Check if user has MPIN set
            val mpinCheck = authRepository.checkMpin(mobile)

            if (mpinCheck.hasMpin) {
                // Returning user → MPIN login screen
                _destination.postValue(
                    LoginDestination.MpinLogin(mobile, mpinCheck.lockedUntilSeconds)
                )
            } else {
                // New user or no MPIN yet → OTP flow
                val response = authRepository.sendOtp(mobile)
                if (response.success) {
                    _destination.postValue(LoginDestination.Otp(mobile))
                }
                // If sendOtp fails, BaseViewModel surfaces the error automatically
            }
        }
    }

    fun onDestinationConsumed() { _destination.value = null }

    private fun isValidMobile(mobile: String) = mobile.length == 10 && mobile.all { it.isDigit() }
}