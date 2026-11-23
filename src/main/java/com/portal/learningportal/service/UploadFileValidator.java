package com.portal.learningportal.service;

import com.portal.learningportal.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
public class UploadFileValidator {

    private static final long MAX_BYTES = 100L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
            "video/mp4",
            "video/webm"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidFileException("File exceeds 100MB limit");
        }
        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return;
        }
        if (matchesAllowedExtension(file.getOriginalFilename())) {
            return;
        }
        throw new InvalidFileException(
                "Unsupported file type. Use PDF, PPT, PPTX, MP4, or WebM.");
    }

    private boolean matchesAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return Set.of("pdf", "ppt", "pptx", "mp4", "webm").contains(ext);
    }
}