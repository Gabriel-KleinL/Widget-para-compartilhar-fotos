package com.vivacomigo.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vivacomigo.app.data.api.*
import com.vivacomigo.app.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authApiDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_api_prefs")
private val USER_ID_KEY = stringPreferencesKey("user_id")
private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")

class AuthRepository(private val context: Context) {

    private val apiService = RetrofitClient.apiService

    /**
     * Get the stored auth token
     */
    suspend fun getAuthToken(): String? {
        return context.authApiDataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }.first()
    }

    /**
     * Get the current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return context.authApiDataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }

    /**
     * Get the stored display name
     */
    suspend fun getDisplayName(): String? {
        return context.authApiDataStore.data.map { preferences ->
            preferences[DISPLAY_NAME_KEY]
        }.first()
    }

    /**
     * Save authentication data to DataStore
     */
    private suspend fun saveAuthData(userId: String, token: String, displayName: String) {
        context.authApiDataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[AUTH_TOKEN_KEY] = token
            preferences[DISPLAY_NAME_KEY] = displayName
        }
    }

    /**
     * Clear all authentication data
     */
    suspend fun clearAuthData() {
        context.authApiDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Register a new user with simple display name (no password)
     */
    suspend fun registerSimple(displayName: String): Result<User> {
        return try {
            Log.d("AuthRepository", "Registering user: $displayName")

            val request = RegisterRequest(displayName)
            val response = apiService.registerSimple(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val apiUser = authResponse.user

                // Save auth data locally
                saveAuthData(apiUser.id, authResponse.token, apiUser.displayName)

                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )

                Log.d("AuthRepository", "User registered successfully: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AuthRepository", "Registration failed: $errorMsg")
                Result.failure(Exception("Registration failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Login with display name (no password)
     */
    suspend fun loginSimple(displayName: String): Result<User> {
        return try {
            Log.d("AuthRepository", "Logging in user: $displayName")

            val request = LoginRequest(displayName)
            val response = apiService.loginSimple(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val apiUser = authResponse.user

                // Save auth data locally
                saveAuthData(apiUser.id, authResponse.token, apiUser.displayName)

                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )

                Log.d("AuthRepository", "User logged in successfully: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AuthRepository", "Login failed: $errorMsg")
                Result.failure(Exception("Login failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Login with pairing code
     */
    suspend fun loginWithCode(pairingCode: String): Result<User> {
        return try {
            Log.d("AuthRepository", "Logging in with code: $pairingCode")

            val request = LoginCodeRequest(pairingCode)
            val response = apiService.loginWithCode(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val apiUser = authResponse.user

                // Save auth data locally
                saveAuthData(apiUser.id, authResponse.token, apiUser.displayName)

                val user = User(
                    id = apiUser.id,
                    email = apiUser.email ?: "",
                    pairing_code = apiUser.pairingCode,
                    partner_id = apiUser.partnerId,
                    display_name = apiUser.displayName
                )

                Log.d("AuthRepository", "User logged in with code successfully: ${user.id}")
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AuthRepository", "Login with code failed: $errorMsg")
                Result.failure(Exception("Login with code failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login with code error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure local user exists (backward compatibility)
     * This will try to get current user from API, or prompt registration
     */
    suspend fun ensureLocalUser(): Result<User> {
        return try {
            val token = getAuthToken()

            if (token != null) {
                // Try to get current user from API
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
                    return Result.success(user)
                }
            }

            // No token or invalid token - need to register/login
            Result.failure(Exception("No authenticated user. Please register or login."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error ensuring local user: ${e.message}", e)
            Result.failure(e)
        }
    }
}
