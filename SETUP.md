# Guia de Configuração - Viva Comigo

Este guia vai te ajudar a configurar e executar o projeto após a migração do Firebase para MySQL.

## 📋 Pré-requisitos

### Backend
- Node.js 18+ instalado
- npm ou yarn
- Acesso ao MySQL (credenciais já configuradas)

### Android
- Android Studio Hedgehog ou superior
- JDK 8 ou superior
- Dispositivo Android ou Emulador (API 26+)

## 🚀 Passo a Passo

### 1. Configurar o Banco de Dados MySQL

Execute o script SQL para criar as tabelas:

```bash
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < backend/database/schema.sql
```

Quando solicitado, digite a senha: `W!M$EL?y6`

### 2. Configurar o Backend

1. Entre na pasta do backend:
```bash
cd backend
```

2. Instale as dependências:
```bash
npm install
```

3. O arquivo `.env` já está configurado com suas credenciais. Se precisar alterar, edite o arquivo:
```bash
# As credenciais já estão configuradas, mas você pode alterar se necessário
```

4. Inicie o servidor:
```bash
npm run dev
```

O servidor estará rodando em `http://localhost:3000`

**Teste se está funcionando:**
```bash
curl http://localhost:3000/health
```

Deve retornar: `{"status":"OK","message":"API Viva Comigo está funcionando"}`

### 3. Configurar o App Android

1. **Atualizar a URL da API**

Abra o arquivo `app/src/main/java/com/vivacomigo/app/data/network/ApiConfig.kt`

Para **emulador Android**, use:
```kotlin
const val BASE_URL = "http://10.0.2.2:3000/api"
```

Para **dispositivo físico**, você precisa:
1. Descobrir o IP da sua máquina na rede local:
   - Linux/Mac: `ifconfig` ou `ip addr`
   - Windows: `ipconfig`
2. Atualizar para o IP encontrado:
```kotlin
const val BASE_URL = "http://192.168.1.X:3000/api" // Substitua X pelo seu IP
```

**Importante:** Certifique-se de que o dispositivo Android e o computador estão na mesma rede Wi-Fi.

2. **Sincronizar o Gradle**

No Android Studio:
- Clique em `File > Sync Project with Gradle Files`
- Aguarde a sincronização terminar

3. **Compilar e Executar**

- Conecte um dispositivo ou inicie um emulador
- Clique em `Run > Run 'app'` ou pressione `Shift+F10`

## 🧪 Testando

### 1. Testar o Backend

Você pode testar os endpoints usando curl ou Postman:

**Registrar usuário:**
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@teste.com","password":"senha123","display_name":"Teste"}'
```

**Login:**
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@teste.com","password":"senha123"}'
```

### 2. Testar o App

1. Abra o app no dispositivo/emulador
2. Crie uma conta com email e senha
3. Anote o código de pareamento exibido
4. Em outro dispositivo (ou outra conta), faça login e use o código para parear
5. Envie uma foto e verifique se aparece no widget

## 🔧 Troubleshooting

### Backend não conecta ao MySQL
- Verifique se as credenciais no `.env` estão corretas
- Teste a conexão manualmente: `mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p`

### App não conecta ao backend
- Verifique se o servidor está rodando: `curl http://localhost:3000/health`
- Verifique a URL em `ApiConfig.kt`
- Para dispositivo físico, confirme que está na mesma rede Wi-Fi
- Verifique o firewall do computador (pode estar bloqueando a porta 3000)

### Erro de autenticação
- Verifique se o token está sendo salvo corretamente
- Limpe os dados do app: `Settings > Apps > Viva Comigo > Clear Data`

### Erro ao fazer upload de foto
- Verifique o tamanho da imagem (máximo 10MB)
- Verifique se o usuário está pareado com outro usuário
- Verifique os logs do backend para mais detalhes

## 📝 Próximos Passos

Após configurar tudo:

1. ✅ Testar registro e login
2. ✅ Testar pareamento de usuários
3. ✅ Testar envio de fotos
4. ✅ Testar widget na tela inicial
5. ✅ Verificar sincronização automática (polling a cada 30 segundos)

## 🚀 Deploy em Produção

Quando estiver pronto para produção:

1. Configure um servidor (VPS, AWS, etc.)
2. Instale Node.js e MySQL no servidor
3. Configure variáveis de ambiente de produção
4. Use PM2 ou similar para manter o servidor rodando
5. Configure HTTPS (Let's Encrypt)
6. Atualize `ApiConfig.kt` com a URL de produção
7. Configure domínio e DNS

## 📞 Suporte

Se encontrar problemas:
1. Verifique os logs do backend no terminal
2. Verifique o Logcat no Android Studio
3. Teste os endpoints manualmente com curl/Postman

