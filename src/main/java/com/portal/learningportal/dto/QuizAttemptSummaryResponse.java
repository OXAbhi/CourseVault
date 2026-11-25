package com.portal.learningportal.dto;

import com.portal.learningportal.entity.QuizAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class QuizAttemptSummaryResponse {

    private final Long attemptId;
    private final Long studentId;
    private final String studentUsername;
    private final String studentDisplayName;
    private final boolean retakePending;
    private final int score;
    private final int totalQuestions;
    private final Instant submittedAt;

    public static QuizAttemptSummaryResponse from(QuizAttempt attempt) {
        return from(attempt, false);
    }

    public static QuizAttemptSummaryResponse from(QuizAttempt attempt, boolean retakePending) {
        var student = attempt.getStudent();
        String display = "%s %s".formatted(
                student.getFirstName() != null ? student.getFirstName() : "",
                student.getLastName() != null ? student.getLastName() : ""
        ).trim();
        return QuizAttemptSummaryResponse.builder()
                .attemptId(attempt.getId())
                .studentId(student.getId())
                .studentUsername(student.getUsername())
                .studentDisplayName(display.isBlank() ? student.getUsername() : display)
                .retakePending(retakePending)
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}