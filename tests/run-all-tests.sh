#!/bin/bash

# Script para executar todos os testes regressivos
# Execute a partir da raiz do projeto

echo "═══════════════════════════════════════════════════════"
echo "  🧪 EXECUTANDO TODOS OS TESTES REGRESSIVOS"
echo "═══════════════════════════════════════════════════════"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TOTAL_PASSED=0
TOTAL_FAILED=0
TESTS_RUN=0

run_test() {
    local test_file=$1
    local test_name=$2
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  Executando: $test_name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    if bash "$test_file"; then
        echo -e "${GREEN}✅ $test_name: PASSOU${NC}"
        ((TESTS_RUN++))
    else
        echo -e "${RED}❌ $test_name: FALHOU${NC}"
        echo -e "${RED}Parando execução...${NC}"
        exit 1
    fi
    
    echo ""
    echo ""
}

# Ir para a raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT" || exit 1

# Executar todos os testes em ordem
run_test "tests/test-fase1.sh" "FASE 1: Backend API"
run_test "tests/test-fase2.sh" "FASE 2: Android API Client"
run_test "tests/test-fase3.sh" "FASE 3: Remover JDBC"
run_test "tests/test-fase4.sh" "FASE 4: Push Notifications"
run_test "tests/test-fase5.sh" "FASE 5: Segurança & Performance"

# Relatório final
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  🎉 TODOS OS TESTES PASSARAM!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo -e "Total de fases testadas: ${BLUE}$TESTS_RUN${NC}"
echo -e "Status: ${GREEN}✅ Todas as fases operacionais${NC}"
echo ""
echo -e "Você pode prosseguir com confiança! 🚀"
echo ""
