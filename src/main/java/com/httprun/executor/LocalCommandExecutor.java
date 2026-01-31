package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 本地命令执行器
 * 
 * ⚠️ 重要说明：所有命令在 HttpRun 服务部署的机器上本地执行
 */
@Slf4j
@Component
public class LocalCommandExecutor implements CommandExecutor {

    private final ExecutorService executorService;
    private final Semaphore semaphore; // 并发控制

    public LocalCommandExecutor() {
        // 最大并发执行数
        int maxConcurrent = 10;
        this.executorService = Executors.newFixedThreadPool(maxConcurrent);
        this.semaphore = new Semaphore(maxConcurrent);
    }

    @Override
    public CommandExecutionResult execute(String command, RunCommandRequest request, int timeoutSeconds) {
        // 尝试获取信号量
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!acquired) {
                return CommandExecutionResult.error("Execution queue full, please retry later");
            }

            return doExecute(command, request, timeoutSeconds);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandExecutionResult.error("Execution interrupted");
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    private CommandExecutionResult doExecute(String command, RunCommandRequest request, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 解析命令为参数列表（类似 Go 的 shlex.Split）
            List<String> cmdArgs = parseCommand(command);
            log.info("Executing command: {}", cmdArgs);

            // 2. 构建进程（类似 Go 的 exec.CommandContext）
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs);

            // 3. 注入环境变量（类似 Go 的 cmd.Env）
            if (request.getEnv() != null) {
                for (var envVar : request.getEnv()) {
                    processBuilder.environment().put(envVar.getName(), envVar.getValue());
                }
            }

            // 4. 合并标准输出和错误输出
            processBuilder.redirectErrorStream(false);

            // 5. 启动进程
            Process process = processBuilder.start();

            // 6. 异步读取输出
            CompletableFuture<String> stdoutFuture = CompletableFuture
                    .supplyAsync(() -> readStream(process.getInputStream()), executorService);
            CompletableFuture<String> stderrFuture = CompletableFuture
                    .supplyAsync(() -> readStream(process.getErrorStream()), executorService);

            // 7. 等待进程完成（带超时）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                // 超时，强制终止进程（类似 Go 的 cmd.Process.Kill()）
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                return CommandExecutionResult.builder()
                        .exitCode(-1)
                        .stderr("Command timeout after " + timeoutSeconds + " seconds")
                        .duration(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 8. 获取输出
            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            return CommandExecutionResult.builder()
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .duration(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Command execution failed", e);
            return CommandExecutionResult.builder()
                    .error(e.getMessage())
                    .duration(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public CompletableFuture<CommandExecutionResult> executeAsync(String command,
            RunCommandRequest request, int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> execute(command, request, timeoutSeconds), executorService);
    }

    /**
     * 解析命令字符串为参数列表
     * 类似 Go 的 shlex.Split
     */
    private List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        args.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    private String readStream(java.io.InputStream inputStream) {
        // Windows 中文系统使用 GBK 编码，其他系统使用 UTF-8
        Charset charset = System.getProperty("os.name").toLowerCase().contains("windows")
                ? Charset.forName("GBK")
                : Charset.forName("UTF-8");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
