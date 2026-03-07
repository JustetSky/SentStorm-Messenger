package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.core.entity.ChatParticipant;
import com.sentstorm.messenger.core.entity.ChatParticipantId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository
        extends JpaRepository<ChatParticipant, ChatParticipantId> {

    List<ChatParticipant> findByChatId(UUID chatId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

}