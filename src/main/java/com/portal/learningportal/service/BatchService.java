package com.portal.learningportal.service;

import com.portal.learningportal.dto.BatchCreateRequest;
import com.portal.learningportal.dto.BatchResponse;
import com.portal.learningportal.dto.BatchUpdateRequest;
import com.portal.learningportal.entity.Batch;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.BatchRepository;
import com.portal.learningportal.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;

    public BatchService(
            BatchRepository batchRepository,
            CourseRepository courseRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService
    ) {
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> listBatchesForCourse(Long courseId) {
        User user = currentUserService.requireAuthenticatedUser();
        courseAccessService.requireReadableCourse(user, courseId);
        return batchRepository.findByCourseId(courseId).stream()
                .sorted(Comparator.comparing(Batch::getName))
                .map(BatchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatch(Long batchId) {
        User user = currentUserService.requireAuthenticatedUser();
        Batch batch = findBatchOrThrow(batchId);
        courseAccessService.requireReadableCourse(user, batch.getCourse().getId());
        return BatchResponse.from(batch);
    }

    @Transactional
    public BatchResponse createBatch(Long courseId, BatchCreateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Batch batch = Batch.builder()
                .name(request.getName())
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .course(course)
                .build();
        return BatchResponse.from(batchRepository.save(batch));
    }

    @Transactional
    public BatchResponse updateBatch(Long batchId, BatchUpdateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        Batch batch = findBatchOrThrow(batchId);
        batch.setName(request.getName());
        batch.setAcademicYear(request.getAcademicYear());
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        return BatchResponse.from(batchRepository.save(batch));
    }

    private Batch findBatchOrThrow(Long batchId) {
        return batchRepository.findByIdWithCourse(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
    }
}