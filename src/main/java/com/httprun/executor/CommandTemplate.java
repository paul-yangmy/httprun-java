package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.ParamDefine;
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
public class CommandTemplate {

    // 匹配 {{.variableName}} 格式的模板变量
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    /**
     * 渲染命令模板
     *
     * 示例:
     * 模板: "ping {{.target}} -c {{.count}}"
     * 参数: {target: "google.com", count: 4}
     * 结果: "ping google.com -c 4"
     */
    public String render(Command command, RunCommandRequest request) {
        String template = command.getCommandConfig().getCommand();

        // 构建参数映射
        Map<String, Object> params = buildParamMap(command, request);

        // 渲染模板
        return renderTemplate(template, params);
    }

    private Map<String, Object> buildParamMap(Command command, RunCommandRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 1. 先填充默认值
        if (command.getCommandConfig().getParams() != null) {
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
        if (command.getCommandConfig().getParams() == null) {
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

            // 验证危险字符（防止命令注入）
            Object value = providedParams.get(paramDef.getName());
            if (value != null) {
                validateParamSecurity(paramDef.getName(), value.toString());
            }
        }
    }

    /**
     * 参数安全验证（防止命令注入）
     */
    private void validateParamSecurity(String name, String value) {
        String[] dangerousChars = { ";", "|", "&", "`", "$", "(", ")", "{", "}", "<", ">", "\n", "\r" };

        for (String dangerous : dangerousChars) {
            if (value.contains(dangerous)) {
                throw new SecurityException(
                        "Parameter '" + name + "' contains dangerous character: " + dangerous);
            }
        }
    }
}
