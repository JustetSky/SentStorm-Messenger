package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.model.message.MessageDto;
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
}