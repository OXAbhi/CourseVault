package com.portal.learningportal.dto;

import com.portal.learningportal.entity.Quiz;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class QuizResponse {

    private final Long id;
    private final String title;
    private final boolean published;
    private final Instant createdAt;
    private final Long courseId;
    private final Long sourceResourceId;
    private final String sourceResourceTitle;
    private final String createdByUsername;
    private final int questionCount;
    private final List<QuizQuestionResponse> questions;
  private final Boolean studentHasAttempt;
    private final Boolean studentCanRetake;

    public static QuizResponse from(Quiz quiz, boolean includeAnswers) {
        return from(quiz, includeAnswers, null, null);
    }

    public static QuizResponse from(Quiz quiz, boolean includeAnswers, Boolean studentHasAttempt, Boolean studentCanRetake) {
        List<QuizQuestionResponse> questions = quiz.getQuestions() == null
                ? List.of()
                : quiz.getQuestions().stream()
                        .map(q -> QuizQuestionResponse.from(q, includeAnswers))
                        .toList();
        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .published(Boolean.TRUE.equals(quiz.getIsPublished()))
                .createdAt(quiz.getCreatedAt())
                .courseId(quiz.getCourse().getId())
                .sourceResourceId(quiz.getSourceResource().getId())
                .sourceResourceTitle(quiz.getSourceResource().getTitle())
                .createdByUsername(quiz.getCreatedBy().getUsername())
                .questionCount(questions.size())
                .questions(questions)
                .studentHasAttempt(studentHasAttempt)
                .studentCanRetake(studentCanRetake)
                .build();
    }

    public static QuizResponse summary(Quiz quiz, int questionCount) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .published(Boolean.TRUE.equals(quiz.getIsPublished()))
                .createdAt(quiz.getCreatedAt())
                .courseId(quiz.getCourse().getId())
                .sourceResourceId(quiz.getSourceResource().getId())
                .sourceResourceTitle(quiz.getSourceResource().getTitle())
                .createdByUsername(quiz.getCreatedBy().getUsername())
                .questionCount(questionCount)
                .questions(List.of())
                .build();
    }
}