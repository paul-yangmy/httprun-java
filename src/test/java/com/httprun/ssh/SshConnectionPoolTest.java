package com.httprun.ssh;

import com.httprun.config.SshPoolConfig;
import com.httprun.entity.RemoteConfig;
import com.httprun.repository.SshHostKeyRepository;
import com.httprun.util.CryptoUtils;
import com.jcraft.jsch.Session;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSH 连接池单元测试
 */
@ExtendWith(MockitoExtension.class)
class SshConnectionPoolTest {

    @Mock
    private CryptoUtils cryptoUtils;

    @Mock
    private SshHostKeyRepository sshHostKeyRepository;

    private SshPoolConfig poolConfig;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        poolConfig = new SshPoolConfig();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void testPoolInitialization() {
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        assertTrue(pool.isEnabled());
        assertEquals(0, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());

        pool.shutdown();
    }

    @Test
    void testPoolDisabled() {
        poolConfig.setEnabled(false);
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        assertFalse(pool.isEnabled());

        pool.shutdown();
    }

    @Test
    void testPoolMetricsRegistered() {
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        assertNotNull(meterRegistry.find("httprun.ssh.pool.active").gauge());
        assertNotNull(meterRegistry.find("httprun.ssh.pool.idle").gauge());
        assertNotNull(meterRegistry.find("httprun.ssh.pool.waiters").gauge());
        assertNotNull(meterRegistry.find("httprun.ssh.pool.created.total").gauge());
        assertNotNull(meterRegistry.find("httprun.ssh.pool.destroyed.total").gauge());

        pool.shutdown();
    }

    @Test
    void testBorrowSession_InvalidHost_ThrowsException() {
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        RemoteConfig config = new RemoteConfig();
        config.setHost("invalid-host-that-does-not-exist.example.com");
        config.setPort(22);
        config.setUsername("testuser");
        config.setPassword("testpass");

        // 应该抛出异常，因为无法连接到不存在的主机
        assertThrows(Exception.class, () -> pool.borrowSession(config));

        pool.shutdown();
    }

    @Test
    void testGetHostCounts() {
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        RemoteConfig config = new RemoteConfig();
        config.setHost("192.168.1.100");
        config.setPort(22);
        config.setUsername("admin");

        assertEquals(0, pool.getActiveCount(config));
        assertEquals(0, pool.getIdleCount(config));

        pool.shutdown();
    }

    @Test
    void testClearHost() {
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        RemoteConfig config = new RemoteConfig();
        config.setHost("192.168.1.100");
        config.setPort(22);
        config.setUsername("admin");

        // 不应抛出异常
        assertDoesNotThrow(() -> pool.clearHost(config));

        pool.shutdown();
    }

    @Test
    void testDefaultPoolConfig() {
        SshPoolConfig config = new SshPoolConfig();

        assertTrue(config.isEnabled());
        assertEquals(5, config.getMaxPerHost());
        assertEquals(2, config.getMaxIdlePerHost());
        assertEquals(0, config.getMinIdlePerHost());
        assertEquals(50, config.getMaxTotal());
        assertEquals(10000, config.getConnectTimeoutMs());
        assertEquals(5000, config.getBorrowTimeoutMs());
        assertEquals(30000, config.getEvictionIntervalMs());
        assertEquals(60000, config.getMinIdleTimeMs());
        assertEquals(600000, config.getMaxLifetimeMs());
        assertTrue(config.isTestOnBorrow());
        assertFalse(config.isTestOnReturn());
        assertTrue(config.isTestWhileIdle());
        assertEquals(15000, config.getKeepAliveIntervalMs());
        // 新增配置项
        assertTrue(config.isHostKeyCheckEnabled());
        assertEquals(0, config.getChannelConnectTimeoutMs());
        assertEquals(0, config.getExecutionTimeoutSeconds());
    }

    @Test
    void testPoolConfig_EffectiveChannelTimeout() {
        SshPoolConfig config = new SshPoolConfig();

        // 默认 channelConnectTimeoutMs=0，应回退到 connectTimeoutMs
        assertEquals(config.getConnectTimeoutMs(), config.getEffectiveChannelConnectTimeoutMs());

        // 设置独立值
        config.setChannelConnectTimeoutMs(5000);
        assertEquals(5000, config.getEffectiveChannelConnectTimeoutMs());
    }

    @Test
    void testPoolConfig_EffectiveExecutionTimeout() {
        SshPoolConfig config = new SshPoolConfig();

        // 默认 executionTimeoutSeconds=0，应回退到 0
        assertEquals(0, config.getEffectiveExecutionTimeoutSeconds());

        // 设置独立值
        config.setExecutionTimeoutSeconds(120);
        assertEquals(120, config.getEffectiveExecutionTimeoutSeconds());
    }

    @Test
    void testPoolInitialization_HostKeyCheckDisabled() {
        poolConfig.setHostKeyCheckEnabled(false);
        SshConnectionPool pool = new SshConnectionPool(cryptoUtils, poolConfig, sshHostKeyRepository, meterRegistry);

        assertTrue(pool.isEnabled());
        pool.shutdown();
    }
}
