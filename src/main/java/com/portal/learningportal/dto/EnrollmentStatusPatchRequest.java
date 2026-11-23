package com.portal.learningportal.dto;

import com.portal.learningportal.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentStatusPatchRequest {

    @NotNull
    private EnrollmentStatus status;
}