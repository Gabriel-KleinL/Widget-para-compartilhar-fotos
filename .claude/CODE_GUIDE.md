# Guia de Código para IA - Viva Comigo

Este documento fornece orientações para IA trabalhar eficientemente no código do projeto.

## Índice
1. [Estrutura de Diretórios](#estrutura-de-diretórios)
2. [Padrões de Código](#padrões-de-código)
3. [Como Fazer Mudanças Comuns](#como-fazer-mudanças-comuns)
4. [Debugging e Troubleshooting](#debugging-e-troubleshooting)
5. [Testes](#testes)
6. [Convenções de Nomenclatura](#convenções-de-nomenclatura)

## Estrutura de Diretórios

### Android App (`/app`)

```
app/src/main/
├── java/com/vivacomigo/app/
│   ├── MainActivity.kt              # ✅ PONTO DE ENTRADA - modifique para mudar navegação
│   ├── VivaApp.kt                   # ✅ Application class - modifique para configurações globais
│   │
│   ├── data/
│   │   ├── database/
│   │   │   ├── DatabaseConfig.kt    # ⚙️ CONFIGURAÇÕES - credenciais MySQL
│   │   │   └── DatabaseHelper.kt    # 🔧 UTILITÁRIO - pool JDBC, transações
│   │   ├── model/
│   │   │   ├── User.kt              # 📦 DATA CLASS - modelo de usuário
│   │   │   └── Photo.kt             # 📦 DATA CLASS - modelo de foto
│   │   └── repository/
│   │       ├── AuthRepository.kt    # 💾 DADOS - gerencia usuário local
│   │       ├── UserRepository.kt    # 💾 DADOS - CRUD de usuários
│   │       ├── PhotoRepository.kt   # 💾 DADOS - upload/download fotos
│   │       └── ImageHelper.kt       # 🔧 UTILITÁRIO - processamento de imagens
│   │
│   ├── ui/
│   │   ├── screen/
│   │   │   ├── HomeScreen.kt        # 🎨 UI - tela principal (após pareamento)
│   │   │   └── PairingScreen.kt     # 🎨 UI - tela de pareamento
│   │   ├── theme/
│   │   │   └── Theme.kt             # 🎨 TEMA - cores, tipografia
│   │   └── viewmodel/
│   │       └── MainViewModel.kt     # 🧠 LÓGICA - estado centralizado
│   │
│   └── widget/
│       ├── PhotoWidget.kt           # 🔲 WIDGET - UI do widget
│       ├── PhotoWidgetReceiver.kt   # 🔲 WIDGET - receiver
│       ├── PhotoWidgetWorker.kt     # ⏰ BACKGROUND - sincronização
│       └── PhotoWidgetDataStore.kt  # 💾 DADOS - armazenamento widget
│
└── res/
    ├── drawable/                    # 🎨 ÍCONES E IMAGENS
    ├── values/
    │   ├── strings.xml              # 📝 STRINGS - textos do app
    │   ├── colors.xml               # 🎨 CORES
    │   └── themes.xml               # 🎨 TEMAS XML
    └── xml/
        └── photo_widget_info.xml    # ⚙️ METADADOS DO WIDGET
```

### Backend (`/backend`)

```
backend/src/
├── server.js                        # ✅ PONTO DE ENTRADA - Express app
├── config/
│   └── database.js                  # ⚙️ POOL MYSQL
├── models/
│   ├── User.js                      # 📦 MODEL - usuário
│   └── Photo.js                     # 📦 MODEL - foto
├── controllers/
│   ├── authController.js            # 🎮 CONTROLLER - registro/login
│   ├── userController.js            # 🎮 CONTROLLER - CRUD usuários
│   └── photoController.js           # 🎮 CONTROLLER - upload/download
├── routes/
│   ├── auth.js                      # 🛣️ ROTAS - /api/auth
│   ├── users.js                     # 🛣️ ROTAS - /api/users
│   └── photos.js                    # 🛣️ ROTAS - /api/photos
└── middleware/
    └── authMiddleware.js            # 🔒 MIDDLEWARE - JWT verification
```

## Padrões de Código

### Kotlin (Android)

#### 1. Data Classes
```kotlin
// ✅ CORRETO: Imutável, com valores padrão
data class User(
    val id: String = "",
    val email: String = "",
    val pairing_code: String = "",
    val partner_id: String? = null,
    val display_name: String = ""
)

// ❌ INCORRETO: Mutável
data class User(
    var id: String,
    var email: String
)
```

#### 2. Repository Pattern
```kotlin
// ✅ CORRETO: Usa suspend functions, retorna Result
class PhotoRepository(
    private val databaseHelper: DatabaseHelper,
    private val imageHelper: ImageHelper
) {
    suspend fun uploadPhoto(
        imageUri: Uri,
        senderId: String,
        receiverId: String,
        context: Context
    ): Result<Photo> = withContext(Dispatchers.IO) {
        try {
            // Lógica aqui
            Result.success(photo)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo", e)
            Result.failure(e)
        }
    }
}

// ❌ INCORRETO: Não usa suspend, não trata erros
fun uploadPhoto(imageUri: Uri): Photo {
    // Bloqueia thread principal
    return photo
}
```

#### 3. ViewModel com StateFlow
```kotlin
// ✅ CORRETO: StateFlow imutável, operações em viewModelScope
class MainViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun sendPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            photoRepository.uploadPhoto(uri, ...).fold(
                onSuccess = { photo ->
                    _uiState.update { it.copy(latestPhoto = photo, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }
}

// ❌ INCORRETO: LiveData, mutação direta de estado
class MainViewModel : ViewModel() {
    val latestPhoto = MutableLiveData<Photo>()

    fun sendPhoto(uri: Uri) {
        latestPhoto.value = photo // Não é thread-safe
    }
}
```

#### 4. Compose UI
```kotlin
// ✅ CORRETO: Composable puro, recebe estado e callbacks
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onSendPhoto: (Uri) -> Unit,
    onUnpair: () -> Unit
) {
    Column {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            PhotoDisplay(photo = uiState.latestPhoto)
            Button(onClick = { /* lança photo picker */ }) {
                Text("Enviar Foto")
            }
        }
    }
}

// ❌ INCORRETO: Acessa ViewModel diretamente
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val photo = viewModel.latestPhoto.value
    // Acoplamento forte
}
```

### JavaScript/Node.js (Backend)

#### 1. Controllers com Async/Await
```javascript
// ✅ CORRETO: Async/await, tratamento de erros, status codes claros
const uploadPhoto = async (req, res) => {
    try {
        const { sender_id, receiver_id } = req.body;
        const imageBuffer = req.file.buffer;

        // Validação
        if (!sender_id || !receiver_id) {
            return res.status(400).json({
                error: 'Sender and receiver required'
            });
        }

        // Lógica de negócio
        const photo = await photoService.create({
            sender_id,
            receiver_id,
            image_data: imageBuffer
        });

        res.status(201).json({ photo: photo.toJSON() });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ error: error.message });
    }
};

// ❌ INCORRETO: Callbacks, sem tratamento de erros
const uploadPhoto = (req, res) => {
    const photo = photoService.create(req.body); // Síncrono, sem await
    res.json(photo);
};
```

#### 2. Middleware de Autenticação
```javascript
// ✅ CORRETO: Verifica token, adiciona userId ao request
const authMiddleware = async (req, res, next) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: 'No token provided' });
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.userId = decoded.userId;
        req.email = decoded.email;

        next();
    } catch (error) {
        res.status(401).json({ error: 'Invalid token' });
    }
};

// ❌ INCORRETO: Não valida token
const authMiddleware = (req, res, next) => {
    next(); // Sempre passa
};
```

## Como Fazer Mudanças Comuns

### 1. Adicionar um Novo Campo ao User

**Passo 1**: Atualizar schema MySQL
```sql
-- backend/database/schema.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

**Passo 2**: Atualizar data class Kotlin
```kotlin
// app/src/main/java/com/vivacomigo/app/data/model/User.kt
data class User(
    val id: String = "",
    val email: String = "",
    val pairing_code: String = "",
    val partner_id: String? = null,
    val display_name: String = "",
    val phone: String = "" // ✅ ADICIONAR AQUI
)
```

**Passo 3**: Atualizar repository
```kotlin
// app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt
suspend fun getUserById(userId: String): Result<User?> = withContext(Dispatchers.IO) {
    val sql = "SELECT id, email, pairing_code, partner_id, display_name, phone FROM users WHERE id = ?" // ✅ ADICIONAR phone
    // ...
    User(
        id = resultSet.getString("id"),
        email = resultSet.getString("email") ?: "",
        pairing_code = resultSet.getString("pairing_code") ?: "",
        partner_id = resultSet.getString("partner_id"),
        display_name = resultSet.getString("display_name") ?: "",
        phone = resultSet.getString("phone") ?: "" // ✅ ADICIONAR AQUI
    )
}
```

**Passo 4**: Atualizar backend (se usado)
```javascript
// backend/src/models/User.js
class User {
    constructor(data) {
        this.phone = data.phone || ''; // ✅ ADICIONAR AQUI
    }
}
```

### 2. Adicionar uma Nova Tela

**Passo 1**: Criar arquivo de Composable
```kotlin
// app/src/main/java/com/vivacomigo/app/ui/screen/SettingsScreen.kt
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onNavigateBack: () -> Unit
) {
    Column {
        Text("Configurações")
        // UI aqui
    }
}
```

**Passo 2**: Adicionar ao Navigation
```kotlin
// app/src/main/java/com/vivacomigo/app/MainActivity.kt
when (uiState.authState) {
    AuthState.PAIRED -> {
        if (uiState.showSettings) { // ✅ ADICIONAR FLAG
            SettingsScreen(
                uiState = uiState,
                onNavigateBack = { viewModel.hideSettings() }
            )
        } else {
            HomeScreen(...)
        }
    }
}
```

**Passo 3**: Adicionar estado ao ViewModel
```kotlin
// app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt
data class MainUiState(
    // ... campos existentes
    val showSettings: Boolean = false // ✅ ADICIONAR
)

fun showSettings() {
    _uiState.update { it.copy(showSettings = true) }
}

fun hideSettings() {
    _uiState.update { it.copy(showSettings = false) }
}
```

### 3. Adicionar um Novo Endpoint na API

**Passo 1**: Criar controller
```javascript
// backend/src/controllers/userController.js
const updateProfile = async (req, res) => {
    try {
        const { userId } = req; // De authMiddleware
        const { display_name, phone } = req.body;

        const [result] = await pool.execute(
            'UPDATE users SET display_name = ?, phone = ? WHERE id = ?',
            [display_name, phone, userId]
        );

        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

module.exports = { updateProfile };
```

**Passo 2**: Adicionar rota
```javascript
// backend/src/routes/users.js
const { updateProfile } = require('../controllers/userController');
const authMiddleware = require('../middleware/authMiddleware');

router.put('/profile', authMiddleware, updateProfile); // ✅ NOVA ROTA
```

**Passo 3**: Adicionar ao repository Android
```kotlin
// app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt
suspend fun updateProfile(
    userId: String,
    displayName: String,
    phone: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val sql = "UPDATE users SET display_name = ?, phone = ? WHERE id = ?"
        databaseHelper.executeUpdate(sql, displayName, phone, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 4. Modificar o Widget

**Passo 1**: Atualizar UI do Widget
```kotlin
// app/src/main/java/com/vivacomigo/app/widget/PhotoWidget.kt
@Composable
fun PhotoWidgetContent(context: Context, photoId: String?) {
    if (photoId != null) {
        // Carregar foto
        val bitmap = loadPhotoFromCache(context, photoId)
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto recebida",
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // ✅ MODIFICAR FALLBACK AQUI
            Text("❤️", fontSize = 48.sp)
        }
    }
}
```

**Passo 2**: Atualizar frequência de sincronização
```kotlin
// app/src/main/java/com/vivacomigo/app/VivaApp.kt
private fun scheduleWidgetUpdates() {
    val workRequest = PeriodicWorkRequestBuilder<PhotoWidgetWorker>(
        15, TimeUnit.MINUTES // ✅ MUDAR DE 30 PARA 15 MINUTOS
    ).build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "widget_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
```

## Debugging e Troubleshooting

### Logs Importantes

#### Android (Kotlin)
```kotlin
// Use tags consistentes
private const val TAG = "PhotoRepository"

Log.d(TAG, "Uploading photo for user $userId") // Debug
Log.e(TAG, "Error uploading photo", exception) // Error
```

**Ver logs no terminal:**
```bash
adb logcat | grep "PhotoRepository"
```

#### Backend (Node.js)
```javascript
console.log('[PhotoController] Uploading photo:', photoId);
console.error('[PhotoController] Error:', error);
```

### Problemas Comuns

#### 1. "Connection timeout" ao conectar ao MySQL

**Causa**: App não consegue conectar ao MySQL remoto

**Solução**:
1. Verificar `DatabaseConfig.kt` - credenciais corretas?
2. Verificar firewall do servidor MySQL
3. Testar conexão manual:
```bash
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p
```

**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt`

#### 2. Widget não atualiza

**Causa**: WorkManager não está executando

**Solução**:
1. Verificar se WorkManager está agendado:
```kotlin
// app/src/main/java/com/vivacomigo/app/VivaApp.kt:23
// Deve chamar scheduleWidgetUpdates() no onCreate()
```

2. Forçar atualização manual:
```kotlin
viewModel.updateWidget() // Após enviar foto
```

3. Verificar logs:
```bash
adb logcat | grep "PhotoWidgetWorker"
```

**Arquivo**: `app/src/main/java/com/vivacomigo/app/widget/PhotoWidgetWorker.kt`

#### 3. Foto não aparece após envio

**Causa 1**: BLOB muito grande (>10MB)

**Solução**: Comprimir antes de upload
```kotlin
// app/src/main/java/com/vivacomigo/app/data/repository/ImageHelper.kt
// Já implementa compressão básica, mas pode ajustar qualidade
```

**Causa 2**: Permissão de leitura negada

**Solução**: Verificar se permissão foi concedida
```kotlin
// app/src/main/java/com/vivacomigo/app/ui/screen/HomeScreen.kt
// Usa Accompanist Permissions
```

#### 4. "Partner not found" ao parear

**Causa**: Código de pareamento não existe ou já foi usado

**Solução**:
1. Verificar no MySQL:
```sql
SELECT * FROM users WHERE pairing_code = 'ABC123';
```

2. Gerar novo código:
```kotlin
// Desparear e parear novamente
viewModel.unpairPartner()
```

**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt:48`

## Testes

### Atualmente Não Implementado

O projeto ainda não possui testes automatizados. Para adicionar:

#### Unit Tests (Android)
```kotlin
// app/src/test/java/com/vivacomigo/app/repository/PhotoRepositoryTest.kt
class PhotoRepositoryTest {
    @Test
    fun `uploadPhoto should return success when upload succeeds`() = runTest {
        // Arrange
        val mockDatabaseHelper = mockk<DatabaseHelper>()
        val repository = PhotoRepository(mockDatabaseHelper, mockk())

        // Act
        val result = repository.uploadPhoto(...)

        // Assert
        assertTrue(result.isSuccess)
    }
}
```

**Dependências necessárias (app/build.gradle.kts)**:
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

#### Integration Tests (Backend)
```javascript
// backend/test/auth.test.js
const request = require('supertest');
const app = require('../src/server');

describe('POST /api/auth/register', () => {
    it('should create a new user', async () => {
        const response = await request(app)
            .post('/api/auth/register')
            .send({ email: 'test@example.com', password: 'password123' });

        expect(response.status).toBe(201);
        expect(response.body).toHaveProperty('token');
    });
});
```

**Dependências necessárias (backend/package.json)**:
```json
{
    "devDependencies": {
        "jest": "^29.7.0",
        "supertest": "^6.3.3"
    }
}
```

## Convenções de Nomenclatura

### Kotlin

#### Classes
```kotlin
// ✅ PascalCase para classes, interfaces, objetos
class PhotoRepository
interface UserDao
object DatabaseConfig
```

#### Funções
```kotlin
// ✅ camelCase para funções
fun uploadPhoto()
suspend fun getLatestPhoto()
private fun validateInput()
```

#### Variáveis
```kotlin
// ✅ camelCase para variáveis
val currentUser: User
private val _uiState = MutableStateFlow(...)
const val MAX_CONNECTIONS = 3 // ✅ UPPER_SNAKE_CASE para constantes
```

#### Composables
```kotlin
// ✅ PascalCase para Composables
@Composable
fun HomeScreen() { }

@Composable
fun PhotoDisplay() { }
```

### JavaScript

#### Arquivos
```javascript
// ✅ camelCase para arquivos
authController.js
userRepository.js
authMiddleware.js
```

#### Funções
```javascript
// ✅ camelCase para funções
async function uploadPhoto() { }
const getUserById = async (id) => { }
```

#### Classes
```javascript
// ✅ PascalCase para classes
class User { }
class Photo { }
```

#### Constantes
```javascript
// ✅ UPPER_SNAKE_CASE para constantes
const JWT_SECRET = process.env.JWT_SECRET;
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
```

### SQL

#### Tabelas
```sql
-- ✅ snake_case para tabelas e colunas
CREATE TABLE users (
    id VARCHAR(36),
    pairing_code VARCHAR(6),
    partner_id VARCHAR(36)
);
```

## Arquivos Críticos - Não Modificar sem Cuidado

### ⚠️ DatabaseConfig.kt
**Localização**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt`

**Cuidado**: Contém credenciais hardcoded. Modificar pode quebrar acesso ao banco.

### ⚠️ DatabaseHelper.kt
**Localização**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseHelper.kt`

**Cuidado**: Gerencia pool de conexões JDBC. Erros podem causar connection leaks.

### ⚠️ MainViewModel.kt
**Localização**: `app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt`

**Cuidado**: Estado centralizado. Mudanças afetam toda a aplicação.

### ⚠️ schema.sql
**Localização**: `backend/database/schema.sql`

**Cuidado**: Schema do banco. Mudanças requerem migrações.

## Comandos Úteis

### Build Android
```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Instalar em dispositivo
./gradlew installDebug
```

### Executar Backend
```bash
cd backend
npm install
npm start # Produção
npm run dev # Desenvolvimento (com nodemon)
```

### Ver logs Android
```bash
# Logs gerais
adb logcat

# Filtrar por tag
adb logcat | grep "PhotoRepository"

# Limpar logs
adb logcat -c
```

### MySQL
```bash
# Conectar ao banco
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p

# Backup
mysqldump -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker > backup.sql

# Restore
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < backup.sql
```

## Checklist para Mudanças

Antes de fazer commit, verificar:

- [ ] Código compila sem erros
- [ ] Logs adicionados onde apropriado
- [ ] Tratamento de erros implementado (try/catch, Result)
- [ ] UI atualizada (se necessário)
- [ ] StateFlow atualizado (se mudança de estado)
- [ ] Documentação inline adicionada (se lógica complexa)
- [ ] Constantes movidas para arquivo de config (não hardcode)
- [ ] Permissões adicionadas ao AndroidManifest (se necessário)
- [ ] Backend sincronizado (se mudança de schema)

## Recursos Adicionais

- [ARCHITECTURE.md](.claude/ARCHITECTURE.md) - Arquitetura detalhada do sistema
- [COMPONENTS.md](.claude/COMPONENTS.md) - Documentação de componentes
- [DATABASE.md](.claude/DATABASE.md) - Schema e queries do banco
- [README.md](../README.md) - Documentação principal do projeto
