package com.sentstorm.messenger.api.model.attachment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachmentUploadResponse {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
}