package com.vivacomigo.app.data.repository

import android.content.Context
import android.util.Log
import com.vivacomigo.app.data.api.FcmTokenRequest
import com.vivacomigo.app.data.api.PairRequest
import com.vivacomigo.app.data.api.RetrofitClient
import com.vivacomigo.app.data.model.User

class UserRepository(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val authRepo = AuthRepository(context)

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getUserById("Bearer $token", userId)

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to get user: $errorMsg")
                Result.failure(Exception("Failed to get user: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val response = apiService.getCurrentUser("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )
                Log.d("UserRepository", "Current user retrieved: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to get current user: $errorMsg")
                Result.failure(Exception("Failed to get current user: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Find user by pairing code
     */
    suspend fun findUserByPairingCode(code: String): Result<User> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val cleanCode = code.trim().uppercase()
            if (cleanCode.isEmpty()) {
                return Result.failure(Exception("Código de pareamento não pode estar vazio"))
            }

            val response = apiService.getUserByPairingCode("Bearer $token", cleanCode)

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )
                Log.d("UserRepository", "User found with code $cleanCode: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to find user by code: $errorMsg")
                Result.failure(Exception("Código de pareamento não encontrado. Verifique se o código está correto."))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error finding user by code: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pair current user with another user by partner code
     */
    suspend fun pairUsers(userId: String, partnerCode: String): Result<User> {
        return try {
            Log.d("UserRepository", "Pairing users - userId: $userId, partnerCode: $partnerCode")

            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val cleanCode = partnerCode.trim().uppercase()
            if (cleanCode.isEmpty()) {
                Log.w("UserRepository", "Empty code")
                return Result.failure(Exception("Código de pareamento não pode estar vazio"))
            }

            // Verify partner exists first
            val partnerResult = findUserByPairingCode(cleanCode)
            if (partnerResult.isFailure) {
                return partnerResult
            }

            val partner = partnerResult.getOrNull()!!

            // Check if trying to pair with self
            if (partner.id == userId) {
                Log.w("UserRepository", "Attempting to pair with self")
                return Result.failure(Exception("Você não pode parear consigo mesmo!"))
            }

            // Check if partner is already paired with someone else
            if (partner.partnerId != null && partner.partnerId != userId) {
                Log.w("UserRepository", "Partner already paired with someone else")
                return Result.failure(Exception("Este usuário já está pareado com outro parceiro"))
            }

            // Send pair request to API
            val pairRequest = PairRequest(cleanCode)
            val response = apiService.pairWithPartner("Bearer $token", pairRequest)

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )
                Log.d("UserRepository", "Pairing successful: ${user.id} paired with ${user.partner_id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Pairing failed: $errorMsg")
                Result.failure(Exception("Falha ao parear: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error pairing users: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unpair current user from their partner
     */
    suspend fun unpairUsers(userId: String): Result<User> {
        return try {
            Log.d("UserRepository", "Unpairing user: $userId")

            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            // Check if user is paired
            val currentUserResult = getCurrentUser()
            if (currentUserResult.isFailure) {
                return currentUserResult
            }

            val currentUser = currentUserResult.getOrNull()!!
            if (currentUser.partner_id == null) {
                // Already unpaired
                Log.d("UserRepository", "User already not paired")
                return Result.success(currentUser)
            }

            // Send unpair request to API
            val response = apiService.unpairPartner("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )
                Log.d("UserRepository", "Unpairing successful: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Unpairing failed: $errorMsg")
                Result.failure(Exception("Falha ao desparear: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unpairing users: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update FCM token for push notifications
     */
    suspend fun updateFcmToken(fcmToken: String): Result<Unit> {
        return try {
            val token = authRepo.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token found"))
            }

            val request = FcmTokenRequest(fcmToken)
            val response = apiService.updateFcmToken("Bearer $token", request)

            if (response.isSuccessful) {
                Log.d("UserRepository", "FCM token updated successfully")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to update FCM token: $errorMsg")
                Result.failure(Exception("Failed to update FCM token"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating FCM token: ${e.message}", e)
            Result.failure(e)
        }
    }
}
