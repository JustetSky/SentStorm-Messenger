package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.dto.ChatDto;
import com.sentstorm.messenger.api.dto.CreateChatRequest;
import com.sentstorm.messenger.core.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.CHATS)
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Chat endpoints")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Create or get private chat")
    public ResponseEntity<ChatDto> createChat(
            @Valid @RequestBody CreateChatRequest request
    ) {
        return ResponseEntity.ok(chatService.createPrivateChat(request));
    }
}