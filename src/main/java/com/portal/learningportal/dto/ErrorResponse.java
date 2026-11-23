package com.portal.learningportal.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;
}