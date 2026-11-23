package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("""
            SELECT r FROM Resource r
            JOIN FETCH r.course c
            JOIN FETCH r.uploadedBy
            LEFT JOIN FETCH r.tags
            WHERE c.id = :courseId
            """)
    List<Resource> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    @Query("""
            SELECT r FROM Resource r
            JOIN FETCH r.course
            JOIN FETCH r.uploadedBy
            LEFT JOIN FETCH r.tags
            WHERE r.id = :id
            """)
    Optional<Resource> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT r FROM Resource r
            JOIN FETCH r.course c
            LEFT JOIN FETCH r.tags t
            WHERE c.id IN :courseIds
            AND (:publishedOnly = false OR r.isPublished = true)
            AND (:q IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:resourceType IS NULL OR r.resourceType = :resourceType)
            AND (:tagSlug IS NULL OR EXISTS (
                SELECT 1 FROM r.tags tg WHERE tg.slug = :tagSlug
            ))
            ORDER BY r.createdAt DESC
            """)
    List<Resource> searchInCourses(
            @Param("courseIds") List<Long> courseIds,
            @Param("publishedOnly") boolean publishedOnly,
            @Param("q") String q,
            @Param("resourceType") ResourceType resourceType,
            @Param("tagSlug") String tagSlug
    );
}