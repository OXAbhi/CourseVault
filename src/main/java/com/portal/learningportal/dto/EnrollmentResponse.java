package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Enrollment;
import com.portal.learningportal.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EnrollmentResponse {

    private final Long id;
    private final EnrollmentStatus status;
    private final Instant enrolledAt;
    private final Long studentId;
    private final String studentUsername;
    private final Long batchId;
    private final String batchName;
    private final Long courseId;
    private final String courseCode;

    public static EnrollmentResponse from(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .studentId(enrollment.getStudent().getId())
                .studentUsername(enrollment.getStudent().getUsername())
                .batchId(enrollment.getBatch().getId())
                .batchName(enrollment.getBatch().getName())
                .courseId(enrollment.getBatch().getCourse().getId())
                .courseCode(enrollment.getBatch().getCourse().getCode())
                .build();
    }
}