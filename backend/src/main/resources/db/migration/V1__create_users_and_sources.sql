-- V1__create_users_and_sources.sql

-- Create users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
INSERT INTO users (id, email, password_hash, full_name, role, active)
VALUES (
           '550e8400-e29b-41d4-a716-446655440000',
           'admin@talentcircle.com',
           '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.7uSyLnS',
           'Admin Ramon Valdez',
           'ADMIN',
           true
       );

-- Create community_sources table
CREATE TABLE community_sources (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    api_url TEXT,
    api_key_encrypted TEXT,
    active BOOLEAN DEFAULT true,
    config TEXT, -- JSONB in PostgreSQL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_sources_active ON community_sources(active);
