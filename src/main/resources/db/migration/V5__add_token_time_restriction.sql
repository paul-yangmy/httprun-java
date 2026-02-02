-- V5: 已废弃 - Token时间限制字段已整合到 V2__create_tokens_table.sql
-- 此文件保留仅用于 Flyway 版本控制，实际字段已在 V2 中创建
-- 如果是全新数据库，此文件不执行任何操作

-- 兼容性检查：如果字段不存在才添加（用于旧数据库升级）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
    AND table_name = 'tokens' 
    AND column_name = 'allowed_start_time'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE tokens 
     ADD COLUMN allowed_start_time VARCHAR(5) DEFAULT NULL COMMENT "允许执行开始时间（HH:mm）",
     ADD COLUMN allowed_end_time VARCHAR(5) DEFAULT NULL COMMENT "允许执行结束时间（HH:mm）",
     ADD COLUMN allowed_weekdays VARCHAR(50) DEFAULT NULL COMMENT "允许执行的星期几",
     ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT "备注"',
    'SELECT "Columns already exist, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修改 expires_at 为可空（支持永久Token）
SET @column_nullable = (
    SELECT IS_NULLABLE 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
    AND table_name = 'tokens' 
    AND column_name = 'expires_at'
);

SET @sql2 = IF(@column_nullable = 'NO',
    'ALTER TABLE tokens MODIFY COLUMN expires_at BIGINT NULL COMMENT "过期时间戳（NULL表示永久有效）"',
    'SELECT "expires_at already nullable, skipping" as message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
