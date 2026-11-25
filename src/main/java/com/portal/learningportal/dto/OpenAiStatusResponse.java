package com.portal.learningportal.dto;

public record OpenAiStatusResponse(
        boolean configured,
        String model,
        String baseUrl,
        /** First characters of the loaded key (for debugging env vs local file — not the full secret). */
        String keyPrefix
) {
}