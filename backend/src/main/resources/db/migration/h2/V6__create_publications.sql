-- V6__create_publications.sql (H2)

CREATE TABLE publications (
    id VARCHAR(36) PRIMARY KEY,
    draft_id VARCHAR(36) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_post_id VARCHAR(255),
    published_at TIMESTAMP,
    error_message CLOB,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (draft_id) REFERENCES drafts(id)
);

CREATE INDEX idx_publications_draft_id ON publications(draft_id);
CREATE INDEX idx_publications_status ON publications(status);
