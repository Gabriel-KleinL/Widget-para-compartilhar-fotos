# Arquitetura do Sistema - Viva Comigo

## Visão Geral

**Viva Comigo** é um aplicativo Android de compartilhamento de fotos entre casais que permite enviar e receber fotos através de um widget na tela inicial.

## Pilha Tecnológica

### Android App
- **Linguagem**: Kotlin 1.9.20
- **UI**: Jetpack Compose + Material 3
- **Arquitetura**: MVVM (Model-View-ViewModel)
- **Gerenciamento de Estado**: StateFlow + ViewModel
- **Banco de Dados**: MySQL (conexão JDBC direta) + DataStore (cache local)
- **Widget**: Glance (AppWidget moderno)
- **Background**: WorkManager
- **Imagens**: Coil + ExifInterface
- **Versão Mínima**: Android 8.0 (API 26)
- **Versão Alvo**: Android 14 (API 34)

### Backend (Node.js)
- **Runtime**: Node.js 18+
- **Framework**: Express 4.18.2
- **Autenticação**: JWT + bcryptjs
- **Upload**: Multer
- **Status**: Parcialmente usado (Android conecta diretamente ao MySQL)

### Banco de Dados
- **SGBD**: MySQL 8
- **Host**: srv1965.hstgr.io:3306
- **Database**: u466620993_poker
- **Tabelas Principais**: `users`, `photos`

## Arquitetura em Camadas

```
┌─────────────────────────────────────────────────────────┐
│                    UI LAYER (Compose)                   │
│  ┌──────────────┬──────────────┬─────────────────────┐ │
│  │ MainActivity │ HomeScreen   │ PairingScreen       │ │
│  │              │              │                     │ │
│  │ - Navegação  │ - Foto       │ - Código pareamento│ │
│  │ - Theme      │ - Enviar     │ - Parear com código│ │
│  └──────────────┴──────────────┴─────────────────────┘ │
│                          ↕                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ PhotoWidget (Glance)                            │  │
│  │ - Exibe última foto recebida na home screen     │  │
│  │ - Atualiza via WorkManager (30 min)            │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────┐
│                  VIEWMODEL LAYER                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ MainViewModel                                    │  │
│  │                                                  │  │
│  │ State: StateFlow<MainUiState>                   │  │
│  │ - authState: AuthState                          │  │
│  │ - currentUser: User?                            │  │
│  │ - partner: User?                                │  │
│  │ - latestPhoto: Photo?                           │  │
│  │ - isLoading: Boolean                            │  │
│  │ - error: String?                                │  │
│  │                                                  │  │
│  │ Funções:                                        │  │
│  │ - loadUserData()                                │  │
│  │ - pairWithPartner(code)                         │  │
│  │ - sendPhoto(uri)                                │  │
│  │ - unpairPartner()                               │  │
│  │ - startPolling() // 30s                         │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────┐
│                  REPOSITORY LAYER                       │
│  ┌──────────────┬──────────────┬────────────────────┐  │
│  │AuthRepository│UserRepository│ PhotoRepository    │  │
│  │              │              │                    │  │
│  │- Usuário     │- CRUD users  │- Upload/download   │  │
│  │  local       │- Pareamento  │- getLatestPhoto    │  │
│  │- DataStore   │- Transações  │- getPhotoImage     │  │
│  └──────────────┴──────────────┴────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ImageHelper                                      │  │
│  │ - Conversão bitmap↔bytes                        │  │
│  │ - Aplicação de rotação EXIF                     │  │
│  │ - Redimensionamento para widget                 │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────┐
│                    DATA LAYER                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │ DatabaseHelper (JDBC Pool)                       │  │
│  │ - Pool de 3 conexões MySQL                      │  │
│  │ - Suporte a transações                          │  │
│  │ - Timeout: 30s                                  │  │
│  │ - Execução em Dispatchers.IO                    │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ DataStore (SharedPreferences)                    │  │
│  │ - Armazena user_id local                        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↕ JDBC
┌─────────────────────────────────────────────────────────┐
│                   MySQL DATABASE                        │
│                srv1965.hstgr.io:3306                    │
│                                                         │
│  users:                                                 │
│  - id (PK, UUID)                                        │
│  - email                                                │
│  - pairing_code (UNIQUE, 6 chars)                       │
│  - partner_id (FK → users.id)                           │
│  - display_name                                         │
│                                                         │
│  photos:                                                │
│  - id (PK, UUID)                                        │
│  - sender_id (FK → users.id)                            │
│  - receiver_id (FK → users.id)                          │
│  - image_data (LONGBLOB até 10MB)                       │
│  - timestamp (BIGINT)                                   │
│  - seen (BOOLEAN)                                       │
└─────────────────────────────────────────────────────────┘
```

## Fluxos Principais

### 1. Primeira Execução (Criação de Usuário Local)

