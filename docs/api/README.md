# HttpRun API 文档

## 概述

HttpRun 是一个 HTTP API Shell 命令网关系统，提供安全的命令执行能力。

**Base URL**: `http://localhost:8080`

**认证方式**:
- 管理员接口: JWT Token (Header: `Authorization: Bearer <jwt_token>`)
- 用户接口: API Token (Header: `Authorization: Bearer <api_token>`)

---

## 认证接口

### 管理员登录

**POST** `/api/auth/login`

**请求体:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应:**
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400000
  }
}
```

---

## 管理员接口

> 所有管理员接口需要 JWT Token 认证

### 命令管理

#### 创建命令

**POST** `/api/admin/commands`

**请求体:**
```json
{
  "name": "echo",
  "description": "Echo message",
  "commandTemplate": "echo {{.message}}",
  "paramsConfig": [
    {
      "name": "message",
      "type": "string",
      "required": true,
      "description": "Message to echo"
    }
  ],
  "executionMode": "LOCAL",
  "timeout": 30000,
  "workingDir": "/tmp",
  "riskLevel": "LOW"
}
```

#### 获取命令列表

**GET** `/api/admin/commands`

**查询参数:**
- `page` (int): 页码，默认 0
- `size` (int): 每页数量，默认 20
- `status` (string): 状态筛选

#### 获取单个命令

**GET** `/api/admin/commands/{id}`

#### 更新命令

**PUT** `/api/admin/commands/{id}`

#### 删除命令

**DELETE** `/api/admin/commands/{id}`

---

### Token 管理

#### 创建 Token

**POST** `/api/admin/tokens`

**请求体:**
```json
{
  "name": "api-token-1",
  "description": "API Token for service",
  "allowedCommands": ["echo", "disk-usage"],
  "allowedIps": ["192.168.1.0/24"],
  "rateLimit": 100,
  "expiresIn": 86400000
}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "api-token-1",
    "token": "httprun_xxxxxx...",
    "expiresAt": "2024-01-01T00:00:00"
  }
}
```

> ⚠️ Token 只在创建时返回一次，请妥善保存

#### 获取 Token 列表

**GET** `/api/admin/tokens`

#### 撤销 Token

**DELETE** `/api/admin/tokens/{id}`

---

### 访问日志

#### 查询日志

**GET** `/api/admin/logs`

**查询参数:**
- `page` (int): 页码
- `size` (int): 每页数量
- `commandName` (string): 命令名筛选
- `status` (string): 状态筛选
- `startTime` (datetime): 开始时间
- `endTime` (datetime): 结束时间

---

## 用户接口

> 使用 API Token 认证

### 执行命令

**POST** `/api/run/{commandName}`

**请求体:**
```json
{
  "params": {
    "message": "Hello World"
  },
  "async": false
}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "commandName": "echo",
    "success": true,
    "exitCode": 0,
    "stdout": "Hello World\n",
    "stderr": "",
    "executionTime": 15,
    "executedAt": "2024-01-01T12:00:00"
  }
}
```

---

## 健康检查

### 健康状态

**GET** `/api/health`

**响应:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "timestamp": "2024-01-01T12:00:00",
    "components": {
      "db": "UP",
      "redis": "UP"
    }
  }
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 参数验证失败 |
| 1002 | 参数缺失 |
| 1003 | 参数格式错误 |
| 2001 | 未授权访问 |
| 2002 | Token 无效 |
| 2003 | Token 过期 |
| 2004 | 权限不足 |
| 3001 | 命令不存在 |
| 3002 | 命令已禁用 |
| 3003 | 命令执行失败 |
| 3004 | 命令执行超时 |
| 4001 | 内部服务错误 |
| 5001 | 速率限制 |
