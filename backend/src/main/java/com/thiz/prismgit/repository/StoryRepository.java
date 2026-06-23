package com.thiz.prismgit.repository;

import com.thiz.prismgit.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    List<Story> findByRepoIdOrderByCreatedAtDesc(UUID repoId);
}
