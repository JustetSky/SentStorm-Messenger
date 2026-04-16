package com.sentstorm.messenger.api.model.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat list item response")
public class ChatListItemDto {

    @Schema(description = "Chat ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID chatId;

    @Schema(description = "ID of the last message in chat", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID lastMessageId;

    @Schema(description = "Encrypted content of the last message", example = "{\"ciphertext\":\"...\"}")
    private String lastMessageCiphertext;

    @Schema(description = "Time of the last message", example = "2026-04-15T14:30:00Z")
    private Instant lastMessageTime;

    @Schema(description = "Other participant information (for private chats)")
    private ChatParticipantDto otherParticipant;
}