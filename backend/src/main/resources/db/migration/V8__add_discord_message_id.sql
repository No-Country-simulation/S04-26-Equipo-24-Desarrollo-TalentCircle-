-- V8__add_discord_message_id.sql
-- Agrega discord_message_id para deduplicación de mensajes de Discord
-- y channel_id para saber de qué canal proviene cada actividad

ALTER TABLE community_activities
    ADD COLUMN IF NOT EXISTS discord_message_id VARCHAR(64);

ALTER TABLE community_activities
    ADD COLUMN IF NOT EXISTS discord_channel_id VARCHAR(64);

-- Índice único para evitar guardar el mismo mensaje dos veces
CREATE UNIQUE INDEX IF NOT EXISTS idx_community_activities_discord_message_id
    ON community_activities(discord_message_id);