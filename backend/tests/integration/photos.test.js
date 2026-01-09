/**
 * Testes de Integração: Fotos
 * Testa os endpoints de upload e download de fotos
 */

const request = require('supertest');
const express = require('express');
const path = require('path');
const fs = require('fs');
const authRoutes = require('../../src/routes/auth');
const usersRoutes = require('../../src/routes/users');
const photosRoutes = require('../../src/routes/photos');
const db = require('../../src/config/database');

// Cria app de teste
const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/photos', photosRoutes);

describe('Photos API Integration Tests', () => {
    let user1, user2;
    let token1, token2;
    let uploadedPhotoId;

    // Cria imagem de teste (1x1 pixel PNG)
    const testImageBuffer = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
        'base64'
    );

    beforeAll(async () => {
        await new Promise(resolve => setTimeout(resolve, 1000));
    });

    beforeEach(async () => {
        // Cria dois usuários
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

        // Pareia os usuários
        await request(app)
            .post('/api/users/pair')
            .set('Authorization', `Bearer ${token1}`)
            .send({ pairingCode: user2.pairingCode });
    });

    afterEach(async () => {
        // Limpa fotos criadas
        try {
            if (uploadedPhotoId) {
                await db.execute('DELETE FROM photos WHERE id = ?', [uploadedPhotoId]);
                uploadedPhotoId = null;
            }
        } catch (error) {
            // Ignora erros
        }

        // Limpa usuários
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

    describe('POST /api/photos', () => {
        it('deve fazer upload de foto entre usuários pareados', async () => {
            const response = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .attach('photo', testImageBuffer, 'test.png')
                .expect('Content-Type', /json/)
                .expect(201);

            expect(response.body).toHaveProperty('message');
            expect(response.body).toHaveProperty('photo');
            expect(response.body.photo).toHaveProperty('id');
            expect(response.body.photo).toHaveProperty('senderId', user1.id);
            expect(response.body.photo).toHaveProperty('receiverId', user2.id);

            uploadedPhotoId = response.body.photo.id;
        }, 15000); // Aumenta timeout para upload

        it('deve rejeitar upload sem arquivo', async () => {
            const response = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .expect(400);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar upload entre usuários não pareados', async () => {
            // Cria um terceiro usuário não pareado
            const name3 = `User3_${Date.now()}_${Math.random().toString(36).substring(7)}`;
            const response3 = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: name3 });

            const user3 = response3.body.user;
            const token3 = response3.body.token;

            const response = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token3}`)
                .attach('photo', testImageBuffer, 'test.png')
                .expect(400);

            expect(response.body).toHaveProperty('error');

            // Cleanup user3
            await db.execute('DELETE FROM users WHERE id = ?', [user3.id]);
        }, 15000);

        it('deve rejeitar arquivo muito grande', async () => {
            // Cria buffer de 6MB (limite é 5MB)
            const largeBuffer = Buffer.alloc(6 * 1024 * 1024);

            const response = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .attach('photo', largeBuffer, 'large.png')
                .expect(400);

            expect(response.body).toHaveProperty('error');
        }, 15000);

        it('deve rejeitar tipo de arquivo inválido', async () => {
            // Cria um arquivo .txt
            const txtBuffer = Buffer.from('This is not an image');

            const response = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .attach('photo', txtBuffer, 'test.txt')
                .expect(400);

            expect(response.body).toHaveProperty('error');
        }, 15000);
    });

    describe('GET /api/photos/latest', () => {
        beforeEach(async () => {
            // Upload uma foto para os testes
            const uploadResponse = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .attach('photo', testImageBuffer, 'test.png');

            uploadedPhotoId = uploadResponse.body.photo.id;
        });

        it('deve retornar última foto recebida', async () => {
            // User2 busca foto enviada por User1
            const response = await request(app)
                .get('/api/photos/latest')
                .set('Authorization', `Bearer ${token2}`)
                .expect('Content-Type', /json/)
                .expect(200);

            expect(response.body).toHaveProperty('id', uploadedPhotoId);
            expect(response.body).toHaveProperty('senderId', user1.id);
            expect(response.body).toHaveProperty('receiverId', user2.id);
            expect(response.body).toHaveProperty('imageData');
        }, 15000);

        it('deve retornar 404 se não houver foto', async () => {
            // Cria usuário novo sem fotos
            const name3 = `User3_${Date.now()}_${Math.random().toString(36).substring(7)}`;
            const response3 = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: name3 });

            const token3 = response3.body.token;

            const response = await request(app)
                .get('/api/photos/latest')
                .set('Authorization', `Bearer ${token3}`)
                .expect(404);

            expect(response.body).toHaveProperty('error');

            // Cleanup
            await db.execute('DELETE FROM users WHERE id = ?', [response3.body.user.id]);
        });
    });

    describe('GET /api/photos/:id/image', () => {
        beforeEach(async () => {
            // Upload uma foto para os testes
            const uploadResponse = await request(app)
                .post('/api/photos')
                .set('Authorization', `Bearer ${token1}`)
                .attach('photo', testImageBuffer, 'test.png');

            uploadedPhotoId = uploadResponse.body.photo.id;
        });

        it('deve retornar imagem por ID', async () => {
            const response = await request(app)
                .get(`/api/photos/${uploadedPhotoId}/image`)
                .set('Authorization', `Bearer ${token2}`)
                .expect('Content-Type', /image/)
                .expect(200);

            expect(response.body).toBeInstanceOf(Buffer);
            expect(response.body.length).toBeGreaterThan(0);
        }, 15000);

        it('deve rejeitar ID inexistente', async () => {
            const response = await request(app)
                .get('/api/photos/99999999/image')
                .set('Authorization', `Bearer ${token1}`)
                .expect(404);

            expect(response.body).toHaveProperty('error');
        });

        it('deve rejeitar acesso de usuário não autorizado', async () => {
            // Cria terceiro usuário não relacionado
            const name3 = `User3_${Date.now()}_${Math.random().toString(36).substring(7)}`;
            const response3 = await request(app)
                .post('/api/auth/register-simple')
                .send({ name: name3 });

            const token3 = response3.body.token;

            const response = await request(app)
                .get(`/api/photos/${uploadedPhotoId}/image`)
                .set('Authorization', `Bearer ${token3}`)
                .expect(403);

            expect(response.body).toHaveProperty('error');

            // Cleanup
            await db.execute('DELETE FROM users WHERE id = ?', [response3.body.user.id]);
        }, 15000);
    });
});
