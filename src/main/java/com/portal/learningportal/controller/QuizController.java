package com.portal.learningportal.controller;

import com.portal.learningportal.dto.QuizAttemptResponse;
import com.portal.learningportal.dto.QuizResponse;
import com.portal.learningportal.dto.QuizSubmitRequest;
import com.portal.learningportal.service.QuizService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuizResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuiz(id));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<QuizResponse> publish(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean published = Boolean.TRUE.equals(body.get("published"));
        return ResponseEntity.ok(quizService.publish(id, published));
    }

    @PostMapping("/{id}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> submit(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.submitAttempt(id, request));
    }

    @GetMapping("/{id}/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> latestAttempt(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getMyLatestAttempt(id));
    }
}