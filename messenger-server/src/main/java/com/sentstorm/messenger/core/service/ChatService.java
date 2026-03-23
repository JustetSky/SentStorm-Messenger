package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.ChatDto;
import com.sentstorm.messenger.api.dto.ChatListItemDto;
import com.sentstorm.messenger.api.dto.CreateChatRequest;

import java.util.List;

public interface ChatService {

    ChatDto createPrivateChat(CreateChatRequest request);

    List<ChatListItemDto> getUserChats();

}