package com.sentstorm.messenger.api.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message response")
public class MessageDto {

    @Schema(description = "Server-generated message ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Client-generated message ID for deduplication", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID clientMessageId;

    @Schema(description = "ID of the message sender", example = "8675a2f2-d029-45cc-a8de-76087f4ec019")
    private UUID senderId;

    @Schema(description = "Encrypted message content (E2E encrypted)", example = "{\"ciphertext\":\"...\"}")
    private String ciphertext;

    @Schema(description = "Message type", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "FILE", "SYSTEM"})
    private String type;

    @Schema(description = "Message delivery state", example = "SENT", allowableValues = {"SENT", "DELIVERED", "READ"})
    private String state;

    @Schema(description = "Message creation timestamp", example = "2026-04-15T14:30:00Z")
    private Instant createdDate;
}