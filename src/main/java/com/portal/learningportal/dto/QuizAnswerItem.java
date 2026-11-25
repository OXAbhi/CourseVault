package com.portal.learningportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuizAnswerItem(
        @NotNull Long questionId,
        @Min(0) @Max(3) int selectedIndex
) {
}