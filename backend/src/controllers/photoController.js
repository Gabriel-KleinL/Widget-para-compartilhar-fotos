const db = require('../config/database');
const Photo = require('../models/Photo');
const multer = require('multer');

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

        // Criar foto
        const photo = new Photo({
            sender_id: senderId,
            receiver_id: receiverId,
            image_data: req.file.buffer,
            timestamp: Date.now()
        });

        await db.execute(
            'INSERT INTO photos (id, sender_id, receiver_id, image_data, timestamp, seen) VALUES (?, ?, ?, ?, ?, ?)',
            [photo.id, photo.sender_id, photo.receiver_id, photo.image_data, photo.timestamp, photo.seen]
        );

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

