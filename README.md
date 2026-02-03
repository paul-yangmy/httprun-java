# HttpRun Java
一个基于[httprun](https://github.com/raojinlin/httprun)项目的改写: 基于Spring Boot 的企业级 HTTP API Shell 命令网关系统，支持通过 RESTful API 安全执行预定义的 Shell 命令。

## 🚀 功能特性

- **命令管理**: 创建、更新、删除和查询命令配置
- **安全执行**: 基于 Token 的 API 访问控制
- **参数模板**: 支持 `{{.variable}}` 模板语法的命令参数
- **多执行模式**: 支持本地执行和 SSH 远程执行
  - 本地执行：在服务器本地运行 Shell 命令
  - SSH 远程执行：通过 SSH 在远程服务器执行命令（支持免密登录和密码认证）
- **SSH 认证方式**: 三级认证机制，优先级为指定私钥 > 系统默认密钥 > 密码认证
- **凭证加密**: 采用 AES-GCM 256 位加密存储 SSH 密码和私钥
- **实时输出**: WebSocket 实时推送命令执行输出
- **审计日志**: 完整的命令执行日志记录
- **IP 白名单**: 支持 IP 访问限制
- **速率限制**: 防止 API 滥用
- **健康检查**: 内置健康检查接口

## 🛠️ 技术栈

- **Java 17** - LTS 版本
- **Spring Boot 3.2.x** - 应用框架
- **Spring Security 6.x** - 安全框架
- **Spring WebSocket** - 实时通信
- **Spring Data JPA** - 数据访问层
- **MySQL 8.0** - 数据库
- **Redis** - 缓存 (可选)
- **Flyway** - 数据库迁移
- **JWT** - Token 认证
- **OpenAPI 3.0** - API 文档
- **Docker** - 容器化部署

### 项目结构

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
│   ├── LocalCommandExecutor.java    # 本地命令执行
│   └── SshCommandExecutor.java      # SSH 远程命令执行
├── security/                    # 安全模块
├── exception/                   # 异常处理
├── aspect/                      # AOP 切面
├── websocket/                   # WebSocket 实时通信
└── util/                        # 工具类
    └── CryptoUtils.java         # AES-GCM 加密工具
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (生产环境)
- Redis 7+ (可选)
- Node.js 18+ (前端开发)
- Docker & Docker Compose (容器化部署)

### 本地开发

#### 后端启动

1. **克隆项目**
```bash
git clone <repository-url>
cd httprun-java
```

2. **构建项目**
```bash
mvn clean package -DskipTests
```

3. **使用开发模式启动**（SQLite 数据库，自动生成管理员 Token）
```bash
java -jar target/httprun-java-1.0.0.jar --httprun.init-admin-token=true
```

4. **保存控制台输出的管理员 Token**
```
========================================
管理员 Token 生成成功!
========================================
Token ID:     1
Token Name:   admin
JWT Token:
----------------------------------------
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIq...
----------------------------------------
请保存此 Token，后续将无法再次查看完整内容!
========================================
```

5. **访问服务**
- Web 界面: http://localhost:8081/admin
- API 文档: http://localhost:8081/swagger-ui.html

#### 前端开发

1. **进入前端目录**
```bash
cd webapp
```

2. **安装依赖**
```bash
npm install
```

3. **启动开发服务器**
```bash
npm start
```

4. **构建生产版本**
```bash
npm run build
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
**创建带有 SSH 远程执行的命令:**
```bash
curl -X POST http://localhost:8080/api/admin/commands \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "remote-echo",
    "description": "在远程服务器执行 echo 命令",
    "commandTemplate": "echo Hello from {{.host}}",
    "executionMode": "SSH",
    "remoteConfig": {
      "host": "192.168.1.100",
      "port": 22,
      "username": "root",
      "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----"
    },
    "paramsConfig": [
      {"name": "host", "type": "string", "required": true}
    ],
    "timeout": 30000
  }'
```

### SSH 密码和私钥处理

- **敏感信息加密**: 所有 SSH 密码和私钥都使用 AES-GCM 256 位加密存储在数据库
- **自动解密**: 执行命令时自动识别和解密加密的认证信息
- **API 脱敏**: 返回给前端的数据中，密码和私钥字段会被脱敏为 "******"

### 执行命令 (使用 API Token)

**本地执行:**
```bash
curl -X POST http://localhost:8080/api/run/hello \
  -H "Authorization: Bearer <api_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "params": {"name": "World"},
    "async": false
  }'
```

**远程 SSH 执行:**
```bash
curl -X POST http://localhost:8080/api/run/remote-echo \
  -H "Authorization: Bearer <api_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "params": {"host": "192.168.1.100"},
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
| `HTTPRUN_CRYPTO_SECRET_KEY` | SSH 认证信息加密密钥 | 系统自动生成 |

### SSH 认证配置

**优先级：**
1. **指定私钥** - 如果 `remoteConfig.privateKey` 不为空，优先使用此密钥
2. **系统默认密钥** - 自动查找 `~/.ssh/id_rsa`、`~/.ssh/id_ed25519` 等默认密钥（免密登录）
3. **密码认证** - 如果前两者都失败，使用 `remoteConfig.password` 进行密码认证

**私钥格式：**
- 支持 RSA、ECDSA、EdDSA 格式的 OpenSSH 格式私钥
- 可通过前端 Monaco Editor 编辑或粘贴
- 敏感信息自动加密存储

## 🔒 安全说明

1. **生产环境必须修改 JWT_SECRET**
2. **SSH 密钥和密码自动使用 AES-GCM 加密存储**
3. **建议配置 HTTPS**
4. **配置 IP 白名单限制访问**
5. **定期轮换 API Token**
6. **不要在日志中输出未脱敏的 SSH 凭证**
7. **SSH 私钥推荐使用无密码密钥或使用 SSH Agent**

## 📊 监控

- **健康检查**: `GET /api/health`
- **Prometheus 指标**: `GET /actuator/prometheus`
- **Grafana 仪表盘**: `http://localhost:3000` (需启用 monitoring profile)

## 📝 许可证

MIT License
