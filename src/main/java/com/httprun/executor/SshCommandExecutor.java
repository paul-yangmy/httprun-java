package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.entity.RemoteConfig;
import com.httprun.util.CryptoUtils;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * SSH 远程命令执行器
 * 
 * 支持两种认证方式：
 * 1. 免密登录：使用系统默认 SSH 密钥（~/.ssh/id_rsa）
 * 2. 密码登录：使用加密存储的密码
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshCommandExecutor implements CommandExecutor {

    private final CryptoUtils cryptoUtils;

    @Override
    public CommandExecutionResult execute(String command, RunCommandRequest request, int timeoutSeconds) {
        RemoteConfig remoteConfig = request.getRemoteConfig();
        if (remoteConfig == null || isLocalhost(remoteConfig.getHost())) {
            return CommandExecutionResult.error("Remote config with valid host is required for SSH execution");
        }

        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;

        try {
            // 1. 创建 SSH 会话
            JSch jsch = new JSch();

            // 认证方式优先级：指定私钥 > 指定密码 > 系统默认密钥（免密登录）
            boolean hasAuth = false;

            // 方式1：使用指定的私钥
            if (remoteConfig.getPrivateKey() != null && !remoteConfig.getPrivateKey().isBlank()) {
                String privateKey = cryptoUtils.isEncrypted(remoteConfig.getPrivateKey())
                        ? cryptoUtils.decrypt(remoteConfig.getPrivateKey())
                        : remoteConfig.getPrivateKey();
                jsch.addIdentity("key", privateKey.getBytes(), null, null);
                hasAuth = true;
                log.debug("Using provided private key for SSH authentication");
            }

            // 方式2：尝试使用系统默认 SSH 密钥（免密登录）
            if (!hasAuth) {
                String defaultKeyPath = getDefaultSshKeyPath();
                if (defaultKeyPath != null) {
                    try {
                        jsch.addIdentity(defaultKeyPath);
                        hasAuth = true;
                        log.debug("Using default SSH key: {}", defaultKeyPath);
                    } catch (Exception e) {
                        log.debug("Failed to load default SSH key: {}", e.getMessage());
                    }
                }
            }

            // 2. 建立连接
            session = jsch.getSession(
                    remoteConfig.getUsername(),
                    remoteConfig.getHost(),
                    remoteConfig.getPort() != null ? remoteConfig.getPort() : 22);

            // 方式3：使用密码认证（解密后使用）
            if (remoteConfig.getPassword() != null && !remoteConfig.getPassword().isBlank()) {
                String password = cryptoUtils.isEncrypted(remoteConfig.getPassword())
                        ? cryptoUtils.decrypt(remoteConfig.getPassword())
                        : remoteConfig.getPassword();
                session.setPassword(password);
                hasAuth = true;
                log.debug("Using password authentication for SSH");
            }

            if (!hasAuth) {
                log.warn("No SSH authentication method available, connection may fail");
            }

            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(timeoutSeconds * 1000);
            session.connect();

            // 3. 执行命令
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);

            channel.connect();

            // 4. 等待命令完成
            while (!channel.isClosed()) {
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
            log.error("SSH execution failed", e);
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
