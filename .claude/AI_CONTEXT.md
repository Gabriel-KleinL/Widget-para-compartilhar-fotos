# Contexto Rápido para IA - Viva Comigo

> **Leia este arquivo PRIMEIRO** antes de fazer qualquer modificação no código!

## O Que É Este Projeto?

**Viva Comigo** é um aplicativo Android que permite casais compartilharem fotos através de um widget na tela inicial. Quando um parceiro envia uma foto, ela aparece automaticamente no widget do outro.

## Stack Tecnológico

```
┌─────────────────────────────────────┐
│  Android App (Kotlin + Compose)    │
│  - Jetpack Compose (UI)            │
│  - Material 3                      │
│  - Glance Widget                   │
│  - WorkManager (background)        │
└────────────┬────────────────────────┘
             │ JDBC direto
             ↓
┌─────────────────────────────────────┐
│  MySQL Database (Remoto)           │
│  - srv1965.hstgr.io:3306           │
│  - Tabelas: users, photos          │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Backend Node.js (Parcialmente)    │
│  - Express + JWT                   │
│  - Atualmente pouco usado          │
└─────────────────────────────────────┘
```

## Arquitetura do Android (MVVM)

```
UI (Compose) → ViewModel (StateFlow) → Repository → DatabaseHelper (JDBC) → MySQL
```

## Arquivos Mais Importantes

### ⭐ Leia SEMPRE antes de modificar:

| Arquivo | O Que Faz | Quando Modificar |
|---------|-----------|------------------|
| `.claude/ARCHITECTURE.md` | Arquitetura completa do sistema | Ao entender fluxos, adicionar features |
| `.claude/CODE_GUIDE.md` | Guia prático de como modificar código | Ao fazer mudanças específicas |
| `.claude/COMPONENTS.md` | Documentação detalhada de componentes | Ao modificar componentes específicos |
| `.claude/DATABASE.md` | Schema e queries do MySQL | Ao trabalhar com banco de dados |

### 🔧 Código Principal:

| Arquivo | Responsabilidade |
|---------|------------------|
| `app/.../MainActivity.kt` | Navegação entre telas |
| `app/.../MainViewModel.kt` | Estado centralizado, lógica de negócio |
| `app/.../HomeScreen.kt` | Tela principal (foto + enviar) |
| `app/.../PairingScreen.kt` | Tela de pareamento |
| `app/.../PhotoRepository.kt` | Upload/download de fotos |
| `app/.../DatabaseHelper.kt` | Pool JDBC, transações |
| `app/.../PhotoWidget.kt` | Widget Glance |

## Como Funciona?

### 1. Pareamento
```
User A abre app → Vê código ABC123
User B abre app → Insere código ABC123
→ Ambos ficam pareados no MySQL
```

### 2. Envio de Foto
```
User A seleciona foto → Upload para MySQL como BLOB
→ Widget de User B atualiza automaticamente
```

### 3. Sincronização
- **Foreground**: Polling a cada 30 segundos (MainViewModel)
- **Background**: WorkManager a cada 30 minutos (PhotoWidgetWorker)

## Banco de Dados (MySQL)

### users
```sql
id (UUID) | email | pairing_code (6 chars) | partner_id | display_name
```

### photos
```sql
id (UUID) | sender_id | receiver_id | image_data (BLOB) | timestamp
```

**CRÍTICO**: Imagens são armazenadas como BLOB binário (até 10MB)

## Estado da Aplicação (MainViewModel)

```kotlin
data class MainUiState(
    val authState: AuthState,        // PAIRED ou UNPAIRED
    val currentUser: User?,
    val partner: User?,
    val latestPhoto: Photo?,
    val isLoading: Boolean,
    val error: String?
)
```

## Padrões de Código

### ✅ CORRETO
```kotlin
// Repository com Result e suspend
suspend fun uploadPhoto(...): Result<Photo> = withContext(Dispatchers.IO) {
    try {
        // lógica
        Result.success(photo)
    } catch (e: Exception) {
        Log.e(TAG, "Error", e)
        Result.failure(e)
    }
}

// ViewModel com StateFlow
fun sendPhoto(uri: Uri) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        // lógica
    }
}

// Composable puro
@Composable
fun HomeScreen(uiState: MainUiState, onSendPhoto: (Uri) -> Unit) {
    // UI
}
```

### ❌ INCORRETO
```kotlin
// Bloquear thread principal
fun uploadPhoto(): Photo {
    return photo // ❌ Sem suspend
}

// Mutação direta de estado
viewModel.latestPhoto = photo // ❌ Sem StateFlow

// ViewModel no Composable
@Composable
fun HomeScreen(viewModel: MainViewModel) { } // ❌ Acoplamento
```

