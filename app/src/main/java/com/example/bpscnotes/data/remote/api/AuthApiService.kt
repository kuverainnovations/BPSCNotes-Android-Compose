package com.example.bpscnotes.data.remote.api

import com.example.bpscnotes.data.remote.dto.SendOtpRequest
import com.example.bpscnotes.data.remote.dto.UserDto
import com.example.bpscnotes.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): SendOtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse
}

data class SendOtpRequest(val mobile: String)
data class SendOtpResponse(val success: Boolean, val message: String)

data class VerifyOtpRequest(val mobile: String, val otp: String)
data class VerifyOtpResponse(val success: Boolean, val token: String, val user: UserDto)