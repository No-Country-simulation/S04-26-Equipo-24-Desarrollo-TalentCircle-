-- V7__create_pipeline_config.sql (H2)
-- Note: H2 does not support DEFAULT on PRIMARY KEY columns, so we insert separately.

CREATE TABLE pipeline_configs (
    id VARCHAR(36) PRIMARY KEY,
    llm_provider VARCHAR(50),
    llm_model VARCHAR(100),
    newsletter_prompt CLOB,
    linkedin_prompt CLOB,
    twitter_prompt CLOB,
    max_items_per_channel INT DEFAULT 10,
    schedule_cron VARCHAR(100) DEFAULT '0 0 18 * * FRI',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO pipeline_configs (id, schedule_cron, max_items_per_channel)
VALUES ('00000000-0000-0000-0000-000000000001', '0 0 18 * * FRI', 10);
