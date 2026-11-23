package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Course;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CourseResponse {

    private final Long id;
    private final String code;
    private final String title;
    private final String description;
    private final boolean active;
    private final Instant createdAt;
    private final Long teacherId;
    private final String teacherUsername;
    private final String teacherName;

    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .code(course.getCode())
                .title(course.getTitle())
                .description(course.getDescription())
                .active(Boolean.TRUE.equals(course.getIsActive()))
                .createdAt(course.getCreatedAt())
                .teacherId(course.getTeacher().getId())
                .teacherUsername(course.getTeacher().getUsername())
                .teacherName(course.getTeacher().getFirstName() + " " + course.getTeacher().getLastName())
                .build();
    }
}