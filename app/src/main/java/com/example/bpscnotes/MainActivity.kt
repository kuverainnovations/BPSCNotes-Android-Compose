package com.example.bpscnotes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.bpscnotes.presentation.payment.RazorpayPaymentListener
import com.example.bpscnotes.presentation.settings.SettingsViewModel
import com.example.bpscnotes.core.config.AppConfigRepository
import kotlinx.coroutines.launch
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(),
    PaymentResultWithDataListener,
    RazorpayPaymentListener {

    @Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager
    private val settingsViewModel: SettingsViewModel by viewModels()
    @Inject lateinit var languageManager: LanguageManager
    @Inject lateinit var coinsApi: CoinsApiService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var fcmTokenManager: FcmTokenManager
    @Inject lateinit var appConfigRepo: AppConfigRepository

    // Callbacks registered by PaymentScreen before launching Razorpay checkout
    private var onPaymentSuccessCallback: ((String, String) -> Unit)? = null
    private var onPaymentFailureCallback: ((Int, String) -> Unit)? = null

    // ── RazorpayPaymentListener ─────────────────────────────
    override fun setPaymentCallbacks(
        onSuccess: (paymentId: String, signature: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit
    ) {
        onPaymentSuccessCallback = onSuccess
        onPaymentFailureCallback = onFailure
    }

    // ── PaymentResultWithDataListener ───────────────────────
    override fun onPaymentSuccess(paymentId: String?, response: PaymentData?) {
        Log.d("Razorpay", "Payment success: paymentId=$paymentId orderId=${response?.orderId}")
        val id        = paymentId ?: ""
        val signature = response?.signature ?: ""
        Event.paymentSuccess("subscription", 0, response?.paymentId ?: "upi")
        onPaymentSuccessCallback?.invoke(id, signature)
        onPaymentSuccessCallback = null
        onPaymentFailureCallback = null
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Log.e("Razorpay", "Payment error: code=$code msg=$response")
        if (code != 0) Event.paymentFailed("subscription", code, response ?: "Payment failed")
        onPaymentFailureCallback?.invoke(code, response ?: "Payment failed")
        onPaymentSuccessCallback = null
        onPaymentFailureCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fetch admin-controlled settings on startup
        lifecycleScope.launch {
            try { appConfigRepo.fetch() } catch (_: Exception) {}
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

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        // ── Fix: prevent black screen on back press / minimize ────────────
        // 1. Give the window a non-transparent background so it's never naked black.
        window.setBackgroundDrawable(
            ColorDrawable(Color.parseColor("#F2F4F8"))
        )
        // 2. Safety-net: if Compose's BackHandler misses a rapid second tap
        //    (race condition during recomposition), catch it here and
        //    minimise the app instead of popping the NavHost into a black screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Only fires when NO Compose BackHandler is enabled above us.
                // The BackHandler in MainShell has priority; this is the last resort.
                moveTaskToBack(true)
            }
        })

        // Handle notification deep-link
        val initialScreen = intent?.getStringExtra("screen")
        val notifType     = intent?.getStringExtra("type") ?: ""
        val notifId       = intent?.getStringExtra("notifId") ?: ""
        if (notifId.isNotBlank()) Event.notificationTapped(notifType, notifId)

        setContent {
            val settingsState by settingsViewModel.state.collectAsState()
            // Collect from static companion — shared by ALL LanguageManager instances
            // This ensures drawer/settings changes apply instantly without restart
            val currentLanguage by com.example.bpscnotes.core.language.LanguageManager.language.collectAsState()
            BPSCNotesTheme(darkMode = settingsState.darkMode, language = currentLanguage) {
                val navController = rememberNavController()
                BpscNavHost(
                    navController = navController,
                    adManager     = adManager,
                )
            }
        }
    }
}