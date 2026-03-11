package com.httprun.service.impl;

import com.httprun.dto.request.CommandImportRequest;
import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandImportResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.dto.response.CommandVersionResponse;
import com.httprun.entity.Command;
import com.httprun.entity.CommandVersion;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.CommandStatus;
import com.httprun.enums.ExecutionMode;
import com.httprun.exception.BusinessException;
import com.httprun.executor.CommandExecutor;
import com.httprun.executor.CommandTemplate;
import com.httprun.executor.LocalCommandExecutor;
import com.httprun.executor.SshCommandExecutor;
import com.httprun.repository.CommandRepository;
import com.httprun.repository.CommandVersionRepository;
import com.httprun.service.CommandService;
import com.httprun.util.CommandSecurityValidator;
import com.httprun.util.CryptoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 命令服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;
    private final CommandVersionRepository commandVersionRepository;
    private final CommandTemplate commandTemplate;
    private final LocalCommandExecutor localExecutor;
    private final SshCommandExecutor sshExecutor;
    private final CryptoUtils cryptoUtils;
    private final CommandSecurityValidator securityValidator;
    private final ObjectMapper objectMapper;

    // 自注入代理引用，用于 importCommands 中绕过 self-call 限制，使每条命令拥有独立事务
    @Lazy
    @Autowired
    private CommandService self;

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public CommandResponse createCommand(CreateCommandRequest request) {
        // 验证命令模板，禁止多命令
        if (request.getCommandConfig() != null && request.getCommandConfig().getCommand() != null) {
            securityValidator.validateCommandTemplate(request.getCommandConfig().getCommand());
        }

        Command command = new Command();
        command.setName(request.getName());
        command.setPath(request.getPath() != null && !request.getPath().isBlank()
                ? request.getPath()
                : "/api/run/" + request.getName());
        command.setDescription(request.getDescription());
        command.setCommandConfig(request.getCommandConfig());
        ExecutionMode mode = request.getExecutionMode() != null ? request.getExecutionMode() : ExecutionMode.LOCAL;
        command.setExecutionMode(mode);
        command.setRemoteConfig(encryptRemoteConfig(request.getRemoteConfig()));
        command.setGroupName(request.getGroupName());
        command.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 30);

        command = commandRepository.save(command);
        return toResponse(command);
    }

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public CommandResponse updateCommand(String name, CreateCommandRequest request) {
        // 验证命令模板，禁止多命令
        if (request.getCommandConfig() != null && request.getCommandConfig().getCommand() != null) {
            securityValidator.validateCommandTemplate(request.getCommandConfig().getCommand());
        }

        Command command = commandRepository.findByName(name)
                .orElseThrow(() -> new BusinessException("Command not found: " + name));

        // 更新前保存版本快照
        saveVersionSnapshot(command, request.getChangeNote());

        // 更新命令信息
        if (request.getPath() != null) {
            command.setPath(request.getPath());
        }
        if (request.getDescription() != null) {
            command.setDescription(request.getDescription());
        }
        if (request.getCommandConfig() != null) {
            command.setCommandConfig(request.getCommandConfig());
        }
        if (request.getExecutionMode() != null) {
            command.setExecutionMode(request.getExecutionMode());
        }
        if (request.getRemoteConfig() != null) {
            RemoteConfig toSave = encryptOrKeepRemoteConfig(
                    command.getRemoteConfig(), request.getRemoteConfig());
            command.setRemoteConfig(toSave);
        }
        // groupName 前端始终显式传递（清空时传 null），无条件更新
        command.setGroupName(
                request.getGroupName() != null && !request.getGroupName().isBlank()
                        ? request.getGroupName()
                        : null);
        if (request.getTimeoutSeconds() != null) {
            command.setTimeoutSeconds(request.getTimeoutSeconds());
        }

        command = commandRepository.save(command);
        return toResponse(command);
    }

    @Override
    @Cacheable(value = "commands", key = "'all'")
    public List<CommandResponse> listAllCommands() {
        return commandRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommandResponse> listCommands(List<String> names) {
        if (names == null || names.isEmpty()) {
            return listAllCommands();
        }
        return commandRepository.findByNameIn(names).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommandExecutionResult runCommand(RunCommandRequest request, String tokenSubject, String allowedGroups) {
        // 1. 查询命令
        Command command = commandRepository.findByName(request.getName())
                .orElseThrow(() -> new BusinessException("Command not found: " + request.getName()));

        // 2. 检查命令状态
        if (command.getStatus() != CommandStatus.ACTIVE) {
            return CommandExecutionResult.error("Command is inactive");
        }

        // 3. 检查权限（优先级：admin > allowedGroups > subject 命令名列表）
        if (tokenSubject != null && !tokenSubject.equals("admin")) {
            boolean permitted = false;
            if (allowedGroups != null && !allowedGroups.isBlank()) {
                // 分组授权：命令所属分组在允许分组列表中
                List<String> groups = Arrays.asList(allowedGroups.split(","));
                permitted = command.getGroupName() != null && groups.contains(command.getGroupName());
            }
            if (!permitted) {
                // 原有 subject 命令名列表校验（仅当 allowedGroups 未配置时）
                if (allowedGroups == null || allowedGroups.isBlank()) {
                    List<String> allowedCommands = Arrays.asList(tokenSubject.split(","));
                    if (!allowedCommands.contains(command.getName())) {
                        return CommandExecutionResult.error("Permission denied");
                    }
                } else {
                    return CommandExecutionResult
                            .error("Permission denied: command does not match token's allowed groups");
                }
            }
        }

        // 4. 验证参数
        commandTemplate.validateParams(command, request);

        // 5. 渲染命令模板（带脱敏）
        String[] rendered = commandTemplate.renderWithMasking(command, request);
        String actualCommand = rendered[0]; // 实际执行的命令
        String maskedCommand = rendered[1]; // 脱敏后的日志
        log.info("Executing command: {} (masked)", maskedCommand);

        // 6. 选择执行器并执行（null 视为本地执行）
        ExecutionMode mode = command.getExecutionMode() != null ? command.getExecutionMode() : ExecutionMode.LOCAL;
        CommandExecutor executor = selectExecutor(mode);
        int timeout = request.getTimeout() != null ? request.getTimeout() : command.getTimeoutSeconds();

        // 7. SSH 模式：使用命令中持久化的 remoteConfig，支持 host/username 参数化模板（{{.host}} 等）
        if (mode == ExecutionMode.SSH) {
            RemoteConfig cmdRemote = command.getRemoteConfig();
            if (cmdRemote == null) {
                return CommandExecutionResult.error("SSH 命令未配置远程主机信息，请在编辑命令中填写主机、端口、用户名等");
            }
            RemoteConfig resolvedRemote = resolveRemoteConfigParams(cmdRemote, request);
            if (resolvedRemote.getHost() == null || resolvedRemote.getHost().isBlank()) {
                return CommandExecutionResult.error(
                        "SSH 主机地址未提供，请在请求参数中传入 host 参数，或在命令配置中填写固定主机地址");
            }
            request.setRemoteConfig(resolvedRemote);
        }

        return executor.execute(actualCommand, request, timeout);
    }

    private CommandExecutor selectExecutor(ExecutionMode mode) {
        if (mode == null) {
            return localExecutor;
        }
        return switch (mode) {
            case SSH -> sshExecutor;
            case AGENT -> throw new UnsupportedOperationException("Agent mode not implemented");
            default -> localExecutor;
        };
    }

    /**
     * 解析 remoteConfig 中的模板变量（host/username 支持 {{.paramName}} 格式）。
     * 密码和私钥不做模板处理，保持安全。
     */
    private RemoteConfig resolveRemoteConfigParams(RemoteConfig config, RunCommandRequest request) {
        Map<String, String> paramMap = new HashMap<>();
        if (request.getParams() != null) {
            request.getParams().forEach(p -> {
                if (p.getName() != null && p.getValue() != null) {
                    paramMap.put(p.getName(), String.valueOf(p.getValue()));
                }
            });
        }
        RemoteConfig resolved = new RemoteConfig();
        resolved.setHost(renderSimpleTemplate(config.getHost(), paramMap));
        resolved.setPort(config.getPort());
        resolved.setUsername(renderSimpleTemplate(config.getUsername(), paramMap));
        resolved.setPassword(config.getPassword());
        resolved.setPrivateKey(config.getPrivateKey());
        resolved.setSshKeyId(config.getSshKeyId());
        resolved.setAgentId(config.getAgentId());
        return resolved;
    }

    /**
     * 简单模板渲染：将 {{.varName}} 或 {{varName}} 替换为 paramMap 中的值。
     */
    private String renderSimpleTemplate(String template, Map<String, String> paramMap) {
        if (template == null || !template.contains("{{")) {
            return template;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("\\{\\{\\s*\\.?([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = paramMap.getOrDefault(varName, "");
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 更新时：合并 host/port/username，仅对新填写的密码/私钥加密，已有加密值保留（避免二次加密）
     */
    private RemoteConfig encryptOrKeepRemoteConfig(RemoteConfig existing, RemoteConfig fromRequest) {
        if (fromRequest == null) {
            return existing;
        }
        RemoteConfig result = new RemoteConfig();
        result.setHost(fromRequest.getHost() != null && !fromRequest.getHost().isBlank()
                ? fromRequest.getHost()
                : (existing != null ? existing.getHost() : null));
        result.setPort(
                fromRequest.getPort() != null ? fromRequest.getPort() : (existing != null ? existing.getPort() : null));
        result.setUsername(fromRequest.getUsername() != null && !fromRequest.getUsername().isBlank()
                ? fromRequest.getUsername()
                : (existing != null ? existing.getUsername() : null));
        result.setSshKeyId(fromRequest.getSshKeyId() != null ? fromRequest.getSshKeyId()
                : (existing != null ? existing.getSshKeyId() : null));
        result.setAgentId(fromRequest.getAgentId() != null ? fromRequest.getAgentId()
                : (existing != null ? existing.getAgentId() : null));
        if (fromRequest.getPassword() != null && !fromRequest.getPassword().isBlank()) {
            result.setPassword(cryptoUtils.encrypt(fromRequest.getPassword()));
        } else if (existing != null && existing.getPassword() != null && !existing.getPassword().isBlank()) {
            result.setPassword(existing.getPassword());
        }
        if (fromRequest.getPrivateKey() != null && !fromRequest.getPrivateKey().isBlank()) {
            result.setPrivateKey(cryptoUtils.encrypt(fromRequest.getPrivateKey()));
        } else if (existing != null && existing.getPrivateKey() != null && !existing.getPrivateKey().isBlank()) {
            result.setPrivateKey(existing.getPrivateKey());
        }
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public void updateCommandStatus(List<String> names, String status) {
        CommandStatus commandStatus = CommandStatus.valueOf(status.toUpperCase());
        commandRepository.updateStatusByNameIn(names, commandStatus);
    }

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public void deleteCommands(List<String> names) {
        commandRepository.deleteByNameIn(names);
    }

    @Override
    public List<CommandResponse> listCommandsByGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return listAllCommands();
        }
        return commandRepository.findAll().stream()
                .filter(cmd -> cmd.getGroupName() != null && groups.contains(cmd.getGroupName()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listAllGroupNames() {
        return commandRepository.findAll().stream()
                .map(Command::getGroupName)
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<CreateCommandRequest> exportCommands(List<String> names) {
        List<Command> commands;
        if (names == null || names.isEmpty()) {
            commands = commandRepository.findAll();
        } else {
            commands = commandRepository.findByNameIn(names);
        }
        return commands.stream()
                .map(this::toExportRequest)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "commands", allEntries = true)
    public CommandImportResult importCommands(CommandImportRequest request) {
        boolean overwrite = "overwrite".equalsIgnoreCase(request.getMode());
        boolean rename = "rename".equalsIgnoreCase(request.getMode());
        int created = 0, overwritten = 0, skipped = 0, failed = 0, renamed = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (CreateCommandRequest cmd : request.getCommands()) {
            try {
                boolean exists = commandRepository.existsByName(cmd.getName());
                if (exists) {
                    if (overwrite) {
                        // 通过代理调用，使 updateCommand 的 @Transactional 生效，确保每条命令独立事务
                        self.updateCommand(cmd.getName(), cmd);
                        overwritten++;
                    } else if (rename) {
                        String newName = generateUniqueName(cmd.getName());
                        cmd.setName(newName);
                        cmd.setPath(null); // 清空旧 path，由 createCommand 基于新名称自动生成
                        self.createCommand(cmd);
                        renamed++;
                    } else {
                        skipped++;
                    }
                } else {
                    self.createCommand(cmd);
                    created++;
                }
            } catch (Exception e) {
                failed++;
                errors.add(cmd.getName() + ": " + e.getMessage());
                log.warn("Failed to import command '{}': {}", cmd.getName(), e.getMessage());
            }
        }

        log.info("Import completed: created={}, overwritten={}, skipped={}, failed={}, renamed={}",
                created, overwritten, skipped, failed, renamed);
        return CommandImportResult.builder()
                .created(created)
                .overwritten(overwritten)
                .skipped(skipped)
                .renamed(renamed)
                .failed(failed)
                .errors(errors)
                .build();
    }

    /**
     * 生成不重复的命令名称（在 baseName 后追加 -copy，如已存在则追加 -copy-2、-copy-3 等）
     */
    private String generateUniqueName(String baseName) {
        String candidate = baseName + "-copy";
        if (!commandRepository.existsByName(candidate)) {
            return candidate;
        }
        int i = 2;
        while (commandRepository.existsByName(candidate + "-" + i)) {
            i++;
        }
        return candidate + "-" + i;
    }

    /**
     * 将 Command 实体转为导出 DTO（密码/私钥脱敏，不暴露加密密文）
     */
    private CreateCommandRequest toExportRequest(Command command) {
        CreateCommandRequest req = new CreateCommandRequest();
        req.setName(command.getName());
        req.setPath(command.getPath());
        req.setDescription(command.getDescription());
        req.setCommandConfig(command.getCommandConfig());
        req.setExecutionMode(command.getExecutionMode());
        req.setGroupName(command.getGroupName());
        req.setTimeoutSeconds(command.getTimeoutSeconds());
        // 导出远程配置时，密码和私钥置空（避免暴露加密密文，需在目标系统重新配置）
        if (command.getRemoteConfig() != null) {
            RemoteConfig exported = new RemoteConfig();
            exported.setHost(command.getRemoteConfig().getHost());
            exported.setPort(command.getRemoteConfig().getPort());
            exported.setUsername(command.getRemoteConfig().getUsername());
            exported.setSshKeyId(command.getRemoteConfig().getSshKeyId());
            exported.setAgentId(command.getRemoteConfig().getAgentId());
            // password 和 privateKey 不导出
            req.setRemoteConfig(exported);
        }
        return req;
    }

    private CommandResponse toResponse(Command command) {
        CommandResponse response = new CommandResponse();
        response.setId(command.getId());
        response.setName(command.getName());
        response.setPath(command.getPath());
        response.setDescription(command.getDescription());
        response.setStatus(command.getStatus().name());
        response.setCommandConfig(command.getCommandConfig());
        response.setExecutionMode(command.getExecutionMode() != null
                ? command.getExecutionMode().name()
                : ExecutionMode.LOCAL.name());
        response.setRemoteConfig(maskRemoteConfig(command.getRemoteConfig()));
        response.setGroupName(command.getGroupName());
        response.setTimeoutSeconds(command.getTimeoutSeconds());
        response.setCreatedAt(command.getCreatedAt());
        response.setUpdatedAt(command.getUpdatedAt());

        // 设置危险等级和警告信息
        int dangerLevel = commandTemplate.detectDangerLevel(command);
        response.setDangerLevel(dangerLevel);
        if (dangerLevel > 0) {
            response.setDangerWarning(commandTemplate.getDangerWarning(command));
        }

        return response;
    }

    /**
     * 加密远程配置中的敏感信息（密码和私钥）
     */
    private RemoteConfig encryptRemoteConfig(RemoteConfig config) {
        if (config == null) {
            return null;
        }
        RemoteConfig encrypted = new RemoteConfig();
        encrypted.setHost(config.getHost());
        encrypted.setPort(config.getPort());
        encrypted.setUsername(config.getUsername());
        encrypted.setSshKeyId(config.getSshKeyId());
        encrypted.setAgentId(config.getAgentId());

        // 加密密码
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            encrypted.setPassword(cryptoUtils.encrypt(config.getPassword()));
        }
        // 加密私钥
        if (config.getPrivateKey() != null && !config.getPrivateKey().isBlank()) {
            encrypted.setPrivateKey(cryptoUtils.encrypt(config.getPrivateKey()));
        }
        return encrypted;
    }

    /**
     * 对返回的远程配置进行脱敏（不返回密码和私钥明文）
     */
    private RemoteConfig maskRemoteConfig(RemoteConfig config) {
        if (config == null) {
            return null;
        }
        RemoteConfig masked = new RemoteConfig();
        masked.setHost(config.getHost());
        masked.setPort(config.getPort());
        masked.setUsername(config.getUsername());
        masked.setSshKeyId(config.getSshKeyId());
        masked.setAgentId(config.getAgentId());

        // 密码脱敏：有值则显示占位符
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            masked.setPassword("******");
        }
        // 私钥脱敏
        if (config.getPrivateKey() != null && !config.getPrivateKey().isBlank()) {
            masked.setPrivateKey("******");
        }
        return masked;
    }

    // ========== 版本历史 ==========

    /**
     * 在更新前保存当前命令配置到版本历史
     */
    private void saveVersionSnapshot(Command command, String changeNote) {
        try {
            CreateCommandRequest snapshot = toExportRequest(command);
            String snapshotJson = objectMapper.writeValueAsString(snapshot);

            int nextVersion = commandVersionRepository
                    .findMaxVersionByCommandName(command.getName())
                    .map(v -> v + 1)
                    .orElse(1);

            CommandVersion version = new CommandVersion();
            version.setCommandName(command.getName());
            version.setVersion(nextVersion);
            version.setSnapshot(snapshotJson);
            version.setChangeNote(changeNote);
            version.setChangedAt(LocalDateTime.now());
            commandVersionRepository.save(version);

            log.debug("Saved version {} for command '{}'", nextVersion, command.getName());
        } catch (Exception e) {
            log.warn("Failed to save version snapshot for command '{}': {}", command.getName(), e.getMessage());
        }
    }

    @Override
    public List<CommandVersionResponse> listCommandVersions(String commandName) {
        return commandVersionRepository.findByCommandNameOrderByVersionDesc(commandName)
                .stream()
                .map(v -> CommandVersionResponse.builder()
                        .id(v.getId())
                        .commandName(v.getCommandName())
                        .version(v.getVersion())
                        .snapshot(v.getSnapshot())
                        .changeNote(v.getChangeNote())
                        .changedAt(v.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public CommandResponse rollbackCommandVersion(String commandName, Long versionId) {
        CommandVersion ver = commandVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("Version not found: " + versionId));
        if (!ver.getCommandName().equals(commandName)) {
            throw new BusinessException("Version does not belong to command: " + commandName);
        }
        try {
            CreateCommandRequest request = objectMapper.readValue(ver.getSnapshot(), CreateCommandRequest.class);
            // changeNote 记录回滚行为
            request.setChangeNote("Rollback to version " + ver.getVersion());
            return updateCommand(commandName, request);
        } catch (Exception e) {
            throw new BusinessException("Failed to rollback: " + e.getMessage());
        }
    }
}
