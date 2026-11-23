package com.portal.learningportal.controller;

import com.portal.learningportal.dto.ResourceResponse;
import com.portal.learningportal.entity.ResourceType;
import com.portal.learningportal.service.CourseResourceService;
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

import java.util.List;

@RestController
public class CourseResourceController {

    private final CourseResourceService courseResourceService;

    public CourseResourceController(CourseResourceService courseResourceService) {
        this.courseResourceService = courseResourceService;
    }

    @GetMapping("/api/courses/{courseId}/resources")
    public List<ResourceResponse> listResources(@PathVariable Long courseId) {
        return courseResourceService.listCourseResources(courseId);
    }

    @PostMapping(path = "/api/courses/{courseId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ResourceResponse> uploadResource(
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam ResourceType resourceType,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "false") boolean published,
            @RequestPart("file") MultipartFile file
    ) {
        ResourceResponse body = courseResourceService.uploadResource(
                courseId, title, description, resourceType, tags, published, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}