package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByIdAndTeacherId(Long id, Long teacherId);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher")
    List<Course> findAllWithTeacher();

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.teacher.id = :teacherId")
    List<Course> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
            SELECT DISTINCT c FROM Course c
            JOIN FETCH c.teacher
            JOIN c.batches b
            JOIN b.enrollments e
            WHERE e.student.id = :studentId AND e.status = :status
            """)
    List<Course> findCoursesForEnrolledStudent(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<Course> findByIdWithTeacher(@Param("id") Long id);
}