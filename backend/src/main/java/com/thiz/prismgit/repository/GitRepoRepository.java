package com.thiz.prismgit.repository;

import com.thiz.prismgit.entity.GitRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GitRepoRepository extends JpaRepository<GitRepo, UUID> {
    List<GitRepo> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<GitRepo> findByIdAndOwnerId(UUID id, UUID ownerId);
}
