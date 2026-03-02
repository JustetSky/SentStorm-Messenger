CREATE TABLE users
(
    id                  UUID PRIMARY KEY,
    keycloak_id         UUID UNIQUE NOT NULL,
    email               VARCHAR(255) NOT NULL
        CONSTRAINT chk_users_email_format
            CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),

    first_name          VARCHAR(50) NOT NULL
        CONSTRAINT chk_users_first_name_length
            CHECK (length(first_name) BETWEEN 2 AND 50),

    last_name           VARCHAR(50) NOT NULL
        CONSTRAINT chk_users_last_name_length
            CHECK (length(last_name) BETWEEN 2 AND 50),

    public_id           VARCHAR(50) UNIQUE NOT NULL
        CONSTRAINT chk_users_public_id_length
            CHECK (length(public_id) BETWEEN 4 AND 50),

    last_seen           TIMESTAMP,
    created_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Пользователи приложения (профили мессенджера)';
COMMENT ON COLUMN users.id IS 'UUID пользователя в системе мессенджера';
COMMENT ON COLUMN users.keycloak_id IS 'UUID пользователя в Keycloak (sub)';
COMMENT ON COLUMN users.email IS 'Email пользователя';
COMMENT ON COLUMN users.first_name IS 'Имя';
COMMENT ON COLUMN users.last_name IS 'Фамилия';
COMMENT ON COLUMN users.public_id IS 'Публичный идентификатор пользователя (username/handle)';
COMMENT ON COLUMN users.last_seen IS 'Время последней активности';
COMMENT ON COLUMN users.created_date IS 'Дата создания профиля';
COMMENT ON COLUMN users.last_modified_date IS 'Дата последнего обновления профиля';

CREATE TABLE user_devices
(
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_id       VARCHAR(100) NOT NULL,
    public_key      TEXT NOT NULL,
    created_date    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active     TIMESTAMP,

    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

COMMENT ON TABLE user_devices IS 'Устройства пользователей и их публичные E2E ключи';
COMMENT ON COLUMN user_devices.id IS 'UUID записи устройства';
COMMENT ON COLUMN user_devices.user_id IS 'Владелец устройства';
COMMENT ON COLUMN user_devices.device_id IS 'Идентификатор устройства клиента';
COMMENT ON COLUMN user_devices.public_key IS 'Публичный ключ устройства для E2E шифрования';
COMMENT ON COLUMN user_devices.created_date IS 'Дата регистрации устройства';
COMMENT ON COLUMN user_devices.last_active IS 'Последняя активность устройства';

CREATE TABLE chats
(
    id                  UUID PRIMARY KEY,
    created_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chats IS 'Чаты (личные и групповые)';
COMMENT ON COLUMN chats.id IS 'UUID чата';
COMMENT ON COLUMN chats.created_date IS 'Дата создания чата';
COMMENT ON COLUMN chats.last_modified_date IS 'Дата последнего изменения чата';

CREATE TABLE chat_participants
(
    chat_id     UUID NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (chat_id, user_id)
);

COMMENT ON TABLE chat_participants IS 'Участники чатов';
COMMENT ON COLUMN chat_participants.chat_id IS 'UUID чата';
COMMENT ON COLUMN chat_participants.user_id IS 'UUID пользователя';
COMMENT ON COLUMN chat_participants.joined_date IS 'Дата присоединения к чату';

CREATE TABLE messages
(
    id                  UUID PRIMARY KEY,
    chat_id             UUID NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id           UUID NOT NULL REFERENCES users (id),
    ciphertext          TEXT NOT NULL,
    type                VARCHAR(20) NOT NULL
        CONSTRAINT chk_messages_type
            CHECK (type IN ('TEXT', 'IMAGE', 'FILE', 'EMOJI', 'SYSTEM')),

    state               VARCHAR(20) NOT NULL
        CONSTRAINT chk_messages_state
            CHECK (state IN ('SENT', 'DELIVERED', 'READ')),

    created_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE messages IS 'Сообщения в чатах (зашифрованные)';
COMMENT ON COLUMN messages.id IS 'UUID сообщения';
COMMENT ON COLUMN messages.chat_id IS 'Чат сообщения';
COMMENT ON COLUMN messages.sender_id IS 'Отправитель';
COMMENT ON COLUMN messages.ciphertext IS 'Зашифрованное содержимое сообщения';
COMMENT ON COLUMN messages.type IS 'Тип сообщения';
COMMENT ON COLUMN messages.state IS 'Статус доставки';
COMMENT ON COLUMN messages.created_date IS 'Дата отправки';
COMMENT ON COLUMN messages.last_modified_date IS 'Дата изменения';

CREATE TABLE message_attachments
(
    id              UUID PRIMARY KEY,
    message_id      UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL
        CONSTRAINT chk_attach_name_length CHECK (length(file_name) BETWEEN 1 AND 255),
    file_type       VARCHAR(50) NOT NULL,
    file_size       BIGINT NOT NULL
        CONSTRAINT chk_attach_file_size CHECK (file_size >= 0),
    file_url        VARCHAR(500) NOT NULL,
    encrypted_key   TEXT NOT NULL
);

COMMENT ON TABLE message_attachments IS 'Вложения сообщений';
COMMENT ON COLUMN message_attachments.id IS 'UUID вложения';
COMMENT ON COLUMN message_attachments.message_id IS 'Сообщение-владелец';
COMMENT ON COLUMN message_attachments.file_name IS 'Имя файла';
COMMENT ON COLUMN message_attachments.file_type IS 'MIME тип файла';
COMMENT ON COLUMN message_attachments.file_size IS 'Размер файла в байтах';
COMMENT ON COLUMN message_attachments.file_url IS 'Путь хранения файла';
COMMENT ON COLUMN message_attachments.encrypted_key IS 'Зашифрованный AES ключ файла';