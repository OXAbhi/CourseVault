package com.portal.learningportal.dto;

import com.portal.learningportal.entity.CloudinaryResourceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudinaryUploadResult {

    private final String publicId;
    private final String secureUrl;
    private final CloudinaryResourceType cloudinaryResourceType;
}