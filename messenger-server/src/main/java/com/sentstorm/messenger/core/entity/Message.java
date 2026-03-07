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

    @Column(name = "client_message_id", nullable = false, unique = true)
    private UUID clientMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String ciphertext;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    private MessageState state;

    @OneToMany(
            mappedBy = "message",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MessageAttachment> attachments;
}