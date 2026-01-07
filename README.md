# ❤️ Viva Comigo

**Viva Comigo** é um aplicativo Android que permite compartilhar fotos especiais com alguém que você ama através de um widget na tela inicial.

> 🤖 **Para IA**: Antes de modificar qualquer código, leia [`AI_CONTEXT.md`](AI_CONTEXT.md) para entender a arquitetura e padrões do projeto!

## 📱 Funcionalidades

- 👥 **Sistema de pareamento** simples com código de 6 dígitos (sem necessidade de login)
- 📸 **Envio de fotos** diretamente para o widget do parceiro
- 🖼️ **Widget na tela inicial** que exibe a última foto recebida em tempo real
- 🔄 **Sincronização automática** via polling (30s) e background sync (30min)
- 💾 **Armazenamento** em MySQL remoto com imagens em BLOB

## 🏗️ Tecnologias

### Android App
- **Kotlin 1.9.20** - Linguagem de programação
- **Jetpack Compose** - UI declarativa moderna com Material 3
- **MVVM** - Arquitetura Model-View-ViewModel
- **Glance** - Framework para Widgets modernos
- **Coil** - Carregamento de imagens assíncrono
- **WorkManager** - Sincronização em background
- **StateFlow** - Gerenciamento de estado reativo
- **MySQL JDBC** - Conexão direta ao banco de dados
- **DataStore** - Cache local de usuário

### Backend (Opcional)
- **Node.js + Express** - API REST
- **JWT** - Autenticação
- **MySQL2** - Driver MySQL para Node.js
- **Multer** - Upload de arquivos

### Banco de Dados
- **MySQL 8** - Banco de dados relacional
- **Tabelas**: `users`, `photos`
- **Storage**: BLOB para imagens (até 10MB)

## 📚 Documentação

### Para Desenvolvedores (Humanos)
- **[SETUP.md](SETUP.md)** - Guia completo de instalação e configuração
- **[README.md](README.md)** - Este arquivo (visão geral do projeto)

### Para IA/Agentes de Código
- **[AI_CONTEXT.md](AI_CONTEXT.md)** - ⭐ **LEIA PRIMEIRO!** Contexto rápido do projeto
- **[.claude/ARCHITECTURE.md](.claude/ARCHITECTURE.md)** - Arquitetura detalhada, fluxos e diagramas
- **[.claude/CODE_GUIDE.md](.claude/CODE_GUIDE.md)** - Guia prático de como modificar código
- **[.claude/COMPONENTS.md](.claude/COMPONENTS.md)** - Documentação de cada componente
- **[.claude/DATABASE.md](.claude/DATABASE.md)** - Schema do banco e queries comuns

## 🚀 Quick Start

### Pré-requisitos
- Android Studio Hedgehog ou superior
- JDK 11 ou superior
- MySQL 8 (local ou remoto)
- Node.js 18+ (opcional, para backend)

### Instalação Rápida

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/Gabriel-KleinL/Widget-para-compartilhar-fotos.git
   cd Widget-para-compartilhar-fotos
   ```

2. **Configure o banco de dados:**
   ```bash
   cd backend
   mysql -u root -p < database/schema.sql
   ```

3. **Configure as credenciais:**

   Edite `app/src/main/java/com/vivacomigo/app/data/database/DatabaseConfig.kt`:
   ```kotlin
   const val HOST = "seu-host-mysql"
   const val PORT = 3306
   const val USER = "seu-usuario"
   const val PASSWORD = "sua-senha"
   const val DATABASE = "nome-do-banco"
   ```

4. **Build e execute:**
   ```bash
   ./gradlew installDebug
   ```

Para instruções detalhadas, consulte [SETUP.md](SETUP.md).

## 📖 Como Usar

### 1. Pareamento

**No dispositivo 1:**
```
Abrir app → Ver código ABC123
```

**No dispositivo 2:**
```
Abrir app → Inserir código ABC123 → Parear
```

Ambos ficam conectados instantaneamente! ❤️

### 2. Enviar Foto

```
Tela principal → Botão "+" → Selecionar foto → Enviar
```

A foto aparece automaticamente no widget do parceiro!

### 3. Adicionar Widget

```
Pressionar e segurar tela inicial → Widgets → "Viva Comigo" → Arrastar
```

O widget mostrará a última foto recebida ou um ❤️ emoji se não houver foto.

## 🔧 Arquitetura

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  MainActivity, HomeScreen, PairingScreen│
└──────────────┬──────────────────────────┘
               │ observa StateFlow
               ↓
┌─────────────────────────────────────────┐
│      ViewModel Layer (State Mgmt)       │
│          MainViewModel                  │
└──────────────┬──────────────────────────┘
               │ chama repositories
               ↓
┌─────────────────────────────────────────┐
│      Repository Layer (Data Access)     │
│  AuthRepo, UserRepo, PhotoRepo          │
└──────────────┬──────────────────────────┘
               │ executa queries
               ↓
┌─────────────────────────────────────────┐
│       Data Layer (JDBC Pool)            │
│       DatabaseHelper                    │
└──────────────┬──────────────────────────┘
               │ JDBC
               ↓
┌─────────────────────────────────────────┐
│        MySQL Database (Remoto)          │
│    Tables: users, photos                │
└─────────────────────────────────────────┘
```

