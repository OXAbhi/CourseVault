package com.portal.learningportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizQuestionUpdateRequest(
        @NotBlank @Size(max = 2000) String questionText,
        @Size(min = 4, max = 4) List<@NotBlank @Size(max = 512) String> options,
        @Min(0) @Max(3) int correctIndex
) {
}