-- V9: 清理已废弃的标签相关字段
-- 权限模型已简化为：admin > allowedGroups（命令分组）> subject 命令列表
-- 标签（tags / allowed_tags）已在代码层完全移除，本次同步清理数据库字段

-- 1. 命令表：移除 tags 字段
ALTER TABLE commands DROP COLUMN tags;

-- 2. Token 表：移除 allowed_tags 字段（V7 引入，已由 allowed_groups 替代）
ALTER TABLE tokens DROP COLUMN allowed_tags;
