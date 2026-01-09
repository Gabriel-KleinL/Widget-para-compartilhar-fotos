#!/bin/bash

# ====================================================================
# SCRIPT DE TESTES - FASE 1: BACKEND API
# ====================================================================
# Este script testa TODOS os endpoints do backend da Fase 1
# Execute sempre que fizer mudanças para garantir que nada quebrou
#
# USO:
#   chmod +x test-fase1.sh
#   ./test-fase1.sh
# ====================================================================

# set -e  # Para no primeiro erro (desabilitado para ver todos os resultados)

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variáveis
BASE_URL="http://localhost:3000"
FAILED=0
PASSED=0

# ====================================================================
# FUNÇÕES AUXILIARES
# ====================================================================

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
}

print_test() {
    echo -e "${YELLOW}→ Testando:${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅ PASSOU:${NC} $1"
    ((PASSED++))
}

print_error() {
    echo -e "${RED}❌ FALHOU:${NC} $1"
    echo -e "${RED}   Resposta:${NC} $2"
    ((FAILED++))
}

test_endpoint() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local headers="$5"
    local expected_status="$6"

    print_test "$test_name"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint" $headers)
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" -H "Content-Type: application/json" $headers -d "$data")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint" -H "Content-Type: application/json" $headers -d "$data")
    fi

    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$status_code" = "$expected_status" ]; then
        print_success "$test_name (HTTP $status_code)"
        echo "$body"
    else
        print_error "$test_name (esperado HTTP $expected_status, recebeu $status_code)" "$body"
    fi

    echo ""
}

cleanup_test_users() {
    echo -e "${YELLOW}🧹 Limpando usuários de teste do banco...${NC}"
    # Aqui você pode adicionar comandos SQL para limpar dados de teste
    # Por enquanto, vamos deixar para limpeza manual se necessário
}

# ====================================================================
# VERIFICAR SE SERVIDOR ESTÁ RODANDO
# ====================================================================

print_header "VERIFICANDO SERVIDOR"

if ! curl -s "$BASE_URL/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ ERRO: Servidor não está rodando em $BASE_URL${NC}"
    echo -e "${YELLOW}Por favor, inicie o servidor com: npm start${NC}"
    exit 1
fi

print_success "Servidor está rodando"
echo ""

# ====================================================================
# TESTE 1: HEALTH CHECK
# ====================================================================

print_header "TESTE 1: HEALTH CHECK"
test_endpoint \
    "Health check" \
    "GET" \
    "/health" \
    "" \
    "" \
    "200"

# ====================================================================
# TESTE 2: REGISTRO SIMPLIFICADO (SEM SENHA)
# ====================================================================

print_header "TESTE 2: REGISTRO SIMPLIFICADO"

# Gerar nomes únicos para evitar conflitos
TIMESTAMP=$(date +%s)
USER_A_NAME="TestUser_A_$TIMESTAMP"
USER_B_NAME="TestUser_B_$TIMESTAMP"

print_test "Registrar usuário A (sem senha)"
sleep 1  # Evitar rate limiting
response=$(curl -s -X POST "$BASE_URL/api/auth/register-simple" \
    -H "Content-Type: application/json" \
    -d "{\"display_name\":\"$USER_A_NAME\"}")

echo "$response" | python3 -m json.tool

if echo "$response" | grep -q "token"; then
    print_success "Usuário A registrado com sucesso"
    TOKEN_A=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")
    USER_A_ID=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['user']['id'])")
    PAIRING_CODE_A=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['user']['pairing_code'])")
    echo "  Token A: $TOKEN_A"
    echo "  ID A: $USER_A_ID"
    echo "  Código A: $PAIRING_CODE_A"
    ((PASSED++))
else
    print_error "Registro do usuário A" "$response"
fi
echo ""

print_test "Registrar usuário B (sem senha)"
response=$(curl -s -X POST "$BASE_URL/api/auth/register-simple" \
    -H "Content-Type: application/json" \
    -d "{\"display_name\":\"$USER_B_NAME\"}")

echo "$response" | python3 -m json.tool

if echo "$response" | grep -q "token"; then
    print_success "Usuário B registrado com sucesso"
    TOKEN_B=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")
    USER_B_ID=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['user']['id'])")
    PAIRING_CODE_B=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['user']['pairing_code'])")
    echo "  Token B: $TOKEN_B"
    echo "  ID B: $USER_B_ID"
    echo "  Código B: $PAIRING_CODE_B"
    ((PASSED++))
else
    print_error "Registro do usuário B" "$response"
fi
echo ""

# ====================================================================
# TESTE 3: LOGIN SIMPLIFICADO
# ====================================================================

print_header "TESTE 3: LOGIN SIMPLIFICADO"

print_test "Login com nome (usuário A)"
response=$(curl -s -X POST "$BASE_URL/api/auth/login-simple" \
    -H "Content-Type: application/json" \
    -d "{\"display_name\":\"$USER_A_NAME\"}")

if echo "$response" | grep -q "token"; then
    print_success "Login com nome funcionando"
    ((PASSED++))
else
    print_error "Login com nome" "$response"
fi
echo ""

print_test "Login com código de pareamento (usuário A)"
response=$(curl -s -X POST "$BASE_URL/api/auth/login-code" \
    -H "Content-Type: application/json" \
    -d "{\"pairing_code\":\"$PAIRING_CODE_A\"}")

if echo "$response" | grep -q "token"; then
    print_success "Login com código funcionando"
    ((PASSED++))
else
    print_error "Login com código" "$response"
fi
echo ""

# ====================================================================
# TESTE 4: BUSCAR USUÁRIOS
# ====================================================================

print_header "TESTE 4: BUSCAR USUÁRIOS"

