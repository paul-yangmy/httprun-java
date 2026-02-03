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

    @Override
    @Transactional
    @CacheEvict(value = "commands", allEntries = true)
    public CommandResponse createCommand(CreateCommandRequest request) {
        Command command = new Command();
        command.setName(request.getName());
        command.setPath(request.getPath());
        command.setDescription(request.getDescription());
        command.setCommandConfig(request.getCommandConfig());
        command.setExecutionMode(request.getExecutionMode() != null ? request.getExecutionMode() : ExecutionMode.LOCAL);
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
            command.setRemoteConfig(encryptRemoteConfig(request.getRemoteConfig()));
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

        // 6. 选择执行器并执行
        CommandExecutor executor = selectExecutor(command.getExecutionMode());
        int timeout = request.getTimeout() != null ? request.getTimeout() : command.getTimeoutSeconds();

        // 7. 如果是 SSH 模式，且请求中没有 remoteConfig，使用命令配置中的 remoteConfig
        if (command.getExecutionMode() == ExecutionMode.SSH && request.getRemoteConfig() == null) {
            request.setRemoteConfig(command.getRemoteConfig());
        }

        return executor.execute(actualCommand, request, timeout);
    }

    private CommandExecutor selectExecutor(ExecutionMode mode) {
        return switch (mode) {
            case SSH -> sshExecutor;
            case AGENT -> throw new UnsupportedOperationException("Agent mode not implemented");
            default -> localExecutor;
        };
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
        response.setExecutionMode(command.getExecutionMode().name());
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
