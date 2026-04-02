package com.sentstorm.messenger.api.model.message;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private UUID id;

    private UUID clientMessageId; 

    private UUID senderId;

    private String ciphertext;

    private String type; 

    private String state;

    private Instant createdDate;
}