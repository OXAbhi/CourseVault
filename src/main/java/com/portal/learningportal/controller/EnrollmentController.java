package com.portal.learningportal.controller;

import com.portal.learningportal.dto.EnrollmentCreateRequest;
import com.portal.learningportal.dto.EnrollmentResponse;
import com.portal.learningportal.dto.EnrollmentStatusPatchRequest;
import com.portal.learningportal.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@PreAuthorize("hasRole('ADMIN')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<EnrollmentResponse> listEnrollments() {
        return enrollmentService.listEnrollments();
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> createEnrollment(@Valid @RequestBody EnrollmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.createEnrollment(request));
    }

    @PatchMapping("/{id}")
    public EnrollmentResponse patchEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentStatusPatchRequest request
    ) {
        return enrollmentService.patchEnrollmentStatus(id, request);
    }
}