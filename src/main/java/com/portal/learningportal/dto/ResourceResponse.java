package com.portal.learningportal.dto;

import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.ResourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ResourceResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final ResourceType resourceType;
    private final String publicId;
    private final String secureUrl;
    private final CloudinaryResourceType cloudinaryResourceType;
    private final String mimeType;
    private final Long fileSize;
    private final boolean published;
    private final Instant createdAt;
    private final Long courseId;
    private final String courseCode;
    private final String uploadedByUsername;
    private final List<String> tags;

    public static ResourceResponse from(Resource resource, boolean includeSecureUrl) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType())
                .publicId(resource.getPublicId())
                .secureUrl(includeSecureUrl ? resource.getSecureUrl() : null)
                .cloudinaryResourceType(resource.getCloudinaryResourceType())
                .mimeType(resource.getMimeType())
                .fileSize(resource.getFileSize())
                .published(Boolean.TRUE.equals(resource.getIsPublished()))
                .createdAt(resource.getCreatedAt())
                .courseId(resource.getCourse().getId())
                .courseCode(resource.getCourse().getCode())
                .uploadedByUsername(resource.getUploadedBy().getUsername())
                .tags(resource.getTags().stream().map(t -> t.getName()).sorted().toList())
                .build();
    }
}