package com.vivacomigo.app.data.repository

import android.content.Context
import com.vivacomigo.app.data.database.DatabaseHelper
import com.vivacomigo.app.data.model.User
import java.sql.ResultSet

class UserRepository(private val context: Context) {
    
    private fun mapUserFromResultSet(rs: ResultSet): User {
        val partnerIdValue = rs.getString("partner_id")
        val isPartnerIdNull = rs.wasNull()
        return User(
            id = rs.getString("id") ?: "",
            email = "", // Não usado mais, mantido para compatibilidade
            pairing_code = rs.getString("pairing_code") ?: "",
            partner_id = if (isPartnerIdNull) null else partnerIdValue,
            display_name = rs.getString("display_name") ?: "Usuário"
        )
    }
    
    suspend fun getUser(userId: String): Result<User> {
        return DatabaseHelper.executeQuery(
            "SELECT id, pairing_code, partner_id, display_name FROM users WHERE id = ?",
            listOf(userId),
            ::mapUserFromResultSet
        )
    }
    
    suspend fun getCurrentUser(): Result<User> {
        val authRepo = AuthRepository(context)
        // Garantir que existe um usuário local
        return authRepo.ensureLocalUser()
    }
    
    suspend fun findUserByPairingCode(code: String): Result<User> {
        val cleanCode = code.trim().uppercase()
        return DatabaseHelper.executeQuery(
            "SELECT id, pairing_code, partner_id, display_name FROM users WHERE UPPER(TRIM(pairing_code)) = ?",
            listOf(cleanCode),
            ::mapUserFromResultSet
        )
    }
    
    suspend fun pairUsers(userId: String, partnerCode: String): Result<User> {
        return try {
            android.util.Log.d("UserRepository", "pairUsers chamado - userId: $userId, partnerCode: $partnerCode")
            val cleanCode = partnerCode.trim().uppercase()
            
            if (cleanCode.isEmpty()) {
                android.util.Log.w("UserRepository", "Código vazio")
                return Result.failure(Exception("Código de pareamento não pode estar vazio"))
            }
            
            android.util.Log.d("UserRepository", "Buscando parceiro com código: $cleanCode")
            // Buscar parceiro pelo código
            val partnerResult = findUserByPairingCode(cleanCode)
            
            return partnerResult.fold(
                onSuccess = { partner ->
                    android.util.Log.d("UserRepository", "Parceiro encontrado: ${partner.id}, pairing_code: ${partner.pairingCode}")
                    
                    if (partner.id == userId) {
                        android.util.Log.w("UserRepository", "Tentativa de parear consigo mesmo")
                        return@fold Result.failure(Exception("Você não pode parear consigo mesmo!"))
                    }
                    
                    // Verificar se já está pareado
                    if (partner.partnerId != null && partner.partnerId != userId) {
                        android.util.Log.w("UserRepository", "Parceiro já está pareado com outro usuário")
                        return@fold Result.failure(Exception("Este usuário já está pareado com outro parceiro"))
                    }
                    
                    android.util.Log.d("UserRepository", "Iniciando transação de pareamento...")
                    // Parear usuários em transação
                    val transactionResult = DatabaseHelper.executeTransaction { connection ->
                        android.util.Log.d("UserRepository", "Atualizando usuário atual ($userId) com partner_id = ${partner.id}")
                        val stmt1 = connection.prepareStatement("UPDATE users SET partner_id = ? WHERE id = ?")
                        stmt1.setString(1, partner.id)
                        stmt1.setString(2, userId)
                        val rows1 = stmt1.executeUpdate()
                        android.util.Log.d("UserRepository", "UPDATE 1 executado: $rows1 linha(s) atualizada(s)")
                        stmt1.close()
                        
                        if (rows1 == 0) {
                            android.util.Log.e("UserRepository", "Nenhuma linha atualizada para usuário atual")
                            throw Exception("Erro ao atualizar usuário atual")
                        }
                        
                        android.util.Log.d("UserRepository", "Atualizando parceiro (${partner.id}) com partner_id = $userId")
                        val stmt2 = connection.prepareStatement("UPDATE users SET partner_id = ? WHERE id = ?")
                        stmt2.setString(1, userId)
                        stmt2.setString(2, partner.id)
                        val rows2 = stmt2.executeUpdate()
                        android.util.Log.d("UserRepository", "UPDATE 2 executado: $rows2 linha(s) atualizada(s)")
                        stmt2.close()
                        
                        if (rows2 == 0) {
                            android.util.Log.e("UserRepository", "Nenhuma linha atualizada para parceiro")
                            throw Exception("Erro ao atualizar parceiro")
                        }
                        
                        android.util.Log.d("UserRepository", "Ambos os UPDATEs executados com sucesso. Aguardando commit...")
                    }
                    
                    transactionResult.fold(
                        onSuccess = {
                            android.util.Log.d("UserRepository", "Buscando usuário atualizado...")
                            // Retornar usuário atualizado
                            getUser(userId)
                        },
                        onFailure = { error ->
                            android.util.Log.e("UserRepository", "Erro na transação: ${error.message}", error)
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("UserRepository", "Parceiro não encontrado: ${error.message}", error)
                    Result.failure(Exception("Código de pareamento não encontrado. Verifique se o código está correto."))
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Erro ao parear usuários: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun unpairUsers(userId: String): Result<User> {
        return try {
            // Buscar parceiro atual antes da transação
            val currentUserResult = getUser(userId)
            val currentUser = currentUserResult.getOrNull()
            val partnerId = currentUser?.partnerId
            
            if (partnerId == null) {
                // Já não está pareado
                return Result.success(currentUser ?: return Result.failure(Exception("Usuário não encontrado")))
            }
            
            // Desparear usuários em transação
            val transactionResult = DatabaseHelper.executeTransaction { connection ->
                // Remover pareamento do usuário atual
                val stmt1 = connection.prepareStatement("UPDATE users SET partner_id = NULL WHERE id = ?")
                stmt1.setString(1, userId)
                stmt1.executeUpdate()
                stmt1.close()
                
                // Remover pareamento do parceiro
                val stmt2 = connection.prepareStatement("UPDATE users SET partner_id = NULL WHERE id = ?")
                stmt2.setString(1, partnerId)
                stmt2.executeUpdate()
                stmt2.close()
            }
            
            return transactionResult.fold(
                onSuccess = {
                    // Retornar usuário atualizado
                    getUser(userId)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Erro ao desparar usuários: ${e.message}", e)
            Result.failure(e)
        }
    }
}
