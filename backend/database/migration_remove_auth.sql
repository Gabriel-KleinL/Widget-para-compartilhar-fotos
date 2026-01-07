-- Migração: Remover campos de autenticação (email obrigatório e password_hash)
-- Execute este script no banco de dados existente

-- Tornar email opcional (remover NOT NULL e UNIQUE se necessário)
ALTER TABLE users 
    MODIFY COLUMN email VARCHAR(255) NULL;

-- Remover índice único de email se existir (pode falhar se não existir, ignore o erro)
ALTER TABLE users DROP INDEX email;

-- Remover coluna password_hash
ALTER TABLE users DROP COLUMN password_hash;

