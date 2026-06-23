package com.thiz.prismgit.repository;

import com.thiz.prismgit.entity.GitRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GitRepoRepository extends JpaRepository<GitRepo, UUID> {
}
