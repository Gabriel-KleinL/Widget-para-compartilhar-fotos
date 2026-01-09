const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/authMiddleware');
const photoController = require('../controllers/photoController');
const { uploadLimiter } = require('../middleware/uploadRateLimiter');

// Todas as rotas requerem autenticação
router.use(authMiddleware);

// Upload com rate limiting específico (10 uploads/hora)
router.post('/', uploadLimiter, photoController.upload, photoController.uploadPhoto);
router.get('/latest', photoController.getLatestPhoto);
router.get('/', photoController.getPhotos);
router.get('/:id/image', photoController.getPhotoImage);
router.put('/:id/seen', photoController.markPhotoAsSeen);

module.exports = router;

