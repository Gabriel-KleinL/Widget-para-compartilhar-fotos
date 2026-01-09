package com.vivacomigo.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.vivacomigo.app.data.api.RetrofitClient
import com.vivacomigo.app.data.model.Photo
import com.vivacomigo.app.util.ImageCompressor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PhotoRepository(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val authRepo = AuthRepository(context)

    /**
     * Upload photo to the API
     */
    suspend fun uploadPhoto(
        imageUri: Uri,
        senderId: String,
        receiverId: String
    ): Result<Photo> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            // Compress image before upload (FASE 5 improvement)
            val compressResult = ImageCompressor.compressImage(imageUri, context)
            if (compressResult.isFailure) {
                val error = compressResult.exceptionOrNull()
                Log.e("PhotoRepository", "Failed to compress image: ${error?.message}")
                return Result.failure(error ?: Exception("Failed to compress image"))
            }

            val imageBytes = compressResult.getOrThrow()
            val sizeKB = imageBytes.size / 1024

            Log.d("PhotoRepository", "Uploading photo from $senderId to $receiverId (${sizeKB}KB compressed)")

            // Create multipart body
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", requestBody)

            // Upload to API
            val response = apiService.uploadPhoto("Bearer $token", imagePart, receiverId)

            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                val photo = Photo(
                    id = uploadResponse.id,
                    sender_id = uploadResponse.senderId,
                    receiver_id = uploadResponse.receiverId,
                    timestamp = uploadResponse.timestamp,
                    seen = uploadResponse.seen
                )
                Log.d("PhotoRepository", "Photo uploaded successfully: ${photo.id}")
                Result.success(photo)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("PhotoRepository", "Failed to upload photo: $errorMsg")
                Result.failure(Exception("Falha ao enviar foto: $errorMsg"))
            }
        } catch (e: OutOfMemoryError) {
            Log.e("PhotoRepository", "OutOfMemoryError uploading photo", e)
            Result.failure(Exception("Memória insuficiente. Tente uma foto menor ou feche outros apps."))
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error uploading photo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get latest photo for user
     */
    suspend fun getLatestPhotoForUser(userId: String): Photo? {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                Log.e("PhotoRepository", "No authentication token found")
                return null
            }

            val response = apiService.getLatestPhoto("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val apiPhoto = response.body()!!
                val photo = Photo(
                    id = apiPhoto.id,
                    sender_id = apiPhoto.senderId,
                    receiver_id = apiPhoto.receiverId,
                    timestamp = apiPhoto.timestamp,
                    seen = apiPhoto.seen
                )
                Log.d("PhotoRepository", "Latest photo retrieved: ${photo.id}")
                photo
            } else {
                Log.d("PhotoRepository", "No latest photo found")
                null
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error getting latest photo: ${e.message}", e)
            null
        }
    }

    /**
     * Get all photos for user
     */
    suspend fun getPhotosForUser(userId: String): List<Photo> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                Log.e("PhotoRepository", "No authentication token found")
                return emptyList()
            }

            val response = apiService.getPhotos("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val apiPhotos = response.body()!!
                val photos = apiPhotos.map { apiPhoto ->
                    Photo(
                        id = apiPhoto.id,
                        sender_id = apiPhoto.senderId,
                        receiver_id = apiPhoto.receiverId,
                        timestamp = apiPhoto.timestamp,
                        seen = apiPhoto.seen
                    )
                }
                Log.d("PhotoRepository", "Retrieved ${photos.size} photos")
                photos
            } else {
                Log.d("PhotoRepository", "No photos found")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error getting photos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get photo image bytes
     * Returns ByteArray that can be converted to Bitmap
     */
    suspend fun getPhotoImage(photoId: String, userId: String): ByteArray? {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                Log.e("PhotoRepository", "No authentication token found")
                return null
            }

            val response = apiService.getPhotoImage("Bearer $token", photoId)

            if (response.isSuccessful && response.body() != null) {
                val imageBytes = response.body()!!.bytes()
                Log.d("PhotoRepository", "Photo image retrieved: $photoId (${imageBytes.size} bytes)")
                imageBytes
            } else {
                Log.e("PhotoRepository", "Failed to get photo image: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error getting photo image: ${e.message}", e)
            null
        }
    }

    /**
     * Get photo image as Bitmap
     */
    suspend fun getPhotoImageBitmap(photoId: String, userId: String): Bitmap? {
        val imageBytes = getPhotoImage(photoId, userId) ?: return null
        return try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error decoding bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Download and cache photo image locally
     * Returns URI to cached file
     */
    suspend fun downloadAndCachePhoto(photoId: String, userId: String): Uri? {
        return try {
            val imageBytes = getPhotoImage(photoId, userId) ?: return null

            // Create cache directory
            val cacheDir = File(context.cacheDir, "photos")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Save to cache
            val imageFile = File(cacheDir, "$photoId.jpg")
            FileOutputStream(imageFile).use { output ->
                output.write(imageBytes)
            }

            Log.d("PhotoRepository", "Photo cached: ${imageFile.absolutePath}")
            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error caching photo: ${e.message}", e)
            null
        }
    }

    /**
     * Mark photo as seen (not implemented in API yet)
     * Placeholder for future implementation
     */
    suspend fun markPhotoAsSeen(photoId: String): Result<Unit> {
        // TODO: Implement when API endpoint is available
        Log.d("PhotoRepository", "markPhotoAsSeen not implemented in API yet")
        return Result.success(Unit)
    }

    /**
     * Clean old cached photos (older than 30 days)
     */
    fun cleanOldCache() {
        try {
            val cacheDir = File(context.cacheDir, "photos")
            if (!cacheDir.exists()) return

            val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L

            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < thirtyDaysAgo) {
                    file.delete()
                    Log.d("PhotoRepository", "Deleted old cached photo: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error cleaning cache: ${e.message}", e)
        }
    }
}
