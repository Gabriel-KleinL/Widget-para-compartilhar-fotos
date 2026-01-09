const rateLimit = require('express-rate-limit');

/**
 * Rate limiter específico para upload de fotos
 *
 * Limites:
 * - 10 uploads por hora por usuário
 * - Baseado no ID do usuário autenticado (não IP)
 * - Desabilitado em desenvolvimento
 *
 * Motivo: Prevenir spam e abuso de uploads
 * Uso típico de casal: 2-5 fotos/dia (10/hora é generoso)
 */
const uploadLimiter = rateLimit({
    windowMs: 60 * 60 * 1000, // 1 hora
    max: 10, // 10 uploads por hora
    message: {
        error: 'Limite de uploads atingido. Você pode enviar até 10 fotos por hora. Tente novamente mais tarde.'
    },
    standardHeaders: true, // Return rate limit info in `RateLimit-*` headers
    legacyHeaders: false, // Disable `X-RateLimit-*` headers

    // Limitar por usuário autenticado (não por IP)
    keyGenerator: (req) => {
        // Se houver usuário autenticado, usar seu ID
        // Caso contrário, usar IP (fallback)
        return req.user?.id || req.ip;
    },

    // Handler customizado para resposta
    handler: (req, res) => {
        console.log(`⚠️  Rate limit atingido para usuário: ${req.user?.id || req.ip}`);
        res.status(429).json({
            error: 'Limite de uploads atingido',
            message: 'Você pode enviar até 10 fotos por hora. Tente novamente mais tarde.',
            retryAfter: req.rateLimit.resetTime
        });
    },

    // Pular rate limiting em desenvolvimento
    skip: (req) => {
        const isDevelopment = process.env.NODE_ENV === 'development';
        if (isDevelopment) {
            console.log('🔧 Rate limiting desabilitado (desenvolvimento)');
        }
        return isDevelopment;
    },

    // Configuração de armazenamento (usa memória por padrão)
    // Em produção, considere usar Redis para múltiplas instâncias
    // store: new RedisStore({...})
});

/**
 * Rate limiter mais restritivo para tentativas de upload suspeitas
 * Ativa após múltiplas falhas de validação
 */
const strictUploadLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutos
    max: 5, // 5 tentativas por 15 minutos
    message: {
        error: 'Muitas tentativas de upload inválidas. Conta temporariamente bloqueada.'
    },
    keyGenerator: (req) => req.user?.id || req.ip,
    skip: (req) => process.env.NODE_ENV === 'development'
});

module.exports = {
    uploadLimiter,
    strictUploadLimiter
};
