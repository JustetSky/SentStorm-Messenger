package com.sentstorm.messenger.core.entity;

import com.sentstorm.messenger.core.entity.base.BaseAuditingEntity;

import jakarta.persistence.*;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat extends BaseAuditingEntity {

    @Id
    private UUID id;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY)
    private List<Message> messages;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY)
    private List<ChatParticipant> participants;
}