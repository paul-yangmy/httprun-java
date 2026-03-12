# Database Migration 说明

数据库为 **PostgreSQL 16+**，迁移工具为 **Flyway 9.x**，由应用启动时自动执行。

## 迁移文件

| 文件 | 说明 |
|------|------|
| `V1__init_schema.sql` | 完整 Schema 初始化，一次性创建所有表、索引及示例数据 |

## 数据库表结构

### commands 表
- 命令配置管理（名称、路径、状态、所属分组）
- 支持本地 / SSH 远程两种执行模式
- `command_config`、`remote_config` 均使用 `JSONB` 类型存储

### tokens 表
- JWT Token 管理与权限控制
- 支持按每日时段（`allowed_start_time` / `allowed_end_time`）及星期几（`allowed_weekdays`）限制执行
- `expires_at = NULL` 表示永久有效
- 支持按命令分组授权（`allowed_groups`）

### access_logs 表
- 完整请求审计日志
- 记录 IP、User-Agent、Referer、请求来源（WEB / API / CLI）
- 支持链路追踪（`request_id`）

### ssh_host_keys 表
- SSH 主机公钥指纹管理，实现 TOFU（Trust On First Use）策略
- 防止中间人攻击

### command_versions 表
- 命令配置变更历史，`snapshot` 字段以 `JSONB` 存储配置快照

## 部署方式

### Docker Compose（推荐）

```bash
docker compose up -d
```

应用启动时 Flyway 自动执行 `V1__init_schema.sql` 完成建表。

### 手动连接生产库

```bash
export DB_HOST=your-pg-host
export DB_PORT=5432
export DB_USER=httprun
export DB_PASSWORD=yourpassword
java -jar httprun-java.jar --spring.profiles.active=prod
```

## 环境变量配置

### 数据库连接

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_URL` | — | 完整 JDBC URL（优先级最高，设置后其余连接变量忽略） |
| `DB_HOST` | `localhost` | 数据库主机 |
| `DB_PORT` | `5432` | 数据库端口 |
| `DB_USER` | `httprun` | 数据库用户名 |
| `DB_PASSWORD` | — | 数据库密码 |
| `DB_DRIVER` | `org.postgresql.Driver` | JDBC 驱动类名 |

### 连接池（HikariCP）

| 变量 | 说明 |
|------|------|
| `DB_POOL_MIN_IDLE` | 最小空闲连接数（生产默认 10） |
| `DB_POOL_MAX_SIZE` | 最大连接数（生产默认 50） |
| `DB_POOL_IDLE_TIMEOUT` | 空闲超时，毫秒 |
| `DB_POOL_MAX_LIFETIME` | 连接最大生命周期，毫秒 |
| `DB_POOL_CONN_TIMEOUT` | 获取连接超时，毫秒 |

## 连接示例

### 标准连接
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=httprun
export DB_PASSWORD=yourpassword
```

### 使用完整 URL（SSL 场景）
```bash
export DB_URL="jdbc:postgresql://localhost:5432/httprun?sslmode=require"
export DB_USER=httprun
export DB_PASSWORD=yourpassword
```
