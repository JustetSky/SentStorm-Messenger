package com.sentstorm.messenger.api.dto;

import java.time.Instant;
import java.util.UUID;

public interface ChatListItemProjection {

    UUID getChatId();

    UUID getLastMessageId();

    String getLastMessageCiphertext();

    Instant getLastMessageTime();

}