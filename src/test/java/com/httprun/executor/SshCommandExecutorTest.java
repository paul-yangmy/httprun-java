package com.httprun.executor;

import com.httprun.config.SshPoolConfig;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.entity.RemoteConfig;
import com.httprun.repository.SshHostKeyRepository;
import com.httprun.ssh.SshConnectionPool;
import com.httprun.util.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SSH 命令执行器单元测试
 */
@ExtendWith(MockitoExtension.class)
class SshCommandExecutorTest {

    @Mock
    private CryptoUtils cryptoUtils;

    @Mock
    private SshConnectionPool sshConnectionPool;

    @Mock
    private SshHostKeyRepository sshHostKeyRepository;

    private SshPoolConfig sshPoolConfig;
    private SshCommandExecutor executor;
    private RunCommandRequest request;

    @BeforeEach
    void setUp() {
        sshPoolConfig = new SshPoolConfig();
        executor = new SshCommandExecutor(cryptoUtils, sshConnectionPool, sshPoolConfig, sshHostKeyRepository);
        request = new RunCommandRequest();
    }

    @Test
    void testIsAvailable() {
        assertTrue(executor.isAvailable());
    }

    @Test
    void testExecute_NullRemoteConfig() {
        request.setRemoteConfig(null);

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Remote config"));
    }

    @Test
    void testExecute_LocalhostHost() {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("localhost");
        request.setRemoteConfig(remoteConfig);

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Remote config"));
    }

    @Test
    void testExecute_LoopbackHost() {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("127.0.0.1");
        request.setRemoteConfig(remoteConfig);

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
    }

    @Test
    void testExecute_Ipv6LoopbackHost() {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("::1");
        request.setRemoteConfig(remoteConfig);

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
    }

    @Test
    void testExecute_BlankHost() {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("  ");
        request.setRemoteConfig(remoteConfig);

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
    }

    @Test
    void testExecute_PoolEnabled_BorrowFails() throws Exception {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("192.168.1.100");
        remoteConfig.setPort(22);
        remoteConfig.setUsername("admin");
        request.setRemoteConfig(remoteConfig);

        when(sshConnectionPool.isEnabled()).thenReturn(true);
        when(sshConnectionPool.borrowSession(remoteConfig))
                .thenThrow(new RuntimeException("Connection refused"));

        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Connection refused"));
        // Session 为 null 时不应调用 invalidate
        verify(sshConnectionPool, never()).invalidateSession(any(), any());
    }

    @Test
    void testExecute_PoolDisabled_DirectMode() throws Exception {
        RemoteConfig remoteConfig = new RemoteConfig();
        remoteConfig.setHost("invalid-host.example.com");
        remoteConfig.setPort(22);
        remoteConfig.setUsername("admin");
        remoteConfig.setPassword("pass");
        request.setRemoteConfig(remoteConfig);

        when(sshConnectionPool.isEnabled()).thenReturn(false);

        CommandExecutionResult result = executor.execute("echo hello", request, 3);

        assertNotNull(result);
        // 直连模式下应该返回错误（无法连接）
        assertNotNull(result.getError());
        // 不应与连接池交互
        verify(sshConnectionPool, never()).borrowSession(any());
        verify(sshConnectionPool, never()).returnSession(any(), any());
    }
}
