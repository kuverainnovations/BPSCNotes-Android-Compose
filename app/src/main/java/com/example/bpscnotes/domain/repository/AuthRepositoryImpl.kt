package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.mock.MockData
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService
) : AuthRepository {

    override suspend fun sendOtp(mobile: String): SendOtpResponse {
        delay(1000) // simulate network delay
        return SendOtpResponse(success = true, message = "OTP sent successfully")
    }

    override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse {
        delay(1200)
        return if (otp == "123456") { // any OTP works in mock mode
            VerifyOtpResponse(
                success = true,
                token = "mock_jwt_token_bpsc_2024",
                user = MockData.currentUser
            )
        } else {
            throw Exception("Invalid OTP. Use 123456 for testing.")
        }
    }
}