package com.sentstorm.messenger.api.model.message;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MessageSendRequest {
    @NotNull
    private UUID chatId;

    @NotNull
    private String ciphertext;

    private String clientMessageId;

    private String type = "TEXT";
}