package com.portal.learningportal.service;

import com.portal.learningportal.config.OpenAiProperties;
import com.portal.learningportal.dto.QuizGenerateRequest;
import com.portal.learningportal.exception.AiBusyException;
import com.portal.learningportal.exception.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenAiService {

    private final OpenAiProperties properties;

    public OpenAiService(OpenAiProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String getModel() {
        return properties.getModel();
    }

    public String getBaseUrl() {
        return normalizeBaseUrl(properties.getBaseUrl());
    }

    /**
     * Ask the model to return strict JSON for multiple-choice questions based on study material.
     */
    public String generateQuizJson(QuizGenerateRequest request, String sourceTitle, String pdfText) {
        if (!properties.isConfigured()) {
            throw new BadRequestException("AI API key is not configured.");
        }
        if (pdfText == null || pdfText.isBlank()) {
            throw new BadRequestException("No text to generate a quiz from.");
        }
        int count = request.resolvedCount();
        String teacherInstructions = request.instructionsOrEmpty();
        String difficulty = request.resolvedDifficulty();
        String systemPrompt = """
                You create university-level multiple-choice quizzes.
                Rules (never break these):
                - Use ONLY the source text provided by the user.
                - Test concepts, code, and application from the body of the material.
                - Never ask about headings, chapter numbers, table of contents, page numbers, or how to use the document.
                - Produce exactly %d questions, each with exactly 4 distinct options and one correct answer (correctIndex 0-3).
                - Option strings must NOT start with A. B. C. or D. prefixes.
                - Do not duplicate questions.
                - Difficulty level: %s.
                - If the teacher gives extra instructions, follow them only when they do not conflict with these rules.
                - Return ONLY a JSON array: [{"question":"...","options":["...","...","...","..."],"correctIndex":0,"explanation":"..."}]
                """.formatted(count, difficulty);
        String userPrompt = """
                Material title: %s
                %s

                --- BEGIN SOURCE TEXT ---
                %s
                --- END SOURCE TEXT ---
                """.formatted(
                sourceTitle,
                teacherInstructions.isBlank() ? "" : "Teacher focus: " + teacherInstructions,
                pdfText);
        return chatCompletion(systemPrompt, userPrompt, 8192, 0.3);
    }

    private String chatCompletion(String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (baseUrl.contains("openrouter.ai")) {
            builder.defaultHeader("HTTP-Referer", "http://localhost:4200")
                    .defaultHeader("X-Title", "CourseVault");
        }
        RestClient client = builder.build();
        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": %d,
                  "temperature": %s
                }
                """.formatted(
                escapeJson(properties.getModel()),
                escapeJson(systemPrompt),
                escapeJson(userPrompt),
                maxTokens,
                temperature);
        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractAssistantReply(response);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new AiBusyException("AI is busy, try again in a minute");
            }
            throw new BadRequestException("AI request failed (" + ex.getStatusCode().value() + "): "
                    + truncate(ex.getResponseBodyAsString(), 400));
        } catch (AiBusyException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI request failed: " + ex.getMessage());
        }
    }

    public String testConnection() {
        if (!properties.isConfigured()) {
            throw new BadRequestException(
                    "OpenAI API key is not set. Add openai.api-key=sk-... to application-local.properties.");
        }
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (baseUrl.contains("openrouter.ai")) {
            builder.defaultHeader("HTTP-Referer", "http://localhost:4200")
                    .defaultHeader("X-Title", "CourseVault");
        }
        return chatCompletion(
                "You help teachers on an education platform. Follow instructions exactly.",
                "Say hello in one word, nothing else.",
                64,
                0.3);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String extractAssistantReply(String responseJson) {
        int choices = responseJson.indexOf("\"choices\"");
        if (choices < 0) {
            throw new BadRequestException("OpenAI returned an unexpected response.");
        }
        int messageKey = responseJson.indexOf("\"message\"", choices);
        if (messageKey < 0) {
            throw new BadRequestException("OpenAI returned an unexpected response.");
        }
        int colon = responseJson.indexOf(':', messageKey);
        int startBrace = responseJson.indexOf('{', colon);
        if (startBrace < 0) {
            throw new BadRequestException("OpenAI returned an unexpected response.");
        }
        int messageEnd = findMatchingBraceFrom(responseJson, startBrace);
        String messageBlock = responseJson.substring(startBrace, messageEnd);

        String content = readJsonStringField(messageBlock, "\"content\"");
        if (content != null && !content.isBlank()) {
            return content.trim();
        }
        String refusal = readJsonStringField(messageBlock, "\"refusal\"");
        if (refusal != null && !refusal.isBlank()) {
            throw new BadRequestException("Model declined the request: " + refusal.trim());
        }
        throw new BadRequestException("Model returned no text (empty content). Try another model on OpenRouter.");
    }

    private static int findMatchingBraceFrom(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return json.length();
    }

    /** Returns null if the field is missing, JSON null, or not a string. */
    private static String readJsonStringField(String json, String fieldName) {
        int key = json.indexOf(fieldName);
        if (key < 0) {
            return null;
        }
        int colon = json.indexOf(':', key + fieldName.length());
        if (colon < 0) {
            return null;
        }
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        if (pos + 3 < json.length() && json.startsWith("null", pos)) {
            return null;
        }
        if (pos >= json.length() || json.charAt(pos) != '"') {
            return null;
        }
        return parseJsonStringValue(json, pos);
    }

    private static String parseJsonStringValue(String json, int openQuoteIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = openQuoteIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                sb.append(switch (next) {
                    case '"', '\\' -> next;
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> next;
                });
                i++;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}