-- MySQL 初始化脚本
-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS httprun 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 授权
GRANT ALL PRIVILEGES ON httprun.* TO 'httprun'@'%';
FLUSH PRIVILEGES;

USE httprun;

-- 注意: 表结构由 Flyway 迁移脚本创建
