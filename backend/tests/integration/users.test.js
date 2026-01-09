/**
 * Testes de Integração: Usuários
 * Testa os endpoints de usuários (busca, pareamento, etc)
 */

const request = require('supertest');
const express = require('express');
const authRoutes = require('../../src/routes/auth');
const usersRoutes = require('../../src/routes/users');
const db = require('../../src/config/database');

// Cria app de teste
const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);

describe('Users API Integration Tests', () => {
    let user1, user2;
    let token1, token2;

    beforeAll(async () => {
        await new Promise(resolve => setTimeout(resolve, 1000));
    });

    beforeEach(async () => {
        // Cria dois usuários para testes
        const name1 = `User1_${Date.now()}_${Math.random().toString(36).substring(7)}`;
        const name2 = `User2_${Date.now()}_${Math.random().toString(36).substring(7)}`;

        const response1 = await request(app)
            .post('/api/auth/register-simple')
            .send({ name: name1 });

        const response2 = await request(app)
            .post('/api/auth/register-simple')
            .send({ name: name2 });

        user1 = response1.body.user;
        token1 = response1.body.token;
        user2 = response2.body.user;
        token2 = response2.body.token;
    });

    afterEach(async () => {
        // Limpa usuários de teste
        try {
            if (user1?.id) await db.execute('DELETE FROM users WHERE id = ?', [user1.id]);
            if (user2?.id) await db.execute('DELETE FROM users WHERE id = ?', [user2.id]);
        } catch (error) {
            // Ignora erros
        }
    });

    afterAll(async () => {
        await db.end();
    });

    describe('GET /api/users/me', () => {
        it('deve retornar dados do usuário autenticado', async () => {
            const response = await request(app)
                .get('/api/users/me')
                .set('Authorization', `Bearer ${token1}`)
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('id', user1.id);
            expect(response.body).toHaveProperty('name', user1.name);
            expect(response.body).toHaveProperty('pairingCode');
        });

        it('deve rejeitar sem token', async () => {
            await request(app)
                .get('/api/users/me')
                .expect(401);
        });

        it('deve rejeitar com token inválido', async () => {
            await request(app)
                .get('/api/users/me')
                .set('Authorization', 'Bearer token_invalido')
                .expect(401);
        });
    });

    describe('GET /api/users/:id', () => {
        it('deve retornar dados de usuário por ID', async () => {
            const response = await request(app)
                .get(`/api/users/${user2.id}`)
                .set('Authorization', `Bearer ${token1}`)
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('id', user2.id);
            expect(response.body).toHaveProperty('name', user2.name);
        });

        it('deve rejeitar ID inexistente', async () => {
            const response = await request(app)
                .get('/api/users/99999999')
                .set('Authorization', `Bearer ${token1}`)
                .expect(404);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('POST /api/users/pair', () => {
        it('deve parear dois usuários com código válido', async () => {
            const response = await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: user2.pairingCode })
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('message');
            expect(response.body).toHaveProperty('partner');
            expect(response.body.partner).toHaveProperty('id', user2.id);
        });

        it('deve rejeitar pareamento com código inválido', async () => {
            const response = await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: 'CODIGO_INVALIDO' })
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar pareamento com o próprio código', async () => {
            const response = await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: user1.pairingCode })
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar se já estiver pareado', async () => {
            // Primeiro pareamento
            await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: user2.pairingCode });

            // Tenta parear novamente
            const response = await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: user2.pairingCode })
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('DELETE /api/users/unpair', () => {
        beforeEach(async () => {
            // Pareia os usuários antes de cada teste
            await request(app)
                .post('/api/users/pair')
                .set('Authorization', `Bearer ${token1}`)
                .send({ pairingCode: user2.pairingCode });
        });

        it('deve desparear usuários com sucesso', async () => {
            const response = await request(app)
                .delete('/api/users/unpair')
                .set('Authorization', `Bearer ${token1}`)
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('message');
        });

        it('deve rejeitar despareamento se não estiver pareado', async () => {
            // Primeiro despareamento
            await request(app)
                .delete('/api/users/unpair')
                .set('Authorization', `Bearer ${token1}`);

            // Tenta desparear novamente
            const response = await request(app)
                .delete('/api/users/unpair')
                .set('Authorization', `Bearer ${token1}`)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('PUT /api/users/fcm-token', () => {
        it('deve atualizar FCM token com sucesso', async () => {
            const testToken = `fcm_token_${Date.now()}`;

            const response = await request(app)
                .put('/api/users/fcm-token')
                .set('Authorization', `Bearer ${token1}`)
                .send({ fcmToken: testToken })
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('message');
        });

        it('deve rejeitar sem FCM token', async () => {
            const response = await request(app)
                .put('/api/users/fcm-token')
                .set('Authorization', `Bearer ${token1}`)
                .send({})
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });
    });

    describe('GET /api/users/pairing-code/:code', () => {
        it('deve retornar usuário por código de pareamento', async () => {
            const response = await request(app)
                .get(`/api/users/pairing-code/${user2.pairingCode}`)
                .set('Authorization', `Bearer ${token1}`)
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('id', user2.id);
            expect(response.body).toHaveProperty('name', user2.name);
        });

        it('deve rejeitar código inválido', async () => {
            const response = await request(app)
                .get('/api/users/pairing-code/CODIGO_INVALIDO')
                .set('Authorization', `Bearer ${token1}`)
                .expect(404);

            expect(response.body).toHaveProperty('error');
        });
    });
});
