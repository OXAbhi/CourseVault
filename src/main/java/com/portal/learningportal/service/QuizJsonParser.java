package com.portal.learningportal.service;

import com.portal.learningportal.exception.BadRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QuizJsonParser {

    private final ObjectMapper objectMapper;

    public QuizJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ParsedQuestion(String text, List<String> options, int correctIndex, String explanation) {
    }

    public List<ParsedQuestion> parseModelJson(String raw, int expectedCount) {
        String json = stripCodeFence(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questions = root.isArray() ? root : root.get("questions");
            if (questions == null || !questions.isArray() || questions.isEmpty()) {
                throw new BadRequestException("AI returned no quiz questions. Try again or use another model.");
            }
            List<ParsedQuestion> result = new ArrayList<>();
            Set<String> seenQuestions = new HashSet<>();
            for (JsonNode node : questions) {
                ParsedQuestion parsed = parseQuestionNode(node);
                if (parsed == null) {
                    continue;
                }
                String key = parsed.text().toLowerCase(Locale.ROOT);
                if (seenQuestions.contains(key)) {
                    continue;
                }
                seenQuestions.add(key);
                result.add(parsed);
            }
            if (result.isEmpty()) {
                throw new BadRequestException("Could not parse quiz questions from AI response.");
            }
            if (result.size() < expectedCount) {
                throw new BadRequestException("AI returned fewer questions than requested (" + result.size()
                        + " of " + expectedCount + ").");
            }
            return result.subList(0, Math.min(result.size(), expectedCount));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid quiz JSON from AI: " + ex.getMessage());
        }
    }

    private ParsedQuestion parseQuestionNode(JsonNode node) {
        String text = text(node, "question", "text");
        if (text.isBlank()) {
            return null;
        }
        JsonNode optionsNode = node.get("options");
        if (optionsNode == null || !optionsNode.isArray() || optionsNode.size() < 4) {
            return null;
        }
        List<String> options = new ArrayList<>();
        for (JsonNode o : optionsNode) {
            options.add(normalizeOption(o.asText()));
        }
        if (options.size() > 4) {
            options = options.subList(0, 4);
        }
        while (options.size() < 4) {
            options.add("Option " + (char) ('A' + options.size()));
        }
        int correctIndex = node.has("correctIndex")
                ? node.get("correctIndex").asInt()
                : letterToIndex(node.path("correct").asText(""));
        if (correctIndex < 0 || correctIndex > 3) {
            correctIndex = 0;
        }
        String explanation = node.has("explanation") && !node.get("explanation").isNull()
                ? node.get("explanation").asText().trim()
                : null;
        return new ParsedQuestion(text, List.copyOf(options), correctIndex, explanation);
    }

    public static String normalizeOption(String option) {
        if (option == null) {
            return "";
        }
        String trimmed = option.trim();
        return trimmed.replaceFirst("^[A-Da-d][.)]\\s*", "").trim();
    }

    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText().trim();
            }
        }
        return "";
    }

    private static int letterToIndex(String letter) {
        if (letter == null || letter.isBlank()) {
            return 0;
        }
        char c = Character.toUpperCase(letter.trim().charAt(0));
        if (c >= 'A' && c <= 'D') {
            return c - 'A';
        }
        return 0;
    }

    private static String stripCodeFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int end = trimmed.lastIndexOf("```");
            if (end > 0) {
                trimmed = trimmed.substring(0, end);
            }
        }
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}