package com.portal.learningportal.service;

import com.portal.learningportal.exception.BadRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class QuizJsonParser {

    private final ObjectMapper objectMapper;

    public QuizJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ParsedQuestion(String text, List<String> options, int correctIndex) {
    }

    public List<ParsedQuestion> parseModelJson(String raw) {
        String json = stripCodeFence(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questions = root.get("questions");
            if (questions == null || !questions.isArray() || questions.isEmpty()) {
                throw new BadRequestException("AI returned no quiz questions. Try again or use another model.");
            }
            List<ParsedQuestion> result = new ArrayList<>();
            for (JsonNode node : questions) {
                String text = text(node, "text", "question");
                JsonNode optionsNode = node.get("options");
                if (optionsNode == null || !optionsNode.isArray() || optionsNode.size() < 2) {
                    continue;
                }
                List<String> options = new ArrayList<>();
                optionsNode.forEach(o -> options.add(o.asText().trim()));
                while (options.size() < 4) {
                    options.add("Option " + (char) ('A' + options.size()));
                }
                List<String> fourOptions = options.size() > 4 ? options.subList(0, 4) : List.copyOf(options);
                int correctIndex = node.has("correctIndex")
                        ? node.get("correctIndex").asInt()
                        : letterToIndex(node.path("correct").asText(""));
                if (correctIndex < 0 || correctIndex > 3) {
                    correctIndex = 0;
                }
                if (!text.isBlank()) {
                    result.add(new ParsedQuestion(text, fourOptions, correctIndex));
                }
            }
            if (result.isEmpty()) {
                throw new BadRequestException("Could not parse quiz questions from AI response.");
            }
            return result;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid quiz JSON from AI: " + ex.getMessage());
        }
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
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}