## Checklist para Mudanças

Antes de modificar qualquer código:

- [ ] Li `.claude/ARCHITECTURE.md` ou `.claude/CODE_GUIDE.md`?
- [ ] Entendi o fluxo que vou modificar?
- [ ] Sei qual componente é responsável por essa funcionalidade?
- [ ] Tenho o schema do banco (se for mexer em dados)?

Antes de fazer commit:

- [ ] Código compila sem erros?
- [ ] Adicionei logs apropriados?
- [ ] Tratei erros (try/catch ou Result)?
- [ ] Atualizei StateFlow (se mudança de estado)?
- [ ] Não hardcodei valores (usei constantes)?

## Comandos Rápidos

```bash
# Build e instalar
./gradlew installDebug

# Ver logs
adb logcat | grep "PhotoRepository"

# Executar backend (se necessário)
cd backend && npm start

# Conectar ao MySQL
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p
```

## ⚠️ Avisos Importantes

### Credenciais Hardcoded
```kotlin
// app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt
const val PASSWORD = "W!M$EL?y6"  // ⚠️ HARDCODED!
```

**NUNCA** compartilhe APK publicamente. Considere usar BuildConfig em produção.

### Conexão Direta ao MySQL
O app Android conecta **diretamente** ao MySQL via JDBC. Isso:
- ✅ Simplifica desenvolvimento
- ❌ Expõe credenciais no APK
- ❌ Dificulta escalabilidade

**Recomendação para produção**: Migrar para API REST via backend Node.js

### Polling vs Push
- **Atual**: Polling a cada 30s (consome bateria)
- **Ideal**: Firebase Cloud Messaging (push notifications)

## Como Adicionar Features Comuns

### Adicionar campo ao User
1. `ALTER TABLE users ADD COLUMN phone VARCHAR(20);`
2. Atualizar `User.kt` data class
3. Atualizar queries em `UserRepository.kt`

### Adicionar nova tela
1. Criar `NewScreen.kt` Composable
2. Adicionar flag ao `MainUiState`
3. Atualizar navegação em `MainActivity.kt`

### Adicionar endpoint na API
1. Criar função em controller (`backend/src/controllers/`)
2. Adicionar rota (`backend/src/routes/`)
3. Adicionar função no repository Android

## Fluxo de Desenvolvimento

```
1. Entender requisito
   ↓
2. Ler documentação relevante (.claude/*.md)
   ↓
3. Identificar componentes a modificar
   ↓
4. Fazer mudanças seguindo padrões
   ↓
5. Testar (build + adb logcat)
   ↓
6. Commit com mensagem clara
```

## Estrutura de Pastas

```
app/src/main/java/com/vivacomigo/app/
├── MainActivity.kt               # PONTO DE ENTRADA
├── VivaApp.kt                    # Application class
├── data/
│   ├── database/                 # DatabaseHelper, Config
│   ├── model/                    # User, Photo
│   └── repository/               # Auth, User, Photo repos
├── ui/
│   ├── screen/                   # HomeScreen, PairingScreen
│   ├── theme/                    # Tema Material 3
│   └── viewmodel/                # MainViewModel
└── widget/                       # PhotoWidget, Worker

backend/src/
├── server.js                     # Express app
├── controllers/                  # Lógica de negócio
├── routes/                       # Rotas HTTP
└── middleware/                   # AuthMiddleware (JWT)
```

## Documentação Completa

Para informações detalhadas, consulte:

- **Arquitetura**: `.claude/ARCHITECTURE.md`
- **Guia de Código**: `.claude/CODE_GUIDE.md`
- **Componentes**: `.claude/COMPONENTS.md`
- **Banco de Dados**: `.claude/DATABASE.md`
- **Setup**: `SETUP.md`
- **README**: `README.md`

## Onde Buscar Informações

| Pergunta | Onde Encontrar |
|----------|----------------|
| Como funciona o pareamento? | `.claude/ARCHITECTURE.md` → Fluxo 2 |
| Como adicionar campo ao User? | `.claude/CODE_GUIDE.md` → Mudanças Comuns #1 |
| Qual query busca última foto? | `.claude/DATABASE.md` → Queries Comuns #2 |
| Como funciona o MainViewModel? | `.claude/COMPONENTS.md` → ViewModel Layer |
| Como modificar o widget? | `.claude/CODE_GUIDE.md` → Mudanças Comuns #4 |

---

**Princípio Fundamental**: Sempre leia a documentação ANTES de fazer mudanças. Economiza tempo e previne bugs! 🚀
