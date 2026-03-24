package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.dto.MessageDto;
import com.sentstorm.messenger.api.dto.PageResponse;
import com.sentstorm.messenger.core.entity.Message;
import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.repository.ChatParticipantRepository;
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

    private MessageDto mapToDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .ciphertext(m.getCiphertext())
                .state(m.getState().name())
                .createdDate(m.getCreatedDate())
                .build();
    }
}