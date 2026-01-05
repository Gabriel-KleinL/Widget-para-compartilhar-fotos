package com.vivacomigo.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.vivacomigo.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.first

class PhotoWidgetWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
        val photoRepository = PhotoRepository()

        return try {
            // Get latest photo
            val photo = photoRepository.getLatestPhotoForUser(userId).first()

            // Save to DataStore
            photo?.imageUrl?.let { url ->
                PhotoWidgetDataStore.savePhotoUrl(context, url)
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
