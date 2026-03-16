package com.sentstorm.messenger.api.dto;

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

    @Schema(description = "Public user identifier", example = "john_doe")
    private String publicId;

    @Schema(description = "User email", example = "john@example.com")
    private String email;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "Last time user was seen online")
    private Instant lastSeen;

}