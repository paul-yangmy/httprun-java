package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地命令执行器测试
 */
class LocalCommandExecutorTest {

    private LocalCommandExecutor executor;
    private RunCommandRequest request;

    @BeforeEach
    void setUp() {
        executor = new LocalCommandExecutor();
        request = new RunCommandRequest();
    }

    @Test
    void testIsAvailable() {
        assertTrue(executor.isAvailable());
    }

    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void testExecute_UnixEcho() {
        CommandExecutionResult result = executor.execute("echo hello", request, 30);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("hello"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecute_WindowsEcho() {
        CommandExecutionResult result = executor.execute("cmd /c echo hello", request, 30);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("hello"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecute_WindowsDir() {
        CommandExecutionResult result = executor.execute("cmd /c dir", request, 30);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertNotNull(result.getStdout());
    }

    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void testExecute_Timeout() {
        // 执行一个会超时的命令
        CommandExecutionResult result = executor.execute("sleep 10", request, 1);

        assertNotNull(result);
        assertTrue(result.getExitCode() != 0 || result.getStderr().contains("timeout"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecute_WindowsTimeout() {
        // Windows 下模拟超时（ping localhost 10次）
        CommandExecutionResult result = executor.execute("cmd /c ping localhost -n 10", request, 1);

        assertNotNull(result);
        // 超时后强制终止
        assertTrue(result.getExitCode() != 0 || result.getStderr() != null);
    }

    @Test
    void testExecuteAsync() throws Exception {
        String command = System.getProperty("os.name").toLowerCase().contains("win")
                ? "cmd /c echo async"
                : "echo async";

        CompletableFuture<CommandExecutionResult> future = executor.executeAsync(command, request, 30);
        CommandExecutionResult result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("async"));
    }

    @Test
    void testDurationTracking() {
        String command = System.getProperty("os.name").toLowerCase().contains("win")
                ? "cmd /c echo test"
                : "echo test";

        CommandExecutionResult result = executor.execute(command, request, 30);

        assertNotNull(result);
        assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }
}
