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

/** Width every page is rasterised at for normal, unzoomed reading. */
const val PDF_BASE_WIDTH = 1080

/**
 * Re-render ONE page at a higher resolution, for when the user zooms in.
 *
 * The base pass rasterises at [PDF_BASE_WIDTH], which is roughly 1:1 with a
 * phone screen — so magnifying those bitmaps just enlarges their pixels and
 * the text goes soft exactly when someone is trying to read it closer.
 *
 * Only ever called for a single visible page. Doing the whole document at
 * this resolution is not an option: a page is width × height × 4 bytes, so
 * at 3x a fifty-page PDF would run to gigabytes.
 *
 * Returns null if the page can't be rendered (including OOM, which is a real
 * possibility at high zoom on a low-memory device — the caller keeps showing
 * the base bitmap in that case).
 */
fun renderPdfPageAtScale(file: File, pageIndex: Int, scale: Float): Bitmap? {
    val targetWidth = (PDF_BASE_WIDTH * scale.coerceIn(1f, 3f)).toInt()
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (pageIndex !in 0 until renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    if (page.width <= 0 || page.height <= 0) return null
                    val pageScale = targetWidth.toFloat() / page.width.toFloat()
                    val bmpHeight = (page.height * pageScale).toInt().coerceIn(1, 8192)
                    val bitmap = Bitmap.createBitmap(targetWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: Exception) {
        null
    }
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
