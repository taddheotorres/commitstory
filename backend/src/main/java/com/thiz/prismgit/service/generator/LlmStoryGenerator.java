package com.thiz.prismgit.service.generator;

import com.thiz.prismgit.entity.CommitEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStoryGenerator implements StoryGenerator {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;

    @Value("${llm.provider}")
    private String provider;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Override
    public String generate(List<CommitEntry> commits, Map<String, String> options) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LLM API key not configured, falling back to template mode explanation");
            return fallbackMessage(commits, options);
        }

        return switch (provider.toLowerCase()) {
            case "openai" -> callOpenAI(commits, options);
            case "anthropic" -> fallbackMessage(commits, options);
            default -> fallbackMessage(commits, options);
        };
    }

    private String callOpenAI(List<CommitEntry> commits, Map<String, String> options) {
        var systemPrompt = """
                You are a technical storyteller. Your task is to transform a list of git commits into a compelling \
                narrative about the development journey. The story should be engaging, well-structured, and highlight \
                the progression of work. Use markdown formatting.
                """;

        var userPrompt = buildPrompt(commits, options);

        var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        var requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        var headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.info("Calling OpenAI model={} with {} commits", model, commits.size());
            var response = restTemplate.exchange(
                    OPENAI_URL, HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class);

            var body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from OpenAI");
            }

            @SuppressWarnings("unchecked")
            var choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }

            @SuppressWarnings("unchecked")
            var message = (Map<String, Object>) choices.get(0).get("message");
            return message != null ? (String) message.get("content") : "";

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            return fallbackMessage(commits, options);
        }
    }

    private String fallbackMessage(List<CommitEntry> commits, Map<String, String> options) {
        var title = options.getOrDefault("title", "Development Story");
        var sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        sb.append("> LLM story generation was not available. ")
                .append("Set the **LLM_API_KEY** environment variable to enable AI-powered stories.\n\n");
        sb.append("**Commits:** ").append(commits.size()).append("\n");

        if (!commits.isEmpty()) {
            sb.append("**From:** ").append(commits.get(0).getAuthoredAt().toLocalDate()).append("\n");
            sb.append("**To:** ").append(commits.get(commits.size() - 1).getAuthoredAt().toLocalDate()).append("\n\n");
        }

        sb.append("### Commits overview\n\n");
        for (var c : commits) {
            var msg = c.getMessage() != null
                    ? c.getMessage().lines().findFirst().orElse("")
                    : "no message";
            sb.append("- `").append(c.getSha(), 0, 7).append("` ")
                    .append(c.getAuthorName()).append(" — ")
                    .append(msg).append("\n");
        }

        return sb.toString();
    }

    private String buildPrompt(List<CommitEntry> commits, Map<String, String> options) {
        var title = options.getOrDefault("title", "Development Story");
        var sb = new StringBuilder();
        sb.append("Tell me the story behind this development work titled \"").append(title).append("\".\n\n");

        sb.append("## Commit History\n\n");

        for (var c : commits) {
            sb.append("### ").append(c.getSha(), 0, 7).append("\n");
            sb.append("- **Author:** ").append(c.getAuthorName()).append("\n");
            sb.append("- **Date:** ").append(c.getAuthoredAt()).append("\n");
            sb.append("- **Message:** ").append(c.getMessage()).append("\n");
            sb.append("- **Files changed:** ").append(c.getFilesChanged()).append("\n\n");
        }

        sb.append("""
                Write a cohesive narrative covering:
                1. What was the overall goal?
                2. How did the work progress?
                3. Key technical decisions or milestones
                4. The final outcome

                Use markdown with headings, bullet points, and code blocks where appropriate.
                """);

        return sb.toString();
    }
}
