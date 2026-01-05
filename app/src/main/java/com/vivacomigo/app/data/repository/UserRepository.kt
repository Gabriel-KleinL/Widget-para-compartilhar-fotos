package com.vivacomigo.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.vivacomigo.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            if (doc.exists()) {
                val user = User.fromMap(doc.data ?: emptyMap())
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.data?.let { User.fromMap(it) }
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUserByPairingCode(code: String): Result<User> {
        return try {
            val query = usersCollection.whereEqualTo("pairingCode", code).get().await()
            if (query.documents.isNotEmpty()) {
                val user = User.fromMap(query.documents[0].data ?: emptyMap())
                Result.success(user)
            } else {
                Result.failure(Exception("User not found with code: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pairUsers(userId: String, partnerId: String): Result<Unit> {
        return try {
            // Update current user
            usersCollection.document(userId).update("partnerId", partnerId).await()
            // Update partner
            usersCollection.document(partnerId).update("partnerId", userId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
