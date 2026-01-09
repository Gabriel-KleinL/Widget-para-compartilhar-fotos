const db = require('../config/database');
const Photo = require('../models/Photo');
const multer = require('multer');
const admin = require('../config/firebase');
const sharp = require('sharp');
const FileType = require('file-type');

// Configurar multer para armazenar em memória
const storage = multer.memoryStorage();
const upload = multer({
    storage: storage,
    limits: {
        fileSize: 10 * 1024 * 1024 // 10MB
    },
    fileFilter: (req, file, cb) => {
        if (file.mimetype.startsWith('image/')) {
            cb(null, true);
        } else {
            cb(new Error('Apenas imagens são permitidas'), false);
        }
    }
});

// Upload de foto
async function uploadPhoto(req, res) {
    try {
        const senderId = req.user.id;
        const { receiverId } = req.body;

        if (!receiverId) {
            return res.status(400).json({ error: 'ID do destinatário é obrigatório' });
        }

        if (!req.file) {
            return res.status(400).json({ error: 'Imagem é obrigatória' });
        }

        // Verificar se o destinatário existe e se é parceiro
        const [users] = await db.execute(
            'SELECT partner_id FROM users WHERE id = ?',
            [senderId]
        );

        if (users.length === 0) {
            return res.status(404).json({ error: 'Usuário não encontrado' });
        }

        if (users[0].partner_id !== receiverId) {
            return res.status(403).json({ error: 'Você só pode enviar fotos para seu parceiro' });
        }

        // ===== FASE 5: VALIDAÇÕES DE SEGURANÇA =====

        // 1. Validar magic bytes (tipo real do arquivo)
        const fileType = await FileType.fromBuffer(req.file.buffer);
        if (!fileType) {
            return res.status(400).json({ error: 'Tipo de arquivo não reconhecido' });
        }

        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png'];
        if (!allowedTypes.includes(fileType.mime)) {
            return res.status(400).json({
                error: `Apenas JPEG e PNG são permitidos. Recebido: ${fileType.mime}`
            });
        }

        // 2. Obter metadata e validar dimensões
        let metadata;
        try {
            metadata = await sharp(req.file.buffer).metadata();
        } catch (error) {
            console.error('Erro ao ler metadata da imagem:', error);
            return res.status(400).json({ error: 'Imagem corrompida ou inválida' });
        }

        const MAX_WIDTH = 4096;
        const MAX_HEIGHT = 4096;
        const MAX_PIXELS = 16_000_000; // 16 megapixels

        if (metadata.width > MAX_WIDTH || metadata.height > MAX_HEIGHT) {
            return res.status(400).json({
                error: `Imagem muito grande. Máximo: ${MAX_WIDTH}x${MAX_HEIGHT}px. Recebido: ${metadata.width}x${metadata.height}px`
            });
        }

        if (metadata.width * metadata.height > MAX_PIXELS) {
            return res.status(400).json({
                error: `Imagem tem muitos pixels. Máximo: 16MP. Recebido: ${(metadata.width * metadata.height / 1_000_000).toFixed(1)}MP`
            });
        }

        // 3. Processar imagem: remover EXIF, otimizar, redimensionar se necessário
        let processedImageBuffer;
        try {
            const originalSize = req.file.buffer.length;
            console.log(`📸 Processando imagem: ${metadata.width}x${metadata.height} (${(originalSize / 1024).toFixed(0)}KB)`);

            processedImageBuffer = await sharp(req.file.buffer)
                .rotate() // Auto-rotaciona baseado em EXIF antes de remover
                .resize(1920, 1920, {
                    fit: 'inside',
                    withoutEnlargement: true
                })
                .jpeg({
                    quality: 85,
                    progressive: true
                })
                .toBuffer();

            const processedSize = processedImageBuffer.length;
            const reduction = ((1 - processedSize / originalSize) * 100).toFixed(0);
            console.log(`✅ Imagem processada: ${(processedSize / 1024).toFixed(0)}KB (redução de ${reduction}%)`);
        } catch (error) {
            console.error('Erro ao processar imagem:', error);
            return res.status(500).json({ error: 'Erro ao processar imagem' });
        }

        // Criar foto (com imagem processada)
        const photo = new Photo({
            sender_id: senderId,
            receiver_id: receiverId,
            image_data: processedImageBuffer, // Usa imagem processada (sem EXIF, otimizada)
            timestamp: Date.now()
        });

        await db.execute(
            'INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen) VALUES (?, ?, ?, ?, ?, ?)',
            [photo.id, photo.sender_id, photo.receiver_id, photo.image_data, photo.timestamp, photo.seen]
        );

        // Enviar push notification para o receiver
        if (admin) {
            try {
                // Buscar FCM token do receiver
                const [receivers] = await db.execute(
                    'SELECT fcm_token FROM users WHERE id = ?',
                    [receiverId]
                );

                if (receivers.length > 0 && receivers[0].fcm_token) {
                    const fcmToken = receivers[0].fcm_token;
                    
                    // Enviar notificação push
                    await admin.messaging().send({
                        token: fcmToken,
                        notification: {
                            title: "Nova foto recebida! ❤️",
                            body: "Você recebeu uma nova foto especial"
                        },
                        data: {
                            photo_id: photo.id,
                            sender_id: senderId,
                            type: 'new_photo'
                        },
                        android: {
                            priority: 'high'
                        }
                    });
                    console.log('✅ Push notification enviada com sucesso para', receiverId);
                }
            } catch (error) {
                console.error('⚠️  Erro ao enviar push notification:', error.message);
                // Não falhar o upload se push falhar
            }
        }

        res.status(201).json(photo.toJSON());
    } catch (error) {
        console.error('Erro ao fazer upload da foto:', error);
        res.status(500).json({ error: 'Erro ao fazer upload da foto' });
    }
}

