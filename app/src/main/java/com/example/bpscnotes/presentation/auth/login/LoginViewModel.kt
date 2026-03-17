package com.example.bpscnotes.presentation.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _sendOtpSuccess = MutableLiveData<String?>()  // returns mobile on success
    val sendOtpSuccess: LiveData<String?> = _sendOtpSuccess

    fun sendOtp(mobile: String) {
        if (!isValidMobile(mobile)) {
            // expose error through base
            return
        }
        launchWithLoading {
            val response = authRepository.sendOtp(mobile)
            if (response.success) {
                _sendOtpSuccess.postValue(mobile)
            }
        }
    }

    private fun isValidMobile(mobile: String) = mobile.length == 10 && mobile.all { it.isDigit() }
}