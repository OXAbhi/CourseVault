package com.portal.learningportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuizGenerateRequest(
        @Size(max = 1000) String instructions,
        @Pattern(regexp = "^$|^\\d{1,4}(-\\d{1,4})?$", message = "Pages must look like 12 or 12-20")
        String pages,
        @Min(5) @Max(15) Integer questionCount,
        @Pattern(regexp = "^$|^(easy|medium|hard)$", message = "Difficulty must be easy, medium, or hard")
        String difficulty
) {
    public int resolvedCount() {
        if (questionCount == null) {
            return 5;
        }
        return questionCount;
    }

    public String resolvedDifficulty() {
        if (difficulty == null || difficulty.isBlank()) {
            return "medium";
        }
        return difficulty.trim().toLowerCase();
    }

    public String instructionsOrEmpty() {
        return instructions == null ? "" : instructions.trim();
    }

    public String pagesOrBlank() {
        return pages == null ? "" : pages.trim();
    }
}