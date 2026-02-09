package com.httprun.service;

import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.response.CommandResponse;
import com.httprun.entity.Command;
import com.httprun.entity.CommandConfig;
import com.httprun.entity.ParamDefine;
import com.httprun.enums.CommandStatus;
import com.httprun.enums.ExecutionMode;
import com.httprun.executor.CommandTemplate;
import com.httprun.executor.LocalCommandExecutor;
import com.httprun.executor.SshCommandExecutor;
import com.httprun.repository.CommandRepository;
import com.httprun.service.impl.CommandServiceImpl;
import com.httprun.util.CommandSecurityValidator;
import com.httprun.util.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 命令服务测试
 */
@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private CommandTemplate commandTemplate;

    @Mock
    private LocalCommandExecutor localExecutor;

    @Mock
    private SshCommandExecutor sshExecutor;

    @Mock
    private CryptoUtils cryptoUtils;

    @Mock
    private CommandSecurityValidator securityValidator;

    @InjectMocks
    private CommandServiceImpl commandService;

    private Command testCommand;
    private CommandConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new CommandConfig();
        testConfig.setCommand("echo {{.message}}");
        ParamDefine paramDef = new ParamDefine();
        paramDef.setName("message");
        paramDef.setType("string");
        paramDef.setRequired(true);
        testConfig.setParams(List.of(paramDef));

        testCommand = new Command();
        testCommand.setId(1L);
        testCommand.setName("test-command");
        testCommand.setDescription("Test command description");
        testCommand.setCommandConfig(testConfig);
        testCommand.setExecutionMode(ExecutionMode.LOCAL);
        testCommand.setTimeoutSeconds(30);
        testCommand.setStatus(CommandStatus.ACTIVE);
    }

    @Test
    void testListAllCommands() {
        when(commandRepository.findAll()).thenReturn(List.of(testCommand));

        List<CommandResponse> result = commandService.listAllCommands();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-command", result.get(0).getName());
        verify(commandRepository).findAll();
    }

    @Test
    void testListCommandsByNames() {
        List<String> names = Arrays.asList("test-command");
        when(commandRepository.findByNameIn(names)).thenReturn(List.of(testCommand));

        List<CommandResponse> result = commandService.listCommands(names);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-command", result.get(0).getName());
    }

    @Test
    void testCreateCommand() {
        CreateCommandRequest request = new CreateCommandRequest();
        request.setName("new-command");
        request.setDescription("New command");
        request.setCommandConfig(testConfig);
        request.setExecutionMode(ExecutionMode.LOCAL);
        request.setTimeoutSeconds(30);

        Command savedCommand = new Command();
        savedCommand.setId(2L);
        savedCommand.setName(request.getName());
        savedCommand.setDescription(request.getDescription());
        savedCommand.setCommandConfig(request.getCommandConfig());
        savedCommand.setExecutionMode(request.getExecutionMode());
        savedCommand.setTimeoutSeconds(request.getTimeoutSeconds());
        savedCommand.setStatus(CommandStatus.ACTIVE);

        when(commandRepository.save(any(Command.class))).thenReturn(savedCommand);

        CommandResponse response = commandService.createCommand(request);

        assertNotNull(response);
        assertEquals("new-command", response.getName());
        verify(commandRepository).save(any(Command.class));
    }

    @Test
    void testUpdateCommandStatus() {
        List<String> names = Arrays.asList("test-command");
        doNothing().when(commandRepository).updateStatusByNameIn(anyList(), any(CommandStatus.class));

        commandService.updateCommandStatus(names, "DISABLED");

        verify(commandRepository).updateStatusByNameIn(eq(names), eq(CommandStatus.DISABLED));
    }

    @Test
    void testDeleteCommands() {
        List<String> names = Arrays.asList("test-command");
        doNothing().when(commandRepository).deleteByNameIn(names);

        commandService.deleteCommands(names);

        verify(commandRepository).deleteByNameIn(names);
    }
}
