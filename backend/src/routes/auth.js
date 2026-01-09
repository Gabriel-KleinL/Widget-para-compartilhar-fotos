const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

// Endpoints antigos (com senha) - manter para compatibilidade
router.post('/register', authController.register);
router.post('/login', authController.login);

// Endpoints novos (SEM senha) - RECOMENDADOS
router.post('/register-simple', authController.registerSimple);     // Registrar só com nome
router.post('/login-simple', authController.loginSimple);           // Login só com nome
router.post('/login-code', authController.loginByPairingCode);      // Login com código de pareamento

module.exports = router;

