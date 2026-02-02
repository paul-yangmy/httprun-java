-- V5: Token 权限细化 - 添加时间范围限制字段
-- 支持按时间范围限制 Token 的有效执行时间

-- 添加允许执行的开始时间（每日，格式：HH:mm，如 09:00）
ALTER TABLE tokens ADD COLUMN allowed_start_time VARCHAR(5) DEFAULT NULL;

-- 添加允许执行的结束时间（每日，格式：HH:mm，如 18:00）
ALTER TABLE tokens ADD COLUMN allowed_end_time VARCHAR(5) DEFAULT NULL;

-- 添加允许执行的星期几（JSON 数组，如 [1,2,3,4,5] 表示周一到周五）
-- 1=周一, 2=周二, ..., 7=周日
ALTER TABLE tokens ADD COLUMN allowed_weekdays VARCHAR(50) DEFAULT NULL;

-- 添加备注字段
ALTER TABLE tokens ADD COLUMN remark VARCHAR(500) DEFAULT NULL;
