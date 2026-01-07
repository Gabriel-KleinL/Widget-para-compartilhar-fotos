# Backend API - Viva Comigo

Backend Node.js/Express para o aplicativo Viva Comigo.

## Pré-requisitos

- Node.js 18+ 
- npm ou yarn
- MySQL 8.0+

## Instalação

1. Instale as dependências:
```bash
npm install
```

2. Configure as variáveis de ambiente:
```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas credenciais do MySQL:
```
DB_HOST=srv1965.hstgr.io
DB_PORT=3306
DB_USER=u466620993_gabrielklein24
DB_PASSWORD=W!M$EL?y6
DB_NAME=u466620993_poker

JWT_SECRET=seu_jwt_secret_super_seguro_aqui
JWT_EXPIRES_IN=7d

PORT=3000
```

3. Execute o script SQL para criar as tabelas:
```bash
mysql -h srv1965.hstgr.io -u u466620993_gabrielklein24 -p u466620993_poker < database/schema.sql
```

## Executar

### Desenvolvimento
```bash
npm run dev
```

### Produção
```bash
npm start
```

O servidor estará rodando em `http://localhost:3000`

## Endpoints

### Autenticação
- `POST /api/auth/register` - Registrar novo usuário
- `POST /api/auth/login` - Fazer login

### Usuários
- `GET /api/users/me` - Obter usuário atual (requer autenticação)
- `GET /api/users/:id` - Obter usuário por ID (requer autenticação)
- `GET /api/users/pairing-code/:code` - Buscar usuário por código de pareamento (requer autenticação)
- `POST /api/users/pair` - Parear usuários (requer autenticação)

### Fotos
- `POST /api/photos` - Upload de foto (requer autenticação, multipart/form-data)
- `GET /api/photos/latest` - Obter última foto recebida (requer autenticação)
- `GET /api/photos` - Listar todas as fotos do usuário (requer autenticação)
- `GET /api/photos/:id/image` - Obter imagem da foto (requer autenticação)
- `PUT /api/photos/:id/seen` - Marcar foto como vista (requer autenticação)

## Estrutura

```
backend/
├── src/
│   ├── config/
│   │   └── database.js       # Configuração MySQL
│   ├── controllers/
│   │   ├── authController.js
│   │   ├── userController.js
│   │   └── photoController.js
│   ├── middleware/
│   │   └── authMiddleware.js # Validação JWT
│   ├── models/
│   │   ├── User.js
│   │   └── Photo.js
│   ├── routes/
│   │   ├── auth.js
│   │   ├── users.js
│   │   └── photos.js
│   └── server.js            # Servidor Express
├── database/
│   └── schema.sql           # Script SQL
├── package.json
└── .env                     # Variáveis de ambiente
```

## Segurança

- Senhas são hashadas com bcrypt (10 rounds)
- JWT tokens com expiração configurável
- Validação de autenticação em todos os endpoints protegidos
- Validação de ownership (usuário só acessa suas próprias fotos)

## Notas

- As imagens são armazenadas como BLOB no MySQL
- O tamanho máximo de upload é 10MB
- O polling do app Android verifica novas fotos a cada 30 segundos

