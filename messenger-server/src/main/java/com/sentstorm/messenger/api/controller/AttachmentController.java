package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.ApiPath;
import com.sentstorm.messenger.api.model.attachment.AttachmentUploadResponse;
import com.sentstorm.messenger.core.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPath.MESSAGES)
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(ApiPath.UPLOAD)
    public ResponseEntity<AttachmentUploadResponse> uploadAttachment(
            @RequestParam("image") MultipartFile image
    ) {
        return ResponseEntity.ok(attachmentService.upload(image));
    }
}