```
App Inicia
    ↓
MainActivity.onCreate()
    ↓
MainViewModel.init() → loadUserData()
    ↓
AuthRepository.ensureLocalUser()
    ├─ Verifica DataStore.userId
    ├─ Se não existe:
    │   ├─ Gera UUID
    │   ├─ Gera código pareamento único (6 dígitos alfanuméricos)
    │   ├─ INSERT INTO users (id, pairing_code, display_name)
    │   └─ Salva userId em DataStore
    └─ Retorna User
    ↓
UI mostra PairingScreen com código do usuário
```

**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt:35`

### 2. Pareamento de Usuários

```
Usuário A tem código: ABC123
Usuário B insere código: ABC123
    ↓
PairingScreen.onPair("ABC123")
    ↓
MainViewModel.pairWithPartner("ABC123")
    ↓
UserRepository.findUserByPairingCode("ABC123")
    ├─ SELECT * FROM users WHERE pairing_code = ?
    └─ Retorna Usuário A
    ↓
UserRepository.pairUsers(userIdB, userIdA)
    ↓
DatabaseHelper.executeTransaction {
    ├─ UPDATE users SET partner_id = A WHERE id = B
    └─ UPDATE users SET partner_id = B WHERE id = A
}
    ↓
Ambos os dispositivos mostram HomeScreen
```

**Arquivos**:
- `app/src/main/java/com/vivacomigo/app/ui/screen/PairingScreen.kt:75`
- `app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt:48`

### 3. Envio de Foto

```
Usuário seleciona foto
    ↓
HomeScreen.onSendPhoto(uri)
    ↓
MainViewModel.sendPhoto(uri)
    ↓
PhotoRepository.uploadPhoto(uri, senderId, receiverId)
    ├─ Valida que receiver é partner
    ├─ Lê bytes da imagem (contentResolver)
    ├─ INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp)
    │   ↳ image_data = BLOB binário
    └─ Retorna Photo
    ↓
MainViewModel.updateWidget()
    ├─ GlanceAppWidgetManager.update()
    └─ Widget do receiver é atualizado
```

**Arquivos**:
- `app/src/main/java/com/vivacomigo/app/ui/screen/HomeScreen.kt:120`
- `app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt:35`

### 4. Recebimento de Foto (Polling)

```
MainViewModel.init()
    ↓
startPolling() // Loop a cada 30 segundos
    ↓
loadLatestPhoto()
    ↓
PhotoRepository.getLatestPhotoForUser(userId)
    ├─ SELECT * FROM photos
    │  WHERE receiver_id = ?
    │  ORDER BY timestamp DESC
    │  LIMIT 1
    └─ Retorna Photo?
    ↓
Se photo existe:
    ├─ PhotoRepository.getPhotoImage(photoId)
    │   └─ SELECT image_data FROM photos WHERE id = ?
    ├─ ImageHelper.saveImageToCache(bytes, photoId)
    ├─ ImageHelper.bytesToBitmap(bytes)
    │   └─ Aplica rotação EXIF
    └─ StateFlow.update { latestPhoto = photo }
    ↓
UI exibe foto em HomeScreen
Widget atualiza automaticamente
```

**Arquivos**:
- `app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt:89`
- `app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt:75`

### 5. Sincronização de Widget (Background)

```
VivaApp.onCreate()
    ↓
WorkManager.enqueueUniquePeriodicWork(
    "widget_sync",
    repeatInterval = 30.minutes
)
    ↓
PhotoWidgetWorker.doWork() [Executa a cada 30 min]
    ├─ AuthRepository.ensureLocalUser()
    ├─ PhotoRepository.getLatestPhotoForUser(userId)
    ├─ PhotoWidgetDataStore.savePhotoId(photoId)
    └─ GlanceAppWidget.update()
    ↓
Widget exibe última foto ou ❤️ emoji
```

**Arquivos**:
- `app/src/main/java/com/vivacomigo/app/VivaApp.kt:23`
- `app/src/main/java/com/vivacomigo/app/widget/PhotoWidgetWorker.kt:25`

## Componentes Chave

### MainActivity
- **Responsabilidade**: Navegação entre PairingScreen e HomeScreen
- **Observa**: MainViewModel.uiState (StateFlow)
- **Decisão de navegação**: Baseada em authState (PAIRED vs UNPAIRED)
- **Localização**: `app/src/main/java/com/vivacomigo/app/MainActivity.kt`

### MainViewModel
- **Responsabilidade**: Gerenciamento de estado centralizado
- **Estado**: MainUiState (authState, currentUser, partner, latestPhoto, isLoading, error)
- **Polling**: Atualiza latestPhoto a cada 30 segundos
- **Operações**: pairWithPartner, sendPhoto, unpairPartner
- **Localização**: `app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt`

### DatabaseHelper
- **Responsabilidade**: Pool de conexões JDBC + Transações
- **Pool**: 3 conexões máximo
- **Timeout**: 30 segundos
- **Thread Safety**: Usa Dispatchers.IO
- **Localização**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseHelper.kt`

