package com.example.bpscnotes.core.language

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val flag: String) {
    ENGLISH("en", "English", "English", "🇬🇧"),
    HINDI("hi",   "Hindi",   "हिन्दी",  "🇮🇳"),
}

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("bpsc_lang_prefs", Context.MODE_PRIVATE)
    private val KEY   = "app_language"

    init {
        // On first creation (app start), push saved value into the shared global flow
        _globalLanguage.value = getSavedLanguage()
    }

    // All instances — Hilt singleton AND any new LanguageManager(ctx) created in Composables —
    // share this ONE static flow. This is the fix: changing language anywhere updates everyone.
    companion object {
        private val _globalLanguage = MutableStateFlow(AppLanguage.ENGLISH)
        val language: StateFlow<AppLanguage> = _globalLanguage.asStateFlow()

        /** Call this from Composables that can't use Hilt injection */
        fun setLanguageGlobal(lang: AppLanguage, context: Context) {
            context.getSharedPreferences("bpsc_lang_prefs", Context.MODE_PRIVATE)
                .edit().putString("app_language", lang.code).apply()
            _globalLanguage.value = lang
        }

        fun isFirstLaunch(context: Context): Boolean =
            !context.getSharedPreferences("bpsc_lang_prefs", Context.MODE_PRIVATE).contains("app_language")
    }

    // Instance API — same as before, backed by the shared flow
    val current: AppLanguage get() = _globalLanguage.value
    val isHindi: Boolean     get() = _globalLanguage.value == AppLanguage.HINDI

    fun setLanguage(lang: AppLanguage) = setLanguageGlobal(lang, context)
    fun isFirstLaunch(): Boolean       = isFirstLaunch(context)

    private fun getSavedLanguage(): AppLanguage {
        val code = prefs.getString(KEY, null) ?: return AppLanguage.ENGLISH
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
    }
}