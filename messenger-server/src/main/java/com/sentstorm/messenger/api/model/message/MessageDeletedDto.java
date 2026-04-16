package com.sentstorm.messenger.api.model.message;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Message deleted event (WebSocket)")
public record MessageDeletedDto(
        @Schema(description = "ID of the deleted message", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID messageId
) {}