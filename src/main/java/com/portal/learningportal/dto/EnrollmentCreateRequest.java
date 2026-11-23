package com.portal.learningportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentCreateRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long batchId;
}