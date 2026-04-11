package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.model.message.MessageDto;
import com.sentstorm.messenger.api.model.message.MessageSendRequest;
import com.sentstorm.messenger.api.model.PageResponse;
import com.sentstorm.messenger.api.mapper.MessageMapper;
import com.sentstorm.messenger.api.model.message.MessageStatusDto;
import com.sentstorm.messenger.core.entity.chat.Chat;
import com.sentstorm.messenger.core.entity.message.Message;
import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.entity.enums.MessageState;
import com.sentstorm.messenger.core.entity.enums.MessageType;
import com.sentstorm.messenger.core.exception.ErrorCode;
import com.sentstorm.messenger.core.exception.ServiceException;
import com.sentstorm.messenger.core.repository.chat.ChatParticipantRepository;
import com.sentstorm.messenger.core.repository.chat.ChatRepository;
import com.sentstorm.messenger.core.repository.message.MessageRepository;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.MessagePublisher;
import com.sentstorm.messenger.core.service.MessageService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final CurrentUserService currentUserService;
    private final ChatRepository chatRepository;
    private final MessageMapper messageMapper;
    private final MessagePublisher messagePublisher;

    @Override
    public PageResponse<MessageDto> getChatMessages(UUID chatId, Pageable pageable) {

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(chatId, currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "Access denied");
        }

        Page<Message> page = messageRepository
                .findByChatIdOrderByCreatedDateDesc(chatId, pageable);

        List<MessageDto> items = page.getContent()
                .stream()
                .map(messageMapper::toDto)
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
            throw new ServiceException(ErrorCode.FORBIDDEN, "You are not a participant of this chat");
        }

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Chat not found")
                );

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

        MessageDto dto = messageMapper.toDto(message);

        messagePublisher.sendToChat(chat.getId(), dto);
        
        return dto;
    }

    @Override
    @Transactional
    public void markAsDelivered(UUID messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Message not found")
                );

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(message.getChat().getId(), currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "Access denied");
        }

        if (message.getSender().getId().equals(currentUser.getId())) {
            throw new ServiceException(ErrorCode.FORBIDDEN,
                    "You cannot mark your own message as delivered");
        }

        if (message.getState() == MessageState.SENT) {
            message.setState(MessageState.DELIVERED);

            messagePublisher.sendStatus(
                    message.getChat().getId(),
                    new MessageStatusDto(messageId, "DELIVERED")
            );
        }
    }

    @Override
    @Transactional
    public void markAsRead(UUID messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Message not found")
                );

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(message.getChat().getId(), currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "Access denied");
        }

        if (message.getSender().getId().equals(currentUser.getId())) {
            throw new ServiceException(ErrorCode.FORBIDDEN,
                    "You cannot mark your own message as read");
        }

        if (message.getState() == MessageState.SENT
                || message.getState() == MessageState.DELIVERED) {

            message.setState(MessageState.READ);

            messagePublisher.sendStatus(
                    message.getChat().getId(),
                    new MessageStatusDto(messageId, "READ")
            );
        }
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        User currentUser = currentUserService.getCurrentUser();
        UUID chatId = message.getChat().getId();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(chatId, currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(
                    ErrorCode.FORBIDDEN,
                    "Access denied"
            );
        }

        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new ServiceException(
                    ErrorCode.FORBIDDEN,
                    "You can delete only your messages"
            );
        }

        messageRepository.delete(message);
        messagePublisher.sendMessageDeleted(chatId, messageId);

        Message newLastMessage = messageRepository.findFirstByChatIdOrderByCreatedDateDesc(chatId);
        messagePublisher.sendLastMessageUpdate(chatId, newLastMessage != null ? newLastMessage.getId() : null);
    }
    
}