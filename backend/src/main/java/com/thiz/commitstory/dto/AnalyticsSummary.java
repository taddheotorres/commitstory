package com.thiz.commitstory.dto;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummary(
        long totalCommits,
        long totalAuthors,
        LocalDate firstCommit,
        LocalDate lastCommit,
        long totalFilesChanged,
        List<AuthorStat> topAuthors,
        List<FileStat> topFiles
) {
    public record AuthorStat(String name, String email, long commitCount) {}
    public record FileStat(String path, long changeCount) {}
}
