const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/database');
const User = require('../models/User');

// Gerar código de pareamento aleatório
function generatePairingCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
}

// Registro de novo usuário
async function register(req, res) {
    try {
        const { email, password, display_name } = req.body;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email e senha são obrigatórios' });
        }

        // Verificar se o email já existe
        const [existingUsers] = await db.execute(
            'SELECT id FROM users WHERE email = ?',
            [email]
        );

        if (existingUsers.length > 0) {
            return res.status(400).json({ error: 'Email já está em uso' });
        }

        // Hash da senha
        const password_hash = await bcrypt.hash(password, 10);

        // Gerar código de pareamento único
        let pairing_code;
        let isUnique = false;
        while (!isUnique) {
            pairing_code = generatePairingCode();
            const [codes] = await db.execute(
                'SELECT id FROM users WHERE pairing_code = ?',
                [pairing_code]
            );
            if (codes.length === 0) {
                isUnique = true;
            }
        }

        // Criar usuário
        const user = new User({
            email,
            password_hash,
            pairing_code,
            display_name: display_name || email.substring(0, email.indexOf('@'))
        });

        await db.execute(
            'INSERT INTO users (id, email, password_hash, pairing_code, display_name) VALUES (?, ?, ?, ?, ?)',
            [user.id, user.email, user.password_hash, user.pairing_code, user.display_name]
        );

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, email: user.email },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
        );

        res.status(201).json({
            token,
            user: user.toJSON()
        });
    } catch (error) {
        console.error('Erro no registro:', error);
        res.status(500).json({ error: 'Erro ao criar usuário' });
    }
}

// Login
async function login(req, res) {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email e senha são obrigatórios' });
        }

        // Buscar usuário
        const [users] = await db.execute(
            'SELECT * FROM users WHERE email = ?',
            [email]
        );

        if (users.length === 0) {
            return res.status(401).json({ error: 'Email ou senha inválidos' });
        }

        const user = User.fromRow(users[0]);

        // Verificar senha
        const isValidPassword = await bcrypt.compare(password, user.password_hash);

        if (!isValidPassword) {
            return res.status(401).json({ error: 'Email ou senha inválidos' });
        }

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, email: user.email },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
        );

        res.json({
            token,
            user: user.toJSON()
        });
    } catch (error) {
        console.error('Erro no login:', error);
        res.status(500).json({ error: 'Erro ao fazer login' });
    }
}

module.exports = {
    register,
    login
};

