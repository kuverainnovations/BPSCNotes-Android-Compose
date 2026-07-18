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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun CreateMpinScreen(
    navController: NavHostController,
    viewModel: MpinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val str = com.example.bpscnotes.core.language.LocalStrings.current

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(color = Color(0xFF0A2472), darkIcons = false)
    }

    var showBiometricDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.mpinCreated) {
        if (state.mpinCreated) { viewModel.consumeMpinCreated(); showBiometricDialog = true }
    }

    if (showBiometricDialog) {
        BiometricOptInDialog(
            onEnable = { showBiometricDialog = false; navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } } },
            onSkip   = { showBiometricDialog = false; navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } } }
        )
    }

    val pin            = state.mpinDigits.joinToString("")
    val confirm        = state.confirmDigits.joinToString("")
    val enteringConfirm = pin.length == 4
    val bothFilled     = pin.length == 4 && confirm.length == 4

    // Auto-submit when confirm is filled
    LaunchedEffect(bothFilled) {
        if (bothFilled) viewModel.createMpin()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E3A8A))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Icon
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(str.mpinCreateTitle, style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(str.mpinCreateSubtitle,
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.65f))

            Spacer(Modifier.height(40.dp))

            // Step indicator
            MpinStepIndicator(step = if (!enteringConfirm) 1 else 2)

            Spacer(Modifier.height(20.dp))

            // Label
            Text(
                if (!enteringConfirm) "Enter your MPIN" else "Confirm your MPIN",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(0.85f), fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))

            // Active dots
            MpinDots(
                digits   = if (!enteringConfirm) state.mpinDigits else state.confirmDigits,
                hasError = state.error != null || state.confirmError != null
            )

            Spacer(Modifier.height(16.dp))

            // Error message
            val errMsg = state.confirmError ?: state.error
            AnimatedVisibility(
                visible = errMsg != null,
                enter = fadeIn() + slideInVertically(),
                exit  = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE74C3C).copy(0.2f))
                        .border(1.dp, Color(0xFFE74C3C).copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp))
                        Text(errMsg ?: "", color = Color(0xFFFF6B6B),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.height(16.dp))
            }

            MpinNumpad(
                enabled     = !state.isLoading,
                onDigit     = { d -> if (!enteringConfirm) viewModel.onMpinDigit(d) else viewModel.onConfirmDigit(d) },
                onBackspace = { if (confirm.isNotEmpty()) viewModel.onConfirmBackspace() else viewModel.onMpinBackspace() }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MpinStepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { i ->
            val active = i + 1 == step
            val done   = i + 1 < step
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (active) 24.dp else 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when { done -> Color(0xFF2ECC71); active -> Color.White; else -> Color.White.copy(0.3f) }
                    )
            )
        }
    }
}
