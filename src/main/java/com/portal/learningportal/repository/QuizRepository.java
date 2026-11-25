package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("""
            SELECT q FROM Quiz q
            JOIN FETCH q.sourceResource
            JOIN FETCH q.createdBy
            WHERE q.course.id = :courseId
            ORDER BY q.createdAt DESC
            """)
    List<Quiz> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    @Query("""
            SELECT q FROM Quiz q
            JOIN FETCH q.course
            JOIN FETCH q.sourceResource
            JOIN FETCH q.createdBy
            LEFT JOIN FETCH q.questions
            WHERE q.id = :id
            """)
    Optional<Quiz> findByIdWithDetails(@Param("id") Long id);
}