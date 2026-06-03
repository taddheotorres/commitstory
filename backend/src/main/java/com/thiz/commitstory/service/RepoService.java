package com.thiz.commitstory.service;

import com.thiz.commitstory.dto.CreateRepoRequest;
import com.thiz.commitstory.dto.RepoResponse;
import com.thiz.commitstory.dto.SyncResponse;
import com.thiz.commitstory.entity.GitRepo;
import com.thiz.commitstory.entity.RepoProvider;
import com.thiz.commitstory.exception.ResourceNotFoundException;
import com.thiz.commitstory.repository.GitRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final GitRepoRepository gitRepoRepository;
    private final RepoSyncService repoSyncService;

    @Transactional
    public RepoResponse createRepo(CreateRepoRequest request) {
        var repo = new GitRepo();
        repo.setName(request.name());
        repo.setLocalPath(request.localPath());
        repo.setRemoteUrl(request.remoteUrl());
        repo.setProvider(request.provider() != null
                ? RepoProvider.valueOf(request.provider().toUpperCase())
                : RepoProvider.NONE);
        repo = gitRepoRepository.save(repo);
        return toResponse(repo);
    }

    public RepoResponse getRepo(UUID id) {
        var repo = findRepo(id);
        return toResponse(repo);
    }

    public List<RepoResponse> listRepos() {
        return gitRepoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteRepo(UUID id) {
        var repo = findRepo(id);
        gitRepoRepository.delete(repo);
    }

    @Transactional
    public SyncResponse syncRepo(UUID id) {
        var repo = findRepo(id);
        return repoSyncService.sync(repo);
    }

    public GitRepo findRepo(UUID id) {
        return gitRepoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitRepo", id));
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
