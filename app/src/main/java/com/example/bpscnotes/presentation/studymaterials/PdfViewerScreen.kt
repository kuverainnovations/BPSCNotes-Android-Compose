package com.example.bpscnotes.presentation.studymaterials

import com.example.bpscnotes.core.language.LocalStrings
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

// ════════════════════════════════════════════════════════════
// PdfViewerScreen — Custom in-app PDF renderer
//
// WHY NOT Intent.ACTION_VIEW or Google Docs Viewer?
// Both hand the PDF off to an external app/browser which shows
// ALL pages with zero control. Locking is impossible externally.
//
// THIS SCREEN:
// - Downloads the PDF to app's private cache
// - Uses Android's PdfRenderer to render each page as a Bitmap
// - Pages 1..freePages  → shown normally
// - Pages freePages+1.. → blurred + lock overlay with buy CTA
// - Users cannot scroll past the lock, cannot share/extract the cached file
//   (it lives in context.cacheDir which is app-private)
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    fileUrl:       String,
    title:         String,
    freePages:     Int     = 3,
    isPurchased:   Boolean = false,
    navController: NavHostController,
    authToken:     String  = "",      // Bearer token for authenticated file URLs
    onPurchase:    () -> Unit = {}
) {
    val str = LocalStrings.current
    val context    = LocalContext.current
    val listState  = rememberLazyListState()

    var pdfPages   by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading  by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }
    var showBuyDialog by remember { mutableStateOf(false) }

    val effectiveFreePages = if (isPurchased) Int.MAX_VALUE else freePages.coerceAtLeast(1)

    // Download + render PDF in background
    LaunchedEffect(fileUrl) {
        isLoading = true
        error     = null
        try {
            val cachedFile = withContext(Dispatchers.IO) {
                downloadPdf(fileUrl, context.cacheDir, authToken)
            }
            val bitmaps = withContext(Dispatchers.Default) {
                renderPdfPages(cachedFile)
            }
            pdfPages   = bitmaps
            totalPages = bitmaps.size
        } catch (e: Exception) {
            Log.e("PdfViewer", "Failed: ${e.message}", e)
            error = e.message ?: "Could not open PDF"
        } finally {
            isLoading = false
        }
    }

    // Buy dialog
    if (showBuyDialog) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title = {
                Text(str.pdfUnlock, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${str.pdfReadFreePages} $freePages/$totalPages.",
                        style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    Text("${str.pdfPurchaseUnlock}",
                        style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBuyDialog = false; onPurchase() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text(str.pdfBuyAccess, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuyDialog = false }) {
                    Text(str.pdfMaybeLater, color = BpscColors.TextHint)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF1A1A2E),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (totalPages > 0) {
                            Text(
                                if (isPurchased) "$totalPages ${str.quizQuestions} · ${str.pdfFullAccess}"
                                else "$freePages / $totalPages ${str.coursesFree}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPurchased) Color(0xFF4CAF50) else Color(0xFFFFA726)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (!isPurchased && totalPages > freePages) {
                        TextButton(onClick = { showBuyDialog = true }) {
                            Text(str.pdfUnlock, color = Color(0xFFFFA726),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(44.dp))
                        Text(str.pdfLoadingPdf, color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }

                error != null -> {
                    Column(
                        Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Text(str.pdfCantLoad, color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge)
                        Text(error!!, color = Color.White.copy(0.6f),
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Button(onClick = { navController.popBackStack() },
                            shape = RoundedCornerShape(12.dp)) {
                            Text(str.pdfGoBack)
                        }
                    }
                }

                pdfPages.isEmpty() -> {
                    Text(str.pdfNoPages, color = Color.White.copy(0.5f))
                }

                else -> {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Page number indicator
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                Arrangement.SpaceBetween, Alignment.CenterVertically
                            ) {
                                Text("${pdfPages.size} pages",
                                    color = Color.White.copy(0.5f),
                                    style = MaterialTheme.typography.labelSmall)
                                if (!isPurchased && totalPages > freePages) {
                                    Text("🔒 ${str.roomsLocked} (${str.pdfGoBack} $freePages)",
                                        color = Color(0xFFFFA726),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Render pages
                        items(pdfPages.size, key = { it }) { pageIndex ->
                            val pageNumber   = pageIndex + 1
                            val isLocked     = pageNumber > effectiveFreePages

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                // ── The page bitmap ──────────────────────────
                                Image(
                                    bitmap              = pdfPages[pageIndex].asImageBitmap(),
                                    contentDescription  = "${str.pdfPageNum} $pageNumber",
                                    modifier            = Modifier
                                        .fillMaxWidth()
                                        .then(if (isLocked) Modifier.blur(16.dp) else Modifier),
                                    contentScale        = ContentScale.FillWidth
                                )

                                // ── Page number badge (top-left) ─────────────
                                if (!isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(0.55f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("$pageNumber / $totalPages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White)
                                    }
                                }

                                // ── Lock overlay for locked pages ────────────
                                if (isLocked) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(0.3f),
                                                        Color.Black.copy(0.85f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            // Lock icon
                                            Box(
                                                Modifier.size(64.dp).clip(CircleShape)
                                                    .background(Color.White.copy(0.15f)),
                                                Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.Lock, null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp))
                                            }

                                            Text("${str.pdfPageLocked} $pageNumber",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold)

                                            Text(
                                                "You've read $freePages free pages.\nUnlock all $totalPages pages to continue.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(0.8f),
                                                textAlign = TextAlign.Center
                                            )

                                            Button(
                                                onClick = { showBuyDialog = true },
                                                modifier = Modifier.height(50.dp).fillMaxWidth(0.75f),
                                                shape    = RoundedCornerShape(14.dp),
                                                colors   = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFFA726)
                                                )
                                            ) {
                                                Text(str.pdfUnlock,
                                                    color = Color(0xFF1A1A1A),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.titleMedium)
                                            }

                                            Text("Remaining ${totalPages - freePages} pages locked",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(0.5f))
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom paywall banner (if not all pages free)
                        if (!isPurchased && totalPages > freePages) {
                            item {
                                Card(
                                    modifier  = Modifier.fillMaxWidth().padding(8.dp),
                                    shape     = RoundedCornerShape(16.dp),
                                    colors    = CardDefaults.cardColors(
                                        containerColor = Color(0xFF1E1E3F)
                                    ),
                                    border    = BorderStroke(1.dp, Color(0xFFFFA726).copy(0.5f))
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        Arrangement.SpaceBetween,
                                        Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("🔒 ${totalPages - freePages} more pages locked",
                                                color = Color(0xFFFFA726),
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleSmall)
                                            Text("${str.pdfPurchaseAccess}",
                                                color = Color.White.copy(0.7f),
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(
                                            onClick = { showBuyDialog = true },
                                            shape   = RoundedCornerShape(12.dp),
                                            colors  = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFA726)
                                            )
                                        ) {
                                            Text(str.pdfUnlock, color = Color(0xFF1A1A1A),
                                                fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// HELPERS — download + render on IO/Default dispatchers
// ════════════════════════════════════════════════════════════

/** Download PDF to app-private cache. Returns the local file. */
private suspend fun downloadPdf(url: String, cacheDir: File, authToken: String = ""): File {
    val fileName = "pdf_${url.hashCode()}.pdf"
    val file     = File(cacheDir, fileName)

    // Return cached version if fresh (< 1 hour)
    if (file.exists() && file.length() > 0 &&
        (System.currentTimeMillis() - file.lastModified()) < 3_600_000L) {
        return file
    }

    val client  = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val reqBuilder = Request.Builder().url(url)
    if (authToken.isNotBlank()) {
        reqBuilder.addHeader("Authorization", "Bearer $authToken")
    }
    val response = client.newCall(reqBuilder.build()).execute()

    if (!response.isSuccessful) {
        throw Exception("Server returned ${response.code}. Please check your connection and try again.")
    }

    val body = response.body ?: throw Exception("Empty response — the file may be unavailable.")
    val bytes = body.bytes()
    if (bytes.isEmpty()) throw Exception("Downloaded file is empty.")

    // Verify it's actually a PDF (starts with %PDF magic bytes)
    val magic = bytes.take(4).toByteArray().toString(Charsets.ISO_8859_1)

    if (bytes.size < 4 || !magic.startsWith("%PDF")) {

        val ext = url.substringAfterLast('.').lowercase()

        throw Exception(
            when {
                ext in listOf("mp4", "mkv", "webm", "avi", "mov") ->
                    "This is a video file. Use a video player app to open it.\nTap back and use the external viewer."

                ext in listOf("jpg", "jpeg", "png", "webp") ->
                    "This is an image file — it can't be rendered as a PDF."

                else ->
                    "File is not a valid PDF (got: $magic). It may be corrupted or in an unsupported format."
            }
        )
    }

    FileOutputStream(file).use { out -> out.write(bytes) }
    return file
}

/** Render all pages of a PDF file to Bitmaps. Screen-width aware. */
private fun renderPdfPages(file: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    val pfd     = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    PdfRenderer(pfd).use { renderer ->
        val targetWidth = 1080   // render at 1080px wide for crisp display on all screens

        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { page ->
                val scale    = targetWidth.toFloat() / page.width.toFloat()
                val bmpWidth = targetWidth
                val bmpHeight = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                // White background (PDFs are transparent by default in PdfRenderer)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
            }
        }
    }
    return bitmaps
}