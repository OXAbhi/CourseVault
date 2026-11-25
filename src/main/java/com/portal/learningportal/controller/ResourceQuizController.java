package com.portal.learningportal.controller;

import com.portal.learningportal.dto.QuizGenerateRequest;
import com.portal.learningportal.dto.QuizResponse;
import com.portal.learningportal.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources/{resourceId}/quizzes")
public class ResourceQuizController {

    private final QuizService quizService;

    public ResourceQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<QuizResponse> generate(
            @PathVariable Long resourceId,
            @Valid @RequestBody(required = false) QuizGenerateRequest request
    ) {
        QuizGenerateRequest body = request == null ? new QuizGenerateRequest(null) : request;
        QuizResponse quiz = quizService.generateFromResource(resourceId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(quiz);
    }
}