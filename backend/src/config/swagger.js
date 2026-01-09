/**
 * Configuração do Swagger (OpenAPI)
 * Documentação interativa da API
 */

const swaggerJsdoc = require('swagger-jsdoc');
const swaggerUi = require('swagger-ui-express');

// Definição básica da API
const swaggerDefinition = {
    openapi: '3.0.0',
    info: {
        title: 'Viva Comigo API',
        version: '1.0.0',
        description: 'API REST para o aplicativo Viva Comigo - Widget para compartilhar fotos entre casais',
        contact: {
            name: 'Equipe Viva Comigo',
            email: 'contato@vivacomigo.app'
        },
        license: {
            name: 'MIT',
            url: 'https://opensource.org/licenses/MIT'
        }
    },
    servers: [
        {
            url: 'http://localhost:3000',
            description: 'Servidor de Desenvolvimento'
        },
        {
            url: 'https://viva-comigo-backend.onrender.com',
            description: 'Servidor de Produção'
        }
    ],
    components: {
        securitySchemes: {
            bearerAuth: {
                type: 'http',
                scheme: 'bearer',
                bearerFormat: 'JWT',
                description: 'Token JWT obtido via login'
            }
        },
        schemas: {
            User: {
                type: 'object',
                properties: {
                    id: { type: 'integer', example: 1 },
                    name: { type: 'string', example: 'João Silva' },
                    pairingCode: { type: 'string', example: 'ABC123' },
                    partnerId: { type: 'integer', nullable: true, example: 2 },
                    createdAt: { type: 'string', format: 'date-time' }
                }
            },
            Photo: {
                type: 'object',
                properties: {
                    id: { type: 'integer', example: 1 },
                    senderId: { type: 'integer', example: 1 },
                    receiverId: { type: 'integer', example: 2 },
                    imageData: { type: 'string', format: 'base64', description: 'Imagem em Base64' },
                    createdAt: { type: 'string', format: 'date-time' }
                }
            },
            Error: {
                type: 'object',
                properties: {
                    error: { type: 'string', example: 'Mensagem de erro' }
                }
            }
        }
    },
    security: [{
        bearerAuth: []
    }]
};

// Opções do Swagger JSDoc
const options = {
    swaggerDefinition,
    // Paths para os arquivos que contêm anotações Swagger
    apis: [
        './src/routes/*.js',
        './src/controllers/*.js'
    ]
};

// Gera a spec do Swagger
const swaggerSpec = swaggerJsdoc(options);

// Opções customizadas para Swagger UI
const swaggerUiOptions = {
    customCss: '.swagger-ui .topbar { display: none }',
    customSiteTitle: 'Viva Comigo API Docs',
    customfavIcon: '/favicon.ico'
};

module.exports = {
    swaggerSpec,
    swaggerUi,
    swaggerUiOptions
};
