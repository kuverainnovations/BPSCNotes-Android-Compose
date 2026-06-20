package com.example.bpscnotes.presentation.currentaffairs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ads.MediumRectangleAdView
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import kotlinx.coroutines.delay

private const val GATE_SECONDS = 15

/**
 * Shown between tapping an article in the list and the article actually
 * opening — a custom 15s countdown wrapping a medium-rectangle AdMob unit
 * (deliberately not a stock Interstitial, which Google controls the close
 * timing/UI for; this needs an exact, app-controlled 15s gate). The user
 * can still back out at any point via the close button — only *moving
 * forward* to the article is gated, not exiting.
 */
@Composable
fun CaAdGateScreen(
    navController: NavHostController,
    affairId: String,
    target: String = "article",
    adManager: AdManager?
) {
    val str = LocalStrings.current
    var secondsLeft by remember { mutableIntStateOf(GATE_SECONDS) }
    val unlocked = secondsLeft <= 0

    LaunchedEffect(affairId, target) {
        secondsLeft = GATE_SECONDS
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    fun proceedToDestination() {
        val nextRoute = if (target == "mcq") "ca_mcq_quiz/$affairId" else Screen.CaArticleDetail.createRoute(affairId)
        navController.navigate(nextRoute) {
            // Removes this gate screen from the back stack so the system
            // back button from the destination goes straight to whatever
            // was below the gate, not back into another 15s wait.
            popUpTo("ca_ad_gate/{affairId}/{target}") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                    start = Offset(0f, 0f), end = Offset(500f, 800f)
                )
            )
    ) {
        Box(
            modifier = Modifier.size(40.dp).statusBarsPadding().padding(12.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                .clickable { navController.popBackStackSafe() }
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                when {
                    unlocked && target == "mcq" -> str.caGateReadyMcq
                    unlocked -> str.caGateReady
                    else -> str.caGateWaiting
                },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                str.caGateSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(24.dp))

            if (adManager != null) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    MediumRectangleAdView(adUnitId = adManager.getGatedReadAdUnitId())
                }
                Spacer(Modifier.height(28.dp))
            }

            // Countdown ring — same drawArc technique as the MCQ quiz timer.
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep = ((GATE_SECONDS - secondsLeft) / GATE_SECONDS.toFloat()) * 360f
                    drawArc(
                        Color.White.copy(alpha = 0.2f), -90f, 360f, false,
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        Color.White, -90f, sweep, false,
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                if (unlocked) {
                    Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("$secondsLeft", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { proceedToDestination() },
                enabled = unlocked,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0A2472),
                    disabledContainerColor = Color.White.copy(alpha = 0.25f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    if (unlocked) {
                        if (target == "mcq") str.caGateContinueMcq else str.caGateContinue
                    } else {
                        str.caGateWaitButton.replace("{s}", "$secondsLeft")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}