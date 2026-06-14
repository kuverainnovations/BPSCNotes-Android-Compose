package com.example.bpscnotes.core.config

import com.example.bpscnotes.data.remote.api.UserStatsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppConfigData(
    val coinValueInr: Float         = 0.1f,
    val maintenanceMode: Boolean    = false,
    val forceUpdate: Boolean        = false,
    val minAppVersion: String       = "1.0.0",
    val newRegistrations: Boolean   = true,
    val supportEmail: String        = "admin@bpscnotes.in",
    val androidStoreUrl: String     = "",
    val dailyQuizLimit: Int         = 5,
    val coinsPerCorrectAnswer: Int  = 2,
    val coinsPerStreakDay: Int       = 10,
    val coinsPerStudyHour: Int      = 15,
    val leaderboardEnabled: Boolean = true,
    val adsEnabled: Boolean         = true,
)

@Singleton
class AppConfigRepository @Inject constructor(
    private val api: UserStatsApiService
) {
    private val _config = MutableStateFlow(AppConfigData())
    val config: StateFlow<AppConfigData> = _config

    suspend fun fetch() {
        try {
            val res = api.getAppConfig()
            val c = res.data?.config ?: return
            _config.value = AppConfigData(
                coinValueInr          = c["coin_value_inr"]?.toFloatOrNull() ?: 0.1f,
                maintenanceMode       = c["maintenance_mode"] == "true",
                forceUpdate           = c["force_update"] == "true",
                minAppVersion         = c["min_app_version"] ?: "1.0.0",
                newRegistrations      = c["new_registrations"] != "false",
                supportEmail          = c["support_email"] ?: "admin@bpscnotes.in",
                androidStoreUrl       = c["android_store_url"] ?: "",
                dailyQuizLimit        = c["daily_quiz_limit"]?.toIntOrNull() ?: 5,
                coinsPerCorrectAnswer = c["coins_per_correct"]?.toIntOrNull() ?: 2,
                coinsPerStreakDay      = c["coins_per_streak_day"]?.toIntOrNull() ?: 10,
                coinsPerStudyHour     = c["coins_per_study_hour"]?.toIntOrNull() ?: 15,
                leaderboardEnabled    = c["leaderboard_enabled"] != "false",
                adsEnabled            = c["ads_enabled"] != "false",
            )
        } catch (_: Exception) { /* keep defaults on network failure */ }
    }
}
