package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.core.entity.MessageAttachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAttachmentRepository
        extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageId(UUID messageId);

}