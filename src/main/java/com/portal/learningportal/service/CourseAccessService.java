package com.portal.learningportal.service;

import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.CourseRepository;
import com.portal.learningportal.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseAccessService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseAccessService(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public boolean canReadCourse(User user, Long courseId) {
        if (user.getRole() == Role.ADMIN) {
            return courseRepository.existsById(courseId);
        }
        if (user.getRole() == Role.TEACHER) {
            return courseRepository.existsByIdAndTeacherId(courseId, user.getId());
        }
        if (user.getRole() == Role.STUDENT) {
            return enrollmentRepository.isStudentEnrolledInCourse(
                    user.getId(), courseId, EnrollmentStatus.ACTIVE);
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean canManageCourse(User user, Long courseId) {
        if (user.getRole() == Role.ADMIN) {
            return courseRepository.existsById(courseId);
        }
        if (user.getRole() == Role.TEACHER) {
            return courseRepository.existsByIdAndTeacherId(courseId, user.getId());
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Course requireReadableCourse(User user, Long courseId) {
        Course course = courseRepository.findByIdWithTeacher(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (!canReadCourse(user, courseId)) {
            throw new ForbiddenException("Access denied");
        }
        return course;
    }

    @Transactional(readOnly = true)
    public Course requireManageableCourse(User user, Long courseId) {
        Course course = courseRepository.findByIdWithTeacher(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (!canManageCourse(user, courseId)) {
            throw new ForbiddenException("Access denied");
        }
        return course;
    }
}