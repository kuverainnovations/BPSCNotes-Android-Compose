package com.example.bpscnotes.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.auth.mpin.MpinDots
import com.example.bpscnotes.presentation.auth.mpin.MpinNumpad
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun ChangeMpinScreen(
    navController: NavHostController,
    viewModel: ChangeMpinViewModel = hiltViewModel()
) {
    val state  by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val cs     = MaterialTheme.colorScheme

    LaunchedEffect(state.success) {
        if (state.success) {
            viewModel.consumeSuccess()
            navController.popBackStackSafe()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change MPIN", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafe() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {}
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(cs.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Rounded.LockReset, null, tint = BpscColors.Primary,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("Update your MPIN", style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface)
            Spacer(Modifier.height(32.dp))

            // Current MPIN
            Text("Current MPIN", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            MpinDots(digits = state.currentDigits, hasError = state.error != null)

            Spacer(Modifier.height(20.dp))

            // New MPIN
            Text("New MPIN", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            MpinDots(digits = state.newDigits)

            Spacer(Modifier.height(20.dp))

            // Confirm
            Text("Confirm New MPIN", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            MpinDots(digits = state.confirmDigits, hasError = state.confirmError != null)

            AnimatedVisibility(state.error != null) {
                Text(state.error ?: "", color = cs.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp))
            }
            AnimatedVisibility(state.confirmError != null) {
                Text(state.confirmError ?: "", color = cs.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp))
            }

            val all3Filled = state.currentDigits.all { it.isNotEmpty() } &&
                             state.newDigits.all { it.isNotEmpty() } &&
                             state.confirmDigits.all { it.isNotEmpty() }

            val hint = when (viewModel.activeField) {
                0 -> "Enter current MPIN"
                1 -> "Enter new MPIN"
                2 -> "Confirm new MPIN"
                else -> ""
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.weight(1f))

            MpinNumpad(
                enabled   = !state.isLoading,
                onDigit   = { d -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onDigit(d) },
                onBackspace = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onBackspace() }
            )

            AnimatedVisibility(all3Filled) {
                Button(
                    onClick  = { viewModel.changeMpin() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp).height(52.dp),
                    enabled  = !state.isLoading,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    if (state.isLoading)
                        CircularProgressIndicator(color = Color.White,
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Update MPIN", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}
