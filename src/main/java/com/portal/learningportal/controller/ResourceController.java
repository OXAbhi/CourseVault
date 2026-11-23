package com.portal.learningportal.controller;

import com.portal.learningportal.dto.ResourceAccessResponse;
import com.portal.learningportal.dto.ResourceResponse;
import com.portal.learningportal.service.CourseResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final CourseResourceService courseResourceService;

    public ResourceController(CourseResourceService courseResourceService) {
        this.courseResourceService = courseResourceService;
    }

    @GetMapping("/{id}")
    public ResourceResponse getResource(@PathVariable Long id) {
        return courseResourceService.getResource(id);
    }

    @GetMapping("/{id}/access")
    public ResourceAccessResponse getResourceAccess(@PathVariable Long id) {
        return courseResourceService.getResourceAccess(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        courseResourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}