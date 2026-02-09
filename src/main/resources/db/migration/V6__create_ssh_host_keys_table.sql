-- V6: 创建 SSH 主机指纹表（SSH Fingerprint Management）
-- 用于存储已知主机的 SSH 公钥指纹，实现 TOFU（Trust On First Use）策略，防止中间人攻击
CREATE TABLE IF NOT EXISTS ssh_host_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口号',
    key_type VARCHAR(50) NOT NULL COMMENT '密钥类型（ssh-rsa, ssh-ed25519 等）',
    fingerprint VARCHAR(2000) NOT NULL COMMENT '公钥指纹（Base64 编码）',
    sha256_hash VARCHAR(100) DEFAULT NULL COMMENT 'SHA-256 哈希摘要',
    first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次记录时间',
    last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后验证时间',
    trusted BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否被信任',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    CONSTRAINT uk_host_port_type UNIQUE (host, port, key_type),
    INDEX idx_hostkey_host_port (host, port)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSH 主机指纹表';
