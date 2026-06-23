package com.thiz.commitstory.service;

import com.thiz.commitstory.dto.CreateRepoRequest;
import com.thiz.commitstory.dto.RepoResponse;
import com.thiz.commitstory.dto.SyncResponse;
import com.thiz.commitstory.entity.GitRepo;
import com.thiz.commitstory.entity.RepoProvider;
import com.thiz.commitstory.exception.ResourceNotFoundException;
import com.thiz.commitstory.repository.GitRepoRepository;
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
        var repo = new GitRepo();
        repo.setName(request.name());
        repo.setLocalPath(request.localPath());
        repo.setRemoteUrl(request.remoteUrl());
        repo.setProvider(request.provider() != null
                ? RepoProvider.valueOf(request.provider().toUpperCase())
                : RepoProvider.NONE);
        repo = gitRepoRepository.save(repo);
        log.info("Repository created successfully: {} (ID: {})", request.name(), repo.getId());
        return toResponse(repo);
    }

    public RepoResponse getRepo(UUID id) {
        log.debug("Fetching repository: {}", id);
        var repo = findRepo(id);
        return toResponse(repo);
    }

    public List<RepoResponse> listRepos() {
        log.debug("Listing all repositories");
        return gitRepoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteRepo(UUID id) {
        log.info("Deleting repository: {}", id);
        var repo = findRepo(id);
        gitRepoRepository.delete(repo);
        log.info("Repository deleted successfully: {}", id);
    }

    @Transactional
    public SyncResponse syncRepo(UUID id) {
        log.info("Syncing repository: {}", id);
        var repo = findRepo(id);
        var response = repoSyncService.sync(repo);
        log.info("Repository synced successfully: {} (commits added: {})", id, response.commitsAdded());
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
