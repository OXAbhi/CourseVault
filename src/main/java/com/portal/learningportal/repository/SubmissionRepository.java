package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.assignment a
            JOIN FETCH a.course
            WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId
            """)
    Optional<Submission> findByAssignmentIdAndStudentIdWithDetails(
            @Param("assignmentId") Long assignmentId,
            @Param("studentId") Long studentId
    );

    List<Submission> findByAssignmentId(Long assignmentId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.assignment a
            JOIN FETCH a.course c
            JOIN FETCH c.teacher
            WHERE s.assignment.id = :assignmentId
            """)
    List<Submission> findByAssignmentIdWithDetails(@Param("assignmentId") Long assignmentId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.assignment a
            JOIN FETCH a.course c
            JOIN FETCH c.teacher
            WHERE s.id = :id
            """)
    Optional<Submission> findByIdWithDetails(@Param("id") Long id);

    List<Submission> findByStudentId(Long studentId);
}