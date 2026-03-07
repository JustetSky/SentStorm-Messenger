package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.core.entity.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByChatIdOrderByCreatedDateDesc(
            UUID chatId,
            Pageable pageable
    );

    Optional<Message> findFirstByChatIdOrderByCreatedDateDesc(UUID chatId);

    Optional<Message> findByClientMessageId(UUID clientMessageId);

}