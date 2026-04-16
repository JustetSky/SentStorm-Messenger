package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.ApiPath;
import com.sentstorm.messenger.api.model.attachment.AttachmentUploadResponse;
import com.sentstorm.messenger.core.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPath.MESSAGES)
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File upload endpoints for messages")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(ApiPath.UPLOAD)
    @Operation(summary = "Upload an image attachment")
    public ResponseEntity<AttachmentUploadResponse> uploadAttachment(
            @RequestParam("image") MultipartFile image
    ) {
        return ResponseEntity.ok(attachmentService.upload(image));
    }
}