const express = require('express');
const cors = require('cors');
const multer = require('multer');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const compression = require('compression');
require('dotenv').config();

const logger = require('./config/logger');
const { swaggerSpec, swaggerUi, swaggerUiOptions } = require('./config/swagger');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const photoRoutes = require('./routes/photos');

const app = express();
const PORT = process.env.PORT || 3000;

// Middlewares de segurança
app.use(helmet()); // Adiciona headers de segurança
app.use(compression()); // Comprime respostas

// Rate limiting
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutos
    max: 100, // Máximo de 100 requisições por IP
    message: 'Muitas requisições deste IP, tente novamente mais tarde.'
});

const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutos
    max: 5, // Máximo de 5 tentativas de login
    message: 'Muitas tentativas de login, tente novamente mais tarde.'
});

app.use('/api/', limiter);
app.use('/api/auth/', authLimiter);

// Middlewares de parsing
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Middleware de logging
app.use(logger.httpLogger);

// Swagger Documentation
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec, swaggerUiOptions));
app.get('/api-docs.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send(swaggerSpec);
});

// Rotas
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/photos', photoRoutes);

// Rota de health check
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        message: 'API Viva Comigo está funcionando',
        timestamp: new Date().toISOString(),
        docs: `${req.protocol}://${req.get('host')}/api-docs`
    });
});

// Middleware de tratamento de erros
app.use((err, req, res, next) => {
    logger.logError(err, {
        method: req.method,
        url: req.originalUrl || req.url,
        userId: req.user?.id
    });
    
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
    logger.info(`🚀 Servidor rodando na porta ${PORT}`);
    logger.info(`📡 API disponível em http://localhost:${PORT}`);
    logger.info(`🌍 Ambiente: ${process.env.NODE_ENV || 'development'}`);
});

