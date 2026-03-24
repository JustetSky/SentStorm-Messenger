package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.MessageDto;
import com.sentstorm.messenger.api.dto.PageResponse;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageService {

    PageResponse<MessageDto> getChatMessages(UUID chatId, Pageable pageable);

}