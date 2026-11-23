package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourseId(Long courseId);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course c
            JOIN FETCH c.teacher
            JOIN FETCH a.createdBy
            WHERE a.course.id = :courseId
            """)
    List<Assignment> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course c
            JOIN FETCH c.teacher
            JOIN FETCH a.createdBy
            WHERE a.id = :id
            """)
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);
}