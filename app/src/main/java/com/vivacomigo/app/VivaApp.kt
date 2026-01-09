package com.vivacomigo.app

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vivacomigo.app.util.CacheCleaner
import com.vivacomigo.app.widget.PhotoWidgetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class VivaApp : Application() {

    companion object {
        private const val TAG = "VivaApp"
    }

    // Application-scoped coroutine
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Crashlytics (FASE 6)
        initializeCrashlytics()

        // Schedule widget updates
        scheduleWidgetUpdates()

        // Clean old cache in background (FASE 5 improvement)
        cleanCacheInBackground()
    }

    /**
     * Initialize Firebase Crashlytics for error tracking
     * FASE 6: Quality & Maintainability
     */
    private fun initializeCrashlytics() {
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            Log.i(TAG, "📊 Firebase Crashlytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Crashlytics", e)
        }
    }

    /**
     * Clean old cached files in background
     * Runs once at app startup
     */
    private fun cleanCacheInBackground() {
        applicationScope.launch {
            try {
                val deletedCount = CacheCleaner.cleanOldCache(applicationContext)
                if (deletedCount > 0) {
                    Log.i(TAG, "🧹 Cleaned $deletedCount old cache files")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning cache: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun scheduleWidgetUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Sincronização a cada 2 horas como fallback (FCM é principal)
        val workRequest = PeriodicWorkRequestBuilder<PhotoWidgetWorker>(
            2, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
