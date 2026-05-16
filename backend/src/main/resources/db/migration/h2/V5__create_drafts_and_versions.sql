-- V5__create_drafts_and_versions.sql (H2)

CREATE TABLE drafts (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    content CLOB,
    edited_content CLOB,
    status VARCHAR(20) NOT NULL,
    ai_score DECIMAL(4,2),
    approved_by VARCHAR(36),
    approved_at TIMESTAMP,
    rejection_reason CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES weekly_executions(id)
);

CREATE INDEX idx_drafts_execution_id ON drafts(execution_id);
CREATE INDEX idx_drafts_channel ON drafts(channel);
CREATE INDEX idx_drafts_status ON drafts(status);

CREATE TABLE draft_versions (
    id VARCHAR(36) PRIMARY KEY,
    draft_id VARCHAR(36) NOT NULL,
    content CLOB,
    edited_by VARCHAR(36),
    edited_at TIMESTAMP,
    version_number INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (draft_id) REFERENCES drafts(id)
);

CREATE INDEX idx_draft_versions_draft_id ON draft_versions(draft_id);

CREATE TABLE draft_sources (
    id VARCHAR(36) PRIMARY KEY,
    draft_id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    relevance_score DECIMAL(4,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (draft_id) REFERENCES drafts(id),
    FOREIGN KEY (activity_id) REFERENCES community_activities(id)
);

CREATE INDEX idx_draft_sources_draft_id ON draft_sources(draft_id);
CREATE INDEX idx_draft_sources_activity_id ON draft_sources(activity_id);
