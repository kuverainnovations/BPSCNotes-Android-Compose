package com.example.bpscnotes.core.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/** Download a PDF to app-private cache and return the local File. */
suspend fun downloadPdf(url: String, cacheDir: File, authToken: String = ""): File {
    if (url.startsWith("file://")) {
        val localFile = File(url.removePrefix("file://"))
        if (localFile.exists() && localFile.length() > 0) return localFile
        throw Exception("Downloaded file not found. Please re-download it.")
    }

    val fileName = "pdf_${url.hashCode()}.pdf"
    val file     = File(cacheDir, fileName)

    // Return cached version if fresh (< 1 hour)
    if (file.exists() && file.length() > 0 &&
        (System.currentTimeMillis() - file.lastModified()) < 3_600_000L) {
        return file
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val reqBuilder = Request.Builder().url(url)
    if (authToken.isNotBlank()) reqBuilder.addHeader("Authorization", "Bearer $authToken")

    val response = client.newCall(reqBuilder.build()).execute()
    if (!response.isSuccessful) throw Exception("Server returned ${response.code}. Please check your connection.")

    val body  = response.body ?: throw Exception("Empty response — the file may be unavailable.")
    val bytes = body.bytes()
    if (bytes.isEmpty()) throw Exception("Downloaded file is empty.")

    val magic = bytes.take(4).toByteArray().toString(Charsets.ISO_8859_1)
    if (bytes.size < 4 || !magic.startsWith("%PDF")) {
        val ext = url.substringAfterLast('.').lowercase()
        throw Exception(when {
            ext in listOf("mp4", "mkv", "webm", "avi", "mov") ->
                "This is a video file — use the video player."
            ext in listOf("jpg", "jpeg", "png", "webp") ->
                "This is an image file, not a PDF."
            else ->
                "File is not a valid PDF (got: $magic). It may be corrupted."
        })
    }

    FileOutputStream(file).use { out -> out.write(bytes) }
    return file
}

/** Render all pages of a PDF file to Bitmaps at exactly 1080px wide.
 *  Height is capped at 8192px to prevent Compose Constraints overflow:
 *  keeping bmpWidth fixed at 1080 means ContentScale.FillWidth never
 *  up-scales a tiny bitmap to an extreme display height. */
fun renderPdfPages(file: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    val pfd     = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    PdfRenderer(pfd).use { renderer ->
        val targetWidth    = 1080
        val maxBitmapHeight = 8192
        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { page ->
                if (page.width <= 0 || page.height <= 0) return@use
                val scale     = targetWidth.toFloat() / page.width.toFloat()
                val bmpWidth  = targetWidth
                val bmpHeight = (page.height * scale).toInt()
                    .coerceIn(1, maxBitmapHeight)
                try {
                    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                } catch (_: Exception) { /* skip unrenderable pages */ }
            }
        }
    }
    return bitmaps
}
