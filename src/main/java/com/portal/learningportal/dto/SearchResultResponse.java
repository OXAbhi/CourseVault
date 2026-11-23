package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.ResourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class SearchResultResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final ResourceType resourceType;
    private final Long courseId;
    private final String courseCode;
    private final String courseTitle;
    private final boolean published;
    private final Instant createdAt;
    private final List<String> tags;

    public static SearchResultResponse from(Resource resource) {
        return SearchResultResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType())
                .courseId(resource.getCourse().getId())
                .courseCode(resource.getCourse().getCode())
                .courseTitle(resource.getCourse().getTitle())
                .published(Boolean.TRUE.equals(resource.getIsPublished()))
                .createdAt(resource.getCreatedAt())
                .tags(resource.getTags().stream().map(t -> t.getName()).sorted().toList())
                .build();
    }
}