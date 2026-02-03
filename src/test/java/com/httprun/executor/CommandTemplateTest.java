package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.CommandConfig;
import com.httprun.entity.ParamDefine;
import com.httprun.util.CommandSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CommandTemplate 单元测试
 */
class CommandTemplateTest {

    private CommandTemplate commandTemplate;
    private Command testCommand;
    private CommandConfig commandConfig;

    @BeforeEach
    void setUp() {
        commandTemplate = new CommandTemplate(new CommandSecurityValidator());

        // 创建命令配置
        commandConfig = new CommandConfig();

        ParamDefine targetParam = new ParamDefine();
        targetParam.setName("target");
        targetParam.setRequired(true);
        targetParam.setSensitive(false);

        ParamDefine countParam = new ParamDefine();
        countParam.setName("count");
        countParam.setRequired(false);
        countParam.setDefaultValue("4");
        countParam.setSensitive(false);

        ParamDefine passwordParam = new ParamDefine();
        passwordParam.setName("password");
        passwordParam.setRequired(true);
        passwordParam.setSensitive(true);

        commandConfig.setParams(List.of(targetParam, countParam, passwordParam));
        commandConfig.setCommand("ping {{.target}} -c {{.count}} -p {{password}}");

        testCommand = new Command();
        testCommand.setName("test-ping");
        testCommand.setCommandConfig(commandConfig);
    }

    @Test
    void render_shouldReplaceVariables() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("target");
        param1.setValue("google.com");

        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("secret123");

        request.setParams(List.of(param1, param2));

        // When
        String result = commandTemplate.render(testCommand, request);

        // Then
        assertThat(result).isEqualTo("ping google.com -c 4 -p secret123");
    }

    @Test
    void render_shouldUseDefaultValues() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("target");
        param1.setValue("example.com");

        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("pass123");

        request.setParams(List.of(param1, param2));

        // When
        String result = commandTemplate.render(testCommand, request);

        // Then
        assertThat(result).contains("-c 4"); // 使用默认值
    }

    @Test
    void renderWithMasking_shouldReturnBothVersions() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("target");
        param1.setValue("google.com");

        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("secret123");

        request.setParams(List.of(param1, param2));

        // When
        String[] result = commandTemplate.renderWithMasking(testCommand, request);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result[0]).contains("secret123"); // 实际命令
        assertThat(result[1]).contains("***"); // 脱敏命令
        assertThat(result[1]).doesNotContain("secret123");
    }

    @Test
    void validateParams_shouldPassWithValidParams() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("target");
        param1.setValue("google.com");

        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("pass123");

        request.setParams(List.of(param1, param2));

        // When / Then
        commandTemplate.validateParams(testCommand, request);
    }

    @Test
    void validateParams_shouldFailWithMissingRequiredParam() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("count");
        param1.setValue("10");

        request.setParams(List.of(param1));

        // When / Then
        assertThatThrownBy(() -> commandTemplate.validateParams(testCommand, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void validateParams_shouldFailWithDangerousCharacters() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ping");

        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("target");
        param1.setValue("google.com; rm -rf /");

        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("pass123");

        request.setParams(List.of(param1, param2));

        // When / Then
        assertThatThrownBy(() -> commandTemplate.validateParams(testCommand, request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("dangerous character");
    }

    @Test
    void render_withoutConfig_shouldThrowException() {
        // Given
        Command emptyCommand = new Command();
        emptyCommand.setName("empty");
        RunCommandRequest request = new RunCommandRequest();

        // When / Then
        assertThatThrownBy(() -> commandTemplate.render(emptyCommand, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Command configuration is not set");
    }
}
