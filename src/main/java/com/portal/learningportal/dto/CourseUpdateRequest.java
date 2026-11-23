package com.portal.learningportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private Long teacherId;

    private Boolean active;
}