package com.portal.learningportal.service;

import com.cloudinary.Cloudinary;
import com.portal.learningportal.dto.CloudinaryUploadResult;
import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.exception.CloudinaryUploadException;
import com.portal.learningportal.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final UploadFileValidator uploadFileValidator;

    public CloudinaryService(Cloudinary cloudinary, UploadFileValidator uploadFileValidator) {
        this.cloudinary = cloudinary;
        this.uploadFileValidator = uploadFileValidator;
    }

    public CloudinaryUploadResult upload(MultipartFile file, CloudinaryResourceType resourceType) {
        uploadFileValidator.validate(file);
        String cloudinaryType = resourceType.name();
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", cloudinaryType);
            options.put("folder", "learning-portal");
            String original = FileDownloadNames.sanitizedOriginalFilename(file.getOriginalFilename());
            if (original != null) {
                options.put("use_filename", true);
                options.put("unique_filename", true);
                options.put("filename_override", original);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            return CloudinaryUploadResult.builder()
                    .publicId((String) result.get("public_id"))
                    .secureUrl((String) result.get("secure_url"))
                    .cloudinaryResourceType(resourceType)
                    .build();
        } catch (IOException ex) {
            throw new InvalidFileException("Failed to read upload");
        } catch (Exception ex) {
            log.warn("Cloudinary upload failed: {}", ex.getMessage());
            throw new CloudinaryUploadException(friendlyCloudinaryMessage(ex));
        }
    }

    private static String friendlyCloudinaryMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Cloudinary upload failed. Check CLOUDINARY_* credentials and restart the API.";
        }
        if (msg.toLowerCase().contains("invalid api key")
                || msg.toLowerCase().contains("unknown api key")
                || msg.toLowerCase().contains("unauthorized")) {
            return "Cloudinary credentials rejected. Verify cloud name, API key, and secret in application.properties.";
        }
        return "Cloudinary upload failed: " + msg;
    }

    public void deleteAsset(String publicId, CloudinaryResourceType resourceType) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", resourceType.name());
            cloudinary.uploader().destroy(publicId, options);
        } catch (Exception ignored) {
            // Best effort per plan
        }
    }
}