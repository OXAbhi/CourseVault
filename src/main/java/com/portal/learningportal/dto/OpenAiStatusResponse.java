package com.portal.learningportal.dto;

public record OpenAiStatusResponse(
        boolean configured,
        String model,
        String baseUrl
) {
}