package com.vivacomigo.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.PhotoRepository
import com.vivacomigo.app.data.repository.UserRepository

class PhotoWidgetWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val authRepository = AuthRepository(context)
        
        // Garantir que existe um usuário local
        val userResult = authRepository.ensureLocalUser()
        val userId = userResult.getOrNull()?.id ?: return Result.failure()

        val photoRepository = PhotoRepository(context)

        return try {
            // Get latest photo
            val photo = photoRepository.getLatestPhotoForUser(userId)

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
            Result.retry()
        }
    }
}