Para detalhes completos, veja [.claude/ARCHITECTURE.md](.claude/ARCHITECTURE.md).

## 🗂️ Estrutura do Projeto

```
Widget-para-compartilhar-fotos/
├── .claude/                     # 📚 Documentação para IA
│   ├── ARCHITECTURE.md          # Arquitetura detalhada
│   ├── CODE_GUIDE.md            # Guia de código
│   ├── COMPONENTS.md            # Docs de componentes
│   └── DATABASE.md              # Schema e queries
│
├── app/                         # 📱 Aplicativo Android
│   └── src/main/
│       ├── java/com/vivacomigo/app/
│       │   ├── data/
│       │   │   ├── database/    # DatabaseHelper, Config
│       │   │   ├── model/       # User, Photo
│       │   │   └── repository/  # Auth, User, Photo repos
│       │   ├── ui/
│       │   │   ├── screen/      # HomeScreen, PairingScreen
│       │   │   ├── theme/       # Tema Material 3
│       │   │   └── viewmodel/   # MainViewModel
│       │   ├── widget/          # PhotoWidget, Worker
│       │   ├── MainActivity.kt
│       │   └── VivaApp.kt
│       └── res/                 # Recursos (strings, cores, etc)
│
├── backend/                     # 🖥️ Backend Node.js (opcional)
│   ├── src/
│   │   ├── controllers/         # Lógica de negócio
│   │   ├── routes/              # Rotas HTTP
│   │   ├── middleware/          # AuthMiddleware
│   │   └── server.js            # Express app
│   └── database/
│       └── schema.sql           # Schema MySQL
│
├── AI_CONTEXT.md                # 🤖 Contexto rápido para IA
├── README.md                    # Este arquivo
├── SETUP.md                     # Guia de setup detalhado
└── build.gradle.kts             # Config Gradle
```

## 🔐 Segurança

### ⚠️ Avisos Importantes

1. **Credenciais hardcoded**: O arquivo `DatabaseConfig.kt` contém credenciais em texto claro. **Não compartilhe o APK publicamente!**

2. **Conexão sem SSL**: Atualmente `USE_SSL = false`. Em produção, habilite SSL/TLS.

3. **BLOB não criptografado**: Fotos são armazenadas sem criptografia. Considere AES-256 para produção.

### Melhorias Recomendadas para Produção

- [ ] Migrar credenciais para BuildConfig ou NDK
- [ ] Usar API REST ao invés de JDBC direto
- [ ] Habilitar SSL para conexões MySQL
- [ ] Implementar criptografia de BLOB
- [ ] Adicionar autenticação biométrica
- [ ] Implementar Firebase Cloud Messaging (substituir polling)

