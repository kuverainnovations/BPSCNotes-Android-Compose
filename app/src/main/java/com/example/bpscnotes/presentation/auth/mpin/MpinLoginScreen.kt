package com.example.bpscnotes.presentation.auth.mpin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.auth.AppBiometricManager
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.di.TokenStoreEntryPoint
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.EntryPointAccessors

@Composable
fun MpinLoginScreen(
    navController: NavHostController,
    mobile: String,
    lockedSecondsInit: Int = 0,
    viewModel: MpinViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current
    val tokenStore = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TokenStoreEntryPoint::class.java
        ).tokenStore()
    }
    val biometricEnabled = remember { tokenStore.isBiometricEnabled() }

    // Blue status bar to match header
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF0A2472),
            darkIcons = false
        )
    }

    LaunchedEffect(lockedSecondsInit) {
        if (lockedSecondsInit > 0) viewModel.initLockout(lockedSecondsInit)
    }

    // Single LaunchedEffect(Unit) — reset stale state, then trigger biometric
    LaunchedEffect(Unit) {
        // Reset any stale navigation flags from previous session FIRST
        viewModel.resetState()
        // Small delay so resetState() takes effect before biometric fires
        kotlinx.coroutines.delay(100)
        if (biometricEnabled && !state.isLocked) {
            kotlinx.coroutines.delay(200)
            AppBiometricManager.authenticate(
                context   = context,
                title     = "BPSCNotes",
                subtitle  = "Use fingerprint to login",
                onSuccess = { viewModel.loginWithMpinViaBiometric(mobile) },
                onError    = {},
                onFallback = {}
            )
        }
    }

    // Navigate to Main only after successful login (navigateToMain set by ViewModel)
    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Auto-submit when 4 digits entered
    val pin = state.mpinDigits.joinToString("")
    LaunchedEffect(pin) {
        if (pin.length == 4 && !state.isLocked) viewModel.loginWithMpin(mobile)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E3A8A))
            ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // App logo / icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "+91 ${mobile.removePrefix("+91")}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.7f)
            )
            TextButton(onClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = false }
                }
            }) {
                Text(
                    "Not you? Change number",
                    color = Color.White.copy(0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(40.dp))

            // Title above dots
            Text(
                when {
                    state.isLocked -> "Account Locked"
                    else           -> "Enter your 4-digit MPIN"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(0.85f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            // MPIN dots
            MpinDots(
                digits   = state.mpinDigits,
                hasError = state.error != null && !state.isLocked
            )

            Spacer(Modifier.height(16.dp))

            // Error / Lockout message
            AnimatedVisibility(
                visible = state.error != null || state.isLocked,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE74C3C).copy(0.2f))
                        .border(1.dp, Color(0xFFE74C3C).copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (state.isLocked) Icons.Rounded.Lock else Icons.Rounded.Warning,
                            null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                state.isLocked -> {
                                    val m = state.lockedSecondsLeft / 60
                                    val s = state.lockedSecondsLeft % 60
                                    "Too many attempts. Try again in ${if (m > 0) "${m}m " else ""}${s}s"
                                }
                                else -> state.error ?: ""
                            },
                            color = Color(0xFFFF6B6B),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Loading indicator
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.height(16.dp))
            }

            // Numpad with biometric key
            MpinNumpad(
                enabled        = !state.isLocked && !state.isLoading,
                onDigit        = { viewModel.onMpinDigit(it) },
                onBackspace    = { viewModel.onMpinBackspace() },
                showBiometric  = biometricEnabled,
                onBiometric    = if (biometricEnabled) {{
                    AppBiometricManager.authenticate(
                        context    = context,
                        title      = "BPSCNotes",
                        subtitle   = "Use fingerprint to login",
                        onSuccess  = { viewModel.loginWithMpinViaBiometric(mobile) },
                        onError    = {},
                        onFallback = {}
                    )
                }} else null
            )

            Spacer(Modifier.height(8.dp))

            // Forgot MPIN
            TextButton(
                onClick = { navController.navigate(Screen.ForgotMpin.createRoute(mobile)) },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    "Forgot MPIN?",
                    color = Color.White.copy(0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}