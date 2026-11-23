package com.portal.learningportal.controller;

import com.portal.learningportal.dto.BatchCreateRequest;
import com.portal.learningportal.dto.BatchResponse;
import com.portal.learningportal.dto.BatchUpdateRequest;
import com.portal.learningportal.dto.EnrollmentResponse;
import com.portal.learningportal.service.BatchService;
import com.portal.learningportal.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BatchController {

    private final BatchService batchService;
    private final EnrollmentService enrollmentService;

    public BatchController(BatchService batchService, EnrollmentService enrollmentService) {
        this.batchService = batchService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/api/courses/{courseId}/batches")
    public List<BatchResponse> listBatchesForCourse(@PathVariable Long courseId) {
        return batchService.listBatchesForCourse(courseId);
    }

    @PostMapping("/api/courses/{courseId}/batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchResponse> createBatch(
            @PathVariable Long courseId,
            @Valid @RequestBody BatchCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(courseId, request));
    }

    @GetMapping("/api/batches/{id}")
    public BatchResponse getBatch(@PathVariable Long id) {
        return batchService.getBatch(id);
    }

    @GetMapping("/api/batches/{id}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> listBatchEnrollments(@PathVariable Long id) {
        return enrollmentService.listEnrollmentsForBatch(id);
    }

    @PutMapping("/api/batches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BatchResponse updateBatch(@PathVariable Long id, @Valid @RequestBody BatchUpdateRequest request) {
        return batchService.updateBatch(id, request);
    }
}