package com.vivacomigo.app.data.database

object DatabaseConfig {
    // Configuração do MySQL
    const val HOST = "srv1965.hstgr.io"
    const val PORT = 3306
    const val USER = "u466620993_gabrielklein24"
    const val PASSWORD = "W!M\$EL?y6"
    const val DATABASE = "u466620993_poker"
    
    // Configurações de conexão
    const val CONNECTION_TIMEOUT_MS = 30000 // 30 segundos em milissegundos
    const val MAX_CONNECTIONS = 3 // Limitar conexões simultâneas
    const val USE_SSL = false // Habilitar SSL se disponível no servidor
}

