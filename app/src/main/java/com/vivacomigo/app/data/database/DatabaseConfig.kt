package com.vivacomigo.app.data.database

/**
 * Configurações de conexão com o banco de dados MySQL.
 *
 * ⚠️ ATENÇÃO: Este arquivo contém credenciais hardcoded. Em produção:
 * 1. Migrar para BuildConfig ou variáveis de ambiente
 * 2. Usar NDK para ofuscar credenciais
 * 3. Implementar backend REST ao invés de conexão JDBC direta
 *
 * 📖 Documentação: Ver .claude/DATABASE.md para detalhes do schema
 */
object DatabaseConfig {
    /**
     * Configurações do servidor MySQL remoto
     */
    const val HOST = "srv1965.hstgr.io"
    const val PORT = 3306
    const val USER = "u466620993_gabrielklein24"
    const val PASSWORD = "W!M\$EL?y6" // ⚠️ HARDCODED - não compartilhar APK publicamente
    const val DATABASE = "u466620993_poker"

    /**
     * Configurações de pool de conexões
     *
     * CONNECTION_TIMEOUT_MS: Tempo máximo para estabelecer conexão (30s)
     * MAX_CONNECTIONS: Número máximo de conexões simultâneas no pool (3)
     * USE_SSL: Desabilitado por padrão (habilitar em produção)
     *
     * 📌 Nota: Pool gerenciado por DatabaseHelper.kt
     */
    const val CONNECTION_TIMEOUT_MS = 30000 // 30 segundos
    const val MAX_CONNECTIONS = 3 // Limitar para evitar sobrecarga
    const val USE_SSL = false // ⚠️ Habilitar em produção
}

