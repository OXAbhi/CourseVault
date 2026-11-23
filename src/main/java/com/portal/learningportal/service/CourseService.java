package com.portal.learningportal.service;

import com.portal.learningportal.dto.CourseActivePatchRequest;
import com.portal.learningportal.dto.CourseCreateRequest;
import com.portal.learningportal.dto.CourseResponse;
import com.portal.learningportal.dto.CourseUpdateRequest;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ConflictException;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.CourseRepository;
import com.portal.learningportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;

    public CourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService
    ) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCoursesForCurrentUser() {
        User user = currentUserService.requireAuthenticatedUser();
        List<Course> courses;
        if (user.getRole() == Role.ADMIN) {
            courses = courseRepository.findAllWithTeacher();
        } else if (user.getRole() == Role.TEACHER) {
            courses = courseRepository.findByTeacherId(user.getId());
        } else if (user.getRole() == Role.STUDENT) {
            courses = courseRepository.findCoursesForEnrolledStudent(user.getId(), EnrollmentStatus.ACTIVE);
        } else {
            courses = List.of();
        }
        return courses.stream()
                .sorted(Comparator.comparing(Course::getCode))
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId) {
        User user = currentUserService.requireAuthenticatedUser();
        Course course = courseAccessService.requireReadableCourse(user, courseId);
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        if (courseRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Course code already exists");
        }
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Assigned user must be a teacher");
        }
        if (!Boolean.TRUE.equals(teacher.getIsActive())) {
            throw new ForbiddenException("Teacher account must be active");
        }
        Course course = Course.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .teacher(teacher)
                .isActive(request.getActive() == null || request.getActive())
                .build();
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        User user = currentUserService.requireAuthenticatedUser();
        Course course = courseAccessService.requireManageableCourse(user, courseId);
        if (user.getRole() == Role.TEACHER && request.getTeacherId() != null
                && !request.getTeacherId().equals(user.getId())) {
            throw new ForbiddenException("Teachers cannot reassign courses");
        }
        if (user.getRole() == Role.TEACHER && request.getActive() != null) {
            throw new ForbiddenException("Teachers cannot change active status");
        }
        if (request.getTeacherId() != null && user.getRole() == Role.ADMIN) {
            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            if (teacher.getRole() != Role.TEACHER) {
                throw new ForbiddenException("Assigned user must be a teacher");
            }
            course.setTeacher(teacher);
        }
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        if (request.getActive() != null) {
            course.setIsActive(request.getActive());
        }
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse patchCourseActive(Long courseId, CourseActivePatchRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        course.setIsActive(request.getActive());
        return CourseResponse.from(courseRepository.save(course));
    }
}