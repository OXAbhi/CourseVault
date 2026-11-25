package com.portal.learningportal.dto;

import com.portal.learningportal.entity.QuizQuestion;
import com.portal.learningportal.service.QuizJsonParser;
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
    private final String explanation;

    public static QuizQuestionResponse from(QuizQuestion q, boolean includeAnswers) {
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .options(List.of(
                        QuizJsonParser.normalizeOption(q.getOptionA()),
                        QuizJsonParser.normalizeOption(q.getOptionB()),
                        QuizJsonParser.normalizeOption(q.getOptionC()),
                        QuizJsonParser.normalizeOption(q.getOptionD())))
                .correctIndex(includeAnswers ? q.getCorrectIndex() : -1)
                .sortOrder(q.getSortOrder())
                .explanation(includeAnswers ? q.getExplanation() : null)
                .build();
    }
}