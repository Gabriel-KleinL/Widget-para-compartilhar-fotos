const express = require('express');
const cors = require('cors');
const multer = require('multer');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const photoRoutes = require('./routes/photos');

const app = express();
const PORT = process.env.PORT || 3000;

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rotas
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/photos', photoRoutes);

// Rota de health check
app.get('/health', (req, res) => {
    res.json({ status: 'OK', message: 'API Viva Comigo está funcionando' });
});

// Middleware de tratamento de erros
app.use((err, req, res, next) => {
    console.error('Erro:', err);
    
    if (err instanceof multer.MulterError) {
        if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({ error: 'Arquivo muito grande. Máximo 10MB' });
        }
    }
    
    res.status(err.status || 500).json({
        error: err.message || 'Erro interno do servidor'
    });
});

app.listen(PORT, () => {
    console.log(`🚀 Servidor rodando na porta ${PORT}`);
    console.log(`📡 API disponível em http://localhost:${PORT}`);
});

