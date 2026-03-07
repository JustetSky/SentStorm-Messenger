package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.api.dto.ChatListItemProjection;
import com.sentstorm.messenger.core.entity.Chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("""
        SELECT
            c.id as chatId,
            m.id as lastMessageId,
            m.ciphertext as lastMessageCiphertext,
            m.createdDate as lastMessageTime
        FROM Chat c
        JOIN ChatParticipant cp ON cp.chat.id = c.id
        LEFT JOIN Message m ON m.chat.id = c.id
        WHERE cp.user.id = :userId
        AND m.createdDate = (
            SELECT MAX(m2.createdDate)
            FROM Message m2
            WHERE m2.chat.id = c.id
        )
        ORDER BY m.createdDate DESC
    """)
    List<ChatListItemProjection> findUserChatList(UUID userId);

    @Query("""
        SELECT c
        FROM Chat c
        JOIN ChatParticipant cp1 ON cp1.chat.id = c.id
        JOIN ChatParticipant cp2 ON cp2.chat.id = c.id
        WHERE cp1.user.id = :userA
        AND cp2.user.id = :userB
    """)
    Optional<Chat> findPrivateChat(UUID userA, UUID userB);

}