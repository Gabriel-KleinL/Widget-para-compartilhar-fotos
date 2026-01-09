package com.vivacomigo.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Utility class for compressing images before upload
 *
 * Features:
 * - Smart sampling (doesn't load full resolution unnecessarily)
 * - Resizes to maximum 1920x1920px (maintains aspect ratio)
 * - Adaptive JPEG compression (85% -> 60% quality)
 * - Target size: <500KB
 * - Memory efficient (recycles bitmaps)
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 1920
    private const val TARGET_SIZE_BYTES = 500_000 // 500KB
    private const val INITIAL_QUALITY = 85
    private const val MIN_QUALITY = 60
    private const val QUALITY_STEP = 5

    /**
     * Compress image from URI
     *
     * @param uri Image URI (from camera or gallery)
     * @param context Android context for content resolver
     * @return Result with compressed ByteArray or error
     */
    fun compressImage(uri: Uri, context: Context): Result<ByteArray> {
        return try {
            Log.d(TAG, "Starting compression for: $uri")

            // Step 1: Get original dimensions WITHOUT loading full image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val inputStream1 = context.contentResolver.openInputStream(uri)
            if (inputStream1 == null) {
                return Result.failure(Exception("Cannot open image"))
            }

            BitmapFactory.decodeStream(inputStream1, null, options)
            inputStream1.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            val originalSize = "Unknown" // Size not available without loading

            Log.d(TAG, "Original dimensions: ${originalWidth}x${originalHeight}")

            // Step 2: Calculate inSampleSize to reduce memory usage
            val sampleSize = calculateInSampleSize(originalWidth, originalHeight, MAX_DIMENSION)
            Log.d(TAG, "Using inSampleSize: $sampleSize")

            // Step 3: Decode bitmap with sampling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val inputStream2 = context.contentResolver.openInputStream(uri)
            if (inputStream2 == null) {
                return Result.failure(Exception("Cannot open image for decoding"))
            }

            val sampledBitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            if (sampledBitmap == null) {
                return Result.failure(Exception("Failed to decode image"))
            }

            Log.d(TAG, "Sampled bitmap: ${sampledBitmap.width}x${sampledBitmap.height}")

            // Step 4: Resize to maximum dimensions (if still needed)
            val resizedBitmap = resizeBitmapIfNeeded(sampledBitmap, MAX_DIMENSION)

            // Step 5: Compress with adaptive quality
            val compressedBytes = compressWithAdaptiveQuality(resizedBitmap)

            // Step 6: Cleanup
            sampledBitmap.recycle()
            if (resizedBitmap != sampledBitmap) {
                resizedBitmap.recycle()
            }

            Log.d(TAG, "Compression complete: ${compressedBytes.size} bytes (${compressedBytes.size / 1024}KB)")
            Log.d(TAG, "Dimensions after compression: ${originalWidth}x${originalHeight} -> ${resizedBitmap.width}x${resizedBitmap.height}")

            Result.success(compressedBytes)

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during compression", e)
            Result.failure(Exception("Não há memória suficiente para processar esta imagem. Tente uma foto menor."))
        } catch (e: Exception) {
            Log.e(TAG, "Error during compression: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Calculate appropriate inSampleSize to reduce memory usage
     *
     * inSampleSize = 1: Full resolution
     * inSampleSize = 2: Half resolution (1/4 memory)
     * inSampleSize = 4: Quarter resolution (1/16 memory)
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1

        // Calculate sample size as power of 2
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        return sampleSize
    }

    /**
     * Resize bitmap if dimensions exceed maximum
     * Maintains aspect ratio
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Already small enough
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        // Calculate scale factor
        val scale = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(TAG, "Resizing: ${width}x${height} -> ${newWidth}x${newHeight}")

        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * Compress bitmap with adaptive quality
     * Starts at 85% quality, reduces to 60% if needed to reach target size
     */
    private fun compressWithAdaptiveQuality(bitmap: Bitmap): ByteArray {
        var quality = INITIAL_QUALITY
        var outputStream = ByteArrayOutputStream()

        // Initial compression
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        Log.d(TAG, "Initial compression (quality $quality): ${outputStream.size()} bytes")

        // Reduce quality if still too large
        while (outputStream.size() > TARGET_SIZE_BYTES && quality >= MIN_QUALITY) {
            quality -= QUALITY_STEP
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            Log.d(TAG, "Re-compressing (quality $quality): ${outputStream.size()} bytes")
        }

        Log.d(TAG, "Final quality: $quality%")

        return outputStream.toByteArray()
    }

    /**
     * Get file size in human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
