package com.thiz.prismgit.service;

import com.thiz.prismgit.dto.SyncResponse;
import com.thiz.prismgit.entity.CommitEntry;
import com.thiz.prismgit.entity.GitRepo;
import com.thiz.prismgit.repository.CommitEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepoSyncService {

    private final CommitEntryRepository commitEntryRepository;
    private final RemoteSyncService remoteSyncService;

    @Transactional
    public SyncResponse sync(GitRepo repo) {
        if (repo.getLocalPath() != null && !repo.getLocalPath().isBlank()) {
            return syncFromLocal(repo);
        }
        if (repo.getRemoteUrl() != null && !repo.getRemoteUrl().isBlank()) {
            return syncFromRemote(repo);
        }
        throw new IllegalArgumentException("Repo has no local path or remote URL configured");
    }

    private SyncResponse syncFromLocal(GitRepo repo) {
        var repoPath = Path.of(repo.getLocalPath());
        if (!Files.exists(repoPath) || !Files.isDirectory(repoPath)) {
            throw new IllegalArgumentException("Local path does not exist: " + repo.getLocalPath());
        }

        try (var git = Git.open(repoPath.toFile())) {
            var commits = git.log().all().call();
            int imported = 0;

            for (RevCommit rev : commits) {
                if (commitEntryRepository.existsByRepoIdAndSha(repo.getId(), rev.getName())) {
                    continue;
                }

                var entry = new CommitEntry();
                entry.setRepo(repo);
                entry.setSha(rev.getName());
                entry.setMessage(rev.getFullMessage().strip());

                var author = rev.getAuthorIdent();
                if (author != null) {
                    entry.setAuthorName(author.getName());
                    entry.setAuthorEmail(author.getEmailAddress());
                }

                entry.setAuthoredAt(toLocalDateTime(rev.getAuthorIdent()));
                entry.setAdditions(0);
                entry.setDeletions(0);
                entry.setFilesChanged(toJsonFileList(repoPath, rev));
                commitEntryRepository.save(entry);
                imported++;
            }

            return new SyncResponse(imported, "Imported %d commits from local repo".formatted(imported));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open git repo at: " + repo.getLocalPath(), e);
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to read git log", e);
        }
    }

    private SyncResponse syncFromRemote(GitRepo repo) {
        var imported = switch (repo.getProvider()) {
            case GITHUB -> remoteSyncService.syncFromGitHub(repo);
            case GITLAB -> syncFromGitLab(repo);
            case NONE -> throw new IllegalArgumentException(
                    "Repo provider not specified for remote URL: " + repo.getRemoteUrl());
        };

        return new SyncResponse(imported,
                "Imported %d commits from %s".formatted(imported, repo.getRemoteUrl()));
    }

    private int syncFromGitLab(GitRepo repo) {
        throw new IllegalArgumentException("GitLab sync not yet implemented: " + repo.getRemoteUrl());
    }

    private String toJsonFileList(Path repoPath, RevCommit commit) {
        try (var git = Git.open(repoPath.toFile())) {
            var diffs = new ArrayList<String>();
            try (var reader = git.getRepository().newObjectReader()) {
                var parentId = commit.getParentCount() > 0 ? commit.getParent(0).getId() : null;
                var newTree = commit.getTree();
                var oldTree = parentId != null
                        ? git.getRepository().parseCommit(parentId).getTree()
                        : null;

                var diffFormatter = new org.eclipse.jgit.diff.DiffFormatter(null);
                diffFormatter.setRepository(git.getRepository());
                var entries = diffFormatter.scan(oldTree, newTree);
                for (var entry : entries) {
                    diffs.add(entry.getNewPath() != null ? entry.getNewPath() : entry.getOldPath());
                }
            }
            return diffs.isEmpty() ? "[]" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(diffs);
        } catch (Exception e) {
            log.warn("Failed to get changed files for commit {}", commit.getName(), e);
            return "[]";
        }
    }

    private LocalDateTime toLocalDateTime(PersonIdent ident) {
        if (ident == null) return LocalDateTime.now();
        return LocalDateTime.ofInstant(ident.getWhen().toInstant(), ZoneId.systemDefault());
    }
}
