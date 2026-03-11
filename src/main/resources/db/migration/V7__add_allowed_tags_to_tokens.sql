-- 为 tokens 表添加 allowed_tags 字段
-- allowed_tags: Token 允许执行的命令标签范围（逗号分隔，如 prod,deploy）
-- 若为 NULL，则按原有 subject 命令名列表校验；配置后按标签交集校验
ALTER TABLE tokens ADD COLUMN allowed_tags VARCHAR(500) DEFAULT NULL COMMENT 'Token 允许的命令标签范围（逗号分隔）';
