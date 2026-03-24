package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.dto.MessageDto;
import com.sentstorm.messenger.api.dto.MessageSendRequest;
import com.sentstorm.messenger.api.dto.PageResponse;
import com.sentstorm.messenger.core.entity.Chat;
import com.sentstorm.messenger.core.entity.Message;
import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.entity.enums.MessageState;
import com.sentstorm.messenger.core.entity.enums.MessageType;
import com.sentstorm.messenger.core.exception.ForbiddenException;
import com.sentstorm.messenger.core.exception.NotFoundException;
import com.sentstorm.messenger.core.repository.ChatParticipantRepository;
import com.sentstorm.messenger.core.repository.ChatRepository;
import com.sentstorm.messenger.core.repository.MessageRepository;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.MessageService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final CurrentUserService currentUserService;
    private final ChatRepository chatRepository;

    @Override
    public PageResponse<MessageDto> getChatMessages(UUID chatId, Pageable pageable) {

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(chatId, currentUser.getId());

        if (!isParticipant) {
            throw new RuntimeException("Access denied");
        }

        Page<Message> page = messageRepository
                .findByChatIdOrderByCreatedDateDesc(chatId, pageable);

        List<MessageDto> items = page.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<MessageDto>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .hasNext(page.hasNext())
                .build();
    }

    @Override
    public MessageDto sendMessage(MessageSendRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(request.getChatId(), currentUser.getId());

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant of this chat");
        }

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .clientMessageId(request.getClientMessageId())
                .chat(chat)
                .sender(currentUser)
                .ciphertext(request.getCiphertext())
                .type(MessageType.TEXT)
                .state(MessageState.SENT)
                .build();

        message = messageRepository.save(message);

        return mapToDto(message);
    }

    private MessageDto mapToDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .clientMessageId(message.getClientMessageId())
                .senderId(message.getSender().getId())
                .ciphertext(message.getCiphertext())
                .type(message.getType().name())
                .state(message.getState().name())
                .createdDate(message.getCreatedDate())
                .build();
    }
}