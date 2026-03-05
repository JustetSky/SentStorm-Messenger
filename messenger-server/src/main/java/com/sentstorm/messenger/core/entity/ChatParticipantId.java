package com.sentstorm.messenger.core.entity;

import jakarta.persistence.Embeddable;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatParticipantId implements Serializable {

    private UUID chatId;

    private UUID userId;
}