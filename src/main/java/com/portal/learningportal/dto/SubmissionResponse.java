package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Submission;
import com.portal.learningportal.entity.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SubmissionResponse {

    private final Long id;
    private final Long assignmentId;
    private final Long studentId;
    private final String studentUsername;
    private final Instant submittedAt;
    private final SubmissionStatus status;
    private final Integer marks;
    private final String feedback;
    private final String secureUrl;

    public static SubmissionResponse from(Submission submission, boolean includeSecureUrl) {
        boolean graded = submission.getStatus() == SubmissionStatus.GRADED;
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .studentId(submission.getStudent().getId())
                .studentUsername(submission.getStudent().getUsername())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .marks(graded ? submission.getMarks() : null)
                .feedback(graded ? submission.getFeedback() : null)
                .secureUrl(includeSecureUrl ? submission.getSecureUrl() : null)
                .build();
    }
}