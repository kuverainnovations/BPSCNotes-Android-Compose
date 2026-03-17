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

    private val _verifySuccess = MutableLiveData(false)
    val verifySuccess: LiveData<Boolean> = _verifySuccess

    private val _resendSuccess = MutableLiveData(false)
    val resendSuccess: LiveData<Boolean> = _resendSuccess

    fun verifyOtp(mobile: String, otp: String) {
        launchWithLoading {
            val response = authRepository.verifyOtp(mobile, otp)
            if (response.success) {
                tokenStore.saveToken(response.token)
                tokenStore.saveUserMobile(mobile)
                _verifySuccess.postValue(true)
            }
        }
    }

    fun resendOtp(mobile: String) {
        launchWithLoading {
            val response = authRepository.sendOtp(mobile)
            _resendSuccess.postValue(response.success)
        }
    }
}