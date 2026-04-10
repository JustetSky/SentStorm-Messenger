package com.sentstorm.messenger.api.model.chat;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListItemDto {

    private UUID chatId;
    private UUID lastMessageId;
    private String lastMessageCiphertext;
    private Instant lastMessageTime;
    private ChatParticipantDto otherParticipant;
}