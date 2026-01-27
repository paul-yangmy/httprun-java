# HttpRun Java

一个基于 Spring Boot 的企业级 HTTP API Shell 命令网关系统，支持通过 RESTful API 安全执行预定义的 Shell 命令。

## 🚀 功能特性

- **命令管理**: 创建、更新、删除和查询命令配置
- **安全执行**: 基于 Token 的 API 访问控制
- **参数模板**: 支持 `{{.variable}}` 模板语法的命令参数
- **多执行模式**: 支持本地执行和 SSH 远程执行
- **审计日志**: 完整的命令执行日志记录
- **审批流程**: 高风险命令需要审批后执行
- **IP 白名单**: 支持 IP 访问限制
- **速率限制**: 防止 API 滥用
- **健康检查**: 内置健康检查接口

## 🛠️ 技术栈

- **Java 17** - LTS 版本
- **Spring Boot 3.2.x** - 应用框架
- **Spring Security 6.x** - 安全框架
- **Spring Data JPA** - 数据访问层
- **MySQL 8.0** - 数据库
- **Redis** - 缓存 (可选)
- **Flyway** - 数据库迁移
- **JWT** - Token 认证
- **OpenAPI 3.0** - API 文档
- **Docker** - 容器化部署

## 📁 项目结构

```
src/main/java/com/httprun/
├── HttpRunApplication.java      # 启动类
├── config/                      # 配置类
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── SwaggerConfig.java
│   └── ...
├── controller/                  # 控制器
│   ├── AuthController.java
│   ├── AdminController.java
│   ├── UserController.java
│   └── HealthController.java
├── service/                     # 服务层
│   ├── CommandService.java
│   ├── TokenService.java
│   └── ...
├── repository/                  # 数据访问层
├── entity/                      # 实体类
├── dto/                         # 数据传输对象
├── executor/                    # 命令执行器
├── security/                    # 安全模块
├── exception/                   # 异常处理
├── aspect/                      # AOP 切面
└── util/                        # 工具类
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (生产环境)
- Redis 7+ (可选)
- Docker & Docker Compose (容器化部署)

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd httprun-java
```

2. **使用开发模式启动** (H2 内存数据库)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. **访问 API 文档**
```
http://localhost:8080/swagger-ui.html
```

4. **访问 H2 控制台** (开发模式)
```
http://localhost:8080/h2-console
```

### Docker 部署

1. **构建并启动**
```bash
docker-compose up -d
```

2. **查看日志**
```bash
docker-compose logs -f httprun
```

3. **启用监控** (可选)
```bash
docker-compose --profile monitoring up -d
```

## 📖 API 使用

### 认证

**管理员登录获取 JWT:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 命令管理 (需要 Admin JWT)

**创建命令:**
```bash
curl -X POST http://localhost:8080/api/admin/commands \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "hello",
    "description": "Hello World 命令",
    "commandTemplate": "echo Hello, {{.name}}!",
    "paramsConfig": [
      {"name": "name", "type": "string", "required": true}
    ],
    "executionMode": "LOCAL",
    "timeout": 30000
  }'
```

### Token 管理 (需要 Admin JWT)

**创建 API Token:**
```bash
curl -X POST http://localhost:8080/api/admin/tokens \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-token",
    "description": "测试用 Token",
    "allowedCommands": ["echo", "hello"],
    "rateLimit": 100,
    "expiresIn": 86400000
  }'
```

### 执行命令 (使用 API Token)

```bash
curl -X POST http://localhost:8080/api/run/hello \
  -H "Authorization: Bearer <api_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "params": {"name": "World"},
    "async": false
  }'
```

## ⚙️ 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `dev` |
| `MYSQL_HOST` | MySQL 主机 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名 | `httprun` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | - |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `JWT_SECRET` | JWT 密钥 | - |
| `JWT_EXPIRATION` | JWT 过期时间(ms) | `3600000` |
| `COMMAND_TIMEOUT` | 命令默认超时(ms) | `30000` |

## 🔒 安全说明

1. **生产环境必须修改 JWT_SECRET**
2. **建议配置 HTTPS**
3. **配置 IP 白名单限制访问**
4. **定期轮换 API Token**
5. **高危命令启用审批流程**

## 📊 监控

- **健康检查**: `GET /api/health`
- **Prometheus 指标**: `GET /actuator/prometheus`
- **Grafana 仪表盘**: `http://localhost:3000` (需启用 monitoring profile)

## 📝 许可证

MIT License
