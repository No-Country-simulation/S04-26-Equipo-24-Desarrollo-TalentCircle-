-- V2__h2__create_weekly_executions.sql (H2)

CREATE TABLE weekly_executions (
    id VARCHAR(36) PRIMARY KEY,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    triggered_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weekly_executions_week_start ON weekly_executions(week_start);
CREATE INDEX idx_weekly_executions_status ON weekly_executions(status);
