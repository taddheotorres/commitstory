package com.thiz.prismgit.controller;

import com.thiz.prismgit.dto.CommitResponse;
import com.thiz.prismgit.dto.CreateRepoRequest;
import com.thiz.prismgit.dto.RepoResponse;
import com.thiz.prismgit.dto.SyncResponse;
import com.thiz.prismgit.repository.CommitEntryRepository;
import com.thiz.prismgit.service.RepoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Repositories", description = "Manage Git repositories")
public class RepoController {

    private final RepoService repoService;
    private final CommitEntryRepository commitEntryRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Create a new repository", description = "Create a new Git repository entry")
    @ApiResponse(responseCode = "201", description = "Repository created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<RepoResponse> createRepo(@Valid @RequestBody CreateRepoRequest request) {
        var response = repoService.createRepo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all repositories", description = "Get a list of all registered repositories")
    @ApiResponse(responseCode = "200", description = "List of repositories")
    public ResponseEntity<List<RepoResponse>> listRepos() {
        return ResponseEntity.ok(repoService.listRepos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get repository by ID", description = "Retrieve details of a specific repository")
    @ApiResponse(responseCode = "200", description = "Repository found")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<RepoResponse> getRepo(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(repoService.getRepo(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete repository", description = "Remove a repository and all associated data")
    @ApiResponse(responseCode = "204", description = "Repository deleted successfully")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<Void> deleteRepo(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID id) {
        repoService.deleteRepo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sync repository", description = "Synchronize repository commits with local or remote source")
    @ApiResponse(responseCode = "200", description = "Sync completed")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<SyncResponse> syncRepo(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID id) {
        var response = repoService.syncRepo(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/commits")
    @Operation(summary = "List commits", description = "Get paginated list of commits for a repository")
    @ApiResponse(responseCode = "200", description = "List of commits")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<List<CommitResponse>> listCommits(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
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
