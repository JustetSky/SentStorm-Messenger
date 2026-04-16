package com.sentstorm.messenger.api.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message status update (WebSocket)")
public class MessageStatusDto {

    @Schema(description = "Message ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID messageId;

    @Schema(description = "New status", example = "DELIVERED", allowableValues = {"SENT","DELIVERED", "READ"})
    private String status;
}