### PhotoRepository
- **Responsabilidade**: Upload/download de fotos
- **Armazenamento**: BLOB binário no MySQL (até 10MB)
- **Validação**: Verifica se sender e receiver são parceiros
- **Cache**: Salva imagens em cache local
- **Localização**: `app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt`

### PhotoWidget (Glance)
- **Responsabilidade**: Exibir última foto na home screen
- **Framework**: Glance (AppWidget moderno)
- **Atualização**: WorkManager (30 min) + Manual (após envio)
- **Fallback**: Emoji ❤️ se não houver foto
- **Localização**: `app/src/main/java/com/vivacomigo/app/widget/PhotoWidget.kt`

### AuthRepository
- **Responsabilidade**: Gerenciar usuário local
- **Armazenamento**: DataStore (SharedPreferences)
- **Operação principal**: ensureLocalUser() - cria se não existir
- **Localização**: `app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt`

## Configurações

### DatabaseConfig
```kotlin
// app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt
const val HOST = "srv1965.hstgr.io"
const val PORT = 3306
const val USER = "u466620993_gabrielklein24"
const val PASSWORD = "W!M$EL?y6"
const val DATABASE = "u466620993_poker"
const val CONNECTION_TIMEOUT_MS = 30000 // 30s
const val MAX_CONNECTIONS = 3
```

**NOTA**: Estas credenciais estão hardcoded. Em produção, devem ser movidas para BuildConfig ou variáveis de ambiente.

### Gradle (app/build.gradle.kts)
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.vivacomigo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.material3:material3")

    // Database
    implementation("mysql:mysql-connector-java:5.1.49")

    // Widget
    implementation("androidx.glance:glance-appwidget:1.0.0")

    // Background
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}
```

## Permissões Android

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
```

## Sincronização e Performance

### Polling (Foreground)
- **Frequência**: 30 segundos
- **Implementação**: MainViewModel.startPolling() com delay
- **Cancelamento**: viewModelScope cancela ao destruir ViewModel

### WorkManager (Background)
- **Frequência**: 30 minutos
- **Work Type**: PeriodicWorkRequest
- **Constraints**: Nenhuma (executa mesmo sem internet)
- **Política**: KEEP (mantém único job)

### Cache de Imagens
- **Localização**: context.cacheDir/photos/
- **Formato**: {photoId}.jpg
- **Limpeza**: Manual (não implementado auto-cleanup)

## Segurança

### Autenticação
- **Método**: Código de pareamento (6 dígitos alfanuméricos)
- **Unicidade**: Garantida por constraint UNIQUE no MySQL
- **Validação**: Backend não é usado - validação direta no Android

### Comunicação
- **Protocolo**: JDBC sobre TCP/IP
- **SSL**: Desabilitado (USE_SSL = false)
- **Timeout**: 30 segundos

### Armazenamento
- **Fotos**: BLOB binário no MySQL (não criptografado)
- **Usuário local**: DataStore (SharedPreferences - não criptografado)

**NOTA**: Em produção, considerar:
1. Criptografia de BLOB
2. SSL/TLS para conexões MySQL
3. Encrypted DataStore
4. Autenticação adicional (biometria)

## Limitações Conhecidas

1. **Conexão Direta MySQL**: App Android conecta diretamente ao MySQL (não usa backend Node.js)
   - Expõe credenciais no APK
   - Dificulta escalabilidade
   - Recomendação: Migrar para API REST via backend

2. **Credenciais Hardcoded**: DatabaseConfig.kt contém senha em texto claro
   - Recomendação: Usar BuildConfig ou NDK para ofuscar

3. **Sem Criptografia**: Fotos armazenadas em BLOB sem criptografia
   - Recomendação: Implementar criptografia AES-256

4. **Polling Ineficiente**: Polling a cada 30s consome bateria
   - Recomendação: Implementar Firebase Cloud Messaging (FCM) ou WebSocket

5. **Sem Validação de Tamanho**: Fotos podem ter até 10MB (limite MySQL)
   - Recomendação: Comprimir antes de upload

## Próximos Passos

1. **Migrar para API REST**: Usar backend Node.js ao invés de JDBC direto
2. **Implementar FCM**: Substituir polling por push notifications
3. **Adicionar Testes**: Unit tests e Integration tests
4. **Segurança**: SSL, criptografia de dados, ofuscação de credenciais
5. **Features**: Galeria de fotos, reações, mensagens de texto
6. **CI/CD**: GitHub Actions para builds automatizados

## Referências

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Glance AppWidget](https://developer.android.com/jetpack/androidx/releases/glance)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/5.1/en/)
