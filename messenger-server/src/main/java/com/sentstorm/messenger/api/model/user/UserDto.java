package com.sentstorm.messenger.api.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile response")
public class UserDto {

    @Schema(description = "Public user identifier", example = "dayman")
    private String publicId;

    @Schema(description = "User email", example = "dayman@test.com")
    private String email;

    @Schema(description = "User first name", example = "Charlie")
    private String firstName;

    @Schema(description = "User last name", example = "Dayman")
    private String lastName;

    @Schema(description = "Last time user was seen online")
    private Instant lastSeen;

}