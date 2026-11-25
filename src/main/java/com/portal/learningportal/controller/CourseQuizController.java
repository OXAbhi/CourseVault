package com.portal.learningportal.controller;

import com.portal.learningportal.dto.QuizResponse;
import com.portal.learningportal.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/quizzes")
public class CourseQuizController {

    private final QuizService quizService;

    public CourseQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuizResponse>> list(@PathVariable Long courseId) {
        return ResponseEntity.ok(quizService.listCourseQuizzes(courseId));
    }
}