package com.example.bpscnotes.presentation.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bpscnotes.core.base.BaseViewModel
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _registerSuccess = MutableLiveData(false)
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    /**
     * Calls POST /auth/register with real user-entered data.
     * Token is saved inside AuthRepositoryImpl on success.
     */
    fun register(
        tempToken: String,
        name: String,
        email: String?,
        district: String?
    ) {
        if (name.isBlank()) {
            // error surfaced through BaseViewModel._error
            launchWithLoading { throw Exception("Please enter your name") }
            return
        }

        launchWithLoading {
            val response = authRepository.register(
                tempToken = tempToken,
                name      = name.trim(),
                email     = email?.trim()?.takeIf { it.isNotEmpty() },
                district  = district?.trim()?.takeIf { it.isNotEmpty() }
            )
            if (response.success) {
                _registerSuccess.postValue(true)
            } else {
                throw Exception(response.message.ifEmpty { "Registration failed" })
            }
        }
    }

    fun onNavigationConsumed() {
        _registerSuccess.value = false
    }
}
