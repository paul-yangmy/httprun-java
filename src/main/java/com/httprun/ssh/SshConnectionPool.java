package com.httprun.ssh;

import com.httprun.config.SshPoolConfig;
import com.httprun.entity.RemoteConfig;
import com.httprun.repository.SshHostKeyRepository;
import com.httprun.util.CryptoUtils;
import com.jcraft.jsch.Session;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH 连接池管理器
 * <p>
 * 基于 Apache Commons Pool2 的 KeyedObjectPool 实现，
 * 以 host:port:username 为 key 对 SSH Session 进行池化管理。
 * <p>
 * 功能特性：
 * <ul>
 * <li>连接复用：相同目标主机的 SSH Session 自动复用</li>
 * <li>健康检查：借用/归还/空闲时自动验证连接可用性</li>
 * <li>自动驱逐：定期检测并剔除失效连接</li>
 * <li>KeepAlive：SSH 层心跳保持连接活跃</li>
 * <li>Prometheus 指标：连接池活跃数、空闲数等指标暴露</li>
 * </ul>
 */
@Slf4j
@Component
public class SshConnectionPool {

    private final GenericKeyedObjectPool<SshSessionKey, Session> pool;
    private final SshPoolConfig poolConfig;
    private final Map<String, Boolean> registeredMetrics = new ConcurrentHashMap<>();

    public SshConnectionPool(CryptoUtils cryptoUtils, SshPoolConfig poolConfig,
            SshHostKeyRepository hostKeyRepository, MeterRegistry meterRegistry) {
        this.poolConfig = poolConfig;

        // 创建主机指纹仓库
        DatabaseHostKeyRepository hostKeyRepo = poolConfig.isHostKeyCheckEnabled()
                ? new DatabaseHostKeyRepository(hostKeyRepository)
                : null;

        // 创建工厂
        SshSessionFactory factory = new SshSessionFactory(cryptoUtils, poolConfig, hostKeyRepo);

        // 配置连接池
        GenericKeyedObjectPoolConfig<Session> config = new GenericKeyedObjectPoolConfig<>();

        // 容量配置
        config.setMaxTotalPerKey(poolConfig.getMaxPerHost());
        config.setMaxIdlePerKey(poolConfig.getMaxIdlePerHost());
        config.setMinIdlePerKey(poolConfig.getMinIdlePerHost());
        config.setMaxTotal(poolConfig.getMaxTotal());

        // 借用等待配置
        config.setMaxWait(Duration.ofMillis(poolConfig.getBorrowTimeoutMs()));
        config.setBlockWhenExhausted(true);

        // 验证配置
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());
        config.setTestWhileIdle(poolConfig.isTestWhileIdle());

        // 驱逐配置
        config.setTimeBetweenEvictionRuns(Duration.ofMillis(poolConfig.getEvictionIntervalMs()));
        config.setMinEvictableIdleDuration(Duration.ofMillis(poolConfig.getMinIdleTimeMs()));

        // LIFO=false 使用 FIFO，均衡使用各连接
        config.setLifo(false);

        // JMX 注册
        config.setJmxEnabled(true);
        config.setJmxNamePrefix("SshSessionPool");

        this.pool = new GenericKeyedObjectPool<>(factory, config);

        // 注册全局 Prometheus 指标
        registerGlobalMetrics(meterRegistry);

