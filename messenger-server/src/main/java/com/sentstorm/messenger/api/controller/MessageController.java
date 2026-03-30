package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.dto.message.MessageDto;
import com.sentstorm.messenger.api.dto.message.MessageSendRequest;
import com.sentstorm.messenger.api.dto.PageResponse;
import com.sentstorm.messenger.core.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
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

    @PatchMapping(ApiPath.MESSAGES + ApiPath.MESSAGE_ID + ApiPath.DELIVERED)
    @Operation(summary = "Mark message as delivered")
    public ResponseEntity<Void> markAsDelivered(@PathVariable UUID messageId) {
        messageService.markAsDelivered(messageId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(ApiPath.MESSAGES + ApiPath.MESSAGE_ID + ApiPath.READ)
    @Operation(summary = "Mark message as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(ApiPath.MESSAGES + ApiPath.MESSAGE_ID)
    @Operation(summary = "Delete message")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId
    ) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}