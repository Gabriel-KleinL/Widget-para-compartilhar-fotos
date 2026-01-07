package com.vivacomigo.app.data.database

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

object DatabaseHelper {
    
    // Pool simples de conexões
    private val connectionPool = ConcurrentLinkedQueue<Connection>()
    private val poolMutex = Mutex()
    private var poolInitialized = false
    
    init {
        try {
            // Carregar o driver MySQL explicitamente para compatibilidade
            Class.forName("com.mysql.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            try {
                // Tentar o nome alternativo para versões mais novas
                Class.forName("com.mysql.cj.jdbc.Driver")
            } catch (e2: ClassNotFoundException) {
                Log.w("DatabaseHelper", "Driver MySQL não encontrado, tentando usar DriverManager automático")
            }
        }
    }
    
    private suspend fun initializePool() = withContext(Dispatchers.IO) {
        if (poolInitialized) return@withContext
        
        poolMutex.withLock {
            if (poolInitialized) return@withLock
            
            try {
                // Criar algumas conexões iniciais
                for (i in 0 until 2) {
                    val conn = createNewConnection()
                    if (conn != null) {
                        connectionPool.offer(conn)
                    }
                }
                poolInitialized = true
                Log.d("DatabaseHelper", "Pool de conexões inicializado com ${connectionPool.size} conexões")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Erro ao inicializar pool: ${e.message}", e)
            }
        }
    }
    
    private fun createNewConnection(): Connection? {
        return try {
            val url = "jdbc:mysql://${DatabaseConfig.HOST}:${DatabaseConfig.PORT}/${DatabaseConfig.DATABASE}" +
                    "?useSSL=${DatabaseConfig.USE_SSL}" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8" +
                    "&connectTimeout=${DatabaseConfig.CONNECTION_TIMEOUT_MS}" +
                    "&socketTimeout=${DatabaseConfig.CONNECTION_TIMEOUT_MS}" +
                    "&autoReconnect=true" +
                    "&failOverReadOnly=false"
            
            val connection = DriverManager.getConnection(
                url,
                DatabaseConfig.USER,
                DatabaseConfig.PASSWORD
            )
            
            // Timeout já configurado na URL através de connectTimeout e socketTimeout
            // setNetworkTimeout não está disponível na versão 5.1.49 do MySQL Connector
            
            connection
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro ao criar conexão MySQL: ${e.message}", e)
            null
        }
    }
    
    private suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        // Inicializar pool se necessário
        if (!poolInitialized) {
            initializePool()
        }
        
        // Tentar obter conexão do pool
        var connection: Connection? = null
        
        poolMutex.withLock {
            connection = connectionPool.poll()
        }
        
        // Se não houver conexão no pool ou se estiver fechada, criar nova
        if (connection == null || connection!!.isClosed) {
            connection = createNewConnection()
        }
        
        connection ?: throw Exception("Não foi possível obter conexão com o banco de dados")
    }
    
    private suspend fun returnConnection(connection: Connection?) {
        if (connection == null) return
        
        poolMutex.withLock {
            // Verificar se o pool não está muito grande
            if (connectionPool.size < DatabaseConfig.MAX_CONNECTIONS) {
                // Verificar se a conexão ainda está válida
                try {
                    if (!connection.isClosed) {
                        // Tentar verificar se está válida (pode não estar disponível em versões antigas)
                        try {
                            if (connection.isValid(2)) {
                                connectionPool.offer(connection)
                            } else {
                                connection.close()
                            }
                        } catch (e: NoSuchMethodError) {
                            // isValid pode não estar disponível, apenas verificar se não está fechada
                            connectionPool.offer(connection)
                        }
                    } else {
                        connection.close()
                    }
                } catch (e: Exception) {
                    try {
                        connection.close()
                    } catch (e2: Exception) {
                        // Ignorar erro ao fechar
                    }
                }
            } else {
                try {
                    connection.close()
                } catch (e: Exception) {
                    // Ignorar erro ao fechar
                }
            }
        }
    }
    
    suspend fun <T> executeQuery(
        sql: String,
        params: List<Any?> = emptyList(),
        mapper: (ResultSet) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        
        try {
            connection = getConnection()
            statement = connection.prepareStatement(sql)
            
            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> statement.setObject(index + 1, param)
                }
            }
            
            resultSet = statement.executeQuery()
            
            if (resultSet.next()) {
                Result.success(mapper(resultSet))
            } else {
                Result.failure(Exception("No results found"))
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro ao executar query: ${e.message}", e)
            Log.e("DatabaseHelper", "SQL: $sql")
            Result.failure(e)
        } finally {
            resultSet?.close()
            statement?.close()
            returnConnection(connection)
        }
    }
    
    suspend fun <T> executeQueryList(
        sql: String,
        params: List<Any?> = emptyList(),
        mapper: (ResultSet) -> T
    ): Result<List<T>> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        
        try {
            connection = getConnection()
            statement = connection.prepareStatement(sql)
            
            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> statement.setObject(index + 1, param)
                }
            }
            
            resultSet = statement.executeQuery()
            val results = mutableListOf<T>()
            
            while (resultSet.next()) {
                results.add(mapper(resultSet))
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro ao executar queryList: ${e.message}", e)
            Log.e("DatabaseHelper", "SQL: $sql")
            Result.failure(e)
        } finally {
            resultSet?.close()
            statement?.close()
            returnConnection(connection)
        }
    }
    
    suspend fun executeUpdate(
        sql: String,
        params: List<Any?> = emptyList()
    ): Result<Int> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        
        try {
            connection = getConnection()
            statement = connection.prepareStatement(sql)
            
            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> statement.setObject(index + 1, param)
                }
            }
            
            val rowsAffected = statement.executeUpdate()
            Result.success(rowsAffected)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro ao executar update: ${e.message}", e)
            Log.e("DatabaseHelper", "SQL: $sql")
            Result.failure(e)
        } finally {
            statement?.close()
            returnConnection(connection)
        }
    }
    
    suspend fun executeTransaction(block: (Connection) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        
        try {
            connection = getConnection()
            Log.d("DatabaseHelper", "Iniciando transação - autoCommit atual: ${connection.autoCommit}")
            connection.autoCommit = false
            Log.d("DatabaseHelper", "autoCommit desabilitado")
            
            block(connection)
            
            Log.d("DatabaseHelper", "Executando commit da transação...")
            connection.commit()
            Log.d("DatabaseHelper", "Commit executado com sucesso")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro na transação: ${e.message}", e)
            try {
                Log.d("DatabaseHelper", "Executando rollback...")
                connection?.rollback()
                Log.d("DatabaseHelper", "Rollback executado")
            } catch (e2: Exception) {
                Log.e("DatabaseHelper", "Erro ao fazer rollback: ${e2.message}", e2)
            }
            Result.failure(e)
        } finally {
            try {
                connection?.autoCommit = true
                Log.d("DatabaseHelper", "autoCommit reabilitado")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Erro ao reabilitar autoCommit: ${e.message}", e)
            }
            returnConnection(connection)
        }
    }
    
    fun generateUUID(): String = UUID.randomUUID().toString()
}

