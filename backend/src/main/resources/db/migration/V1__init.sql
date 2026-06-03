CREATE TABLE git_repo (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    local_path VARCHAR(1024),
    remote_url VARCHAR(1024),
    provider VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE commit_entry (
    id UUID NOT NULL,
    repo_id UUID NOT NULL,
    sha VARCHAR(40) NOT NULL,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    authored_at TIMESTAMP NOT NULL,
    message TEXT,
    files_changed TEXT,
    additions INTEGER DEFAULT 0,
    deletions INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (repo_id, sha)
);

ALTER TABLE commit_entry
    ADD CONSTRAINT fk_commit_entry_repo
    FOREIGN KEY (repo_id) REFERENCES git_repo(id)
    ON DELETE CASCADE;

CREATE TABLE story (
    id UUID NOT NULL,
    repo_id UUID NOT NULL,
    title VARCHAR(500),
    content TEXT,
    mode VARCHAR(20) NOT NULL,
    start_sha VARCHAR(40),
    end_sha VARCHAR(40),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

ALTER TABLE story
    ADD CONSTRAINT fk_story_repo
    FOREIGN KEY (repo_id) REFERENCES git_repo(id)
    ON DELETE CASCADE;
