package com.thiz.prismgit.repository;

import com.thiz.prismgit.entity.CommitEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommitEntryRepository extends JpaRepository<CommitEntry, UUID> {

    Page<CommitEntry> findByRepoIdOrderByAuthoredAtDesc(UUID repoId, Pageable pageable);

    List<CommitEntry> findByRepoIdOrderByAuthoredAtAsc(UUID repoId);

    Optional<CommitEntry> findByRepoIdAndSha(UUID repoId, String sha);

    boolean existsByRepoIdAndSha(UUID repoId, String sha);
}
