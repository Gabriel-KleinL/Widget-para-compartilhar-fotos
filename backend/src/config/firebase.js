const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

let serviceAccount = null;

// Tentar carregar da variável de ambiente primeiro (produção)
if (process.env.FIREBASE_ADMIN_KEY) {
    try {
        serviceAccount = JSON.parse(process.env.FIREBASE_ADMIN_KEY);
        console.log('✅ Credenciais Firebase carregadas da variável de ambiente');
    } catch (error) {
        console.error('❌ Erro ao parsear FIREBASE_ADMIN_KEY:', error.message);
    }
}

// Se não encontrou na variável de ambiente, tentar arquivo local (desenvolvimento)
if (!serviceAccount) {
    const serviceAccountPath = path.join(__dirname, '../../firebase-admin-key.json');

    if (fs.existsSync(serviceAccountPath)) {
        try {
            serviceAccount = require(serviceAccountPath);
            console.log('✅ Credenciais Firebase carregadas do arquivo local');
        } catch (error) {
            console.error('❌ Erro ao carregar firebase-admin-key.json:', error.message);
        }
    }
}

// Inicializar Firebase Admin
if (!serviceAccount) {
    console.warn('⚠️  Firebase não configurado. Push notifications não funcionarão.');
    console.warn('   Desenvolvimento: Adicione firebase-admin-key.json na raiz do projeto');
    console.warn('   Produção: Configure variável FIREBASE_ADMIN_KEY no Render.com');
    module.exports = null;
} else {
    try {
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
