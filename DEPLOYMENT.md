# 🚀 Guia de Deployment - Viva Comigo

Este guia detalha como fazer o deploy completo do app Viva Comigo em produção.

---

## 📋 Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Backend (Render.com)](#backend-rendercom)
3. [Banco de Dados (MySQL)](#banco-de-dados-mysql)
4. [Firebase (FCM + Crashlytics)](#firebase-fcm--crashlytics)
5. [App Android](#app-android)
6. [Verificação e Testes](#verificação-e-testes)
7. [Monitoramento](#monitoramento)
8. [Troubleshooting](#troubleshooting)

---

## 🔧 Pré-requisitos

### Contas Necessárias
- ✅ [Render.com](https://render.com) (gratuito)
- ✅ [Firebase Console](https://console.firebase.google.com) (gratuito)
- ✅ [Hostinger](https://hostinger.com) ou outro MySQL (já possui)
- ✅ [Google Play Console](https://play.google.com/console) (opcional, US$ 25 one-time)

### Ferramentas Locais
```bash
# Node.js 18+ e npm
node --version  # v18.0.0 ou superior
npm --version   # 8.0.0 ou superior

# Android Studio (para build do APK)
# Git (para deploy via Render)
```

---

## 🌐 Backend (Render.com)

### 1. Preparar Repositório Git

```bash
# Inicializar Git (se ainda não tiver)
cd backend
git init
git add .
git commit -m "Initial commit"

# Push para GitHub/GitLab
git remote add origin https://github.com/seu-usuario/viva-comigo-backend.git
git push -u origin main
```

### 2. Criar Web Service no Render

1. Acesse [Render Dashboard](https://dashboard.render.com/)
2. Clique em **"New+"** → **"Web Service"**
3. Conecte seu repositório GitHub/GitLab
4. Configurações:

```yaml
Name: viva-comigo-backend
Environment: Node
Region: Ohio (US East) # ou mais próximo
Branch: main
Root Directory: backend
Build Command: npm install
Start Command: node src/server.js
Instance Type: Free
```

### 3. Configurar Variáveis de Ambiente

No painel do Render, vá em **Environment** e adicione:

```env
NODE_ENV=production
PORT=10000
JWT_SECRET=SEU_SECRET_AQUI_GERE_UM_RANDOM_STRING_LONGO

# Banco de Dados MySQL
DB_HOST=srv1965.hstgr.io
DB_USER=u466620993_gabrielklein24
DB_PASSWORD=W!M$EL?y6
DB_DATABASE=u466620993_poker

# Logs
LOG_LEVEL=info
```

**⚠️ IMPORTANTE**: Gere um JWT_SECRET forte:
```bash
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
```

### 4. Fazer Deploy

1. Clique em **"Create Web Service"**
2. Aguarde o build (3-5 minutos)
3. Sua API estará disponível em: `https://viva-comigo-backend.onrender.com`

### 5. Verificar Health Check

```bash
curl https://viva-comigo-backend.onrender.com/health
```

Resposta esperada:
```json
{
  "status": "OK",
  "message": "API Viva Comigo está funcionando",
  "timestamp": "2026-01-08T...",
  "docs": "https://viva-comigo-backend.onrender.com/api-docs"
}
```

---

## 🗄️ Banco de Dados (MySQL)

### 1. Verificar Conexão

O MySQL já está configurado na Hostinger. Verifique:

```bash
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p
# Senha: W!M$EL?y6
```

### 2. Executar Migrations

```bash
cd backend/database

# Migration 1: Estrutura inicial
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < schema.sql

# Migration 2: Adicionar FCM
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < migration_add_fcm.sql
```

### 3. Backup (CRÍTICO!)

**Antes de fazer qualquer mudança:**

```bash
# Backup completo
mysqldump -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker > backup_$(date +%Y%m%d).sql

# Restaurar (se necessário)
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < backup_20260108.sql
```

### 4. Verificar Tabelas

```sql
USE u466620993_poker;
SHOW TABLES;
-- Deve mostrar: users, photos

DESCRIBE users;
-- Deve ter: id, name, pairing_code, partner_id, fcm_token, created_at

DESCRIBE photos;
-- Deve ter: id, sender_id, receiver_id, image_data, created_at
```

---

## 🔥 Firebase (FCM + Crashlytics)

### 1. Criar Projeto Firebase

1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Clique em **"Adicionar projeto"**
3. Nome do projeto: `viva-comigo-app`
4. Desabilite Google Analytics (opcional)
5. Clique em **"Criar projeto"**

### 2. Adicionar App Android

1. No Firebase Console, clique no ícone Android
2. **Nome do pacote Android**: `com.vivacomigo.app`
3. **Apelido do app**: Viva Comigo
4. **SHA-1**: Deixe em branco por enquanto
5. Clique em **"Registrar app"**

### 3. Baixar google-services.json

1. Download do arquivo `google-services.json`
2. Copie para: `app/google-services.json`

```bash
cp ~/Downloads/google-services.json app/
```

### 4. Ativar Cloud Messaging

1. No Firebase Console: **Build** → **Cloud Messaging**
2. Clique em **"Começar"**
3. Não precisa configurar mais nada (Android já está pronto)

### 5. Ativar Crashlytics

1. No Firebase Console: **Build** → **Crashlytics**
2. Clique em **"Ativar Crashlytics"**
3. SDK já está configurado no app

### 6. Baixar Chave Privada (Backend)

1. **Configurações** (ícone de engrenagem) → **Configurações do projeto**
2. Aba **"Contas de serviço"**
3. Clique em **"Gerar nova chave privada"**
4. Salve como: `backend/firebase-admin-key.json`

```bash
cp ~/Downloads/viva-comigo-app-firebase-adminsdk-xxxxx.json backend/firebase-admin-key.json
```

### 7. Atualizar Render com Firebase

No Render, adicione as credenciais Firebase como variável de ambiente:

```env
# Copie TODO o conteúdo de firebase-admin-key.json
FIREBASE_ADMIN_KEY={"type":"service_account","project_id":"viva-comigo-app",...}
```

**OU** (recomendado) modifique `backend/src/config/firebase.js`:

```javascript
// Usar variável de ambiente em produção
const serviceAccount = process.env.NODE_ENV === 'production'
    ? JSON.parse(process.env.FIREBASE_ADMIN_KEY)
    : require('../../firebase-admin-key.json');
```

---

## 📱 App Android

### 1. Configurar BASE_URL

Edite `app/src/main/java/com/vivacomigo/app/data/api/ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val BASE_URL = "https://viva-comigo-backend.onrender.com/api/"
}
```

### 2. Build de Produção

#### Opção A: APK Debug (Teste)

```bash
cd /caminho/para/Widget-para-compartilhar-fotos

# Via Android Studio
# Build → Build Bundle(s) / APK(s) → Build APK(s)

# Via Linha de Comando (se tiver gradlew)
./gradlew assembleDebug

# APK gerado em:
# app/build/outputs/apk/debug/app-debug.apk
```

#### Opção B: APK Release (Produção)

**1. Gerar Keystore**:

```bash
keytool -genkey -v -keystore viva-comigo-release.keystore \
  -alias viva-comigo \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**2. Configurar app/build.gradle.kts**:

```kotlin
android {
    // ...
    signingConfigs {
        create("release") {
            storeFile = file("../viva-comigo-release.keystore")
            storePassword = "SUA_SENHA_AQUI"
            keyAlias = "viva-comigo"
            keyPassword = "SUA_SENHA_AQUI"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**3. Build Release**:

```bash
./gradlew assembleRelease

# APK gerado em:
# app/build/outputs/apk/release/app-release.apk
```

### 3. Instalação Manual

```bash
# Conectar dispositivo via USB (ativar Depuração USB)
adb devices

# Instalar APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Ver logs em tempo real
adb logcat | grep -E "VivaApp|MainViewModel|FirebaseCrashlytics"
```

### 4. Google Play Store (Opcional)

1. **Criar conta Google Play Console** (US$ 25 one-time)
2. **Criar app** no Play Console
3. **Upload do APK/AAB**:

```bash
# Gerar AAB (Android App Bundle)
./gradlew bundleRelease

# AAB gerado em:
# app/build/outputs/bundle/release/app-release.aab
```

4. **Preencher listagem**:
   - Ícone (512x512px)
   - Screenshots (mínimo 2)
   - Descrição
   - Categoria: Social
   - Classificação etária: Livre

5. **Publicar** (revisão pode levar 1-7 dias)

---

## ✅ Verificação e Testes

### 1. Testar Backend

```bash
# Health check
curl https://viva-comigo-backend.onrender.com/health

# Documentação Swagger
# Abrir no navegador:
https://viva-comigo-backend.onrender.com/api-docs

# Registrar usuário
curl -X POST https://viva-comigo-backend.onrender.com/api/auth/register-simple \
  -H "Content-Type: application/json" \
  -d '{"name":"Teste Deploy"}'

# Deve retornar: { "token": "...", "user": { "id": ..., "pairingCode": "ABC-123" } }
```

### 2. Testar App Android

1. **Registrar 2 usuários** no app
2. **Parear** usando código de pareamento
3. **Enviar foto** de User1 para User2
4. **Verificar push notification** em User2
5. **Verificar widget** atualiza automaticamente

### 3. Executar Testes Automatizados

```bash
cd /caminho/para/Widget-para-compartilhar-fotos

# Todos os testes (Fases 1-6)
bash tests/run-all-tests.sh

# Apenas backend
cd backend && npm test

# Apenas validações
bash tests/test-fase6.sh
```

---

## 📊 Monitoramento

### 1. Logs Backend (Render)

```bash
# No painel do Render, clique em "Logs"
# OU via Render CLI:
render logs -s viva-comigo-backend
```

### 2. Logs Android (Crashlytics)

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. **Crashlytics** → Ver crashes e ANRs
3. **Analytics** → Ver eventos (opcional)

### 3. Winston Logs (Backend Local)

```bash
cd backend

# Ver logs em tempo real
tail -f logs/combined-$(date +%Y-%M-%d).log

# Ver apenas erros
tail -f logs/error-$(date +%Y-%M-%d).log
```

### 4. Métricas Render

- **CPU Usage**: <50% em média
- **Memory**: <512MB
- **Response Time**: <500ms (p95)
- **Uptime**: >99%

---

## 🚨 Troubleshooting

### Backend não inicia no Render

**Problema**: Build falha ou service não sobe

**Soluções**:
```bash
# 1. Verificar logs do Render
# 2. Testar localmente:
cd backend
npm install
NODE_ENV=production node src/server.js

# 3. Verificar variáveis de ambiente estão corretas
# 4. Verificar conexão com MySQL:
node -e "require('./src/config/database').execute('SELECT 1')"
```

### Pushes não chegam no Android

**Problema**: Upload funciona mas notificação não aparece

**Checklist**:
1. ✅ `google-services.json` está no lugar certo?
2. ✅ `firebase-admin-key.json` está no backend?
3. ✅ FCM token foi enviado ao backend? (ver logs: `registerFcmToken`)
4. ✅ Permissão `POST_NOTIFICATIONS` foi concedida? (Android 13+)
5. ✅ App está em foreground ou background?
6. ✅ Backend envia push após upload? (ver logs: `Push notification enviada`)

### App crasheia ao abrir

**Problema**: Crash ao iniciar ou usar funcionalidade

**Debug**:
```bash
# Ver logs detalhados
adb logcat | grep -E "AndroidRuntime|Crash"

# Ver Crashlytics no Firebase Console
# Crashlytics mostra stack trace completo
```

### Upload de foto muito lento

**Problema**: Upload demora >10 segundos

**Verificações**:
1. ✅ ImageCompressor está ativo? (ver log: "Compressed image")
2. ✅ WiFi ou dados móveis estáveis?
3. ✅ Backend Render não está dormindo? (primeiro request pode demorar 30s)

**Solução para Render sleeping**:
- Upgrade para plano pago ($7/mês)
- OU: Use [Uptime Robot](https://uptimerobot.com) para ping a cada 5min

---

## 🔒 Segurança em Produção

### Checklist de Segurança

- [ ] **JWT_SECRET** é forte e único (64+ caracteres)
- [ ] **firebase-admin-key.json** NÃO está no Git
- [ ] **google-services.json** NÃO contém chaves secretas (OK expor)
- [ ] **DB_PASSWORD** está em variáveis de ambiente (não hardcoded)
- [ ] **HTTPS** está ativo no backend (Render faz automaticamente)
- [ ] **Rate limiting** está ativo (10 uploads/hora, 100 requests/15min)
- [ ] **Logs** não expõem senhas ou tokens

### Rotar Secrets (A cada 6 meses)

```bash
# 1. Gerar novo JWT_SECRET
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"

# 2. Atualizar no Render
# 3. Re-deploy backend

# 4. (Opcional) Rotar Firebase Admin Key
# Firebase Console → Contas de serviço → Gerar nova chave
```

---

## 📞 Suporte

### Recursos Úteis

- 📖 **Documentação API**: `/api-docs` no backend
- 📝 **CHANGELOG**: `CHANGELOG.md`
- 🧪 **Testes**: `tests/README.md`
- 🏗️ **Arquitetura**: `.claude/ARCHITECTURE.md`

### Contato

- **Issues**: [GitHub Issues](https://github.com/seu-usuario/viva-comigo/issues)
- **Email**: contato@vivacomigo.app

---

## 🎉 Próximos Passos

Após deploy bem-sucedido:

1. ✅ **Monitorar Crashlytics** primeiros dias
2. ✅ **Coletar feedback** de usuários beta
3. ✅ **Otimizar performance** baseado em métricas
4. ✅ **Implementar analytics** (Firebase Analytics)
5. ✅ **Adicionar features** (galeria de fotos, filtros, etc)

---

**Última atualização**: 08 de Janeiro de 2026
**Versão**: 1.6.0 (Fase 6 Completa)
