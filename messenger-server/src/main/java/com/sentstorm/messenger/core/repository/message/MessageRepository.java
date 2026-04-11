package com.sentstorm.messenger.core.repository.message;

import com.sentstorm.messenger.core.entity.message.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByChatIdOrderByCreatedDateDesc(UUID chatId, Pageable pageable);

    @Query("""
        SELECT m
        FROM Message m
        WHERE m.chat.id = :chatId
        ORDER BY m.createdDate DESC
        LIMIT 1
    """)
    Message findFirstByChatIdOrderByCreatedDateDesc(UUID chatId);

}