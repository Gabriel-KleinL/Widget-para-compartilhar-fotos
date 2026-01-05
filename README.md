# ❤️ Viva Comigo

**Viva Comigo** é um aplicativo Android que permite compartilhar fotos especiais com alguém que você ama através de um widget na tela inicial.

## 📱 Funcionalidades

- 🔐 **Autenticação segura** com Firebase Authentication
- 👥 **Sistema de pareamento** simples com código de 6 dígitos
- 📸 **Envio de fotos** diretamente para o widget do parceiro
- 🖼️ **Widget na tela inicial** que exibe a última foto recebida
- 🔄 **Sincronização automática** em tempo real
- 💾 **Armazenamento seguro** no Firebase Storage

## 🏗️ Arquitetura

O app utiliza as seguintes tecnologias modernas do Android:

- **Kotlin** - Linguagem de programação
- **Jetpack Compose** - UI declarativa moderna
- **Material 3** - Design system
- **Firebase**:
  - Authentication - Autenticação de usuários
  - Firestore - Banco de dados em tempo real
  - Storage - Armazenamento de imagens
- **Glance** - Framework para Widgets
- **Coil** - Carregamento de imagens
- **WorkManager** - Sincronização em background
- **Coroutines & Flow** - Programação assíncrona

## 🚀 Como configurar

### 1. Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 8 ou superior
- Conta no Firebase

### 2. Configurar o Firebase

1. Acesse o [Firebase Console](https://console.firebase.google.com/)
2. Crie um novo projeto ou use um existente
3. Adicione um app Android ao projeto:
   - Package name: `com.vivacomigo.app`
   - Download do arquivo `google-services.json`
4. Coloque o arquivo `google-services.json` na pasta `app/`

5. No Firebase Console, ative os seguintes serviços:

   **Authentication:**
   - Acesse "Authentication" > "Sign-in method"
   - Ative "Email/Password"

   **Firestore Database:**
   - Acesse "Firestore Database"
   - Crie um banco de dados em modo "production"
   - Configure as regras de segurança:

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
         allow read: if request.auth != null;
       }

       match /photos/{photoId} {
         allow create: if request.auth != null;
         allow read: if request.auth != null &&
           (resource.data.senderId == request.auth.uid ||
            resource.data.receiverId == request.auth.uid);
       }
     }
   }
   ```

   **Storage:**
   - Acesse "Storage"
   - Configure as regras de segurança:

   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /photos/{photoId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null;
       }
     }
   }
   ```

### 3. Compilar e executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/Gabriel-KleinL/Widget-para-compartilhar-fotos.git
   cd Widget-para-compartilhar-fotos
   ```

2. Abra o projeto no Android Studio

3. Certifique-se de que o arquivo `google-services.json` está em `app/`

4. Sincronize o Gradle (Build > Sync Project with Gradle Files)

5. Execute o app em um dispositivo ou emulador Android (API 26+)

## 📖 Como usar

1. **Registro/Login:**
   - Abra o app e crie uma conta com email e senha
   - Ou faça login se já tiver uma conta

2. **Pareamento:**
   - Após o login, você verá seu código de pareamento de 6 dígitos
   - Compartilhe este código com seu parceiro
   - Digite o código do seu parceiro para conectar

3. **Enviar fotos:**
   - Na tela principal, toque no botão "+"
   - Selecione uma foto da galeria
   - A foto será enviada para o widget do seu parceiro

4. **Widget:**
   - Pressione e segure na tela inicial do Android
   - Toque em "Widgets"
   - Encontre "Viva Comigo - Foto Compartilhada"
   - Arraste para a tela inicial
   - O widget mostrará a última foto recebida

## 🔧 Estrutura do projeto

```
app/
├── src/main/
│   ├── java/com/vivacomigo/app/
│   │   ├── data/
│   │   │   ├── model/          # Modelos de dados (User, Photo)
│   │   │   └── repository/     # Repositórios Firebase
│   │   ├── ui/
│   │   │   ├── screen/         # Telas Compose
│   │   │   ├── theme/          # Tema do app
│   │   │   └── viewmodel/      # ViewModels
│   │   ├── widget/             # Widget e sincronização
│   │   ├── MainActivity.kt
│   │   └── VivaApp.kt
│   ├── res/                    # Recursos (layouts, strings, etc)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🛡️ Segurança

- Todas as imagens são armazenadas de forma segura no Firebase Storage
- Regras do Firestore garantem que apenas usuários autenticados possam acessar seus dados
- Cada usuário só pode ver fotos enviadas para ele ou por ele
- Senhas são gerenciadas pelo Firebase Authentication

## 📝 Licença

Este projeto é de código aberto e está disponível para uso educacional.

## 🤝 Contribuições

Contribuições são bem-vindas! Sinta-se à vontade para:
- Reportar bugs
- Sugerir novas funcionalidades
- Enviar pull requests

## 👨‍💻 Desenvolvimento

Desenvolvido com ❤️ usando as melhores práticas do Android moderno.

---

**Nota:** Este aplicativo requer o arquivo `google-services.json` do Firebase para funcionar. Siga as instruções de configuração acima para obter este arquivo.
