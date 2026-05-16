-- V3__create_community_activities.sql (H2)

CREATE TABLE community_activities (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    source_id VARCHAR(255),
    title CLOB NOT NULL,
    content CLOB,
    reaction_count INT DEFAULT 0,
    response_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    author VARCHAR(255),
    published_at TIMESTAMP,
    source_url CLOB,
    discord_message_id VARCHAR(255) UNIQUE,
    discord_channel_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES weekly_executions(id)
);

CREATE INDEX idx_community_activities_execution_id ON community_activities(execution_id);
CREATE INDEX idx_community_activities_type ON community_activities(type);
