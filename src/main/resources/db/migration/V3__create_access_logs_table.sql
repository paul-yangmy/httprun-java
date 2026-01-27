-- V3: 创建 access_logs 表
CREATE TABLE IF NOT EXISTS access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id VARCHAR(500),
    path VARCHAR(200) NOT NULL,
    ip VARCHAR(50),
    method VARCHAR(20),
    request TEXT,
    response MEDIUMTEXT,
    status_code INT,
    duration BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_accesslog_token (token_id(100)),
    INDEX idx_accesslog_path (path),
    INDEX idx_accesslog_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
