package com.portal.learningportal.dto;

public record OpenAiTestResponse(
        boolean ok,
        String model,
        String reply
) {
}