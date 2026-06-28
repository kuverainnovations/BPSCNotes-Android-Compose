package com.example.bpscnotes.domain.repository

import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.ChangeMpinRequest
import com.example.bpscnotes.data.remote.api.CheckMpinData
import com.example.bpscnotes.data.remote.api.CreateMpinData
import com.example.bpscnotes.data.remote.api.CreateMpinRequest
import com.example.bpscnotes.data.remote.api.ForgotMpinRequest
import com.example.bpscnotes.data.remote.api.LoginMpinRequest
import com.example.bpscnotes.data.remote.api.RegisterResponse
import com.example.bpscnotes.data.remote.api.ResetMpinRequest
import com.example.bpscnotes.data.remote.api.SendOtpRequest
import com.example.bpscnotes.data.remote.api.SendOtpResponse
import com.example.bpscnotes.data.remote.api.VerifyOtpRequest
import com.example.bpscnotes.data.remote.api.VerifyOtpResponse
import com.example.bpscnotes.data.remote.dto.RegisterRequest
import com.example.bpscnotes.data.remote.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenStore: TokenStore
) : AuthRepository {

    // ── OTP ─────────────────────────────────────────────────────
    override suspend fun sendOtp(mobile: String): SendOtpResponse =
        api.sendOtp(SendOtpRequest("+91$mobile"))

    override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResponse {
        val response = api.verifyOtp(VerifyOtpRequest("+91$mobile", otp))
        if (response.success) {
            val data = response.data
            if (data != null && !data.isNewUser && data.accessToken != null) {
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserMobile(mobile)
                tokenStore.setHasMpin(data.hasMpin)
            }
        }
        return response
    }

    override suspend fun validateForgotMpinOtp(mobile: String, otp: String): Boolean {
        val response = api.validateForgotMpinOtp(VerifyOtpRequest("+91$mobile", otp))
        return response.success
    }

    // ── Registration ────────────────────────────────────────────
    override suspend fun register(
        tempToken: String, name: String, email: String?, district: String?
    ): RegisterResponse {
        val response = api.register(RegisterRequest(tempToken, name, email, district))
        if (response.success) {
            val data = response.data
            if (data != null) {
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserName(name)
                tokenStore.setHasMpin(false)  // fresh registration has no MPIN yet
            }
        }
        return response
    }

    // ── MPIN ────────────────────────────────────────────────────
    override suspend fun checkMpin(mobile: String): CheckMpinData {
        return try {
            val response = api.checkMpin("+91$mobile")
            response.data ?: CheckMpinData()
        } catch (e: Exception) {
            CheckMpinData()  // network error → show OTP flow (safe fallback)
        }
    }

    override suspend fun loginMpin(mobile: String, mpin: String): VerifyOtpResponse {
        val response = api.loginMpin(LoginMpinRequest("+91$mobile", mpin))
        if (response.success) {
            val data = response.data
            if (data != null && data.accessToken != null) {
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserMobile(mobile)
                tokenStore.setHasMpin(true)
            }
        }
        return response
    }

    override suspend fun createMpin(mpin: String): CreateMpinData {
        val response = api.createMpin(CreateMpinRequest(mpin))
        if (response.success) {
            tokenStore.setHasMpin(true)
        }
        return response.data ?: CreateMpinData(mpinCreated = false)
    }

    override suspend fun forgotMpin(mobile: String): Boolean {
        val response = api.forgotMpin(ForgotMpinRequest("+91$mobile"))
        return response.success
    }

    override suspend fun resetMpin(mobile: String, otp: String, newMpin: String): VerifyOtpResponse {
        val response = api.resetMpin(ResetMpinRequest("+91$mobile", otp, newMpin))
        if (response.success) {
            val data = response.data
            if (data != null && data.accessToken != null) {
                tokenStore.saveToken(data.accessToken)
                tokenStore.saveUserMobile(mobile)
                tokenStore.setHasMpin(true)
            }
        }
        return response
    }

    override suspend fun changeMpin(currentMpin: String, newMpin: String): Boolean {
        val response = api.changeMpin(ChangeMpinRequest(currentMpin, newMpin))
        return response.success
    }

    // ── Profile ─────────────────────────────────────────────────
    override suspend fun getMe(): UserDto? {
        return try {
            val response = api.getMe()
            if (response.success) response.data?.user else null
        } catch (e: Exception) { null }
    }
}
