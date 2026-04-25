package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.remote.api.RegisterResponse
import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse
import com.example.bpscnotes.data.remote.dto.UserDto

interface AuthRepository {

    /** POST /auth/send-otp */
    suspend fun sendOtp(mobile: String): SendOtpResponse

    /**
     * POST /auth/verify-otp
     * If response.data.isNewUser == true → caller must navigate to RegisterScreen
     * If response.data.isNewUser == false → accessToken is ready, token saved automatically
     */
    suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse

    /**
     * POST /auth/register — called with REAL user-entered data.
     * Token is saved to TokenStore automatically on success.
     */
    suspend fun register(
        tempToken: String,
        name: String,
        email: String?,
        district: String?
    ): RegisterResponse

    /** GET /auth/me — fetches current user profile */
    suspend fun getMe(): UserDto?
}
