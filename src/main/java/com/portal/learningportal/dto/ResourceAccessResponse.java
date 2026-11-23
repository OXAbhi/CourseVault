package com.portal.learningportal.dto;

import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.entity.ResourceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceAccessResponse {

    private final String secureUrl;
    private final ResourceType resourceType;
    private final CloudinaryResourceType cloudinaryResourceType;
    private final String mimeType;
}