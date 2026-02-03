package com.httprun.util;

import com.httprun.entity.Command;
import com.httprun.entity.CommandConfig;
import com.httprun.entity.ParamDefine;
import com.httprun.exception.SecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandSecurityValidator 单元测试
 * 覆盖命令注入防护、参数白名单验证、路径遍历防护
 */
class CommandSecurityValidatorTest {

    private CommandSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CommandSecurityValidator();
    }

    // ========== 命令注入防护测试 ==========
    @Nested
    @DisplayName("命令注入防护测试")
    class CommandInjectionTests {

        @Test
        @DisplayName("检测分号命令分隔符注入")
        void shouldDetectSemicolonInjection() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("target", "localhost; rm -rf /"));
        }

        @Test
        @DisplayName("检测管道符注入")
        void shouldDetectPipeInjection() {
            assertThrows(SecurityException.class,
                    () -> validator.validateParamValue("target", "localhost | cat /etc/passwd"));
        }

        @Test
        @DisplayName("检测命令替换注入 $()")
        void shouldDetectCommandSubstitution() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("name", "$(whoami)"));
        }

        @Test
        @DisplayName("检测反引号命令替换注入")
        void shouldDetectBacktickInjection() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("name", "`id`"));
        }

        @Test
        @DisplayName("检测变量展开注入 ${}")
        void shouldDetectVariableExpansion() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("name", "${HOME}"));
        }

        @Test
        @DisplayName("检测换行注入")
        void shouldDetectNewlineInjection() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("target", "localhost\nrm -rf /"));
        }

        @Test
        @DisplayName("检测 && 逻辑连接符注入")
        void shouldDetectAndOperatorInjection() {
            assertThrows(SecurityException.class,
                    () -> validator.validateParamValue("target", "localhost && rm -rf /"));
        }

        @Test
        @DisplayName("检测 || 逻辑连接符注入")
        void shouldDetectOrOperatorInjection() {
            assertThrows(SecurityException.class,
                    () -> validator.validateParamValue("target", "localhost || rm -rf /"));
        }

        @Test
        @DisplayName("检测重定向符注入")
        void shouldDetectRedirectionInjection() {
            assertThrows(SecurityException.class, () -> validator.validateParamValue("file", "/tmp/log > /etc/passwd"));
        }

        @Test
        @DisplayName("正常参数值应该通过验证")
        void shouldPassNormalValues() {
            assertDoesNotThrow(() -> validator.validateParamValue("target", "192.168.1.1"));
            assertDoesNotThrow(() -> validator.validateParamValue("name", "hello_world"));
            assertDoesNotThrow(() -> validator.validateParamValue("count", "10"));
        }
    }

    // ========== 参数白名单验证测试 ==========
    @Nested
    @DisplayName("参数白名单验证测试")
    class WhitelistValidationTests {

        @Test
        @DisplayName("验证字母数字格式")
        void shouldValidateAlphanumeric() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("name", "hello_world-123", "alphanumeric"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("name", "hello world", "alphanumeric"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("name", "hello;world", "alphanumeric"));
        }

        @Test
        @DisplayName("验证严格字母数字格式（无空格）")
        void shouldValidateStrictAlphanumeric() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("name", "hello_world-123", "strict_alphanumeric"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("name", "hello world", "strict_alphanumeric"));
        }

        @Test
        @DisplayName("验证文件名格式")
        void shouldValidateFilename() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("file", "report.pdf", "filename"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("file", "my-file_v2.0.tar.gz", "filename"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("file", "../etc/passwd", "filename"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("file", "file;rm", "filename"));
        }

        @Test
        @DisplayName("验证安全路径格式")
        void shouldValidateSafePath() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("path", "logs/app.log", "safe_path"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("path", "../etc/passwd", "safe_path"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("path", "/etc/passwd", "safe_path"));
        }

        @Test
        @DisplayName("验证 IPv4 地址格式")
        void shouldValidateIPv4() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("ip", "192.168.1.1", "ipv4"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("ip", "10.0.0.1", "ipv4"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("ip", "256.1.1.1", "ipv4"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("ip", "192.168.1", "ipv4"));
        }

        @Test
        @DisplayName("验证主机名格式")
        void shouldValidateHostname() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("host", "example.com", "hostname"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("host", "server-01.internal.corp", "hostname"));
            assertThrows(SecurityException.class,
                    () -> validator.validateWithWhitelist("host", "-invalid.com", "hostname"));
        }

        @Test
        @DisplayName("验证端口号格式")
        void shouldValidatePort() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("port", "80", "port"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("port", "8080", "port"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("port", "65535", "port"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("port", "0", "port"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("port", "65536", "port"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("port", "abc", "port"));
        }

        @Test
        @DisplayName("验证整数格式")
        void shouldValidateInteger() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("count", "123", "integer"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("count", "-456", "integer"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("count", "12.34", "integer"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("count", "abc", "integer"));
        }

        @Test
        @DisplayName("验证布尔值格式")
        void shouldValidateBoolean() {
            assertDoesNotThrow(() -> validator.validateWithWhitelist("enabled", "true", "boolean"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("enabled", "false", "boolean"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("enabled", "yes", "boolean"));
            assertDoesNotThrow(() -> validator.validateWithWhitelist("enabled", "1", "boolean"));
            assertThrows(SecurityException.class, () -> validator.validateWithWhitelist("enabled", "maybe", "boolean"));
        }
    }

    // ========== 禁止特殊字符测试 ==========
    @Nested
    @DisplayName("禁止特殊字符测试")
    class ForbiddenCharsTests {

        @Test
        @DisplayName("检测禁止的特殊字符")
        void shouldDetectForbiddenChars() {
            assertThrows(SecurityException.class, () -> validator.validateNoSpecialChars("input", "hello;world"));
            assertThrows(SecurityException.class, () -> validator.validateNoSpecialChars("input", "hello|world"));
            assertThrows(SecurityException.class, () -> validator.validateNoSpecialChars("input", "hello`world"));
            assertThrows(SecurityException.class, () -> validator.validateNoSpecialChars("input", "hello$world"));
            assertThrows(SecurityException.class, () -> validator.validateNoSpecialChars("input", "hello\nworld"));
        }

        @Test
        @DisplayName("允许安全字符")
        void shouldAllowSafeChars() {
            assertDoesNotThrow(() -> validator.validateNoSpecialChars("input", "hello world"));
            assertDoesNotThrow(() -> validator.validateNoSpecialChars("input", "hello-world_123"));
            assertDoesNotThrow(() -> validator.validateNoSpecialChars("input", "path/to/file.txt"));
            assertDoesNotThrow(() -> validator.validateNoSpecialChars("input", "192.168.1.1"));
        }
    }

    // ========== 路径遍历防护测试 ==========
    @Nested
    @DisplayName("路径遍历防护测试")
    class PathTraversalTests {

        @Test
        @DisplayName("检测 ../ 路径遍历")
        void shouldDetectDotDotSlash() {
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "../etc/passwd"));
            assertThrows(SecurityException.class,
                    () -> validator.validatePathSecurity("path", "logs/../../../etc/passwd"));
        }

        @Test
        @DisplayName("检测 ..\\ 路径遍历（Windows）")
        void shouldDetectDotDotBackslash() {
            assertThrows(SecurityException.class,
                    () -> validator.validatePathSecurity("path", "..\\windows\\system32"));
        }

        @Test
        @DisplayName("检测 URL 编码路径遍历")
        void shouldDetectUrlEncodedTraversal() {
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "%2e%2e/etc/passwd"));
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "%2e%2e%2f"));
        }

        @Test
        @DisplayName("检测敏感系统路径")
        void shouldDetectSensitivePaths() {
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "/etc/passwd"));
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "/root/.ssh/id_rsa"));
            assertThrows(SecurityException.class, () -> validator.validatePathSecurity("path", "/proc/self/environ"));
        }

        @Test
        @DisplayName("检测 Windows 敏感路径")
        void shouldDetectWindowsSensitivePaths() {
            // 测试 \windows 模式（包含在敏感路径列表中）
            assertThrows(SecurityException.class,
                    () -> validator.validatePathSecurity("path", "\\windows\\system32\\config"));
        }

        @Test
        @DisplayName("允许安全相对路径")
        void shouldAllowSafeRelativePaths() {
            assertDoesNotThrow(() -> validator.validatePathSecurity("path", "logs/app.log"));
            assertDoesNotThrow(() -> validator.validatePathSecurity("path", "data/reports/2024/report.pdf"));
        }
    }

    // ========== 高危命令检测测试 ==========
    @Nested
    @DisplayName("高危命令检测测试")
    class DangerousCommandTests {

        @Test
        @DisplayName("检测 rm 命令为警告级别")
        void shouldDetectRmAsWarning() {
            int level = validator.detectDangerLevel("rm file.txt", null);
            assertEquals(1, level);
        }

        @Test
        @DisplayName("检测 rm -rf / 为高危")
        void shouldDetectRmRfRootAsHighRisk() {
            int level = validator.detectDangerLevel("rm -rf /", null);
            assertEquals(2, level);
        }

        @Test
        @DisplayName("检测 shutdown 为警告级别")
        void shouldDetectShutdownAsWarning() {
            int level = validator.detectDangerLevel("shutdown -h now", null);
            assertEquals(1, level);
        }

        @Test
        @DisplayName("检测 dd 写入设备为高危")
        void shouldDetectDdToDeviceAsHighRisk() {
            int level = validator.detectDangerLevel("dd if=/dev/zero of=/dev/sda", null);
            assertEquals(2, level);
        }

        @Test
        @DisplayName("检测 DROP DATABASE 为高危")
        void shouldDetectDropDatabaseAsHighRisk() {
            int level = validator.detectDangerLevel("DROP DATABASE mydb", null);
            assertEquals(2, level);
        }

        @Test
        @DisplayName("安全命令应该返回 0")
        void shouldReturnZeroForSafeCommands() {
            assertEquals(0, validator.detectDangerLevel("ls -la", null));
            assertEquals(0, validator.detectDangerLevel("cat file.txt", null));
            assertEquals(0, validator.detectDangerLevel("grep pattern file", null));
            assertEquals(0, validator.detectDangerLevel("ping google.com", null));
        }

        @Test
        @DisplayName("获取高危命令警告信息")
        void shouldGetDangerWarning() {
            String warning = validator.getDangerWarning("rm -rf /tmp/*");
            assertNotNull(warning);
            assertTrue(warning.contains("删除"));
        }
    }

    // ========== 综合安全检查测试 ==========
    @Nested
    @DisplayName("综合安全检查测试")
    class FullSecurityCheckTests {

        private Command createCommandWithParams(String... params) {
            Command command = new Command();
            CommandConfig config = new CommandConfig();
            config.setCommand("echo {{.input}}");
            if (params.length > 0) {
                ParamDefine[] paramDefines = new ParamDefine[params.length / 2];
                for (int i = 0; i < params.length; i += 2) {
                    ParamDefine def = new ParamDefine();
                    def.setName(params[i]);
                    def.setType(params[i + 1]);
                    paramDefines[i / 2] = def;
                }
                config.setParams(Arrays.asList(paramDefines));
            }
            command.setCommandConfig(config);
            return command;
        }

        @Test
        @DisplayName("综合检查应该捕获命令注入")
        void shouldCatchInjectionInFullCheck() {
            Command command = createCommandWithParams("input", "string");
            Map<String, Object> params = new HashMap<>();
            params.put("input", "hello; rm -rf /");

            assertThrows(SecurityException.class, () -> validator.performFullSecurityCheck(command, params, true));
        }

        @Test
        @DisplayName("综合检查应该捕获路径遍历")
        void shouldCatchPathTraversalInFullCheck() {
            Command command = createCommandWithParams("file", "path");
            Map<String, Object> params = new HashMap<>();
            params.put("file", "../etc/passwd");

            assertThrows(SecurityException.class, () -> validator.performFullSecurityCheck(command, params, true));
        }

        @Test
        @DisplayName("综合检查应该通过安全参数")
        void shouldPassSafeParams() {
            Command command = createCommandWithParams("name", "string", "count", "integer");
            Map<String, Object> params = new HashMap<>();
            params.put("name", "hello");
            params.put("count", "10");

            assertDoesNotThrow(() -> validator.performFullSecurityCheck(command, params, false));
        }
    }
}
