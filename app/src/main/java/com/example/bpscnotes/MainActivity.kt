package com.example.bpscnotes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.core.analytics.Analytics
import com.example.bpscnotes.core.language.LanguageManager
import com.example.bpscnotes.core.analytics.Event
import com.example.bpscnotes.core.notifications.FcmTokenManager
import com.example.bpscnotes.core.ui.t.BPSCNotesTheme
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.presentation.navigation.NavGraph.BpscNavHost
import com.example.bpscnotes.presentation.payment.CashfreePaymentListener
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import com.example.bpscnotes.presentation.shared.UpdateGateDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.core.config.AppConfigRepository
import com.example.bpscnotes.core.config.CoinsConfigRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.google.firebase.installations.FirebaseInstallations

@AndroidEntryPoint
class MainActivity : ComponentActivity(),
    CashfreePaymentListener,
    CFCheckoutResponseCallback {

    @Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager
    private val settingsViewModel: SettingsViewModel by viewModels()
    @Inject lateinit var languageManager: LanguageManager
    @Inject lateinit var coinsApi: CoinsApiService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var fcmTokenManager: FcmTokenManager
    @Inject lateinit var appConfigRepo: AppConfigRepository
    @Inject lateinit var coinsConfigRepo: CoinsConfigRepository

    // Callbacks registered by payment screens before launching Cashfree SDK
    private var onPaymentSuccessCallback: ((String) -> Unit)? = null
    private var onPaymentFailureCallback: ((Int, String) -> Unit)? = null

    // ── CashfreePaymentListener ──────────────────────────────
    override fun setPaymentCallbacks(
        onSuccess: (cfPaymentId: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit
    ) {
        onPaymentSuccessCallback = onSuccess
        onPaymentFailureCallback = onFailure
    }

    /** Called by CashfreePaymentResultCallback after a successful payment. */
    fun onCashfreePaymentSuccess(cfPaymentId: String) {
        Log.d("CF_DEBUG", "Payment success callback = ${onPaymentSuccessCallback != null}")

        Log.d("Cashfree", "Payment success: cfPaymentId=$cfPaymentId")
        onPaymentSuccessCallback?.invoke(cfPaymentId)

        onPaymentSuccessCallback = null
        onPaymentFailureCallback = null
    }

    /** Called by CashfreePaymentResultCallback after a failed/cancelled payment. */
    fun onCashfreePaymentError(code: Int, message: String) {
        Log.e("Cashfree", "Payment error: code=$code msg=$message")
        if (code != 0) Event.paymentFailed("subscription", code, message)
        onPaymentFailureCallback?.invoke(code, message)
        onPaymentSuccessCallback = null
        onPaymentFailureCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dismiss system splash instantly (no crossfade) so composable splash takes over
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        // Persist Firebase Installation ID so AuthInterceptor can read it synchronously
        if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseInstallations.getInstance().id.addOnSuccessListener { fid ->
                tokenStore.saveDeviceId(fid)
            }
        }

        // Fetch admin-controlled settings on startup
        lifecycleScope.launch {
            try { appConfigRepo.fetch() } catch (_: Exception) {}
        }
        // Fetch admin-controlled coin amounts/caps/economy settings —
        // single source for every coin number shown in the app
        lifecycleScope.launch {
            try { coinsConfigRepo.fetch() } catch (_: Exception) {}
        }

        lifecycleScope.launch {
            try {
                val token = tokenStore.getToken()
                if (!token.isNullOrBlank()) {
                    coinsApi.checkIn()
                    Log.d("AutoCheckIn", "Daily check-in triggered")
                    // Sync FCM token — only if Firebase is initialized
                    if (com.google.firebase.FirebaseApp.getApps(this@MainActivity).isNotEmpty()) {
                        fcmTokenManager.syncTokenIfNeeded()
                    }
                }
            } catch (e: Exception) {
                Log.d("AutoCheckIn", "Check-in skipped: ${e.message}")
            }
        }
        // Track app open
        Event.appOpen()

        // dark style = light/white icons in status bar (time, wifi, battery)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Safety-net: if Compose's BackHandler misses a rapid second tap
        //    (race condition during recomposition), catch it here and
        //    minimise the app instead of popping the NavHost into a black screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Only fires when NO Compose BackHandler is enabled above us.
                // The BackHandler in MainShell has priority; this is the last resort.
                moveTaskToBack(true)
            }
        })

        // Handle notification deep-link — read from launch intent so navigation
        // works even when the app is fully closed (cold start from tray notification).
        val initialScreen       = intent?.getStringExtra("screen") ?: ""
        val initialNotifType    = intent?.getStringExtra("type") ?: ""
        val initialNotifId      = intent?.getStringExtra("notifId") ?: ""
        val initialCourseId     = intent?.getStringExtra("courseId") ?: ""
        val initialDeepLink     = intent?.getStringExtra("deepLink") ?: ""
        if (initialNotifId.isNotBlank()) Event.notificationTapped(initialNotifType, initialNotifId)

        setContent {
            val settingsState by settingsViewModel.state.collectAsState()
            val currentLanguage by com.example.bpscnotes.core.language.LanguageManager.language.collectAsState()
            BPSCNotesTheme(darkMode = settingsState.darkMode, language = currentLanguage) {
                val navController = rememberNavController()
                // The strip painted under the (transparent) 3-button nav bar
                // must match the current screen: theme background for normal
                // pages, the gradient's end color on full-bleed dark screens
                // (splash, MPIN) so the blue continues behind the buttons.
                val currentRoute = navController
                    .currentBackStackEntryAsState().value?.destination?.route
                val darkStripColor = when (currentRoute) {
                    Screen.Splash.route -> Color(0xFF0D47A1)
                    Screen.MpinLogin.route, Screen.CreateMpin.route,
                    Screen.ResetMpin.route, Screen.ChangeMpin.route -> Color(0xFF1E3A8A)
                    else -> null
                }
                val stripColor by animateColorAsState(
                    targetValue = darkStripColor ?: MaterialTheme.colorScheme.background,
                    label = "navBarStrip"
                )
                // Nav-bar icon contrast must follow what's behind the buttons,
                // not the enableEdgeToEdge() call above (which runs before the
                // theme is known and forces light icons).
                val lightNavIcons = !settingsState.darkMode && darkStripColor == null
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightNavigationBars = lightNavIcons
                }
                // Keep the app above the system navigation bar on 3-button
                // devices: tappableElement is the bar height there but ZERO on
                // gesture navigation, so gesture devices keep full edge-to-edge.
                // windowInsetsPadding() consumes what it applies, so the many
                // per-screen navigationBarsPadding() calls downstream become
                // no-ops instead of double-padding.
                Box(
                    Modifier
                        .background(stripColor)
                        .windowInsetsPadding(
                            WindowInsets.tappableElement
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                ) {
                    BpscNavHost(
                        navController        = navController,
                        adManager            = adManager,
                        // FIX Issue 2: pass intent extras so NavHost can deep-link
                        // on cold start (app was closed when notification was tapped)
                        initialNotifScreen   = initialScreen,
                        initialNotifType     = initialNotifType,
                        initialNotifCourseId = initialCourseId,
                    )
                    val appConfig by appConfigRepo.config.collectAsState()
                    UpdateGateDialog(appConfig)
                }
            }
        }
    }

    override fun onPaymentVerify(orderId: String) {
        Log.d("Cashfree", "Verified orderId=$orderId")
        onCashfreePaymentSuccess(orderId)
    }

    override fun onPaymentFailure(
        errorResponse: CFErrorResponse,
        orderId: String
    ) {
        val code = if (errorResponse.status == "cancelled") 0 else -1

        onCashfreePaymentError(
            code,
            errorResponse.message ?: "Payment failed"
        )
    }
}