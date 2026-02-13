package com.httprun.service.impl;

import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.entity.Command;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.CommandStatus;
import com.httprun.enums.ExecutionMode;
import com.httprun.exception.BusinessException;
import com.httprun.executor.CommandExecutor;
import com.httprun.executor.CommandTemplate;
import com.httprun.executor.LocalCommandExecutor;
import com.httprun.executor.SshCommandExecutor;
import com.httprun.repository.CommandRepository;
import com.httprun.service.CommandService;
import com.httprun.util.CommandSecurityValidator;
import com.httprun.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命令服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;
    private final CommandTemplate commandTemplate;
    private final LocalCommandExecutor localExecutor;
    private final SshCommandExecutor sshExecutor;
    private final CryptoUtils cryptoUtils;
    private final CommandSecurityValidator securityValidator;

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
                ? request.getPath() : "/api/run/" + request.getName());
        command.setDescription(request.getDescription());
        command.setCommandConfig(request.getCommandConfig());
        ExecutionMode mode = request.getExecutionMode() != null ? request.getExecutionMode() : ExecutionMode.LOCAL;
        command.setExecutionMode(mode);
        command.setRemoteConfig(encryptRemoteConfig(request.getRemoteConfig()));
        command.setGroupName(request.getGroupName());
        command.setTags(request.getTags());
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
        if (request.getGroupName() != null) {
            command.setGroupName(request.getGroupName());
        }
        if (request.getTags() != null) {
            command.setTags(request.getTags());
        }
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
    public CommandExecutionResult runCommand(RunCommandRequest request, String tokenSubject) {
        // 1. 查询命令
        Command command = commandRepository.findByName(request.getName())
                .orElseThrow(() -> new BusinessException("Command not found: " + request.getName()));

        // 2. 检查命令状态
        if (command.getStatus() != CommandStatus.ACTIVE) {
            return CommandExecutionResult.error("Command is inactive");
        }

        // 3. 检查权限
        if (tokenSubject != null && !tokenSubject.equals("admin")) {
            List<String> allowedCommands = Arrays.asList(tokenSubject.split(","));
            if (!allowedCommands.contains(command.getName())) {
                return CommandExecutionResult.error("Permission denied");
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

        // 7. SSH 模式：必须使用命令中持久化的 remoteConfig，否则无法在远程主机执行
        if (mode == ExecutionMode.SSH) {
            RemoteConfig cmdRemote = command.getRemoteConfig();
            if (cmdRemote == null || cmdRemote.getHost() == null || cmdRemote.getHost().isBlank()) {
                return CommandExecutionResult.error("SSH 命令未配置远程主机信息，请在编辑命令中填写主机、端口、用户名等");
            }
            request.setRemoteConfig(cmdRemote);
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
     * 更新时：合并 host/port/username，仅对新填写的密码/私钥加密，已有加密值保留（避免二次加密）
     */
    private RemoteConfig encryptOrKeepRemoteConfig(RemoteConfig existing, RemoteConfig fromRequest) {
        if (fromRequest == null) {
            return existing;
        }
        RemoteConfig result = new RemoteConfig();
        result.setHost(fromRequest.getHost() != null && !fromRequest.getHost().isBlank()
                ? fromRequest.getHost() : (existing != null ? existing.getHost() : null));
        result.setPort(fromRequest.getPort() != null ? fromRequest.getPort() : (existing != null ? existing.getPort() : null));
        result.setUsername(fromRequest.getUsername() != null && !fromRequest.getUsername().isBlank()
                ? fromRequest.getUsername() : (existing != null ? existing.getUsername() : null));
        result.setSshKeyId(fromRequest.getSshKeyId() != null ? fromRequest.getSshKeyId() : (existing != null ? existing.getSshKeyId() : null));
        result.setAgentId(fromRequest.getAgentId() != null ? fromRequest.getAgentId() : (existing != null ? existing.getAgentId() : null));
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

    private CommandResponse toResponse(Command command) {
        CommandResponse response = new CommandResponse();
        response.setId(command.getId());
        response.setName(command.getName());
        response.setPath(command.getPath());
        response.setDescription(command.getDescription());
        response.setStatus(command.getStatus().name());
        response.setCommandConfig(command.getCommandConfig());
        response.setExecutionMode(command.getExecutionMode() != null
                ? command.getExecutionMode().name() : ExecutionMode.LOCAL.name());
        response.setRemoteConfig(maskRemoteConfig(command.getRemoteConfig()));
        response.setGroupName(command.getGroupName());
        response.setTags(command.getTags());
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
}
