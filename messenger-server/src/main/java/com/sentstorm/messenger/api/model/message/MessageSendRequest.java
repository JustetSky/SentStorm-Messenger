package com.sentstorm.messenger.api.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Send message request")
public class MessageSendRequest {

    @NotNull
    @Schema(description = "Chat ID to send message to", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID chatId;

    @NotNull
    @Schema(description = "Encrypted message content (E2E encrypted)", example = "{\"ciphertext\":\"...\"}")
    private String ciphertext;

    @Schema(description = "Client-generated message ID for deduplication", example = "550e8400-e29b-41d4-a716-446655440000")
    private String clientMessageId;

    @Schema(description = "Message type", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "FILE", "SYSTEM"})
    private String type = "TEXT";
}