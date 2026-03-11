-- V8: 新增 Token 分组授权字段 + 命令版本历史表

-- 1. tokens 表新增 allowed_groups 字段（命令分组授权，逗号分隔）
ALTER TABLE tokens
    ADD COLUMN allowed_groups VARCHAR(500) DEFAULT NULL COMMENT 'Token 允许的命令分组范围（逗号分隔）';

-- 2. 创建命令版本历史表
CREATE TABLE command_versions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    command_name  VARCHAR(100)  NOT NULL COMMENT '关联的命令名称',
    version       INT           NOT NULL COMMENT '版本号（单命令递增）',
    snapshot      JSON          NOT NULL COMMENT '命令配置快照（CreateCommandRequest JSON）',
    change_note   VARCHAR(500)           COMMENT '变更说明',
    changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    INDEX idx_cv_name_version (command_name, version DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '命令版本历史';
