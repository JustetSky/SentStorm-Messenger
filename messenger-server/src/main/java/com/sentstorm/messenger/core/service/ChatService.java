package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.ChatDto;
import com.sentstorm.messenger.api.dto.CreateChatRequest;

public interface ChatService {

    ChatDto createPrivateChat(CreateChatRequest request);

}