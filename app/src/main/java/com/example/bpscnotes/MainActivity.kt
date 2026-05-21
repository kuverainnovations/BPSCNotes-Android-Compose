package com.example.bpscnotes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.core.ui.t.BPSCNotesTheme
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.presentation.navigation.NavGraph.BpscNavHost
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// MainActivity — reads darkMode from SettingsViewModel and
// passes it to BPSCNotesTheme so toggling in Settings
// immediately changes the whole app theme.
// ════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager

    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject lateinit var coinsApi: CoinsApiService
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Auto check-in on app open ──────────────────────────
        // Fire-and-forget: silently awards streak coins if user
        // hasn't checked in today. No UI impact on failure.
        lifecycleScope.launch {
            try {
                val token = tokenStore.getToken()
                if (!token.isNullOrBlank()) {
                    coinsApi.checkIn()   // idempotent — server ignores duplicate same-day check-ins
                    Log.d("AutoCheckIn", "Daily check-in triggered")
                }
            } catch (e: Exception) {
                // Silently ignore — network error, already checked in, etc.
                Log.d("AutoCheckIn", "Check-in skipped: ${e.message}")
            }
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        setContent {
            // Collect dark mode from SettingsViewModel.
            // This StateFlow is backed by SharedPreferences so it
            // loads the persisted value on the first frame.
            val settingsState by settingsViewModel.state.collectAsState()
            val darkMode       = settingsState.darkMode

            BPSCNotesTheme(darkMode = darkMode) {
                val navController = rememberNavController()
                BpscNavHost(navController = navController, adManager = adManager)
            }
        }
    }
}