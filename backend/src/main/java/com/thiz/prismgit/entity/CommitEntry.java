package com.thiz.prismgit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "commit_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"repo_id", "sha"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private GitRepo repo;

    @Column(nullable = false, length = 40)
    private String sha;

    private String authorName;

    private String authorEmail;

    @Column(nullable = false)
    private LocalDateTime authoredAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String filesChanged;

    private int additions;

    private int deletions;
}
