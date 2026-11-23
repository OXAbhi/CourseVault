package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Enrollment;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    List<Enrollment> findByBatchId(Long batchId);

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.student
            JOIN FETCH e.batch b
            JOIN FETCH b.course
            WHERE b.id = :batchId
            ORDER BY e.student.username
            """)
    List<Enrollment> findByBatchIdWithDetails(@Param("batchId") Long batchId);

    Optional<Enrollment> findByStudentIdAndBatchId(Long studentId, Long batchId);

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.student
            JOIN FETCH e.batch b
            JOIN FETCH b.course
            """)
    List<Enrollment> findAllWithDetails();

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.student
            JOIN FETCH e.batch b
            JOIN FETCH b.course
            WHERE e.id = :id
            """)
    Optional<Enrollment> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Enrollment e
            JOIN e.batch b
            WHERE e.student.id = :studentId AND b.course.id = :courseId AND e.status = :status
            """)
    boolean isStudentEnrolledInCourse(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentStatus status);

    @Query("""
            SELECT DISTINCT e.student FROM Enrollment e
            JOIN e.batch b
            WHERE b.course.id = :courseId AND e.status = :status
            """)
    List<User> findActiveStudentsByCourseId(
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentStatus status);
}