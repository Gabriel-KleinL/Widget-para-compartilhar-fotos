const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/authMiddleware');
const userController = require('../controllers/userController');

// Todas as rotas requerem autenticação
router.use(authMiddleware);

router.get('/me', userController.getCurrentUser);
router.get('/:id', userController.getUserById);
router.get('/pairing-code/:code', userController.getUserByPairingCode);
router.post('/pair', userController.pairUsers);
router.delete('/unpair', userController.unpairUsers);
router.put('/fcm-token', userController.updateFcmToken);

module.exports = router;

