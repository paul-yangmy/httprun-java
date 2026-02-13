package com.httprun.controller;

import com.httprun.entity.Command;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.ExecutionMode;
import com.httprun.repository.CommandRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SSH 配置诊断 API
 * <p>
 * 提供命令配置诊断功能，帮助排查 SSH 执行问题
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Tag(name = "Diagnostic", description = "诊断与调试 API")
public class DiagnosticController {

    private final CommandRepository commandRepository;

    @GetMapping("/commands")
    @Operation(summary = "诊断所有命令配置", description = "检查所有命令的执行模式和配置完整性")
    public ResponseEntity<DiagnosticResult> diagnoseAllCommands() {
        List<Command> allCommands = commandRepository.findAll();

        DiagnosticResult result = new DiagnosticResult();
        result.setTotalCommands(allCommands.size());

        Map<String, Integer> modeStats = new HashMap<>();
        List<CommandIssue> issues = new ArrayList<>();

        for (Command command : allCommands) {
            // 统计执行模式
            String mode = command.getExecutionMode().name();
            modeStats.put(mode, modeStats.getOrDefault(mode, 0) + 1);

            // 检查 SSH 命令配置
            if (command.getExecutionMode() == ExecutionMode.SSH) {
                CommandIssue issue = checkSshCommand(command);
                if (issue != null) {
                    issues.add(issue);
                }
            }
        }

        result.setModeStatistics(modeStats);
        result.setIssues(issues);
        result.setHealthy(issues.isEmpty());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/commands/{name}")
    @Operation(summary = "诊断指定命令", description = "检查指定命令的配置详情")
    public ResponseEntity<CommandDiagnostic> diagnoseCommand(@PathVariable String name) {
        Command command = commandRepository.findByName(name).orElse(null);

        if (command == null) {
            return ResponseEntity.notFound().build();
        }

        CommandDiagnostic diagnostic = new CommandDiagnostic();
        diagnostic.setName(command.getName());
        diagnostic.setExecutionMode(command.getExecutionMode().name());

        if (command.getExecutionMode() == ExecutionMode.SSH) {
            RemoteConfig remoteConfig = command.getRemoteConfig();

            diagnostic.setHasRemoteConfig(remoteConfig != null);

            if (remoteConfig != null) {
                diagnostic.setHost(remoteConfig.getHost());
                diagnostic.setPort(remoteConfig.getPort());
                diagnostic.setUsername(remoteConfig.getUsername());
                diagnostic.setHasPassword(remoteConfig.getPassword() != null && !remoteConfig.getPassword().isBlank());
                diagnostic.setHasPrivateKey(
                        remoteConfig.getPrivateKey() != null && !remoteConfig.getPrivateKey().isBlank());

                // 检查配置问题
                List<String> problems = new ArrayList<>();

                if (remoteConfig.getHost() == null || remoteConfig.getHost().isBlank()) {
                    problems.add("host 未配置");
                } else if (isLocalhost(remoteConfig.getHost())) {
                    problems.add("host 配置为本地地址: " + remoteConfig.getHost());
                }

                if (remoteConfig.getUsername() == null || remoteConfig.getUsername().isBlank()) {
                    problems.add("username 未配置");
                }

                if (!diagnostic.isHasPassword() && !diagnostic.isHasPrivateKey()) {
                    problems.add("未配置密码或私钥，将尝试使用系统默认 SSH 密钥");
                }

                diagnostic.setProblems(problems);
                diagnostic.setHealthy(problems.stream().noneMatch(p -> p.contains("未配置")));
            } else {
                diagnostic.setProblems(List.of("remoteConfig 未配置"));
                diagnostic.setHealthy(false);
            }
        } else {
            diagnostic.setHealthy(true);
        }

        return ResponseEntity.ok(diagnostic);
    }

    /**
     * 检查 SSH 命令配置
     */
    private CommandIssue checkSshCommand(Command command) {
        RemoteConfig remoteConfig = command.getRemoteConfig();
        List<String> problems = new ArrayList<>();

        if (remoteConfig == null) {
            problems.add("remoteConfig 为 null");
        } else {
            String host = remoteConfig.getHost();
            if (host == null || host.isBlank()) {
                problems.add("host 为空或 null");
            } else if (isLocalhost(host)) {
                problems.add("host 配置为本地地址: " + host);
            }

            if (remoteConfig.getUsername() == null || remoteConfig.getUsername().isBlank()) {
                problems.add("username 为空或 null");
            }

            boolean hasPassword = remoteConfig.getPassword() != null && !remoteConfig.getPassword().isBlank();
            boolean hasPrivateKey = remoteConfig.getPrivateKey() != null && !remoteConfig.getPrivateKey().isBlank();

            if (!hasPassword && !hasPrivateKey) {
                problems.add("未配置密码或私钥（将尝试使用系统默认 SSH 密钥）");
            }
        }

        if (!problems.isEmpty()) {
            CommandIssue issue = new CommandIssue();
            issue.setCommandName(command.getName());
            issue.setCommandId(command.getId());
            issue.setProblems(problems);
            return issue;
        }

        return null;
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

    @Data
    public static class DiagnosticResult {
        private int totalCommands;
        private Map<String, Integer> modeStatistics;
        private List<CommandIssue> issues;
        private boolean healthy;
    }

    @Data
    public static class CommandIssue {
        private Long commandId;
        private String commandName;
        private List<String> problems;
    }

    @Data
    public static class CommandDiagnostic {
        private String name;
        private String executionMode;
        private boolean hasRemoteConfig;
        private String host;
        private Integer port;
        private String username;
        private boolean hasPassword;
        private boolean hasPrivateKey;
        private List<String> problems;
        private boolean healthy;
    }
}
