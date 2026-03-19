package com.sentstorm.messenger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create private chat request")
public class CreateChatRequest {

    @NotBlank
    @Schema(description = "Public ID of another user", example = "nightman")
    private String userPublicId;
}