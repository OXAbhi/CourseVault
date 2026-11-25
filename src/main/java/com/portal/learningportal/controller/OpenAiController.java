package com.portal.learningportal.controller;

import com.portal.learningportal.dto.OpenAiStatusResponse;
import com.portal.learningportal.dto.OpenAiTestResponse;
import com.portal.learningportal.service.OpenAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class OpenAiController {

    private final OpenAiService openAiService;

    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    /** Whether a key is loaded (does not expose the key). */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OpenAiStatusResponse> status() {
        return ResponseEntity.ok(new OpenAiStatusResponse(
                openAiService.isConfigured(),
                openAiService.getModel(),
                openAiService.getBaseUrl()
        ));
    }

    /** Calls OpenAI once with a tiny prompt — use after pasting key in application-local.properties. */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OpenAiTestResponse> test() {
        String reply = openAiService.testConnection();
        return ResponseEntity.ok(new OpenAiTestResponse(true, openAiService.getModel(), reply));
    }
}