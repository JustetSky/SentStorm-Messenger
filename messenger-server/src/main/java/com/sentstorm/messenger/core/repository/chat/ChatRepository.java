package com.sentstorm.messenger.core.repository.chat;

import com.sentstorm.messenger.api.model.chat.ChatListItemProjection;
import com.sentstorm.messenger.core.entity.chat.Chat;

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
            m.createdDate as lastMessageTime,
            otherUser.id as otherUserId,
            otherUser.publicId as otherUserPublicId,
            otherUser.firstName as otherUserFirstName,
            otherUser.lastName as otherUserLastName
        FROM Chat c
        JOIN ChatParticipant cp ON cp.chat.id = c.id
        JOIN ChatParticipant otherCp ON otherCp.chat.id = c.id
        JOIN otherCp.user otherUser
        LEFT JOIN Message m ON m.id = (
            SELECT m2.id
            FROM Message m2
            WHERE m2.chat.id = c.id
            ORDER BY m2.createdDate DESC
            LIMIT 1
        )
        WHERE cp.user.id = :userId
          AND otherUser.id != :userId
        ORDER BY m.createdDate DESC NULLS LAST
    """)
    List<ChatListItemProjection> findUserChatList(UUID userId);

    @Query("""
        SELECT c
        FROM Chat c
        JOIN ChatParticipant cp1 ON cp1.chat.id = c.id
        JOIN ChatParticipant cp2 ON cp2.chat.id = c.id
        WHERE cp1.user.id = :userA
          AND cp2.user.id = :userB
          AND c.id IN (
              SELECT cp.chat.id
              FROM ChatParticipant cp
              GROUP BY cp.chat.id
              HAVING COUNT(cp.user.id) = 2
          )
    """)
    Optional<Chat> findPrivateChat(UUID userA, UUID userB);
}