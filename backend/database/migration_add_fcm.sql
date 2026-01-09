-- Migration: Adicionar campo fcm_token para Push Notifications
-- Data: 2026-01-08
-- Fase 4: FCM Implementation

ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255) NULL;
ALTER TABLE users ADD INDEX idx_fcm_token (fcm_token);
