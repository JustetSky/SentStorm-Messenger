package com.sentstorm.messenger.api.model.message;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusDto {

    private UUID messageId;
    private String status;
}