package com.portal.learningportal.repository;

import com.portal.learningportal.entity.QuizRetakeGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizRetakeGrantRepository extends JpaRepository<QuizRetakeGrant, Long> {

    Optional<QuizRetakeGrant> findByQuizIdAndStudentId(Long quizId, Long studentId);

    List<QuizRetakeGrant> findByQuizIdAndStudentIdInAndConsumedFalse(Long quizId, Collection<Long> studentIds);
}