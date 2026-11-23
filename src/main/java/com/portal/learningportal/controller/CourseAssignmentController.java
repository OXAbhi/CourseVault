package com.portal.learningportal.controller;

import com.portal.learningportal.dto.AssignmentResponse;
import com.portal.learningportal.service.AssignmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
public class CourseAssignmentController {

    private final AssignmentService assignmentService;

    public CourseAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    public List<AssignmentResponse> listAssignments(@PathVariable Long courseId) {
        return assignmentService.listCourseAssignments(courseId);
    }

    @PostMapping(path = "/api/courses/{courseId}/assignments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueAt,
            @RequestParam int maxMarks,
            @RequestParam(defaultValue = "false") boolean published,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        AssignmentResponse body = assignmentService.createAssignment(
                courseId, title, description, dueAt, maxMarks, published, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}