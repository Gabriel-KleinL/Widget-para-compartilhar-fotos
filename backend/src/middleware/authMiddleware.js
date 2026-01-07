const jwt = require('jsonwebtoken');
const db = require('../config/database');

const authMiddleware = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;
        
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ error: 'Token de autenticação não fornecido' });
        }

        const token = authHeader.substring(7); // Remove "Bearer "

        try {
            const decoded = jwt.verify(token, process.env.JWT_SECRET);
            
            // Verificar se o usuário ainda existe
            const [users] = await db.execute(
                'SELECT id, email FROM users WHERE id = ?',
                [decoded.userId]
            );

            if (users.length === 0) {
                return res.status(401).json({ error: 'Usuário não encontrado' });
            }

            req.user = {
                id: decoded.userId,
                email: decoded.email
            };

            next();
        } catch (error) {
            return res.status(401).json({ error: 'Token inválido ou expirado' });
        }
    } catch (error) {
        console.error('Erro no middleware de autenticação:', error);
        return res.status(500).json({ error: 'Erro interno do servidor' });
    }
};

module.exports = authMiddleware;

