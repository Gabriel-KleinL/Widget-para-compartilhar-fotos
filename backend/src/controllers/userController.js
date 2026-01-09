const db = require('../config/database');
const User = require('../models/User');
const authMiddleware = require('../middleware/authMiddleware');

// Obter usuário atual
async function getCurrentUser(req, res) {
    try {
        const [users] = await db.execute(
            'SELECT * FROM users WHERE id = ?',
            [req.user.id]
        );

        if (users.length === 0) {
            return res.status(404).json({ error: 'Usuário não encontrado' });
        }

        const user = User.fromRow(users[0]);
        res.json(user.toJSON());
    } catch (error) {
        console.error('Erro ao buscar usuário:', error);
        res.status(500).json({ error: 'Erro ao buscar usuário' });
    }
}

// Obter usuário por ID
async function getUserById(req, res) {
    try {
        const { id } = req.params;

        const [users] = await db.execute(
            'SELECT * FROM users WHERE id = ?',
            [id]
        );

        if (users.length === 0) {
            return res.status(404).json({ error: 'Usuário não encontrado' });
        }

        const user = User.fromRow(users[0]);
        res.json(user.toJSON());
    } catch (error) {
        console.error('Erro ao buscar usuário:', error);
        res.status(500).json({ error: 'Erro ao buscar usuário' });
    }
}

// Buscar usuário por código de pareamento
async function getUserByPairingCode(req, res) {
    try {
        const { code } = req.params;

        const [users] = await db.execute(
            'SELECT * FROM users WHERE pairing_code = ?',
            [code.toUpperCase()]
        );

        if (users.length === 0) {
            return res.status(404).json({ error: 'Código de pareamento não encontrado' });
        }

        const user = User.fromRow(users[0]);
        res.json(user.toJSON());
    } catch (error) {
        console.error('Erro ao buscar usuário por código:', error);
        res.status(500).json({ error: 'Erro ao buscar usuário' });
    }
}

// Parear usuários
async function pairUsers(req, res) {
    try {
        const { partnerCode } = req.body;
        const currentUserId = req.user.id;

        if (!partnerCode) {
            return res.status(400).json({ error: 'Código de pareamento é obrigatório' });
        }

        // Buscar parceiro pelo código
        const [partnerUsers] = await db.execute(
            'SELECT * FROM users WHERE pairing_code = ?',
            [partnerCode.toUpperCase()]
        );

        if (partnerUsers.length === 0) {
            return res.status(404).json({ error: 'Código de pareamento não encontrado' });
        }

        const partner = User.fromRow(partnerUsers[0]);

        if (partner.id === currentUserId) {
            return res.status(400).json({ error: 'Você não pode parear consigo mesmo!' });
        }

        // Atualizar pareamento (transação)
        const connection = await db.getConnection();
        try {
            await connection.beginTransaction();

            // Atualizar usuário atual
            await connection.execute(
                'UPDATE users SET partner_id = ? WHERE id = ?',
                [partner.id, currentUserId]
            );

            // Atualizar parceiro
            await connection.execute(
                'UPDATE users SET partner_id = ? WHERE id = ?',
                [currentUserId, partner.id]
            );

            await connection.commit();

            // Buscar usuário atualizado
            const [updatedUsers] = await db.execute(
                'SELECT * FROM users WHERE id = ?',
                [currentUserId]
            );

            const updatedUser = User.fromRow(updatedUsers[0]);
            res.json(updatedUser.toJSON());
        } catch (error) {
            await connection.rollback();
            throw error;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error('Erro ao parear usuários:', error);
        res.status(500).json({ error: 'Erro ao parear usuários' });
    }
}

// Desparear usuários
async function unpairUsers(req, res) {
    try {
        const currentUserId = req.user.id;

        // Buscar parceiro atual
        const [users] = await db.execute(
            'SELECT partner_id FROM users WHERE id = ?',
            [currentUserId]
        );

        if (users.length === 0) {
            return res.status(404).json({ error: 'Usuário não encontrado' });
        }

        const partnerId = users[0].partner_id;

        if (!partnerId) {
            return res.status(400).json({ error: 'Você não está pareado com ninguém' });
        }

        // Desparear (transação)
        const connection = await db.getConnection();
        try {
            await connection.beginTransaction();

            // Remover parceiro do usuário atual
            await connection.execute(
                'UPDATE users SET partner_id = NULL WHERE id = ?',
                [currentUserId]
            );

            // Remover parceiro do outro usuário
            await connection.execute(
                'UPDATE users SET partner_id = NULL WHERE id = ?',
                [partnerId]
            );

            await connection.commit();

            // Buscar usuário atualizado
            const [updatedUsers] = await db.execute(
                'SELECT * FROM users WHERE id = ?',
                [currentUserId]
            );

            const updatedUser = User.fromRow(updatedUsers[0]);
            res.json(updatedUser.toJSON());
        } catch (error) {
            await connection.rollback();
            throw error;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error('Erro ao desparear usuários:', error);
        res.status(500).json({ error: 'Erro ao desparear usuários' });
    }
}

// Atualizar FCM token
async function updateFcmToken(req, res) {
    try {
        const userId = req.user.id;
        const { fcmToken } = req.body;

        if (!fcmToken) {
            return res.status(400).json({ error: 'FCM token é obrigatório' });
        }

        await db.execute(
            'UPDATE users SET fcm_token = ? WHERE id = ?',
            [fcmToken, userId]
        );

        res.json({ message: 'FCM token atualizado com sucesso' });
    } catch (error) {
        console.error('Erro ao atualizar FCM token:', error);
        res.status(500).json({ error: 'Erro ao atualizar FCM token' });
    }
}

module.exports = {
    getCurrentUser,
    getUserById,
    getUserByPairingCode,
    pairUsers,
    unpairUsers,
    updateFcmToken
};

