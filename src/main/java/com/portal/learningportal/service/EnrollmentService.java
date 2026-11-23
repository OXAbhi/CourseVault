package com.portal.learningportal.service;

import com.portal.learningportal.dto.EnrollmentCreateRequest;
import com.portal.learningportal.dto.EnrollmentResponse;
import com.portal.learningportal.dto.EnrollmentStatusPatchRequest;
import com.portal.learningportal.entity.Batch;
import com.portal.learningportal.entity.Enrollment;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ConflictException;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.BatchRepository;
import com.portal.learningportal.repository.EnrollmentRepository;
import com.portal.learningportal.repository.UserRepository;
import com.portal.learningportal.service.CourseAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            BatchRepository batchRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listEnrollmentsForBatch(Long batchId) {
        User user = currentUserService.requireAnyRole(Role.ADMIN, Role.TEACHER);
        Batch batch = batchRepository.findByIdWithCourse(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
        if (user.getRole() == Role.TEACHER
                && !courseAccessService.canManageCourse(user, batch.getCourse().getId())) {
            throw new ForbiddenException("Access denied");
        }
        return enrollmentRepository.findByBatchIdWithDetails(batchId).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listEnrollments() {
        currentUserService.requireRole(Role.ADMIN);
        return enrollmentRepository.findAllWithDetails().stream()
                .sorted(Comparator.comparing(Enrollment::getId))
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("User must be a student");
        }
        if (!Boolean.TRUE.equals(student.getIsActive())) {
            throw new ForbiddenException("Student account must be active");
        }
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
        if (enrollmentRepository.findByStudentIdAndBatchId(student.getId(), batch.getId()).isPresent()) {
            throw new ConflictException("Student is already enrolled in this batch");
        }
        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .batch(batch)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        Long id = enrollmentRepository.save(enrollment).getId();
        return EnrollmentResponse.from(enrollmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found")));
    }

    @Transactional
    public EnrollmentResponse patchEnrollmentStatus(Long enrollmentId, EnrollmentStatusPatchRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollment.setStatus(request.getStatus());
        enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(enrollmentRepository.findByIdWithDetails(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found")));
    }
}