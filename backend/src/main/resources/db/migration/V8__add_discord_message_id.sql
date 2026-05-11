-- V8__add_discord_message_id.sql
-- Agregamos las columnas una por una para asegurar compatibilidad total

ALTER TABLE community_activities ADD COLUMN discord_message_id VARCHAR(64);
ALTER TABLE community_activities ADD COLUMN discord_channel_id VARCHAR(64);

-- Creamos el índice único para evitar duplicados
CREATE UNIQUE INDEX idx_community_activities_discord_msg_id
    ON community_activities(discord_message_id);

-- Índice para optimizar búsquedas por fecha
CREATE INDEX idx_community_activities_published_at
    ON community_activities(published_at);