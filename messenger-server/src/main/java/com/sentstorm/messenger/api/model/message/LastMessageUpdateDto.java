package com.sentstorm.messenger.api.model.message;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Last message update event (WebSocket)")
public record LastMessageUpdateDto(
        @Schema(description = "ID of the new last message (null if no messages left)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID lastMessageId
) {}