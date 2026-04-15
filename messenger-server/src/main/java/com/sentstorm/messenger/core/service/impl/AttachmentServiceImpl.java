package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.model.attachment.AttachmentUploadResponse;
import com.sentstorm.messenger.core.service.AttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Value("${application.file.uploads.media-output-path:./uploads}")
    private String uploadDir;

    @Override
    public AttachmentUploadResponse upload(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only images are allowed");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(storedFilename);
            file.transferTo(filePath);

            String fileUrl = "/uploads/" + storedFilename;

            return AttachmentUploadResponse.builder()
                    .fileUrl(fileUrl)
                    .fileName(originalFilename)
                    .fileType(contentType)
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}