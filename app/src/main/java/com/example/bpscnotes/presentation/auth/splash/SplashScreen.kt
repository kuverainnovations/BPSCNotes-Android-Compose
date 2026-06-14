package com.example.bpscnotes.presentation.auth.splash

import com.example.bpscnotes.core.language.LocalStrings
import com.kuvera.bpscnotes.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavHostController) {
    val str = LocalStrings.current
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ))
        }
        launch {
            alpha.animateTo(1f, animationSpec = tween(600))
        }
        delay(2200)
        val token = tokenStore.getToken()
        /*val destination = if (token.isNullOrEmpty()) {
            val isOnboarded = tokenStore.isOnboarded()
            if (isOnboarded) Screen.Login.route else Screen.Onboarding.route
        } else {
            Screen.Main.route
        }*/

        // Navigation decision tree:
        // 1. Has JWT token               → Main (already authenticated)
        // 2. Has saved mobile + hasMpin  → MPIN Login screen (skip mobile entry)
        // 3. Onboarded, no MPIN          → Login screen
        // 4. Never onboarded             → Onboarding (intro pages)
        //
        // NOTE: Language selection is not part of the first-launch flow for
        // now — it was previously shown before Onboarding on a fresh
        // install, but the screen isn't ready/used. The app defaults to
        // English (LanguageManager.current defaults to ENGLISH when no
        // language has been saved) and language can still be changed later
        // from Settings.
        val savedMobile = tokenStore.getUserMobile()
        val destination = when {
            tokenStore.hasValidToken()              -> Screen.Main.route
            !savedMobile.isNullOrBlank() && tokenStore.hasMpin() ->
                Screen.MpinLogin.createRoute(savedMobile)
            tokenStore.isOnboarded()               -> Screen.Login.route
            else                                   -> Screen.Onboarding.route
        }
        // TEMP DIAGNOSTIC — remove once the logout/reopen issue is confirmed fixed.
        // Filter logcat by tag "SplashDecision" to see exactly what Splash
        // reads from TokenStore on each cold start.
        android.util.Log.d(
            "SplashDecision",
            "token=${if (token.isNullOrEmpty()) "null/empty" else "PRESENT"}, " +
                    "tokenExpired=${if (token.isNullOrEmpty()) "n/a" else tokenStore.isTokenExpired()}, " +
                    "savedMobile=$savedMobile, hasMpin=${tokenStore.hasMpin()}, " +
                    "isOnboarded=${tokenStore.isOnboarded()}, destination=$destination"
        )
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BpscColors.Primary,
                        Color(0xFF0D47A1)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Replace with your actual Lottie or Image asset
                Image(
                    painter = painterResource(R.drawable.ic_bpsc_logo),
                    contentDescription = "BPSCNotes Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BPSCNotes",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = str.splashTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.alpha(alpha.value)
            )
        }

        // Version tag at bottom
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}