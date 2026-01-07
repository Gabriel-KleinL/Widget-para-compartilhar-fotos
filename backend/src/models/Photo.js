const { v4: uuidv4 } = require('uuid');

class Photo {
    constructor(data) {
        this.id = data.id || uuidv4();
        this.sender_id = data.sender_id || '';
        this.receiver_id = data.receiver_id || '';
        this.image_data = data.image_data || null;
        this.timestamp = data.timestamp || Date.now();
        this.seen = data.seen || false;
        this.created_at = data.created_at || null;
    }

    toJSON() {
        return {
            id: this.id,
            sender_id: this.sender_id,
            receiver_id: this.receiver_id,
            image_url: `/api/photos/${this.id}/image`, // URL para acessar a imagem
            timestamp: this.timestamp,
            seen: this.seen,
            created_at: this.created_at
        };
    }

    static fromRow(row) {
        return new Photo({
            id: row.id,
            sender_id: row.sender_id,
            receiver_id: row.receiver_id,
            image_data: row.image_data,
            timestamp: row.timestamp,
            seen: row.seen,
            created_at: row.created_at
        });
    }
}

module.exports = Photo;

