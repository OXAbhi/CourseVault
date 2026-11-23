package com.portal.learningportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusPatchRequest {

    @NotNull
    private Boolean active;
}