package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.remote.api.CheckMpinData
import com.example.bpscnotes.data.remote.api.CreateMpinData
import com.example.bpscnotes.data.remote.api.RegisterResponse
import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse
import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.example.bpscnotes.data.remote.dto.UserDto

interface AuthRepository {

    // ── OTP (used only for new registration + forgot MPIN) ──────
    suspend fun sendOtp(mobile: String): SendOtpResponse
    suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse
    /** Validates OTP without consuming it — for forgot-mpin OTP screen pre-check */
    suspend fun validateForgotMpinOtp(mobile: String, otp: String): Boolean

    // ── Registration ────────────────────────────────────────────
    suspend fun register(tempToken: String, name: String, email: String?, district: String?): RegisterResponse

    // ── MPIN ────────────────────────────────────────────────────
    /** Check if user has MPIN set — determines login screen to show */
    suspend fun checkMpin(mobile: String): CheckMpinData

    /** Primary login for returning users */
    suspend fun loginMpin(mobile: String, mpin: String): VerifyOtpResponse

    /** Set MPIN for the first time after registration (JWT required) */
    suspend fun createMpin(mpin: String): CreateMpinData

    /** Trigger OTP send for Forgot MPIN flow */
    suspend fun forgotMpin(mobile: String): Boolean

    /** Verify OTP + set new MPIN → returns JWT (logs user in) */
    suspend fun resetMpin(mobile: String, otp: String, newMpin: String): VerifyOtpResponse

    /** Change MPIN from settings (JWT required) */
    suspend fun changeMpin(currentMpin: String, newMpin: String): Boolean

    // ── Profile ─────────────────────────────────────────────────
    suspend fun getMe(): UserDto?
}
