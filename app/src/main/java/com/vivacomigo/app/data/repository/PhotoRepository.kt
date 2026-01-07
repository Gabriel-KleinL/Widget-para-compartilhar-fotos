package com.vivacomigo.app.data.repository

import android.content.Context
import android.net.Uri
import com.vivacomigo.app.data.database.DatabaseHelper
import com.vivacomigo.app.data.model.Photo
import java.io.InputStream
import java.sql.ResultSet

class PhotoRepository(private val context: Context) {
    
    private fun mapPhotoFromResultSet(rs: ResultSet): Photo {
        return Photo(
            id = rs.getString("id") ?: "",
            sender_id = rs.getString("sender_id") ?: "",
            receiver_id = rs.getString("receiver_id") ?: "",
            timestamp = rs.getLong("timestamp"),
            seen = rs.getBoolean("seen")
        )
    }
    
    suspend fun uploadPhoto(
        imageUri: Uri,
        senderId: String,
        receiverId: String
    ): Result<Photo> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            
            if (inputStream == null) {
                return Result.failure(Exception("Não foi possível abrir a imagem"))
            }
            
            val imageBytes = inputStream.readBytes()
            inputStream.close()
            
            // Verificar se o destinatário é o parceiro
            val userRepo = UserRepository(context)
            val currentUserResult = userRepo.getUser(senderId)
            
            return currentUserResult.fold(
                onSuccess = { user ->
                    if (user.partnerId != receiverId) {
                        return@fold Result.failure(Exception("Você só pode enviar fotos para seu parceiro"))
                    }
                    
                    // Criar foto
                    val photoId = DatabaseHelper.generateUUID()
                    val timestamp = System.currentTimeMillis()
                    
                    val insertResult = DatabaseHelper.executeUpdate(
                        "INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen) VALUES (?, ?, ?, ?, ?, ?)",
                        listOf(photoId, senderId, receiverId, imageBytes, timestamp, false)
                    )
                    
                    insertResult.fold(
                        onSuccess = {
                            val photo = Photo(
                                id = photoId,
                                sender_id = senderId,
                                receiver_id = receiverId,
                                timestamp = timestamp,
                                seen = false
                            )
                            Result.success(photo)
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLatestPhotoForUser(userId: String): Photo? {
        return DatabaseHelper.executeQuery(
            "SELECT id, sender_id, receiver_id, timestamp, seen FROM photos WHERE receiver_id = ? ORDER BY timestamp DESC LIMIT 1",
            listOf(userId),
            ::mapPhotoFromResultSet
        ).getOrNull()
    }
    
    suspend fun getPhotosForUser(userId: String): List<Photo> {
        return DatabaseHelper.executeQueryList(
            "SELECT id, sender_id, receiver_id, timestamp, seen FROM photos WHERE receiver_id = ? ORDER BY timestamp DESC",
            listOf(userId),
            ::mapPhotoFromResultSet
        ).getOrElse { emptyList() }
    }
    
    suspend fun getPhotoImage(photoId: String, userId: String): ByteArray? {
        return try {
            // Verificar se o usuário tem permissão (é o remetente ou destinatário)
            val photoResult = DatabaseHelper.executeQuery(
                "SELECT sender_id, receiver_id, image_data FROM photos WHERE id = ?",
                listOf(photoId)
            ) { rs ->
                Triple(
                    rs.getString("sender_id") ?: "",
                    rs.getString("receiver_id") ?: "",
                    rs.getBytes("image_data") ?: ByteArray(0)
                )
            }
            
            photoResult.fold(
                onSuccess = { (senderId, receiverId, imageData) ->
                    if (senderId == userId || receiverId == userId) {
                        imageData
                    } else {
                        null
                    }
                },
                onFailure = { null }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun markPhotoAsSeen(photoId: String): Result<Unit> {
        return DatabaseHelper.executeUpdate(
            "UPDATE photos SET seen = TRUE WHERE id = ?",
            listOf(photoId)
        ).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error -> Result.failure(error) }
        )
    }
}
