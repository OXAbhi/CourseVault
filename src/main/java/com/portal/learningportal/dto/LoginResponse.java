package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final Role role;
    private final UserProfileResponse profile;
}