package com.sentstorm.messenger.api.model.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat participant information")
public class ChatParticipantDto {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Public ID of the user", example = "dayman")
    private String publicId;

    @Schema(description = "First name", example = "Charlie")
    private String firstName;

    @Schema(description = "Last name", example = "Dayman")
    private String lastName;
}