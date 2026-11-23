package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;
    private final String firstName;
    private final String lastName;
    private final boolean active;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}