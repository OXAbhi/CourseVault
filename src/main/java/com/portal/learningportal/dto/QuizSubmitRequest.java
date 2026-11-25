package com.portal.learningportal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuizSubmitRequest(
        @NotEmpty @Valid List<QuizAnswerItem> answers
) {
}