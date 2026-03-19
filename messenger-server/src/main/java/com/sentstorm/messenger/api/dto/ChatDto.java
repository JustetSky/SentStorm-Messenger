package com.sentstorm.messenger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat response")
public class ChatDto {

    @Schema(description = "Chat ID")
    private UUID id;
}