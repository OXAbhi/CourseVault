package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Batch;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BatchResponse {

    private final Long id;
    private final String name;
    private final String academicYear;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long courseId;
    private final String courseCode;
    private final String courseTitle;

    public static BatchResponse from(Batch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .academicYear(batch.getAcademicYear())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .courseId(batch.getCourse().getId())
                .courseCode(batch.getCourse().getCode())
                .courseTitle(batch.getCourse().getTitle())
                .build();
    }
}