Veja mais em [.claude/ARCHITECTURE.md → Segurança](.claude/ARCHITECTURE.md#segurança).

## 🛠️ Comandos Úteis

```bash
# Build e instalar debug
./gradlew installDebug

# Ver logs do app
adb logcat | grep "PhotoRepository"

# Executar backend (opcional)
cd backend && npm start

# Conectar ao MySQL
mysql -h srv1965.hstgr.io -u usuario -p

# Backup do banco
mysqldump -h host -u user -p database > backup.sql
```

## 🧪 Desenvolvimento

### Adicionar Nova Funcionalidade

1. **Leia a documentação relevante:**
   - [AI_CONTEXT.md](AI_CONTEXT.md) - Contexto geral
   - [.claude/CODE_GUIDE.md](.claude/CODE_GUIDE.md) - Como fazer mudanças

2. **Identifique os componentes a modificar:**
   - Consulte [.claude/COMPONENTS.md](.claude/COMPONENTS.md)

3. **Siga os padrões de código:**
   - Use `suspend fun` para operações assíncronas
   - Retorne `Result<T>` dos repositories
   - Atualize `StateFlow` no ViewModel
   - Componha UI com Composables puros

4. **Teste e faça commit:**
   ```bash
   ./gradlew assembleDebug
   git add .
   git commit -m "feat: descrição da mudança"
   ```

### Exemplos de Mudanças Comuns

- **Adicionar campo ao User**: Ver [CODE_GUIDE.md → Mudanças Comuns #1](.claude/CODE_GUIDE.md#1-adicionar-um-novo-campo-ao-user)
- **Criar nova tela**: Ver [CODE_GUIDE.md → Mudanças Comuns #2](.claude/CODE_GUIDE.md#2-adicionar-uma-nova-tela)
- **Modificar widget**: Ver [CODE_GUIDE.md → Mudanças Comuns #4](.claude/CODE_GUIDE.md#4-modificar-o-widget)

## 📊 Banco de Dados

### Schema Simplificado

**users**
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,        -- UUID
    pairing_code VARCHAR(6) UNIQUE,    -- Código de pareamento
    partner_id VARCHAR(36),            -- ID do parceiro
    display_name VARCHAR(255)
);
```

**photos**
```sql
CREATE TABLE photos (
    id VARCHAR(36) PRIMARY KEY,        -- UUID
    sender_id VARCHAR(36),             -- Quem enviou
    receiver_id VARCHAR(36),           -- Quem recebe
    image_data LONGBLOB,               -- Foto (BLOB até 10MB)
    timestamp BIGINT                   -- Quando foi enviada
);
```

Para schema completo e queries, veja [.claude/DATABASE.md](.claude/DATABASE.md).

## 🐛 Troubleshooting

### App não conecta ao MySQL
- Verifique credenciais em `DatabaseConfig.kt`
- Teste conexão: `mysql -h host -u user -p`
- Veja [DATABASE.md → Troubleshooting](.claude/DATABASE.md#troubleshooting)

### Widget não atualiza
- Verifique se WorkManager está agendado
- Force atualização: `viewModel.updateWidget()`
- Veja [CODE_GUIDE.md → Debugging](.claude/CODE_GUIDE.md#2-widget-não-atualiza)

### Foto não aparece após envio
- Verifique tamanho (máx 10MB)
- Verifique permissões de leitura
- Veja logs: `adb logcat | grep PhotoRepository`

## 🤝 Contribuindo

Contribuições são bem-vindas! Para contribuir:

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/nova-feature`
3. Commit: `git commit -m 'feat: adiciona nova feature'`
4. Push: `git push origin feature/nova-feature`
5. Abra um Pull Request

### Diretrizes
- Siga os padrões de código em [CODE_GUIDE.md](.claude/CODE_GUIDE.md)
- Adicione testes (quando disponível)
- Atualize documentação se necessário
- Use commits semânticos (feat, fix, docs, refactor, etc)

## 📝 Licença

Este projeto é de código aberto e está disponível para uso educacional.

## 👨‍💻 Autor

Desenvolvido com ❤️ por [Gabriel Klein](https://github.com/Gabriel-KleinL)

## 🙏 Agradecimentos

- **Jetpack Compose** pela UI moderna
- **Glance** pelo framework de widgets
- **Comunidade Android** pelas melhores práticas

---

## 📖 Índice de Documentação

| Documento | Público-Alvo | Quando Ler |
|-----------|--------------|------------|
| [README.md](README.md) | Todos | Visão geral do projeto |
| [SETUP.md](SETUP.md) | Desenvolvedores | Ao configurar ambiente |
| [AI_CONTEXT.md](AI_CONTEXT.md) | IA/Agentes | **Antes de qualquer mudança** |
| [.claude/ARCHITECTURE.md](.claude/ARCHITECTURE.md) | IA/Dev | Entender arquitetura |
| [.claude/CODE_GUIDE.md](.claude/CODE_GUIDE.md) | IA/Dev | Ao fazer mudanças |
| [.claude/COMPONENTS.md](.claude/COMPONENTS.md) | IA/Dev | Ao modificar componentes |
| [.claude/DATABASE.md](.claude/DATABASE.md) | IA/Dev | Ao trabalhar com banco |

---

**Nota**: Este projeto usa MySQL JDBC direto do Android. Para produção, considere migrar para API REST. Veja [ARCHITECTURE.md → Limitações](.claude/ARCHITECTURE.md#limitações-conhecidas).
