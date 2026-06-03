package com.thiz.commitstory.service.generator;

import com.thiz.commitstory.entity.CommitEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TemplateStoryGenerator implements StoryGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    @Override
    public String generate(List<CommitEntry> commits, Map<String, String> options) {
        if (commits.isEmpty()) {
            return "No commits to tell a story about.";
        }

        var byDate = groupByDate(commits);
        var sb = new StringBuilder();

        sb.append("# ").append(options.getOrDefault("title", "Development Story")).append("\n\n");
        sb.append("A narrative generated from **").append(commits.size())
                .append(" commits** spanning ")
                .append(commits.get(0).getAuthoredAt().toLocalDate())
                .append(" to ")
                .append(commits.get(commits.size() - 1).getAuthoredAt().toLocalDate())
                .append(".\n\n");

        for (var entry : byDate.entrySet()) {
            var date = entry.getKey();
            var dayCommits = entry.getValue();

            sb.append("## ").append(date.format(DATE_FMT)).append("\n\n");

            var byAuthor = dayCommits.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getAuthorName() != null ? c.getAuthorName() : "Unknown",
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (var authorEntry : byAuthor.entrySet()) {
                var author = authorEntry.getKey();
                var authorCommits = authorEntry.getValue();

                sb.append("**").append(author).append("** made ")
                        .append(authorCommits.size())
                        .append(authorCommits.size() == 1 ? " commit:" : " commits:")
                        .append("\n\n");

                for (var commit : authorCommits) {
                    sb.append("- `").append(commit.getSha(), 0, 7).append("` ");
                    var msg = commit.getMessage() != null
                            ? commit.getMessage().lines().findFirst().orElse("")
                            : "no message";
                    sb.append(msg).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private LinkedHashMap<LocalDate, List<CommitEntry>> groupByDate(List<CommitEntry> commits) {
        return commits.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getAuthoredAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
