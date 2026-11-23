package com.portal.learningportal.controller;

import com.portal.learningportal.dto.GradeSubmissionRequest;
import com.portal.learningportal.dto.SubmissionResponse;
import com.portal.learningportal.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {

    private final AssignmentService assignmentService;

    public SubmissionController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PutMapping("/api/submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public SubmissionResponse grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request
    ) {
        return assignmentService.gradeSubmission(id, request);
    }
}