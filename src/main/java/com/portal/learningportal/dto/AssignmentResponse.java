package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Assignment;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AssignmentResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final Instant dueAt;
    private final int maxMarks;
    private final String attachmentPublicId;
    private final String attachmentSecureUrl;
    private final boolean published;
    private final Instant createdAt;
    private final Long courseId;
    private final String courseCode;
    private final String createdByUsername;
    private final SubmissionResponse mySubmission;

    public static AssignmentResponse from(
            Assignment assignment,
            boolean includeAttachmentUrl,
            SubmissionResponse mySubmission
    ) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueAt(assignment.getDueAt())
                .maxMarks(assignment.getMaxMarks())
                .attachmentPublicId(assignment.getAttachmentPublicId())
                .attachmentSecureUrl(includeAttachmentUrl ? assignment.getAttachmentSecureUrl() : null)
                .published(Boolean.TRUE.equals(assignment.getIsPublished()))
                .createdAt(assignment.getCreatedAt())
                .courseId(assignment.getCourse().getId())
                .courseCode(assignment.getCourse().getCode())
                .createdByUsername(assignment.getCreatedBy().getUsername())
                .mySubmission(mySubmission)
                .build();
    }
}