package com.sentstorm.messenger.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Image upload response")
public class AttachmentUploadResponse {

    @Schema(description = "URL of the uploaded image", example = "/uploads/abc-123.jpg")
    private String fileUrl;

    @Schema(description = "Original file name", example = "photo.jpg")
    private String fileName;

    @Schema(description = "MIME type of the file", example = "image/jpeg")
    private String fileType;

    @Schema(description = "File size in bytes", example = "123456")
    private Long fileSize;
}