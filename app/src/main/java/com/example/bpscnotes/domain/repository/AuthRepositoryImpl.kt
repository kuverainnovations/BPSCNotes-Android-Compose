package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.RegisterResponse
import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse
import com.example.bpscnotes.data.remote.dto.RegisterRequest
import com.example.bpscnotes.data.remote.dto.UserDto
import com.example.bpscnotes.data.remote.api.SendOtpRequest
import com.example.bpscnotes.data.remote.api.VerifyOtpRequest
import com.example.bpscnotes.data.remote.dto.GetMeData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenStore: TokenStore
) : AuthRepository {

    override suspend fun sendOtp(mobile: String): SendOtpResponse {
        // Backend expects +91 prefix
        return api.sendOtp(SendOtpRequest("+91$mobile"))
    }

    override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse {
        val response = api.verifyOtp(VerifyOtpRequest("+91$mobile", otp))

        if (response.success) {
            val data = response.data
            if (data != null && !data.isNewUser && data.accessToken != null) {
                // Existing user — save token immediately
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserMobile(mobile)
            }
            // New user case: tempToken returned, no token saved yet —
            // caller must navigate to RegisterScreen to collect user's name
        }

        return response
    }

    override suspend fun register(
        tempToken: String,
        name: String,
        email: String?,
        district: String?
    ): RegisterResponse {
        val response = api.register(
            RegisterRequest(
                tempToken = tempToken,
                name      = name,
                email     = email,
                district  = district
            )
        )

        if (response.success) {
            val data = response.data
            if (data != null) {
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserName(name)
            }
        }

        return response
    }

    override suspend fun getMe(): UserDto? {
        return try {
            val response = api.getMe()
            if (response.success) {
                response.data?.user   // ✅ FIX
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
