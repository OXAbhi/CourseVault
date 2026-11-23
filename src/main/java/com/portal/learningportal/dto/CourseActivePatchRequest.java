package com.portal.learningportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseActivePatchRequest {

    @NotNull
    private Boolean active;
}