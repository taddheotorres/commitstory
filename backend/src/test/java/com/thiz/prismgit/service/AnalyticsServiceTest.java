package com.thiz.prismgit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiz.prismgit.entity.CommitEntry;
import com.thiz.prismgit.entity.GitRepo;
import com.thiz.prismgit.repository.CommitEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CommitEntryRepository commitEntryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID repoId;
    private GitRepo repo;
    private CommitEntry c1, c2, c3;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        c1 = new CommitEntry(UUID.randomUUID(), repo, "a1", "Alice", "a@b.com",
                LocalDateTime.of(2024, 1, 15, 10, 0), "fix: stuff", "[\"src/a.ts\",\"src/b.ts\"]", 5, 1);
        c2 = new CommitEntry(UUID.randomUUID(), repo, "a2", "Alice", "a@b.com",
                LocalDateTime.of(2024, 1, 16, 14, 30), "feat: add x", "[\"src/a.ts\"]", 10, 2);
        c3 = new CommitEntry(UUID.randomUUID(), repo, "a3", "Bob", "b@c.com",
                LocalDateTime.of(2024, 1, 16, 16, 0), "docs: readme", "[\"README.md\"]", 1, 0);
    }

    @Test
    void summary_empty() {
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId)).thenReturn(List.of());
        var s = analyticsService.summary(repoId);
        assertEquals(0, s.totalCommits());
        assertEquals(0, s.totalAuthors());
        assertNull(s.firstCommit());
    }

    @Test
    void summary_success() throws Exception {
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId)).thenReturn(List.of(c1, c2, c3));
        when(objectMapper.readValue(any(String.class), any(TypeReference.class)))
                .thenAnswer(inv -> {
                    String json = inv.getArgument(0);
                    if (json.contains("src/a.ts") && json.contains("src/b.ts"))
                        return List.of("src/a.ts", "src/b.ts");
                    if (json.contains("src/a.ts"))
                        return List.of("src/a.ts");
                    if (json.contains("README.md"))
                        return List.of("README.md");
                    return List.of();
                });
        var s = analyticsService.summary(repoId);
        assertEquals(3, s.totalCommits());
        assertEquals(2, s.totalAuthors());
        assertEquals(3, s.totalFilesChanged());
        assertEquals("2024-01-15", s.firstCommit().toString());
        assertEquals("2024-01-16", s.lastCommit().toString());
        assertEquals(2, s.topAuthors().size());
        assertEquals(2, s.topAuthors().get(0).commitCount());
        assertEquals(3, s.topFiles().size());
        assertEquals("src/a.ts", s.topFiles().get(0).path());
    }

    @Test
    void timeline_success() {
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId)).thenReturn(List.of(c1, c2, c3));
        var t = analyticsService.timeline(repoId);
        assertEquals(2, t.size());
        assertEquals(1, t.get(0).commitCount());
        assertEquals(2, t.get(1).commitCount());
    }

    @Test
    void activityByHour_success() {
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId)).thenReturn(List.of(c1, c2, c3));
        var a = analyticsService.activityByHour(repoId);
        assertEquals(24, a.size());
        assertEquals(1, a.get(10).commitCount());  // 10h (c1)
        assertEquals(1, a.get(14).commitCount());  // 14h (c2)
        assertEquals(1, a.get(16).commitCount());  // 16h (c3)
    }

    @Test
    void activityByDay_success() {
        // Jan 15 2024 = Monday, Jan 16 = Tuesday
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId)).thenReturn(List.of(c1, c2, c3));
        var a = analyticsService.activityByDay(repoId);
        assertEquals(7, a.size());
        // DayOfWeek.getValue(): Mon=1, Tue=2...
        assertEquals(1, a.get(0).commitCount());  // Monday
        assertEquals(2, a.get(1).commitCount());  // Tuesday
    }
}
