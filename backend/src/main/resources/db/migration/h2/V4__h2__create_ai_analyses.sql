-- V4__h2__create_ai_analyses.sql (H2)

CREATE TABLE ai_analyses (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL UNIQUE,
    top_topics CLOB,
    executive_summary CLOB,
    relevance_scores CLOB,
    llm_provider VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES weekly_executions(id)
);

CREATE INDEX idx_ai_analyses_execution_id ON ai_analyses(execution_id);
