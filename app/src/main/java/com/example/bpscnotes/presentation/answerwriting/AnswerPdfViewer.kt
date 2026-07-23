package com.example.bpscnotes.presentation.answerwriting

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.pdf.downloadPdf
import com.example.bpscnotes.core.pdf.renderPdfPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────
// Full-screen viewer for an answer PDF — reuses the study-material
// PDF helpers (download + render to bitmaps). Used from the answer
// detail (my answer) and the peer-review screen. The uploads are
// served publicly, so no auth token is needed.
// ─────────────────────────────────────────────────────────────
@Composable
fun AnswerPdfDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val str = LocalStrings.current
    var pages by remember(url) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        loading = true; error = null
        try {
            pages = withContext(Dispatchers.IO) {
                val file = downloadPdf(url, context.cacheDir)
                renderPdfPages(file)
            }
            if (pages.isEmpty()) error = "Could not open this PDF."
        } catch (e: Exception) {
            error = "Could not open this PDF. Please check your connection."
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF202124))) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    str.awAnswerPdfTitle, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(color = Color.White)
                    error != null -> Text(error!!, color = Color.White, modifier = Modifier.padding(24.dp))
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pages) { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

// Compact "Open PDF" button shown wherever an answer has a PDF.
@Composable
fun OpenAnswerPdfButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val str = LocalStrings.current
    OutlinedButton(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Text("📄  ", style = MaterialTheme.typography.bodyMedium)
        Text(str.awOpenPdfAnswer, fontWeight = FontWeight.Bold)
    }
}
