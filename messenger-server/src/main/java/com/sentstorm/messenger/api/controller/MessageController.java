package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.dto.MessageDto;
import com.sentstorm.messenger.api.dto.MessageSendRequest;
import com.sentstorm.messenger.api.dto.PageResponse;
import com.sentstorm.messenger.core.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Message endpoints")
public class MessageController {

    private final MessageService messageService;

    @GetMapping(ApiPath.CHATS + ApiPath.CHAT_ID + ApiPath.MESSAGES)
    @Operation(summary = "Get chat messages")
    public ResponseEntity<PageResponse<MessageDto>> getMessages(
            @PathVariable UUID chatId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                messageService.getChatMessages(chatId, pageable)
        );
    }

    @PostMapping(ApiPath.MESSAGES)
    @Operation(summary = "Send message to chat")
    public ResponseEntity<MessageDto> sendMessage(
            @RequestBody @Valid MessageSendRequest request
    ) {
        return ResponseEntity.ok(
                messageService.sendMessage(request)
        );
    }
}