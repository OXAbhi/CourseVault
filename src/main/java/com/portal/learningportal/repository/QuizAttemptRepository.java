package com.portal.learningportal.repository;

import com.portal.learningportal.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    @Query("""
            SELECT a FROM QuizAttempt a
            JOIN FETCH a.quiz
            JOIN FETCH a.answers ans
            JOIN FETCH ans.question
            WHERE a.id = :id
            """)
    Optional<QuizAttempt> findByIdWithAnswers(@Param("id") Long id);

    @Query("""
            SELECT a FROM QuizAttempt a
            JOIN FETCH a.quiz
            JOIN FETCH a.answers ans
            JOIN FETCH ans.question
            WHERE a.quiz.id = :quizId AND a.student.id = :studentId
            ORDER BY a.submittedAt DESC
            """)
    List<QuizAttempt> findByQuizIdAndStudentIdOrderBySubmittedAtDesc(
            @Param("quizId") Long quizId,
            @Param("studentId") Long studentId
    );

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(Long quizId, Long studentId);

    @Query("""
            SELECT a FROM QuizAttempt a
            JOIN FETCH a.student
            WHERE a.quiz.id = :quizId
            ORDER BY a.submittedAt DESC
            """)
    List<QuizAttempt> findByQuizIdWithStudent(@Param("quizId") Long quizId);
}