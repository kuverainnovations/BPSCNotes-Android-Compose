package com.example.bpscnotes.presentation.liveclasses

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────
// LiveClassViewerScreen
//
// Opens a live class meeting link inside the app using WebView.
// - Handles Google Meet / Zoom / Teams / custom URLs
// - Requests camera + mic permissions for the WebView
// - Back guard prevents accidental exit mid-class
// - Falls back to external browser if WebView can't handle the URL
// ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveClassViewerScreen(
    navController:  NavHostController,
    meetingUrl:     String,
    classTitle:     String,
    instructor:     String,
    durationMins:   Int
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var pageTitle       by remember { mutableStateOf(classTitle) }
    var isLoading       by remember { mutableStateOf(true) }
    var loadError       by remember { mutableStateOf<String?>(null) }
    var showExitDialog  by remember { mutableStateOf(false) }
    var showControls    by remember { mutableStateOf(true) }
    var webViewRef      by remember { mutableStateOf<WebView?>(null) }
    var canGoBack       by remember { mutableStateOf(false) }
    var elapsedSecs     by remember { mutableIntStateOf(0) }

    // Permissions for camera + microphone (needed for video calls in WebView)
    var permissionsGranted by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }
    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ))
    }

    // Elapsed timer — shows how long user has been in the class
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSecs++
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            showExitDialog = true
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🚪", fontSize = 24.sp)
                    Text("Leave Class?", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                }
            },
            text = {
                Text(
                    "You're currently in \"$classTitle\". Are you sure you want to leave?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false; navController.popBackStackSafe() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Leave", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Stay", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {

        // ── WebView ───────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize().clickable { showControls = true },
            factory  = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled        = true
                        domStorageEnabled        = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess          = true
                        allowContentAccess       = true
                        useWideViewPort          = true
                        loadWithOverviewMode     = true
                        setSupportZoom(true)
                        // Use a desktop user agent — Google Meet works much better
                        userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Safari/537.36"
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            // Grant camera + mic to WebView automatically
                            request?.grant(request.resources)
                        }
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank() && title != "about:blank") {
                                pageTitle = title
                            }
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            loadError = null
                            canGoBack = view?.canGoBack() == true
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                        }
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                loadError = "Couldn't load the class page. Check your connection."
                            }
                        }
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Let http/https load in WebView
                            if (url.startsWith("http://") || url.startsWith("https://")) return false
                            // Deep links (zoomus://, msteams://, etc.) → open natively
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {}
                            return true
                        }
                    }
                    webViewRef = this
                    loadUrl(if (meetingUrl.startsWith("http")) meetingUrl else "https://$meetingUrl")
                }
            }
        )

        // ── Loading overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = isLoading,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0D0D0D)), contentAlignment = Alignment.Center) {
                AppLoader(message = "Joining class…")
            }
        }

        // ── Error state ───────────────────────────────────────
        if (loadError != null && !isLoading) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0D0D0D)), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⚠️", fontSize = 48.sp)
                    Text("Couldn't load class", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(loadError!!, color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    // Open in browser as fallback
                    Button(
                        onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))) } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Browser", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { navController.popBackStackSafe() },
                        shape   = RoundedCornerShape(12.dp),
                        border  = BorderStroke(1.dp, Color.White.copy(0.3f)),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Go Back") }
                }
            }
        }

        // ── Top control bar ───────────────────────────────────
        AnimatedVisibility(
            visible  = showControls && !isLoading && loadError == null,
            enter    = slideInVertically { -it } + fadeIn(),
            exit     = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back / exit button
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable {
                                if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                                else showExitDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (canGoBack) Icons.Rounded.ArrowBack else Icons.Rounded.Close,
                            null, tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            classTitle, color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            instructor, color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // LIVE badge + elapsed time
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE74C3C))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Pulsing dot
                        val pulse by rememberInfiniteTransition(label = "pulse")
                            .animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(pulse)))
                        Text("LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        Text(
                            formatElapsed(elapsedSecs),
                            color = Color.White.copy(0.8f),
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ── Bottom control bar ────────────────────────────────
        AnimatedVisibility(
            visible  = showControls && !isLoading && loadError == null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reload button
                    ControlButton(
                        icon  = Icons.Rounded.Refresh,
                        label = "Reload",
                        tint  = Color.White,
                        bg    = Color.White.copy(0.15f),
                        onClick = { webViewRef?.reload() }
                    )

                    // Open in browser (fallback)
                    ControlButton(
                        icon  = Icons.Rounded.OpenInBrowser,
                        label = "Browser",
                        tint  = Color.White,
                        bg    = Color.White.copy(0.15f),
                        onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))) } catch (_: Exception) {}
                        }
                    )

                    // Leave class button — prominent red
                    ControlButton(
                        icon    = Icons.Rounded.CallEnd,
                        label   = "Leave",
                        tint    = Color.White,
                        bg      = Color(0xFFE74C3C),
                        size    = 56.dp,
                        onClick = { showExitDialog = true }
                    )

                    // Copy link
                    ControlButton(
                        icon  = Icons.Rounded.ContentCopy,
                        label = "Copy Link",
                        tint  = Color.White,
                        bg    = Color.White.copy(0.15f),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Meeting link", meetingUrl))
                        }
                    )

                    // Share link
                    ControlButton(
                        icon  = Icons.Rounded.Share,
                        label = "Share",
                        tint  = Color.White,
                        bg    = Color.White.copy(0.15f),
                        onClick = {
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join live class: $classTitle\n$meetingUrl")
                                }, "Share class link"
                            ))
                        }
                    )
                }
            }
        }

        // ── Tap hint — first 2 seconds ────────────────────────
        var showHint by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(2000); showHint = false }
        AnimatedVisibility(
            visible  = showHint,
            exit     = fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Tap anywhere for controls", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon:    ImageVector,
    label:   String,
    tint:    Color,
    bg:      Color,
    size:    Dp = 44.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.45f))
        }
        Text(label, color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
    }
}

private fun formatElapsed(secs: Int): String {
    val m = secs / 60; val s = secs % 60
    return "%02d:%02d".format(m, s)
}