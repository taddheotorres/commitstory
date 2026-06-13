package com.thiz.commitstory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiz.commitstory.dto.ActivityDistribution;
import com.thiz.commitstory.dto.AnalyticsSummary;
import com.thiz.commitstory.dto.TimelinePoint;
import com.thiz.commitstory.entity.CommitEntry;
import com.thiz.commitstory.repository.CommitEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CommitEntryRepository commitEntryRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsSummary summary(UUID repoId) {
        var commits = commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId);
        if (commits.isEmpty()) {
            return new AnalyticsSummary(0, 0, null, null, 0, List.of(), List.of());
        }

        var authorCount = commits.stream()
                .map(CommitEntry::getAuthorEmail)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        var files = new HashMap<String, Long>();
        var authorCommits = new HashMap<String, AnalyticsSummary.AuthorStat>();
        for (var c : commits) {
            var filesList = parseFiles(c.getFilesChanged());
            for (var f : filesList) {
                files.merge(f, 1L, Long::sum);
            }
            if (c.getAuthorEmail() != null) {
                authorCommits.merge(c.getAuthorEmail(),
                        new AnalyticsSummary.AuthorStat(c.getAuthorName(), c.getAuthorEmail(), 1),
                        (a, b) -> new AnalyticsSummary.AuthorStat(a.name(), a.email(), a.commitCount() + 1));
            }
        }

        var topAuthors = authorCommits.values().stream()
                .sorted(Comparator.comparingLong(AnalyticsSummary.AuthorStat::commitCount).reversed())
                .limit(10)
                .toList();

        var topFiles = files.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new AnalyticsSummary.FileStat(e.getKey(), e.getValue()))
                .toList();

        return new AnalyticsSummary(
                commits.size(),
                authorCount,
                commits.get(0).getAuthoredAt().toLocalDate(),
                commits.get(commits.size() - 1).getAuthoredAt().toLocalDate(),
                files.size(),
                topAuthors,
                topFiles
        );
    }

    public List<TimelinePoint> timeline(UUID repoId) {
        var commits = commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId);
        return commits.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getAuthoredAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new TimelinePoint(e.getKey(), e.getValue()))
                .toList();
    }

    public List<ActivityDistribution> activityByHour(UUID repoId) {
        var commits = commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId);
        var byHour = new int[24];
        for (var c : commits) {
            byHour[c.getAuthoredAt().getHour()]++;
        }
        var result = new ArrayList<ActivityDistribution>(24);
        for (int i = 0; i < 24; i++) {
            result.add(new ActivityDistribution(i, byHour[i]));
        }
        return result;
    }

    public List<ActivityDistribution> activityByDay(UUID repoId) {
        var commits = commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId);
        var byDay = new long[7];
        for (var c : commits) {
            byDay[c.getAuthoredAt().getDayOfWeek().getValue() - 1]++;
        }
        var result = new ArrayList<ActivityDistribution>(7);
        for (int i = 0; i < 7; i++) {
            result.add(new ActivityDistribution(i + 1, byDay[i]));
        }
        return result;
    }

    private List<String> parseFiles(String filesChanged) {
        if (filesChanged == null || filesChanged.isBlank()) return List.of();
        try {
            return objectMapper.readValue(filesChanged, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
