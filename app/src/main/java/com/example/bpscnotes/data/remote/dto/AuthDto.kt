package com.example.bpscnotes.data.remote.dto

data class SendOtpRequest(val mobile: String)

data class SendOtpResponse(
    val success: Boolean,
    val message: String
)

data class VerifyOtpRequest(
    val mobile: String,
    val otp: String
)

data class VerifyOtpResponse(
    val success: Boolean,
    val token: String,
    val user: UserDto         // ← now resolves
)