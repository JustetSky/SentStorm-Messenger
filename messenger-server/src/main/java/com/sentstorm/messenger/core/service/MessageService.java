package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.model.message.MessageDto;
import com.sentstorm.messenger.api.model.message.MessageSendRequest;
import com.sentstorm.messenger.api.model.PageResponse;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageService {

    PageResponse<MessageDto> getChatMessages(UUID chatId, Pageable pageable);

    MessageDto sendMessage(MessageSendRequest request);

    void markAsDelivered(UUID messageId);

    void markAsRead(UUID messageId);
    
    void deleteMessage(UUID messageId);
}