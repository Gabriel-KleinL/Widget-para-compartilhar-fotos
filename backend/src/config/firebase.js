const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Caminho para a chave privada do Firebase
const serviceAccountPath = path.join(__dirname, '../../firebase-admin-key.json');

// Verificar se o arquivo existe
if (!fs.existsSync(serviceAccountPath)) {
    console.warn('⚠️  firebase-admin-key.json não encontrado. Push notifications não funcionarão.');
    console.warn('   Para configurar: Firebase Console → Configurações → Contas de serviço → Gerar nova chave privada');
    module.exports = null;
} else {
    try {
        const serviceAccount = require(serviceAccountPath);
        
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });
        
        console.log('✅ Firebase Admin inicializado com sucesso');
        module.exports = admin;
    } catch (error) {
        console.error('❌ Erro ao inicializar Firebase Admin:', error.message);
        module.exports = null;
    }
}
