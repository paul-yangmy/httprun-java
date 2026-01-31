-- V4: 审计增强 - 添加更多审计字段到 access_logs 表

-- 添加 User-Agent 字段
ALTER TABLE access_logs ADD COLUMN user_agent VARCHAR(500) COMMENT '用户代理';

-- 添加 Referer 字段
ALTER TABLE access_logs ADD COLUMN referer VARCHAR(500) COMMENT '来源页面';

-- 添加来源字段 (WEB/API/CLI)
ALTER TABLE access_logs ADD COLUMN source VARCHAR(20) COMMENT '请求来源：WEB/API/CLI';

-- 添加转发 IP 字段
ALTER TABLE access_logs ADD COLUMN forwarded_for VARCHAR(200) COMMENT '原始客户端 IP（代理场景）';

-- 添加请求 ID 字段（用于链路追踪）
ALTER TABLE access_logs ADD COLUMN request_id VARCHAR(50) COMMENT '请求链路 ID';

-- 添加命令名称字段
ALTER TABLE access_logs ADD COLUMN command_name VARCHAR(100) COMMENT '命令/接口名称';

-- 添加索引以优化查询
CREATE INDEX idx_accesslog_ip ON access_logs(ip);
CREATE INDEX idx_accesslog_source ON access_logs(source);
CREATE INDEX idx_accesslog_request_id ON access_logs(request_id);
CREATE INDEX idx_accesslog_command_name ON access_logs(command_name);
