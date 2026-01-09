# API Documentation - Viva Comigo Backend

**Base URL**: `http://localhost:3000/api`
**Versão**: 1.0.0
**Data**: 2026-01-07

---

## 📋 Índice

1. [Autenticação](#autenticação)
2. [Usuários](#usuários)
3. [Fotos](#fotos)
4. [Segurança](#segurança)
5. [Códigos de Erro](#códigos-de-erro)

---

## 🔐 Autenticação

Todas as rotas (exceto `/auth/register` e `/auth/login`) requerem um token JWT no header:

```
Authorization: Bearer {seu-token-jwt}
```

### Rate Limiting:
- Rotas de autenticação: **5 requisições a cada 15 minutos**
- Outras rotas: **100 requisições a cada 15 minutos**

---

## 1️⃣ Autenticação

### POST `/api/auth/register`

Registra um novo usuário.

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "senha123",
  "display_name": "João Silva" // Opcional
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@example.com",
    "pairing_code": "ABC123",
    "display_name": "João Silva",
    "partner_id": null
  }
}
```

**Errors:**
- `400` - Email e senha são obrigatórios
- `400` - Email já está em uso

---

### POST `/api/auth/login`

Faz login de um usuário existente.

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "senha123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@example.com",
    "pairing_code": "ABC123",
    "display_name": "João Silva",
    "partner_id": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

**Errors:**
- `400` - Email e senha são obrigatórios
- `401` - Email ou senha inválidos

---

## 2️⃣ Usuários

### GET `/api/users/me`

Retorna dados do usuário autenticado.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@example.com",
  "pairing_code": "ABC123",
  "display_name": "João Silva",
  "partner_id": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Errors:**
- `401` - Token inválido ou expirado
- `404` - Usuário não encontrado

---

### GET `/api/users/:id`

Retorna dados de um usuário específico por ID.

**Headers:**
```
Authorization: Bearer {token}
```

**Parameters:**
- `id` (URL) - UUID do usuário

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "email": "parceiro@example.com",
  "pairing_code": "XYZ789",
  "display_name": "Maria Silva",
  "partner_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Errors:**
- `401` - Token inválido
- `404` - Usuário não encontrado

---

### GET `/api/users/pairing-code/:code`

Busca usuário por código de pareamento.

**Headers:**
```
Authorization: Bearer {token}
```

**Parameters:**
- `code` (URL) - Código de pareamento (6 caracteres)

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "email": "parceiro@example.com",
  "pairing_code": "XYZ789",
  "display_name": "Maria Silva",
  "partner_id": null
}
```

**Errors:**
- `401` - Token inválido
- `404` - Código de pareamento não encontrado

---

### POST `/api/users/pair`

Parear com outro usuário usando código de pareamento.

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "partnerCode": "XYZ789"
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@example.com",
  "pairing_code": "ABC123",
  "display_name": "João Silva",
  "partner_id": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Errors:**
- `400` - Código de pareamento é obrigatório
- `400` - Você não pode parear consigo mesmo
- `404` - Código de pareamento não encontrado
- `401` - Token inválido

**Nota:** Esta operação é uma transação que atualiza ambos os usuários simultaneamente.

---

### DELETE `/api/users/unpair`

Desparear do parceiro atual.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@example.com",
  "pairing_code": "ABC123",
  "display_name": "João Silva",
  "partner_id": null
}
```

**Errors:**
- `400` - Você não está pareado com ninguém
- `401` - Token inválido
- `404` - Usuário não encontrado

**Nota:** Esta operação remove o pareamento de ambos os usuários.

---

## 3️⃣ Fotos

### POST `/api/photos`

Upload de uma nova foto para o parceiro.

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
- `image` (File) - Arquivo da imagem (JPG, PNG, etc)
- `receiverId` (String) - UUID do destinatário

**Response (201 Created):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "sender_id": "550e8400-e29b-41d4-a716-446655440000",
  "receiver_id": "660e8400-e29b-41d4-a716-446655440001",
  "timestamp": 1704672000000,
  "seen": false
}
```

**Errors:**
- `400` - ID do destinatário é obrigatório
- `400` - Imagem é obrigatória
- `400` - Arquivo muito grande (máx 10MB)
- `403` - Você só pode enviar fotos para seu parceiro
- `404` - Usuário não encontrado
- `401` - Token inválido

---

### GET `/api/photos/latest`

Retorna a última foto recebida pelo usuário autenticado.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "sender_id": "660e8400-e29b-41d4-a716-446655440001",
  "receiver_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1704672000000,
  "seen": false,
  "created_at": "2026-01-07T10:30:00.000Z"
}
```

**Response quando não há fotos:**
```json
null
```

**Errors:**
- `401` - Token inválido

---

### GET `/api/photos`

Lista todas as fotos recebidas pelo usuário autenticado.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "sender_id": "660e8400-e29b-41d4-a716-446655440001",
    "receiver_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1704672000000,
    "seen": true,
    "created_at": "2026-01-07T10:30:00.000Z"
  },
  {
    "id": "770e8400-e29b-41d4-a716-446655440003",
    "sender_id": "660e8400-e29b-41d4-a716-446655440001",
    "receiver_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1704658800000,
    "seen": true,
    "created_at": "2026-01-07T06:00:00.000Z"
  }
]
```

**Errors:**
- `401` - Token inválido

---

### GET `/api/photos/:id/image`

Retorna os bytes da imagem de uma foto específica.

**Headers:**
```
Authorization: Bearer {token}
```

**Parameters:**
- `id` (URL) - UUID da foto

**Response (200 OK):**
```
Content-Type: image/jpeg
[Binary image data]
```

**Errors:**
- `401` - Token inválido
- `403` - Acesso negado (você não é sender nem receiver)
- `404` - Foto não encontrada

---

### PUT `/api/photos/:id/seen`

Marca uma foto como visualizada.

**Headers:**
```
Authorization: Bearer {token}
```

**Parameters:**
- `id` (URL) - UUID da foto

**Response (200 OK):**
```json
{
  "message": "Foto marcada como vista"
}
```

**Errors:**
- `401` - Token inválido
- `403` - Acesso negado (foto não pertence a você)
- `404` - Foto não encontrada

---

## 🔒 Segurança

### Headers de Segurança (Helmet)

O servidor adiciona automaticamente headers de segurança:

- `X-DNS-Prefetch-Control`
- `X-Frame-Options`
- `X-Content-Type-Options`
- `Strict-Transport-Security`
- `X-Download-Options`
- `X-Permitted-Cross-Domain-Policies`

### CORS

O servidor aceita requisições de qualquer origem (configurado para desenvolvimento).

**⚠️ Em produção, configurar origens permitidas:**

```javascript
app.use(cors({
  origin: ['https://seuapp.com', 'https://app.seuapp.com']
}));
```

### Compressão

Respostas são comprimidas automaticamente com gzip/deflate.

---

## ❌ Códigos de Erro

### 400 - Bad Request
Parâmetros inválidos ou faltando

### 401 - Unauthorized
Token JWT inválido, expirado ou não fornecido

### 403 - Forbidden
Operação não permitida (ex: enviar foto para não-parceiro)

### 404 - Not Found
Recurso não encontrado

### 429 - Too Many Requests
Rate limit excedido

### 500 - Internal Server Error
Erro no servidor

---

## 📝 Exemplos de Uso

### Exemplo 1: Registro e Login

```bash
# Registrar
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "senha123",
    "display_name": "João"
  }'

# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "senha123"
  }'
```

### Exemplo 2: Pareamento

```bash
# Usuário A obtém código
curl -X GET http://localhost:3000/api/users/me \
  -H "Authorization: Bearer {token-usuario-a}"

# Usuário B busca por código
curl -X GET http://localhost:3000/api/users/pairing-code/ABC123 \
  -H "Authorization: Bearer {token-usuario-b}"

# Usuário B pareia com A
curl -X POST http://localhost:3000/api/users/pair \
  -H "Authorization: Bearer {token-usuario-b}" \
  -H "Content-Type: application/json" \
  -d '{"partnerCode": "ABC123"}'
```

### Exemplo 3: Enviar Foto

```bash
curl -X POST http://localhost:3000/api/photos \
  -H "Authorization: Bearer {token}" \
  -F "image=@/path/to/photo.jpg" \
  -F "receiverId=660e8400-e29b-41d4-a716-446655440001"
```

### Exemplo 4: Buscar Última Foto

```bash
# Obter metadados
curl -X GET http://localhost:3000/api/photos/latest \
  -H "Authorization: Bearer {token}"

# Baixar imagem
curl -X GET http://localhost:3000/api/photos/{photo-id}/image \
  -H "Authorization: Bearer {token}" \
  --output photo.jpg
```

---

## 🚀 Iniciar Servidor

```bash
# Desenvolvimento (com nodemon)
npm run dev

# Produção
npm start
```

---

## 🧪 Testar API

Recomenda-se usar:
- **Postman**: https://www.postman.com/
- **Insomnia**: https://insomnia.rest/
- **Thunder Client** (VS Code extension)

---

**Última atualização**: 2026-01-07
**Autor**: Viva Comigo Backend Team