print_test "GET /users/me (usuário A)"
response=$(curl -s -X GET "$BASE_URL/api/users/me" \
    -H "Authorization: Bearer $TOKEN_A")

if echo "$response" | grep -q "$USER_A_ID"; then
    print_success "Buscar usuário atual funcionando"
    ((PASSED++))
else
    print_error "Buscar usuário atual" "$response"
fi
echo ""

print_test "GET /users/pairing-code/:code (buscar usuário A pelo código)"
response=$(curl -s -X GET "$BASE_URL/api/users/pairing-code/$PAIRING_CODE_A" \
    -H "Authorization: Bearer $TOKEN_B")

if echo "$response" | grep -q "$USER_A_ID"; then
    print_success "Buscar por código de pareamento funcionando"
    ((PASSED++))
else
    print_error "Buscar por código" "$response"
fi
echo ""

# ====================================================================
# TESTE 5: PAREAMENTO
# ====================================================================

print_header "TESTE 5: PAREAMENTO"

print_test "Parear usuário B com usuário A"
response=$(curl -s -X POST "$BASE_URL/api/users/pair" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN_B" \
    -d "{\"partnerCode\":\"$PAIRING_CODE_A\"}")

if echo "$response" | grep -q "partner_id"; then
    print_success "Pareamento funcionando"
    echo "$response" | python3 -m json.tool
    ((PASSED++))
else
    print_error "Pareamento" "$response"
fi
echo ""

# ====================================================================
# TESTE 6: UPLOAD DE FOTO
# ====================================================================

print_header "TESTE 6: UPLOAD DE FOTO"

# Criar arquivo de teste
echo "Test photo data" > /tmp/test_photo_fase1.jpg

print_test "Upload de foto (A → B)"
response=$(curl -s -X POST "$BASE_URL/api/photos" \
    -H "Authorization: Bearer $TOKEN_A" \
    -F "image=@/tmp/test_photo_fase1.jpg" \
    -F "receiverId=$USER_B_ID")

if echo "$response" | grep -q "id"; then
    print_success "Upload de foto funcionando"
    echo "$response" | python3 -m json.tool
    PHOTO_ID=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")
    ((PASSED++))
else
    print_error "Upload de foto" "$response"
fi
echo ""

# ====================================================================
# TESTE 7: BUSCAR FOTOS
# ====================================================================

print_header "TESTE 7: BUSCAR FOTOS"

print_test "GET /photos/latest (última foto de B)"
response=$(curl -s -X GET "$BASE_URL/api/photos/latest" \
    -H "Authorization: Bearer $TOKEN_B")

if echo "$response" | grep -q "id"; then
    print_success "Buscar última foto funcionando"
    echo "$response" | python3 -m json.tool
    ((PASSED++))
else
    print_error "Buscar última foto" "$response"
fi
echo ""

print_test "GET /photos (listar todas as fotos de B)"
response=$(curl -s -X GET "$BASE_URL/api/photos" \
    -H "Authorization: Bearer $TOKEN_B")

if echo "$response" | grep -q "\["; then
    print_success "Listar fotos funcionando"
    echo "$response" | python3 -m json.tool
    ((PASSED++))
else
    print_error "Listar fotos" "$response"
fi
echo ""

if [ -n "$PHOTO_ID" ]; then
    print_test "GET /photos/:id/image (baixar imagem)"
    status_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/photos/$PHOTO_ID/image" \
        -H "Authorization: Bearer $TOKEN_B")

    if [ "$status_code" = "200" ]; then
        print_success "Baixar imagem funcionando (HTTP 200)"
        ((PASSED++))
    else
        print_error "Baixar imagem (esperado 200, recebeu $status_code)" ""
    fi
    echo ""
fi

# ====================================================================
# TESTE 8: DESPAREAR
# ====================================================================

print_header "TESTE 8: DESPAREAR"

print_test "DELETE /users/unpair (desparear usuário B)"
response=$(curl -s -X DELETE "$BASE_URL/api/users/unpair" \
    -H "Authorization: Bearer $TOKEN_B")

if echo "$response" | grep -q "partner_id.*null" || ! echo "$response" | grep -q "error"; then
    print_success "Desparear funcionando"
    echo "$response" | python3 -m json.tool
    ((PASSED++))
else
    print_error "Desparear" "$response"
fi
echo ""

# ====================================================================
# TESTE 9: SEGURANÇA E RATE LIMITING
# ====================================================================

print_header "TESTE 9: SEGURANÇA"

print_test "Endpoint protegido sem token (deve retornar 401)"
status_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users/me")

if [ "$status_code" = "401" ]; then
    print_success "Autenticação obrigatória funcionando (HTTP 401)"
    ((PASSED++))
else
    print_error "Autenticação (esperado 401, recebeu $status_code)" ""
fi
echo ""

print_test "Verificar headers de segurança (Helmet)"
headers=$(curl -s -I "$BASE_URL/health")

if echo "$headers" | grep -qi "X-Content-Type-Options"; then
    print_success "Headers de segurança (Helmet) presentes"
    ((PASSED++))
else
    print_error "Headers de segurança não encontrados" "$headers"
fi
echo ""

# ====================================================================
# RELATÓRIO FINAL
# ====================================================================

print_header "RELATÓRIO FINAL"

TOTAL=$((PASSED + FAILED))

echo ""
echo -e "Total de testes: ${BLUE}$TOTAL${NC}"
echo -e "✅ Passaram: ${GREEN}$PASSED${NC}"
echo -e "❌ Falharam: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✅ FASE 1: TODOS OS TESTES PASSARAM! 🎉${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  ❌ FASE 1: ALGUNS TESTES FALHARAM${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo ""
    exit 1
fi
