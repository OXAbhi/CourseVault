package com.portal.learningportal.dto;

import com.portal.learningportal.entity.QuizAttempt;
import com.portal.learningportal.entity.QuizAttemptAnswer;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class QuizAttemptResponse {

    private final Long attemptId;
    private final Long quizId;
    private final String quizTitle;
    private final int score;
    private final int totalQuestions;
    private final Instant submittedAt;
    private final List<QuizAttemptResultItem> results;

    public static QuizAttemptResponse from(QuizAttempt attempt) {
        List<QuizAttemptResultItem> results = attempt.getAnswers().stream()
                .sorted(Comparator.comparing(a -> a.getQuestion().getSortOrder()))
                .map(QuizAttemptResponse::toResultItem)
                .toList();
        return QuizAttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .submittedAt(attempt.getSubmittedAt())
                .results(results)
                .build();
    }

    private static QuizAttemptResultItem toResultItem(QuizAttemptAnswer answer) {
        var q = answer.getQuestion();
        return QuizAttemptResultItem.builder()
                .questionId(q.getId())
                .questionText(q.getQuestionText())
                .options(List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .selectedIndex(answer.getSelectedIndex())
                .correctIndex(q.getCorrectIndex())
                .correct(answer.isCorrect())
                .build();
    }
}