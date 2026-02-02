# Database Migration 说明

## 文件说明

### 主要建表语句（用于全新数据库）

- **V1__create_commands_table.sql** - 创建命令配置表
- **V2__create_tokens_table.sql** - 创建 Token 表（包含时间限制、备注、永久有效支持）
- **V3__create_access_logs_table.sql** - 创建访问日志表（包含完整审计字段）

### 兼容性升级语句（用于旧数据库）

- **V4__add_audit_fields_to_access_logs.sql** - 为旧数据库添加审计字段（已整合到 V3）
- **V5__add_token_time_restriction.sql** - 为旧数据库添加 Token 时间限制字段（已整合到 V2）

## 使用方式

### 全新数据库部署

1. 配置数据库连接
2. 启动应用，Flyway 会自动执行 V1、V2、V3 创建完整表结构
3. V4、V5 会检测字段已存在，自动跳过

### 旧数据库升级

1. 如果已有旧版本数据库（执行过 V1-V5 ALTER 语句）
2. V4、V5 会检测字段是否存在，只在缺失时添加
3. 实现平滑升级，不影响现有数据

## 核心表结构

### commands 表
- 命令配置管理
- 支持本地/SSH 远程执行
- JSON 格式存储参数配置

### tokens 表
- JWT Token 认证
- 支持时间范围限制（每日时段、星期几）
- 支持永久有效 Token（expires_at = NULL）
- 支持备注信息

### access_logs 表
- 完整审计日志
- 包含 IP、User-Agent、Referer 等信息
- 支持请求链路追踪（request_id）
- 区分请求来源（WEB/API/CLI）

## 环境变量配置

### 数据库连接
- `DB_URL` - 完整数据库 URL（优先级最高）
- `DB_HOST` - 数据库主机（默认: localhost）
- `DB_PORT` - 数据库端口（默认: 3306）
- `DB_USER` - 数据库用户名
- `DB_PASSWORD` - 数据库密码
- `DB_DRIVER` - 数据库驱动（默认: com.mysql.cj.jdbc.Driver）
  - MySQL 8+: `com.mysql.cj.jdbc.Driver`
  - MySQL 5.7: `com.mysql.jdbc.Driver`

### 连接池配置
- `DB_POOL_MIN_IDLE` - 最小空闲连接数
- `DB_POOL_MAX_SIZE` - 最大连接数
- `DB_POOL_IDLE_TIMEOUT` - 空闲超时（毫秒）
- `DB_POOL_MAX_LIFETIME` - 连接最大生命周期（毫秒）
- `DB_POOL_CONN_TIMEOUT` - 连接超时（毫秒）

## 示例

### MySQL 8.x
```bash
export DB_DRIVER=com.mysql.cj.jdbc.Driver
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=yourpassword
```

### MySQL 5.7
```bash
export DB_DRIVER=com.mysql.jdbc.Driver
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=yourpassword
```

### 使用完整 URL
```bash
export DB_URL="jdbc:mysql://localhost:3306/httprun?useUnicode=true&characterEncoding=utf8&useSSL=false"
export DB_USER=root
export DB_PASSWORD=yourpassword
export DB_DRIVER=com.mysql.cj.jdbc.Driver
```
