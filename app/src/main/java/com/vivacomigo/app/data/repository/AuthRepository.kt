package com.vivacomigo.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vivacomigo.app.data.database.DatabaseHelper
import com.vivacomigo.app.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.sql.ResultSet
import kotlin.random.Random

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
private val USER_ID_KEY = stringPreferencesKey("user_id")

class AuthRepository(private val context: Context) {
    
    private fun generatePairingCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    suspend fun getCurrentUserId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }
    
    private suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }
    
    /**
     * Garante que existe um usuário local criado no banco de dados.
     * Se não existir, cria automaticamente com ID único e código de pareamento.
     */
    suspend fun ensureLocalUser(): Result<User> {
        return try {
            // Verificar se já existe um usuário local salvo
            val existingUserId = getCurrentUserId()
            
            if (existingUserId != null) {
                // Tentar buscar o usuário no banco
                val userResult = DatabaseHelper.executeQuery(
                    "SELECT id, pairing_code, partner_id, display_name FROM users WHERE id = ?",
                    listOf(existingUserId)
                ) { rs ->
                    val partnerIdValue = rs.getString("partner_id")
                    val isPartnerIdNull = rs.wasNull()
                    User(
                        id = rs.getString("id") ?: "",
                        email = "", // Não usado mais
                        pairing_code = rs.getString("pairing_code") ?: "",
                        partner_id = if (isPartnerIdNull) null else partnerIdValue,
                        display_name = rs.getString("display_name") ?: "Usuário"
                    )
                }
                
                if (userResult.isSuccess) {
                    return userResult
                } else {
                    // Usuário não encontrado no banco, criar novo
                    Log.w("AuthRepository", "Usuário local não encontrado no banco, criando novo")
                }
            }
            
            // Criar novo usuário local
            var pairingCode: String
            var isUnique = false
            var attempts = 0
            val maxAttempts = 20
            
            while (!isUnique && attempts < maxAttempts) {
                pairingCode = generatePairingCode()
                val checkCodeResult = DatabaseHelper.executeQuery(
                    "SELECT id FROM users WHERE pairing_code = ?",
                    listOf(pairingCode)
                ) { rs -> rs.getString("id") }
                
                if (checkCodeResult.isFailure) {
                    isUnique = true
                    
                    // Criar usuário no banco
                    val userId = DatabaseHelper.generateUUID()
                    val displayName = "Usuário"
                    
                    val insertResult = DatabaseHelper.executeUpdate(
                        "INSERT INTO users (id, pairing_code, display_name) VALUES (?, ?, ?)",
                        listOf(userId, pairingCode, displayName)
                    )
                    
                    return insertResult.fold(
                        onSuccess = {
                            saveUserId(userId)
                            val user = User(
                                id = userId,
                                email = "",
                                pairing_code = pairingCode,
                                display_name = displayName
                            )
                            Log.d("AuthRepository", "Usuário local criado: $userId com código: $pairingCode")
                            Result.success(user)
                        },
                        onFailure = { error ->
                            Log.e("AuthRepository", "Erro ao criar usuário local: ${error.message}", error)
                            Result.failure(error)
                        }
                    )
                }
                attempts++
            }
            
            Result.failure(Exception("Erro ao gerar código de pareamento único após $maxAttempts tentativas"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro ao garantir usuário local: ${e.message}", e)
            Result.failure(e)
        }
    }
}
