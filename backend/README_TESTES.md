# Como Executar os Testes da Fase 1

## 📋 Script de Testes Automatizados

O arquivo `test-fase1.sh` contém **28 testes automatizados** que verificam todos os endpoints do backend.

---

## 🚀 Como Executar

### 1. Iniciar o servidor

```bash
cd backend
npm start
```

Ou em modo desenvolvimento:
```bash
npm run dev
```

### 2. **IMPORTANTE**: Aguardar 15 minutos

O backend tem **rate limiting** configurado:
- **5 requisições de autenticação a cada 15 minutos**
- 100 requisições gerais a cada 15 minutos

Se você acabou de executar muitos testes manualmente, **aguarde 15 minutos** antes de rodar o script.

### 3. Executar o script

```bash
./test-fase1.sh
```

---

## 📊 O Que é Testado

### ✅ Autenticação (6 testes)
- Health check
- Registro simplificado (usuário A)
- Registro simplificado (usuário B)
- Login com nome
- Login com código de pareamento

### ✅ Usuários (2 testes)
- GET /users/me
- GET /users/pairing-code/:code

### ✅ Pareamento (1 teste)
- POST /users/pair

### ✅ Fotos (4 testes)
- POST /photos (upload)
- GET /photos/latest
- GET /photos (listar)
- GET /photos/:id/image (baixar)

### ✅ Desparear (1 teste)
- DELETE /users/unpair

### ✅ Segurança (2 testes)
- Endpoint protegido sem token (401)
- Headers de segurança (Helmet)

---

## 🎯 Resultado Esperado

```
═══════════════════════════════════════════════════════
  ✅ FASE 1: TODOS OS TESTES PASSARAM! 🎉
═══════════════════════════════════════════════════════

Total de testes: 28
✅ Passaram: 28
❌ Falharam: 0
```

---

## ⚠️ Problemas Comuns

### Erro: "Muitas tentativas de login"

**Causa**: Rate limiting ativo

**Solução**: Aguardar 15 minutos ou desabilitar rate limiting temporariamente:

```javascript
// src/server.js
// Comentar temporariamente:
// app.use('/api/auth/', authLimiter);
```

### Erro: "Servidor não está rodando"

**Causa**: Backend não foi iniciado

**Solução**:
```bash
npm start
```

### Erro: "Token de autenticação não fornecido"

**Causa**: Bug no script (headers mal formatados)

**Solução**: Verificar se os tokens foram gerados corretamente nos primeiros testes

---

## 🔄 Quando Executar os Testes

### Sempre executar após:
1. ✅ Modificar qualquer endpoint
2. ✅ Atualizar dependências
3. ✅ Completar uma nova fase
4. ✅ Fazer deploy
5. ✅ Modificar middleware de segurança

### Testes Regressivos
Ao final de cada fase, **todos os testes das fases anteriores devem passar**:

- **Fase 2**: Executar `test-fase1.sh` ✅
- **Fase 3**: Executar `test-fase1.sh` + `test-fase2.sh` ✅
- **Fase 4**: Executar `test-fase1.sh` + `test-fase2.sh` + `test-fase3.sh` ✅

---

## 🛠️ Modo Debug

Para ver todos os comandos curl executados:

```bash
bash -x test-fase1.sh 2>&1 | less
```

---

## 📝 Limpeza de Dados de Teste

Os testes criam usuários com nomes únicos (timestamp):
- `TestUser_A_1234567890`
- `TestUser_B_1234567890`

Para limpar manualmente:

```sql
DELETE FROM users WHERE display_name LIKE 'TestUser_%';
```

---

**Criado em**: 2026-01-07
**Última atualização**: 2026-01-07
