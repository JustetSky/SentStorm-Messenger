package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.chat.ChatDto;
import com.sentstorm.messenger.api.dto.chat.ChatListItemDto;
import com.sentstorm.messenger.api.dto.chat.CreateChatRequest;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatDto createPrivateChat(CreateChatRequest request);

    List<ChatListItemDto> getUserChats();

    ChatDto getChat(UUID chatId);

}