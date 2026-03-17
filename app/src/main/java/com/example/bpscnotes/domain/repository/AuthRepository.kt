package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse

interface AuthRepository {
    suspend fun sendOtp(mobile: String): SendOtpResponse
    suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse
}