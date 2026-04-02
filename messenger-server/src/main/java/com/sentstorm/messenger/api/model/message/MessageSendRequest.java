package com.sentstorm.messenger.api.model.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest {

    @NotNull
    private UUID chatId;

    @NotBlank
    private String ciphertext;

    @NotNull
    private UUID clientMessageId;
}