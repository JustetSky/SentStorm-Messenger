package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.dto.ChatDto;
import com.sentstorm.messenger.api.dto.ChatListItemDto;
import com.sentstorm.messenger.api.dto.CreateChatRequest;
import com.sentstorm.messenger.core.entity.Chat;
import com.sentstorm.messenger.core.entity.ChatParticipant;
import com.sentstorm.messenger.core.entity.ChatParticipantId;
import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.repository.ChatParticipantRepository;
import com.sentstorm.messenger.core.repository.ChatRepository;
import com.sentstorm.messenger.core.repository.UserRepository;
import com.sentstorm.messenger.core.service.ChatService;
import com.sentstorm.messenger.core.service.CurrentUserService;

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
                .map(p -> ChatListItemDto.builder()
                        .chatId(p.getChatId())
                        .lastMessageId(p.getLastMessageId())
                        .lastMessageCiphertext(p.getLastMessageCiphertext())
                        .lastMessageTime(p.getLastMessageTime())
                        .build()
                )
                .toList();
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