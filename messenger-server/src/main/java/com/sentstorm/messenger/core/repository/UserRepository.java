package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.core.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(UUID keycloakId);

    Optional<User> findByPublicId(String publicId);

    @Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.publicId) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<User> searchUsers(String query, Pageable pageable);

}