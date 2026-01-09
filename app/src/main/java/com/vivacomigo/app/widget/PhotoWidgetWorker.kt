package com.vivacomigo.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.PhotoRepository

class PhotoWidgetWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = getUserIdFromApi()

            if (userId == null) {
                return Result.failure()
            }

            // Get latest photo
            val photoRepositoryApi = PhotoRepository(context)
            val photo = photoRepositoryApi.getLatestPhotoForUser(userId)

            // Save photo ID to DataStore
            photo?.id?.let { photoId ->
                PhotoWidgetDataStore.savePhotoId(context, photoId)
            }

            // Update widget
            val glanceId = GlanceAppWidgetManager(context)
                .getGlanceIds(PhotoWidget::class.java)
                .firstOrNull()

            glanceId?.let {
                PhotoWidget().update(context, it)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PhotoWidgetWorker", "Error: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun getUserIdFromApi(): String? {
        val authRepositoryApi = AuthRepository(context)
        return authRepositoryApi.getCurrentUserId()
    }
}
