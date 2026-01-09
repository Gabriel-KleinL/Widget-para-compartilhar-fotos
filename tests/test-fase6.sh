#!/bin/bash

# Navega para a raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT" || exit 1

echo "=========================================="
echo "  TESTE FASE 6: Qualidade & Manutenibilidade"
echo "=========================================="
echo ""

passed=0
failed=0

# Função auxiliar para teste
test_check() {
    if [ $? -eq 0 ]; then
        echo "   ✅ $1"
        ((passed++))
    else
        echo "   ❌ $1"
        ((failed++))
    fi
}

echo "📊 1. TESTES DE INTEGRAÇÃO (JEST)"
echo "---"

# 1.1 - Verificar package.json tem jest
grep -q "\"jest\"" backend/package.json
test_check "Jest instalado no backend"

# 1.2 - Verificar supertest
grep -q "\"supertest\"" backend/package.json
test_check "Supertest instalado"

# 1.3 - Verificar scripts de teste
grep -q "\"test\":" backend/package.json
test_check "Script 'npm test' configurado"

# 1.4 - Verificar testes de Auth
test -f backend/tests/integration/auth.test.js
test_check "Testes de Auth criados (auth.test.js)"

# 1.5 - Verificar testes de Users
test -f backend/tests/integration/users.test.js
test_check "Testes de Users criados (users.test.js)"

# 1.6 - Verificar testes de Photos
test -f backend/tests/integration/photos.test.js
test_check "Testes de Photos criados (photos.test.js)"

# 1.7 - Verificar setup de testes
test -f backend/tests/setup.js
test_check "Setup de testes criado (setup.js)"

# 1.8 - Verificar configuração Jest no package.json
grep -q "\"jest\":" backend/package.json
test_check "Configuração Jest no package.json"

echo ""
echo "📝 2. WINSTON LOGGING"
echo "---"

# 2.1 - Verificar winston instalado
grep -q "\"winston\"" backend/package.json
test_check "Winston instalado"

# 2.2 - Verificar winston-daily-rotate-file
grep -q "\"winston-daily-rotate-file\"" backend/package.json
test_check "Winston Daily Rotate File instalado"

# 2.3 - Verificar arquivo de configuração
test -f backend/src/config/logger.js
test_check "Logger configurado (logger.js)"

# 2.4 - Verificar logger importado no server.js
grep -q "require('./config/logger')" backend/src/server.js
test_check "Logger importado no server.js"

# 2.5 - Verificar middleware httpLogger
grep -q "logger.httpLogger" backend/src/server.js
test_check "Middleware httpLogger aplicado"

# 2.6 - Verificar logger.logError usado
grep -q "logger.logError" backend/src/server.js
test_check "logger.logError usado para erros"

# 2.7 - Verificar diretório de logs no .gitignore
grep -q "logs/" backend/.gitignore
test_check "Diretório logs/ no .gitignore"

# 2.8 - Verificar DailyRotateFile configurado
grep -q "DailyRotateFile" backend/src/config/logger.js
test_check "DailyRotateFile configurado no logger"

echo ""
echo "📊 3. FIREBASE CRASHLYTICS (ANDROID)"
echo "---"

# 3.1 - Verificar plugin crashlytics no build.gradle.kts (root)
grep -q "com.google.firebase.crashlytics" build.gradle.kts
test_check "Plugin Crashlytics no build.gradle.kts (root)"

# 3.2 - Verificar plugin crashlytics no app/build.gradle.kts
grep -q "com.google.firebase.crashlytics" app/build.gradle.kts
test_check "Plugin Crashlytics no app/build.gradle.kts"

# 3.3 - Verificar dependência crashlytics-ktx
grep -q "firebase-crashlytics-ktx" app/build.gradle.kts
test_check "firebase-crashlytics-ktx adicionado"

# 3.4 - Verificar dependência analytics-ktx
grep -q "firebase-analytics-ktx" app/build.gradle.kts
test_check "firebase-analytics-ktx adicionado"

# 3.5 - Verificar import Crashlytics no VivaApp.kt
grep -q "import com.google.firebase.crashlytics.FirebaseCrashlytics" app/src/main/java/com/vivacomigo/app/VivaApp.kt
test_check "Import FirebaseCrashlytics no VivaApp.kt"

# 3.6 - Verificar inicialização Crashlytics
grep -q "FirebaseCrashlytics.getInstance()" app/src/main/java/com/vivacomigo/app/VivaApp.kt
test_check "Crashlytics inicializado no VivaApp"

# 3.7 - Verificar função initializeCrashlytics
grep -q "initializeCrashlytics" app/src/main/java/com/vivacomigo/app/VivaApp.kt
test_check "Função initializeCrashlytics() criada"

# 3.8 - Verificar recordException usado
grep -q "recordException" app/src/main/java/com/vivacomigo/app/VivaApp.kt
test_check "recordException usado para logar exceções"

echo ""
echo "📖 4. SWAGGER/OpenAPI DOCUMENTATION"
echo "---"

# 4.1 - Verificar swagger-ui-express instalado
grep -q "\"swagger-ui-express\"" backend/package.json
test_check "swagger-ui-express instalado"

# 4.2 - Verificar swagger-jsdoc instalado
grep -q "\"swagger-jsdoc\"" backend/package.json
test_check "swagger-jsdoc instalado"

# 4.3 - Verificar arquivo de configuração swagger
test -f backend/src/config/swagger.js
test_check "Swagger configurado (swagger.js)"

# 4.4 - Verificar swagger importado no server.js
grep -q "require('./config/swagger')" backend/src/server.js
test_check "Swagger importado no server.js"

