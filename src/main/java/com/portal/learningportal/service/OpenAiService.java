package com.portal.learningportal.service;

import com.portal.learningportal.config.OpenAiProperties;
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

    /** Safe hint so teachers can see if a stale OPENAI_API_KEY env var overwrote application-local. */
    public String getKeyPrefix() {
        String key = properties.getApiKey();
        if (key == null || key.isBlank()) {
            return "";
        }
        key = key.trim();
        int end = Math.min(12, key.length());
        return key.substring(0, end) + (key.length() > end ? "…" : "");
    }

    /**
     * Minimal chat call to verify API key and connectivity (local / pre-quiz testing).
     */
    /**
     * Ask the model to return strict JSON for multiple-choice questions based on study material.
     */
    public String generateQuizJson(String sourceTitle, String pdfText, int questionCount) {
        if (!properties.isConfigured()) {
            throw new BadRequestException("AI API key is not configured.");
        }
        if (pdfText == null || pdfText.isBlank()) {
            throw new BadRequestException("No text to generate a quiz from.");
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
        RestClient client = builder.build();
        String userPrompt = """
                Create exactly %d multiple-choice questions from the study material below.
                Output ONLY valid JSON (no markdown) in this shape:
                {"questions":[{"text":"...","options":["A","B","C","D"],"correctIndex":0}]}
                correctIndex is 0-3 matching options order. Base questions only on the material.
                Material title: %s

                --- BEGIN MATERIAL ---
                %s
                --- END MATERIAL ---
                """.formatted(questionCount, sourceTitle, pdfText);
        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "You write exam questions for university courses. Return only JSON."},
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": 4096,
                  "temperature": 0.3
                }
                """.formatted(escapeJson(properties.getModel()), escapeJson(userPrompt));
        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractAssistantReply(response);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("AI request failed (" + ex.getStatusCode().value() + "): "
                    + truncate(ex.getResponseBodyAsString(), 400));
        } catch (BadRequestException ex) {
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
        RestClient client = builder.build();
        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "You help teachers on an education platform. Follow instructions exactly."},
                    {"role": "user", "content": "Say hello in one word, nothing else."}
                  ],
                  "max_tokens": 64
                }
                """.formatted(escapeJson(properties.getModel()));
        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractAssistantReply(response);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("OpenAI request failed (" + ex.getStatusCode().value() + "): "
                    + truncate(ex.getResponseBodyAsString(), 300));
        } catch (Exception ex) {
            throw new BadRequestException("OpenAI request failed: " + ex.getMessage());
        }
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