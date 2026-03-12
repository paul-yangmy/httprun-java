-- PostgreSQL 初始化脚本
-- 此脚本在容器首次启动时由 Docker entrypoint 执行
-- 数据库及用户已通过环境变量 POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD 自动创建
-- 表结构由 Flyway 迁移脚本（V1__create_commands_table.sql）负责创建

-- 确保 httprun 数据库存在（Docker 镜像已通过 POSTGRES_DB 创建，此处为冗余保障）
SELECT 'Database httprun is ready.' AS status;
