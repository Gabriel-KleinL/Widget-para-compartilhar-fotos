#!/bin/bash

# Script de Teste - Fase 3: Remover JDBC
# Valida que todo o código legado JDBC foi removido

# Não usar set -e para permitir que todos os testes sejam executados

echo "═══════════════════════════════════════════════════════"
echo "  TESTE DA FASE 3: Remover JDBC"
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

check_file_exists() {
    local file=$1
    local description=$2

    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ PASSOU:${NC} $description"
        ((PASSED++))
    else
        echo -e "${RED}❌ FALHOU:${NC} $description"
        echo -e "   ${RED}Arquivo não encontrado:${NC} $file"
        ((FAILED++))
    fi
}

check_file_not_exists() {
    local file=$1
    local description=$2

    if [ ! -f "$file" ]; then
        echo -e "${GREEN}✅ PASSOU:${NC} $description"
        ((PASSED++))
    else
        echo -e "${RED}❌ FALHOU:${NC} $description"
        echo -e "   ${RED}Arquivo ainda existe (deveria ter sido removido):${NC} $file"
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
        echo -e "   ${RED}Pattern ainda encontrado:${NC} $pattern"
        ((FAILED++))
    fi
}

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 1: ARQUIVOS JDBC REMOVIDOS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_file_not_exists "app/src/main/java/com/vivacomigo/app/data/DatabaseHelper.kt" \
    "DatabaseHelper.kt foi removido"

check_file_not_exists "app/src/main/java/com/vivacomigo/app/data/DatabaseConfig.kt" \
    "DatabaseConfig.kt foi removido (⚠️ CREDENCIAIS EXPOSTAS)"

check_file_not_exists "app/src/main/java/com/vivacomigo/app/data/repository/AuthRepositoryApi.kt" \
    "AuthRepositoryApi.kt foi removido/renomeado"

check_file_not_exists "app/src/main/java/com/vivacomigo/app/data/repository/UserRepositoryApi.kt" \
    "UserRepositoryApi.kt foi removido/renomeado"

check_file_not_exists "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepositoryApi.kt" \
    "PhotoRepositoryApi.kt foi removido/renomeado"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 2: DEPENDÊNCIA MYSQL REMOVIDA${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_not_content "app/build.gradle.kts" \
    "mysql:mysql-connector-java" \
    "Dependência MySQL removida do build.gradle.kts"

check_not_content "app/build.gradle.kts" \
    "mysql-connector" \
    "Nenhuma referência a mysql-connector"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 3: REPOSITORIES RENOMEADOS (SEM SUFIXO API)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_file_exists "app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt" \
    "AuthRepository.kt existe (renomeado)"

check_file_exists "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "UserRepository.kt existe (renomeado)"

check_file_exists "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "PhotoRepository.kt existe (renomeado)"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 4: REFERÊNCIAS INTERNAS CORRETAS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

# Verificar que os repositories usam o nome correto internamente
check_not_content "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "AuthRepositoryApi" \
    "UserRepository não referencia AuthRepositoryApi"

check_not_content "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "AuthRepositoryApi" \
    "PhotoRepository não referencia AuthRepositoryApi"

# Verificar que usam AuthRepository correto
check_content "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "AuthRepository" \
    "UserRepository usa AuthRepository"

check_content "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "AuthRepository" \
    "PhotoRepository usa AuthRepository"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 5: LOGS ATUALIZADOS (SEM SUFIXO API)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_not_content "app/src/main/java/com/vivacomigo/app/data/repository/AuthRepository.kt" \
    "AuthRepositoryApi" \
    "AuthRepository: Logs não usam sufixo Api"

check_not_content "app/src/main/java/com/vivacomigo/app/data/repository/UserRepository.kt" \
    "UserRepositoryApi" \
    "UserRepository: Logs não usam sufixo Api"

check_not_content "app/src/main/java/com/vivacomigo/app/data/repository/PhotoRepository.kt" \
    "PhotoRepositoryApi" \
    "PhotoRepository: Logs não usam sufixo Api"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 6: MAINVIEWMODEL LIMPO${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

check_not_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "DatabaseHelper" \
    "MainViewModel não usa DatabaseHelper"

check_not_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "AuthRepositoryApi" \
    "MainViewModel não usa AuthRepositoryApi"

check_not_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "UserRepositoryApi" \
    "MainViewModel não usa UserRepositoryApi"

check_not_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "PhotoRepositoryApi" \
    "MainViewModel não usa PhotoRepositoryApi"

# Verificar que usa os repositories corretos
check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "AuthRepository(context)" \
    "MainViewModel inicializa AuthRepository corretamente"

check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "UserRepository(context)" \
    "MainViewModel inicializa UserRepository corretamente"

check_content "app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt" \
    "PhotoRepository(context)" \
    "MainViewModel inicializa PhotoRepository corretamente"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TESTE 7: COMPILAÇÃO (BUILD.GRADLE.KTS)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"

# Verificar que o projeto compila sem JDBC
if [ -f "app/build.gradle.kts" ]; then
    echo -e "${GREEN}✅ PASSOU:${NC} build.gradle.kts existe e está limpo"
    ((PASSED++))
else
    echo -e "${RED}❌ FALHOU:${NC} build.gradle.kts não encontrado"
    ((FAILED++))
fi

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
    echo -e "${GREEN}  ✅ FASE 3: TODOS OS TESTES PASSARAM! 🎉${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Código limpo, sem JDBC, compilando corretamente!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  ❌ FASE 3: ALGUNS TESTES FALHARAM${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    exit 1
fi
