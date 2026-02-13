package com.httprun.executor;

import com.httprun.config.SshPoolConfig;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.entity.RemoteConfig;
import com.httprun.repository.SshHostKeyRepository;
import com.httprun.ssh.DatabaseHostKeyRepository;
import com.httprun.ssh.SshConnectionPool;
import com.httprun.util.CryptoUtils;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSH 远程命令执行器
 * <p>
 * 支持两种模式：
 * 1. 连接池模式（默认）：通过 SshConnectionPool 复用 SSH Session，减少连接建立开销
 * 2. 直连模式：当连接池被禁用时，每次新建 SSH Session（向后兼容）
 * <p>
 * 超时分层：
 * - connectTimeoutMs：SSH 握手超时（建立 TCP + SSH 连接）
 * - channelConnectTimeoutMs：在已建立的 Session 上打开 exec 通道的超时
 * - executionTimeoutSeconds：命令执行等待结果的超时
 * <p>
 * 认证方式优先级：
 * 1. 用户显式提供的用户名+密码 / 私钥（二选一）
 * 2. 未显式提供时，使用服务器本地的默认 SSH 密钥（如 ~/.ssh/id_rsa）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshCommandExecutor implements CommandExecutor {

    private final CryptoUtils cryptoUtils;
    private final SshConnectionPool sshConnectionPool;
    private final SshPoolConfig sshPoolConfig;
    private final SshHostKeyRepository sshHostKeyRepository;

    @Override
    public CommandExecutionResult execute(String command, RunCommandRequest request, int timeoutSeconds) {
        RemoteConfig remoteConfig = request.getRemoteConfig();
        if (remoteConfig == null || isLocalhost(remoteConfig.getHost())) {
            return CommandExecutionResult.error("Remote config with valid host is required for SSH execution");
        }

        if (sshConnectionPool.isEnabled()) {
            return executeWithPool(command, remoteConfig, timeoutSeconds);
        } else {
            return executeDirectly(command, remoteConfig, timeoutSeconds);
        }
    }

    /**
     * 直连模式下创建 SSH Session，统一处理认证优先级与指纹校验。
     */
    private Session createDirectSession(RemoteConfig remoteConfig, int connectTimeoutMs) throws Exception {
        JSch jsch = new JSch();

        // 指纹管理
        if (sshPoolConfig.isHostKeyCheckEnabled()) {
            jsch.setHostKeyRepository(new DatabaseHostKeyRepository(sshHostKeyRepository));
            log.debug("Using database host key verification (direct mode)");
        }

        boolean hasPrivateKey = remoteConfig.getPrivateKey() != null && !remoteConfig.getPrivateKey().isBlank();
        boolean hasPassword = remoteConfig.getPassword() != null && !remoteConfig.getPassword().isBlank();

        // 优先使用用户显式提供的私钥
        if (hasPrivateKey) {
            String privateKey = cryptoUtils.isEncrypted(remoteConfig.getPrivateKey())
                    ? cryptoUtils.decrypt(remoteConfig.getPrivateKey())
                    : remoteConfig.getPrivateKey();
            jsch.addIdentity("key", privateKey.getBytes(), null, null);
            log.debug("Using provided private key for SSH authentication");
        }

        String defaultKeyPath = null;

        // 仅当用户未提供任何认证信息时，才尝试使用系统默认 SSH 密钥（免密登录）
        if (!hasPrivateKey && !hasPassword) {
            defaultKeyPath = getDefaultSshKeyPath();
            if (defaultKeyPath != null) {
                try {
                    jsch.addIdentity(defaultKeyPath);
                    log.debug("Using default SSH key: {}", defaultKeyPath);
                } catch (Exception e) {
                    log.debug("Failed to load default SSH key: {}", e.getMessage());
                }
            }
        }

        Session session = jsch.getSession(
                remoteConfig.getUsername(),
                remoteConfig.getHost(),
                remoteConfig.getPort() != null ? remoteConfig.getPort() : 22);

        // 认证方式选择：私钥优先，其次密码，最后默认密钥
        if (!hasPrivateKey && hasPassword) {
            String password = cryptoUtils.isEncrypted(remoteConfig.getPassword())
                    ? cryptoUtils.decrypt(remoteConfig.getPassword())
                    : remoteConfig.getPassword();
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", "password");
            log.debug("Using password authentication for SSH");
        } else if (hasPrivateKey || defaultKeyPath != null) {
            // 用户提供私钥或使用默认密钥时优先尝试公钥认证，并允许回退到密码认证
            session.setConfig("PreferredAuthentications", "publickey,password");
        } else {
            log.warn("No SSH authentication method available, connection may fail");
        }

        if (!sshPoolConfig.isHostKeyCheckEnabled()) {
            session.setConfig("StrictHostKeyChecking", "no");
        }
        session.setTimeout(connectTimeoutMs);
        session.connect(connectTimeoutMs);
        return session;
    }

    /**
     * 使用连接池执行 SSH 命令
     */
    private CommandExecutionResult executeWithPool(String command, RemoteConfig remoteConfig, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;
        boolean sessionInvalid = false;

        // 超时分层：区分通道连接超时和命令执行超时
        int channelTimeoutMs = sshPoolConfig.getEffectiveChannelConnectTimeoutMs();
        int execTimeoutSec = sshPoolConfig.getEffectiveExecutionTimeoutSeconds() > 0
                ? sshPoolConfig.getEffectiveExecutionTimeoutSeconds()
                : timeoutSeconds;

        log.debug("SSH timeout config: channelConnect={}ms, execution={}s", channelTimeoutMs, execTimeoutSec);

        try {
            // 1. 从连接池借用 Session
            session = sshConnectionPool.borrowSession(remoteConfig);

            // 2. 在复用的 Session 上打开 exec 通道执行命令
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);

            channel.connect(channelTimeoutMs);

            // 3. 等待命令完成（使用独立的执行超时）
            long deadline = System.currentTimeMillis() + (long) execTimeoutSec * 1000;
            while (!channel.isClosed()) {
                if (System.currentTimeMillis() > deadline) {
                    log.warn("SSH command execution timed out after {}s", execTimeoutSec);
                    return CommandExecutionResult.builder()
                            .error("Command execution timed out after " + execTimeoutSec + " seconds")
                            .stdout(stdout.toString())
                            .stderr(stderr.toString())
                            .exitCode(-1)
                            .duration(System.currentTimeMillis() - startTime)
                            .build();
                }
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();

            return CommandExecutionResult.builder()
                    .stdout(stdout.toString())
                    .stderr(stderr.toString())
                    .exitCode(exitCode)
                    .duration(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("SSH execution failed (pooled mode)", e);
            sessionInvalid = true;
            return CommandExecutionResult.builder()
                    .error(e.getMessage())
                    .duration(System.currentTimeMillis() - startTime)
                    .build();
        } finally {
            // 关闭 channel（但不关闭 session，session 归还给池）
            if (channel != null) {
                channel.disconnect();
            }
            // 根据执行结果归还或作废 session
            if (session != null) {
                if (sessionInvalid) {
                    sshConnectionPool.invalidateSession(remoteConfig, session);
                } else {
                    sshConnectionPool.returnSession(remoteConfig, session);
                }
            }
        }
    }

    /**
     * 直连模式执行 SSH 命令（兼容旧逻辑，连接池禁用时使用）
     */
    private CommandExecutionResult executeDirectly(String command, RemoteConfig remoteConfig, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;

        try {
            // 超时分层
            int connectTimeoutMs = sshPoolConfig.getConnectTimeoutMs();
            int channelTimeoutMs = sshPoolConfig.getEffectiveChannelConnectTimeoutMs();
            int execTimeoutSec = sshPoolConfig.getEffectiveExecutionTimeoutSeconds() > 0
                    ? sshPoolConfig.getEffectiveExecutionTimeoutSeconds()
                    : timeoutSeconds;

            // 1. 创建 SSH 会话（包含统一的认证优先级逻辑）
            session = createDirectSession(remoteConfig, connectTimeoutMs);

            // 2. 执行命令
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);

            channel.connect(channelTimeoutMs);

            // 4. 等待命令完成（使用独立的执行超时）
            long deadline = System.currentTimeMillis() + (long) execTimeoutSec * 1000;
            while (!channel.isClosed()) {
                if (System.currentTimeMillis() > deadline) {
                    log.warn("SSH command execution timed out after {}s (direct mode)", execTimeoutSec);
                    return CommandExecutionResult.builder()
                            .error("Command execution timed out after " + execTimeoutSec + " seconds")
                            .stdout(stdout.toString())
                            .stderr(stderr.toString())
                            .exitCode(-1)
                            .duration(System.currentTimeMillis() - startTime)
                            .build();
                }
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();

            return CommandExecutionResult.builder()
                    .stdout(stdout.toString())
                    .stderr(stderr.toString())
                    .exitCode(exitCode)
                    .duration(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("SSH execution failed (direct mode)", e);
            return CommandExecutionResult.builder()
                    .error(e.getMessage())
                    .duration(System.currentTimeMillis() - startTime)
                    .build();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    @Override
    public CompletableFuture<CommandExecutionResult> executeAsync(String command,
            RunCommandRequest request, int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> execute(command, request, timeoutSeconds));
    }

    /**
     * 流式执行 SSH 命令：在执行过程中通过 lineConsumer 逐行回调 stdout/stderr，
     * 并通过 cancelRegistrar 注册取消回调（调用者可在需要时执行该回调以断开 channel）。
     * 返回命令退出码。
     */
    public int executeStreaming(RemoteConfig remoteConfig, String command, int timeoutSeconds,
            BiConsumer<String, String> lineConsumer,
            Consumer<Runnable> cancelRegistrar) throws Exception {
        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;
        boolean borrowed = false;
        try {
            int channelTimeoutMs = sshPoolConfig.getEffectiveChannelConnectTimeoutMs();

            // 获取或创建 session（优先使用连接池）
            if (sshConnectionPool.isEnabled()) {
                session = sshConnectionPool.borrowSession(remoteConfig);
                borrowed = true;
            } else {
                session = createDirectSession(remoteConfig, sshPoolConfig.getConnectTimeoutMs());
            }

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // 使用管道捕获 stderr，以便可以分离读取
            PipedOutputStream errOut = new PipedOutputStream();
            PipedInputStream errIn = new PipedInputStream(errOut);
            channel.setErrStream(errOut);

            InputStreamReader stdoutReader = new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader stdoutBuf = new BufferedReader(stdoutReader);
            BufferedReader stderrBuf = new BufferedReader(new InputStreamReader(errIn, StandardCharsets.UTF_8));

            // 注册取消回调
            AtomicReference<ChannelExec> channelRef = new AtomicReference<>();
            cancelRegistrar.accept(() -> {
                ChannelExec ch = channelRef.get();
                if (ch != null) {
                    try {
                        ch.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            });

            channel.connect(channelTimeoutMs);
            channelRef.set(channel);

            // 启动读取线程
            Thread outThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutBuf.readLine()) != null) {
                        lineConsumer.accept("stdout", line);
                    }
                } catch (Exception ignored) {
                }
            }, "ssh-stdout-" + remoteConfig.getHost());

            Thread errThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrBuf.readLine()) != null) {
                        lineConsumer.accept("stderr", line);
                    }
                } catch (Exception ignored) {
                }
            }, "ssh-stderr-" + remoteConfig.getHost());

            outThread.start();
            errThread.start();

            long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;
            while (!channel.isClosed()) {
                if (timeoutSeconds > 0 && System.currentTimeMillis() > deadline) {
                    channel.disconnect();
                    throw new RuntimeException("Command execution timed out after " + timeoutSeconds + " seconds");
                }
                Thread.sleep(100);
            }

            // 等待读取线程结束
            outThread.join(2000);
            errThread.join(2000);

            int exit = channel.getExitStatus();
            return exit;

        } finally {
            if (channel != null) {
                try {
                    channel.disconnect();
                } catch (Exception ignored) {
                }
            }
            if (session != null) {
                if (borrowed) {
                    // 归还到连接池
                    sshConnectionPool.returnSession(remoteConfig, session);
                } else {
                    try {
                        session.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 判断是否为本机地址
     */
    private boolean isLocalhost(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String lower = host.toLowerCase().trim();
        return lower.equals("localhost") || lower.equals("127.0.0.1") || lower.equals("::1");
    }

    /**
     * 获取系统默认 SSH 私钥路径
     * 优先查找 id_rsa，然后是 id_ed25519
     */
    private String getDefaultSshKeyPath() {
        String userHome = System.getProperty("user.home");
        String[] keyNames = { "id_rsa", "id_ed25519", "id_ecdsa", "id_dsa" };

        for (String keyName : keyNames) {
            File keyFile = new File(userHome, ".ssh" + File.separator + keyName);
            if (keyFile.exists() && keyFile.isFile() && keyFile.canRead()) {
                return keyFile.getAbsolutePath();
            }
        }
        return null;
    }
}
