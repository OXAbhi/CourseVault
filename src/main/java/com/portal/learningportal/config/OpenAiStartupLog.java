package com.portal.learningportal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs effective OpenAI/OpenRouter settings at startup so local overrides are easy to verify.
 */
@Component
public class OpenAiStartupLog implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenAiStartupLog.class);

    private final OpenAiProperties properties;

    public OpenAiStartupLog(OpenAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("OpenAI: no API key loaded (set openai.api-key in application-local.properties or OPENAI_API_KEY)");
            return;
        }
        log.info("OpenAI: model={}, baseUrl={} (if this differs from application-local.properties, check OPENAI_MODEL / OPENAI_BASE_URL env vars)",
                properties.getModel(),
                properties.getBaseUrl());
    }
}