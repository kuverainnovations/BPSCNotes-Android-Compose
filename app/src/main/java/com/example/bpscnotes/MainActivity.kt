package com.example.bpscnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.core.ui.t.BPSCNotesTheme
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.NavGraph.BpscNavHost
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// MainActivity — reads darkMode from SettingsViewModel and
// passes it to BPSCNotesTheme so toggling in Settings
// immediately changes the whole app theme.
// ════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // SettingsViewModel is Activity-scoped so its dark mode
    // state survives composition recompositions.
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                BpscNavHost(navController = navController)
            }
        }
    }
}
