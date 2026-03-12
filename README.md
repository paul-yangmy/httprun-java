# HttpRun Java

HttpRun Java是一个基于[httprun](https://github.com/raojinlin/httprun)项目的改写:基于 Spring Boot 的 HTTP API Shell 命令网关，支持通过 REST API 安全执行预定义命令，提供本地/SSH 执行、Token 鉴权、审计日志、访问控制与可观测性能力。

## 核心特性

- 命令管理: 创建、更新、启用/禁用、删除命令
- 双执行模式: 本地执行 (`LOCAL`) + SSH 远程执行 (`SSH`)
- 参数模板: 支持 `{{.variable}}` 模板语法
- 安全鉴权: JWT Token、管理员/普通权限隔离
- 敏感信息保护: SSH 密码和私钥加密存储（AES-GCM）
- 可观测性: 健康检查、Actuator、Prometheus 指标
- 审计日志: 请求与命令执行日志追踪

## 技术栈

- Java 17
- Spring Boot 3.2.4
- Spring Security
- Spring Data JPA
- Spring WebSocket
- PostgreSQL 16（生产）/ SQLite（开发）
- Flyway
- Caffeine（本地缓存）
- Springdoc OpenAPI

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL 16+(生产); SQLite(开发)


### 1) 构建项目

默认会构建后端并打包前端到 Jar 内。

```bash
mvn clean package -DskipTests
```

仅构建后端（跳过前端）:

```bash
mvn clean package -DskipTests -DskipFrontend=true
```

### 2) 首次启动（生成管理员 Token）

```bash
java -jar target/httprun-java-1.0.0.jar --httprun.init-admin-token=true
```

启动后请保存控制台输出的管理员 Token（完整值只显示一次）。

### 3) 访问地址

- 管理界面: http://localhost:8081/admin
- API 文档: http://localhost:8081/swagger/index.html
- 健康检查: http://localhost:8081/api/health

## 开发模式

默认 `dev` 配置使用 SQLite（`./httprun.db`），无需额外配置数据库。

直接启动:

```bash
java -jar target/httprun-java-1.0.0.jar
```

## 生产部署（Jar）

### 首次启动（初始化管理员 Token）

```bash
java \
  -DSPRING_PROFILES_ACTIVE=prod \
  -DDB_HOST=your-pg-host \
  -DDB_PORT=5432 \
  -DDB_USER=httprun \
  -DDB_PASSWORD=your-db-password \
  -DJWT_SECRET=your-32+chars-secret \
  -jar target/httprun-java-1.0.0.jar \
  --httprun.init-admin-token=true
```

### 正式启动

```bash
java \
  -DSPRING_PROFILES_ACTIVE=prod \
  -DDB_HOST=your-pg-host \
  -DDB_PORT=5432 \
  -DDB_USER=httprun \
  -DDB_PASSWORD=your-db-password \
  -DJWT_SECRET=your-32+chars-secret \
  -jar target/httprun-java-1.0.0.jar
```

## Docker 部署

```bash
docker-compose up -d
```

查看日志:

```bash
docker-compose logs -f httprun
```

## 关键环境变量

| 变量 | 必填 | 说明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 否 | 运行环境，默认 `dev` |
| `DB_URL` | 否 | 完整 JDBC URL（设置后可忽略 DB_HOST/DB_PORT） |
| `DB_HOST` | 否 | PostgreSQL 主机，默认 `localhost` |
| `DB_PORT` | 否 | PostgreSQL 端口，默认 `5432` |
| `DB_USER` | 否 | 数据库用户名，默认 `httprun` |
| `DB_PASSWORD` | 否 | 数据库密码 |
| `JWT_SECRET` | 生产必填 | JWT 密钥，至少 32 字符 |
| `INIT_ADMIN_TOKEN` | 否 | 是否在启动时初始化管理员 Token |
| `WEBAPP_BUILD_DIR` | 否 | 前端构建目录，默认 `./webapp/dist` |

## 常用 API 示例

### 1) 创建普通 API Token（需管理员 Token）

```bash
curl -X POST http://localhost:8081/api/admin/token \
  -H "Authorization: Bearer <admin_jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "runner-token",
    "subject": "*",
    "isAdmin": false,
    "expiresIn": 24
  }'
```

### 2) 创建命令（需管理员 Token）

```bash
curl -X POST http://localhost:8081/api/admin/command \
  -H "Authorization: Bearer <admin_jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "hello",
    "description": "Say hello",
    "commandTemplate": "echo Hello, {{.name}}!",
    "paramsConfig": [
      {"name": "name", "type": "string", "required": true}
    ],
    "executionMode": "LOCAL",
    "timeout": 30000
  }'
```

### 3) 执行命令（使用 API Token）

```bash
curl -X POST http://localhost:8081/api/run/hello \
  -H "Authorization: Bearer <api_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "params": {"name": "World"},
    "async": false
  }'
```

## 安全建议

- 生产环境必须修改 `JWT_SECRET`
- 优先使用 HTTPS
- 配置 IP 白名单与限流策略
- 定期轮换 Token
- 避免在日志中输出敏感凭据

## 许可证

MIT License
