CREATE TABLE message_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    discord_id VARCHAR(50) NOT NULL,
    author VARCHAR(100) NOT NULL,
    content TEXT,
    published_at TIMESTAMP
);