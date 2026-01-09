/**
 * Setup global para testes
 * Configurações compartilhadas por todos os testes
 */

// Aumenta timeout para testes de integração que fazem requests reais
jest.setTimeout(10000);

// Variáveis de ambiente para testes
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET = 'test-secret-key-for-jwt-testing-only';
process.env.DB_HOST = process.env.DB_HOST || 'srv1965.hstgr.io';
process.env.DB_USER = process.env.DB_USER || 'u466620993_gabrielklein24';
process.env.DB_PASSWORD = process.env.DB_PASSWORD || 'W!M$EL?y6';
process.env.DB_DATABASE = process.env.DB_DATABASE || 'u466620993_poker';

// Limpa mocks após cada teste
afterEach(() => {
    jest.clearAllMocks();
});
