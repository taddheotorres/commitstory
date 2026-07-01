package com.thiz.prismgit.service;

import com.thiz.prismgit.dto.CreateRepoRequest;
import com.thiz.prismgit.dto.RepoResponse;
import com.thiz.prismgit.dto.SyncResponse;
import com.thiz.prismgit.entity.GitRepo;
import com.thiz.prismgit.entity.RepoProvider;
import com.thiz.prismgit.exception.ResourceNotFoundException;
import com.thiz.prismgit.repository.GitRepoRepository;
import com.thiz.prismgit.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepoService {

    private final GitRepoRepository gitRepoRepository;
    private final RepoSyncService repoSyncService;

    @Transactional
    public RepoResponse createRepo(CreateRepoRequest request) {
        log.info("Creating repository: {}", request.name());
        var ownerId = SecurityUtil.requireCurrentUserId();
        var repo = new GitRepo();
        repo.setName(request.name());
        repo.setLocalPath(request.localPath());
        repo.setRemoteUrl(request.remoteUrl());
        repo.setProvider(request.provider() != null
                ? RepoProvider.valueOf(request.provider().toUpperCase())
                : RepoProvider.NONE);
        repo.setOwnerId(ownerId);
        repo = gitRepoRepository.save(repo);
        log.info("Repository created successfully: {} (ID: {})", request.name(), repo.getId());
        return toResponse(repo);
    }

    public RepoResponse getRepo(UUID id) {
        log.debug("Fetching repository: {}", id);
        var repo = findOwnedRepo(id);
        return toResponse(repo);
    }

    public List<RepoResponse> listRepos() {
        log.debug("Listing all repositories");
        var ownerId = SecurityUtil.requireCurrentUserId();
        return gitRepoRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteRepo(UUID id) {
        log.info("Deleting repository: {}", id);
        var repo = findOwnedRepo(id);
        gitRepoRepository.delete(repo);
        log.info("Repository deleted successfully: {}", id);
    }

    @Transactional
    public SyncResponse syncRepo(UUID id) {
        log.info("Syncing repository: {}", id);
        var repo = findOwnedRepo(id);
        var response = repoSyncService.sync(repo);
        log.info("Repository synced successfully: {} (commits imported: {})", id, response.commitsImported());
        return response;
    }

    public GitRepo findRepo(UUID id) {
        log.debug("Finding repository: {}", id);
        return gitRepoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Repository not found: {}", id);
                    return new ResourceNotFoundException("GitRepo", id);
                });
    }

    public GitRepo findOwnedRepo(UUID id) {
        log.debug("Finding owned repository: {}", id);
        var ownerId = SecurityUtil.requireCurrentUserId();
        return gitRepoRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    log.warn("Repository not found or access denied: {}", id);
                    return new ResourceNotFoundException("GitRepo", id);
                });
    }

    private RepoResponse toResponse(GitRepo repo) {
        return new RepoResponse(
                repo.getId(),
                repo.getName(),
                repo.getLocalPath(),
                repo.getRemoteUrl(),
                repo.getProvider(),
                repo.getCreatedAt()
        );
    }
}
