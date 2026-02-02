-- V2: 创建 tokens 表（包含时间限制和备注字段）
CREATE TABLE IF NOT EXISTS tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(1000) NOT NULL COMMENT '授权命令列表',
    name VARCHAR(100) NOT NULL COMMENT 'Token名称',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否管理员',
    issued_at BIGINT NOT NULL COMMENT '签发时间戳',
    expires_at BIGINT NULL COMMENT '过期时间戳（NULL表示永久有效）',
    jwt_token TEXT NOT NULL COMMENT 'JWT Token字符串',
    revoked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已撤销',
    allowed_start_time VARCHAR(5) DEFAULT NULL COMMENT '允许执行开始时间（HH:mm）',
    allowed_end_time VARCHAR(5) DEFAULT NULL COMMENT '允许执行结束时间（HH:mm）',
    allowed_weekdays VARCHAR(50) DEFAULT NULL COMMENT '允许执行的星期几',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_token_name (name),
    UNIQUE INDEX idx_token_jwt (jwt_token(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token认证表';
