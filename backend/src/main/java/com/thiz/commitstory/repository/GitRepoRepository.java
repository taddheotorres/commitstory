package com.thiz.commitstory.repository;

import com.thiz.commitstory.entity.GitRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GitRepoRepository extends JpaRepository<GitRepo, UUID> {
}
