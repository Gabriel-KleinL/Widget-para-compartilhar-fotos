package com.vivacomigo.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {
    /**
     * Converte bytes de imagem em URI local para exibição
     */
    fun saveImageToCache(context: Context, imageBytes: ByteArray, photoId: String): Uri? {
        return try {
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "photo_$photoId.jpg")
            
            FileOutputStream(imageFile).use { fos ->
                fos.write(imageBytes)
            }
            
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Converte bytes em Bitmap, aplicando rotação EXIF se necessário
     */
    fun bytesToBitmap(imageBytes: ByteArray): Bitmap? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            bitmap?.let { applyExifRotation(it, imageBytes) }
        } catch (e: Exception) {
            android.util.Log.e("ImageHelper", "Erro ao converter bytes em bitmap: ${e.message}", e)
            null
        }
    }
    
    /**
     * Aplica rotação EXIF ao bitmap se necessário
     */
    private fun applyExifRotation(bitmap: Bitmap, imageBytes: ByteArray): Bitmap {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(imageBytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap // Sem rotação necessária
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageHelper", "Erro ao aplicar rotação EXIF: ${e.message}", e)
            bitmap // Retorna o bitmap original em caso de erro
        }
    }
    
    /**
     * Redimensiona um bitmap para um tamanho máximo adequado para widgets
     * Mantém a proporção da imagem original
     */
    fun resizeBitmapForWidget(bitmap: Bitmap, maxSize: Int = 512): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Se já está dentro do tamanho máximo, retorna o bitmap original
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        // Calcula o novo tamanho mantendo a proporção
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        
        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageHelper", "Erro ao redimensionar bitmap: ${e.message}", e)
            bitmap // Retorna o original em caso de erro
        }
    }
}

