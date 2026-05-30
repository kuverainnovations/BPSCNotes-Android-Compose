package com.example.bpscnotes.presentation.settings

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.analytics.Event
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// SettingsViewModel — fully dynamic
// • Toggle preferences persist in SharedPreferences
// • Storage sizes computed from real filesystem
// • Logout calls API then clears all local data
// • Delete account calls API then clears local data
// ════════════════════════════════════════════════════════════

data class SettingsUiState(
    // Toggles — loaded from SharedPreferences on init
    val darkMode:           Boolean = false,
    val studyReminder:      Boolean = true,
    val autoPlay:           Boolean = true,
    val sound:              Boolean = true,
    val haptics:            Boolean = true,
    // Storage — computed from real filesystem
    val downloadedSizeMb:   Float   = 0f,
    val cacheSizeMb:        Float   = 0f,
    val isComputingStorage: Boolean = false,
    val isClearingCache:    Boolean = false,
    // Account actions
    val isLoggingOut:         Boolean = false,
    val isDeletingAccount:    Boolean = false,
    val showDeleteConfirm:    Boolean = false,
    // Feedback
    val successMessage:     String? = null,
    val error:              String? = null,
    val loggedOut:          Boolean = false   // triggers navigation to Login
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApi:    AuthApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    companion object { private const val TAG = "SettingsVM" }

    init {
        loadPersistedPreferences()
        computeStorageSizes()
    }

    // ── Load preferences from SharedPreferences ───────────────
    private fun loadPersistedPreferences() {
        _state.update {
            it.copy(
                darkMode      = tokenStore.getBoolPref("dark_mode",      false),
                studyReminder = tokenStore.getBoolPref("study_reminder", true),
                autoPlay      = tokenStore.getBoolPref("auto_play",      true),
                sound         = tokenStore.getBoolPref("sound",          true),
                haptics       = tokenStore.getBoolPref("haptics",        true),
            )
        }
    }

    // ── Toggle setters — each persists immediately ────────────
    fun setDarkMode(v: Boolean)      { _state.update { it.copy(darkMode = v) };      tokenStore.setBoolPref("dark_mode", v);      Event.settingsChanged("dark_mode", v.toString()) }
    fun setStudyReminder(v: Boolean) { _state.update { it.copy(studyReminder = v) }; tokenStore.setBoolPref("study_reminder", v); Event.settingsChanged("study_reminder", v.toString()) }
    fun setAutoPlay(v: Boolean)      { _state.update { it.copy(autoPlay = v) };      tokenStore.setBoolPref("auto_play", v) }
    fun setSound(v: Boolean)         { _state.update { it.copy(sound = v) };         tokenStore.setBoolPref("sound", v);         Event.settingsChanged("sound", v.toString()) }
    fun setHaptics(v: Boolean)       { _state.update { it.copy(haptics = v) };       tokenStore.setBoolPref("haptics", v) }

    // ── Real storage sizes ────────────────────────────────────
    fun computeStorageSizes() {
        viewModelScope.launch {
            _state.update { it.copy(isComputingStorage = true) }
            try {
                // Downloaded materials: BPSCNotes folder in public Downloads
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "BPSCNotes"
                )
                val downloadedBytes =
                    if (downloadsDir.exists() && downloadsDir.isDirectory)
                        downloadsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    else 0L

                // App cache: OkHttp response cache + Coil image cache
                val cacheBytes = context.cacheDir
                    .walkTopDown().filter { it.isFile }.sumOf { it.length() }

                _state.update {
                    it.copy(
                        downloadedSizeMb  = downloadedBytes / (1024f * 1024f),
                        cacheSizeMb       = cacheBytes / (1024f * 1024f),
                        isComputingStorage = false
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "computeStorage: ${e.message}")
                _state.update { it.copy(isComputingStorage = false) }
            }
        }
    }

    // ── Clear cache ────────────────────────────────────────────
    fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isClearingCache = true) }
            try {
                context.cacheDir.deleteRecursively()
                _state.update {
                    it.copy(
                        isClearingCache = false,
                        cacheSizeMb     = 0f,
                        successMessage  = "✅ Cache cleared successfully"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isClearingCache = false, error = "Failed to clear cache")
                }
            }
        }
    }

    // ── Log out ───────────────────────────────────────────────
    // Calls POST /auth/logout to invalidate server token.
    // Then clears ALL local data regardless of server response.
    fun logOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }
            // Best-effort server logout — don't block UI on network errors
            runCatching { authApi.logOut() }
            tokenStore.clearAll()
            _state.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    // ── Delete account ─────────────────────────────────────────
    fun showDeleteConfirm() = _state.update { it.copy(showDeleteConfirm = true) }
    fun hideDeleteConfirm() = _state.update { it.copy(showDeleteConfirm = false) }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true, showDeleteConfirm = false) }
            try {
                authApi.deleteAccount()
                tokenStore.clearAll()
                _state.update { it.copy(isDeletingAccount = false, loggedOut = true) }
            } catch (e: Exception) {
                Log.e(TAG, "deleteAccount: ${e.message}", e)
                _state.update {
                    it.copy(isDeletingAccount = false,
                        error = "Failed to delete account. Please try again.")
                }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(successMessage = null, error = null) }
}
