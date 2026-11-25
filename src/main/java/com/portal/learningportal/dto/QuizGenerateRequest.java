package com.portal.learningportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record QuizGenerateRequest(
        @Min(1) @Max(15) Integer questionCount
) {
    public int resolvedCount() {
        return questionCount == null ? 5 : questionCount;
    }
}