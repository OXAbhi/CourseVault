package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Query("SELECT b FROM Batch b JOIN FETCH b.course WHERE b.course.id = :courseId")
    List<Batch> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT b FROM Batch b JOIN FETCH b.course WHERE b.id = :id")
    Optional<Batch> findByIdWithCourse(@Param("id") Long id);
}