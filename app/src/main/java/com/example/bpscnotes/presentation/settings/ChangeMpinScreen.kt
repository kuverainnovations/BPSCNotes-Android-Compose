package com.example.bpscnotes.presentation.settings

import androidx.compose.animation.*
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
import com.example.bpscnotes.presentation.auth.mpin.MpinDots
import com.example.bpscnotes.presentation.auth.mpin.MpinNumpad
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun ChangeMpinScreen(
    navController: NavHostController,
    viewModel: ChangeMpinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val str = com.example.bpscnotes.core.language.LocalStrings.current

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(color = Color(0xFF0A2472), darkIcons = false)
    }

    LaunchedEffect(state.success) {
        if (state.success) navController.popBackStackSafe()
    }

    // activeField: 0=current, 1=new, 2=confirm
    val activeField     = viewModel.activeField
    val current         = state.currentDigits.joinToString("")
    val new             = state.newDigits.joinToString("")
    val confirm         = state.confirmDigits.joinToString("")
    val allFilled       = current.length == 4 && new.length == 4 && confirm.length == 4

    LaunchedEffect(allFilled) {
        if (allFilled) viewModel.changeMpin()
    }

    // Active dots to show
    val activeDots = when (activeField) {
        0    -> state.currentDigits
        1    -> state.newDigits
        else -> state.confirmDigits
    }
    val stepLabel = when (activeField) {
        0    -> "Enter current MPIN"
        1    -> "Enter new MPIN"
        else -> "Confirm new MPIN"
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E3A8A))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStackSafe() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LockReset, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(str.mpinChangeTitle, style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(str.mpinChangeSubtitle,
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.65f))

            Spacer(Modifier.height(32.dp))

            // 3-step indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val done   = i < activeField
                    val active = i == activeField
                    Box(modifier = Modifier.height(4.dp).width(if (active) 24.dp else 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(when { done -> Color(0xFF2ECC71); active -> Color.White; else -> Color.White.copy(0.3f) }))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(stepLabel, style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(0.85f), fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            MpinDots(
                digits   = activeDots,
                hasError = state.error != null || state.confirmError != null
            )

            Spacer(Modifier.height(16.dp))

            val errMsg = state.confirmError ?: state.error
            AnimatedVisibility(visible = errMsg != null, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                Box(modifier = Modifier.padding(horizontal = 32.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE74C3C).copy(0.2f))
                    .border(1.dp, Color(0xFFE74C3C).copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                        Text(errMsg ?: "", color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.height(16.dp))
            }

            MpinNumpad(
                enabled     = !state.isLoading,
                onDigit     = { d -> viewModel.onDigit(d) },
                onBackspace = { viewModel.onBackspace() }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}