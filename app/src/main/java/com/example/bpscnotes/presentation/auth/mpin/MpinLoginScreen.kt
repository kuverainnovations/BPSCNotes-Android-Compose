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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.auth.AppBiometricManager
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import com.example.bpscnotes.di.TokenStoreEntryPoint

@Composable
fun MpinLoginScreen(
    navController: NavHostController,
    mobile: String,
    lockedSecondsInit: Int = 0,
    viewModel: MpinViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsState()
    val haptic  = LocalHapticFeedback.current
    val context = LocalContext.current
    val tokenStore = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TokenStoreEntryPoint::class.java
        ).tokenStore()
    }
    val biometricEnabled = remember { tokenStore.isBiometricEnabled() }

    // Init lockout if passed from LoginViewModel
    LaunchedEffect(lockedSecondsInit) {
        if (lockedSecondsInit > 0) viewModel.initLockout(lockedSecondsInit)
    }

    // Navigate to Main on success
    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Auto-submit when 6 digits entered
    val pin = state.mpinDigits.joinToString("")
    LaunchedEffect(pin) {
        if (pin.length == 6 && !state.isLocked) viewModel.loginWithMpin(mobile)
    }

    // Show biometric on screen entry if enabled
    LaunchedEffect(Unit) {
        if (biometricEnabled && !state.isLocked) {
            AppBiometricManager.authenticate(
                context = context,
                title = "BPSCNotes",
                subtitle = "Login with your fingerprint",
                onSuccess = { viewModel.loginWithMpin(mobile) },
                onError   = { /* fallback to MPIN — already showing */ },
                onFallback = { /* user tapped cancel — show MPIN */ }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BpscColors.Primary, Color(0xFF1557C0))))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Welcome Back", style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Text("+91 $mobile", style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.75f))
                TextButton(onClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = false }
                    }
                }) {
                    Text("Change number", color = Color.White.copy(0.6f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Biometric button ───────────────────────────────────
        if (biometricEnabled && !state.isLocked) {
            OutlinedButton(
                onClick = {
                    AppBiometricManager.authenticate(
                        context   = context,
                        title     = "BPSCNotes",
                        subtitle  = "Login with your fingerprint",
                        onSuccess = { viewModel.loginWithMpin(mobile) },
                        onError   = {},
                        onFallback = {}
                    )
                },
                modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.5.dp, BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Fingerprint, null, tint = BpscColors.Primary,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Login with Biometric", color = BpscColors.Primary,
                    style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
            Text("— or enter MPIN —", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
        } else {
            Text("Enter your MPIN", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
        }

        // ── Lockout message ────────────────────────────────────
        AnimatedVisibility(visible = state.isLocked) {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3))
            ) {
                Row(Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                    Text(
                        buildString {
                            append("Too many attempts. Try again in ")
                            val m = state.lockedSecondsLeft / 60
                            val s = state.lockedSecondsLeft % 60
                            if (m > 0) append("${m}m ")
                            append("${s}s")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── MPIN dots ──────────────────────────────────────────
        MpinDots(digits = state.mpinDigits, hasError = state.error != null && !state.isLocked)
        Spacer(Modifier.height(8.dp))

        // ── Error message ──────────────────────────────────────
        AnimatedVisibility(visible = state.error != null && !state.isLocked) {
            Text(
                state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Numpad ─────────────────────────────────────────────
        MpinNumpad(
            enabled = !state.isLocked && !state.isLoading,
            onDigit = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onMpinDigit(it)
            },
            onBackspace = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onMpinBackspace()
            }
        )

        // ── Forgot MPIN ────────────────────────────────────────
        TextButton(
            onClick = { navController.navigate(Screen.ForgotMpin.createRoute(mobile)) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Forgot MPIN?", color = BpscColors.Primary,
                style = MaterialTheme.typography.bodyMedium)
        }

        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = BpscColors.Primary
            )
        }
    }
}
