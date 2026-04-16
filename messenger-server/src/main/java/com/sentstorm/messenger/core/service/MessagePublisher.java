package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.model.message.LastMessageUpdateDto;
import com.sentstorm.messenger.api.model.message.MessageDeletedDto;
import com.sentstorm.messenger.api.model.message.MessageDto;
import com.sentstorm.messenger.api.model.message.MessageStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToChat(UUID chatId, MessageDto dto) {
        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId,
                dto
        );
    }

    public void sendStatus(UUID chatId, MessageStatusDto dto) {
        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId + "/status",
                dto
        );
    }

    public void sendMessageDeleted(UUID chatId, UUID messageId) {
        MessageDeletedDto payload = new MessageDeletedDto(messageId);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId + "/deleted",
                payload
        );
    }

    public void sendLastMessageUpdate(UUID chatId, UUID lastMessageId) {
        LastMessageUpdateDto payload = new LastMessageUpdateDto(lastMessageId);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId + "/last-message",
                payload
        );
    }
}