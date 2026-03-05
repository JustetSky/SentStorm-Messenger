package com.sentstorm.messenger.core.entity;

import com.sentstorm.messenger.core.entity.base.BaseAuditingEntity;
import com.sentstorm.messenger.core.entity.enums.MessageState;
import com.sentstorm.messenger.core.entity.enums.MessageType;

import jakarta.persistence.*;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseAuditingEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String ciphertext;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    private MessageState state;

    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY)
    private List<MessageAttachment> attachments;
}