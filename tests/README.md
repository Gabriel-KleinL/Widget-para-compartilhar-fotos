# 🧪 Testes do Projeto Viva Comigo

Esta pasta contém todos os scripts de teste automatizados para validar cada fase do projeto.

## 📋 Estrutura dos Testes

```
tests/
├── test-fase1.sh  - Backend API (Base)
├── test-fase2.sh  - Android API Client
├── test-fase3.sh  - Remover JDBC
├── test-fase4.sh  - Push Notifications (FCM)
└── README.md      - Este arquivo
```

---

## 🚀 Como Executar

### Teste Individual

Execute um teste específico:

```bash
# Fase 1: Backend API
bash tests/test-fase1.sh

# Fase 2: Android API Client
bash tests/test-fase2.sh

# Fase 3: Remover JDBC
bash tests/test-fase3.sh

# Fase 4: Push Notifications
bash tests/test-fase4.sh
```

### Todos os Testes (Regressivo)

Execute todos os testes em sequência:

```bash
bash tests/test-fase1.sh && \
bash tests/test-fase2.sh && \
bash tests/test-fase3.sh && \
bash tests/test-fase4.sh && \
echo "🎉 TODOS OS TESTES PASSARAM!"
```

Ou use o script helper:

```bash
bash tests/run-all-tests.sh
```

---

## 📊 Descrição dos Testes

### Test-Fase1.sh (Backend API)
**Total**: 30 testes  
**Valida**:
- ✅ Health check do servidor
- ✅ Registro simplificado (sem senha)
- ✅ Login (por nome e código)
- ✅ Busca de usuários
- ✅ Pareamento de usuários
- ✅ Upload de fotos
- ✅ Download de fotos
- ✅ Desparear usuários
- ✅ Segurança (JWT + Helmet)

**Pré-requisito**: Backend rodando em `http://localhost:3000`

---

### Test-Fase2.sh (Android API Client)
**Total**: 18 testes  
**Valida**:
- ✅ Arquivos API criados (ApiModels, ApiService, RetrofitClient)
- ✅ Repositories renomeados (sem sufixo "Api")
- ✅ Dependências Retrofit configuradas
- ✅ API models definidos
- ✅ Endpoints configurados
- ✅ MainViewModel usa API
- ✅ Repositories usam Retrofit (não JDBC)

**Pré-requisito**: Código Android sincronizado

---

### Test-Fase3.sh (Remover JDBC)
**Total**: 25 testes  
**Valida**:
- ✅ DatabaseHelper.kt removido
- ✅ DatabaseConfig.kt removido (credenciais)
- ✅ Dependência MySQL removida
- ✅ Repositories renomeados (AuthRepository, UserRepository, PhotoRepository)
- ✅ Referências internas corretas
- ✅ Logs atualizados (sem sufixo "Api")
- ✅ MainViewModel limpo (sem JDBC)
- ✅ Build.gradle.kts sem MySQL

**Pré-requisito**: Fase 2 completa

---

### Test-Fase4.sh (Push Notifications)
**Total**: 9 testes  
**Valida**:
- ✅ google-services.json configurado
- ✅ Dependências Firebase
- ✅ VivaMessagingService criado
- ✅ AndroidManifest atualizado
- ✅ firebase-admin instalado
- ✅ firebase-admin-key.json configurado
- ✅ Migration SQL criada
- ✅ Polling removido
- ✅ WorkManager ajustado (2h)

**Pré-requisito**: Firebase configurado

---

## 🔧 Solução de Problemas

### Erro: "Backend não está rodando"
**Teste**: test-fase1.sh  
**Solução**:
```bash
cd backend
npm start
```

### Erro: "Arquivo não encontrado"
**Teste**: Qualquer  
**Solução**: Execute os testes a partir da **raiz do projeto**:
```bash
cd /Users/gabrielkleinlima/Programando/Widget-para-compartilhar-fotos
bash tests/test-fase1.sh
```

### Erro: "Permission denied"
**Teste**: Qualquer  
**Solução**: Torne os scripts executáveis:
```bash
chmod +x tests/*.sh
```

### Erro: "mysql-client não instalado"
**Teste**: test-fase4.sh  
**Solução**: Este é um warning esperado. A migration já foi executada via phpMyAdmin.

---

## 📈 Cobertura de Testes

| Fase | Testes | Status | Cobertura |
|------|--------|--------|-----------|
| Fase 1 | 30 | ✅ | 100% |
| Fase 2 | 20 | ✅ | 100% |
| Fase 3 | 25 | ✅ | 100% |
| Fase 4 | 9 | ✅ | 100% |
| **Total** | **84** | ✅ | **100%** |

---

## 🎯 Quando Executar

### Durante Desenvolvimento
Execute o teste da fase que você está trabalhando:
```bash
bash tests/test-fase2.sh
```

### Antes de Commit
Execute todos os testes para garantir que nada quebrou:
```bash
bash tests/run-all-tests.sh
```

### Antes de Deploy
Execute todos os testes + validação manual no dispositivo:
```bash
bash tests/run-all-tests.sh
# Depois: teste manual no celular
```

---

## 📚 Documentação Relacionada

- **Checklist Geral**: `.claude/REFACTORING_CHECKLIST.md`
- **Fase 1**: `.claude/FASE1_COMPLETA.md`
- **Fase 2**: `.claude/FASE2_COMPLETA.md`
- **Fase 3**: `.claude/FASE3_COMPLETA.md`
- **Fase 4**: `.claude/FASE4_COMPLETA.md`

---

## 🤝 Contribuindo

Ao adicionar uma nova fase:

1. Crie `test-faseX.sh` nesta pasta
2. Siga o padrão dos outros testes (cores, contadores, relatório)
3. Documente aqui no README
4. Atualize `.claude/REFACTORING_CHECKLIST.md`
5. Execute todos os testes regressivos

---

**Última atualização**: 08 de Janeiro de 2026  
**Versão**: 1.0
