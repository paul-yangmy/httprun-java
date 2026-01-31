package com.httprun.util;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.CommandConfig;
import com.httprun.entity.ParamDefine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveDataMasker 单元测试
 */
class SensitiveDataMaskerTest {

    private Command testCommand;
    private Map<String, Object> testParams;

    @BeforeEach
    void setUp() {
        // 创建测试命令
        CommandConfig config = new CommandConfig();
        
        ParamDefine passwordParam = new ParamDefine();
        passwordParam.setName("password");
        passwordParam.setSensitive(true);
        
        ParamDefine apiKeyParam = new ParamDefine();
        apiKeyParam.setName("api_key");
        apiKeyParam.setSensitive(true);
        
        ParamDefine usernameParam = new ParamDefine();
        usernameParam.setName("username");
        usernameParam.setSensitive(false);
        
        config.setParams(List.of(passwordParam, apiKeyParam, usernameParam));
        config.setCommand("ssh {{username}}@{{host}} -p {{password}}");
        
        testCommand = new Command();
        testCommand.setName("test-ssh");
        testCommand.setCommandConfig(config);
        
        // 创建测试参数
        testParams = new HashMap<>();
        testParams.put("username", "admin");
        testParams.put("password", "secret123");
        testParams.put("api_key", "sk-abc123");
        testParams.put("host", "example.com");
    }

    @Test
    void maskParams_shouldMaskSensitiveParameters() {
        // When
        Map<String, Object> masked = SensitiveDataMasker.maskParams(testCommand, testParams);

        // Then
        assertThat(masked.get("username")).isEqualTo("admin");
        assertThat(masked.get("password")).isEqualTo("***");
        assertThat(masked.get("api_key")).isEqualTo("***");
        assertThat(masked.get("host")).isEqualTo("example.com");
    }

    @Test
    void maskCommand_shouldReplaceSenitiveValues() {
        // Given
        String renderedCommand = "ssh admin@example.com -p secret123 --key=sk-abc123";

        // When
        String masked = SensitiveDataMasker.maskCommand(testCommand, renderedCommand, testParams);

        // Then
        assertThat(masked).contains("admin@example.com");
        assertThat(masked).contains("***");
        assertThat(masked).doesNotContain("secret123");
        assertThat(masked).doesNotContain("sk-abc123");
    }

    @Test
    void maskRequestJson_shouldMaskParamsInJson() {
        // Given
        String requestJson = "{\"name\":\"test-ssh\",\"params\":[" +
                "{\"name\":\"username\",\"value\":\"admin\"}," +
                "{\"name\":\"password\",\"value\":\"secret123\"}," +
                "{\"name\":\"api_key\",\"value\":\"sk-abc123\"}" +
                "]}";

        // When
        String masked = SensitiveDataMasker.maskRequestJson(testCommand, requestJson);

        // Then
        assertThat(masked).contains("\"username\"");
        assertThat(masked).contains("\"admin\"");
        assertThat(masked).contains("***");
        assertThat(masked).doesNotContain("secret123");
        assertThat(masked).doesNotContain("sk-abc123");
    }

    @Test
    void maskRequest_shouldMaskRunCommandRequest() {
        // Given
        RunCommandRequest request = new RunCommandRequest();
        request.setName("test-ssh");
        
        RunCommandRequest.ParamInput param1 = new RunCommandRequest.ParamInput();
        param1.setName("username");
        param1.setValue("admin");
        
        RunCommandRequest.ParamInput param2 = new RunCommandRequest.ParamInput();
        param2.setName("password");
        param2.setValue("secret123");
        
        request.setParams(List.of(param1, param2));

        // When
        String masked = SensitiveDataMasker.maskRequest(testCommand, request);

        // Then
        assertThat(masked).contains("\"username\"");
        assertThat(masked).contains("\"admin\"");
        assertThat(masked).contains("***");
        assertThat(masked).doesNotContain("secret123");
    }

    @Test
    void maskParams_withNullCommand_shouldReturnOriginal() {
        // When
        Map<String, Object> masked = SensitiveDataMasker.maskParams(null, testParams);

        // Then
        assertThat(masked).isEqualTo(testParams);
    }

    @Test
    void maskCommand_withNullCommand_shouldReturnOriginal() {
        // Given
        String command = "echo test";

        // When
        String masked = SensitiveDataMasker.maskCommand(null, command, testParams);

        // Then
        assertThat(masked).isEqualTo(command);
    }
}
