package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.model.chat.ChatDto;
import com.sentstorm.messenger.api.model.chat.ChatListItemDto;
import com.sentstorm.messenger.api.model.chat.ChatParticipantDto;
import com.sentstorm.messenger.api.model.chat.CreateChatRequest;
import com.sentstorm.messenger.core.entity.chat.Chat;
import com.sentstorm.messenger.core.entity.chat.ChatParticipant;
import com.sentstorm.messenger.core.entity.chat.ChatParticipantId;
import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.repository.chat.ChatParticipantRepository;
import com.sentstorm.messenger.core.repository.chat.ChatRepository;
import com.sentstorm.messenger.core.repository.user.UserRepository;
import com.sentstorm.messenger.core.service.ChatService;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.exception.ErrorCode;
import com.sentstorm.messenger.core.exception.ServiceException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ChatDto createPrivateChat(CreateChatRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        User otherUser = userRepository.findByPublicId(request.getUserPublicId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }

        return chatRepository.findPrivateChat(currentUser.getId(), otherUser.getId())
                .map(chat -> ChatDto.builder()
                        .id(chat.getId())
                        .build()
                )
                .orElseGet(() -> createNewChat(currentUser, otherUser));
    }

    @Override
    public List<ChatListItemDto> getUserChats() {
        User currentUser = currentUserService.getCurrentUser();

        return chatRepository.findUserChatList(currentUser.getId())
                .stream()
                .map(p -> {
                    // Создаем DTO для второго участника
                    ChatParticipantDto otherParticipant = null;
                    if (p.getOtherUserId() != null) {
                        otherParticipant = ChatParticipantDto.builder()
                                .userId(p.getOtherUserId())
                                .publicId(p.getOtherUserPublicId())
                                .firstName(p.getOtherUserFirstName())
                                .lastName(p.getOtherUserLastName())
                                .build();
                    }

                    return ChatListItemDto.builder()
                            .chatId(p.getChatId())
                            .lastMessageId(p.getLastMessageId())
                            .lastMessageCiphertext(p.getLastMessageCiphertext())
                            .lastMessageTime(p.getLastMessageTime())
                            .otherParticipant(otherParticipant)
                            .build();
                })
                .toList();
    }

    @Override
    public ChatDto getChat(UUID chatId) {
        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(chatId, currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "Access denied");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Chat not found")
                );

        // Получаем всех участников чата
        List<User> participants = chatParticipantRepository.findUsersByChatId(chatId);

        // Преобразуем в DTO
        List<ChatParticipantDto> participantDtos = participants.stream()
                .map(user -> ChatParticipantDto.builder()
                        .userId(user.getId())
                        .publicId(user.getPublicId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build())
                .toList();

        // Находим второго участника (для личных чатов)
        ChatParticipantDto otherParticipant = participants.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .findFirst()
                .map(user -> ChatParticipantDto.builder()
                        .userId(user.getId())
                        .publicId(user.getPublicId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build())
                .orElse(null);

        return ChatDto.builder()
                .id(chat.getId())
                .participants(participantDtos)
                .otherParticipant(otherParticipant)
                .build();
    }

    @Override
    @Transactional
    public void deleteChat(UUID chatId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.NOT_FOUND,
                        "Chat not found"
                ));

        User currentUser = currentUserService.getCurrentUser();

        boolean isParticipant = chatParticipantRepository
                .existsByChatIdAndUserId(chatId, currentUser.getId());

        if (!isParticipant) {
            throw new ServiceException(
                    ErrorCode.FORBIDDEN,
                    "Access denied"
            );
        }

        chatParticipantRepository.deleteByChatId(chatId);

        chatRepository.delete(chat);
    }

    private ChatDto createNewChat(User user1, User user2) {

        Chat chat = new Chat();
        chat.setId(UUID.randomUUID());

        chatRepository.save(chat);

        Instant now = Instant.now();

        ChatParticipant p1 = new ChatParticipant();
        p1.setId(new ChatParticipantId(chat.getId(), user1.getId()));
        p1.setChat(chat);
        p1.setUser(user1);
        p1.setJoinedDate(now);

        ChatParticipant p2 = new ChatParticipant();
        p2.setId(new ChatParticipantId(chat.getId(), user2.getId()));
        p2.setChat(chat);
        p2.setUser(user2);
        p2.setJoinedDate(now);

        chatParticipantRepository.save(p1);
        chatParticipantRepository.save(p2);

        return ChatDto.builder()
                .id(chat.getId())
                .build();
    }
}