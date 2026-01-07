package com.vivacomigo.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.vivacomigo.app.MainActivity
import com.vivacomigo.app.R
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.ImageHelper
import com.vivacomigo.app.data.repository.PhotoRepository
import kotlinx.coroutines.runBlocking

class PhotoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            PhotoWidgetContent(context)
        }
    }

    @Composable
    private fun PhotoWidgetContent(context: Context) {
        // Buscar foto recebida mais recente
        val (photoId, imageBitmap) = runBlocking {
            try {
                android.util.Log.d("PhotoWidget", "Iniciando carregamento de foto no widget...")
                val authRepository = AuthRepository(context)
                val userResult = authRepository.ensureLocalUser()
                val userId = userResult.getOrNull()?.id
                
                android.util.Log.d("PhotoWidget", "User ID: $userId")
                
                if (userId != null) {
                    val photoRepository = PhotoRepository(context)
                    val photo = photoRepository.getLatestPhotoForUser(userId)
                    
                    android.util.Log.d("PhotoWidget", "Foto encontrada: ${photo?.id}")
                    
                    if (photo != null) {
                        val imageBytes = photoRepository.getPhotoImage(photo.id, userId)
                        android.util.Log.d("PhotoWidget", "Bytes da imagem: ${imageBytes?.size ?: 0}")
                        
                        val bitmap = imageBytes?.let { 
                            val bmp = ImageHelper.bytesToBitmap(it)
                            android.util.Log.d("PhotoWidget", "Bitmap criado: ${bmp != null}, tamanho original: ${bmp?.width}x${bmp?.height}")
                            
                            // Redimensionar para widget (máximo 512x512 para evitar erro de memória)
                            val resized = bmp?.let { ImageHelper.resizeBitmapForWidget(it, maxSize = 512) }
                            android.util.Log.d("PhotoWidget", "Bitmap redimensionado: ${resized != null}, tamanho: ${resized?.width}x${resized?.height}")
                            resized
                        }
                        Pair(photo.id, bitmap)
                    } else {
                        android.util.Log.d("PhotoWidget", "Nenhuma foto encontrada")
                        Pair(null, null)
                    }
                } else {
                    android.util.Log.w("PhotoWidget", "User ID é null")
                    Pair(null, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoWidget", "Erro ao carregar foto: ${e.message}", e)
                e.printStackTrace()
                Pair(null, null)
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                imageBitmap != null -> {
                    // Exibir imagem da foto recebida
                    Image(
                        provider = ImageProvider(imageBitmap),
                        contentDescription = "Foto recebida",
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
                photoId != null -> {
                    // Há foto mas não conseguiu carregar a imagem
                    Text(
                        text = "❤️",
                        style = TextStyle(
                            fontSize = 48.sp,
                            color = ColorProvider(R.color.primary)
                        )
                    )
                }
                else -> {
                    // Estado vazio
                    Text(
                        text = context.getString(R.string.tap_to_add),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(R.color.primary)
                        ),
                        modifier = GlanceModifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
