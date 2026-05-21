package com.example.bpscnotes.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("bpsc_prefs", Context.MODE_PRIVATE)

    // ── Auth token ─────────────────────────────────────────────
    fun getToken(): String? = prefs.getString("auth_token", null)
    fun saveToken(token: String) = prefs.edit().putString("auth_token", token).apply()
    fun clearToken() = prefs.edit().remove("auth_token").apply()
    val isLoggedIn: Boolean get() = !getToken().isNullOrEmpty()

    // ── User info ──────────────────────────────────────────────
    fun saveUserMobile(mobile: String) = prefs.edit().putString("user_mobile", mobile).apply()
    fun getUserMobile(): String? = prefs.getString("user_mobile", null)
    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getUserName(): String? = prefs.getString("user_name", null)
    fun saveUserId(id: String) = prefs.edit().putString("user_id", id).apply()
    fun getUserId(): String? = prefs.getString("user_id", null)

    // ── Onboarding ─────────────────────────────────────────────
    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)
    fun setOnboarded() = prefs.edit().putBoolean("is_onboarded", true).apply()

    // ── Exam Setup ────────────────────────────────────────────
    fun isExamSetupDone(): Boolean = prefs.getBoolean("exam_setup_done", false)
    fun setExamSetupDone() = prefs.edit().putBoolean("exam_setup_done", true).apply()
    fun saveUserPrimaryExam(exam: String) = prefs.edit().putString("primary_exam", exam).apply()
    fun getUserPrimaryExam(): String? = prefs.getString("primary_exam", null)
    fun saveUserPrepLevel(level: String) = prefs.edit().putString("prep_level", level).apply()
    fun getUserPrepLevel(): String? = prefs.getString("prep_level", null)

    // ── Generic boolean prefs — settings toggles ───────────────
    // Persists user preferences across app restarts
    fun getBoolPref(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean("pref_$key", default)
    fun setBoolPref(key: String, value: Boolean) =
        prefs.edit().putBoolean("pref_$key", value).apply()

    // ── Downloaded material IDs (persisted so "Saved" button survives restart) ──
    fun getDownloadedIds(): Set<String> =
        prefs.getStringSet("downloaded_material_ids", emptySet()) ?: emptySet()

    fun addDownloadedId(id: String) {
        val current = getDownloadedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("downloaded_material_ids", current).apply()
    }

    fun removeDownloadedId(id: String) {
        val current = getDownloadedIds().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet("downloaded_material_ids", current).apply()
    }

    // ── Purchased material IDs (locked PDF access) ──────────────
    fun getPurchasedIds(): Set<String> =
        prefs.getStringSet("purchased_material_ids", emptySet()) ?: emptySet()

    fun addPurchasedId(id: String) {
        val current = getPurchasedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("purchased_material_ids", current).apply()
    }

    // ── Clear all (logout) ─────────────────────────────────────
    fun clearAll() = prefs.edit().clear().apply()
}