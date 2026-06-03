package com.thiz.commitstory.repository;

import com.thiz.commitstory.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    List<Story> findByRepoIdOrderByCreatedAtDesc(UUID repoId);
}
