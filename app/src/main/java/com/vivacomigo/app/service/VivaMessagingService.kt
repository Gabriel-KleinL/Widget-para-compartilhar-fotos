package com.vivacomigo.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vivacomigo.app.MainActivity
import com.vivacomigo.app.R
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.UserRepository
import com.vivacomigo.app.widget.PhotoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VivaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Push recebida: ${message.data}")
        
        val type = message.data["type"]
        
        when (type) {
            "new_photo" -> handleNewPhoto(message)
            else -> Log.w(TAG, "Tipo de push desconhecido: $type")
        }
    }

    private fun handleNewPhoto(message: RemoteMessage) {
        val photoId = message.data["photo_id"]
        val senderId = message.data["sender_id"]
        
        Log.d(TAG, "Nova foto recebida - ID: $photoId, Sender: $senderId")
        
        // Atualizar widget
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PhotoWidget().updateAll(applicationContext)
                Log.d(TAG, "Widget atualizado via push")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar widget", e)
            }
        }
        
        // Mostrar notificação
        showNotification(
            title = message.notification?.title ?: "Nova foto",
            body = message.notification?.body ?: "Você recebeu uma foto"
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo FCM token: $token")
        
        // Enviar token para backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendTokenToBackend(token)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar token", e)
            }
        }
    }

    private suspend fun sendTokenToBackend(token: String) {
        val authRepo = AuthRepository(applicationContext)
        val authToken = authRepo.getAuthToken() ?: return
        
        val userRepo = UserRepository(applicationContext)
        userRepo.updateFcmToken(token).fold(
            onSuccess = {
                Log.d(TAG, "FCM token enviado ao backend com sucesso")
            },
            onFailure = { error ->
                Log.e(TAG, "Erro ao enviar token ao backend", error)
            }
        )
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "viva_comigo_photos"
        
        createNotificationChannel(channelId)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fotos do Parceiro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de novas fotos recebidas"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "VivaMessagingService"
        private const val NOTIFICATION_ID = 1001
    }
}
