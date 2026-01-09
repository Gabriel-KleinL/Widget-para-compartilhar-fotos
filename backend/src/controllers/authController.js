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

// Registro simplificado - SÓ COM NOME (sem senha)
async function registerSimple(req, res) {
    try {
        const { display_name } = req.body;

        if (!display_name || display_name.trim() === '') {
            return res.status(400).json({ error: 'Nome é obrigatório' });
        }

        // Gerar código de pareamento único
        let pairing_code;
        let isUnique = false;
        let attempts = 0;

        while (!isUnique && attempts < 10) {
            pairing_code = generatePairingCode();
            const [codes] = await db.execute(
                'SELECT id FROM users WHERE pairing_code = ?',
                [pairing_code]
            );
            if (codes.length === 0) {
                isUnique = true;
            }
            attempts++;
        }

        if (!isUnique) {
            return res.status(500).json({ error: 'Erro ao gerar código único' });
        }

        // Criar usuário (sem email e password)
        const user = new User({
            pairing_code,
            display_name: display_name.trim()
        });

        await db.execute(
            'INSERT INTO users (id, pairing_code, display_name) VALUES (?, ?, ?)',
            [user.id, user.pairing_code, user.display_name]
        );

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, display_name: user.display_name },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '30d' }
        );

        res.status(201).json({
            token,
            user: {
                id: user.id,
                pairing_code: user.pairing_code,
                display_name: user.display_name,
                partner_id: null
            }
        });
    } catch (error) {
        console.error('Erro no registro simplificado:', error);
        res.status(500).json({ error: 'Erro ao criar usuário' });
    }
}

// Login simplificado - SÓ COM NOME (sem senha)
async function loginSimple(req, res) {
    try {
        const { display_name } = req.body;

        if (!display_name || display_name.trim() === '') {
            return res.status(400).json({ error: 'Nome é obrigatório' });
        }

        // Buscar usuário por display_name
        const [users] = await db.execute(
            'SELECT * FROM users WHERE display_name = ?',
            [display_name.trim()]
        );

        if (users.length === 0) {
            return res.status(401).json({ error: 'Usuário não encontrado' });
        }

        if (users.length > 1) {
            return res.status(400).json({
                error: 'Múltiplos usuários com este nome. Use o código de pareamento para login.',
                suggestion: 'use_pairing_code'
            });
        }

        const user = User.fromRow(users[0]);

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, display_name: user.display_name },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '30d' }
        );

        res.json({
            token,
            user: user.toJSON()
        });
    } catch (error) {
        console.error('Erro no login simplificado:', error);
        res.status(500).json({ error: 'Erro ao fazer login' });
    }
}

// Login por código de pareamento (alternativa)
async function loginByPairingCode(req, res) {
    try {
        const { pairing_code } = req.body;

        if (!pairing_code || pairing_code.trim() === '') {
            return res.status(400).json({ error: 'Código de pareamento é obrigatório' });
        }

        // Buscar usuário por pairing_code
        const [users] = await db.execute(
            'SELECT * FROM users WHERE pairing_code = ?',
            [pairing_code.toUpperCase().trim()]
        );

        if (users.length === 0) {
            return res.status(401).json({ error: 'Código inválido' });
        }

        const user = User.fromRow(users[0]);

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, display_name: user.display_name },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '30d' }
        );

        res.json({
            token,
            user: user.toJSON()
        });
    } catch (error) {
        console.error('Erro no login por código:', error);
        res.status(500).json({ error: 'Erro ao fazer login' });
    }
}

module.exports = {
    register,              // Antigo (com senha) - manter compatibilidade
    login,                 // Antigo (com senha) - manter compatibilidade
    registerSimple,        // NOVO - só nome
    loginSimple,           // NOVO - só nome
    loginByPairingCode     // NOVO - por código
};

