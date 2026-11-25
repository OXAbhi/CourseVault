package com.portal.learningportal.repository;

import com.portal.learningportal.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    long countByQuizId(Long quizId);
}