// Obter imagem da foto
async function getPhotoImage(req, res) {
    try {
        const { id } = req.params;
        const userId = req.user.id;

        const [photos] = await db.execute(
            'SELECT image_data, sender_id, receiver_id FROM photos WHERE id = ?',
            [id]
        );

        if (photos.length === 0) {
            return res.status(404).json({ error: 'Foto não encontrada' });
        }

        const photo = photos[0];

        // Verificar se o usuário tem permissão (é o remetente ou destinatário)
        if (photo.sender_id !== userId && photo.receiver_id !== userId) {
            return res.status(403).json({ error: 'Acesso negado' });
        }

        res.setHeader('Content-Type', 'image/jpeg');
        res.send(photo.image_data);
    } catch (error) {
        console.error('Erro ao buscar imagem:', error);
        res.status(500).json({ error: 'Erro ao buscar imagem' });
    }
}

// Obter última foto recebida
async function getLatestPhoto(req, res) {
    try {
        const userId = req.user.id;

        const [photos] = await db.execute(
            'SELECT id, sender_id, receiver_id, timestamp, seen, created_at FROM photos WHERE receiver_id = ? ORDER BY timestamp DESC LIMIT 1',
            [userId]
        );

        if (photos.length === 0) {
            return res.json(null);
        }

        const photo = Photo.fromRow(photos[0]);
        res.json(photo.toJSON());
    } catch (error) {
        console.error('Erro ao buscar última foto:', error);
        res.status(500).json({ error: 'Erro ao buscar última foto' });
    }
}

// Listar fotos do usuário
async function getPhotos(req, res) {
    try {
        const userId = req.user.id;

        const [photos] = await db.execute(
            'SELECT id, sender_id, receiver_id, timestamp, seen, created_at FROM photos WHERE receiver_id = ? ORDER BY timestamp DESC',
            [userId]
        );

        const photosList = photos.map(row => {
            const photo = Photo.fromRow(row);
            return photo.toJSON();
        });

        res.json(photosList);
    } catch (error) {
        console.error('Erro ao listar fotos:', error);
        res.status(500).json({ error: 'Erro ao listar fotos' });
    }
}

// Marcar foto como vista
async function markPhotoAsSeen(req, res) {
    try {
        const { id } = req.params;
        const userId = req.user.id;

        // Verificar se a foto existe e pertence ao usuário
        const [photos] = await db.execute(
            'SELECT receiver_id FROM photos WHERE id = ?',
            [id]
        );

        if (photos.length === 0) {
            return res.status(404).json({ error: 'Foto não encontrada' });
        }

        if (photos[0].receiver_id !== userId) {
            return res.status(403).json({ error: 'Acesso negado' });
        }

        await db.execute(
            'UPDATE photos SET seen = TRUE WHERE id = ?',
            [id]
        );

        res.json({ message: 'Foto marcada como vista' });
    } catch (error) {
        console.error('Erro ao marcar foto como vista:', error);
        res.status(500).json({ error: 'Erro ao marcar foto como vista' });
    }
}

module.exports = {
    uploadPhoto,
    getPhotoImage,
    getLatestPhoto,
    getPhotos,
    markPhotoAsSeen,
    upload: upload.single('image')
};

