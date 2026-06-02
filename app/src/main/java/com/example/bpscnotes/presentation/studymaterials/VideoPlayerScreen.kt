package com.example.bpscnotes.presentation.studymaterials

import android.app.Activity
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ads.AdManager
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

@Composable
fun VideoPlayerScreen(
    videoUrl:      String,
    title:         String,
    navController: NavHostController,
    adManager:     AdManager? = null
) {
    val context  = LocalContext.current
    val activity = context as? Activity

    var videoReady by remember { mutableStateOf(adManager == null) }

    LaunchedEffect(Unit) {
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfReady(activity) {
                videoReady = true
            }
        }
    }

    fun navigateBack() {
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfReady(activity) {
                navController.popBackStackSafe()
            }
        } else {
            navController.popBackStackSafe()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (videoReady) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val mc = MediaController(ctx)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        // Strip file:// prefix — VideoView.setVideoPath expects a raw path
                        val path = if (videoUrl.startsWith("file://")) videoUrl.removePrefix("file://") else videoUrl
                        setVideoPath(path)
                        setOnPreparedListener { mp ->
                            mp.start()
                            mp.isLooping = false
                        }
                        setOnErrorListener { _, _, _ -> false }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = BpscColors.Primary)
                Spacer(Modifier.height(12.dp))
                Text("Loading…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Top bar overlay
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
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { navigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
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
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    BackHandler { navigateBack() }
}