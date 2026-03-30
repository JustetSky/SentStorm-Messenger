package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.dto.chat.ChatDto;
import com.sentstorm.messenger.api.dto.chat.ChatListItemDto;
import com.sentstorm.messenger.api.dto.chat.CreateChatRequest;
import com.sentstorm.messenger.core.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping
    @Operation(summary = "Get current user's chats")
    public ResponseEntity<List<ChatListItemDto>> getChats() {

        return ResponseEntity.ok(chatService.getUserChats());
    }

    @GetMapping(ApiPath.CHAT_ID)
    @Operation(summary = "Get chat by id")
    public ResponseEntity<ChatDto> getChat(
            @PathVariable UUID chatId
    ) {
        return ResponseEntity.ok(chatService.getChat(chatId));
    }

    @DeleteMapping(ApiPath.CHAT_ID)
    @Operation(summary = "Delete chat")
    public ResponseEntity<Void> deleteChat(
            @PathVariable UUID chatId
    ) {
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }
}