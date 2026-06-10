package com.example.bpscnotes.presentation.auth.mpin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen

/**
 * Shown after OTP verification during Forgot MPIN flow.
 * mobile + otp are passed from OtpScreen.
 */
@Composable
fun ResetMpinScreen(
    navController: NavHostController,
    mobile: String,
    otp: String,
    viewModel: MpinViewModel = hiltViewModel()
) {
    val state  by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val cs     = MaterialTheme.colorScheme

    var showBiometricDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.mpinCreated) {
        if (state.mpinCreated) {
            viewModel.consumeMpinCreated()
            showBiometricDialog = true
        }
    }

    if (showBiometricDialog) {
        BiometricOptInDialog(
            onEnable = {
                showBiometricDialog = false
                navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } }
            },
            onSkip = {
                showBiometricDialog = false
                navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().background(cs.background).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BpscColors.Primary, Color(0xFF1557C0))))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LockReset, null, tint = Color.White,
                    modifier = Modifier.size(40.dp))
                Text("Set New MPIN", style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Text("Choose a new 6-digit MPIN",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("New MPIN", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        MpinDots(digits = state.mpinDigits, hasError = state.error != null)

        Spacer(Modifier.height(24.dp))

        Text("Confirm MPIN", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        MpinDots(digits = state.confirmDigits, hasError = state.confirmError != null)

        AnimatedVisibility(state.error != null) {
            Text(state.error ?: "", color = cs.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp))
        }
        AnimatedVisibility(state.confirmError != null) {
            Text(state.confirmError ?: "", color = cs.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
        }

        val pin     = state.mpinDigits.joinToString("")
        val confirm = state.confirmDigits.joinToString("")
        val enteringConfirm = pin.length == 6

        Text(
            if (enteringConfirm) "Now confirm your MPIN" else "Enter 6 digits",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        MpinNumpad(
            enabled = !state.isLoading,
            onDigit = { d ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!enteringConfirm) viewModel.onMpinDigit(d)
                else viewModel.onConfirmDigit(d)
            },
            onBackspace = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (confirm.isNotEmpty()) viewModel.onConfirmBackspace()
                else viewModel.onMpinBackspace()
            }
        )

        AnimatedVisibility(visible = pin.length == 6 && confirm.length == 6) {
            Button(
                onClick = { viewModel.resetMpin(mobile, otp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp).height(52.dp),
                enabled = !state.isLoading,
                shape   = RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (state.isLoading) CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Reset MPIN", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
