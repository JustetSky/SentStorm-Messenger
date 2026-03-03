-- V2: Push support + DB improvements
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE user_devices
ADD COLUMN IF NOT EXISTS push_token TEXT;

COMMENT ON COLUMN user_devices.push_token
IS 'Push token устройства (FCM/Web Push)';

ALTER TABLE user_devices
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN user_devices.is_active
IS 'Флаг активного WebSocket-соединения устройства';

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id
ON user_devices(user_id);

ALTER TABLE users
    ALTER COLUMN created_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN last_modified_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN last_seen TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_devices
    ALTER COLUMN created_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN last_active TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE chats
    ALTER COLUMN created_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN last_modified_date TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE messages
    ALTER COLUMN created_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN last_modified_date TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_state_values
    CHECK (state IN ('SENT','DELIVERED','READ'));

CREATE INDEX IF NOT EXISTS idx_messages_chat_created
ON messages(chat_id, created_date);

CREATE INDEX IF NOT EXISTS idx_chat_participants_user
ON chat_participants(user_id);

CREATE INDEX IF NOT EXISTS idx_messages_sender
ON messages(sender_id);