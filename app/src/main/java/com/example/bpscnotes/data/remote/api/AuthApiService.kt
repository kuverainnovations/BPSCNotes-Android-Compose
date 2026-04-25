package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.GetMeData
import com.example.bpscnotes.data.remote.dto.RegisterRequest
import com.example.bpscnotes.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ── Auth API Service ──────────────────────────────────────────────────────────
interface AuthApiService {

    /** Step 1 — request OTP */
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): SendOtpResponse

    /** Step 2 — verify OTP (handles both new + existing users) */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    /** Step 3 (new users only) — register with name collected from UI */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    /** Get currently logged-in user profile */
    @GET("auth/me")
    suspend fun getMe(): GetMeResponse
}

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class SendOtpRequest(val mobile: String)

data class SendOtpResponse(
    val success: Boolean,
    val message: String
)

data class VerifyOtpRequest(val mobile: String, val otp: String)

data class VerifyOtpResponse(
    val success: Boolean,
    val message: String,
    val data: VerifyOtpData?    = null,
    val timestamp: String?      = null
)

data class VerifyOtpData(
    val isNewUser: Boolean      = false,
    val tempToken: String?      = null,   // present only when isNewUser = true
    val accessToken: String?    = null,   // present only when isNewUser = false
    val refreshToken: String?   = null
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: RegisterData?     = null
)

data class RegisterData(
    val accessToken: String,
    val refreshToken: String
)

/** Wrapper for GET /auth/me */
data class GetMeResponse(
    val success: Boolean,
    val message: String,
    val data: GetMeData?          = null
)
