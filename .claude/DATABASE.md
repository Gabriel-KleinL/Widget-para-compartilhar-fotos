# Documentação de Banco de Dados - Viva Comigo

## Configuração do Banco

**SGBD**: MySQL 8
**Host**: srv1965.hstgr.io:3306
**Database**: u466620993_poker
**User**: u466620993_gabrielklein24
**Engine**: InnoDB
**Charset**: utf8mb4_unicode_ci

## Esquema de Tabelas

### users

Armazena informações de usuários e pareamentos.

```sql
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,                      -- UUID do usuário
    email VARCHAR(255) NULL,                         -- Email (opcional, usado no backend)
    pairing_code VARCHAR(6) UNIQUE NOT NULL,        -- Código de pareamento único
    partner_id VARCHAR(36) NULL,                     -- ID do parceiro (NULL se não pareado)
    display_name VARCHAR(255) NOT NULL,              -- Nome de exibição
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Data de criação
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- Data de atualização
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pairing_code (pairing_code),           -- Índice para busca por código
    INDEX idx_partner_id (partner_id),               -- Índice para busca por parceiro

    FOREIGN KEY (partner_id)                         -- FK para relacionamento
        REFERENCES users(id)
        ON DELETE SET NULL                           -- Despareia se parceiro deletado
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Campos

| Campo | Tipo | Nulo | Descrição |
|-------|------|------|-----------|
| `id` | VARCHAR(36) | NOT NULL | UUID gerado no Android via `UUID.randomUUID()` |
| `email` | VARCHAR(255) | NULL | Email do usuário (usado apenas no backend Node.js) |
| `pairing_code` | VARCHAR(6) | NOT NULL | Código único de 6 caracteres (A-Z, 0-9) |
| `partner_id` | VARCHAR(36) | NULL | ID do parceiro pareado (NULL = não pareado) |
| `display_name` | VARCHAR(255) | NOT NULL | Nome de exibição do usuário |
| `created_at` | TIMESTAMP | NOT NULL | Data de criação do registro |
| `updated_at` | TIMESTAMP | NOT NULL | Data da última atualização |

#### Constraints

- **PRIMARY KEY**: `id`
- **UNIQUE**: `pairing_code`
- **FOREIGN KEY**: `partner_id` → `users(id)` ON DELETE SET NULL
- **INDEX**: `idx_pairing_code` (otimiza busca por código)
- **INDEX**: `idx_partner_id` (otimiza busca por parceiro)

#### Estados Possíveis

```
┌─────────────┐
│ UNPAIRED    │ partner_id = NULL
├─────────────┤
│ User A      │ pairing_code: ABC123
│ partner_id: │ Aguardando pareamento
│ NULL        │
└─────────────┘
         ↓ pairUsers(A, B)
