package com.example.bpscnotes.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Downscale + JPEG-recompress an image picked from camera or gallery before
 * upload.
 *
 * Why: a camera capture is a full-sensor JPEG (often 5–12 MB). Uploaded raw
 * on a weak connection it can't finish inside the HTTP write timeout, so the
 * answer submit hung on "Submitting…" then failed with "Request timed out".
 * A downsampled ~1600 px JPEG is a few hundred KB — it uploads in a moment,
 * stores less, and loads faster for the mentor. Camera photos also carry
 * their rotation in EXIF; we bake it in so the answer isn't sideways.
 */
object ImageCompressor {

    /** Longest edge of the output; plenty for reading handwritten answers. */
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 82

    /**
     * Returns a compressed JPEG for [uri]. Falls back to the original bytes if
     * the image can't be decoded (e.g. an unexpected format), so a submit
     * never fails just because compression didn't apply.
     */
    fun compressToJpeg(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // 1) Bounds only — decide the downsample factor without loading pixels.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return rawBytes(context, uri)

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(w, h, MAX_DIMENSION)
        }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return rawBytes(context, uri)

        // 2) Apply EXIF rotation (camera photos are usually stored rotated).
        bmp = applyExifRotation(context, uri, bmp)

        // 3) Final clamp to the long edge, then JPEG-encode.
        bmp = scaleToMax(bmp, MAX_DIMENSION)
        return ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private fun rawBytes(context: Context, uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw java.io.IOException("Could not read the selected image")

    /** Largest power-of-two that keeps the decoded image ≥ [target] on both edges. */
    private fun sampleSize(w: Int, h: Int, target: Int): Int {
        var sample = 1
        var halfW = w / 2
        var halfH = h / 2
        while (halfW >= target && halfH >= target) {
            sample *= 2
            halfW /= 2
            halfH /= 2
        }
        return sample
    }

    private fun scaleToMax(bmp: Bitmap, max: Int): Bitmap {
        val longEdge = maxOf(bmp.width, bmp.height)
        if (longEdge <= max) return bmp
        val ratio = max.toFloat() / longEdge
        val scaled = Bitmap.createScaledBitmap(
            bmp, (bmp.width * ratio).toInt().coerceAtLeast(1),
            (bmp.height * ratio).toInt().coerceAtLeast(1), true
        )
        if (scaled != bmp) bmp.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bmp: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> m.postScale(1f, -1f)
            else -> return bmp
        }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (rotated != bmp) bmp.recycle()
        return rotated
    }
}
