package com.example.bpscnotes.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("bpsc_prefs", Context.MODE_PRIVATE)

    suspend fun getToken(): String? = prefs.getString("auth_token", null)
    suspend fun saveToken(token: String) = prefs.edit().putString("auth_token", token).apply()
    suspend fun clearToken() = prefs.edit().remove("auth_token").apply()

    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)
    fun setOnboarded() = prefs.edit().putBoolean("is_onboarded", true).apply()

    fun saveUserMobile(mobile: String) = prefs.edit().putString("mobile", mobile).apply()
    fun getUserMobile(): String? = prefs.getString("mobile", null)
}