┌─────────────┐
│ PAIRED      │ partner_id != NULL
├─────────────┤
│ User A      │ pairing_code: ABC123
│ partner_id: │ Pareado com User B
│ uuid-B      │
└─────────────┘
```

---

### photos

Armazena fotos enviadas entre parceiros.

```sql
CREATE TABLE IF NOT EXISTS photos (
    id VARCHAR(36) PRIMARY KEY,                      -- UUID da foto
    sender_id VARCHAR(36) NOT NULL,                  -- ID do remetente
    receiver_id VARCHAR(36) NOT NULL,                -- ID do destinatário
    image_data LONGBLOB NOT NULL,                    -- Dados binários da imagem
    timestamp BIGINT NOT NULL,                       -- Timestamp (millis since epoch)
    seen BOOLEAN DEFAULT FALSE,                      -- Se foi visualizada
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Data de criação no banco

    INDEX idx_receiver_timestamp (receiver_id, timestamp),  -- Busca rápida por receiver + data
    INDEX idx_sender_id (sender_id),                 -- Busca por remetente

    FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE CASCADE,                           -- Deleta fotos se usuário deletado
    FOREIGN KEY (receiver_id)
        REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Campos

| Campo | Tipo | Nulo | Descrição |
|-------|------|------|-----------|
| `id` | VARCHAR(36) | NOT NULL | UUID gerado via `UUID.randomUUID()` |
| `sender_id` | VARCHAR(36) | NOT NULL | ID do usuário que enviou a foto |
| `receiver_id` | VARCHAR(36) | NOT NULL | ID do usuário que receberá a foto |
| `image_data` | LONGBLOB | NOT NULL | Bytes da imagem (máx ~10MB) |
| `timestamp` | BIGINT | NOT NULL | `System.currentTimeMillis()` no momento do upload |
| `seen` | BOOLEAN | NOT NULL | `false` por padrão, marcado como `true` quando visualizado |
| `created_at` | TIMESTAMP | NOT NULL | Timestamp de criação no MySQL |

#### Constraints

- **PRIMARY KEY**: `id`
- **FOREIGN KEY**: `sender_id` → `users(id)` ON DELETE CASCADE
- **FOREIGN KEY**: `receiver_id` → `users(id)` ON DELETE CASCADE
- **INDEX**: `idx_receiver_timestamp` (otimiza query `ORDER BY timestamp DESC`)
- **INDEX**: `idx_sender_id` (otimiza busca por remetente)

#### Tamanho de LONGBLOB

- **Máximo teórico**: 4GB
- **Limite prático**: ~10MB (configuração do aplicativo)
- **Recomendação**: Comprimir imagens antes de salvar

---

## Queries Comuns

### Usuários

#### 1. Criar novo usuário
```sql
INSERT INTO users (id, pairing_code, display_name, created_at)
VALUES (?, ?, ?, NOW());
```

**Parâmetros**:
- `id`: UUID gerado no Android
- `pairing_code`: Código de 6 caracteres (A-Z0-9)
- `display_name`: Nome do usuário (padrão: "Usuário")

**Usado em**: `AuthRepository.ensureLocalUser()`

---

#### 2. Buscar usuário por ID
```sql
SELECT id, email, pairing_code, partner_id, display_name
FROM users
WHERE id = ?;
```

**Parâmetros**:
- `id`: UUID do usuário

**Usado em**: `UserRepository.getCurrentUser()`, `UserRepository.getUserById()`

---

#### 3. Buscar usuário por código de pareamento
```sql
SELECT id, email, pairing_code, partner_id, display_name
FROM users
WHERE pairing_code = ?;
```

**Parâmetros**:
- `pairing_code`: Código de 6 caracteres

**Usado em**: `UserRepository.findUserByPairingCode()`

**Performance**: Usa índice `idx_pairing_code` (busca O(log n))

---

#### 4. Parear dois usuários (TRANSAÇÃO)
```sql
START TRANSACTION;

UPDATE users SET partner_id = ? WHERE id = ?;  -- Atualiza User A
UPDATE users SET partner_id = ? WHERE id = ?;  -- Atualiza User B

COMMIT;
```

**Parâmetros**:
- `partner_id (1)`: ID do User B
- `id (1)`: ID do User A
- `partner_id (2)`: ID do User A
- `id (2)`: ID do User B

**Usado em**: `UserRepository.pairUsers()`

**Importante**: Sempre usar transação para garantir atomicidade!

---

#### 5. Desparear dois usuários (TRANSAÇÃO)
```sql
START TRANSACTION;

UPDATE users SET partner_id = NULL WHERE id = ?;  -- Despareia User A
UPDATE users SET partner_id = NULL WHERE id = ?;  -- Despareia User B

COMMIT;
```

**Usado em**: `UserRepository.unpairUsers()`

---

#### 6. Atualizar nome de exibição
```sql
UPDATE users SET display_name = ? WHERE id = ?;
```

**Parâmetros**:
- `display_name`: Novo nome
- `id`: UUID do usuário

---

### Fotos

#### 1. Upload de foto
```sql
INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen, created_at)
VALUES (?, ?, ?, ?, ?, false, NOW());
```

**Parâmetros**:
- `id`: UUID da foto
- `sender_id`: UUID do remetente
- `receiver_id`: UUID do destinatário
- `image_data`: Bytes da imagem (BLOB)
- `timestamp`: `System.currentTimeMillis()`

**Usado em**: `PhotoRepository.uploadPhoto()`

**Exemplo Kotlin**:
```kotlin
val sql = """
    INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen, created_at)
    VALUES (?, ?, ?, ?, ?, false, NOW())
""".trimIndent()

databaseHelper.executeUpdate(
    sql,
    photoId,           // UUID
    senderId,          // UUID
    receiverId,        // UUID
    imageBytes,        // ByteArray
    System.currentTimeMillis()
)
```

---

#### 2. Buscar última foto recebida
```sql
SELECT id, sender_id, receiver_id, timestamp, seen
FROM photos
WHERE receiver_id = ?
ORDER BY timestamp DESC
LIMIT 1;
```

**Parâmetros**:
- `receiver_id`: UUID do usuário

**Usado em**:
- `PhotoRepository.getLatestPhotoForUser()`
- Polling (MainViewModel a cada 30s)
- Widget sync (PhotoWidgetWorker a cada 30 min)

**Performance**: Usa índice composto `idx_receiver_timestamp` (busca + ordenação O(log n))

---

#### 3. Buscar dados binários da foto
```sql
SELECT image_data
FROM photos
WHERE id = ?;
```

**Parâmetros**:
- `id`: UUID da foto

**Usado em**: `PhotoRepository.getPhotoImage()`

**Nota**: Retorna ByteArray que é convertido para Bitmap

---

#### 4. Marcar foto como visualizada
```sql
UPDATE photos SET seen = true WHERE id = ?;
```

**Parâmetros**:
- `id`: UUID da foto

**Atualmente não implementado**, mas pode ser usado para:
- Indicador de "não lida"
- Analytics

---

#### 5. Buscar histórico de fotos
```sql
SELECT id, sender_id, receiver_id, timestamp, seen
FROM photos
WHERE receiver_id = ?
ORDER BY timestamp DESC
LIMIT ?;
```

**Parâmetros**:
- `receiver_id`: UUID do usuário
- `limit`: Quantidade de fotos (ex: 10)

**Não implementado**, mas útil para feature de galeria

---

#### 6. Contar fotos enviadas por um usuário
```sql
SELECT COUNT(*) as total
FROM photos
WHERE sender_id = ?;
```

**Uso potencial**: Estatísticas, gamificação

---

#### 7. Deletar foto
```sql
DELETE FROM photos WHERE id = ?;
```

**Nota**: Quando usuário é deletado, CASCADE deleta automaticamente suas fotos

---

### Queries de Relacionamento

#### 1. Buscar todas as fotos trocadas entre um casal
```sql
SELECT id, sender_id, receiver_id, timestamp, seen
FROM photos
WHERE (sender_id = ? AND receiver_id = ?)
   OR (sender_id = ? AND receiver_id = ?)
ORDER BY timestamp DESC;
```

**Parâmetros**:
- `sender_id (1)`: User A
- `receiver_id (1)`: User B
- `sender_id (2)`: User B
- `receiver_id (2)`: User A

---

#### 2. Verificar se dois usuários são parceiros
```sql
SELECT 1
FROM users
WHERE id = ?
  AND partner_id = ?;
```

**Retorna**: 1 se parceiros, vazio se não

**Usado em**: Validação antes de upload de foto

---

## Índices e Performance

### Índices Existentes

| Tabela | Índice | Colunas | Uso |
|--------|--------|---------|-----|
| users | PRIMARY | id | Busca por UUID |
| users | UNIQUE | pairing_code | Busca por código (pareamento) |
| users | idx_pairing_code | pairing_code | Busca otimizada |
| users | idx_partner_id | partner_id | Busca por parceiro |
| photos | PRIMARY | id | Busca por UUID |
| photos | idx_receiver_timestamp | receiver_id, timestamp | Busca última foto (CRITICAL!) |
| photos | idx_sender_id | sender_id | Busca fotos enviadas |

### Query Plans

#### getLatestPhotoForUser (CRITICAL PATH)
```sql
EXPLAIN SELECT id, sender_id, receiver_id, timestamp, seen
FROM photos
WHERE receiver_id = 'uuid'
ORDER BY timestamp DESC
LIMIT 1;
```

**Resultado esperado**:
```
+----+-------------+--------+------------+------+------------------------+------------------------+---------+-------+------+----------+-------------+
| id | select_type | table  | partitions | type | possible_keys          | key                    | key_len | ref   | rows | filtered | Extra       |
+----+-------------+--------+------------+------+------------------------+------------------------+---------+-------+------+----------+-------------+
|  1 | SIMPLE      | photos | NULL       | ref  | idx_receiver_timestamp | idx_receiver_timestamp | 146     | const |    5 |   100.00 | Using index |
+----+-------------+--------+------------+------+------------------------+------------------------+---------+-------+------+----------+-------------+
```

**Key points**:
- `type: ref` → Usa índice
- `key: idx_receiver_timestamp` → Índice correto
- `Extra: Using index` → Covering index (não precisa acessar tabela)

---

## Migrações

### Adicionar Campo à Tabela users

```sql
-- Adicionar campo phone
ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL AFTER display_name;

-- Criar índice se necessário
CREATE INDEX idx_phone ON users(phone);
```

**Checklist**:
1. ✅ Executar ALTER TABLE no banco de produção
2. ✅ Atualizar `schema.sql`
3. ✅ Atualizar data class `User.kt`
4. ✅ Atualizar queries em `UserRepository.kt`
5. ✅ Atualizar backend `User.js` (se usado)

---

### Adicionar Campo à Tabela photos

```sql
-- Adicionar campo caption (legenda da foto)
ALTER TABLE photos ADD COLUMN caption TEXT NULL AFTER image_data;
```

**Checklist**:
1. ✅ Executar ALTER TABLE
2. ✅ Atualizar `schema.sql`
3. ✅ Atualizar data class `Photo.kt`
4. ✅ Atualizar queries em `PhotoRepository.kt`
5. ✅ Atualizar UI para exibir caption

---

### Criar Nova Tabela (Exemplo: reactions)

```sql
CREATE TABLE IF NOT EXISTS reactions (
    id VARCHAR(36) PRIMARY KEY,
    photo_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    emoji VARCHAR(10) NOT NULL,  -- ❤️, 😂, 😍, etc
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_photo_id (photo_id),

    FOREIGN KEY (photo_id)
        REFERENCES photos(id)
        ON DELETE CASCADE,
    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    UNIQUE KEY unique_reaction (photo_id, user_id)  -- Um usuário = uma reação por foto
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Backup e Restore

### Backup

```bash
# Backup completo
mysqldump -h srv1965.hstgr.io \
  -u u466620993_gabrielklein24 \
  -p \
  u466620993_poker > backup_$(date +%Y%m%d).sql

# Backup apenas estrutura
mysqldump -h srv1965.hstgr.io \
  -u u466620993_gabrielklein24 \
  -p \
  --no-data \
  u466620993_poker > schema_backup.sql

# Backup apenas dados
mysqldump -h srv1965.hstgr.io \
  -u u466620993_gabrielklein24 \
  -p \
  --no-create-info \
  u466620993_poker > data_backup.sql
```

### Restore

```bash
# Restore completo
mysql -h srv1965.hstgr.io \
  -u u466620993_gabrielklein24 \
  -p \
  u466620993_poker < backup_20260107.sql

# Restore apenas estrutura
mysql -h srv1965.hstgr.io \
  -u u466620993_gabrielklein24 \
  -p \
  u466620993_poker < schema_backup.sql
```

---

## Monitoramento e Manutenção

### Estatísticas de Tabelas

```sql
-- Tamanho das tabelas
SELECT
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'u466620993_poker'
ORDER BY (data_length + index_length) DESC;
```

### Contagem de Registros

```sql
-- Total de usuários
SELECT COUNT(*) as total_users FROM users;

-- Usuários pareados
SELECT COUNT(*) as paired_users FROM users WHERE partner_id IS NOT NULL;

-- Total de fotos
SELECT COUNT(*) as total_photos FROM photos;

-- Fotos visualizadas
SELECT COUNT(*) as seen_photos FROM photos WHERE seen = true;
```

### Espaço Usado por BLOB

```sql
-- Espaço total usado por fotos
SELECT
    COUNT(*) as total_photos,
    ROUND(SUM(LENGTH(image_data)) / 1024 / 1024, 2) as total_mb,
    ROUND(AVG(LENGTH(image_data)) / 1024, 2) as avg_kb
FROM photos;
```

### Limpeza de Dados Antigos

```sql
-- Deletar fotos com mais de 90 dias
DELETE FROM photos
WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Deletar usuários não pareados há mais de 30 dias
DELETE FROM users
WHERE partner_id IS NULL
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

---

## Troubleshooting

### Problema: "Duplicate entry for key 'pairing_code'"

**Causa**: Código de pareamento já existe

**Solução**:
```kotlin
// Regenerar código até encontrar único
fun generateUniquePairingCode(): String {
    var code = generatePairingCode()
    var attempts = 0

    while (isPairingCodeTaken(code) && attempts < 10) {
        code = generatePairingCode()
        attempts++
    }

    return code
}
```

---

### Problema: "BLOB too large"

**Causa**: Imagem maior que 10MB

**Solução**:
```kotlin
// Comprimir antes de upload
val compressedBytes = compressImage(originalBytes, maxSizeBytes = 5 * 1024 * 1024)
```

---

### Problema: Query lenta em `getLatestPhotoForUser`

**Diagnóstico**:
```sql
EXPLAIN SELECT * FROM photos WHERE receiver_id = ? ORDER BY timestamp DESC LIMIT 1;
```

**Solução**:
- Verificar se índice `idx_receiver_timestamp` existe
- Recriar índice se corrompido:
```sql
DROP INDEX idx_receiver_timestamp ON photos;
CREATE INDEX idx_receiver_timestamp ON photos(receiver_id, timestamp);
```

---

### Problema: Conexão timeout

**Causa**: Firewall ou credenciais incorretas

**Teste de conexão**:
```bash
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p
```

**Verificar configuração**:
```kotlin
// app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt
const val HOST = "srv1965.hstgr.io"
const val PORT = 3306
const val USER = "u466620993_gabrielklein24"
const val PASSWORD = "W!M$EL?y6"  // Verificar se correto
const val DATABASE = "u466620993_poker"
```

---

## Considerações de Segurança

### ⚠️ Problemas Atuais

1. **Credenciais Hardcoded**:
   - `DatabaseConfig.kt` contém senha em texto claro
   - APK pode ser descompilado e senha extraída
   - **Recomendação**: Usar BuildConfig ou NDK

2. **Conexão sem SSL**:
   - `USE_SSL = false` em `DatabaseConfig.kt`
   - Dados trafegam em texto claro
   - **Recomendação**: Habilitar SSL/TLS

3. **BLOB não criptografado**:
   - Fotos armazenadas sem criptografia
   - **Recomendação**: AES-256 antes de salvar

4. **Sem validação de parceiro**:
   - Qualquer usuário pode enviar foto para qualquer ID
   - **Recomendação**: Validar `partner_id` antes de INSERT

### ✅ Melhorias Recomendadas

```kotlin
// Validar antes de upload
suspend fun uploadPhoto(...): Result<Photo> {
    // Verificar se sender e receiver são parceiros
    val arePartners = userRepository.arePartners(senderId, receiverId)

    if (!arePartners) {
        return Result.failure(Exception("Users are not paired"))
    }

    // Criptografar antes de salvar
    val encryptedBytes = encryptImage(imageBytes)

    // ...
}
```

---

## Referências

- **Schema Original**: `backend/database/schema.sql`
- **DatabaseHelper**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseHelper.kt`
- **Repositories**: `app/src/main/java/com/vivacomigo/app/data/repository/`
- **MySQL Connector/J Docs**: https://dev.mysql.com/doc/connector-j/5.1/en/

---

**Última Atualização**: 2026-01-07
