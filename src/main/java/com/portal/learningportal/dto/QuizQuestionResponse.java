package com.portal.learningportal.dto;

import com.portal.learningportal.entity.QuizQuestion;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizQuestionResponse {

    private final Long id;
    private final String questionText;
    private final List<String> options;
    private final int correctIndex;
    private final int sortOrder;

    public static QuizQuestionResponse from(QuizQuestion q, boolean includeAnswers) {
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .options(List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .correctIndex(includeAnswers ? q.getCorrectIndex() : -1)
                .sortOrder(q.getSortOrder())
                .build();
    }
}