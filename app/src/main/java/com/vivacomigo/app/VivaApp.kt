package com.vivacomigo.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.vivacomigo.app.widget.PhotoWidgetWorker
import java.util.concurrent.TimeUnit

class VivaApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Schedule widget updates
        scheduleWidgetUpdates()
    }

    private fun scheduleWidgetUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<PhotoWidgetWorker>(
            30, TimeUnit.MINUTES
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
