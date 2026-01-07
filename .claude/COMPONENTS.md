# Documentação de Componentes - Viva Comigo

Este documento detalha cada componente do sistema, suas responsabilidades, interfaces e como modificá-los.

## Índice
- [Android App](#android-app)
  - [UI Layer](#ui-layer)
  - [ViewModel Layer](#viewmodel-layer)
  - [Repository Layer](#repository-layer)
  - [Data Layer](#data-layer)
  - [Widget Layer](#widget-layer)
- [Backend](#backend)
  - [Controllers](#controllers)
  - [Routes](#routes)
  - [Middleware](#middleware)
  - [Models](#models)

---

# Android App

## UI Layer

### MainActivity
**Arquivo**: `app/src/main/java/com/vivacomigo/app/MainActivity.kt`

**Responsabilidade**: Ponto de entrada da aplicação, gerencia navegação principal.

**Funções**:
```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VivaTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (uiState.authState) {
                    AuthState.PAIRED -> HomeScreen(...)
                    AuthState.UNPAIRED -> PairingScreen(...)
                }
            }
        }
    }
}
```

**Estados de Navegação**:
- `AuthState.PAIRED` → Mostra `HomeScreen`
- `AuthState.UNPAIRED` → Mostra `PairingScreen`

**Como Modificar**:
```kotlin
// Adicionar nova tela
when (uiState.authState) {
    AuthState.PAIRED -> {
        if (uiState.showSettings) {
            SettingsScreen(...) // Nova tela
        } else {
            HomeScreen(...)
        }
    }
    AuthState.UNPAIRED -> PairingScreen(...)
}
```

**Dependências**:
- `MainViewModel`: Fornece estado e operações
- `VivaTheme`: Tema Material 3

---

### HomeScreen
**Arquivo**: `app/src/main/java/com/vivacomigo/app/ui/screen/HomeScreen.kt`

**Responsabilidade**: Exibe foto recebida, permite enviar nova foto, desparear.

**Interface**:
```kotlin
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onSendPhoto: (Uri) -> Unit,
    onUnpair: () -> Unit
)
```

**Estrutura**:
```
┌────────────────────────────────┐
│      Viva Comigo ❤️           │
├────────────────────────────────┤
│                                │
│   [Foto Recebida ou ❤️]       │
│                                │
├────────────────────────────────┤
│ Pareado com: Gabriel           │
├────────────────────────────────┤
│  [+] Enviar Foto               │
│  [Desparear]                   │
└────────────────────────────────┘
```

**Principais Composables**:
```kotlin
// Exibição de foto
if (uiState.latestPhoto != null) {
    AsyncImage(
        model = photoUri,
        contentDescription = "Foto recebida",
        modifier = Modifier.fillMaxWidth().height(400.dp),
        contentScale = ContentScale.Crop
    )
} else {
    Text("❤️", fontSize = 120.sp)
}

// Botão de enviar foto
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    uri?.let { onSendPhoto(it) }
}

Button(onClick = { launcher.launch("image/*") }) {
    Icon(Icons.Default.Add, contentDescription = "Enviar foto")
    Text("Enviar Foto")
}

// Botão de desparear
OutlinedButton(onClick = onUnpair) {
    Text("Desparear")
}
```

**Permissões Necessárias**:
- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12-)

**Como Modificar**:
```kotlin
// Adicionar galeria de fotos antigas
LazyColumn {
    items(uiState.photoHistory) { photo ->
        PhotoHistoryItem(photo)
    }
}

// Adicionar reação à foto
IconButton(onClick = { onReact("❤️") }) {
    Icon(Icons.Default.Favorite, contentDescription = "Curtir")
}
```

---

### PairingScreen
**Arquivo**: `app/src/main/java/com/vivacomigo/app/ui/screen/PairingScreen.kt`

**Responsabilidade**: Exibe código de pareamento, permite parear com parceiro.

**Interface**:
```kotlin
@Composable
fun PairingScreen(
    uiState: MainUiState,
    onPair: (String) -> Unit
)
```

**Estrutura**:
```
┌────────────────────────────────┐
│   Viva Comigo ❤️              │
├────────────────────────────────┤
│  Seu código de pareamento:    │
│                                │
│        ABC123                  │
│                                │
├────────────────────────────────┤
│  Código do parceiro:           │
│  [_______]                     │
│  [Parear]                      │
└────────────────────────────────┘
```

**Principais Composables**:
```kotlin
// Exibir código próprio
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
) {
    Text(
        text = uiState.currentUser?.pairing_code ?: "------",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

// Input do código do parceiro
var partnerCode by remember { mutableStateOf("") }

OutlinedTextField(
    value = partnerCode,
    onValueChange = { partnerCode = it.uppercase() },
    label = { Text("Código do parceiro") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

// Botão de parear
Button(
    onClick = { onPair(partnerCode) },
    enabled = partnerCode.length == 6 && !uiState.isLoading
) {
    Text("Parear")
}
```

**Validação**:
- Código deve ter exatamente 6 caracteres
- Código é automaticamente convertido para uppercase
- Botão desabilitado durante loading

**Como Modificar**:
```kotlin
// Adicionar QR Code para pareamento
val bitmap = generateQRCode(uiState.currentUser?.pairing_code ?: "")
Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code")

// Adicionar scanner de QR Code
val launcher = rememberLauncherForActivityResult(
    ScanQRCodeContract()
) { result ->
    result?.let { onPair(it) }
}
```

---

### VivaTheme
**Arquivo**: `app/src/main/java/com/vivacomigo/app/ui/theme/Theme.kt`

**Responsabilidade**: Define tema Material 3 da aplicação.

**Cores**:
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),      // Rosa vibrante
    secondary = Color(0xFFFF4081),     // Rosa accent
    tertiary = Color(0xFFFFC0CB),      // Rosa claro
    background = Color(0xFFFFFBFE),    // Branco levemente rosa
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF4081),
    secondary = Color(0xFFE91E63),
    tertiary = Color(0xFFFFC0CB),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F)
)
```

**Tipografia**:
```kotlin
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

**Como Modificar**:
```kotlin
// Mudar cor primária
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Roxo
    // ...
)

// Adicionar fonte customizada
val CustomFontFamily = FontFamily(
    Font(R.font.roboto_regular),
    Font(R.font.roboto_bold, FontWeight.Bold)
)
```

---

## ViewModel Layer

### MainViewModel
**Arquivo**: `app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt`

**Responsabilidade**: Gerenciamento de estado centralizado, operações de negócio.

**Estado**:
```kotlin
data class MainUiState(
    val authState: AuthState = AuthState.UNPAIRED,
    val currentUser: User? = null,
    val partner: User? = null,
    val latestPhoto: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class AuthState {
    PAIRED,
    UNPAIRED
}
```

**StateFlow**:
```kotlin
private val _uiState = MutableStateFlow(MainUiState())
val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
```

**Funções Principais**:

#### loadUserData()
```kotlin
private fun loadUserData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        authRepository.ensureLocalUser().fold(
            onSuccess = { userId ->
                userRepository.getCurrentUser(userId).fold(
                    onSuccess = { user ->
                        _uiState.update {
                            it.copy(
                                currentUser = user,
                                authState = if (user.partner_id != null)
                                    AuthState.PAIRED else AuthState.UNPAIRED,
                                isLoading = false
                            )
                        }

                        if (user.partner_id != null) {
                            loadPartner(user.partner_id)
                            startPolling()
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message, isLoading = false)
                        }
                    }
                )
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(error = error.message, isLoading = false)
                }
            }
        )
    }
}
```

#### pairWithPartner()
```kotlin
fun pairWithPartner(partnerCode: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val currentUserId = _uiState.value.currentUser?.id ?: return@launch

        userRepository.findUserByPairingCode(partnerCode).fold(
            onSuccess = { partner ->
                if (partner == null) {
                    _uiState.update {
                        it.copy(
                            error = "Código inválido",
                            isLoading = false
                        )
                    }
                    return@fold
                }

                userRepository.pairUsers(currentUserId, partner.id).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                authState = AuthState.PAIRED,
                                partner = partner,
                                isLoading = false
                            )
                        }
                        startPolling()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message, isLoading = false)
                        }
                    }
                )
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(error = error.message, isLoading = false)
                }
            }
        )
    }
}
```

#### sendPhoto()
```kotlin
fun sendPhoto(uri: Uri) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val senderId = _uiState.value.currentUser?.id ?: return@launch
        val receiverId = _uiState.value.partner?.id ?: return@launch

        photoRepository.uploadPhoto(uri, senderId, receiverId, context).fold(
            onSuccess = { photo ->
                _uiState.update {
                    it.copy(latestPhoto = photo, isLoading = false)
                }
                updateWidget()
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(error = error.message, isLoading = false)
                }
            }
        )
    }
}
```

#### startPolling()
```kotlin
private fun startPolling() {
    viewModelScope.launch {
        while (true) {
            delay(30_000) // 30 segundos
            loadLatestPhoto()
        }
    }
}

private suspend fun loadLatestPhoto() {
    val userId = _uiState.value.currentUser?.id ?: return

    photoRepository.getLatestPhotoForUser(userId).fold(
        onSuccess = { photo ->
            if (photo != null && photo.id != _uiState.value.latestPhoto?.id) {
                _uiState.update { it.copy(latestPhoto = photo) }
                updateWidget()
            }
        },
        onFailure = { /* Silenciar erros de polling */ }
    )
}
```

**Como Adicionar Nova Funcionalidade**:
```kotlin
// 1. Adicionar campo ao estado
data class MainUiState(
    // ... campos existentes
    val photoHistory: List<Photo> = emptyList()
)

// 2. Adicionar função pública
fun loadPhotoHistory() {
    viewModelScope.launch {
        val userId = _uiState.value.currentUser?.id ?: return@launch

        photoRepository.getPhotoHistory(userId, limit = 10).fold(
            onSuccess = { photos ->
                _uiState.update { it.copy(photoHistory = photos) }
            },
            onFailure = { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        )
    }
}
```

---

## Repository Layer

### AuthRepository
**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt`

**Responsabilidade**: Gerenciar usuário local (criação, armazenamento em DataStore).

**Funções**:

#### ensureLocalUser()
```kotlin
suspend fun ensureLocalUser(): Result<String> = withContext(Dispatchers.IO) {
    try {
        // Verificar se já existe userId no DataStore
        val existingUserId = dataStore.data.first()[USER_ID_KEY]

        if (existingUserId != null) {
            return@withContext Result.success(existingUserId)
        }

        // Criar novo usuário
        val userId = UUID.randomUUID().toString()
        val pairingCode = generatePairingCode()

        // Inserir no MySQL
        val sql = """
            INSERT INTO users (id, pairing_code, display_name, created_at)
            VALUES (?, ?, ?, NOW())
        """.trimIndent()

        databaseHelper.executeUpdate(sql, userId, pairingCode, "Usuário")

        // Salvar no DataStore
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }

        Result.success(userId)
    } catch (e: Exception) {
        Log.e(TAG, "Error ensuring local user", e)
        Result.failure(e)
    }
}
```

#### generatePairingCode()
```kotlin
private fun generatePairingCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars.random() }
        .joinToString("")
}
```

**DataStore**:
```kotlin
private val Context.dataStore by preferencesDataStore("viva_prefs")
private val USER_ID_KEY = stringPreferencesKey("user_id")
```

**Como Modificar**:
```kotlin
// Adicionar armazenamento de token JWT
suspend fun saveAuthToken(token: String) {
    dataStore.edit { preferences ->
        preferences[AUTH_TOKEN_KEY] = token
    }
}

suspend fun getAuthToken(): String? {
    return dataStore.data.first()[AUTH_TOKEN_KEY]
}
```

---

### UserRepository
**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt`

**Responsabilidade**: CRUD de usuários, pareamento.

**Funções**:

#### getCurrentUser()
```kotlin
suspend fun getCurrentUser(userId: String): Result<User> = withContext(Dispatchers.IO) {
    try {
        val sql = """
            SELECT id, email, pairing_code, partner_id, display_name
            FROM users
            WHERE id = ?
        """.trimIndent()

        val user = databaseHelper.executeQuery(sql, userId) { resultSet ->
            if (resultSet.next()) {
                User(
                    id = resultSet.getString("id"),
                    email = resultSet.getString("email") ?: "",
                    pairing_code = resultSet.getString("pairing_code") ?: "",
                    partner_id = resultSet.getString("partner_id"),
                    display_name = resultSet.getString("display_name") ?: ""
                )
            } else {
                null
            }
        }

        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting current user", e)
        Result.failure(e)
    }
}
```

#### pairUsers()
```kotlin
suspend fun pairUsers(userId: String, partnerId: String): Result<Unit> =
    withContext(Dispatchers.IO) {
    try {
        databaseHelper.executeTransaction { connection ->
            // Atualizar usuário atual
            val sql1 = "UPDATE users SET partner_id = ? WHERE id = ?"
            connection.prepareStatement(sql1).use { stmt ->
                stmt.setString(1, partnerId)
                stmt.setString(2, userId)
                stmt.executeUpdate()
            }

            // Atualizar parceiro
            connection.prepareStatement(sql1).use { stmt ->
                stmt.setString(1, userId)
                stmt.setString(2, partnerId)
                stmt.executeUpdate()
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error pairing users", e)
        Result.failure(e)
    }
}
```

#### unpairUsers()
```kotlin
suspend fun unpairUsers(userId: String, partnerId: String): Result<Unit> =
    withContext(Dispatchers.IO) {
    try {
        databaseHelper.executeTransaction { connection ->
            val sql = "UPDATE users SET partner_id = NULL WHERE id = ?"

            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeUpdate()
            }

            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, partnerId)
                stmt.executeUpdate()
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error unpairing users", e)
        Result.failure(e)
    }
}
```

**Como Adicionar Nova Operação**:
```kotlin
suspend fun updateDisplayName(
    userId: String,
    newName: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val sql = "UPDATE users SET display_name = ? WHERE id = ?"
        databaseHelper.executeUpdate(sql, newName, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating display name", e)
        Result.failure(e)
    }
}
```

---

### PhotoRepository
**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt`

**Responsabilidade**: Upload/download de fotos, cache de imagens.

**Funções**:

#### uploadPhoto()
```kotlin
suspend fun uploadPhoto(
    imageUri: Uri,
    senderId: String,
    receiverId: String,
    context: Context
): Result<Photo> = withContext(Dispatchers.IO) {
    try {
        // Ler bytes da imagem
        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { input ->
            input.readBytes()
        } ?: return@withContext Result.failure(Exception("Failed to read image"))

        // Validar tamanho (máx 10MB)
        if (imageBytes.size > 10 * 1024 * 1024) {
            return@withContext Result.failure(Exception("Image too large (max 10MB)"))
        }

        // Criar photo
        val photoId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val sql = """
            INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen, created_at)
            VALUES (?, ?, ?, ?, ?, false, NOW())
        """.trimIndent()

        databaseHelper.executeUpdate(sql, photoId, senderId, receiverId, imageBytes, timestamp)

        // Salvar no cache
        imageHelper.saveImageToCache(imageBytes, photoId, context)

        Result.success(Photo(
            id = photoId,
            sender_id = senderId,
            receiver_id = receiverId,
            timestamp = timestamp,
            seen = false
        ))
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading photo", e)
        Result.failure(e)
    }
}
```

#### getLatestPhotoForUser()
```kotlin
suspend fun getLatestPhotoForUser(userId: String): Result<Photo?> =
    withContext(Dispatchers.IO) {
    try {
        val sql = """
            SELECT id, sender_id, receiver_id, timestamp, seen
            FROM photos
            WHERE receiver_id = ?
            ORDER BY timestamp DESC
            LIMIT 1
        """.trimIndent()

        val photo = databaseHelper.executeQuery(sql, userId) { resultSet ->
            if (resultSet.next()) {
                Photo(
                    id = resultSet.getString("id"),
                    sender_id = resultSet.getString("sender_id"),
                    receiver_id = resultSet.getString("receiver_id"),
                    timestamp = resultSet.getLong("timestamp"),
                    seen = resultSet.getBoolean("seen")
                )
            } else {
                null
            }
        }

        Result.success(photo)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting latest photo", e)
        Result.failure(e)
    }
}
```

#### getPhotoImage()
```kotlin
suspend fun getPhotoImage(photoId: String, context: Context): Result<Bitmap?> =
    withContext(Dispatchers.IO) {
    try {
        // Verificar cache primeiro
        val cachedBitmap = imageHelper.loadImageFromCache(photoId, context)
        if (cachedBitmap != null) {
            return@withContext Result.success(cachedBitmap)
        }

        // Buscar do banco
        val sql = "SELECT image_data FROM photos WHERE id = ?"

        val bitmap = databaseHelper.executeQuery(sql, photoId) { resultSet ->
            if (resultSet.next()) {
                val bytes = resultSet.getBytes("image_data")
                imageHelper.bytesToBitmap(bytes)
            } else {
                null
            }
        }

        // Salvar no cache
        if (bitmap != null) {
            imageHelper.saveBitmapToCache(bitmap, photoId, context)
        }

        Result.success(bitmap)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting photo image", e)
        Result.failure(e)
    }
}
```

---

### ImageHelper
**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/repository/ImageHelper.kt`

**Responsabilidade**: Processamento de imagens (conversão, rotação EXIF, cache).

**Funções**:

#### bytesToBitmap()
```kotlin
fun bytesToBitmap(bytes: ByteArray): Bitmap? {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Aplicar rotação EXIF se necessário
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error converting bytes to bitmap", e)
        null
    }
}
```

#### saveImageToCache()
```kotlin
fun saveImageToCache(bytes: ByteArray, photoId: String, context: Context) {
    try {
        val cacheDir = File(context.cacheDir, "photos")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val file = File(cacheDir, "$photoId.jpg")
        file.outputStream().use { output ->
            output.write(bytes)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error saving image to cache", e)
    }
}
```

#### resizeForWidget()
```kotlin
fun resizeForWidget(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

    return if (ratio < 1.0f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (width * ratio).toInt(),
            (height * ratio).toInt(),
            true
        )
    } else {
        bitmap
    }
}
```

---

## Data Layer

### DatabaseHelper
**Arquivo**: `app/src/main/java/com/vivacomigo/app/data/database/DatabaseHelper.kt`

**Responsabilidade**: Pool de conexões JDBC, transações, execução de queries.

**Configuração**:
```kotlin
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"
    private val connectionPool = mutableListOf<Connection>()
    private val connectionLock = Mutex()

    private const val MAX_CONNECTIONS = 3
    private const val CONNECTION_TIMEOUT = 30000 // 30s
}
```

**Funções**:

#### getConnection()
```kotlin
private suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
    connectionLock.withLock {
        // Procurar conexão disponível
        val availableConnection = connectionPool.find { !it.isClosed }

        if (availableConnection != null) {
            return@withContext availableConnection
        }

        // Criar nova conexão se abaixo do limite
        if (connectionPool.size < MAX_CONNECTIONS) {
            val newConnection = createConnection()
            connectionPool.add(newConnection)
            return@withContext newConnection
        }

        // Aguardar conexão disponível
        delay(1000)
        getConnection()
    }
}

private fun createConnection(): Connection {
    Class.forName("com.mysql.jdbc.Driver")

    val url = "jdbc:mysql://${DatabaseConfig.HOST}:${DatabaseConfig.PORT}/${DatabaseConfig.DATABASE}"

    return DriverManager.getConnection(url, DatabaseConfig.USER, DatabaseConfig.PASSWORD).apply {
        autoCommit = true
    }
}
```

#### executeQuery()
```kotlin
suspend fun <T> executeQuery(
    sql: String,
    vararg params: Any,
    mapper: (ResultSet) -> T
): T = withContext(Dispatchers.IO) {
    val connection = getConnection()

    connection.prepareStatement(sql).use { statement ->
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }

        val resultSet = statement.executeQuery()
        mapper(resultSet)
    }
}
```

#### executeUpdate()
```kotlin
suspend fun executeUpdate(
    sql: String,
    vararg params: Any
): Int = withContext(Dispatchers.IO) {
    val connection = getConnection()

    connection.prepareStatement(sql).use { statement ->
        params.forEachIndexed { index, param ->
            when (param) {
                is ByteArray -> statement.setBytes(index + 1, param)
                else -> statement.setObject(index + 1, param)
            }
        }

        statement.executeUpdate()
    }
}
```

#### executeTransaction()
```kotlin
suspend fun <T> executeTransaction(
    block: (Connection) -> T
): T = withContext(Dispatchers.IO) {
    val connection = getConnection()

    try {
        connection.autoCommit = false
        val result = block(connection)
        connection.commit()
        result
    } catch (e: Exception) {
        connection.rollback()
        throw e
    } finally {
        connection.autoCommit = true
    }
}
```

**Como Usar**:
```kotlin
// Query simples
val user = databaseHelper.executeQuery(
    "SELECT * FROM users WHERE id = ?",
    userId
) { resultSet ->
    if (resultSet.next()) {
        User(...)
    } else {
        null
    }
}

// Update
databaseHelper.executeUpdate(
    "UPDATE users SET display_name = ? WHERE id = ?",
    newName,
    userId
)

// Transação
databaseHelper.executeTransaction { connection ->
    connection.prepareStatement("UPDATE users SET partner_id = ? WHERE id = ?").use { stmt ->
        stmt.setString(1, partnerId)
        stmt.setString(2, userId)
        stmt.executeUpdate()
    }

    connection.prepareStatement("UPDATE users SET partner_id = ? WHERE id = ?").use { stmt ->
        stmt.setString(1, userId)
        stmt.setString(2, partnerId)
        stmt.executeUpdate()
    }
}
```

---

## Widget Layer

### PhotoWidget
**Arquivo**: `app/src/main/java/com/vivacomigo/app/widget/PhotoWidget.kt`

**Responsabilidade**: UI do widget Glance.

**Estrutura**:
```kotlin
class PhotoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            PhotoWidgetContent(context)
        }
    }
}

@Composable
fun PhotoWidgetContent(context: Context) {
    val photoId = PhotoWidgetDataStore.getPhotoId(context)

    if (photoId != null) {
        val bitmap = loadPhotoFromCache(context, photoId)

        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Foto recebida",
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("❤️", style = TextStyle(fontSize = 48.sp))
        }
    } else {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text("❤️", style = TextStyle(fontSize = 48.sp))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text("Viva Comigo")
        }
    }
}
```

**Como Modificar**:
```kotlin
// Adicionar botão ao widget
Button(
    text = "Atualizar",
    onClick = actionRunCallback<UpdateWidgetCallback>()
)

// Callback
class UpdateWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId) {
        PhotoWidgetWorker.enqueue(context)
    }
}
```

---

### PhotoWidgetWorker
**Arquivo**: `app/src/main/java/com/vivacomigo/app/widget/PhotoWidgetWorker.kt`

**Responsabilidade**: Sincronização em background.

**Implementação**:
```kotlin
class PhotoWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val authRepository = AuthRepository(applicationContext)
            val photoRepository = PhotoRepository(DatabaseHelper, ImageHelper)

            // Obter userId
            val userIdResult = authRepository.ensureLocalUser()
            val userId = userIdResult.getOrNull() ?: return Result.failure()

            // Buscar última foto
            val photoResult = photoRepository.getLatestPhotoForUser(userId)
            val photo = photoResult.getOrNull()

            if (photo != null) {
                // Salvar photoId
                PhotoWidgetDataStore.savePhotoId(applicationContext, photo.id)

                // Atualizar widget
                GlanceAppWidgetManager(applicationContext).getGlanceIds(PhotoWidget::class.java)
                    .forEach { glanceId ->
                        PhotoWidget().update(applicationContext, glanceId)
                    }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in widget worker", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<PhotoWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
```

**Agendamento**:
```kotlin
// app/src/main/java/com/vivacomigo/app/VivaApp.kt
private fun scheduleWidgetUpdates() {
    val workRequest = PeriodicWorkRequestBuilder<PhotoWidgetWorker>(
        30, TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "widget_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
```

---

# Backend

## Controllers

### authController
**Arquivo**: `backend/src/controllers/authController.js`

**Responsabilidade**: Registro e login de usuários.

**Funções**:

#### register
```javascript
const register = async (req, res) => {
    try {
        const { email, password, display_name } = req.body;

        // Validação
        if (!email || !password) {
            return res.status(400).json({
                error: 'Email and password required'
            });
        }

        // Verificar se email já existe
        const [existing] = await pool.execute(
            'SELECT id FROM users WHERE email = ?',
            [email]
        );

        if (existing.length > 0) {
            return res.status(409).json({
                error: 'Email already registered'
            });
        }

        // Hash da senha
        const password_hash = await bcrypt.hash(password, 10);

        // Gerar código de pareamento
        const pairing_code = generatePairingCode();

        // Criar usuário
        const userId = uuidv4();

        await pool.execute(
            `INSERT INTO users (id, email, password_hash, pairing_code, display_name, created_at)
             VALUES (?, ?, ?, ?, ?, NOW())`,
            [userId, email, password_hash, pairing_code, display_name || 'Usuário']
        );

        // Gerar JWT
        const token = jwt.sign(
            { userId, email },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
        );

        res.status(201).json({
            token,
            user: { id: userId, email, pairing_code, display_name }
        });
    } catch (error) {
        console.error('[authController] Register error:', error);
        res.status(500).json({ error: error.message });
    }
};
```

#### login
```javascript
const login = async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({
                error: 'Email and password required'
            });
        }

        // Buscar usuário
        const [rows] = await pool.execute(
            'SELECT id, email, password_hash, pairing_code, display_name FROM users WHERE email = ?',
            [email]
        );

        if (rows.length === 0) {
            return res.status(401).json({
                error: 'Invalid credentials'
            });
        }

        const user = rows[0];

        // Verificar senha
        const isValid = await bcrypt.compare(password, user.password_hash);

        if (!isValid) {
            return res.status(401).json({
                error: 'Invalid credentials'
            });
        }

        // Gerar JWT
        const token = jwt.sign(
            { userId: user.id, email: user.email },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
        );

        res.json({
            token,
            user: {
                id: user.id,
                email: user.email,
                pairing_code: user.pairing_code,
                display_name: user.display_name
            }
        });
    } catch (error) {
        console.error('[authController] Login error:', error);
        res.status(500).json({ error: error.message });
    }
};
```

---

## Routes

### auth.js
**Arquivo**: `backend/src/routes/auth.js`

```javascript
const express = require('express');
const router = express.Router();
const { register, login } = require('../controllers/authController');

router.post('/register', register);
router.post('/login', login);

module.exports = router;
```

### users.js
**Arquivo**: `backend/src/routes/users.js`

```javascript
const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/authMiddleware');
const {
    getCurrentUser,
    getUserById,
    pairUsers,
    unpairUsers
} = require('../controllers/userController');

router.get('/me', authMiddleware, getCurrentUser);
router.get('/:id', authMiddleware, getUserById);
router.post('/pair', authMiddleware, pairUsers);
router.post('/unpair', authMiddleware, unpairUsers);

module.exports = router;
```

---

## Middleware

### authMiddleware
**Arquivo**: `backend/src/middleware/authMiddleware.js`

```javascript
const jwt = require('jsonwebtoken');

const authMiddleware = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;

        if (!authHeader) {
            return res.status(401).json({
                error: 'No authorization header'
            });
        }

        const token = authHeader.split(' ')[1]; // Bearer TOKEN

        if (!token) {
            return res.status(401).json({
                error: 'No token provided'
            });
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        req.userId = decoded.userId;
        req.email = decoded.email;

        next();
    } catch (error) {
        if (error.name === 'TokenExpiredError') {
            return res.status(401).json({
                error: 'Token expired'
            });
        }

        return res.status(401).json({
            error: 'Invalid token'
        });
    }
};

module.exports = authMiddleware;
```

---

Este documento fornece visão completa de todos os componentes do sistema. Para informações arquiteturais, consulte [ARCHITECTURE.md](.claude/ARCHITECTURE.md).
