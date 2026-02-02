-- V1: 创建 commands 表
CREATE TABLE IF NOT EXISTS commands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    path VARCHAR(200) UNIQUE,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    command_config JSON,
    execution_mode VARCHAR(20) DEFAULT 'LOCAL',
    remote_config JSON,
    group_name VARCHAR(50),
    tags VARCHAR(200),
    timeout_seconds INT DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_command_name (name),
    INDEX idx_command_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='命令配置表';

-- 插入示例命令
INSERT INTO commands (name, description, command_config, execution_mode, timeout_seconds) VALUES
    ('echo', 'Echo 回显命令', '{"command": "echo {{.message}}", "params": [{"name": "message", "type": "string", "required": true, "description": "要回显的消息"}]}', 'LOCAL', 30),
    ('ping', 'Ping 网络测试', '{"command": "ping -c {{.count}} {{.host}}", "params": [{"name": "host", "type": "string", "required": true}, {"name": "count", "type": "integer", "defaultValue": "4"}]}', 'LOCAL', 60);
