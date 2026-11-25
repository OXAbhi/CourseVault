package com.portal.learningportal.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizAttemptResultItem {

    private final Long questionId;
    private final String questionText;
    private final List<String> options;
    private final int selectedIndex;
    private final int correctIndex;
    private final boolean correct;
}