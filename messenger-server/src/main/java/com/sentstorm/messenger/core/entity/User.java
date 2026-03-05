package com.sentstorm.messenger.core.entity;

import com.sentstorm.messenger.core.entity.base.BaseAuditingEntity;

import jakarta.persistence.*;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditingEntity {

    @Id
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private UUID keycloakId;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserDevice> devices;
}