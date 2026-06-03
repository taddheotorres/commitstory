package com.thiz.commitstory.service.generator;

import com.thiz.commitstory.entity.CommitEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmStoryGenerator implements StoryGenerator {

    @Override
    public String generate(List<CommitEntry> commits, Map<String, String> options) {
        var model = options.getOrDefault("model", "default");

        var prompt = buildPrompt(commits);
        log.info("LLM story generation requested (model={}), prompt length={} chars", model, prompt.length());

        // TODO: Integrate with OpenAI / Anthropic API
        // 1. Build system prompt with narrative instructions
        // 2. Send messages to LLM API
        // 3. Parse and return response
        // 4. Handle rate limiting, errors, and fallback

        return """
               # Story Generation via LLM

               Mode: **LLM** (model: %s)

               This is a placeholder. LLM integration is not yet implemented.

               **Commits provided:** %d
               **Date range:** %s — %s

               To enable LLM generation:
               1. Add your API key (OpenAI/Anthropic) to configuration
               2. Implement the API call in this class
               3. Configure the model and system prompt
               """
                .formatted(
                        model,
                        commits.size(),
                        commits.get(0).getAuthoredAt().toLocalDate(),
                        commits.get(commits.size() - 1).getAuthoredAt().toLocalDate()
                );
    }

    private String buildPrompt(List<CommitEntry> commits) {
        var sb = new StringBuilder();
        sb.append("Here is a git commit history. Please write a narrative story about the development journey:\n\n");

        for (var c : commits) {
            sb.append("Commit: ").append(c.getSha(), 0, 7).append("\n");
            sb.append("Author: ").append(c.getAuthorName()).append("\n");
            sb.append("Date: ").append(c.getAuthoredAt()).append("\n");
            sb.append("Message: ").append(c.getMessage()).append("\n");
            sb.append("Files: ").append(c.getFilesChanged()).append("\n");
            sb.append("---\n");
        }

        return sb.toString();
    }
}
