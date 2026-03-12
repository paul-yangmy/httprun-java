-- V1: 完整数据库 Schema 初始化（PostgreSQL）
-- 整合原 V1-V9 所有迁移，全新安装直接执行此脚本即可

-- ============================================================
-- commands 表
-- ============================================================
CREATE TABLE IF NOT EXISTS commands (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    path            VARCHAR(200),
    description     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    command_config  JSONB,
    execution_mode  VARCHAR(20)  DEFAULT 'LOCAL',
    remote_config   JSONB,
    group_name      VARCHAR(50),
    timeout_seconds INT          DEFAULT 30,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_command_name UNIQUE (name),
    CONSTRAINT uk_command_path UNIQUE (path)
);

CREATE INDEX IF NOT EXISTS idx_command_name   ON commands (name);
CREATE INDEX IF NOT EXISTS idx_command_status ON commands (status);

-- ============================================================
-- tokens 表
-- ============================================================
CREATE TABLE IF NOT EXISTS tokens (
    id                 BIGSERIAL     PRIMARY KEY,
    subject            VARCHAR(1000) NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    is_admin           BOOLEAN       NOT NULL DEFAULT FALSE,
    issued_at          BIGINT        NOT NULL,
    expires_at         BIGINT,
    jwt_token          TEXT          NOT NULL,
    revoked            BOOLEAN       NOT NULL DEFAULT FALSE,
    allowed_start_time VARCHAR(5),
    allowed_end_time   VARCHAR(5),
    allowed_weekdays   VARCHAR(50),
    remark             VARCHAR(500),
    allowed_tags       VARCHAR(500),
    allowed_groups     VARCHAR(500),
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_token_jwt UNIQUE (jwt_token)
);

CREATE INDEX IF NOT EXISTS idx_token_name ON tokens (name);

-- ============================================================
-- access_logs 表
-- ============================================================
CREATE TABLE IF NOT EXISTS access_logs (
    id            BIGSERIAL    PRIMARY KEY,
    token_id      VARCHAR(500),
    path          VARCHAR(200) NOT NULL,
    ip            VARCHAR(50),
    method        VARCHAR(20),
    request       TEXT,
    response      TEXT,
    status_code   INT,
    duration      BIGINT,
    user_agent    VARCHAR(500),
    referer       VARCHAR(500),
    source        VARCHAR(20),
    forwarded_for VARCHAR(200),
    request_id    VARCHAR(50),
    command_name  VARCHAR(100),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_accesslog_token        ON access_logs (token_id);
CREATE INDEX IF NOT EXISTS idx_accesslog_path         ON access_logs (path);
CREATE INDEX IF NOT EXISTS idx_accesslog_created      ON access_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_accesslog_ip           ON access_logs (ip);
CREATE INDEX IF NOT EXISTS idx_accesslog_source       ON access_logs (source);
CREATE INDEX IF NOT EXISTS idx_accesslog_request_id   ON access_logs (request_id);
CREATE INDEX IF NOT EXISTS idx_accesslog_command_name ON access_logs (command_name);

-- ============================================================
-- ssh_host_keys 表
-- ============================================================
CREATE TABLE IF NOT EXISTS ssh_host_keys (
    id          BIGSERIAL     PRIMARY KEY,
    host        VARCHAR(255)  NOT NULL,
    port        INT           NOT NULL,
    key_type    VARCHAR(50)   NOT NULL,
    fingerprint VARCHAR(2000) NOT NULL,
    sha256_hash VARCHAR(100),
    first_seen  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trusted     BOOLEAN       NOT NULL DEFAULT TRUE,
    remark      VARCHAR(500),
    CONSTRAINT uk_host_port_type UNIQUE (host, port, key_type)
);

CREATE INDEX IF NOT EXISTS idx_hostkey_host_port ON ssh_host_keys (host, port);

-- ============================================================
-- command_versions 表
-- ============================================================
CREATE TABLE IF NOT EXISTS command_versions (
    id           BIGSERIAL    PRIMARY KEY,
    command_name VARCHAR(100) NOT NULL,
    version      INT          NOT NULL,
    snapshot     JSONB        NOT NULL,
    change_note  VARCHAR(500),
    changed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cv_name_version ON command_versions (command_name, version DESC);

-- ============================================================
-- 示例数据
-- ============================================================
INSERT INTO commands (name, description, command_config, execution_mode, timeout_seconds) VALUES
    ('echo', 'Echo 回显命令',
     '{"command": "echo {{.message}}", "params": [{"name": "message", "type": "string", "required": true, "description": "要回显的消息"}]}',
     'LOCAL', 30),
    ('ping', 'Ping 网络测试',
     '{"command": "ping -c {{.count}} {{.host}}", "params": [{"name": "host", "type": "string", "required": true}, {"name": "count", "type": "integer", "defaultValue": "4"}]}',
     'LOCAL', 60);
