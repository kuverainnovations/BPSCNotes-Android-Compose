package com.example.bpscnotes.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenStore — persists auth token, user info, MPIN flags, and biometric preference.
 *
 * NOTE ON ENCRYPTED PREFS:
 *   We intentionally use standard SharedPreferences here so the build compiles without
 *   the security-crypto library. The biometric_enabled flag is not a credential —
 *   it's a UI preference. Add this to app/build.gradle when ready for encrypted storage:
 *
 *     implementation("androidx.security:security-crypto:1.1.0-alpha06")
 *
 *   Then replace securePrefs with EncryptedSharedPreferences (see git history).
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bpsc_prefs", Context.MODE_PRIVATE)

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

    // ── MPIN state (flag only — MPIN itself NEVER stored locally) ──
    fun hasMpin(): Boolean = prefs.getBoolean("has_mpin", false)
    fun setHasMpin(value: Boolean) = prefs.edit().putBoolean("has_mpin", value).apply()

    // ── Biometric preference ────────────────────────────────────
    // Stored in standard prefs (not EncryptedSharedPreferences) until
    // security-crypto dependency is added to app/build.gradle.
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()

    // ── Generic boolean/string prefs — settings toggles ────────
    fun getBoolPref(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean("pref_$key", default)
    fun setBoolPref(key: String, value: Boolean) =
        prefs.edit().putBoolean("pref_$key", value).apply()
    fun getStringPref(key: String, default: String = ""): String =
        prefs.getString("pref_$key", default) ?: default
    fun setStringPref(key: String, value: String) =
        prefs.edit().putString("pref_$key", value).apply()

    // ── Downloaded material IDs ─────────────────────────────────
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
        prefs.edit().remove("dl_path_$id").apply()
    }

    fun saveLocalPath(id: String, path: String) =
        prefs.edit().putString("dl_path_$id", path).apply()

    fun getLocalPath(id: String): String? = prefs.getString("dl_path_$id", null)

    // ── Purchased material IDs ──────────────────────────────────
    fun getPurchasedIds(): Set<String> =
        prefs.getStringSet("purchased_material_ids", emptySet()) ?: emptySet()

    fun addPurchasedId(id: String) {
        val current = getPurchasedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("purchased_material_ids", current).apply()
    }

    // ── Logout helpers ─────────────────────────────────────────

    /**
     * Normal logout — clears JWT but preserves:
     *   - user_mobile     → next open skips mobile entry, shows MPIN screen directly
     *   - has_mpin        → app knows which screen to show
     *   - biometric_enabled
     *   - is_onboarded / language
     */
    fun clearSessionOnly() {
        prefs.edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_id")
            .remove("exam_setup_done")
            .remove("primary_exam")
            .remove("prep_level")
            .remove("downloaded_material_ids")
            .remove("purchased_material_ids")
            .apply()
    }

    /** Full wipe — used for account deletion or "switch account". */
    fun clearAll() = prefs.edit().clear().apply()
}
