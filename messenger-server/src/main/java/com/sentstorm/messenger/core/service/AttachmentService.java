package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.model.attachment.AttachmentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    AttachmentUploadResponse upload(MultipartFile file);
}