package com.example.bpscnotes.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenStore — stores auth token + basic user data in SharedPreferences.
 *
 * All methods are synchronous (SharedPreferences is synchronous under the hood).
 * No `suspend` anywhere — avoids runBlocking in AuthInterceptor.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("bpsc_prefs", Context.MODE_PRIVATE)

    // ── Auth token ────────────────────────────────────────────
    fun getToken(): String? = prefs.getString("auth_token", null)

    fun saveToken(token: String) =
        prefs.edit().putString("auth_token", token).apply()

    fun clearToken() =
        prefs.edit().remove("auth_token").apply()

    val isLoggedIn: Boolean get() = !getToken().isNullOrEmpty()

    // ── User info ─────────────────────────────────────────────
    fun saveUserMobile(mobile: String) =
        prefs.edit().putString("user_mobile", mobile).apply()

    fun getUserMobile(): String? = prefs.getString("user_mobile", null)

    fun saveUserName(name: String) =
        prefs.edit().putString("user_name", name).apply()

    fun getUserName(): String? = prefs.getString("user_name", null)

    // ── Onboarding ────────────────────────────────────────────
    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)

    fun setOnboarded() =
        prefs.edit().putBoolean("is_onboarded", true).apply()
}
