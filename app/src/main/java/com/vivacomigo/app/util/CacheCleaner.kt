package com.vivacomigo.app.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Utility for cleaning old cached files
 *
 * Features:
 * - Removes cache files older than 7 days
 * - Calculates total cache size
 * - Safe cleanup (doesn't crash on errors)
 * - Logs cleanup activity
 */
object CacheCleaner {

    private const val TAG = "CacheCleaner"
    private const val MAX_CACHE_AGE_DAYS = 7

    /**
     * Clean old cached files (older than MAX_CACHE_AGE_DAYS)
     *
     * This includes:
     * - Temporary camera photos
     * - Downloaded photos
     * - Any other cached files
     *
     * @param context Android context
     * @return Number of files deleted
     */
    fun cleanOldCache(context: Context): Int {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                Log.d(TAG, "Cache directory doesn't exist")
                return 0
            }

            val cutoffTime = System.currentTimeMillis() - (MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000L)
            var deletedCount = 0

            Log.d(TAG, "Starting cache cleanup (files older than $MAX_CACHE_AGE_DAYS days)...")

            // Recursively walk through cache directory
            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    val fileSizeKB = file.length() / 1024
                    val fileName = file.name
                    val deleted = file.delete()

                    if (deleted) {
                        deletedCount++
                        Log.d(TAG, "Deleted old cache: $fileName (${fileSizeKB}KB)")
                    } else {
                        Log.w(TAG, "Failed to delete: $fileName")
                    }
                }
            }

            if (deletedCount > 0) {
                Log.i(TAG, "✅ Cache cleanup complete: $deletedCount files deleted")
            } else {
                Log.d(TAG, "No old cache files to clean")
            }

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache: ${e.message}", e)
            0
        }
    }

    /**
     * Get total cache size in bytes
     *
     * @param context Android context
     * @return Total cache size in bytes
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                return 0
            }

            cacheDir.walkTopDown().sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size: ${e.message}", e)
            0
        }
    }

    /**
     * Get human-readable cache size
     *
     * @param context Android context
     * @return Cache size as formatted string (e.g., "2.5 MB")
     */
    fun getCacheSizeFormatted(context: Context): String {
        val bytes = getCacheSize(context)
        return formatFileSize(bytes)
    }

    /**
     * Clean ALL cache (regardless of age)
     * Use with caution!
     *
     * @param context Android context
     * @return Number of files deleted
     */
    fun cleanAllCache(context: Context): Int {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                return 0
            }

            var deletedCount = 0

            Log.w(TAG, "⚠️  Cleaning ALL cache files...")

            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile && file != cacheDir) {
                    val deleted = file.delete()
                    if (deleted) {
                        deletedCount++
                        Log.d(TAG, "Deleted: ${file.name}")
                    }
                }
            }

            Log.i(TAG, "✅ All cache cleaned: $deletedCount files deleted")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning all cache: ${e.message}", e)
            0
        }
    }

    /**
     * Format file size in human-readable format
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Get cache statistics
     *
     * @param context Android context
     * @return Map with cache statistics
     */
    fun getCacheStats(context: Context): Map<String, Any> {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                return mapOf(
                    "exists" to false,
                    "totalSize" to 0L,
                    "totalFiles" to 0,
                    "oldFiles" to 0
                )
            }

            val cutoffTime = System.currentTimeMillis() - (MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000L)
            var totalFiles = 0
            var oldFiles = 0
            var totalSize = 0L

            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalFiles++
                    totalSize += file.length()
                    if (file.lastModified() < cutoffTime) {
                        oldFiles++
                    }
                }
            }

            mapOf(
                "exists" to true,
                "totalSize" to totalSize,
                "totalSizeFormatted" to formatFileSize(totalSize),
                "totalFiles" to totalFiles,
                "oldFiles" to oldFiles,
                "maxAgeDays" to MAX_CACHE_AGE_DAYS
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache stats: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
