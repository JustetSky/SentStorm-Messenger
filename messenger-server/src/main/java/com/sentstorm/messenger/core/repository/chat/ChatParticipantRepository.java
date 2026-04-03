package com.sentstorm.messenger.core.repository.chat;

import com.sentstorm.messenger.core.entity.chat.ChatParticipant;
import com.sentstorm.messenger.core.entity.chat.ChatParticipantId;

import com.sentstorm.messenger.core.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository
        extends JpaRepository<ChatParticipant, ChatParticipantId> {

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);
    
    void deleteByChatId(UUID chatId);

    @Query("""
        select cp.user
        from ChatParticipant cp
        where cp.chat.id = :chatId
    """)
    List<User> findUsersByChatId(UUID chatId);

}