        log.info("SSH connection pool initialized: maxPerHost={}, maxTotal={}, testOnBorrow={}, evictionInterval={}ms",
                poolConfig.getMaxPerHost(), poolConfig.getMaxTotal(),
                poolConfig.isTestOnBorrow(), poolConfig.getEvictionIntervalMs());
    }

    /**
     * 从池中借用一个 SSH Session
     *
     * @param remoteConfig 远程配置（含认证信息）
     * @return 可用的 SSH Session
     * @throws Exception 借用失败时抛出异常
     */
    public Session borrowSession(RemoteConfig remoteConfig) throws Exception {
        SshSessionKey key = buildKey(remoteConfig);
        log.debug("Borrowing SSH session for {}, pool status: active={}, idle={}",
                key.toLabel(), pool.getNumActive(key), pool.getNumIdle(key));

        try {
            // 通过 ThreadLocal 传递认证信息给工厂
            SshSessionFactory.setRemoteConfig(remoteConfig);
            Session session = pool.borrowObject(key);
            log.debug("Borrowed SSH session for {}, pool status: active={}, idle={}",
                    key.toLabel(), pool.getNumActive(key), pool.getNumIdle(key));
            return session;
        } finally {
            SshSessionFactory.clearRemoteConfig();
        }
    }

    /**
     * 归还 SSH Session 到池中
     *
     * @param remoteConfig 远程配置
     * @param session      要归还的 Session
     */
    public void returnSession(RemoteConfig remoteConfig, Session session) {
        SshSessionKey key = buildKey(remoteConfig);
        try {
            pool.returnObject(key, session);
            log.debug("Returned SSH session for {}, pool status: active={}, idle={}",
                    key.toLabel(), pool.getNumActive(key), pool.getNumIdle(key));
        } catch (Exception e) {
            log.warn("Failed to return SSH session for {}, destroying it: {}", key.toLabel(), e.getMessage());
            invalidateSession(remoteConfig, session);
        }
    }

    /**
     * 标记 Session 无效并从池中移除
     *
     * @param remoteConfig 远程配置
     * @param session      无效的 Session
     */
    public void invalidateSession(RemoteConfig remoteConfig, Session session) {
        SshSessionKey key = buildKey(remoteConfig);
        try {
            pool.invalidateObject(key, session);
            log.info("Invalidated SSH session for {}", key.toLabel());
        } catch (Exception e) {
            log.warn("Failed to invalidate SSH session for {}: {}", key.toLabel(), e.getMessage());
            // 确保断开连接
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 清除指定主机的所有空闲连接
     */
    public void clearHost(RemoteConfig remoteConfig) {
        SshSessionKey key = buildKey(remoteConfig);
        pool.clear(key);
        log.info("Cleared all idle SSH sessions for {}", key.toLabel());
    }

    /**
     * 关闭连接池，释放所有连接
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SSH connection pool, active={}, idle={}",
                pool.getNumActive(), pool.getNumIdle());
        pool.close();
        log.info("SSH connection pool shut down");
    }

    /**
     * 获取全局活跃连接数
     */
    public int getActiveCount() {
        return pool.getNumActive();
    }

    /**
     * 获取全局空闲连接数
     */
    public int getIdleCount() {
        return pool.getNumIdle();
    }

    /**
     * 获取指定主机的活跃连接数
     */
    public int getActiveCount(RemoteConfig remoteConfig) {
        return pool.getNumActive(buildKey(remoteConfig));
    }

    /**
     * 获取指定主机的空闲连接数
     */
    public int getIdleCount(RemoteConfig remoteConfig) {
        return pool.getNumIdle(buildKey(remoteConfig));
    }

    /**
     * 判断连接池是否启用
     */
    public boolean isEnabled() {
        return poolConfig.isEnabled();
    }

    /**
     * 构建连接池 Key
     */
    private SshSessionKey buildKey(RemoteConfig remoteConfig) {
        return new SshSessionKey(
                remoteConfig.getHost(),
                remoteConfig.getPort() != null ? remoteConfig.getPort() : 22,
                remoteConfig.getUsername());
    }

    /**
     * 注册全局 Prometheus 指标
     */
    private void registerGlobalMetrics(MeterRegistry meterRegistry) {
        Gauge.builder("httprun.ssh.pool.active", pool, GenericKeyedObjectPool::getNumActive)
                .description("SSH connection pool active sessions")
                .tags(Tags.empty())
                .register(meterRegistry);

        Gauge.builder("httprun.ssh.pool.idle", pool, GenericKeyedObjectPool::getNumIdle)
                .description("SSH connection pool idle sessions")
                .tags(Tags.empty())
                .register(meterRegistry);

        Gauge.builder("httprun.ssh.pool.waiters", pool, GenericKeyedObjectPool::getNumWaiters)
                .description("SSH connection pool threads waiting for a session")
                .tags(Tags.empty())
                .register(meterRegistry);

        Gauge.builder("httprun.ssh.pool.created.total", pool, GenericKeyedObjectPool::getCreatedCount)
                .description("SSH connection pool total sessions created")
                .tags(Tags.empty())
                .register(meterRegistry);

        Gauge.builder("httprun.ssh.pool.destroyed.total", pool, GenericKeyedObjectPool::getDestroyedCount)
                .description("SSH connection pool total sessions destroyed")
                .tags(Tags.empty())
                .register(meterRegistry);
    }
}
