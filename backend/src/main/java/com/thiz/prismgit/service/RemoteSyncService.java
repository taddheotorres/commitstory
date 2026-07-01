package com.thiz.prismgit.service;

import com.thiz.prismgit.entity.CommitEntry;
import com.thiz.prismgit.entity.RepoProvider;
import com.thiz.prismgit.repository.CommitEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteSyncService {

    private static final String GITHUB_API = "https://api.github.com";
    private static final Pattern GITHUB_URL = Pattern.compile(
            "(?:https?://github\\.com/|git@github\\.com:)([^/]+)/([^/.]+)(?:\\.git)?");

    private final RestTemplate restTemplate;
    private final CommitEntryRepository commitEntryRepository;

    @Value("${github.token}")
    private String githubToken;

    public int syncFromGitHub(com.thiz.prismgit.entity.GitRepo repo) {
        var matcher = GITHUB_URL.matcher(repo.getRemoteUrl());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid GitHub URL: " + repo.getRemoteUrl());
        }

        var owner = matcher.group(1);
        var repoName = matcher.group(2);

        var headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        headers.set("Accept", "application/vnd.github.v3+json");

        var imported = 0;
        var page = 1;

        while (true) {
            var url = "%s/repos/%s/%s/commits?per_page=100&page=%d".formatted(GITHUB_API, owner, repoName, page);
            var exchange = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            var commits = exchange.getBody();
            if (commits == null || commits.isEmpty()) break;

            for (var ghCommit : commits) {
                var sha = (String) ghCommit.get("sha");
                if (commitEntryRepository.existsByRepoIdAndSha(repo.getId(), sha)) continue;

                var entry = mapCommit(repo, ghCommit, owner, repoName);
                commitEntryRepository.save(entry);
                imported++;
            }

            var linkHeader = exchange.getHeaders().getFirst("Link");
            if (linkHeader == null || !linkHeader.contains("rel=\"next\"")) break;
            page++;
        }

        log.info("Imported {} commits from GitHub {}/{}", imported, owner, repoName);
        return imported;
    }

    @SuppressWarnings("unchecked")
    private CommitEntry mapCommit(com.thiz.prismgit.entity.GitRepo repo, Map<String, Object> ghCommit,
                                  String owner, String repoName) {
        var entry = new CommitEntry();
        entry.setRepo(repo);
        entry.setSha((String) ghCommit.get("sha"));

        var commitData = (Map<String, Object>) ghCommit.get("commit");
        if (commitData != null) {
            var authorData = (Map<String, Object>) commitData.get("author");
            if (authorData != null) {
                entry.setAuthorName((String) authorData.get("name"));
                entry.setAuthorEmail((String) authorData.get("email"));
                var dateStr = (String) authorData.get("date");
                if (dateStr != null) {
                    entry.setAuthoredAt(OffsetDateTime.parse(dateStr).toLocalDateTime());
                }
            }
            entry.setMessage((String) commitData.get("message"));
        }

        var files = fetchCommitFiles(owner, repoName, entry.getSha());
        entry.setFilesChanged(files.toString());
        entry.setAdditions(0);
        entry.setDeletions(0);

        return entry;
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchCommitFiles(String owner, String repo, String sha) {
        var headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        headers.set("Accept", "application/vnd.github.v3+json");

        var url = "%s/repos/%s/%s/commits/%s".formatted(GITHUB_API, owner, repo, sha);

        try {
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            var body = response.getBody();
            if (body == null) return List.of();

            return ((List<Map<String, Object>>) body.getOrDefault("files", List.of()))
                    .stream()
                    .map(f -> (String) f.get("filename"))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch files for commit {}: {}", sha, e.getMessage());
            return List.of();
        }
    }
}
