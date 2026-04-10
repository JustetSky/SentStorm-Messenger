package com.sentstorm.messenger.api.model.chat;

import java.time.Instant;
import java.util.UUID;

public interface ChatListItemProjection {

    UUID getChatId();
    UUID getLastMessageId();
    String getLastMessageCiphertext();
    Instant getLastMessageTime();

    // Данные второго участника
    UUID getOtherUserId();
    String getOtherUserPublicId();
    String getOtherUserFirstName();
    String getOtherUserLastName();
}