# 4.5 - Verificar rota /api-docs
grep -q "/api-docs" backend/src/server.js
test_check "Rota /api-docs configurada"

# 4.6 - Verificar swaggerUi.setup
grep -q "swaggerUi.setup" backend/src/server.js
test_check "swaggerUi.setup configurado"

# 4.7 - Verificar rota /api-docs.json
grep -q "/api-docs.json" backend/src/server.js
test_check "Rota /api-docs.json (JSON spec)"

# 4.8 - Verificar OpenAPI version no swagger.js
grep -q "openapi.*3.0.0" backend/src/config/swagger.js
test_check "OpenAPI 3.0.0 configurado"

# 4.9 - Verificar security schemes (bearerAuth)
grep -q "bearerAuth" backend/src/config/swagger.js
test_check "Security schemes (bearerAuth) configurado"

# 4.10 - Verificar schemas definidos
grep -q "schemas:" backend/src/config/swagger.js
test_check "Schemas (User, Photo, Error) definidos"

echo ""
echo "📜 5. DOCUMENTAÇÃO"
echo "---"

# 5.1 - Verificar CHANGELOG.md
test -f CHANGELOG.md
test_check "CHANGELOG.md criado"

# 5.2 - Verificar DEPLOYMENT.md
test -f DEPLOYMENT.md
test_check "DEPLOYMENT.md criado"

# 5.3 - Verificar versões no CHANGELOG
grep -q "\[1.6.0\]" CHANGELOG.md
test_check "Versão 1.6.0 (Fase 6) no CHANGELOG"

# 5.4 - Verificar todas as fases no CHANGELOG
grep -q "Fase 1.*Backend API" CHANGELOG.md && \
grep -q "Fase 2.*Android Client" CHANGELOG.md && \
grep -q "Fase 3.*Remover JDBC" CHANGELOG.md && \
grep -q "Fase 4.*Push Notifications" CHANGELOG.md && \
grep -q "Fase 5.*Segurança.*Performance" CHANGELOG.md && \
grep -q "Fase 6.*Qualidade.*Manutenibilidade" CHANGELOG.md
test_check "Todas as 6 fases documentadas no CHANGELOG"

# 5.5 - Verificar seções no DEPLOYMENT.md
grep -q "Backend.*Render" DEPLOYMENT.md && \
grep -q "Firebase.*FCM" DEPLOYMENT.md && \
grep -q "App Android" DEPLOYMENT.md
test_check "Seções principais no DEPLOYMENT.md"

# 5.6 - Verificar health check atualizado
grep -q "docs.*req.protocol" backend/src/server.js
test_check "Health check retorna link para /api-docs"

echo ""
echo "🗂️ 6. ESTRUTURA DE TESTES"
echo "---"

# 6.1 - Verificar diretório tests/
test -d tests
test_check "Diretório tests/ existe"

# 6.2 - Verificar test-fase1.sh
test -f tests/test-fase1.sh
test_check "test-fase1.sh movido para tests/"

# 6.3 - Verificar test-fase2.sh
test -f tests/test-fase2.sh
test_check "test-fase2.sh movido para tests/"

# 6.4 - Verificar test-fase3.sh
test -f tests/test-fase3.sh
test_check "test-fase3.sh movido para tests/"

# 6.5 - Verificar test-fase4.sh
test -f tests/test-fase4.sh
test_check "test-fase4.sh movido para tests/"

# 6.6 - Verificar test-fase5.sh
test -f tests/test-fase5.sh
test_check "test-fase5.sh movido para tests/"

# 6.7 - Verificar run-all-tests.sh
test -f tests/run-all-tests.sh
test_check "run-all-tests.sh criado"

# 6.8 - Verificar tests/README.md
test -f tests/README.md
test_check "tests/README.md criado"

# 6.9 - Verificar este arquivo (test-fase6.sh)
test -f tests/test-fase6.sh
test_check "test-fase6.sh criado"

echo ""
echo "📦 7. ARQUIVOS .EXAMPLE E .GITIGNORE"
echo "---"

# 7.1 - Verificar logs no .gitignore
grep -q "logs/" backend/.gitignore 2>/dev/null || grep -q "logs/" .gitignore
test_check "logs/ no .gitignore"

# 7.2 - Verificar node_modules no .gitignore
grep -q "node_modules" backend/.gitignore 2>/dev/null || grep -q "node_modules" .gitignore
test_check "node_modules/ no .gitignore"

# 7.3 - Verificar firebase-admin-key.json no .gitignore
grep -q "firebase-admin-key.json" .gitignore
test_check "firebase-admin-key.json no .gitignore"

# 7.4 - Verificar google-services.json no .gitignore
grep -q "google-services.json" .gitignore
test_check "google-services.json no .gitignore"

echo ""
echo "=========================================="
echo "  RESUMO DOS TESTES"
echo "=========================================="
echo ""
echo "✅ Testes Passaram: $passed"
echo "❌ Testes Falharam: $failed"
echo ""

total=$((passed + failed))
percentage=$((passed * 100 / total))

echo "📊 Taxa de Sucesso: $percentage% ($passed/$total)"
echo ""

if [ $failed -eq 0 ]; then
    echo "🎉 FASE 6 COMPLETA! Todos os testes passaram!"
    echo ""
    echo "📋 Próximos passos:"
    echo "  1. Executar testes de integração: cd backend && npm test"
    echo "  2. Testar Swagger: http://localhost:3000/api-docs"
    echo "  3. Build do Android: ./gradlew assembleDebug"
    echo "  4. Fazer deploy conforme DEPLOYMENT.md"
    echo ""
    exit 0
else
    echo "⚠️  FASE 6 INCOMPLETA. Verifique os testes que falharam acima."
    echo ""
    exit 1
fi
