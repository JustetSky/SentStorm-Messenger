package com.sentstorm.messenger.api.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User search result item")
public class UserSearchDto {

    @Schema(description = "Public user identifier", example = "dayman")
    private String publicId;

    @Schema(description = "User first name", example = "Charlie")
    private String firstName;

    @Schema(description = "User last name", example = "Dayman")
    private String lastName;

    @Schema(description = "Last time user was seen online", example = "2026-04-15T14:30:00Z")
    private Instant lastSeen;
}