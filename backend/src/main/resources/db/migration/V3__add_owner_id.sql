ALTER TABLE git_repo
    ADD COLUMN owner_id UUID;

ALTER TABLE git_repo
    ADD CONSTRAINT fk_git_repo_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE SET NULL;

ALTER TABLE story
    ADD COLUMN owner_id UUID;

ALTER TABLE story
    ADD CONSTRAINT fk_story_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE SET NULL;
