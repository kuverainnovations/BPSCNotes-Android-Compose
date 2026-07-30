package com.example.bpscnotes.core.config

import com.example.bpscnotes.data.remote.api.UserStatsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppConfigData(
    val maintenanceMode: Boolean    = false,
    val forceUpdate: Boolean        = false,
    val minAppVersion: String       = "1.0.0",
    val latestAppVersion: String    = "1.0.0",
    val newRegistrations: Boolean   = true,
    val supportEmail: String        = "admin@bpscnotes.in",
    val androidStoreUrl: String     = "",
    val dailyQuizLimit: Int         = 5,
    val leaderboardEnabled: Boolean = true,
    val adsEnabled: Boolean         = true,
    val studyRoomsEnabled: Boolean  = true,
    val screenCaptureProtection: Boolean = true,
    /**
     * False until the first successful fetch. Gates the blocking dialogs so a
     * cold start can't flash "Under Maintenance" for a frame before the real
     * config lands — the defaults above are optimistic on purpose.
     */
    val loaded: Boolean             = false,
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
                maintenanceMode       = c["maintenance_mode"] == "true",
                forceUpdate           = c["force_update"] == "true",
                minAppVersion         = c["min_app_version"] ?: "1.0.0",
                latestAppVersion      = c["app_version"] ?: "1.0.0",
                newRegistrations      = c["new_registrations"] != "false",
                supportEmail          = c["support_email"] ?: "admin@bpscnotes.in",
                androidStoreUrl       = c["android_store_url"] ?: "",
                dailyQuizLimit        = c["daily_quiz_limit"]?.toIntOrNull() ?: 5,
                leaderboardEnabled    = c["leaderboard_enabled"] != "false",
                adsEnabled            = c["ads_enabled"] != "false",
                studyRoomsEnabled     = c["study_rooms_enabled"] != "false",
                screenCaptureProtection = c["screen_capture_protection"] != "false",
                loaded                = true,
            )
        } catch (_: Exception) { /* keep defaults on network failure */ }
    }
}
