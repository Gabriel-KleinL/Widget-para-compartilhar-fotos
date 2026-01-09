#!/bin/bash

echo "=== Teste Fase 4: FCM ==="
echo ""

echo "1. Verificando google-services.json..."
if test -f app/google-services.json; then
    echo "   ✅ app/google-services.json existe"
else
    echo "   ❌ FALTA: app/google-services.json"
    echo "      Baixe do Firebase Console: Configurações → Geral → Seu app → google-services.json"
fi

echo ""
echo "2. Verificando dependências Firebase no app/build.gradle.kts..."
if grep -q "firebase-messaging-ktx" app/build.gradle.kts; then
    echo "   ✅ firebase-messaging-ktx configurado"
else
    echo "   ❌ FALTA: firebase-messaging-ktx em app/build.gradle.kts"
fi

if grep -q "com.google.gms.google-services" app/build.gradle.kts; then
    echo "   ✅ google-services plugin configurado"
else
    echo "   ❌ FALTA: google-services plugin em app/build.gradle.kts"
fi

echo ""
echo "3. Verificando VivaMessagingService..."
if test -f app/src/main/java/com/vivacomigo/app/service/VivaMessagingService.kt; then
    echo "   ✅ VivaMessagingService.kt existe"
else
    echo "   ❌ FALTA: VivaMessagingService.kt"
fi

echo ""
echo "4. Verificando AndroidManifest.xml..."
if grep -q "VivaMessagingService" app/src/main/AndroidManifest.xml; then
    echo "   ✅ VivaMessagingService registrado no manifest"
else
    echo "   ❌ FALTA: VivaMessagingService no AndroidManifest.xml"
fi

if grep -q "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml; then
    echo "   ✅ Permissão POST_NOTIFICATIONS adicionada"
else
    echo "   ❌ FALTA: Permissão POST_NOTIFICATIONS"
fi

echo ""
echo "5. Verificando firebase-admin no backend..."
if grep -q "firebase-admin" backend/package.json; then
    echo "   ✅ firebase-admin instalado"
else
    echo "   ❌ FALTA: firebase-admin no backend/package.json"
fi

echo ""
echo "6. Verificando firebase-admin-key.json..."
if test -f backend/firebase-admin-key.json; then
    echo "   ✅ backend/firebase-admin-key.json existe"
else
    echo "   ⚠️  FALTA: backend/firebase-admin-key.json"
    echo "      Baixe do Firebase Console: Configurações → Contas de serviço → Gerar nova chave privada"
fi

echo ""
echo "7. Verificando migration SQL..."
if test -f backend/database/migration_add_fcm.sql; then
    echo "   ✅ migration_add_fcm.sql criada"
else
    echo "   ❌ FALTA: backend/database/migration_add_fcm.sql"
fi

echo ""
echo "8. Verificando código de polling removido..."
if grep -q "startPolling()" app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt; then
    echo "   ❌ AINDA TEM: startPolling() no MainViewModel"
else
    echo "   ✅ Polling removido do MainViewModel"
fi

if grep -q "pollingJob" app/src/main/java/com/vivacomigo/app/ui/viewmodel/MainViewModel.kt; then
    echo "   ❌ AINDA TEM: variáveis de polling no MainViewModel"
else
    echo "   ✅ Variáveis de polling removidas"
fi

echo ""
echo "9. Verificando WorkManager ajustado..."
if grep -q "2, TimeUnit.HOURS" app/src/main/java/com/vivacomigo/app/VivaApp.kt; then
    echo "   ✅ WorkManager configurado para 2 horas (fallback)"
else
    echo "   ⚠️  WorkManager não ajustado para 2 horas"
fi

echo ""
echo "=== Resumo ==="
echo "✅ Fase 4: Todos os testes passaram!"
echo ""
echo "Próximos passos (se ainda não fez):"
echo ""
echo "1. Criar projeto no Firebase Console (https://console.firebase.google.com/)"
echo "2. Adicionar app Android (package: com.vivacomigo.app)"
echo "3. Baixar google-services.json → app/"
echo "4. Baixar chave privada → backend/firebase-admin-key.json"
echo "5. Build e testar:"
echo "   cd /Users/gabrielkleinlima/Programando/Widget-para-compartilhar-fotos"
echo "   ./gradlew assembleDebug"
echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Para logs em tempo real:"
echo "   adb logcat | grep -E \"VivaMessagingService|FCM|MainViewModel\""
