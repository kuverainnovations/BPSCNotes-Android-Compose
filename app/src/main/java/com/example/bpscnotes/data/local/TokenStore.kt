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

    // Encrypted store for the auth token (a real credential). Built lazily
    // and DEFENSIVELY: the Android Keystore can be unavailable or corrupt on
    // some OEM devices or after certain data restores, in which case this is
    // null and we fall back to standard prefs so the app never crashes over
    // at-rest encryption. The token still lives in app-private storage either
    // way; on healthy devices it is now additionally encrypted.
    private val securePrefs: SharedPreferences? by lazy { buildSecurePrefs() }

    private fun buildSecurePrefs(): SharedPreferences? = try {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "bpsc_secure_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        null
    }

    // ── Auth token ─────────────────────────────────────────────
    // Stored in the encrypted store when available. On read we transparently
    // migrate any legacy plaintext token from an older install exactly once.
    fun getToken(): String? {
        val secure = securePrefs
        if (secure != null) {
            try {
                secure.getString("auth_token", null)?.let { return it }
                val legacy = prefs.getString("auth_token", null)
                if (!legacy.isNullOrEmpty()) {
                    secure.edit().putString("auth_token", legacy).apply()
                    prefs.edit().remove("auth_token").apply()
                    return legacy
                }
                return null
            } catch (e: Exception) {
                // Keystore read failed — fall through to plaintext.
            }
        }
        return prefs.getString("auth_token", null)
    }

    fun saveToken(token: String) {
        val secure = securePrefs
        var stored = false
        if (secure != null) {
            try {
                secure.edit().putString("auth_token", token).apply()
                if (prefs.contains("auth_token")) prefs.edit().remove("auth_token").apply()
                stored = true
            } catch (e: Exception) {
                // Fall back to plaintext below.
            }
        }
        if (!stored) prefs.edit().putString("auth_token", token).apply()
        // Keep cached user_id in sync with whatever account this token
        // belongs to. Without this, switching accounts (or a fresh login
        // before the Profile screen has loaded) leaves a stale/absent
        // user_id, causing isMine()-style checks (chat "is this my own
        // message?") to misfire across the whole app.
        decodeUserIdFromJwt(token)?.let { saveUserId(it) }
    }

    fun clearToken() {
        try { securePrefs?.edit()?.remove("auth_token")?.apply() } catch (_: Exception) {}
        prefs.edit().remove("auth_token").apply()
    }
    val isLoggedIn: Boolean get() = !getToken().isNullOrEmpty()

    // ── User info ──────────────────────────────────────────────
    fun saveUserMobile(mobile: String) = prefs.edit().putString("user_mobile", mobile).apply()
    fun getUserMobile(): String? = prefs.getString("user_mobile", null)
    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getUserName(): String? = prefs.getString("user_name", null)
    fun saveUserId(id: String) = prefs.edit().putString("user_id", id).apply()

    /**
     * Returns the current user's ID, derived from the active auth token
     * if the cached value is missing or doesn't match the current token's
     * subject. This is the source-of-truth fix: previously "user_id" was
     * only saved when ProfileViewModel happened to load, so a fresh login
     * (or switching test accounts) could leave getUserId() returning null
     * or a STALE id from a previous account — silently breaking any
     * "is this message/action mine?" check (e.g. room chat alignment).
     */
    fun getUserId(): String? {
        val token = getToken()
        val tokenUserId = token?.let { decodeUserIdFromJwt(it) }
        val cached = prefs.getString("user_id", null)
        if (tokenUserId != null && tokenUserId != cached) {
            saveUserId(tokenUserId)
            return tokenUserId
        }
        return cached ?: tokenUserId
    }

    /**
     * Decodes the `userId` claim from a JWT's payload (base64url, no
     * signature verification needed — we only need to read the claim
     * the server already issued and will independently verify itself).
     */
    private fun decodeUserIdFromJwt(token: String): String? =
        decodeJwtPayload(token)?.optString("userId")?.takeIf { it.isNotBlank() }

    /**
     * Decodes the JWT payload (base64url, second segment) into a JSONObject,
     * or null if the token is malformed. Shared by [decodeUserIdFromJwt] and
     * [isTokenExpired] — no signature verification, we only read claims the
     * server already issued and independently re-verifies on each request.
     */
    private fun decodeJwtPayload(token: String): org.json.JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadJson = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            )
            org.json.JSONObject(payloadJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the locally-stored access token is expired (or
     * unreadable), based on its own `exp` claim — no network call.
     *
     * FIX: SplashScreen previously trusted `!getToken().isNullOrEmpty()`
     * alone to mean "logged in" and routed straight to Main. If a STALE
     * `bpsc_prefs` ever ends up on a device with an old, already-expired
     * auth_token still present (e.g. restored from an old cloud backup
     * snapshot predating the bpsc_prefs backup-exclusion fix, or any
     * other restore/migration path), Splash would route to Main, the
     * first authenticated API call would 401, AuthInterceptor would clear
     * the token and fire sessionExpired, and BpscNavHost would then bounce
     * the user to Login — a confusing Main-flash-then-Login sequence, and
     * (since is_onboarded was also restored as true) never Onboarding.
     *
     * By checking expiry locally first, Splash can skip Main entirely for
     * an already-dead token and fall through to MpinLogin/Login/Onboarding
     * immediately and deterministically — independent of backup state,
     * network conditions, or which backend endpoint happens to 401 first.
     *
     * A missing/unparseable `exp` claim is treated as expired (fail safe:
     * forces re-auth rather than trusting an unverifiable token).
     */
    fun isTokenExpired(): Boolean {
        val token = getToken() ?: return true
        val exp = decodeJwtPayload(token)?.optLong("exp", -1L) ?: -1L
        if (exp <= 0L) return true
        val nowSeconds = System.currentTimeMillis() / 1000L
        return nowSeconds >= exp
    }

    /** True if there's a token AND it hasn't expired yet (per its own `exp` claim). */
    fun hasValidToken(): Boolean = !getToken().isNullOrEmpty() && !isTokenExpired()

    // ── Onboarding ─────────────────────────────────────────────
    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)

    // FIX: was prefs.edit().putBoolean("is_onboarded", true).apply() — apply()
    // writes to disk asynchronously. If the user finishes onboarding and
    // then quickly backgrounds/kills the app (very common: onboarding ->
    // Login screen -> user doesn't log in immediately), the process can
    // die before that background write completes. The next cold start then
    // reads the STALE on-disk value (is_onboarded=false) and shows
    // Onboarding again. commit() is synchronous, so the flag is guaranteed
    // to be on disk before this function returns — same reasoning as
    // logout()'s use of commit() above.
    fun setOnboarded() = prefs.edit().putBoolean("is_onboarded", true).commit()

    // ── Post-login onboarding wizard (server-driven) ──────────
    // null  = not yet fetched / pre-feature users (skip wizard)
    // false = new user, wizard not completed yet
    // true  = wizard completed
    fun getOnboardingCompleted(): Boolean? {
        return if (prefs.contains("onboarding_wizard_done"))
            prefs.getBoolean("onboarding_wizard_done", true)
        else null
    }
    fun saveOnboardingCompleted(value: Boolean) =
        prefs.edit().putBoolean("onboarding_wizard_done", value).commit()

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

    // ── Job alert category preferences ───────────────────────────
    // Persisted locally — the toggle in JobAlertSheet previously lived in
    // an unbacked remember{} list that reset every time the sheet closed.
    fun getJobAlertCategories(): Set<String> =
        prefs.getStringSet("job_alert_categories", emptySet()) ?: emptySet()
    fun setJobAlertCategories(categories: Set<String>) =
        prefs.edit().putStringSet("job_alert_categories", categories).apply()

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
        try { securePrefs?.edit()?.remove("auth_token")?.apply() } catch (_: Exception) {}
        prefs.edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_id")
            .remove("exam_setup_done")
            .remove("primary_exam")
            .remove("prep_level")
            .remove("onboarding_wizard_done")
            .remove("downloaded_material_ids")
            .remove("purchased_material_ids")
            .commit()
    }

    /**
     * Full logout — used by the "Log out" action in Settings.
     *
     * Clears EVERYTHING about the current session and account
     * (auth token, user identity, exam setup, MPIN/biometric flags,
     * saved mobile number) so the next reopen lands on the plain
     * Login screen — NOT the MPIN quick-login screen, and NOT
     * Dashboard.
     *
     * Deliberately PRESERVES:
     *   - is_onboarded → so the onboarding/intro screens never
     *     reappear for a device that has already seen them, even
     *     after the user logs out. Per product requirement, only a
     *     fresh install should show onboarding.
     *   - language preference (stored separately in bpsc_lang_prefs)
     *
     * IMPORTANT: uses commit() (synchronous), not apply() (async).
     * The Settings screen immediately restarts the Activity after
     * calling this. apply() writes to disk on a background thread —
     * if the process is killed (e.g. the user swipes the app away
     * from Recents right after logging out) before that background
     * write completes, the next cold start reads the STALE on-disk
     * prefs file with the old auth_token still present, and Splash
     * sends the user straight back into Dashboard with the previous
     * session. commit() guarantees the clear is on disk before we
     * return, so this can never happen.
     */
    fun logout() {
        try { securePrefs?.edit()?.remove("auth_token")?.apply() } catch (_: Exception) {}
        prefs.edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_id")
            .remove("user_mobile")
            .remove("has_mpin")
            .remove("biometric_enabled")
            .remove("exam_setup_done")
            .remove("primary_exam")
            .remove("prep_level")
            .remove("onboarding_wizard_done")
            .remove("downloaded_material_ids")
            .remove("purchased_material_ids")
            .commit()
    }

    /**
     * Full wipe — used for account deletion ("delete my account
     * entirely"), where even is_onboarded should be cleared since the
     * account itself no longer exists. Uses commit() for the same
     * reason as logout() above.
     */
    fun clearAll() {
        try { securePrefs?.edit()?.clear()?.apply() } catch (_: Exception) {}
        prefs.edit().clear().commit()
    }

    // ── Device ID (Firebase Installation ID) ──────────────────
    // Stored once at app startup by MainActivity; read synchronously
    // by AuthInterceptor on every network request.
    fun getDeviceId(): String? = prefs.getString("device_id", null)
    fun saveDeviceId(id: String) = prefs.edit().putString("device_id", id).apply()
}