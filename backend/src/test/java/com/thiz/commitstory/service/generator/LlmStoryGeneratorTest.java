package com.thiz.commitstory.service.generator;

import com.thiz.commitstory.entity.CommitEntry;
import com.thiz.commitstory.entity.GitRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmStoryGeneratorTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LlmStoryGenerator llmGenerator;

    private UUID repoId;
    private GitRepo repo;
    private CommitEntry commit1, commit2;
    private Map<String, String> options;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");

        commit1 = new CommitEntry(UUID.randomUUID(), repo, "abc123", "Alice", "alice@example.com",
                LocalDateTime.of(2024, 1, 1, 10, 0), "feat: add feature", "[\"src/main.ts\"]", 5, 1);
        commit2 = new CommitEntry(UUID.randomUUID(), repo, "def456", "Bob", "bob@example.com",
                LocalDateTime.of(2024, 1, 2, 14, 0), "fix: bug fix", "[\"src/bug.ts\"]", 2, 2);

        options = new LinkedHashMap<>();
        options.put("title", "Test Story");
    }

    @Test
    void should_return_fallback_when_api_key_not_configured() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", null);
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("Test Story"));
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_return_fallback_when_api_key_blank() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "");
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM_API_KEY"));
    }

    @Test
    void should_call_openai_when_provider_configured() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");
        ReflectionTestUtils.setField(llmGenerator, "model", "gpt-4o-mini");

        Map<String, Object> mockResponse = new LinkedHashMap<>();
        mockResponse.put("choices", List.of(
                Map.of("message", Map.of("content", "Generated story from LLM"))
        ));

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("Generated story from LLM"));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void should_return_fallback_on_openai_error() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("OpenAI API error"));

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_return_fallback_for_anthropic_provider() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "anthropic");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_return_fallback_for_unknown_provider() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "unknown-provider");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_fallback_on_empty_response() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        Map<String, Object> mockResponse = new LinkedHashMap<>();
        mockResponse.put("choices", List.of());

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_fallback_on_null_body() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("LLM story generation was not available"));
    }

    @Test
    void should_include_commits_in_fallback() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", null);
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("Commits"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("abc123"));
        assertTrue(result.contains("def456"));
    }

    @Test
    void should_use_title_from_options() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", null);
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");
        options.put("title", "My Custom Title");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("My Custom Title"));
    }

    @Test
    void should_use_default_title_when_not_provided() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", null);
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");
        options.clear();

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertTrue(result.contains("Development Story"));
    }

    @Test
    void should_not_be_empty() {
        ReflectionTestUtils.setField(llmGenerator, "apiKey", null);
        ReflectionTestUtils.setField(llmGenerator, "provider", "openai");

        String result = llmGenerator.generate(List.of(commit1, commit2), options);

        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}
