-- V4: 已废弃 - 审计字段已整合到 V3__create_access_logs_table.sql
-- 此文件保留仅用于 Flyway 版本控制，实际字段已在 V3 中创建
-- 如果是全新数据库，此文件不执行任何操作

-- 兼容性检查：如果字段不存在才添加（用于旧数据库升级）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
    AND table_name = 'access_logs' 
    AND column_name = 'user_agent'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE access_logs 
     ADD COLUMN user_agent VARCHAR(500) COMMENT "用户代理",
     ADD COLUMN referer VARCHAR(500) COMMENT "来源页面",
     ADD COLUMN source VARCHAR(20) COMMENT "请求来源：WEB/API/CLI",
     ADD COLUMN forwarded_for VARCHAR(200) COMMENT "原始客户端 IP（代理场景）",
     ADD COLUMN request_id VARCHAR(50) COMMENT "请求链路 ID",
     ADD COLUMN command_name VARCHAR(100) COMMENT "命令/接口名称"',
    'SELECT "Columns already exist, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_accesslog_ip ON access_logs(ip);
CREATE INDEX IF NOT EXISTS idx_accesslog_source ON access_logs(source);
CREATE INDEX IF NOT EXISTS idx_accesslog_request_id ON access_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_accesslog_command_name ON access_logs(command_name);
