CREATE INDEX IF NOT EXISTS idx_messages_chat_created_desc
ON messages(chat_id, created_date DESC);

CREATE INDEX IF NOT EXISTS idx_chat_participants_chat
ON chat_participants(chat_id);

CREATE INDEX IF NOT EXISTS idx_users_public_id_lower
ON users(LOWER(public_id));

CREATE INDEX IF NOT EXISTS idx_chat_participants_user_chat
ON chat_participants(user_id, chat_id);