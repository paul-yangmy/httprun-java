package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.entity.RemoteConfig;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * SSH 远程命令执行器
 */
@Slf4j
@Component
public class SshCommandExecutor implements CommandExecutor {

    @Override
    public CommandExecutionResult execute(String command, RunCommandRequest request, int timeoutSeconds) {
        RemoteConfig remoteConfig = request.getRemoteConfig();
        if (remoteConfig == null) {
            return CommandExecutionResult.error("Remote config is required for SSH execution");
        }

        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;

        try {
            // 1. 创建 SSH 会话
            JSch jsch = new JSch();

            // 添加私钥认证
            if (remoteConfig.getPrivateKey() != null) {
                jsch.addIdentity("key", remoteConfig.getPrivateKey().getBytes(), null, null);
            }

            // 2. 建立连接
            session = jsch.getSession(
                    remoteConfig.getUsername(),
                    remoteConfig.getHost(),
                    remoteConfig.getPort() != null ? remoteConfig.getPort() : 22);

            // 密码认证
            if (remoteConfig.getPassword() != null) {
                session.setPassword(remoteConfig.getPassword());
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
}
