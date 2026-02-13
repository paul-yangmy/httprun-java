package com.httprun.util;

import com.httprun.entity.Command;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.ExecutionMode;
import com.httprun.repository.CommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSH 命令配置诊断工具
 * <p>
 * 用于排查 SSH 命令配置问题，检查：
 * 1. executionMode 是否正确设置为 SSH
 * 2. remoteConfig 是否完整配置
 * 3. host 是否为有效的远程地址（非 localhost）
 * <p>
 * 使用方法：启动应用时添加 profile: --spring.profiles.active=dev,ssh-diagnostic
 */
@Slf4j
@Component
@Profile("ssh-diagnostic")
@RequiredArgsConstructor
public class SshCommandDiagnostic implements CommandLineRunner {

    private final CommandRepository commandRepository;

    @Override
    public void run(String... args) {
        log.info("================== SSH 命令配置诊断开始 ==================");

        List<Command> allCommands = commandRepository.findAll();
        log.info("数据库中共有 {} 条命令", allCommands.size());

        int sshCount = 0;
        int localCount = 0;
        int agentCount = 0;
        int configIssues = 0;

        for (Command command : allCommands) {
            ExecutionMode mode = command.getExecutionMode();

            // 统计执行模式
            switch (mode) {
                case SSH -> sshCount++;
                case LOCAL -> localCount++;
                case AGENT -> agentCount++;
            }

            // 检查 SSH 模式的命令配置
            if (mode == ExecutionMode.SSH) {
                boolean hasIssue = false;
                StringBuilder issue = new StringBuilder();
                issue.append(String.format("命令 [%s] (ID: %d) 配置问题: ", command.getName(), command.getId()));

                RemoteConfig remoteConfig = command.getRemoteConfig();

                if (remoteConfig == null) {
                    issue.append("❌ remoteConfig 为 null");
                    hasIssue = true;
                } else {
                    // 检查 host
                    String host = remoteConfig.getHost();
                    if (host == null || host.isBlank()) {
                        issue.append("❌ host 为空或 null");
                        hasIssue = true;
                    } else if (isLocalhost(host)) {
                        issue.append(String.format("⚠️  host 配置为本地地址: %s (命令会在本地执行)", host));
                        hasIssue = true;
                    } else {
                        issue.append(String.format("✅ host: %s", host));
                    }

                    // 检查端口
                    Integer port = remoteConfig.getPort();
                    if (port == null) {
                        issue.append(", ⚠️  port 为 null (将使用默认 22)");
                    } else {
                        issue.append(String.format(", ✅ port: %d", port));
                    }

                    // 检查用户名
                    String username = remoteConfig.getUsername();
                    if (username == null || username.isBlank()) {
                        issue.append(", ❌ username 为空或 null");
                        hasIssue = true;
                    } else {
                        issue.append(String.format(", ✅ username: %s", username));
                    }

                    // 检查认证方式
                    boolean hasPassword = remoteConfig.getPassword() != null && !remoteConfig.getPassword().isBlank();
                    boolean hasPrivateKey = remoteConfig.getPrivateKey() != null
                            && !remoteConfig.getPrivateKey().isBlank();

                    if (hasPassword) {
                        issue.append(", ✅ 已配置密码");
                    }
                    if (hasPrivateKey) {
                        issue.append(", ✅ 已配置私钥");
                    }
                    if (!hasPassword && !hasPrivateKey) {
                        issue.append(", ⚠️  未配置密码和私钥 (将尝试使用系统默认 SSH 密钥)");
                    }
                }

                if (hasIssue) {
                    log.warn(issue.toString());
                    configIssues++;
                } else {
                    log.info("命令 [{}] (ID: {}) SSH 配置正常: {}",
                            command.getName(), command.getId(), issue.toString());
                }
            }
        }

        log.info("================== 诊断结果汇总 ==================");
        log.info("执行模式统计:");
        log.info("  - LOCAL 模式: {} 条", localCount);
        log.info("  - SSH 模式: {} 条", sshCount);
        log.info("  - AGENT 模式: {} 条", agentCount);

        if (configIssues > 0) {
            log.warn("⚠️  发现 {} 条 SSH 命令配置存在问题，请检查上述日志", configIssues);
        } else {
            log.info("✅ 所有 SSH 命令配置正常");
        }

        log.info("================== SSH 命令配置诊断结束 ==================");
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
}
