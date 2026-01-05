package com.vivacomigo.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vivacomigo.app.data.model.Photo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PhotoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val photosCollection = firestore.collection("photos")

    suspend fun uploadPhoto(
        imageUri: Uri,
        senderId: String,
        receiverId: String
    ): Result<Photo> {
        return try {
            // Upload image to Firebase Storage
            val filename = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("photos/$filename")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Create photo document
            val photoId = photosCollection.document().id
            val photo = Photo(
                id = photoId,
                senderId = senderId,
                receiverId = receiverId,
                imageUrl = downloadUrl,
                timestamp = System.currentTimeMillis()
            )

            photosCollection.document(photoId).set(photo.toMap()).await()
            Result.success(photo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLatestPhotoForUser(userId: String): Flow<Photo?> = callbackFlow {
        val listener = photosCollection
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val photo = snapshot?.documents?.firstOrNull()?.data?.let {
                    Photo.fromMap(it)
                }
                trySend(photo)
            }

        awaitClose { listener.remove() }
    }

    fun getPhotosForUser(userId: String): Flow<List<Photo>> = callbackFlow {
        val listener = photosCollection
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val photos = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Photo.fromMap(it) }
                } ?: emptyList()

                trySend(photos)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markPhotoAsSeen(photoId: String): Result<Unit> {
        return try {
            photosCollection.document(photoId).update("seen", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
