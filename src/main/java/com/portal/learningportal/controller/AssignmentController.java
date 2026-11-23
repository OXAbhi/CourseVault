package com.portal.learningportal.controller;

import com.portal.learningportal.dto.AssignmentResponse;
import com.portal.learningportal.dto.AssignmentUpdateRequest;
import com.portal.learningportal.dto.SubmissionResponse;
import com.portal.learningportal.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/assignments/{id}")
    public AssignmentResponse getAssignment(@PathVariable Long id) {
        return assignmentService.getAssignment(id);
    }

    @PutMapping("/api/assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AssignmentResponse updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentUpdateRequest request
    ) {
        return assignmentService.updateAssignment(id, request);
    }

    @PostMapping(path = "/api/assignments/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        SubmissionResponse body = assignmentService.submitOrReplace(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/api/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<SubmissionResponse> listSubmissions(@PathVariable Long id) {
        return assignmentService.listSubmissions(id);
    }
}