package com.thiz.commitstory.controller;

import com.thiz.commitstory.dto.CommitResponse;
import com.thiz.commitstory.dto.CreateRepoRequest;
import com.thiz.commitstory.dto.RepoResponse;
import com.thiz.commitstory.dto.SyncResponse;
import com.thiz.commitstory.repository.CommitEntryRepository;
import com.thiz.commitstory.service.RepoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;
    private final CommitEntryRepository commitEntryRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<RepoResponse> createRepo(@Valid @RequestBody CreateRepoRequest request) {
        var response = repoService.createRepo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RepoResponse>> listRepos() {
        return ResponseEntity.ok(repoService.listRepos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepoResponse> getRepo(@PathVariable UUID id) {
        return ResponseEntity.ok(repoService.getRepo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepo(@PathVariable UUID id) {
        repoService.deleteRepo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncResponse> syncRepo(@PathVariable UUID id) {
        var response = repoService.syncRepo(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/commits")
    public ResponseEntity<List<CommitResponse>> listCommits(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var commits = commitEntryRepository
                .findByRepoIdOrderByAuthoredAtDesc(id, PageRequest.of(page, size));
        var response = commits.stream()
                .map(c -> {
                    List<String> files = parseFiles(c.getFilesChanged());
                    return new CommitResponse(
                            c.getId(), c.getSha(), c.getAuthorName(), c.getAuthorEmail(),
                            c.getAuthoredAt(), c.getMessage(), files,
                            c.getAdditions(), c.getDeletions());
                })
                .toList();
        return ResponseEntity.ok(response);
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
