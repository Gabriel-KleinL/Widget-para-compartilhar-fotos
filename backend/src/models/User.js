const { v4: uuidv4 } = require('uuid');

class User {
    constructor(data) {
        this.id = data.id || uuidv4();
        this.email = data.email || '';
        this.password_hash = data.password_hash || '';
        this.pairing_code = data.pairing_code || '';
        this.partner_id = data.partner_id || null;
        this.display_name = data.display_name || '';
        this.created_at = data.created_at || null;
        this.updated_at = data.updated_at || null;
    }

    toJSON() {
        return {
            id: this.id,
            email: this.email,
            pairing_code: this.pairing_code,
            partner_id: this.partner_id,
            display_name: this.display_name,
            created_at: this.created_at,
            updated_at: this.updated_at
        };
    }

    static fromRow(row) {
        return new User({
            id: row.id,
            email: row.email,
            password_hash: row.password_hash,
            pairing_code: row.pairing_code,
            partner_id: row.partner_id,
            display_name: row.display_name,
            created_at: row.created_at,
            updated_at: row.updated_at
        });
    }
}

module.exports = User;

