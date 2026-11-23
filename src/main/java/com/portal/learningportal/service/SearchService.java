package com.portal.learningportal.service;

import com.portal.learningportal.dto.SearchResultResponse;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.ResourceType;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.BadRequestException;
import com.portal.learningportal.repository.CourseRepository;
import com.portal.learningportal.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SearchService {

    private final ResourceRepository resourceRepository;
    private final CourseRepository courseRepository;
    private final CurrentUserService currentUserService;

    public SearchService(
            ResourceRepository resourceRepository,
            CourseRepository courseRepository,
            CurrentUserService currentUserService
    ) {
        this.resourceRepository = resourceRepository;
        this.courseRepository = courseRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<SearchResultResponse> search(String query, String type, String tag) {
        User user = currentUserService.requireAuthenticatedUser();
        List<Long> courseIds = resolveAccessibleCourseIds(user);
        if (courseIds.isEmpty()) {
            return List.of();
        }

        ResourceType resourceType = parseResourceType(type);
        String tagSlug = toTagSlug(tag);
        String q = query == null ? "" : query.trim();
        boolean studentsOnlyPublished = user.getRole() == Role.STUDENT;

        List<Resource> resources = resourceRepository.searchInCourses(
                courseIds,
                studentsOnlyPublished,
                q.isEmpty() ? null : q,
                resourceType,
                tagSlug == null || tagSlug.isEmpty() ? null : tagSlug);

        return resources.stream().map(SearchResultResponse::from).toList();
    }

    private List<Long> resolveAccessibleCourseIds(User user) {
        return switch (user.getRole()) {
            case ADMIN -> courseRepository.findAllWithTeacher().stream().map(Course::getId).toList();
            case TEACHER -> courseRepository.findByTeacherId(user.getId()).stream().map(Course::getId).toList();
            case STUDENT -> courseRepository
                    .findCoursesForEnrolledStudent(user.getId(), EnrollmentStatus.ACTIVE)
                    .stream()
                    .map(Course::getId)
                    .toList();
        };
    }

    private static ResourceType parseResourceType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return ResourceType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid resource type");
        }
    }

    private static String toTagSlug(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        return tag.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}