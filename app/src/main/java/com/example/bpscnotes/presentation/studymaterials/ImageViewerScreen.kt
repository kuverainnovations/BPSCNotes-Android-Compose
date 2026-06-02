package com.example.bpscnotes.presentation.studymaterials

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.studymaterials.clickable

// ════════════════════════════════════════════════════════════
// ImageViewerScreen — Full-screen in-app image viewer
//
// Features:
// - Pinch-to-zoom (up to 5×)
// - Pan after zoom
// - Interstitial ad on open AND on close
// ════════════════════════════════════════════════════════════

@Composable
fun ImageViewerScreen(
    imageUrl:      String,
    title:         String,
    navController: NavHostController,
    adManager:     AdManager? = null
) {
    val context  = LocalContext.current
    val activity = context as? Activity

    // Show ad on entry — image shows after dismiss
    var imageReady by remember { mutableStateOf(adManager == null) }
    LaunchedEffect(Unit) {
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfReady(activity) { imageReady = true }
        }
    }

    fun navigateBack() {
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfReady(activity) { navController.popBackStackSafe() }
        } else {
            navController.popBackStackSafe()
        }
    }

    // Pinch-zoom state
    var scale       by remember { mutableStateOf(1f) }
    var offsetX     by remember { mutableStateOf(0f) }
    var offsetY     by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (imageReady) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX       = scale,
                        scaleY       = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )
        } else {
            CircularProgressIndicator(
                color    = BpscColors.Primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.5f))
                    .clickable { navigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                title,
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Zoom hint
        if (scale <= 1f && imageReady) {
            Text(
                "Pinch to zoom",
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }

    BackHandler { navigateBack() }
}

@Composable
private fun Modifier.clickable(onClick: () -> Unit) = this.then(
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
)
