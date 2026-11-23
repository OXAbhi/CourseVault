package com.portal.learningportal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeSubmissionRequest {

    @NotNull
    @Min(0)
    private Integer marks;

    private String feedback;
}