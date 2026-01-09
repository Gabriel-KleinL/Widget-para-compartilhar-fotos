#!/bin/bash

# Script de Teste - Fase 2: Android API Client
# Valida que a integração com API REST está funcional

# Não usar set -e para permitir que todos os testes sejam executados

echo "═══════════════════════════════════════════════════════"
echo "  TESTE DA FASE 2: Android API Client"
echo "═══════════════════════════════════════════════════════"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0

check_file() {
    local file=$1
    local description=$2

    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ PASSOU:${NC} $description"
        echo -e "   ${BLUE}Arquivo:${NC} $file"
        ((PASSED++))
    else
        echo -e "${RED}❌ FALHOU:${NC} $description"
        echo -e "   ${RED}Arquivo não encontrado:${NC} $file"
        ((FAILED++))
    fi
}

check_content() {
    local file=$1
    local pattern=$2
    local description=$3

    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✅ PASSOU:${NC} $description"
        ((PASSED++))
    else
        echo -e "${RED}❌ FALHOU:${NC} $description"
        echo -e "   ${RED}Pattern não encontrado:${NC} $pattern"
        ((FAILED++))
    fi
}

check_not_content() {
    local file=$1
    local pattern=$2
    local description=$3

    if ! grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✅ PASSOU:${NC} $description"
        ((PASSED++))
    else
        echo -e "${RED}❌ FALHOU:${NC} $description"
        echo -e "   ${RED}Pattern ainda existe:${NC} $pattern"
        ((FAILED++))
    fi
}

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 1: ARQUIVOS API CRIADOS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_file "app/src/main/java/com/vivacomigo/app/data/api/ApiModels.kt" \
    "ApiModels.kt existe"

check_file "app/src/main/java/com/vivacomigo/app/data/api/ApiService.kt" \
    "ApiService.kt existe"

check_file "app/src/main/java/com/vivacomigo/app/data/api/RetrofitClient.kt" \
    "RetrofitClient.kt existe"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 2: REPOSITORIES API (RENOMEADOS NA FASE 3)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

# Nota: Na Fase 3, os repositories foram renomeados removendo o sufixo "Api"
check_file "app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt" \
    "AuthRepository.kt existe (renomeado de AuthRepositoryApi)"

check_file "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "UserRepository.kt existe (renomeado de UserRepositoryApi)"

check_file "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "PhotoRepository.kt existe (renomeado de PhotoRepositoryApi)"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 3: DEPENDÊNCIAS RETROFIT${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_content "app/build.gradle.kts" \
    "retrofit2:retrofit" \
    "Retrofit dependency adicionada"

check_content "app/build.gradle.kts" \
    "retrofit2:converter-gson" \
    "Gson converter adicionado"

check_content "app/build.gradle.kts" \
    "okhttp3:logging-interceptor" \
    "OkHttp logging interceptor adicionado"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 4: API MODELS E ENDPOINTS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_content "app/src/main/java/com/vivacomigo/app/data/api/ApiModels.kt" \
    "data class ApiUser" \
    "ApiUser model definido"

check_content "app/src/main/java/com/vivacomigo/app/data/api/ApiModels.kt" \
    "data class ApiPhoto" \
    "ApiPhoto model definido"

check_content "app/src/main/java/com/vivacomigo/app/data/api/ApiService.kt" \
    "registerSimple" \
    "Endpoint de registro definido"

check_content "app/src/main/java/com/vivacomigo/app/data/api/ApiService.kt" \
    "uploadPhoto" \
    "Endpoint de upload definido"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 5: MAINVIEWMODEL USA API${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "AuthRepository" \
    "MainViewModel usa AuthRepository"

check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "UserRepository" \
    "MainViewModel usa UserRepository"

check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "PhotoRepository" \
    "MainViewModel usa PhotoRepository"

# Verificar que NÃO usa DatabaseHelper (JDBC removido)
check_not_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "DatabaseHelper" \
    "MainViewModel NÃO usa DatabaseHelper (JDBC removido)"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 6: REPOSITORIES USAM RETROFIT${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_content "app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt" \
    "apiService" \
    "AuthRepository usa apiService"

check_content "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "apiService" \
    "UserRepository usa apiService"

check_content "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "apiService" \
    "PhotoRepository usa apiService"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  RELATÓRIO FINAL${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""
echo -e "Total de testes: ${BLUE}$((PASSED + FAILED))${NC}"
echo -e "${GREEN}✅ Passaram: $PASSED${NC}"
echo -e "${RED}❌ Falharam: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✅ FASE 2: TODOS OS TESTES PASSARAM! 🎉${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  ❌ FASE 2: ALGUNS TESTES FALHARAM${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    exit 1
fi
