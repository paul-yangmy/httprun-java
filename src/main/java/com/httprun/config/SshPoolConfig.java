package com.httprun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSH 连接池配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ssh.pool")
public class SshPoolConfig {

    /**
     * 是否启用连接池
     */
    private boolean enabled = true;

    /**
     * 每个主机的最大连接数
     */
    private int maxPerHost = 5;

    /**
     * 每个主机的最大空闲连接数
     */
    private int maxIdlePerHost = 2;

    /**
     * 每个主机的最小空闲连接数
     */
    private int minIdlePerHost = 0;

    /**
     * 全局最大连接数
     */
    private int maxTotal = 50;

    /**
     * 连接超时时间（毫秒）- 建立 SSH 连接的超时
     */
    private int connectTimeoutMs = 10000;

    /**
     * 借用连接等待超时（毫秒）
     */
    private long borrowTimeoutMs = 5000;

    /**
     * 空闲连接驱逐检查间隔（毫秒）
     */
    private long evictionIntervalMs = 30000;

    /**
     * 空闲连接最小存活时间（毫秒），超过此时间的空闲连接可能被驱逐
     */
    private long minIdleTimeMs = 60000;

    /**
     * 连接最大存活时间（毫秒），超过后强制回收，-1 表示不限制
     */
    private long maxLifetimeMs = 600000;

    /**
     * 借用时是否测试连接可用性
     */
    private boolean testOnBorrow = true;

    /**
     * 归还时是否测试连接可用性
     */
    private boolean testOnReturn = false;

    /**
     * 空闲时是否测试连接可用性（驱逐检查时）
     */
    private boolean testWhileIdle = true;

    /**
     * 健康检查发送 keepAlive 的间隔（毫秒）
     */
    private long keepAliveIntervalMs = 15000;

    // ========== 指纹管理 ==========

    /**
     * 是否启用主机指纹验证（防中间人攻击）
     * <p>
     * 启用后，首次连接将记录主机指纹（TOFU 策略），
     * 后续连接自动验证指纹是否变化。
     */
    private boolean hostKeyCheckEnabled = true;

    // ========== 超时配置 ==========

    /**
     * 通道连接超时（毫秒）— 在已建立的 Session 上打开 exec 通道的超时
     * <p>
     * 与 connectTimeoutMs（SSH 握手超时）独立配置。
     * 默认 0 表示跟随 connectTimeoutMs。
     */
    private int channelConnectTimeoutMs = 0;

    /**
     * 命令执行超时（秒）— 命令执行后等待结果的最大时间
     * <p>
     * 与 connectTimeoutMs（SSH 握手超时）独立配置。
     * 默认 0 表示使用调用方传入的 timeoutSeconds。
     */
    private int executionTimeoutSeconds = 0;

    /**
     * 获取有效的通道连接超时（毫秒）
     * <p>
     * 如果未单独配置，则使用 connectTimeoutMs
     */
    public int getEffectiveChannelConnectTimeoutMs() {
        return channelConnectTimeoutMs > 0 ? channelConnectTimeoutMs : connectTimeoutMs;
    }

    /**
     * 获取有效的执行超时（秒）
     * <p>
     * 如果未单独配置，则返回 0 表示使用调用方参数
     */
    public int getEffectiveExecutionTimeoutSeconds() {
        return executionTimeoutSeconds;
    }
}
