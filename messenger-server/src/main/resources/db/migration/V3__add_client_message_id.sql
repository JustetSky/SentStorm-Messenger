ALTER TABLE messages
ADD COLUMN client_message_id UUID;

CREATE UNIQUE INDEX idx_messages_client_message_id
ON messages(client_message_id);