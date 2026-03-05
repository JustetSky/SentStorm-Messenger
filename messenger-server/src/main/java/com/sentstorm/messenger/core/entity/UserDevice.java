package com.sentstorm.messenger.core.entity;

import com.sentstorm.messenger.core.entity.base.BaseAuditingEntity;

import jakarta.persistence.*;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice extends BaseAuditingEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "last_active")
    private Instant lastActive;

    @Column(name = "push_token")
    private String pushToken;

    @Column(name = "is_active")
    private Boolean isActive;
}