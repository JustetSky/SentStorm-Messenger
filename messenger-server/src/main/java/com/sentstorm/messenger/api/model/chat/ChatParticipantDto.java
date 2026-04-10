package com.sentstorm.messenger.api.model.chat;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDto {

    private UUID userId;
    private String publicId;
    private String firstName;
    private String lastName;
}