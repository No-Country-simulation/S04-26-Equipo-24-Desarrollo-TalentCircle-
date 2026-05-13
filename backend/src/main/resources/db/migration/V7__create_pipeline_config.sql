-- V7__create_pipeline_config.sql

CREATE TABLE pipeline_configs
(
    id                    VARCHAR(36)        DEFAULT '00000000-0000-0000-0000-000000000001' PRIMARY KEY,
    llm_provider          VARCHAR(50),
    llm_model             VARCHAR(100),
    newsletter_prompt     TEXT,
    linkedin_prompt       TEXT,
    twitter_prompt        TEXT,
    max_items_per_channel INT                DEFAULT 10,
    schedule_cron         VARCHAR(100)       DEFAULT '0 18 * * FRI',
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default config
INSERT INTO pipeline_configs (id, schedule_cron)
VALUES ('00000000-0000-0000-0000-000000000001', '0 18 * * FRI');