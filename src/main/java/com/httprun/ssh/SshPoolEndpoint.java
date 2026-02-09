package com.httprun.ssh;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSH 连接池 Actuator 端点
 * <p>
 * 访问路径: /actuator/ssh-pool
 * <p>
 * 提供连接池状态查看和管理操作
 */
@Slf4j
@Component
@Endpoint(id = "ssh-pool")
@RequiredArgsConstructor
public class SshPoolEndpoint {

    private final SshConnectionPool sshConnectionPool;

    /**
     * GET /actuator/ssh-pool — 查看连接池状态
     */
    @ReadOperation
    public Map<String, Object> poolStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", sshConnectionPool.isEnabled());
        status.put("activeConnections", sshConnectionPool.getActiveCount());
        status.put("idleConnections", sshConnectionPool.getIdleCount());
        return status;
    }
}
