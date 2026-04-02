package com.sentstorm.messenger.api.model.chat;

import java.time.Instant;
import java.util.UUID;

public interface ChatListItemProjection {

    UUID getChatId();

    UUID getLastMessageId();

    String getLastMessageCiphertext();

    Instant getLastMessageTime();

}