/**
 * Testes de Integração: Autenticação
 * Testa os endpoints de registro e login
 */

const request = require('supertest');
const express = require('express');
const authRoutes = require('../../src/routes/auth');
const db = require('../../src/config/database');

// Cria app de teste
const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);

describe('Auth API Integration Tests', () => {
    // Variáveis compartilhadas
    let testUserName;
    let testUserId;
    let authToken;

    beforeAll(async () => {
        // Aguarda conexão com banco
        await new Promise(resolve => setTimeout(resolve, 1000));
    });

    beforeEach(() => {
        // Nome único para cada teste
        testUserName = `TestUser_${Date.now()}_${Math.random().toString(36).substring(7)}`;
    });

    afterEach(async () => {
        // Limpa usuários de teste criados
        if (testUserId) {
            try {
                await db.execute('DELETE FROM users WHERE id = ?', [testUserId]);
                testUserId = null;
            } catch (error) {
                // Ignora erros de cleanup
            }
        }
    });

    afterAll(async () => {
        // Fecha conexão com banco
        await db.end();
    });

    describe('POST /api/auth/register-simple', () => {
        it('deve registrar um novo usuário com sucesso', async () => {
            const response = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: testUserName })
                .expect('Content-Type', /json/)
                .expect(201);

            expect(response.body).toHaveProperty('token');
            expect(response.body).toHaveProperty('user');
            expect(response.body.user).toHaveProperty('id');
            expect(response.body.user).toHaveProperty('name', testUserName);
            expect(response.body.user).toHaveProperty('pairingCode');

            // Salva para cleanup
            testUserId = response.body.user.id;
            authToken = response.body.token;
        });

        it('deve rejeitar registro sem nome', async () => {
            const response = await request(app)
                .post('/api/auth/register-simple')
                .send({})
                .expect('Content-Type', /json/)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar nome muito curto', async () => {
            const response = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: 'AB' })
                .expect('Content-Type', /json/)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar nome muito longo', async () => {
            const response = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: 'A'.repeat(101) })
                .expect('Content-Type', /json/)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('POST /api/auth/login-simple', () => {
        beforeEach(async () => {
            // Cria usuário para testes de login
            const registerResponse = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: testUserName });

            testUserId = registerResponse.body.user.id;
        });

        it('deve fazer login com nome correto', async () => {
            const response = await request(app)
                .post('/api/auth/login-simple')
                .send({ name: testUserName })
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('token');
            expect(response.body).toHaveProperty('user');
            expect(response.body.user).toHaveProperty('name', testUserName);
        });

        it('deve rejeitar login com nome inexistente', async () => {
            const response = await request(app)
                .post('/api/auth/login-simple')
                .send({ name: 'UsuarioQueNaoExiste123' })
                .expect('Content-Type', /json/)
                .expect(401);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar login sem nome', async () => {
            const response = await request(app)
                .post('/api/auth/login-simple')
                .send({})
                .expect('Content-Type', /json/)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('POST /api/auth/login-code', () => {
        let testPairingCode;

        beforeEach(async () => {
            // Cria usuário para testes de login por código
            const registerResponse = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: testUserName });

            testUserId = registerResponse.body.user.id;
            testPairingCode = registerResponse.body.user.pairingCode;
        });

        it('deve fazer login com código correto', async () => {
            const response = await request(app)
                .post('/api/auth/login-code')
                .send({ pairingCode: testPairingCode })
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('token');
            expect(response.body).toHaveProperty('user');
            expect(response.body.user).toHaveProperty('pairingCode', testPairingCode);
        });

        it('deve rejeitar login com código inválido', async () => {
            const response = await request(app)
                .post('/api/auth/login-code')
                .send({ pairingCode: 'CODIGO_INVALIDO' })
                .expect('Content-Type', /json/)
                .expect(401);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar login sem código', async () => {
            const response = await request(app)
                .post('/api/auth/login-code')
                .send({})
                .expect('Content-Type', /json/)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });
});
