package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.ParamDefine;
import com.httprun.util.CommandSecurityValidator;
import com.httprun.util.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令模板处理器
 * 实现类似 Go text/template 的模板渲染功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandTemplate {

    private final CommandSecurityValidator securityValidator;

    // 匹配 {{.variableName}} 或 {{variableName}} 格式的模板变量（点号可选）
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.?([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    /**
     * 渲染命令模板
     *
     * 示例:
     * 模板: "ping {{.target}} -c {{.count}}"
     * 参数: {target: "google.com", count: 4}
     * 结果: "ping google.com -c 4"
     */
    public String render(Command command, RunCommandRequest request) {
        // 检查 commandConfig 是否存在
        if (command.getCommandConfig() == null || command.getCommandConfig().getCommand() == null) {
            throw new IllegalArgumentException(
                    "Command configuration is not set. Please configure the command template first.");
        }
        String template = command.getCommandConfig().getCommand();

        // 构建参数映射
        Map<String, Object> params = buildParamMap(command, request);

        // 渲染模板
        return renderTemplate(template, params);
    }

    /**
     * 渲染命令模板并返回脱敏后的日志字符串
     * 用于日志记录，敏感参数会被替换为 ***
     *
     * @return 数组 [0]=实际命令, [1]=脱敏后的日志
     */
    public String[] renderWithMasking(Command command, RunCommandRequest request) {
        String actualCommand = render(command, request);
        Map<String, Object> params = buildParamMap(command, request);
        String maskedCommand = SensitiveDataMasker.maskCommand(command, actualCommand, params);
        return new String[] { actualCommand, maskedCommand };
    }

    private Map<String, Object> buildParamMap(Command command, RunCommandRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 1. 先填充默认值
        if (command.getCommandConfig() != null && command.getCommandConfig().getParams() != null) {
            for (ParamDefine paramDef : command.getCommandConfig().getParams()) {
                if (paramDef.getDefaultValue() != null) {
                    params.put(paramDef.getName(), paramDef.getDefaultValue());
                }
            }
        }

        // 2. 用请求参数覆盖
        if (request.getParams() != null) {
            for (var param : request.getParams()) {
                if (param.getValue() != null) {
                    params.put(param.getName(), param.getValue());
                }
            }
        }

        return params;
    }

    private String renderTemplate(String template, Map<String, Object> params) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = params.get(varName);
            String replacement = value != null ? String.valueOf(value) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 验证参数
     */
    public void validateParams(Command command, RunCommandRequest request) {
        // 如果没有配置 commandConfig，跳过参数验证
        if (command.getCommandConfig() == null || command.getCommandConfig().getParams() == null) {
            return;
        }

        Map<String, Object> providedParams = new HashMap<>();
        if (request.getParams() != null) {
            for (var param : request.getParams()) {
                providedParams.put(param.getName(), param.getValue());
            }
        }

        for (ParamDefine paramDef : command.getCommandConfig().getParams()) {
            if (paramDef.isRequired()) {
                Object value = providedParams.get(paramDef.getName());
                if (value == null || value.toString().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Parameter '" + paramDef.getName() + "' is required");
                }
            }
        }

        // 使用增强的综合安全检查（包含命令注入防护、参数白名单验证、路径遍历防护）
        // strictMode=true 启用严格模式（参数白名单验证）
        securityValidator.performFullSecurityCheck(command, providedParams, true);
    }

    /**
     * 检测命令危险等级
     * 
     * @return 0=安全, 1=警告, 2=高危
     */
    public int detectDangerLevel(Command command) {
        if (command.getCommandConfig() == null || command.getCommandConfig().getCommand() == null) {
            return 0;
        }
        return securityValidator.detectDangerLevel(command.getCommandConfig().getCommand(), null);
    }

    /**
     * 获取高危命令警告信息
     */
    public String getDangerWarning(Command command) {
        if (command.getCommandConfig() == null || command.getCommandConfig().getCommand() == null) {
            return null;
        }
        int level = detectDangerLevel(command);
        if (level > 0) {
            return securityValidator.getDangerWarning(command.getCommandConfig().getCommand());
        }
        return null;
    }
}
